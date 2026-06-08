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

## 두 가지 보험을 다룬다

<div class="bigcards">
  <div class="bigcard">
    <h3>의료보험</h3>
    <p>진료비를 청구합니다.</p>
    <p>입원·통원 보장, 보장 한도와 자기부담금.</p>
    <p><small>청구가 간단하면 즉시 지급, 복잡하면 심사로 넘어갑니다.</small></p>
  </div>
  <div class="bigcard amber">
    <h3>자동차보험</h3>
    <p>사고를 접수합니다.</p>
    <p>차량 정보, 운전자 범위, 사고 이력으로 보험료 산정.</p>
    <p><small>접수되면 담당 직원이 지급액을 사정합니다.</small></p>
  </div>
</div>

---

## 세 주체가 참여한다

<div class="bigcards three">
  <div class="bigcard">
    <h3>고객</h3>
    <p>가입자.</p>
    <p>가입·납부·청구·사고접수.</p>
  </div>
  <div class="bigcard">
    <h3>보험사 직원</h3>
    <p>심사자.</p>
    <p>가입 심사·지급 심사.</p>
  </div>
  <div class="bigcard amber">
    <h3>시스템</h3>
    <p>자동 처리.</p>
    <p>계약 생성·고지서·보험금 지급.</p>
  </div>
</div>

---

## 여정 ① — 가입

<div class="flow">
  <div class="node">상품 조회<small>고객</small></div>
  <div class="arrow">→</div>
  <div class="node">가입 신청<small>고객</small></div>
  <div class="arrow">→</div>
  <div class="node">가입 심사<small>직원</small></div>
  <div class="arrow">→</div>
  <div class="node db">계약 자동 생성<small>시스템</small></div>
</div>

<p class="verdict">심사가 승인되면 계약이 자동으로 만들어집니다.</p>

---

## 여정 ② — 납부와 미납

<div class="flow">
  <div class="node">매달 보험료 납부<small>고객</small></div>
  <div class="arrow">→</div>
  <div class="node">미납 발생<small>시스템 감지</small></div>
  <div class="arrow">→</div>
  <div class="node db">고지서 자동 발송<small>시스템</small></div>
</div>

<p class="verdict">납부가 밀리면 시스템이 미납 고지서를 자동으로 보냅니다.</p>

---

## 여정 ③ — 보상과 지급

<div class="flow">
  <div class="node">청구·사고 접수<small>고객</small></div>
  <div class="arrow">→</div>
  <div class="node">보상 심사<small>직원</small></div>
  <div class="arrow">→</div>
  <div class="node db">보험금 지급<small>시스템</small></div>
</div>

<p class="verdict">의료청구는 복잡도에 따라, 자동차사고는 직원 사정으로 지급됩니다.</p>

---

## 무엇으로 이루어져 있나 — 도메인

<div class="matchbar">
  <span class="check" style="border-color:#0b3d5c;color:#0b3d5c">사용자</span>
  <span class="check" style="border-color:#0b3d5c;color:#0b3d5c">상품</span>
  <span class="check" style="border-color:#0b3d5c;color:#0b3d5c">계약·납부</span>
  <span class="check" style="border-color:#0b3d5c;color:#0b3d5c">청구·사고</span>
  <span class="check" style="border-color:#0b3d5c;color:#0b3d5c">심사·지급</span>
</div>

<p class="verdict">이 다섯 도메인을 직접 클래스 다이어그램으로 설계했습니다.</p>

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

<div class="sd">
  <div class="same"><b>같은 점</b> 상속 구조와 관계가 그대로입니다.</div>
  <div class="diff"><b>다른 점</b> 식별자 String→Long, 날짜 Date→LocalDate (JPA 표준).</div>
</div>

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

<div class="sd">
  <div class="same"><b>같은 점</b> 계약–납부–고지 구성, 청구 상속이 동일합니다.</div>
  <div class="diff"><b>다른 점</b> 계약에 AutoDebit(자동이체) 추가, 청구 상태 4→6값.</div>
</div>

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

<div class="sd">
  <div class="same"><b>같은 점</b> 심사 상속 구조가 동일합니다.</div>
  <div class="diff"><b>다른 점</b> 사고이력은 더미라 AccidentRecord 제거, 지급심사 대상에 자동차 포함.</div>
</div>

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
