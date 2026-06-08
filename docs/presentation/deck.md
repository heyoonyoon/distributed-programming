---
marp: true
theme: control
paginate: true
size: 16:9
math: false
header: ''
footer: ''
---

<!-- _class: lead -->
<!-- _paginate: false -->

# 설계대로 구현하고, AI를 통제하다

## 의료·자동차 보험 시스템

---

## 어떤 서비스인가

<div class="flow">
  <div class="node">보험에 가입한다<small>고객</small></div>
  <div class="arrow">→</div>
  <div class="node">사고가 나서 청구한다<small>고객</small></div>
  <div class="arrow">→</div>
  <div class="node db">보험금을 받는다<small>시스템</small></div>
</div>

<p class="verdict">고객이 보험에 가입하고, 사고가 나면 보험금을 받는 서비스입니다.</p>

---

## 무엇으로 이루어져 있나 — 도메인

<div class="matchbar">
  <span class="check" style="border-color:#0b3d5c;color:#0b3d5c">사용자</span>
  <span class="check" style="border-color:#0b3d5c;color:#0b3d5c">상품</span>
  <span class="check" style="border-color:#0b3d5c;color:#0b3d5c">계약·납부</span>
  <span class="check" style="border-color:#0b3d5c;color:#0b3d5c">청구·사고</span>
  <span class="check" style="border-color:#0b3d5c;color:#0b3d5c">심사·지급</span>
</div>

<p class="verdict">서비스는 다섯 개의 도메인으로 이루어집니다.</p>

<p class="small-note" style="text-align:center">이 도메인들을 직접 클래스 다이어그램으로 설계하고, 구현은 AI로 진행했습니다.</p>

---

## 설계 = 구현 ① — 사용자 · 상품

<div class="clgrid">
  <div class="spacer"></div>
  <div class="clhead d">내가 설계</div>
  <div class="clhead b">코드 역설계</div>
  <div class="cllabel">사용자</div>
  <img src="diagrams/clusters/user-design.png" alt="">
  <img src="diagrams/clusters/user-code.png" alt="">
  <div class="cllabel">상품</div>
  <img src="diagrams/clusters/product-design.png" alt="">
  <img src="diagrams/clusters/product-code.png" alt="">
</div>

<p class="verdict">설계한 구조 그대로 구현되었습니다.</p>

---

## 설계 = 구현 ② — 계약 · 청구

<div class="clgrid">
  <div class="spacer"></div>
  <div class="clhead d">내가 설계</div>
  <div class="clhead b">코드 역설계</div>
  <div class="cllabel">계약</div>
  <img src="diagrams/clusters/contract-design.png" alt="">
  <img src="diagrams/clusters/contract-code.png" alt="">
  <div class="cllabel">청구</div>
  <img src="diagrams/clusters/claim-design.png" alt="">
  <img src="diagrams/clusters/claim-code.png" alt="">
</div>

<p class="verdict">자동이체(AutoDebit) 기능 하나가 더해졌을 뿐, 구조는 같습니다.</p>

---

## 설계 = 구현 ③ — 심사 · 사고이력

<div class="clgrid">
  <div class="spacer"></div>
  <div class="clhead d">내가 설계</div>
  <div class="clhead b">코드 역설계</div>
  <div class="cllabel">심사</div>
  <img src="diagrams/clusters/review-design.png" alt="">
  <img src="diagrams/clusters/review-code.png" alt="">
  <div class="cllabel">사고이력</div>
  <img src="diagrams/clusters/accident-design.png" alt="">
  <img src="diagrams/clusters/accident-code.png" alt="">
</div>

<p class="verdict">외부 사고이력은 실제 연동이 아니라 더미라서, 개별 기록을 집계값으로 단순화했습니다.</p>

---

## 차이는 왜 생겼나

| 차이 | 이유 |
|---|---|
| 식별자 String → **Long**, 날짜 → LocalDate | JPA 표준 매핑 |
| 계약에 **AutoDebit** 추가 | 자동이체 기능 |
| 청구 상태 4값 → **6값** | 송금 성공·실패까지 추적 (ADR 0007) |
| 지급심사 대상 → **자동차사고 포함** | 자동차도 사정 필요 (ADR 0009) |
| AccidentRecord 제거 | 더미 외부연동 단순화 |

<p class="verdict">차이는 모두 구현상의 이유가 있고, ADR로 기록했습니다.</p>

---

## 유스케이스도 설계대로 동작한다

<div style="text-align:center">
  <img src="diagrams/uc-map.png" alt="유스케이스 지도" style="height:430px;">
</div>

<p class="verdict">설계한 유스케이스가 데모에서 그대로 동작합니다 (노란 경로).</p>

---

## AI를 어떻게 썼는가

<div class="who">
  <div class="human"><b>사람(설계자)</b><br>다이어그램 설계 · 결정(ADR) · 리뷰</div>
  <div class="ai"><b>AI</b><br>구현 · 테스트 · 리팩토링</div>
</div>

<div class="aipipe">
  <div class="aistep"><b>설계</b><small>다이어그램</small></div>
  <div class="arrow">→</div>
  <div class="aistep"><b>취조</b><small>모델 대조</small></div>
  <div class="arrow">→</div>
  <div class="aistep"><b>계획</b></div>
  <div class="arrow">→</div>
  <div class="aistep tdd"><b>TDD 구현</b><small>AI</small></div>
  <div class="arrow">→</div>
  <div class="aistep"><b>리뷰</b></div>
</div>

<p class="verdict">설계와 결정은 사람이, 구현은 AI가 했습니다.</p>

---

## AI를 쓰며 생긴 문제와 해결

<div class="cards">
  <div class="card"><h3>① 성급한 추상화</h3><p class="fix">실익 없는 계층 분리 → 되돌림</p></div>
  <div class="card"><h3>② 명세와 불일치</h3><p class="fix">자동 배정 → 수동 배정으로 정정</p></div>
  <div class="card"><h3>③ 계층 패턴 일탈</h3><p class="fix">점검으로 식별·관리</p></div>
  <div class="card"><h3>④ 용어·도메인 오염</h3><p class="fix">용어집·ADR로 사전 차단</p></div>
</div>

<p class="verdict">AI는 빠르지만, 방향은 사람이 잡았습니다.</p>

---

<!-- _class: lead -->

# 결론

<br>

## 설계한 그대로 구현했고, 그 과정에서 AI를 통제했습니다.

<br>

감사합니다.
