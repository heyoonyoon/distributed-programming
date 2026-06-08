#!/usr/bin/env python3
# 편집 가능한 네이티브 PPTX 생성 (텍스트·도형이 PowerPoint 객체)
import os
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
DIA = os.path.join(HERE, "diagrams")
CL = os.path.join(DIA, "clusters")

NAVY = RGBColor(0x0B, 0x3D, 0x5C)
NAVY2 = RGBColor(0x0F, 0x2A, 0x43)
AMBER = RGBColor(0xF2, 0xA9, 0x00)
INK = RGBColor(0x1C, 0x2B, 0x36)
MUTED = RGBColor(0x5B, 0x71, 0x85)
PANEL = RGBColor(0xEE, 0xF3, 0xF7)
LINE = RGBColor(0xD7, 0xE1, 0xEA)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
OKBG = RGBColor(0xEE, 0xF6, 0xF4)
OK = RGBColor(0x1F, 0x9E, 0x8F)
AMBERBG = RGBColor(0xFF, 0xF3, 0xD6)
CUST = RGBColor(0x2C, 0x6F, 0x9C)
STAFF = RGBColor(0x1F, 0x9E, 0x8F)
SYS = RGBColor(0x0B, 0x3D, 0x5C)
LIGHT = RGBColor(0xF5, 0xF9, 0xFC)

FONT = "Apple SD Gothic Neo"
MONO = "Menlo"

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
BLANK = prs.slide_layouts[6]
SW = 13.333


def slide(bg=None):
    s = prs.slides.add_slide(BLANK)
    if bg is not None:
        f = s.background.fill
        f.solid()
        f.fore_color.rgb = bg
    return s


def _set(tf, runs, align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, wrap=True):
    tf.word_wrap = wrap
    tf.vertical_anchor = anchor
    first = True
    for line in runs:  # line = list of (text,size,bold,color,name)
        p = tf.paragraphs[0] if first else tf.add_paragraph()
        first = False
        p.alignment = align
        for (txt, size, bold, color, name) in line:
            r = p.add_run(); r.text = txt
            r.font.size = Pt(size); r.font.bold = bold
            r.font.color.rgb = color; r.font.name = name or FONT


def textbox(s, l, t, w, h, runs, align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP):
    tb = s.shapes.add_textbox(Inches(l), Inches(t), Inches(w), Inches(h))
    _set(tb.text_frame, runs, align, anchor)
    return tb


def box(s, l, t, w, h, fill, line, runs, align=PP_ALIGN.CENTER,
        anchor=MSO_ANCHOR.MIDDLE, line_w=1.0, shape=MSO_SHAPE.ROUNDED_RECTANGLE):
    sp = s.shapes.add_shape(shape, Inches(l), Inches(t), Inches(w), Inches(h))
    sp.shadow.inherit = False
    if fill is None:
        sp.fill.background()
    else:
        sp.fill.solid(); sp.fill.fore_color.rgb = fill
    if line is None:
        sp.line.fill.background()
    else:
        sp.line.color.rgb = line; sp.line.width = Pt(line_w)
    tf = sp.text_frame
    tf.margin_top = Pt(4); tf.margin_bottom = Pt(4)
    tf.margin_left = Pt(8); tf.margin_right = Pt(8)
    _set(tf, runs, align, anchor)
    return sp


def title(s, text):
    textbox(s, 0.6, 0.45, 12, 0.9, [[(text, 30, True, NAVY, FONT)]], anchor=MSO_ANCHOR.MIDDLE)
    ln = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.62), Inches(1.32), Inches(2.6), Inches(0.06))
    ln.shadow.inherit = False
    ln.fill.solid(); ln.fill.fore_color.rgb = AMBER; ln.line.fill.background()


def arrow(s, l, t):
    textbox(s, l, t, 0.5, 0.6, [[("→", 30, True, AMBER, FONT)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)


def verdict(s, text, t=6.2):
    textbox(s, 0.6, t, 12.13, 0.9, [[(text, 21, True, NAVY, FONT)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)


def img_fit(s, path, bl, bt, bw, bh):
    iw, ih = Image.open(path).size
    r = iw / ih
    w, h = bw, bw / r
    if h > bh:
        h, w = bh, bh * r
    l = bl + (bw - w) / 2
    t = bt + (bh - h) / 2
    s.shapes.add_picture(path, Inches(l), Inches(t), Inches(w), Inches(h))


# ---------- 1. 타이틀 ----------
s = slide(NAVY2)
textbox(s, 1.0, 2.7, 11.3, 1.3, [[("설계대로 구현하고, AI를 통제하다", 40, True, WHITE, FONT)]], anchor=MSO_ANCHOR.MIDDLE)
textbox(s, 1.0, 4.0, 11.3, 0.8, [[("의료·자동차 보험 시스템", 24, True, AMBER, FONT)]])

# ---------- 2. 세 주체 ----------
s = slide(); title(s, "누가 사용하나 — 세 주체")
personas = [("고객", CUST, "보험에 가입하고\n청구·납부한다"),
            ("보험사 직원", STAFF, "가입과 지급을\n심사한다"),
            ("시스템", SYS, "계약·고지서·지급을\n자동 처리한다")]
xs = [1.7, 5.6, 9.5]
for (name, col, role), x in zip(personas, xs):
    box(s, x, 2.0, 2.1, 2.1, col, None, [[(name.split()[0] if name == "보험사 직원" else name, 18, True, WHITE, FONT)]], shape=MSO_SHAPE.OVAL)
    textbox(s, x - 0.5, 4.25, 3.1, 0.6, [[(name, 24, True, NAVY, FONT)]], align=PP_ALIGN.CENTER)
    textbox(s, x - 0.5, 4.85, 3.1, 1.1, [[(ln, 18, False, MUTED, FONT)] for ln in role.split("\n")], align=PP_ALIGN.CENTER)

# ---------- 3. 두 가지 보험 ----------
s = slide(); title(s, "두 가지 보험")
for (nm, tag, top), x in zip([("의료보험", "병원비", NAVY), ("자동차보험", "자동차 사고", AMBER)], [2.0, 7.2]):
    c = box(s, x, 2.6, 4.1, 2.3, LIGHT, LINE, [], anchor=MSO_ANCHOR.TOP)
    bar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x), Inches(2.6), Inches(4.1), Inches(0.12))
    bar.shadow.inherit = False; bar.fill.solid(); bar.fill.fore_color.rgb = top; bar.line.fill.background()
    textbox(s, x, 3.1, 4.1, 0.9, [[(nm, 30, True, NAVY, FONT)]], align=PP_ALIGN.CENTER)
    textbox(s, x, 3.95, 4.1, 0.7, [[(tag, 21, False, MUTED, FONT)]], align=PP_ALIGN.CENTER)

# ---------- 4-5. 보험 상세 ----------
def detail(titletext, lead, bullets):
    s = slide(); title(s, titletext)
    bar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.7), Inches(2.0), Inches(0.14), Inches(0.7))
    bar.shadow.inherit = False; bar.fill.solid(); bar.fill.fore_color.rgb = AMBER; bar.line.fill.background()
    textbox(s, 1.0, 2.0, 11, 0.8, [[(lead, 30, True, NAVY, FONT)]], anchor=MSO_ANCHOR.MIDDLE)
    rows = [[("—  ", 23, True, AMBER, FONT), (b, 23, False, INK, FONT)] for b in bullets]
    tb = textbox(s, 1.0, 3.2, 11.5, 3.0, rows)
    for p in tb.text_frame.paragraphs:
        p.space_after = Pt(14)


detail("의료보험이란", "병원비를 보험금으로 돌려주는 보험",
       ["병원비를 청구하면 보험금을 계좌로 입금해 준다",
        "청구액이 100만 원보다 적으면 — 묻지 않고 바로 입금 (예: 30만 원 청구 → 즉시)",
        "100만 원 이상이면 — 직원이 확인한 뒤 입금"])
detail("자동차보험이란", "사고 피해를 보상해 주는 보험",
       ["사고를 접수하면 접수번호가 나온다",
        "직원 한 명이 그 사고를 맡아(배정) 보상 금액을 정한다",
        "정해진 금액이 계좌로 입금된다"])

# ---------- 6-8. 여정 ----------
def journey(titletext, steps, vtext):
    s = slide(); title(s, titletext)
    n = len(steps)
    iw, gap = 2.2, 0.6
    total = n * iw + (n - 1) * gap
    x = (SW - total) / 2
    colmap = {"cust": CUST, "staff": STAFF, "sys": SYS}
    for i, (who, label, action) in enumerate(steps):
        cx = x + iw / 2 - 0.55
        box(s, cx, 2.5, 1.1, 1.1, colmap[who], None, [[(label, 17, True, WHITE, FONT)]], shape=MSO_SHAPE.OVAL)
        box(s, x, 3.85, iw, 1.0, LIGHT, LINE, [[(action, 19, True, NAVY, FONT)]], line_w=1.3)
        if i < n - 1:
            arrow(s, x + iw + 0.05, 4.0)
        x += iw + gap
    verdict(s, vtext, t=5.4)


journey("여정 ① — 가입",
        [("cust", "고객", "상품 조회"), ("cust", "고객", "가입 신청"),
         ("staff", "직원", "가입 심사"), ("sys", "시스템", "계약 자동 생성")],
        "승인되면 계약이 바로 생깁니다.")
journey("여정 ② — 납부와 미납",
        [("cust", "고객", "매달 보험료 납부"), ("sys", "시스템", "미납 감지"),
         ("sys", "시스템", "고지서 자동 발송")],
        "낼 회차보다 납부가 밀리면 미납으로 잡히고, 매일 아침 고지서를 보냅니다 (30일 넘으면 해지 예고).")
journey("여정 ③ — 보상과 지급",
        [("cust", "고객", "청구·사고 접수"), ("staff", "직원", "보상 심사"),
         ("sys", "시스템", "보험금 지급")],
        "병원비는 100만 원 미만이면 즉시, 이상이거나 자동차 사고면 직원 심사를 거쳐 지급됩니다.")

# ---------- 9. 도메인 여섯 개 ----------
s = slide(); title(s, "도메인 여섯 개")
doms = ["사용자", "상품", "계약", "청구", "심사", "사고이력"]
cw, gap = 1.8, 0.25
total = len(doms) * cw + (len(doms) - 1) * gap
x = (SW - total) / 2
for d in doms:
    box(s, x, 3.2, cw, 1.0, PANEL, NAVY, [[(d, 20, True, NAVY, FONT)]], line_w=1.3)
    x += cw + gap

# ---------- 10-16. 설계=구현 비교 ----------
comps = [
    ("사용자", "user", "상속(User→Policyholder·Employee)과 모든 필드가 동일.",
     "userId(String) → id(Long), birthDate Date → LocalDate."),
    ("상품", "product", "상품 상속과 CoverageItem 합성 관계가 동일.",
     "productId·itemId(String) → id(Long)."),
    ("계약", "contract", "계약–납부–고지 합성 구조가 동일.",
     "자동이체 정보 AutoDebit 추가, 날짜 타입 변경."),
    ("청구", "claim", "Claim 상속(의료/자동차)과 필드가 동일.",
     "첨부파일 ClaimAttachment 추가, ClaimStatus 4값 → 6값(송금 결과 추적)."),
    ("__CODE__", None, None, None),
    ("심사", "review", "Review 상속(가입심사/지급심사) 구조가 동일.",
     "지급심사 대상이 의료청구 → Claim(의료+자동차)으로 확대 (ADR 0009)."),
    ("사고이력", "accident", "사고 집계 정보(건수·지급액·면허상태)를 동일하게 보유.",
     "외부 연동이 더미라 개별 AccidentRecord 제거, 값 객체로 단순화."),
]
for name, key, same, diff in comps:
    if name == "__CODE__":
        s = slide(); title(s, "설계 = 구현 — 청구 상태값 (코드)")
        code = ("public enum ClaimStatus {\n"
                "    PENDING, IN_REVIEW, APPROVED, REJECTED,   // 설계 4값\n"
                "    COMPLETED, FAILED                         // 구현 추가: 송금 성공·실패 (ADR 0007)\n"
                "}")
        cb = box(s, 1.0, 2.2, 11.3, 2.0, PANEL, LINE,
                 [[(ln, 16, False, NAVY2, MONO)] for ln in code.split("\n")],
                 align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.MIDDLE)
        verdict(s, "송금 결과까지 추적해야 해서 두 값을 더했습니다.", t=4.6)
        continue
    s = slide(); title(s, f"설계 = 구현 — {name}")
    box(s, 0.6, 1.45, 5.9, 0.5, PANEL, None, [[("설계", 17, True, NAVY, FONT)]])
    box(s, 6.83, 1.45, 5.9, 0.5, AMBER, None, [[("코드 역설계", 17, True, NAVY2, FONT)]])
    img_fit(s, os.path.join(CL, f"{key}-design.png"), 0.6, 2.05, 5.9, 3.3)
    img_fit(s, os.path.join(CL, f"{key}-code.png"), 6.83, 2.05, 5.9, 3.3)
    box(s, 0.6, 5.6, 5.9, 1.3, OKBG, None,
        [[("= 같은 점", 16, True, OK, FONT)], [(same, 15, False, INK, FONT)]],
        align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.MIDDLE)
    box(s, 6.83, 5.6, 5.9, 1.3, AMBERBG, None,
        [[("≠ 다른 점", 16, True, RGBColor(0xC8, 0x86, 0x0A), FONT)], [(diff, 15, False, INK, FONT)]],
        align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.MIDDLE)

# ---------- 17. 유스케이스 ----------
s = slide(); title(s, "유스케이스 — 실제 동작 경로")
img_fit(s, os.path.join(DIA, "uc-map.png"), 3.0, 1.6, 7.3, 4.3)
verdict(s, "노란 경로가 데모에서 실제로 동작합니다.", t=6.1)

# ---------- 18. AI 사용 ----------
s = slide(); title(s, "AI를 어떻게 썼는가")
box(s, 0.8, 1.7, 5.7, 1.5, OKBG, None,
    [[("사람(설계자)", 21, True, NAVY, FONT)], [("다이어그램 설계 · 결정(ADR) · 리뷰", 18, False, INK, FONT)]],
    align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.MIDDLE)
box(s, 6.83, 1.7, 5.7, 1.5, PANEL, None,
    [[("AI", 21, True, NAVY, FONT)], [("구현 · 테스트 · 리팩토링", 18, False, INK, FONT)]],
    align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.MIDDLE)
steps = ["설계", "취조", "계획", "TDD 구현", "리뷰"]
cw, gap = 2.0, 0.45
total = len(steps) * cw + (len(steps) - 1) * gap
x = (SW - total) / 2
for i, st in enumerate(steps):
    top = AMBER if st == "TDD 구현" else NAVY
    sp = box(s, x, 3.7, cw, 1.0, PANEL, LINE, [[(st, 18, True, NAVY, FONT)]], line_w=1.2)
    bar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x), Inches(3.7), Inches(cw), Inches(0.1))
    bar.shadow.inherit = False; bar.fill.solid(); bar.fill.fore_color.rgb = top; bar.line.fill.background()
    if i < len(steps) - 1:
        arrow(s, x + cw + 0.02, 3.85)
    x += cw + gap
verdict(s, "설계와 결정은 사람이, 구현은 AI가 했습니다.", t=5.3)

# ---------- 19. AI 문제 ----------
s = slide(); title(s, "AI를 쓰며 생긴 문제와 해결")
cards = [("① 성급한 추상화", "실익 없는 계층 분리 → 되돌림"),
         ("② 명세와 불일치", "자동 배정 → 수동 배정으로 정정"),
         ("③ 계층 패턴 일탈", "점검으로 식별·관리"),
         ("④ 용어·도메인 오염", "용어집·ADR로 사전 차단")]
positions = [(0.8, 1.7), (6.83, 1.7), (0.8, 3.65), (6.83, 3.65)]
for (h, fix), (x, y) in zip(cards, positions):
    bx = box(s, x, y, 5.7, 1.7, LIGHT, LINE,
             [[(h, 20, True, NAVY, FONT)], [(fix, 17, False, OK, FONT)]],
             align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.MIDDLE, line_w=1.0)
    bar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x), Inches(y), Inches(0.12), Inches(1.7))
    bar.shadow.inherit = False; bar.fill.solid(); bar.fill.fore_color.rgb = AMBER; bar.line.fill.background()
verdict(s, "AI는 빠르지만, 방향은 사람이 잡았습니다.", t=5.7)

# ---------- 20. 결론 ----------
s = slide(NAVY2)
textbox(s, 1.0, 2.7, 11.3, 0.9, [[("결론", 36, True, WHITE, FONT)]])
textbox(s, 1.0, 3.9, 11.3, 1.2, [[("설계한 그대로 구현했고, 그 과정에서 AI를 통제했습니다.", 26, True, AMBER, FONT)]])

out = os.path.join(HERE, "보험발표-편집가능.pptx")
prs.save(out)
print("saved:", out, "| slides:", len(prs.slides._sldIdLst))
