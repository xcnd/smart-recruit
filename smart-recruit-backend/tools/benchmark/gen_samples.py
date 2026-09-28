#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""简历评测样本生成器。

生成 4 类共 60 份样本，覆盖简历解析的三条通道，并混入「坏样本」用于触发
异常路径（人工复核、截断、失败）：

  text_pdf/     20 份  文本型 PDF（规则引擎 + LLM 增强通道）
  docx/         20 份  DOCX，含表格版与双栏版（解析鲁棒性）
  image/        12 份  JPG/PNG（视觉通道）
  scanned_pdf/   8 份  扫描件 PDF，无文本层（视觉通道 + 多页截断）

其中 8 份为坏样本，在 manifest.json 中标记 is_bad=true 与 bad_reason。

用法：
    python gen_samples.py
    python gen_samples.py --out ../../data/benchmark/samples --seed 42

产出：
    <out>/<类别>/*.{pdf,docx,jpg,png}
    <out>/manifest.json   每份样本的期望元数据，供统计脚本比对
"""

import argparse
import json
import random
import sys
from pathlib import Path

# ---------------------------------------------------------------------------
# 内容素材
# ---------------------------------------------------------------------------

SURNAMES = list("王李张刘陈杨黄赵周吴徐孙马朱胡林郭何高罗")
GIVEN_NAMES = [
    "伟", "芳", "娜", "敏", "静", "磊", "洋", "勇", "艳", "杰",
    "涛", "明", "超", "秀英", "霞", "平", "刚", "桂英", "文", "辉",
    "子轩", "雨桐", "浩然", "思远", "梓涵", "博文", "嘉懿", "若曦",
]

CITIES = ["北京", "上海", "深圳", "杭州", "广州", "成都", "南京", "武汉", "西安", "苏州"]
HOUSEHOLDS = ["山东济南", "河南郑州", "河北石家庄", "湖南长沙", "四川成都", "安徽合肥",
              "江西南昌", "陕西西安", "福建福州", "湖北武汉"]

SCHOOLS = [
    ("清华大学", "计算机科学与技术", "硕士"),
    ("北京邮电大学", "软件工程", "硕士"),
    ("华中科技大学", "计算机技术", "硕士"),
    ("西安电子科技大学", "信息安全", "本科"),
    ("郑州大学", "计算机科学与技术", "本科"),
    ("杭州电子科技大学", "软件工程", "本科"),
    ("南京理工大学", "网络工程", "本科"),
    ("西南交通大学", "通信工程", "硕士"),
    ("山东大学", "电子信息工程", "本科"),
    ("合肥工业大学", "计算机应用技术", "本科"),
]

# 岗位画像：期望职位 / 技能池 / 当前职位 / 项目主题
ROLES = [
    {
        "title": "Java后端开发工程师",
        "skills": ["Java", "Spring Boot", "Spring Cloud", "MyBatis", "MySQL", "Redis",
                   "Kafka", "Docker", "Kubernetes", "微服务"],
        "current": "高级后端开发工程师",
        "projects": ["订单中心重构", "支付网关对接", "用户增长中台", "消息推送平台"],
        "duties": [
            "负责订单核心链路的服务拆分与接口设计，支撑日均百万级订单",
            "基于 Redis 实现分布式锁与缓存预热，接口平均响应时间下降明显",
            "主导支付网关的多渠道对接，统一异常处理与对账流程",
            "搭建基于 Kafka 的异步消息链路，解耦下单与履约流程",
        ],
    },
    {
        "title": "前端开发工程师",
        "skills": ["JavaScript", "TypeScript", "Vue", "React", "Webpack", "Vite",
                   "Node.js", "CSS", "Element Plus", "小程序"],
        "current": "前端开发工程师",
        "projects": ["运营后台重构", "移动端商城", "数据可视化大屏", "组件库建设"],
        "duties": [
            "负责运营后台的架构升级，从 Vue2 迁移到 Vue3 + TypeScript",
            "封装公司级组件库，沉淀 30+ 通用组件，降低重复开发成本",
            "优化首屏加载，通过路由懒加载与资源分包改善性能指标",
            "对接可视化大屏需求，使用 ECharts 完成多维度数据展示",
        ],
    },
    {
        "title": "算法工程师",
        "skills": ["Python", "PyTorch", "TensorFlow", "机器学习", "深度学习", "NLP",
                   "大模型", "SQL", "Spark", "推荐系统"],
        "current": "算法工程师",
        "projects": ["推荐系统冷启动", "文本分类模型优化", "搜索排序策略", "用户画像构建"],
        "duties": [
            "负责推荐系统的召回与排序模型迭代，优化线上点击率指标",
            "基于 BERT 构建文本分类模型，F1 相比规则方案有较大提升",
            "搭建特征工程流水线，支撑每日全量特征更新",
            "与工程团队协作完成模型上线与服务化部署",
        ],
    },
    {
        "title": "测试开发工程师",
        "skills": ["Java", "Python", "Selenium", "JMeter", "Pytest", "接口测试",
                   "自动化测试", "CI/CD", "Jenkins", "MySQL"],
        "current": "测试开发工程师",
        "projects": ["自动化测试平台", "接口回归体系", "性能压测方案", "质量看板"],
        "duties": [
            "搭建接口自动化测试框架，覆盖核心业务链路，回归效率显著提升",
            "基于 JMeter 设计压测方案，定位多个性能瓶颈",
            "推动 CI 流程接入自动化用例，实现提交即验证",
            "建设质量数据看板，量化各版本缺陷分布",
        ],
    },
    {
        "title": "产品经理",
        "skills": ["需求分析", "原型设计", "Axure", "数据分析", "项目管理", "用户研究",
                   "SQL", "PRD", "B端产品", "增长"],
        "current": "产品经理",
        "projects": ["会员体系设计", "商家后台改版", "增长实验平台", "客服工单系统"],
        "duties": [
            "负责会员体系的从 0 到 1 设计，完成 PRD 撰写与跨部门推动落地",
            "主导商家后台改版，通过用户访谈梳理核心痛点并排定优先级",
            "设计并跟进 A/B 实验，依据数据结论迭代产品方案",
            "协调研发、设计、运营资源，把控版本节奏与交付质量",
        ],
    },
    {
        "title": "数据分析师",
        "skills": ["SQL", "Python", "Excel", "Tableau", "Hive", "数据建模",
                   "指标体系", "A/B测试", "统计", "BI"],
        "current": "数据分析师",
        "projects": ["经营指标体系建设", "用户留存分析", "投放效果归因", "数据看板搭建"],
        "duties": [
            "搭建业务指标体系，统一口径并输出周期性经营分析报告",
            "通过留存分析定位流失关键节点，输出策略建议并跟进落地",
            "负责投放渠道效果归因，优化预算分配方案",
            "使用 Tableau 搭建自助分析看板，降低业务方取数成本",
        ],
    },
    {
        "title": "运维开发工程师",
        "skills": ["Linux", "Shell", "Python", "Docker", "Kubernetes", "Prometheus",
                   "Ansible", "Nginx", "CI/CD", "云原生"],
        "current": "运维开发工程师",
        "projects": ["容器化改造", "监控告警体系", "发布流水线", "成本优化"],
        "duties": [
            "推动核心服务容器化改造，完成 Kubernetes 集群搭建与迁移",
            "基于 Prometheus + Grafana 建设监控告警体系，缩短故障发现时间",
            "设计并实现自动化发布流水线，支持灰度与快速回滚",
            "梳理云资源使用情况，通过规格优化降低月度成本",
        ],
    },
    {
        "title": "Android开发工程师",
        "skills": ["Java", "Kotlin", "Android", "Jetpack", "Retrofit", "性能优化",
                   "组件化", "Flutter", "Gradle", "MVVM"],
        "current": "Android开发工程师",
        "projects": ["App 性能优化", "组件化架构改造", "Flutter 混合开发", "推送体系建设"],
        "duties": [
            "负责 App 启动速度与内存占用优化，提升冷启动表现",
            "主导组件化架构改造，明确模块边界与通信规范",
            "接入 Flutter 完成部分页面的混合开发",
            "优化推送链路，提升消息到达率",
        ],
    },
]

# 坏样本定义：(文件名, 类别, 说明)
BAD_SAMPLES = [
    ("bad_01_no_name", "text_pdf", "有完整内容但缺少姓名，用于验证缺失姓名的处理"),
    ("bad_02_garbled", "text_pdf", "正文为乱码，用于验证解析鲁棒性与失败标记"),
    ("bad_03_empty", "text_pdf", "空白页无任何文本，用于验证空文本通道切换"),
    ("bad_04_huge_image", "image", "图片体积超过 10MB 上传上限（UPLOAD_MAX_SIZE_MB），应被上传校验拒绝"),
    ("bad_05_many_pages", "scanned_pdf", "扫描件共 5 页，超过 3 页上限，用于验证截断"),
    ("bad_06_long_text", "text_pdf", "正文约 20000 字，超过 15000 字符截断上限"),
    ("bad_07_docx_image_only", "docx", "DOCX 仅含图片无文字，抽取文本为空"),
    ("bad_08_wrong_format", "docx", "扩展名被允许但解析器不支持，用于验证失败路径"),
]

GARBLED = "锟斤拷烫烫烫锟斤拷屯屯屯锟斤拷" * 40


# ---------------------------------------------------------------------------
# 简历内容构造
# ---------------------------------------------------------------------------

def make_person(rng, role):
    return {
        "name": rng.choice(SURNAMES) + rng.choice(GIVEN_NAMES),
        "gender": rng.choice(["男", "女"]),
        "age": rng.randint(23, 38),
        "household": rng.choice(HOUSEHOLDS),
        "location": rng.choice(CITIES),
        "phone": "1%d%08d" % (rng.choice([3, 5, 7, 8, 9]), rng.randint(0, 99999999)),
        "email": "candidate%d@example.com" % rng.randint(1000, 9999),
        "school": rng.choice(SCHOOLS),
        "role": role,
        "years": rng.randint(1, 10),
        "expect_city": rng.choice(CITIES),
    }


def resume_text(p, rng):
    """生成简历纯文本（各格式共用同一份内容，保证可比性）。"""
    school, major, degree = p["school"]
    role = p["role"]
    skills = rng.sample(role["skills"], k=min(len(role["skills"]), rng.randint(6, 10)))
    duties = rng.sample(role["duties"], k=min(len(role["duties"]), rng.randint(3, 4)))
    projects = rng.sample(role["projects"], k=min(len(role["projects"]), 2))

    grad_year = 2026 - p["years"] - (2 if degree == "硕士" else 0)
    company = rng.choice(["某科技有限公司", "某网络科技公司", "某信息技术公司", "某数据服务公司"])

    lines = []
    lines.append("%s　%s　%d岁" % (p["name"], p["gender"], p["age"]))
    lines.append("电话：%s　邮箱：%s" % (p["phone"], p["email"]))
    lines.append("籍贯：%s　现居地：%s" % (p["household"], p["location"]))
    lines.append("求职意向：%s　期望城市：%s" % (role["title"], p["expect_city"]))
    lines.append("")
    lines.append("【教育背景】")
    lines.append("%d.%02d - %d.%02d　%s　%s　%s" % (
        grad_year - 4, rng.randint(1, 12), grad_year, 6, school, major, degree))
    lines.append("")
    lines.append("【工作经历】")
    lines.append("%d.%02d - 至今　%s　%s" % (
        grad_year, rng.randint(1, 12), company, role["current"]))
    for d in duties:
        lines.append("· %s" % d)
    lines.append("")
    lines.append("【项目经历】")
    for proj in projects:
        lines.append("%s（%d 个月）" % (proj, rng.randint(3, 12)))
        lines.append("· 负责该项目的核心模块设计与开发，按时完成交付并上线")
        lines.append("· 与产品、测试协作推进需求落地，上线后运行稳定")
    lines.append("")
    lines.append("【专业技能】")
    lines.append("　".join(skills))
    lines.append("")
    lines.append("【自我评价】")
    lines.append("工作踏实，沟通顺畅，具备较强的学习能力和问题定位能力，能独立承担模块级任务。")
    return "\n".join(lines), {"name": p["name"], "skills": skills,
                              "education": [school, major, degree],
                              "expect": role["title"]}


def text_pdf(path, text, font_name):
    from reportlab.lib.pagesizes import A4
    from reportlab.pdfbase import pdfmetrics
    from reportlab.pdfbase.cidfonts import UnicodeCIDFont
    from reportlab.pdfgen import canvas

    pdfmetrics.registerFont(UnicodeCIDFont(font_name))
    c = canvas.Canvas(str(path), pagesize=A4)
    width, height = A4
    c.setFont(font_name, 10.5)
    y = height - 50
    for line in text.split("\n"):
        if y < 50:
            c.showPage()
            c.setFont(font_name, 10.5)
            y = height - 50
        c.drawString(45, y, line)
        y -= 16
    c.save()


def docx_file(path, text, with_table=False, two_column=False):
    from docx import Document
    from docx.shared import Pt
    from docx.oxml.ns import qn

    doc = Document()
    style = doc.styles["Normal"]
    style.font.name = "微软雅黑"
    style.font.size = Pt(10.5)
    style.element.rPr.rFonts.set(qn("w:eastAsia"), "微软雅黑")

    if two_column:
        # 用两列表格模拟双栏简历排版
        table = doc.add_table(rows=1, cols=2)
        cells = table.rows[0].cells
        lines = text.split("\n")
        half = len(lines) // 2
        cells[0].text = "\n".join(lines[:half])
        cells[1].text = "\n".join(lines[half:])
    elif with_table:
        head, _, tail = text.partition("【工作经历】")
        doc.add_paragraph(head)
        doc.add_paragraph("【工作经历】")
        table = doc.add_table(rows=1, cols=2)
        table.style = "Table Grid"
        table.rows[0].cells[0].text = "职责"
        table.rows[0].cells[1].text = "内容"
        for line in tail.strip().split("\n"):
            if not line.strip():
                continue
            row = table.add_row()
            row.cells[0].text = "工作内容"
            row.cells[1].text = line.lstrip("·").strip()
    else:
        for line in text.split("\n"):
            doc.add_paragraph(line)
    doc.save(str(path))


def text_to_image(text, font_path, width=1240, max_height=1754):
    from PIL import Image, ImageDraw, ImageFont

    font = ImageFont.truetype(font_path, 22)
    line_h = 34
    lines = []
    for line in text.split("\n"):
        lines.append(line)
    height = min(max_height, 60 + line_h * len(lines))
    img = Image.new("RGB", (width, height), "white")
    draw = ImageDraw.Draw(img)
    y = 30
    for line in lines:
        if y + line_h > height:
            break
        draw.text((40, y), line, fill="black", font=font)
        y += line_h
    return img


def main():
    ap = argparse.ArgumentParser(description="生成简历评测样本")
    ap.add_argument("--out", default=str(Path(__file__).resolve().parent.parent.parent
                                         / "data" / "benchmark" / "samples"))
    ap.add_argument("--seed", type=int, default=42)
    args = ap.parse_args()

    out = Path(args.out).resolve()
    rng = random.Random(args.seed)

    sys.stdout.reconfigure(encoding="utf-8")
    font_path = Path("C:/Windows/Fonts/msyh.ttc")
    if not font_path.exists():
        sys.exit("找不到中文字体 C:/Windows/Fonts/msyh.ttc，无法生成图片样本")

    cats = ["text_pdf", "docx", "image", "scanned_pdf"]
    for c in cats:
        (out / c).mkdir(parents=True, exist_ok=True)

    manifest = []
    idx = 0

    def record(path, category, person, meta, is_bad=False, bad_reason=None, pages=1):
        manifest.append({
            "id": idx,
            "path": str(path.relative_to(out)).replace("\\", "/"),
            "category": category,
            "is_bad": is_bad,
            "bad_reason": bad_reason,
            "pages": pages,
            "expected_name": person.get("name") if person else None,
            "expected_skills": meta.get("skills", []) if meta else [],
            "expect_position": meta.get("expect") if meta else None,
            "size_bytes": path.stat().st_size if path.exists() else 0,
        })

    # ── 正常样本（配额 = 该类总数 - 该类坏样本数，合计 52 + 8 坏样本 = 60）──
    bad_per_cat = {}
    for _, c, _ in BAD_SAMPLES:
        bad_per_cat[c] = bad_per_cat.get(c, 0) + 1
    plan = [(c, total - bad_per_cat.get(c, 0)) for c, total in
            [("text_pdf", 20), ("docx", 20), ("image", 12), ("scanned_pdf", 8)]]
    for category, total in plan:
        for i in range(total):
            idx += 1
            role = ROLES[(idx - 1) % len(ROLES)]
            person = make_person(rng, role)
            text, meta = resume_text(person, rng)

            if category == "text_pdf":
                name = "pdf_%02d_%s" % (i + 1, person["name"])
                path = out / "text_pdf" / (name + ".pdf")
                text_pdf(path, text, "STSong-Light")
                record(path, category, person, meta)

            elif category == "docx":
                mode = i % 3  # 0=普通 1=表格 2=双栏
                name = "docx_%02d_%s" % (i + 1, person["name"])
                path = out / "docx" / (name + ".docx")
                docx_file(path, text, with_table=(mode == 1), two_column=(mode == 2))
                record(path, category, person, meta)

            elif category == "image":
                name = "img_%02d_%s" % (i + 1, person["name"])
                img = text_to_image(text, str(font_path))
                if i % 2 == 0:
                    path = out / "image" / (name + ".jpg")
                    img.save(str(path), "JPEG", quality=88)
                else:
                    path = out / "image" / (name + ".png")
                    img.save(str(path), "PNG")
                record(path, category, person, meta)

            else:  # scanned_pdf：先把文本渲染成图，再包成无文本层的 PDF
                name = "scan_%02d_%s" % (i + 1, person["name"])
                img = text_to_image(text, str(font_path))
                path = out / "scanned_pdf" / (name + ".pdf")
                img.save(str(path), "PDF", resolution=150)
                record(path, category, person, meta)

    # ── 坏样本 ──────────────────────────────────────────────────────────────
    for name, category, reason in BAD_SAMPLES:
        idx += 1
        role = ROLES[idx % len(ROLES)]
        person = make_person(rng, role)
        text, meta = resume_text(person, rng)

        if name == "bad_01_no_name":
            path = out / category / (name + ".pdf")
            body = "\n".join(l for l in text.split("\n") if person["name"] not in l)
            text_pdf(path, body, "STSong-Light")
            record(path, category, None, meta, True, reason)

        elif name == "bad_02_garbled":
            path = out / category / (name + ".pdf")
            text_pdf(path, GARBLED, "STSong-Light")
            record(path, category, None, {}, True, reason)

        elif name == "bad_03_empty":
            path = out / category / (name + ".pdf")
            text_pdf(path, "", "STSong-Light")
            record(path, category, None, {}, True, reason)

        elif name == "bad_04_huge_image":
            import os as _os
            from PIL import Image
            path = out / category / (name + ".jpg")
            # 彩色噪点图，几乎不可压缩，保证体积超过 5MB 上限
            side = 2400
            noise = Image.frombytes("RGB", (side, side), _os.urandom(side * side * 3))
            noise.save(str(path), "JPEG", quality=100)
            record(path, category, None, {}, True, reason)

        elif name == "bad_05_many_pages":
            imgs = [text_to_image(text + "\n第 %d 页" % (i + 1), str(font_path))
                    for i in range(5)]
            path = out / category / (name + ".pdf")
            imgs[0].save(str(path), "PDF", resolution=150, save_all=True,
                         append_images=imgs[1:])
            record(path, category, person, meta, True, reason, pages=5)

        elif name == "bad_06_long_text":
            path = out / category / (name + ".pdf")
            padded = text + ("\n" + "\n".join(role["duties"] * 60)) * 4
            text_pdf(path, padded, "STSong-Light")
            record(path, category, person, meta, True, reason)

        elif name == "bad_07_docx_image_only":
            from docx import Document
            from docx.shared import Inches
            tmp_img = out / category / "_tmp_embed.png"
            text_to_image(text, str(font_path)).save(str(tmp_img))
            path = out / category / (name + ".docx")
            doc = Document()
            doc.add_picture(str(tmp_img), width=Inches(6))
            doc.save(str(path))
            tmp_img.unlink()
            record(path, category, None, {}, True, reason)

        else:  # bad_08_wrong_format：扩展名在允许列表内，但解析器不支持
            path = out / category / (name + ".xlsx")
            path.write_bytes(b"PK\x03\x04" + b"\x00" * 512)
            record(path, category, None, {}, True, reason)

    (out / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")

    # ── 汇总 ────────────────────────────────────────────────────────────────
    print("样本输出目录: %s" % out)
    print("%-14s %6s %6s" % ("类别", "总数", "坏样本"))
    for c in cats:
        rows = [m for m in manifest if m["category"] == c]
        print("%-14s %6d %6d" % (c, len(rows), sum(1 for m in rows if m["is_bad"])))
    total_mb = sum(m["size_bytes"] for m in manifest) / 1024 / 1024
    print("%-14s %6d %6d  (%.1f MB)" % ("合计", len(manifest),
                                        sum(1 for m in manifest if m["is_bad"]), total_mb))
    print("\n清单: %s" % (out / "manifest.json"))


if __name__ == "__main__":
    main()
