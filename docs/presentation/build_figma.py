#!/usr/bin/env python3
# 절제된 에디토리얼: 흰 캔버스 + 거의 검정 + 인디고 액센트 한 색(+톤) + 다크 네이비.
import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from PIL import Image

HERE=os.path.dirname(os.path.abspath(__file__)); DIA=os.path.join(HERE,"diagrams"); CL=os.path.join(DIA,"clusters")

def C(h): return RGBColor(int(h[0:2],16),int(h[2:4],16),int(h[4:6],16))
BLACK=C("1a1a1a"); WHITE=C("ffffff"); HAIR=C("e6e6e6"); SOFT=C("f4f4f6"); GRAY=C("6b7280")
NAVY=C("1f1d3d")
ACCENT=C("5b5bd6"); ACCENT_SOFT=C("ecebfb"); ACCENT_MED=C("c9c4f3"); ACCENT_LT=C("cdbff7")
WARM=C("f7ede6")
SANS="Apple SD Gothic Neo"; MONO="Menlo"
A_CUST=ACCENT_SOFT; A_STAFF=ACCENT; A_SYS=NAVY      # 인디고 명도 3단계
def fg(fill): return WHITE if fill in (ACCENT,NAVY) else BLACK

prs=Presentation(); prs.slide_width=Inches(13.333); prs.slide_height=Inches(7.5)
BLANK=prs.slide_layouts[6]; SW=13.333

def slide(bg=WHITE):
    s=prs.slides.add_slide(BLANK); f=s.background.fill; f.solid(); f.fore_color.rgb=bg; return s

def _runs(tf,runs,align,anchor,wrap,spc):
    tf.word_wrap=wrap; tf.vertical_anchor=anchor; first=True
    for line in runs:
        p=tf.paragraphs[0] if first else tf.add_paragraph(); first=False; p.alignment=align
        for (t,sz,b,c,nm) in line:
            r=p.add_run(); r.text=t; r.font.size=Pt(sz); r.font.bold=b; r.font.color.rgb=c; r.font.name=nm or SANS
            if spc is not None: r.font._rPr.set('spc',str(int(spc*100)))

def tb(s,l,t,w,h,runs,align=PP_ALIGN.LEFT,anchor=MSO_ANCHOR.TOP,wrap=True,spc=None):
    x=s.shapes.add_textbox(Inches(l),Inches(t),Inches(w),Inches(h)); _runs(x.text_frame,runs,align,anchor,wrap,spc); return x

def panel(s,l,t,w,h,fill,rad=0.12,line=None):
    sp=s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,Inches(l),Inches(t),Inches(w),Inches(h)); sp.shadow.inherit=False
    try: sp.adjustments[0]=rad
    except Exception: pass
    sp.fill.solid(); sp.fill.fore_color.rgb=fill
    if line is None: sp.line.fill.background()
    else: sp.line.color.rgb=line; sp.line.width=Pt(1.0)
    return sp

def box(s,l,t,w,h,fill,runs,align=PP_ALIGN.CENTER,anchor=MSO_ANCHOR.MIDDLE,rad=0.16,line=None,wrap=True,shape=MSO_SHAPE.ROUNDED_RECTANGLE,spc=None):
    sp=s.shapes.add_shape(shape,Inches(l),Inches(t),Inches(w),Inches(h)); sp.shadow.inherit=False
    if shape==MSO_SHAPE.ROUNDED_RECTANGLE:
        try: sp.adjustments[0]=rad
        except Exception: pass
    if fill is None: sp.fill.background()
    else: sp.fill.solid(); sp.fill.fore_color.rgb=fill
    if line is None: sp.line.fill.background()
    else: sp.line.color.rgb=line; sp.line.width=Pt(1.2)
    tf=sp.text_frame; tf.margin_top=Pt(6); tf.margin_bottom=Pt(6); tf.margin_left=Pt(14); tf.margin_right=Pt(14)
    _runs(tf,runs,align,anchor,wrap,spc); return sp

def head(s,eyebrow,title,ink=BLACK):
    tb(s,0.85,0.6,11.6,0.5,[[(eyebrow,15,False,ACCENT,MONO)]],spc=0.6)
    tb(s,0.82,1.05,11.7,1.3,[[(title,40,True,ink,SANS)]],spc=-0.8)

def arrow(s,l,t,sz=30,col=ACCENT):
    tb(s,l,t,0.7,0.7,[[("→",sz,True,col,SANS)]],align=PP_ALIGN.CENTER,anchor=MSO_ANCHOR.MIDDLE)

def img_fit(s,path,bl,bt,bw,bh):
    iw,ih=Image.open(path).size; r=iw/ih; w,h=bw,bw/r
    if h>bh: h,w=bh,bh*r
    s.shapes.add_picture(path,Inches(bl+(bw-w)/2),Inches(bt+(bh-h)/2),Inches(w),Inches(h))

# 1. 타이틀
s=slide(NAVY)
tb(s,1.0,2.2,11.3,0.5,[[("FINAL PRESENTATION",16,False,ACCENT_LT,MONO)]],spc=0.8)
tb(s,0.95,2.75,11.6,1.7,[[("설계대로 구현하고, AI를 통제하다",50,True,WHITE,SANS)]],spc=-1.0)
tb(s,1.0,4.55,11.3,0.8,[[("의료·자동차 보험 시스템",24,False,ACCENT_LT,SANS)]],spc=-0.3)

# 2. 세 주체
s=slide(); head(s,"ACTORS","누가 사용하나")
data=[("고객",A_CUST,"가입·청구·납부"),("직원",A_STAFF,"가입·지급 심사"),("시스템",A_SYS,"계약·고지서·지급 자동화")]
xs=[1.55,5.42,9.29]
for (lab,col,role),x in zip(data,xs):
    box(s,x,2.5,2.5,2.5,col,[[(lab,30,True,fg(col),SANS)]],shape=MSO_SHAPE.OVAL,wrap=False)
    tb(s,x-1.0,5.25,4.5,0.7,[[(role,21,True,BLACK,SANS)]],align=PP_ALIGN.CENTER,spc=-0.3)

# 3. 두 가지 보험
s=slide(); head(s,"PRODUCTS","두 가지 보험")
for nm,ess,col,x in [("의료보험","아플 때 — 병원비",ACCENT_SOFT,1.45),("자동차보험","사고 났을 때 — 수리·치료비",WARM,7.0)]:
    panel(s,x,2.55,4.9,3.0,col,rad=0.12)
    tb(s,x,3.35,4.9,1.0,[[(nm,36,True,BLACK,SANS)]],align=PP_ALIGN.CENTER,spc=-0.6)
    tb(s,x,4.5,4.9,0.7,[[(ess,21,False,GRAY,SANS)]],align=PP_ALIGN.CENTER,spc=-0.3)

# 4. 의료보험 — 분기
s=slide(); head(s,"MEDICAL","병원비를 보험금으로 돌려준다")
box(s,1.0,3.9,3.0,1.3,ACCENT,[[("병원비 청구",23,True,WHITE,SANS)]],wrap=False)
arrow(s,4.3,3.15,28); arrow(s,4.3,5.05,28)
box(s,5.2,2.7,7.2,1.35,ACCENT_SOFT,[[("100만 원 미만  →  바로 입금",22,True,BLACK,SANS)]],align=PP_ALIGN.LEFT,wrap=False)
box(s,5.2,4.55,7.2,1.35,WARM,[[("100만 원 이상  →  직원 확인 후 입금",22,True,BLACK,SANS)]],align=PP_ALIGN.LEFT,wrap=False)

# 5. 자동차보험 — 흐름
s=slide(); head(s,"AUTO","사고 피해를 보상한다")
steps=["사고 접수","직원이 맡음","보상액 사정","계좌 입금"]
iw,gap=2.55,0.6; total=len(steps)*iw+(len(steps)-1)*gap; x=(SW-total)/2
for i,st in enumerate(steps):
    col=ACCENT_SOFT if i==len(steps)-1 else SOFT
    box(s,x,3.7,iw,1.4,col,[[(st,22,True,BLACK,SANS)]],line=HAIR,wrap=False)
    if i<len(steps)-1: arrow(s,x+iw+0.04,3.95)
    x+=iw+gap

# 6-8. 여정
def journey(eye,t,steps,vtext):
    s=slide(); head(s,eye,t); n=len(steps); iw,gap=2.45,0.7
    total=n*iw+(n-1)*gap; x=(SW-total)/2
    cmap={"cust":A_CUST,"staff":A_STAFF,"sys":A_SYS}
    for i,(who,lab,act) in enumerate(steps):
        col=cmap[who]
        box(s,x+iw/2-0.75,2.55,1.5,1.5,col,[[(lab,20,True,fg(col),SANS)]],shape=MSO_SHAPE.OVAL,wrap=False)
        box(s,x,4.3,iw,1.2,col,[[(act,22,True,fg(col),SANS)]],wrap=False)
        if i<n-1: arrow(s,x+iw+0.08,4.55)
        x+=iw+gap
    tb(s,0.85,5.95,11.6,0.9,[[(vtext,20,False,GRAY,SANS)]],align=PP_ALIGN.LEFT,spc=-0.3)

journey("JOURNEY 1","가입",
    [("cust","고객","상품 조회"),("cust","고객","가입 신청"),("staff","직원","가입 심사"),("sys","시스템","계약 생성")],
    "승인되면 계약이 바로 생깁니다.")
journey("JOURNEY 2","납부와 미납",
    [("cust","고객","보험료 납부"),("sys","시스템","미납 감지"),("sys","시스템","고지서 발송")],
    "납부가 밀리면 미납으로 잡히고, 매일 아침 고지서가 나갑니다 (30일 초과 시 해지 예고).")
journey("JOURNEY 3","보상과 지급",
    [("cust","고객","청구·접수"),("staff","직원","보상 심사"),("sys","시스템","보험금 지급")],
    "병원비는 100만 원 미만이면 즉시, 이상이거나 자동차 사고는 직원 심사 후 지급.")

# 9. 도메인 여섯 개
s=slide(); head(s,"DOMAINS","도메인 여섯 개")
doms=["사용자","상품","계약","청구","심사","사고이력"]
cw,gap=1.92,0.28; total=len(doms)*cw+(len(doms)-1)*gap; x=(SW-total)/2
for d in doms:
    box(s,x,3.3,cw,1.3,SOFT,[[(d,23,True,BLACK,SANS)]],line=HAIR,wrap=False); x+=cw+gap

# 10-16. 비교
comps=[("사용자","user","상속(User→Policyholder·Employee)과 모든 필드가 동일.","userId(String) → id(Long), birthDate Date → LocalDate."),
       ("상품","product","상품 상속과 CoverageItem 합성 관계가 동일.","productId·itemId(String) → id(Long)."),
       ("계약","contract","계약–납부–고지 합성 구조가 동일.","자동이체 정보 AutoDebit 추가, 날짜 타입 변경."),
       ("청구","claim","Claim 상속(의료/자동차)과 필드가 동일.","첨부파일 ClaimAttachment 추가, ClaimStatus 4값 → 6값."),
       ("__CODE__",None,None,None),
       ("심사","review","Review 상속(가입심사/지급심사) 구조가 동일.","지급심사 대상이 의료청구 → Claim(의료+자동차)으로 확대."),
       ("사고이력","accident","사고 집계 정보(건수·지급액·면허상태)를 동일하게 보유.","외부 연동이 더미라 AccidentRecord 제거, 값 객체로 단순화.")]
for name,key,same,diff in comps:
    if name=="__CODE__":
        s=slide(); head(s,"DESIGN = CODE","청구 상태값")
        code=("public enum ClaimStatus {\n    PENDING, IN_REVIEW, APPROVED, REJECTED,   // 설계 4값\n"
              "    COMPLETED, FAILED                         // 구현 추가: 송금 성공·실패\n}")
        panel(s,0.95,2.5,11.4,2.0,SOFT,rad=0.04)
        tb(s,1.3,2.7,10.8,1.7,[[(l,18,False,BLACK,MONO)] for l in code.split("\n")],anchor=MSO_ANCHOR.MIDDLE)
        tb(s,0.85,4.8,11.6,0.8,[[("송금 결과까지 추적하려고 두 값을 더했다.",22,True,BLACK,SANS)]],spc=-0.3); continue
    s=slide(); head(s,"DESIGN = CODE",name)
    tb(s,0.85,2.0,5.9,0.45,[[("설계",14,False,GRAY,MONO)]],align=PP_ALIGN.CENTER,spc=0.5)
    tb(s,6.6,2.0,5.9,0.45,[[("코드 역설계",14,False,GRAY,MONO)]],align=PP_ALIGN.CENTER,spc=0.5)
    img_fit(s,os.path.join(CL,f"{key}-design.png"),0.85,2.45,5.9,3.0)
    img_fit(s,os.path.join(CL,f"{key}-code.png"),6.6,2.45,5.9,3.0)
    box(s,0.85,5.65,5.9,1.3,ACCENT_SOFT,[[("같은 점",16,True,ACCENT,SANS)],[(same,15,False,BLACK,SANS)]],align=PP_ALIGN.LEFT,rad=0.1)
    box(s,6.6,5.65,5.9,1.3,WARM,[[("다른 점",16,True,C("b5793a"),SANS)],[(diff,15,False,BLACK,SANS)]],align=PP_ALIGN.LEFT,rad=0.1)

# 17. 유스케이스
s=slide(); head(s,"USE CASES","실제 동작 경로")
img_fit(s,os.path.join(DIA,"uc-map.png"),3.0,1.9,7.3,4.0)
tb(s,0.85,6.05,11.6,0.8,[[("노란 경로가 데모에서 실제로 동작한다.",22,True,BLACK,SANS)]],align=PP_ALIGN.CENTER,spc=-0.3)

# 18. AI 사용
s=slide(); head(s,"HOW WE USED AI","AI를 어떻게 썼나")
panel(s,0.85,2.0,5.7,1.6,ACCENT_SOFT,rad=0.12)
tb(s,1.2,2.2,5.1,1.3,[[("사람",22,True,ACCENT,SANS)],[("설계·결정(ADR)·리뷰",18,False,BLACK,SANS)]],anchor=MSO_ANCHOR.MIDDLE)
panel(s,6.75,2.0,5.7,1.6,SOFT,rad=0.12,line=HAIR)
tb(s,7.1,2.2,5.1,1.3,[[("AI",22,True,BLACK,SANS)],[("구현·테스트·리팩토링",18,False,BLACK,SANS)]],anchor=MSO_ANCHOR.MIDDLE)
steps=["설계","취조","계획","TDD 구현","리뷰"]; cw,gap=2.1,0.5; total=len(steps)*cw+(len(steps)-1)*gap; x=(SW-total)/2
for i,st in enumerate(steps):
    col=ACCENT_SOFT if st=="TDD 구현" else SOFT
    box(s,x,4.1,cw,1.1,col,[[(st,20,True,BLACK,SANS)]],line=HAIR,wrap=False)
    if i<len(steps)-1: arrow(s,x+cw+0.02,4.3,26)
    x+=cw+gap
tb(s,0.85,5.6,11.6,0.8,[[("설계·결정은 사람이, 구현은 AI가 했다.",22,True,BLACK,SANS)]],align=PP_ALIGN.CENTER,spc=-0.3)

# 19. AI 문제
s=slide(); head(s,"AI PITFALLS","문제와 해결")
cards=[("성급한 추상화","실익 없는 계층 분리 → 되돌림"),("명세와 불일치","자동 배정 → 수동 배정 정정"),
       ("계층 패턴 일탈","점검으로 식별·관리"),("용어·도메인 오염","용어집·ADR로 사전 차단")]
pos=[(0.85,2.1),(6.75,2.1),(0.85,4.0),(6.75,4.0)]
for (h,fix),(x,y) in zip(cards,pos):
    panel(s,x,y,5.7,1.7,SOFT,rad=0.1,line=HAIR)
    bar=s.shapes.add_shape(MSO_SHAPE.RECTANGLE,Inches(x),Inches(y+0.25),Inches(0.1),Inches(1.2)); bar.shadow.inherit=False
    bar.fill.solid(); bar.fill.fore_color.rgb=ACCENT; bar.line.fill.background()
    tb(s,x+0.45,y+0.25,5.0,1.3,[[(h,22,True,BLACK,SANS)],[(fix,18,False,GRAY,SANS)]],anchor=MSO_ANCHOR.MIDDLE,spc=-0.3)
tb(s,0.85,6.0,11.6,0.8,[[("AI는 빠르지만, 방향은 사람이 잡았다.",22,True,BLACK,SANS)]],align=PP_ALIGN.CENTER,spc=-0.3)

# 20. 결론
s=slide(NAVY)
tb(s,1.0,2.4,11.3,0.5,[[("CONCLUSION",16,False,ACCENT_LT,MONO)]],spc=0.8)
tb(s,0.95,3.0,11.6,1.8,[[("설계한 그대로 구현했고,",44,True,WHITE,SANS)],[("그 과정에서 AI를 통제했다.",44,True,WHITE,SANS)]],spc=-0.9)

out=os.path.join(HERE,"보험발표-figma.pptx"); prs.save(out); print("saved:",out,"| slides:",len(prs.slides._sldIdLst))
