#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基准测试结果统计 —— 直接查库出「可写进简历的真实数字」。

数据来源（全部来自数据库，不靠日志抓取）：
  smart_recruit_ai.ai_resume_parse_log         解析耗时 / Token / 模型（权威口径）
  smart_recruit_ai.ai_agent_task               筛选任务耗时
  smart_recruit_recruitment.rec_resume         解析状态 / 匹配分
  smart_recruit_recruitment.rec_ai_screening_result  筛选来源 LLM/HEURISTIC、建议

连接方式：调用本机 mysql 客户端（已确认存在），无需 pip 装 pymysql。
若 mysql 不在 PATH，用 --mysql 指定 exe 路径。

用法：
  python report.py --run run_20260921_193900.json
  python report.py --resume-ids 1001,1002,1003        # 不依赖 run 文件
  python report.py --run run_xxx.json --no-screen     # run 里没跑批筛
  python report.py --run run_xxx.json --out report.md
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import statistics
import subprocess
import sys
from datetime import datetime, timedelta
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
BACKEND_ROOT = SCRIPT_DIR.parent.parent
DEFAULT_RUNS_DIR = BACKEND_ROOT / "data" / "benchmark" / "runs"

# ─── 「通道」口径：以样本清单的 category 为准（这是唯一的 ground truth）───
CHANNEL_OF_CATEGORY = {
    "text_pdf": "文本PDF（规则+LLM）",
    "docx": "Word（规则+LLM）",
    "image": "图片（视觉模型）",
    "scanned_pdf": "扫描件（无文本层）",
}

PARSE_STATUS_NAME = {0: "PENDING", 1: "PARSING", 2: "SUCCESS", 3: "FAILED", -1: "TIMEOUT"}

# 8 份坏样本的设计意图。必须分开统计：
#   reject  = 该被拦下（上传拒绝 或 标记解析失败转人工）
#   degrade = 该被优雅降级处理（截断/兼容后仍能解析出来），成功了才算达标
# 合成一个「异常样本识别率」会把两类混在一起，既低估了降级能力、
# 又会把「静默产出空壳却标记成功」洗成好结果。
BAD_INTENT = {
    "bad_01_no_name": "degrade",
    "bad_02_garbled": "reject",
    "bad_03_empty": "reject",
    "bad_04_huge_image": "reject",
    "bad_05_many_pages": "degrade",
    "bad_06_long_text": "degrade",
    "bad_07_docx_image_only": "reject",
    "bad_08_wrong_format": "reject",
}

# 结构化结果为空壳的判定阈值（字符数）。空壳 JSON 实测约 288 字符，
# 真实解析结果都在 900 字符以上；「解析成功但低于此值」按空壳统计。
EMPTY_CONTENT_MAX_CHARS = 320

# ─── 单价（美元 / 百万 Token）。请按厂商官网现价核对后修改！────────────────
# 这里只用于「费用估算」参考，报告里 Token 总量是库里的权威值，费用另行标注为估算。
PRICE_PER_MTOK = {
    # model 关键字（小写包含匹配） -> (输入单价, 输出单价)
    "qwen3-max": (1.2, 6.0),
    "qwen-max": (1.2, 6.0),
    "qwen-plus": (0.4, 1.2),
    "qwen-vl-max": (3.0, 9.0),
    "qwen-vl": (3.0, 9.0),
    "deepseek-chat": (0.27, 1.1),
    "deepseek-reasoner": (0.55, 2.19),
}
DEFAULT_PRICE = (1.0, 3.0)  # 命中不到时用的保守默认，报告里会标注


# ──────────────────────────── mysql 客户端封装 ────────────────────────────


class MySqlCli:
    def __init__(self, exe: str, host: str, port: int, user: str, password: str):
        self.exe = exe
        self.host = host
        self.port = port
        self.user = user
        self.password = password

    def query(self, sql: str) -> list[dict]:
        """执行 SQL，返回 list[dict]（NULL 转 None，数值列自动转型）。"""
        # 不加 --raw：让 mysql 把字段内的 \t / \n / \\ 转义掉，
        # 否则 parse_error（Java 堆栈）里的换行会把一行拆成多行、破坏行对齐。
        cmd = [
            self.exe, "--batch", "--skip-column-names",
            f"--host={self.host}", f"--port={self.port}", f"--user={self.user}",
            "--default-character-set=utf8mb4", "-e", sql,
        ]
        env = {**os.environ, "MYSQL_PWD": self.password}  # 避免密码出现在命令行
        try:
            proc = subprocess.run(cmd, capture_output=True, env=env, timeout=60)
        except FileNotFoundError:
            sys.exit(f"找不到 mysql 客户端：{self.exe}\n用 --mysql 指定完整路径，"
                     f"例如 --mysql 'D:/MySQL/MySQL Server 8.0/bin/mysql.exe'")
        except subprocess.TimeoutExpired:
            sys.exit("查询超时（60s），请检查数据库是否可用。")

        if proc.returncode != 0:
            err = proc.stderr.decode("utf-8", "replace").strip()
            sys.exit(f"SQL 执行失败：\n{err}\n\nSQL: {sql[:400]}")

        out = proc.stdout.decode("utf-8", "replace")
        if not out.strip():
            return []

        # --batch 用 \t 分隔，字段内的 \t/\n/\\ 已被 mysql 转义，因此每行恰好一列一字段。
        lines = out.replace("\r\n", "\n").split("\n")
        if lines and lines[-1] == "":
            lines.pop()
        ncols = len(lines[0].split("\t"))
        rows = []
        for line in lines:
            parts = line.split("\t")
            while len(parts) < ncols:
                parts.append("")
            rows.append([_coerce(_unescape(p)) for p in parts])
        return [dict(enumerate(r)) for r in rows]


_NUM_RE = re.compile(r"^-?\d+$")
_DEC_RE = re.compile(r"^-?\d+\.\d+$")


_ESCAPES = {"0": "\0", "t": "\t", "n": "\n", "\\": "\\"}


def _unescape(v: str) -> str:
    """还原 mysql --batch（非 --raw）的字段转义：\\0 \\t \\n \\\\ 以及 \\' 等原样保留。"""
    if "\\" not in v:
        return v
    out = []
    i = 0
    while i < len(v):
        if v[i] == "\\" and i + 1 < len(v):
            out.append(_ESCAPES.get(v[i + 1], v[i:i + 2]))
            i += 2
        else:
            out.append(v[i])
            i += 1
    return "".join(out)


def _coerce(v: str):
    if v == "NULL":
        return None
    if _NUM_RE.match(v):
        return int(v)
    if _DEC_RE.match(v):
        return float(v)
    return v


def col(rows: list[dict], i: int) -> list:
    return [r[i] for r in rows]


# ──────────────────────────── 统计工具 ────────────────────────────


def percentile(values: list[float], p: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    if len(ordered) == 1:
        return float(ordered[0])
    idx = (len(ordered) - 1) * p
    lo, hi = int(idx), min(int(idx) + 1, len(ordered) - 1)
    return ordered[lo] + (ordered[hi] - ordered[lo]) * (idx - lo)


def latency_block(values: list[float], unit: str = "ms") -> dict:
    if not values:
        return {}
    return {
        "n": len(values),
        "avg": round(statistics.mean(values), 1),
        "p50": round(percentile(values, 0.50), 1),
        "p95": round(percentile(values, 0.95), 1),
        "p99": round(percentile(values, 0.99), 1),
        "min": round(min(values), 1),
        "max": round(max(values), 1),
        "unit": unit,
    }


def price_for(model: str | None) -> tuple[float, float, bool]:
    """按模型名匹配单价，返回 (输入价, 输出价, 是否命中已知价)。"""
    m = (model or "").lower()
    for key, price in PRICE_PER_MTOK.items():
        if key in m:
            return price[0], price[1], True
    return DEFAULT_PRICE[0], DEFAULT_PRICE[1], False


def fmt(v, suffix: str = "") -> str:
    if v is None:
        return "-"
    if isinstance(v, float):
        return f"{v:g}{suffix}"
    return f"{v}{suffix}"


def s(v) -> str:
    """字符串列，None 转空。"""
    return "" if v is None else str(v)


# ──────────────────────────── 取数 ────────────────────────────


def load_run(run_path: Path | None, resume_ids_arg: str | None) -> tuple[dict, list[str]]:
    if resume_ids_arg:
        ids = [x.strip() for x in resume_ids_arg.split(",") if x.strip()]
        return ({"meta": {"note": "手动指定 resume_ids"}, "resumes": [], "summary": {}}, ids)

    if run_path is None:
        sys.exit("需要 --run <file> 或 --resume-ids <id,id,...>")

    if not run_path.exists():
        # 允许只传文件名，自动到 runs 目录找
        alt = DEFAULT_RUNS_DIR / run_path.name
        if alt.exists():
            run_path = alt
        else:
            sys.exit(f"run 文件不存在：{run_path}\n可用文件：\n  " +
                     "\n  ".join(p.name for p in sorted(DEFAULT_RUNS_DIR.glob("*.json"))))

    run = json.loads(run_path.read_text(encoding="utf-8"))
    ids = [r["resume_id"] for r in run.get("resumes", []) if r.get("resume_id")]
    run["_path"] = str(run_path)
    return run, ids


def fetch_parse_logs(db: MySqlCli, ids: list[str]) -> list[dict]:
    if not ids:
        return []
    sql = (
        "SELECT resume_id, engine, model, input_tokens, output_tokens, total_tokens, "
        "duration_ms, status, IFNULL(error_msg,''), create_time "
        "FROM smart_recruit_ai.ai_resume_parse_log "
        f"WHERE resume_id IN ({','.join(ids)});"
    )
    rows = db.query(sql)
    keys = ["resume_id", "engine", "model", "input_tokens", "output_tokens",
            "total_tokens", "duration_ms", "status", "error_msg", "create_time"]
    return [dict(zip(keys, [r[i] for i in range(len(keys))])) for r in rows]


def fetch_resumes(db: MySqlCli, ids: list[str]) -> dict[str, dict]:
    if not ids:
        return {}
    sql = (
        "SELECT id, file_name, file_type, parse_status, IFNULL(parse_error,''), "
        "screening_status, ai_match_score, create_time, "
        "CHAR_LENGTH(IFNULL(parsed_content,'')) "
        "FROM smart_recruit_recruitment.rec_resume "
        f"WHERE id IN ({','.join(ids)});"
    )
    rows = db.query(sql)
    keys = ["id", "file_name", "file_type", "parse_status", "parse_error",
            "screening_status", "ai_match_score", "create_time", "content_len"]
    return {str(r[0]): dict(zip(keys, [r[i] for i in range(len(keys))])) for r in rows}


def fetch_screening(db: MySqlCli, ids: list[str]) -> dict[str, dict]:
    if not ids:
        return {}
    sql = (
        "SELECT resume_id, overall_score, IFNULL(suggestion,''), IFNULL(source,''), create_time "
        "FROM smart_recruit_recruitment.rec_ai_screening_result "
        f"WHERE resume_id IN ({','.join(ids)});"
    )
    rows = db.query(sql)
    keys = ["resume_id", "overall_score", "suggestion", "source", "create_time"]
    return {str(r[0]): dict(zip(keys, [r[i] for i in range(len(keys))])) for r in rows}


def fetch_agent_tasks(db: MySqlCli, since: datetime, until: datetime) -> list[dict]:
    """筛选类 Agent 任务耗时。ai_agent_task 无 resume_id，只能按时间窗 + 类型圈定。"""
    sql = (
        "SELECT agent_name, task_type, status, duration_ms, created_at "
        "FROM smart_recruit_ai.ai_agent_task "
        f"WHERE created_at BETWEEN '{since:%Y-%m-%d %H:%M:%S}' "
        f"AND '{until:%Y-%m-%d %H:%M:%S}' "
        "AND duration_ms IS NOT NULL;"
    )
    rows = db.query(sql)
    keys = ["agent_name", "task_type", "status", "duration_ms", "created_at"]
    return [dict(zip(keys, [r[i] for i in range(len(keys))])) for r in rows]


# ──────────────────────────── 汇总 ────────────────────────────


def build_stats(run: dict, ids: list[str], logs: list[dict],
                resumes: dict[str, dict], screening: dict[str, dict],
                tasks: list[dict]) -> dict:
    samples = {str(r["sample_id"]): r for r in run.get("resumes", [])}
    by_resume: dict[str, dict] = {i: {"sample": None, "logs": []} for i in ids}
    for rid, info in by_resume.items():
        info["resume"] = resumes.get(rid)
        info["screening"] = screening.get(rid)
    for log in logs:
        rid = str(log["resume_id"])
        if rid in by_resume:
            by_resume[rid]["logs"].append(log)
    for r in run.get("resumes", []):
        if r.get("resume_id"):
            by_resume[str(r["resume_id"])]["sample"] = r

    # ── 解析耗时 / Token（权威：ai_resume_parse_log）──
    ok_logs = [l for l in logs if l["status"] == 0]
    fail_logs = [l for l in logs if l["status"] != 0]

    by_model: dict[str, dict] = {}
    for l in logs:
        key = f"{s(l['engine'])} / {s(l['model'])}"
        by_model.setdefault(key, {"logs": []})["logs"].append(l)

    model_blocks = {}
    total_in = total_out = total_tok = 0
    cost_total = 0.0
    price_known_all = True
    for key, grp in sorted(by_model.items()):
        gl = grp["logs"]
        gl_ok = [l for l in gl if l["status"] == 0]
        lat = latency_block([l["duration_ms"] for l in gl_ok if l["duration_ms"] is not None])
        tin = sum(l["input_tokens"] or 0 for l in gl_ok)
        tout = sum(l["output_tokens"] or 0 for l in gl_ok)
        ttok = sum(l["total_tokens"] or 0 for l in gl_ok)
        total_in += tin
        total_out += tout
        total_tok += ttok
        model = gl[0].get("model")
        pin, pout, known = price_for(model)
        price_known_all = price_known_all and known
        cost = tin / 1e6 * pin + tout / 1e6 * pout
        cost_total += cost
        model_blocks[key] = {
            "calls": len(gl), "success": len(gl_ok), "failed": len(gl) - len(gl_ok),
            "latency": lat,
            "input_tokens": tin, "output_tokens": tout, "total_tokens": ttok,
            "avg_input_per_call": round(tin / len(gl_ok), 1) if gl_ok else None,
            "avg_output_per_call": round(tout / len(gl_ok), 1) if gl_ok else None,
            "avg_total_per_call": round(ttok / len(gl_ok), 1) if gl_ok else None,
            "cost_est_usd": round(cost, 4), "price_known": known,
        }

    # ── 规则命中率：只看「解析成功」+「有没有 LLM 调用记录」，不依赖清单 ──
    # 空壳「成功」不算规则命中：内容字节数与空白页等价，是静默失败而非规则引擎的产出。
    rules_only = 0
    llm_called = 0
    for info in by_resume.values():
        res = info.get("resume") or {}
        if len(info["logs"]) > 0:
            llm_called += 1
        elif res.get("parse_status") == 2 and (res.get("content_len") or 0) >= EMPTY_CONTENT_MAX_CHARS:
            rules_only += 1

    # ── 通道拆分（按样本 category 分组，必须有清单才准）──
    by_channel: dict[str, dict] = {}
    good_total = good_success = 0
    bad_total = bad_rejected = 0
    for rid, info in by_resume.items():
        sample = info.get("sample")
        if not sample:
            continue
        cat = sample.get("category", "unknown")
        ch = by_channel.setdefault(CHANNEL_OF_CATEGORY.get(cat, cat),
                                   {"total": 0, "parse_success": 0, "parse_failed": 0,
                                    "llm_called": 0, "rules_only": 0, "bad_total": 0,
                                    "bad_rejected": 0})
        ch["total"] += 1
        res = info.get("resume") or {}
        pstatus = res.get("parse_status")
        if pstatus == 2:
            ch["parse_success"] += 1
        elif pstatus == 3:
            ch["parse_failed"] += 1
        if len(info["logs"]) > 0:
            ch["llm_called"] += 1
        elif pstatus == 2 and (res.get("content_len") or 0) >= EMPTY_CONTENT_MAX_CHARS:
            ch["rules_only"] += 1
        if sample.get("is_bad"):
            ch["bad_total"] += 1
            if is_bad_intercepted(sample, pstatus):
                ch["bad_rejected"] += 1
        else:
            good_total += 1
            if pstatus == 2:
                good_success += 1

    # ── 异常样本逐条判定 ──
    # 必须覆盖清单里的全部坏样本：上传期就被校验拦下的那份没有 resume 行，
    # 只遍历 by_resume 会把它从分母里漏掉（旧口径 1/7 就是这么来的）。
    bad_detail: list[dict] = []
    bad_grp = {"reject": {"n": 0, "ok": 0}, "degrade": {"n": 0, "ok": 0}}
    for _, sample in sorted(samples.items(), key=lambda kv: kv[1].get("id") or 0):
        if not sample.get("is_bad"):
            continue
        rid = sample.get("resume_id")
        info = by_resume.get(str(rid)) if rid else None
        res = (info or {}).get("resume") or {}
        pstatus = res.get("parse_status")
        clen = res.get("content_len") or 0
        intent = BAD_INTENT.get(Path(sample["file"]).stem, "reject")
        entered = pstatus is not None

        if sample.get("rejected"):
            outcome, ok = "上传期被校验拦下（未进入解析）", True
        elif pstatus == 3:
            outcome, ok = "解析标记失败 → 转人工复核", True
        elif pstatus == 2 and clen < EMPTY_CONTENT_MAX_CHARS:
            outcome, ok = f"标记「成功」但内容为空壳（{clen} 字符）", False
        elif pstatus == 2:
            outcome, ok = f"解析成功（{clen} 字符）", intent == "degrade"
        elif pstatus == 1:
            outcome, ok = "超时，仍在解析中", False
        else:
            outcome, ok = "未进入解析链路", False

        bad_grp[intent]["n"] += 1
        if ok:
            bad_grp[intent]["ok"] += 1
        bad_detail.append({
            "file": sample["file"],
            "category": sample.get("category"),
            "reason": sample.get("bad_reason"),
            "intent": intent,
            "outcome": outcome,
            "ok": ok,
            "entered": entered,
            "error": ((sample.get("error") or sample.get("parse_error") or "")
                      .splitlines() or [""])[0],
        })

    # 全局坏样本数以清单为准：上传期就被校验拦下的那份没有 resume 行，
    # 只统计入库样本会把它从分母里漏掉（旧口径 7/59 就是这么来的）。
    bad_total = len(bad_detail)
    bad_rejected = bad_grp["reject"]["ok"]

    # ── 空壳成功：解析标记成功但结构化内容几乎是空的（应为 0）──
    empty_success = sum(
        1 for r in resumes.values()
        if r.get("parse_status") == 2 and (r.get("content_len") or 0) < EMPTY_CONTENT_MAX_CHARS)

    # ── 筛选 ──
    screen_src: dict[str, int] = {}
    screen_sugg: dict[str, int] = {}
    for info in by_resume.values():
        sc = info.get("screening")
        if not sc:
            continue
        screen_src[s(sc.get("source")) or "UNKNOWN"] = \
            screen_src.get(s(sc.get("source")) or "UNKNOWN", 0) + 1
        screen_sugg[s(sc.get("suggestion")) or "UNKNOWN"] = \
            screen_sugg.get(s(sc.get("suggestion")) or "UNKNOWN", 0) + 1

    screen_tasks = [t for t in tasks if t.get("task_type") == 1]
    screen_lat = latency_block([t["duration_ms"] for t in screen_tasks])

    return {
        "model_blocks": model_blocks,
        "tokens": {
            "input": total_in, "output": total_out, "total": total_tok,
            "cost_est_usd": round(cost_total, 4), "price_known_all": price_known_all,
        },
        "parse_latency": latency_block([l["duration_ms"] for l in ok_logs
                                        if l["duration_ms"] is not None]),
        "llm_calls": len(logs), "llm_ok": len(ok_logs), "llm_failed": len(fail_logs),
        "llm_errors": [s(l["error_msg"])[:160] for l in fail_logs if l["error_msg"]][:5],
        "channel": by_channel,
        "rules_only_success": rules_only,
        "llm_called_resumes": llm_called,
        "good_total": good_total, "good_success": good_success,
        "bad_total": bad_total, "bad_rejected": bad_rejected,
        "bad_detail": bad_detail, "bad_reject_grp": bad_grp["reject"],
        "bad_degrade_grp": bad_grp["degrade"], "empty_success": empty_success,
        "screening_source": screen_src, "screening_suggestion": screen_sugg,
        "screening_latency": screen_lat,
        "screening_task_count": len(screen_tasks),
        "resume_count": len(ids),
        # 评测集总量以清单为准（含上传期被拦下、没进库的那份）
        "sample_count": len(run.get("resumes", [])),
        "resumes_found": len(resumes),
        "parse_success_db": sum(1 for r in resumes.values() if r.get("parse_status") == 2),
        "parse_failed_db": sum(1 for r in resumes.values() if r.get("parse_status") == 3),
    }


def is_bad_intercepted(sample: dict, pstatus) -> bool:
    """坏样本是否按预期被拦下：上传期校验拒绝，或解析期标记失败转人工。

    <p>不能用 `not upload_ok` —— 那会把网络故障、超时也当成正确的拦截。</p>
    """
    return bool(sample.get("rejected")) or pstatus == 3


# ──────────────────────────── 报告渲染 ────────────────────────────


def pct(num: int, den: int) -> str:
    return f"{num / den * 100:.1f}%" if den else "-"


def render(stats: dict, run: dict) -> str:
    L: list[str] = []
    meta = run.get("meta", {})
    L.append("# smart-recruit 简历解析 / AI 筛选 基准测试报告")
    L.append("")
    L.append(f"- 生成时间：{datetime.now():%Y-%m-%d %H:%M:%S}")
    if meta.get("run_name"):
        L.append(f"- 运行名称：{meta['run_name']}")
    if meta.get("started_at"):
        L.append(f"- 采集窗口：{meta.get('started_at')} ~ {meta.get('finished_at')}")
    L.append(f"- 简历数：{stats['resume_count']}（库中命中 {stats['resumes_found']}）")
    L.append("")

    # ── 1. 解析结果 ──
    L.append("## 1. 解析结果")
    L.append("")
    L.append("| 指标 | 数值 |")
    L.append("|---|---|")
    L.append(f"| 解析成功 | {stats['parse_success_db']} / {stats['resume_count']}"
             f"（{pct(stats['parse_success_db'], stats['resume_count'])}）|")
    L.append(f"| 解析失败 | {stats['parse_failed_db']} |")
    if stats["good_total"]:
        L.append(f"| 正常样本解析成功率 | {stats['good_success']} / {stats['good_total']}"
                 f"（{pct(stats['good_success'], stats['good_total'])}）|")
    rg, dg = stats["bad_reject_grp"], stats["bad_degrade_grp"]
    if rg["n"]:
        L.append(f"| 异常样本·应拦截 → 已拦下 | {rg['ok']} / {rg['n']}"
                 f"（{pct(rg['ok'], rg['n'])}）|")
    if dg["n"]:
        L.append(f"| 异常样本·应降级 → 已正确处理 | {dg['ok']} / {dg['n']}"
                 f"（{pct(dg['ok'], dg['n'])}）|")
    L.append(f"| **标记成功但内容为空壳** | {stats['empty_success']}"
             f"（应为 0；空壳阈值 < {EMPTY_CONTENT_MAX_CHARS} 字符）|")
    L.append(f"| 触发 LLM 增强的简历 | {stats['llm_called_resumes']}"
             f"（{pct(stats['llm_called_resumes'], stats['resume_count'])}）|")
    if stats['rules_only_success']:
        L.append(f"| 纯规则引擎即成功（未调 LLM） | {stats['rules_only_success']}"
                 f"（{pct(stats['rules_only_success'], stats['resume_count'])}）|")
    L.append("")

    # ── 1.1 异常样本逐条结果 ──
    if stats["bad_detail"]:
        L.append("### 1.1 异常样本逐条结果")
        L.append("")
        L.append("| 样本 | 设计意图 | 预期 | 实际结果 | 达标 |")
        L.append("|---|---|---|---|---|")
        for d in stats["bad_detail"]:
            intent = "应拦截" if d["intent"] == "reject" else "应降级"
            expect = "拒绝 / 转人工" if d["intent"] == "reject" else "降级后仍解析出内容"
            mark = "是" if d["ok"] else "**否**"
            L.append(f"| `{d['file']}` | {intent} | {expect} | {d['outcome']} | {mark} |")
        L.append("")
        L.append("> 「应降级」指设计上不要求拒绝，能截断/兼容后正常解析出内容即达标"
                 "（如超长文本截断、多页扫描件只读前几页）；")
        L.append("> 「应拦截」指必须拒绝或转人工复核。两类混算一个比率会同时低估降级能力、"
                 "并掩盖「空壳被标记成功」这种静默失败。")
        L.append("")

    # ── 2. 各通道表现 ──
    if stats["channel"]:
        L.append("## 2. 各通道表现")
        L.append("")
        L.append("| 通道 | 样本 | 解析成功 | 解析失败 | 调用LLM | 纯规则命中 | 异常样本被拒 |")
        L.append("|---|---|---|---|---|---|---|")
        for name, c in stats["channel"].items():
            bad = f"{c['bad_rejected']}/{c['bad_total']}" if c["bad_total"] else "-"
            L.append(f"| {name} | {c['total']} | {c['parse_success']} | {c['parse_failed']} | "
                     f"{c['llm_called']} | {c['rules_only']} | {bad} |")
        L.append("")
        missing = [d for d in stats["bad_detail"] if not d["entered"]]
        if missing:
            L.append("> 注：上表只统计进入解析链路的样本。" + "、".join(
                f"`{d['file']}`" for d in missing)
                + " 未进入解析链路（原因见 1.1），故各通道样本数之和小于清单计划数。")
            L.append("")

    # ── 3. 耗时（权威：ai_resume_parse_log.duration_ms）──
    L.append("## 3. 解析耗时（来源：ai_resume_parse_log）")
    L.append("")
    pl = stats["parse_latency"]
    if pl:
        L.append("| 指标 | 耗时(ms) |")
        L.append("|---|---|")
        for k in ("n", "avg", "p50", "p95", "p99", "min", "max"):
            L.append(f"| {k} | {fmt(pl.get(k))} |")
        L.append("")
    else:
        L.append("_无 LLM 调用记录（可能全部由本地规则解析成功，或埋点未生效）_")
        L.append("")

    if stats["model_blocks"]:
        L.append("### 按模型拆分")
        L.append("")
        L.append("| 引擎 / 模型 | 调用 | 成功 | 失败 | P50(ms) | P95(ms) | 均输入Tok | 均输出Tok | 合计Tok | 估算费用($) |")
        L.append("|---|---|---|---|---|---|---|---|---|---|")
        for key, b in stats["model_blocks"].items():
            lat = b["latency"]
            flag = "" if b["price_known"] else " ⚠"
            L.append(f"| {key} | {b['calls']} | {b['success']} | {b['failed']} | "
                     f"{fmt(lat.get('p50'))} | {fmt(lat.get('p95'))} | "
                     f"{fmt(b['avg_input_per_call'])} | {fmt(b['avg_output_per_call'])} | "
                     f"{b['total_tokens']} | {b['cost_est_usd']}{flag} |")
        L.append("")

    # ── 4. Token ──
    t = stats["tokens"]
    if t["total"] or t["input"]:
        L.append("## 4. Token 消耗（来源：ai_resume_parse_log）")
        L.append("")
        L.append("| 指标 | 数值 |")
        L.append("|---|---|")
        L.append(f"| 输入 Token 合计 | {t['input']:,} |")
        L.append(f"| 输出 Token 合计 | {t['output']:,} |")
        L.append(f"| Token 合计 | {t['total']:,} |")
        # Token 合计只来自「成功产生 LLM 调用」的那些解析，除数必须用调用数而非简历数
        if stats["llm_ok"]:
            L.append(f"| 单次解析均值 | {round(t['total'] / max(stats['llm_ok'], 1)):,} |")
        L.append(f"| 估算费用（美元） | {t['cost_est_usd']} |")
        L.append(f"| 估算费用（人民币，@7.2） | {round(t['cost_est_usd'] * 7.2, 3)} |")
        L.append("")
        L.append(f"> 费用为**估算**（单价表见 report.py 顶部 `PRICE_PER_MTOK`，需按厂商官网现价核对）。"
                 f"Token 数量本身来自调用返回的真实 usage。"
                 f"{'部分模型未匹配到单价，已用默认价，注意核对。' if not t['price_known_all'] else ''}")
        L.append("")

    # ── 5. 筛选 ──
    if stats["screening_source"] or stats["screening_suggestion"]:
        L.append("## 5. AI 筛选")
        L.append("")
        L.append("| 指标 | 数值 |")
        L.append("|---|---|")
        for k, v in sorted(stats["screening_source"].items()):
            L.append(f"| 评分来源 {k} | {v} |")
        llm_ct = stats["screening_source"].get("LLM", 0)
        heur_ct = stats["screening_source"].get("HEURISTIC", 0)
        if llm_ct + heur_ct:
            L.append(f"| LLM 降级到启发式比例 | {pct(heur_ct, llm_ct + heur_ct)} |")
        for k, v in sorted(stats["screening_suggestion"].items()):
            L.append(f"| 建议 {k} | {v} |")
        sl = stats["screening_latency"]
        if sl:
            L.append(f"| 单份筛选耗时 P50 / P95 (ms) | {fmt(sl.get('p50'))} / {fmt(sl.get('p95'))} |")
            L.append(f"| 筛选任务数（ai_agent_task） | {stats['screening_task_count']} |")
        L.append("")

    # ── 6. 批筛吞吐（来自 run 文件，客户端观测）──
    bs = (run.get("summary") or {}).get("batch_screen")
    if bs:
        L.append("## 6. 批量筛选吞吐（客户端观测）")
        L.append("")
        L.append("| 指标 | 数值 |")
        L.append("|---|---|")
        L.append(f"| 提交份数 | {bs.get('submitted')} |")
        L.append(f"| 完成 / 总数 | {bs.get('completed')} / {bs.get('total')} |")
        L.append(f"| 失败 | {bs.get('failed')} |")
        L.append(f"| 墙钟耗时 | {bs.get('wall_seconds')} s |")
        L.append(f"| 吞吐 | {bs.get('throughput_per_min')} 份/分钟 |")
        L.append(f"| 单份平均 | {bs.get('avg_ms_per_resume')} ms |")
        L.append("")

    # ── 7. 可写进简历的口径 ──
    L.append("## 7. 可直接使用的口径（面试用）")
    L.append("")
    for line in resume_bullets(stats, run):
        L.append(f"- {line}")
    L.append("")
    L.append("---")
    L.append("")
    L.append("**口径说明（被追问时用）**")
    L.append("")
    L.append(f"- 样本为自建评测集 {stats['sample_count']} 份，覆盖文本 PDF / Word / 图片 / 扫描件"
             f"四类，其中 {stats['bad_total']} 份为故意构造的边界与异常样本。")
    L.append("- 解析耗时与 Token 均取自服务端埋点表 `ai_resume_parse_log`，为调用链路真实值，"
             "非客户端轮询估算。")
    L.append("- 费用为按公开单价的估算值；Token 数量为接口返回的真实 usage。")
    if bs:
        L.append(f"- 批筛吞吐为客户端观测的端到端墙钟（提交 → 全部完成），"
                 f"并发度 {meta.get('concurrency', '-')}，受本机与模型限流影响。")
    return "\n".join(L)


def resume_bullets(stats: dict, run: dict) -> list[str]:
    """把数字拼成能直接用的简历措辞。"""
    out = []
    pl = stats["parse_latency"]
    t = stats["tokens"]
    n = stats["resume_count"]
    sc = stats["sample_count"]

    if stats["good_total"] and stats["good_success"]:
        out.append(
            f"自建 {sc} 份简历评测集（含 {stats['bad_total']} 份异常样本），"
            f"正常样本解析成功率 {pct(stats['good_success'], stats['good_total'])}"
            f"（{stats['good_success']}/{stats['good_total']}）")
    rg, dg = stats["bad_reject_grp"], stats["bad_degrade_grp"]
    if rg["n"] or dg["n"]:
        out.append(
            f"异常样本分组达标：应拦截 {rg['ok']}/{rg['n']}（上传校验拒绝或标记失败转人工）、"
            f"应降级 {dg['ok']}/{dg['n']}（截断/兼容后仍解析出内容）")
    if stats["parse_success_db"]:
        out.append(f"解析标记成功但结构化内容为空壳的简历 {stats['empty_success']} 份"
                   f"（空壳阈值 < {EMPTY_CONTENT_MAX_CHARS} 字符）")
    if pl:
        out.append(f"解析链路（含大模型结构化）耗时 P50 {fmt(pl.get('p50'), 'ms')}、"
                   f"P95 {fmt(pl.get('p95'), 'ms')}")
    if stats["rules_only_success"]:
        out.append(f"本地规则引擎独立完成 {stats['rules_only_success']} 份解析"
                   f"（占比 {pct(stats['rules_only_success'], n)}），未产生大模型调用，"
                   f"有效降低 Token 成本")
    if t["total"]:
        calls = max(stats["llm_ok"], 1)
        out.append(f"单次解析平均消耗 {round(t['total'] / calls):,} Token"
                   f"（输入 {round(t['input'] / calls):,} / 输出 {round(t['output'] / calls):,}），"
                   f"全量 {calls} 次解析合计 {t['total']:,} Token，估算成本 {t['cost_est_usd']} 美元")
    src = stats["screening_source"]
    if src:
        llm_ct, heur_ct = src.get("LLM", 0), src.get("HEURISTIC", 0)
        if llm_ct + heur_ct:
            out.append(f"AI 筛选大模型调用占比 {pct(llm_ct, llm_ct + heur_ct)}，"
                       f"降级到启发式 {pct(heur_ct, llm_ct + heur_ct)}，"
                       f"保证大模型不可用时链路不中断")
    sl = stats["screening_latency"]
    bs = (run.get("summary") or {}).get("batch_screen") or {}
    if sl and bs.get("throughput_per_min"):
        out.append(f"批量筛选吞吐 {bs['throughput_per_min']} 份/分钟，"
                   f"单份筛选 P50 {fmt(sl.get('p50'), 'ms')}、P95 {fmt(sl.get('p95'), 'ms')}")
    return out or ["_数据不足，先跑一轮 run_benchmark.py 再生成_。"]


# ──────────────────────────── main ────────────────────────────


def main() -> int:
    ap = argparse.ArgumentParser(
        description="基准测试结果统计（直连 MySQL，无需 pip 依赖）")
    ap.add_argument("--run", type=Path, default=None, help="run_benchmark.py 产出的 run_*.json")
    ap.add_argument("--resume-ids", default=None, help="逗号分隔的简历 ID（替代 --run）")
    ap.add_argument("--mysql", default=shutil.which("mysql") or "mysql", help="mysql 客户端路径")
    ap.add_argument("--host", default=os.environ.get("MYSQL_HOST", "localhost"))
    ap.add_argument("--port", type=int, default=int(os.environ.get("MYSQL_PORT", "3306")))
    ap.add_argument("--user", default=os.environ.get("MYSQL_USER", "root"))
    ap.add_argument("--password", default=os.environ.get("MYSQL_PASSWORD", "123456"))
    ap.add_argument("--window-pad", type=int, default=300,
                    help="筛选任务时间窗前后各放宽秒数（默认 300）")
    ap.add_argument("--no-screen", action="store_true", help="不查筛选相关表")
    ap.add_argument("--out", default=None, help="报告输出路径（默认 runs/report_<ts>.md）")
    args = ap.parse_args()

    run, ids = load_run(args.run, args.resume_ids)
    if not ids:
        sys.exit("没有拿到任何 resume_id（run 文件里 upload 可能全失败了）。")

    print(f"[查询] 简历 {len(ids)} 份，连接 {args.user}@{args.host}:{args.port} ...")
    db = MySqlCli(args.mysql, args.host, args.port, args.user, args.password)

    logs = fetch_parse_logs(db, ids)
    print(f"[查询] ai_resume_parse_log: {len(logs)} 条解析调用记录")
    resumes = fetch_resumes(db, ids)
    print(f"[查询] rec_resume: {len(resumes)} 条")

    screening: dict[str, dict] = {}
    tasks: list[dict] = []
    if not args.no_screen:
        screening = fetch_screening(db, ids)
        print(f"[查询] rec_ai_screening_result: {len(screening)} 条")
        meta = run.get("meta", {})
        started = meta.get("started_at")
        finished = meta.get("finished_at")
        if started and finished:
            since = datetime.fromisoformat(started).replace(tzinfo=None) - timedelta(seconds=args.window_pad)
            until = datetime.fromisoformat(finished).replace(tzinfo=None) + timedelta(seconds=args.window_pad)
        else:
            until = datetime.now()
            since = until - timedelta(hours=6)
        tasks = fetch_agent_tasks(db, since, until)
        print(f"[查询] ai_agent_task: {len(tasks)} 条（窗口 {since:%m-%d %H:%M} ~ {until:%m-%d %H:%M}）")

    stats = build_stats(run, ids, logs, resumes, screening, tasks)
    report = render(stats, run)

    out_path = Path(args.out) if args.out else (
        DEFAULT_RUNS_DIR / f"report_{datetime.now():%Y%m%d_%H%M%S}.md")
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(report, encoding="utf-8")

    print("\n" + report)
    print(f"\n报告已写入: {out_path}")
    return 0


if __name__ == "__main__":
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
