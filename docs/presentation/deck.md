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

## 누가 사용하나 — 세 주체

<div class="personas">
  <div class="persona">
    <div class="pc cust">고객</div>
    <div class="pname">고객</div>
    <div class="prole">보험에 가입하고<br>청구·납부한다</div>
  </div>
  <div class="persona">
    <div class="pc staff">직원</div>
    <div class="pname">보험사 직원</div>
    <div class="prole">가입과 지급을<br>심사한다</div>
  </div>
  <div class="persona">
    <div class="pc sys">시스템</div>
    <div class="pname">시스템</div>
    <div class="prole">계약·고지서·지급을<br>자동 처리한다</div>
  </div>
</div>

---

## 두 가지 보험

<div class="kinds">
  <div class="kind"><h3>의료보험</h3><p>병원비</p></div>
  <div class="kind amber"><h3>자동차보험</h3><p>자동차 사고</p></div>
</div>

---

## 의료보험이란

<div class="detail">
  <p class="lead2">병원비를 보험금으로 돌려주는 보험</p>
  <ul>
    <li>병원비를 청구하면 보험금을 계좌로 입금해 준다</li>
    <li>청구액이 100만 원보다 적으면 — 묻지 않고 바로 입금 (예: 30만 원 청구 → 즉시)</li>
    <li>100만 원 이상이면 — 직원이 확인한 뒤 입금</li>
  </ul>
</div>

---

## 자동차보험이란

<div class="detail">
  <p class="lead2">사고 피해를 보상해 주는 보험</p>
  <ul>
    <li>사고를 접수하면 접수번호가 나온다</li>
    <li>직원 한 명이 그 사고를 맡아(배정) 보상 금액을 정한다</li>
    <li>정해진 금액이 계좌로 입금된다</li>
  </ul>
</div>

---

## 여정 ① — 가입

<div class="flowj">
  <div class="jcol"><div class="javatar cust">고객</div><div class="jaction">상품 조회</div></div>
  <div class="jarrow">→</div>
  <div class="jcol"><div class="javatar cust">고객</div><div class="jaction">가입 신청</div></div>
  <div class="jarrow">→</div>
  <div class="jcol"><div class="javatar staff">직원</div><div class="jaction">가입 심사</div></div>
  <div class="jarrow">→</div>
  <div class="jcol"><div class="javatar sys">시스템</div><div class="jaction">계약 자동 생성</div></div>
</div>

<p class="verdict">승인되면 계약이 바로 생깁니다.</p>

---

## 여정 ② — 납부와 미납

<div class="flowj">
  <div class="jcol"><div class="javatar cust">고객</div><div class="jaction">매달 보험료 납부</div></div>
  <div class="jarrow">→</div>
  <div class="jcol"><div class="javatar sys">시스템</div><div class="jaction">미납 감지</div></div>
  <div class="jarrow">→</div>
  <div class="jcol"><div class="javatar sys">시스템</div><div class="jaction">고지서 자동 발송</div></div>
</div>

<p class="verdict">낼 회차보다 납부가 밀리면 미납으로 잡히고, 매일 아침 시스템이 고지서를 보냅니다 (30일 넘으면 해지 예고).</p>

---

## 여정 ③ — 보상과 지급

<div class="flowj">
  <div class="jcol"><div class="javatar cust">고객</div><div class="jaction">청구·사고 접수</div></div>
  <div class="jarrow">→</div>
  <div class="jcol"><div class="javatar staff">직원</div><div class="jaction">보상 심사</div></div>
  <div class="jarrow">→</div>
  <div class="jcol"><div class="javatar sys">시스템</div><div class="jaction">보험금 지급</div></div>
</div>

<p class="verdict">병원비는 100만 원 미만이면 즉시, 이상이거나 자동차 사고면 직원 심사를 거쳐 지급됩니다.</p>

---

## 도메인 여섯 개

<div class="legend" style="flex-wrap:wrap">
  <span class="ly">사용자</span><span class="ly">상품</span><span class="ly">계약</span><span class="ly">청구</span><span class="ly">심사</span><span class="ly">사고이력</span>
</div>

---

## 설계 = 구현 — 사용자

<div class="cmp2">
  <figure><span class="cap cap-d">설계</span><img src="diagrams/clusters/user-design.png"></figure>
  <figure><span class="cap cap-b">코드 역설계</span><img src="diagrams/clusters/user-code.png"></figure>
</div>

<div class="sd">
  <div class="same"><b>같은 점</b> 상속(User→Policyholder·Employee)과 모든 필드가 동일.</div>
  <div class="diff"><b>다른 점</b> userId(String) → id(Long), birthDate Date → LocalDate.</div>
</div>

---

## 설계 = 구현 — 상품

<div class="cmp2">
  <figure><span class="cap cap-d">설계</span><img src="diagrams/clusters/product-design.png"></figure>
  <figure><span class="cap cap-b">코드 역설계</span><img src="diagrams/clusters/product-code.png"></figure>
</div>

<div class="sd">
  <div class="same"><b>같은 점</b> 상품 상속과 CoverageItem 합성 관계가 동일.</div>
  <div class="diff"><b>다른 점</b> productId·itemId(String) → id(Long).</div>
</div>

---

## 설계 = 구현 — 계약

<div class="cmp2">
  <figure><span class="cap cap-d">설계</span><img src="diagrams/clusters/contract-design.png"></figure>
  <figure><span class="cap cap-b">코드 역설계</span><img src="diagrams/clusters/contract-code.png"></figure>
</div>

<div class="sd">
  <div class="same"><b>같은 점</b> 계약–납부–고지 합성 구조가 동일.</div>
  <div class="diff"><b>다른 점</b> 자동이체 정보 AutoDebit 추가, 날짜 타입 변경.</div>
</div>

---

## 설계 = 구현 — 청구

<div class="cmp2">
  <figure><span class="cap cap-d">설계</span><img src="diagrams/clusters/claim-design.png"></figure>
  <figure><span class="cap cap-b">코드 역설계</span><img src="diagrams/clusters/claim-code.png"></figure>
</div>

<div class="sd">
  <div class="same"><b>같은 점</b> Claim 상속(의료/자동차)과 필드가 동일.</div>
  <div class="diff"><b>다른 점</b> 첨부파일 ClaimAttachment 추가, ClaimStatus 4값 → 6값(송금 결과 추적).</div>
</div>

---

## 설계 = 구현 — 청구 상태값 (코드)

```java
public enum ClaimStatus {
    PENDING, IN_REVIEW, APPROVED, REJECTED,  // 설계 4값
    COMPLETED, FAILED                        // 구현 추가: 송금 성공·실패 (ADR 0007)
}
```

<p class="verdict">송금 결과까지 추적해야 해서 두 값을 더했습니다.</p>

---

## 설계 = 구현 — 심사

<div class="cmp2">
  <figure><span class="cap cap-d">설계</span><img src="diagrams/clusters/review-design.png"></figure>
  <figure><span class="cap cap-b">코드 역설계</span><img src="diagrams/clusters/review-code.png"></figure>
</div>

<div class="sd">
  <div class="same"><b>같은 점</b> Review 상속(가입심사/지급심사) 구조가 동일.</div>
  <div class="diff"><b>다른 점</b> 지급심사 대상이 의료청구 → Claim(의료+자동차)으로 확대 (ADR 0009).</div>
</div>

---

## 설계 = 구현 — 사고이력

<div class="cmp2">
  <figure><span class="cap cap-d">설계</span><img src="diagrams/clusters/accident-design.png"></figure>
  <figure><span class="cap cap-b">코드 역설계</span><img src="diagrams/clusters/accident-code.png"></figure>
</div>

<div class="sd">
  <div class="same"><b>같은 점</b> 사고 집계 정보(건수·지급액·면허상태)를 동일하게 보유.</div>
  <div class="diff"><b>다른 점</b> 외부 연동이 더미라 개별 AccidentRecord 제거, 값 객체로 단순화.</div>
</div>

---

## 유스케이스 — 실제 동작 경로

<div style="text-align:center">
  <img src="diagrams/uc-map.png" alt="유스케이스 지도" style="height:430px;">
</div>

<p class="verdict">노란 경로가 데모에서 실제로 동작합니다.</p>

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
