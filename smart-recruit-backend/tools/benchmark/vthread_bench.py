#!/usr/bin/env python3
"""虚拟线程 A/B 基准：并行化 AnalyticsServiceImpl.getOverview 的 8 个日汇总查询。

被测接口：GET {base_url}/api/v1/analytics/overview（talent 服务，默认 8085）

模式由**服务端**配置决定，不是请求参数：
    BENCH_THREAD_MODE=sequential | virtual | pool   （application.yml: benchmark.thread-mode）

所以两种模式需要**分别重启服务**再各跑一次，脚本用 --mode 只做标签记录。
为防贴错标签，每次运行都会打印带重启命令的横幅，compare 也要求两侧标签不同。

用法：
    # 1) 顺序档（现状基线）
    BENCH_THREAD_MODE=sequential <启动 talent>
    python vthread_bench.py run --mode sequential --n 300

    # 2) 虚拟线程档（重启 talent 时换 env）
    BENCH_THREAD_MODE=virtual <启动 talent>
    python vthread_bench.py run --mode virtual --n 300

    # 3) 出对比报告
    python vthread_bench.py compare

说明：LLM 洞察走 Redis 缓存 + 异步执行（AnalyticsServiceImpl.buildOverviewInsights），
不在被测延迟路径上；默认 20 次预热足够让首次 LLM 生成落到缓存里，不污染数据。
"""

from __future__ import annotations

import argparse
import json
import statistics
import subprocess
import sys
import time
from datetime import datetime
from http.client import HTTPConnection
from pathlib import Path
from urllib.parse import urlsplit

SCRIPT_DIR = Path(__file__).resolve().parent
BACKEND_ROOT = SCRIPT_DIR.parent.parent
DEFAULT_RUNS_DIR = BACKEND_ROOT / "data" / "benchmark" / "runs"

DEFAULT_BASE_URL = "http://localhost:8085"
OVERVIEW_PATH = "/api/v1/analytics/overview"

# 与 run_benchmark.py 一致：网关注入的用户信息头。talent 的 SecurityConfig 是 permitAll
# 且未启用 method security，@PreAuthorize 目前不生效，带上只为贴近真实调用链。
DEFAULT_HEADERS = {
    "X-User-Id": "1",
    "X-Username": "benchmark",
    "X-User-Role": "admin",
}

HTTP_TIMEOUT = 120
MODES = ("sequential", "virtual", "pool")


# ──────────────────────────── HTTP ────────────────────────────


class ProbeError(RuntimeError):
    """探针本身有问题（连不上 / 返回不是预期报文）——先怀疑探针，再怀疑代码。"""


def curl_smoke(base_url: str, path: str, headers: dict[str, str]) -> str:
    """用 curl 打一次探针，原样发送自定义头。

    探针有效性先于性能数据：curl 通的报文才算数（python 的 urllib 会规范化头名，
    历史上把「服务正常」误判成「服务缺陷」过）。
    """
    cmd = ["curl", "-sS", "-w", "\n---HTTP:%{http_code} TOTAL:%{time_total}---\n"]
    for k, v in headers.items():
        cmd += ["-H", f"{k}: {v}"]
    cmd.append(f"{base_url}{path}")
    proc = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8",
                          errors="replace", timeout=HTTP_TIMEOUT + 30)
    if proc.returncode != 0:
        raise ProbeError(
            f"curl 探针失败（exit={proc.returncode}）：{proc.stderr.strip()}\n"
            f"服务起了吗？talent 默认端口 8085。"
        )
    return proc.stdout


def path_slug(path: str) -> str:
    """路径末段作为归档文件名的一部分，如 /api/v1/analytics/funnel -> funnel。"""
    return path.rstrip("/").rsplit("/", 1)[-1] or "root"


def parse_smoke(out: str) -> tuple[int, float, dict]:
    body, _, meta = out.rpartition("---HTTP:")
    http_part, _, time_part = meta.partition(" TOTAL:")
    http_code = int(http_part.strip())
    total_s = float(time_part.replace("---", "").strip())
    if http_code != 200:
        raise ProbeError(f"探针 HTTP {http_code}，报文前 300 字：{body[:300]}")
    try:
        payload = json.loads(body)
    except json.JSONDecodeError as e:
        raise ProbeError(f"响应不是 JSON（{e}）：{body[:300]}") from e
    if not payload.get("success"):
        raise ProbeError(f"接口返回 success=false：{json.dumps(payload, ensure_ascii=False)[:300]}")
    return http_code, total_s * 1000.0, payload.get("data") or {}


def timed_get(conn: HTTPConnection, path: str, headers: dict[str, str]) -> float:
    """单个请求的端到端墙钟耗时（ms）。http.client + 显式 putheader，保连接复用。"""
    start = time.perf_counter()
    conn.request("GET", path, headers=headers)
    resp = conn.getresponse()
    body = resp.read()
    elapsed = (time.perf_counter() - start) * 1000.0
    if resp.status != 200:
        raise ProbeError(f"HTTP {resp.status}: {body[:200]!r}")
    return elapsed


def percentile(values: list[float], p: float) -> float | None:
    """线性插值分位，与 run_benchmark.py 同口径。"""
    if not values:
        return None
    ordered = sorted(values)
    if len(ordered) == 1:
        return round(ordered[0], 1)
    idx = (len(ordered) - 1) * p
    lo, hi = int(idx), min(int(idx) + 1, len(ordered) - 1)
    return round(ordered[lo] + (ordered[hi] - ordered[lo]) * (idx - lo), 1)


# ──────────────────────────── 运行 ────────────────────────────


def measure(base_url: str, path: str, n: int, warmup: int, concurrency: int,
            headers: dict[str, str]) -> dict:
    parts = urlsplit(base_url)
    host = parts.hostname or "localhost"
    port = parts.port or (443 if parts.scheme == "https" else 80)

    conn = HTTPConnection(host, port, timeout=HTTP_TIMEOUT)
    for _ in range(warmup):
        timed_get(conn, path, headers)

    latencies: list[float] = []
    if concurrency <= 1:
        window_start = time.perf_counter()
        for _ in range(n):
            latencies.append(timed_get(conn, path, headers))
        window_s = time.perf_counter() - window_start
    else:
        # 并发档：每线程各自一条连接，测的是高并发下的吞吐（虚拟线程的强项）
        import threading

        lock = threading.Lock()
        window_start = time.perf_counter()
        per = max(1, n // concurrency)

        def worker() -> None:
            c = HTTPConnection(host, port, timeout=HTTP_TIMEOUT)
            local: list[float] = []
            for _ in range(per):
                local.append(timed_get(c, path, headers))
            c.close()
            with lock:
                latencies.extend(local)

        threads = [threading.Thread(target=worker) for _ in range(concurrency)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        window_s = time.perf_counter() - window_start

    conn.close()

    return {
        "n": len(latencies),
        "warmup": warmup,
        "concurrency": concurrency,
        "window_s": round(window_s, 3),
        "throughput_per_s": round(len(latencies) / window_s, 2) if window_s > 0 else None,
        "p50_ms": percentile(latencies, 0.50),
        "p95_ms": percentile(latencies, 0.95),
        "p99_ms": percentile(latencies, 0.99),
        "min_ms": round(min(latencies), 1) if latencies else None,
        "max_ms": round(max(latencies), 1) if latencies else None,
        "mean_ms": round(statistics.fmean(latencies), 1) if latencies else None,
        "latencies_ms": [round(v, 1) for v in latencies],
    }


def cmd_run(args: argparse.Namespace) -> int:
    header = dict(DEFAULT_HEADERS)
    header.update(h.split(":", 1) for h in (args.header or []) if ":" in h)

    print("=" * 68)
    print(f"  本次标签 --mode {args.mode}")
    print(f"  服务端必须是同一档，重启 talent 时带上：")
    print(f"      BENCH_THREAD_MODE={args.mode}")
    print(f"  （标签只用于归档出图，脚本无法替你校验服务端真实档位）")
    print("=" * 68)

    path = args.path
    slug = path_slug(path)
    print(f"[1/2] curl 探针 {args.base_url}{path}")
    code, smoke_ms, _ = parse_smoke(curl_smoke(args.base_url, path, header))
    print(f"      HTTP {code}，success=true，curl 单次 {smoke_ms:.1f} ms")

    print(f"[2/2] 计时：{args.n} 次（预热 {args.warmup}，并发 {args.concurrency}）")
    result = measure(args.base_url, path, args.n, args.warmup,
                     args.concurrency, header)

    result.update({
        "mode": args.mode,
        "path": path,
        "base_url": args.base_url,
        "measured_at": datetime.now().isoformat(timespec="seconds"),
        "smoke_curl_ms": round(smoke_ms, 1),
    })

    DEFAULT_RUNS_DIR.mkdir(parents=True, exist_ok=True)
    out = DEFAULT_RUNS_DIR / f"vthread_{slug}_{args.mode}_c{args.concurrency}.json"
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")

    print("-" * 68)
    print(f"  P50 {result['p50_ms']} ms | P95 {result['p95_ms']} ms | "
          f"P99 {result['p99_ms']} ms")
    print(f"  吞吐 {result['throughput_per_s']} req/s（窗口 {result['window_s']}s）")
    print(f"  已写入 {out.relative_to(BACKEND_ROOT)}")
    return 0


# ──────────────────────────── 对比 ────────────────────────────


def cmd_compare(args: argparse.Namespace) -> int:
    files = sorted(DEFAULT_RUNS_DIR.glob("vthread_*.json"))
    if len(files) < 2:
        print(f"只有 {len(files)} 份结果，至少要两种模式。先分别跑：\n"
              f"  python {Path(__file__).name} run --mode sequential --n 300\n"
              f"  python {Path(__file__).name} run --mode virtual --n 300")
        return 1

    runs = [json.loads(f.read_text(encoding="utf-8")) for f in files]
    by_key: dict[tuple[str, str, int], dict] = {}
    for r in runs:
        by_key[(r.get("path", OVERVIEW_PATH), r["mode"], r["concurrency"])] = r

    lines = [
        "# 虚拟线程 A/B：AnalyticsServiceImpl.getOverview 日汇总并行化",
        "",
        f"- 生成时间：{datetime.now().isoformat(timespec='seconds')}",
        "- 被测接口：talent 服务（由 `benchmark.thread-mode` 切换档位）",
        f"- 每档样本数：{', '.join(str(r['n']) for r in runs)}",
        "",
    ]

    # 按「路径 × 并发」分组出表
    groups = sorted({(k[0], k[2]) for k in by_key})
    for path, c in groups:
        group = [by_key[k] for k in by_key if k[0] == path and k[2] == c]
        group.sort(key=lambda r: MODES.index(r["mode"]) if r["mode"] in MODES else 99)
        base = next((r for r in group if r["mode"] == "sequential"), None)

        lines.append(f"## `{path}`（并发 {c}）")
        lines.append("")
        lines.append("| 模式 | P50 (ms) | P95 (ms) | P99 (ms) | 吞吐 (req/s) | vs sequential |")
        lines.append("|---|---|---|---|---|---|")
        for r in group:
            delta = "—"
            if base and base is not r and base["p50_ms"] and r["p50_ms"]:
                d = (r["p50_ms"] - base["p50_ms"]) / base["p50_ms"] * 100
                delta = f"P50 {d:+.1f}%"
                if base["throughput_per_s"]:
                    dt = (r["throughput_per_s"] - base["throughput_per_s"]) / base["throughput_per_s"] * 100
                    delta += f" / 吞吐 {dt:+.1f}%"
            lines.append(
                f"| {r['mode']} | {r['p50_ms']} | {r['p95_ms']} | {r['p99_ms']} | "
                f"{r['throughput_per_s']} | {delta} |")
        lines.append("")

    lines += [
        "## 口径说明",
        "",
        "- 模式靠**重启服务**切换（`BENCH_THREAD_MODE`），脚本的 `--mode` 只是归档标签。",
        "- 耗时含连接复用下的端到端墙钟；预热已吸收首次 LLM 洞察生成（其本身异步且走 Redis 缓存）。",
        "- 固定池档线程数由 `benchmark.pool-size` 控制（默认 8）；本地库 Hikari 连接池上限 20。",
    ]

    out = DEFAULT_RUNS_DIR / "vthread_ab.md"
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))
    print(f"\n已写入 {out.relative_to(BACKEND_ROOT)}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_run = sub.add_parser("run", help="跑一档并归档 JSON")
    p_run.add_argument("--mode", required=True, choices=MODES,
                       help="服务端当前档位标签（需与 BENCH_THREAD_MODE 一致）")
    p_run.add_argument("--n", type=int, default=300, help="计时请求数（默认 300）")
    p_run.add_argument("--warmup", type=int, default=20, help="预热请求数（默认 20）")
    p_run.add_argument("--concurrency", type=int, default=1, help="并发线程数（默认 1）")
    p_run.add_argument("--base-url", default=DEFAULT_BASE_URL)
    p_run.add_argument("--path", default=OVERVIEW_PATH,
                       help=f"被测路径（默认 {OVERVIEW_PATH}）")
    p_run.add_argument("--header", action="append",
                       help="附加请求头 Key:Value，可重复")
    p_run.set_defaults(func=cmd_run)

    p_cmp = sub.add_parser("compare", help="读 runs/ 下所有 vthread_*.json 出对比报告")
    p_cmp.set_defaults(func=cmd_compare)

    args = parser.parse_args()
    try:
        return args.func(args)
    except ProbeError as e:
        print(f"\n[探针失败] {e}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
