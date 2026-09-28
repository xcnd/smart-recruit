#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
简历解析 / AI 筛选 基准测试驱动脚本（零第三方依赖，仅用标准库）。

流程：
  1. 预检   — GET /api/v1/jobs 解析出一个可关联的 jobPositionId
  2. 上传   — 并发 multipart 上传全部样本到 POST /api/v1/resumes/upload（autoScreen=false）
  3. 轮询   — 轮询 GET /api/v1/resumes/{id}/parse-status 直到全部到达终态
  4. 批筛   — POST /api/v1/resumes/batch-screen，再轮询 /batch-screen/{taskId}/progress
  5. 落盘   — 写出 data/benchmark/runs/run_<ts>.json 供 report.py 消费

用法：
  python run_benchmark.py                      # 全量 60 份
  python run_benchmark.py --dry-run            # 只做连通性 + 清单校验，不上传
  python run_benchmark.py --job-id 12          # 指定关联职位
  python run_benchmark.py --limit 10           # 只跑前 10 份（省 API 费用）
  python run_benchmark.py --good-only          # 跳过 8 份坏样本
  python run_benchmark.py --skip-screen        # 只测解析，不跑批筛
  python run_benchmark.py --concurrency 1      # 串行上传，测单份端到端耗时

重要说明（面试可讲的口径）：
  * 上传耗时（upload_ms）   = 本脚本客户端观测的 HTTP 往返，真实可信。
  * 解析耗时（parse_ms）    = 本脚本**轮询**到终态的时间，是「轮询分辨率上界」，
                              会随 --poll-interval 偏大，不要当精确值用。
  * 解析真实耗时/Token      = 由服务端 ai_resume_parse_log 记录，report.py 直接查库，
                              这是权威口径，面试报这个数。
  * 本脚本只负责「触发 + 收 resumeId + 测吞吐」，精确指标一律以库表为准。
"""

from __future__ import annotations

import argparse
import json
import mimetypes
import os
import random
import statistics
import sys
import threading
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path

# ──────────────────────────── 常量 ────────────────────────────

PARSE_PENDING, PARSE_PARSING, PARSE_SUCCESS, PARSE_FAILED = 0, 1, 2, 3
PARSE_TERMINAL = {PARSE_SUCCESS, PARSE_FAILED}
PARSE_STATUS_NAME = {0: "PENDING", 1: "PARSING", 2: "SUCCESS", 3: "FAILED"}

SCRIPT_DIR = Path(__file__).resolve().parent
BACKEND_ROOT = SCRIPT_DIR.parent.parent  # smart-recruit-backend/
DEFAULT_MANIFEST = BACKEND_ROOT / "data" / "benchmark" / "samples" / "manifest.json"
DEFAULT_RUNS_DIR = BACKEND_ROOT / "data" / "benchmark" / "runs"

# 服务直连（网关只是转发 + 鉴权，压测走直连避免网关成为瓶颈/单点）
DEFAULT_BASE_URL = os.environ.get("BENCH_BASE_URL", "http://localhost:8082")

# 请求头：网关注入的用户信息。服务端 SecurityConfig 是 permitAll 且未开 method security，
# 不带也能通；带上是为了让 createdBy / createUserId 等字段有值，行为更贴近真实调用。
DEFAULT_HEADERS = {
    "X-User-Id": "1",
    "X-Username": "benchmark",
    "X-User-Role": "admin",
}

HTTP_TIMEOUT = 120
UPLOAD_RETRIES = 3


# ──────────────────────────── HTTP 工具 ────────────────────────────


class HttpError(Exception):
    def __init__(self, status: int, body: str):
        super().__init__(f"HTTP {status}: {body[:300]}")
        self.status = status
        self.body = body


class ApiRejected(RuntimeError):
    """服务端以业务错误码拒绝（success=false）。

    校验类（4xxxx，如文件超限/类型不允许）属于「按预期拦下了这份文件」，
    是基准测试想要的正常结果，不是链路故障；50001 之类才是真故障。
    """

    def __init__(self, code, message: str):
        super().__init__(f"[{code}] {message}")
        self.code = code
        self.message = message


def is_rejection_code(code) -> bool:
    """4xxxx 业务校验错误码 = 预期的拦截；5xxxx 是内部故障，不能算拦截。"""
    return isinstance(code, int) and 40000 <= code < 50000


def _open(req: urllib.request.Request, timeout: int = HTTP_TIMEOUT) -> tuple[int, bytes]:
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as e:
        raise HttpError(e.code, e.read().decode("utf-8", "replace")) from None
    except urllib.error.URLError as e:
        raise ConnectionError(f"无法连接 {req.full_url}: {e.reason}") from None


def api_get(base_url: str, path: str, timeout: int = HTTP_TIMEOUT) -> dict:
    req = urllib.request.Request(base_url + path, headers=DEFAULT_HEADERS, method="GET")
    _, raw = _open(req, timeout)
    return json.loads(raw.decode("utf-8"))


def api_post_json(base_url: str, path: str, payload: dict,
                  timeout: int = HTTP_TIMEOUT) -> dict:
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    headers = {**DEFAULT_HEADERS, "Content-Type": "application/json; charset=utf-8"}
    req = urllib.request.Request(base_url + path, data=body, headers=headers, method="POST")
    _, raw = _open(req, timeout)
    return json.loads(raw.decode("utf-8"))


def api_post_multipart(base_url: str, path: str, fields: dict[str, str],
                       file_field: str, file_path: Path) -> dict:
    """手工拼 multipart/form-data（避免引入 requests 依赖）。"""
    boundary = "----bench" + uuid.uuid4().hex
    ctype = mimetypes.guess_type(str(file_path))[0] or "application/octet-stream"

    buf = bytearray()
    for name, value in fields.items():
        if value is None:
            continue
        buf += f"--{boundary}\r\n".encode()
        buf += f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode()
        buf += str(value).encode("utf-8")
        buf += b"\r\n"

    buf += f"--{boundary}\r\n".encode()
    # filename 用 UTF-8 原名（Spring Boot 3 的 multipart 解析默认 UTF-8）
    buf += (
        f'Content-Disposition: form-data; name="{file_field}"; '
        f'filename="{file_path.name}"\r\n'
    ).encode("utf-8")
    buf += f"Content-Type: {ctype}\r\n\r\n".encode()
    buf += file_path.read_bytes()
    buf += f"\r\n--{boundary}--\r\n".encode()

    headers = {
        **DEFAULT_HEADERS,
        "Content-Type": f"multipart/form-data; boundary={boundary}",
    }
    req = urllib.request.Request(base_url + path, data=bytes(buf),
                                 headers=headers, method="POST")
    _, raw = _open(req)
    return json.loads(raw.decode("utf-8"))


def unwrap(resp: dict) -> dict:
    """校验 ApiResponse.success 并取出 data。"""
    if not isinstance(resp, dict) or not resp.get("success"):
        if isinstance(resp, dict):
            raise ApiRejected(resp.get("code"), resp.get("message") or "")
        raise RuntimeError(f"接口返回失败: {resp}")
    return resp.get("data") or {}


# ──────────────────────────── 阶段 1：预检 ────────────────────────────


def resolve_job_id(base_url: str, job_id: int | None) -> int:
    """确定一个可关联简历的职位 ID（不能是 DRAFT/CLOSED）。"""
    if job_id:
        print(f"[预检] 使用指定职位 jobPositionId={job_id}")
        return job_id

    print("[预检] 查询 GET /api/v1/jobs 以解析 jobPositionId ...")
    data = unwrap(api_get(base_url, "/api/v1/jobs?page=1&size=100"))
    records = data.get("records") or []
    if not records:
        sys.exit(
            "[预检] 失败：职位列表为空。请先在系统里创建一个「已发布」职位，"
            "或用 --job-id 指定，或手动建一个：\n"
            "  curl -X POST http://localhost:8082/api/v1/jobs -H 'Content-Type: application/json' "
            "-d '{\"title\":\"Java后端开发工程师\",...}'"
        )

    # JobStatus: 参考 RecruitmentEnums，DRAFT 与 CLOSED 不能关联简历
    usable = [j for j in records if j.get("status") not in (None, 0, 3)]
    pool = usable or records
    job = pool[0]
    print(f"[预检] 选用职位 id={job.get('id')} title={job.get('title')!r} status={job.get('status')}")
    if not usable:
        print("[预检] 警告：没有找到「已发布/暂停」状态的职位，可能上传会被拒。")
    return int(job["id"])


# ──────────────────────────── 阶段 2：上传 ────────────────────────────


def upload_one(base_url: str, samples_dir: Path, sample: dict, job_id: int) -> dict:
    """上传单份样本，返回带耗时与 resumeId 的记录。"""
    path = samples_dir / sample["path"]
    rec = {
        "sample_id": sample["id"],
        "path": sample["path"],
        "file": path.name,
        "category": sample["category"],
        "is_bad": sample["is_bad"],
        "bad_reason": sample.get("bad_reason"),
        "expected_name": sample.get("expected_name"),
        "expect_position": sample.get("expect_position"),
        "size_bytes": sample.get("size_bytes"),
        "upload_ok": False,
        "rejected": False,
        "resume_id": None,
        "upload_ms": None,
        "http_status": None,
        "error": None,
        "parse_status": None,
        "parse_error": None,
        "parse_ms": None,
    }

    fields = {"jobPositionId": str(job_id), "autoScreen": "false"}
    last_err = None
    for attempt in range(1, UPLOAD_RETRIES + 1):
        t0 = time.perf_counter()
        try:
            resp = api_post_multipart(base_url, "/api/v1/resumes/upload",
                                      fields, "file", path)
            rec["upload_ms"] = round((time.perf_counter() - t0) * 1000, 1)
            data = unwrap(resp)
            rec["upload_ok"] = True
            rec["resume_id"] = str(data.get("id"))
            rec["_t_done"] = time.perf_counter()
            return rec
        except ApiRejected as e:
            rec["upload_ms"] = round((time.perf_counter() - t0) * 1000, 1)
            if is_rejection_code(e.code):
                rec["rejected"] = True
                last_err = f"REJECTED(code={e.code}): {e.message}"
                break  # 校验失败是确定性的，重试无意义
            last_err = f"API_ERROR(code={e.code}): {e.message}"
        except HttpError as e:
            rec["upload_ms"] = round((time.perf_counter() - t0) * 1000, 1)
            rec["http_status"] = e.status
            last_err = f"HTTP {e.status}: {e.body[:200]}"
            # 4xx 是我们的问题，重试无意义
            if 400 <= e.status < 500:
                break
        except Exception as e:  # 连接重置、超时等，值得重试
            rec["upload_ms"] = round((time.perf_counter() - t0) * 1000, 1)
            last_err = f"{type(e).__name__}: {e}"
        if attempt < UPLOAD_RETRIES:
            time.sleep(1.5 * attempt)

    rec["error"] = last_err
    return rec


def run_uploads(base_url: str, samples_dir: Path, samples: list[dict],
                job_id: int, concurrency: int) -> list[dict]:
    total = len(samples)
    print(f"[上传] 开始上传 {total} 份样本，并发度={concurrency} ...")
    results: list[dict] = [None] * total  # type: ignore[list-item]
    done = 0
    lock = threading.Lock()

    t_start = time.perf_counter()
    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        futures = {pool.submit(upload_one, base_url, samples_dir, s, job_id): i
                   for i, s in enumerate(samples)}
        for fut in as_completed(futures):
            idx = futures[fut]
            results[idx] = fut.result()
            with lock:
                done += 1
                r = results[idx]
                mark = "OK " if r["upload_ok"] else ("REJ" if r.get("rejected") else "ERR")
                print(f"  [{done:>3}/{total}] {mark} {r['file']} "
                      f"({r['upload_ms']}ms) resumeId={r['resume_id'] or '-'}")
    wall = time.perf_counter() - t_start

    ok = [r for r in results if r["upload_ok"]]
    print(f"[上传] 完成：成功 {len(ok)}/{total}，墙钟 {wall:.1f}s，"
          f"吞吐 {len(ok) / wall * 60:.1f} 份/分钟")
    return results


# ──────────────────────────── 阶段 3：轮询解析 ────────────────────────────


def poll_parse(base_url: str, results: list[dict], interval: float,
               timeout: float) -> None:
    """轮询所有已上传简历的解析状态，写回 parse_status / parse_ms。"""
    pending = {r["resume_id"]: r for r in results if r["upload_ok"] and r["resume_id"]}
    if not pending:
        print("[解析] 没有成功上传的简历，跳过")
        return

    print(f"[解析] 轮询 {len(pending)} 份简历的解析状态（间隔 {interval}s，上限 {timeout}s）...")
    t0 = time.perf_counter()
    while pending and (time.perf_counter() - t0) < timeout:
        for rid in list(pending.keys()):
            try:
                data = unwrap(api_get(base_url, f"/api/v1/resumes/{rid}/parse-status"))
            except Exception as e:
                print(f"  [解析] 查询 {rid} 失败（下轮重试）: {e}")
                continue
            status = data.get("parseStatus")
            if status in PARSE_TERMINAL:
                rec = pending.pop(rid)
                rec["parse_status"] = status
                rec["parse_error"] = data.get("parseError")
                # 从「该份上传返回」到「轮询发现终态」，是分辨率上界而非精确耗时
                rec["parse_ms"] = round(
                    (time.perf_counter() - rec.get("_t_done", t0)) * 1000, 1)

        if pending:
            done = len(results) - len(pending)
            print(f"  [解析] 已完成 {sum(1 for r in results if r.get('parse_status') in PARSE_TERMINAL)}"
                  f"/{len(results)}")
            time.sleep(interval)

    for rid, rec in pending.items():
        rec["parse_status"] = -1  # 超时未终结
        rec["parse_error"] = f"polling timeout after {timeout}s"

    succ = sum(1 for r in results if r.get("parse_status") == PARSE_SUCCESS)
    fail = sum(1 for r in results if r.get("parse_status") == PARSE_FAILED)
    tmo = sum(1 for r in results if r.get("parse_status") == -1)
    print(f"[解析] 全部终结：SUCCESS={succ} FAILED={fail} TIMEOUT={tmo}")


# ──────────────────────────── 阶段 4：批量 AI 筛选 ────────────────────────────


def run_batch_screen(base_url: str, results: list[dict], job_id: int,
                     interval: float, timeout: float) -> dict:
    ids = [r["resume_id"] for r in results if r["upload_ok"] and r["resume_id"]]
    if not ids:
        return {"submitted": 0, "error": "no uploaded resumes"}

    print(f"[批筛] 提交 {len(ids)} 份简历到 POST /api/v1/resumes/batch-screen ...")
    try:
        data = unwrap(api_post_json(base_url, "/api/v1/resumes/batch-screen",
                                    {"resumeIds": ids, "jobId": job_id}))
    except Exception as e:
        print(f"[批筛] 提交失败: {e}")
        return {"submitted": len(ids), "error": str(e)}

    task_id = data.get("taskId")
    print(f"[批筛] taskId={task_id}，开始轮询进度（间隔 {interval}s，上限 {timeout}s）...")

    t0 = time.perf_counter()
    last = {}
    while (time.perf_counter() - t0) < timeout:
        try:
            last = unwrap(api_get(base_url, f"/api/v1/resumes/batch-screen/{task_id}/progress"))
        except Exception as e:
            print(f"  [批筛] 查询进度失败（下轮重试）: {e}")
            time.sleep(interval)
            continue
        status = last.get("status")
        print(f"  [批筛] {status} {last.get('completed')}/{last.get('total')} "
              f"failed={last.get('failed')}")
        if status in ("COMPLETED", "FAILED", "NOT_FOUND"):
            break
        time.sleep(interval)

    wall = time.perf_counter() - t0
    completed = last.get("completed") or 0
    summary = {
        "task_id": task_id,
        "submitted": len(ids),
        "total": last.get("total"),
        "completed": completed,
        "failed": last.get("failed"),
        "status": last.get("status"),
        "failed_details": last.get("failedDetails") or [],
        "wall_seconds": round(wall, 2),
        "throughput_per_min": round(completed / wall * 60, 2) if wall > 0 else None,
        "avg_ms_per_resume": round(wall * 1000 / completed, 1) if completed else None,
    }
    print(f"[批筛] 完成：{completed}/{summary['total']}，墙钟 {wall:.1f}s，"
          f"吞吐 {summary['throughput_per_min']} 份/分钟")
    return summary


# ──────────────────────────── 汇总 / 落盘 ────────────────────────────


def percentile(values: list[float], p: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    if len(ordered) == 1:
        return round(ordered[0], 1)
    idx = (len(ordered) - 1) * p
    lo, hi = int(idx), min(int(idx) + 1, len(ordered) - 1)
    return round(ordered[lo] + (ordered[hi] - ordered[lo]) * (idx - lo), 1)


def build_summary(results: list[dict], screen: dict | None) -> dict:
    """客户端侧汇总。权威指标（解析耗时 / Token）由 report.py 查库产出。"""
    def stats(key: str, records: list[dict]) -> dict:
        vals = [r[key] for r in records if r.get(key) is not None]
        if not vals:
            return {"n": 0}
        return {
            "n": len(vals),
            "avg": round(statistics.mean(vals), 1),
            "min": round(min(vals), 1),
            "max": round(max(vals), 1),
            "p50": percentile(vals, 0.50),
            "p95": percentile(vals, 0.95),
            "p99": percentile(vals, 0.99),
        }

    uploaded = [r for r in results if r["upload_ok"]]
    by_cat: dict[str, dict] = {}
    for cat in sorted({r["category"] for r in results}):
        rows = [r for r in results if r["category"] == cat]
        good = [r for r in rows if not r["is_bad"]]
        bad = [r for r in rows if r["is_bad"]]
        by_cat[cat] = {
            "total": len(rows),
            "upload_ok": sum(1 for r in rows if r["upload_ok"]),
            "parse_success": sum(1 for r in rows if r.get("parse_status") == PARSE_SUCCESS),
            "parse_failed": sum(1 for r in rows if r.get("parse_status") == PARSE_FAILED),
            "good_parse_success": sum(1 for r in good if r.get("parse_status") == PARSE_SUCCESS),
            "good_total": len(good),
            "bad_total": len(bad),
            # 「被拒」= 上传期被校验拦下（rejected）或解析期标记失败（FAILED）。
            # 不能写成 not upload_ok —— 那会把网络故障、超时也当成正确的拦截。
            "bad_rejected": sum(1 for r in bad
                                if r.get("rejected") or r.get("parse_status") == PARSE_FAILED),
        }

    return {
        "upload_total": len(results),
        "upload_ok": len(uploaded),
        "upload_failed": len(results) - len(uploaded),
        "upload_rejected": sum(1 for r in results if r.get("rejected")),
        "parse_success": sum(1 for r in results if r.get("parse_status") == PARSE_SUCCESS),
        "parse_failed": sum(1 for r in results if r.get("parse_status") == PARSE_FAILED),
        "upload_ms": stats("upload_ms", results),
        "parse_ms_client_polling": stats("parse_ms", results),
        "by_category": by_cat,
        "batch_screen": screen,
    }


def main() -> int:
    ap = argparse.ArgumentParser(
        description="smart-recruit 简历解析/AI 筛选基准测试驱动",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    ap.add_argument("--base-url", default=DEFAULT_BASE_URL,
                    help=f"recruitment 服务地址（默认 {DEFAULT_BASE_URL}）")
    ap.add_argument("--manifest", default=str(DEFAULT_MANIFEST),
                    help="样本清单 manifest.json 路径")
    ap.add_argument("--job-id", type=int, default=None, help="关联职位 ID（缺省自动解析）")
    ap.add_argument("--concurrency", type=int, default=4, help="上传并发度（默认 4）")
    ap.add_argument("--limit", type=int, default=None, help="只跑前 N 份")
    ap.add_argument("--good-only", action="store_true", help="跳过坏样本")
    ap.add_argument("--bad-only", action="store_true", help="只跑坏样本（测容错）")
    ap.add_argument("--skip-screen", action="store_true", help="只测解析，不跑批筛")
    ap.add_argument("--poll-interval", type=float, default=2.0, help="轮询间隔秒（默认 2）")
    ap.add_argument("--parse-timeout", type=float, default=900, help="解析轮询上限秒（默认 900）")
    ap.add_argument("--screen-timeout", type=float, default=1800, help="批筛轮询上限秒（默认 1800）")
    ap.add_argument("--seed", type=int, default=None, help="打散样本顺序的随机种子")
    ap.add_argument("--run-name", default=None, help="本次运行的名称，写入结果文件")
    ap.add_argument("--out-dir", default=str(DEFAULT_RUNS_DIR), help="结果输出目录")
    ap.add_argument("--dry-run", action="store_true", help="只做连通性与清单校验")
    args = ap.parse_args()

    manifest_path = Path(args.manifest)
    if not manifest_path.exists():
        sys.exit(f"清单不存在：{manifest_path}\n请先运行：python gen_samples.py")
    samples = json.loads(manifest_path.read_text(encoding="utf-8"))

    if args.good_only:
        samples = [s for s in samples if not s["is_bad"]]
    if args.bad_only:
        samples = [s for s in samples if s["is_bad"]]
    if args.seed is not None:
        random.Random(args.seed).shuffle(samples)
    if args.limit:
        samples = samples[: args.limit]

    print("=" * 72)
    print(f"smart-recruit 基准测试  |  {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"  服务地址   : {args.base_url}")
    print(f"  样本清单   : {manifest_path}")
    print(f"  样本数量   : {len(samples)}（坏样本 {sum(1 for s in samples if s['is_bad'])}）")
    print(f"  并发度     : {args.concurrency}")
    print("=" * 72)

    # dry-run：只校验本地样本文件，不碰网络
    if args.dry_run:
        missing = [s["path"] for s in samples
                   if not (manifest_path.parent / s["path"]).exists()]
        print(f"[dry-run] 样本文件缺失 {len(missing)} 个")
        for m in missing[:10]:
            print(f"    - {m}")
        total_mb = sum(s.get("size_bytes") or 0 for s in samples) / 1024 / 1024
        print(f"[dry-run] 待上传 {len(samples)} 份，合计 {total_mb:.1f} MB")
        print("[dry-run] 结束，未上传任何文件。")
        return 0 if not missing else 1

    # 阶段 1：预检
    try:
        job_id = resolve_job_id(args.base_url, args.job_id)
        jobs = unwrap(api_get(args.base_url, "/api/v1/jobs?page=1&size=1"))
        print(f"[预检] 服务连通性 OK（职位总数 {jobs.get('total')}）")
    except ConnectionError as e:
        sys.exit(f"[预检] 连接失败：{e}\n请确认 recruitment 服务已启动（端口 8082），"
                 f"且 --base-url 指向正确；如需改地址用 --base-url 或环境变量 BENCH_BASE_URL。")
    except Exception as e:
        sys.exit(f"[预检] 失败：{type(e).__name__}: {e}")

    started_at = datetime.now(timezone.utc).astimezone()

    # 阶段 2：上传
    results = run_uploads(args.base_url, manifest_path.parent, samples,
                          job_id, args.concurrency)

    # 阶段 3：轮询解析
    poll_parse(args.base_url, results, args.poll_interval, args.parse_timeout)

    # 阶段 4：批筛
    screen = None
    if not args.skip_screen:
        screen = run_batch_screen(args.base_url, results, job_id,
                                  args.poll_interval, args.screen_timeout)

    # 阶段 5：落盘
    summary = build_summary(results, screen)
    finished_at = datetime.now(timezone.utc).astimezone()

    # 去掉内部用的 perf_counter 时间戳，不进结果文件
    for r in results:
        r.pop("_t_done", None)

    run = {
        "meta": {
            "run_name": args.run_name,
            "started_at": started_at.isoformat(),
            "finished_at": finished_at.isoformat(),
            "duration_seconds": round((finished_at - started_at).total_seconds(), 1),
            "base_url": args.base_url,
            "job_id": job_id,
            "concurrency": args.concurrency,
            "sample_count": len(samples),
            "good_only": args.good_only,
            "bad_only": args.bad_only,
            "skip_screen": args.skip_screen,
            "manifest": str(manifest_path),
            "note": "parse_ms_client_polling 是轮询上界；权威解析耗时/Token 见 ai_resume_parse_log（用 report.py 查库）",
        },
        "summary": summary,
        "resumes": results,
    }

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    stamp = started_at.strftime("%Y%m%d_%H%M%S")
    out_path = out_dir / f"run_{stamp}.json"
    out_path.write_text(json.dumps(run, ensure_ascii=False, indent=2), encoding="utf-8")

    print("=" * 72)
    print("客户端侧汇总（权威指标请跑 report.py 查库）")
    print(f"  上传成功     : {summary['upload_ok']}/{summary['upload_total']}")
    if summary.get("upload_rejected"):
        print(f"  上传被校验拦下 : {summary['upload_rejected']}（预期内的拒绝，已计入异常样本拦截）")
    print(f"  解析成功     : {summary['parse_success']}  失败 {summary['parse_failed']}")
    up = summary["upload_ms"]
    if up.get("n"):
        print(f"  上传耗时 P50/P95 : {up['p50']}ms / {up['p95']}ms")
    pc = summary["parse_ms_client_polling"]
    if pc.get("n"):
        print(f"  解析耗时(轮询上界) P50/P95 : {pc['p50']}ms / {pc['p95']}ms")
    if screen and screen.get("throughput_per_min"):
        print(f"  批筛吞吐     : {screen['throughput_per_min']} 份/分钟"
              f"（{screen['completed']}/{screen['total']}）")
    print(f"\n结果已写入: {out_path}")
    print(f"下一步: python report.py --run {out_path.name}")
    print("=" * 72)
    return 0


if __name__ == "__main__":
    # Windows 控制台默认 GBK，中文输出会乱码；尽量切到 UTF-8
    # （若仍乱码，先在终端执行 chcp 65001）
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\n已中断。")
        sys.exit(130)
