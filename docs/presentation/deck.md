---
marp: true
theme: control
paginate: true
size: 16:9
math: false
header: ''
footer: '보험 시스템 · AI 통제 발표 · 2026-06-09'
---

<!-- _class: lead -->
<!-- _paginate: false -->
<!-- _footer: '' -->

# 보험 시스템 — AI로 만들고, **AI를 통제하다**

## 설계는 사람, 구현은 AI, 통제의 증거는 git

<br>

발표자: 본인 · 2026-06-09 · 분산프로그래밍 기말

---

## 무엇을 만들었나 — 도메인 한눈에

<div class="flow">
  <div class="node">상품 조회<small>고객</small></div>
  <div class="arrow">→</div>
  <div class="node">가입 신청<small>고객</small></div>
  <div class="arrow">→</div>
  <div class="node">가입 심사<small>직원</small></div>
  <div class="arrow">→</div>
  <div class="node">계약 자동생성<small>시스템</small></div>
  <div class="arrow">→</div>
  <div class="node">납입<small>고객</small></div>
</div>

<div class="flow">
  <div class="node">청구·사고 접수<small>고객</small></div>
  <div class="arrow">→</div>
  <div class="node">보상 심사<small>직원</small></div>
  <div class="arrow">→</div>
  <div class="node db">보험금 지급<small>시스템</small></div>
</div>

**의료·자동차보험**의 가입부터 청구·지급까지. 액터는 셋 — 고객 · 보험사 직원 · 시스템.
<small>회원가입 같은 뻔한 건 생략. 도메인이 살아있는 지점만 보여줍니다.</small>

---

<!-- _class: lead -->
<!-- _footer: '' -->

# DEMO

## 가입 신청 → 직원 조건부 승인+할증 → **계약 자동 생성** → 납입

<small>지금 보실 건 "역할을 오가며 도메인 상태가 실제로 흐르는" 유일하게 끝까지 동작하는 흐름입니다.</small>

---

## 데모 포인트 — 화면이 아니라 **규칙**이 돈다

- 직원이 입력한 **할증률**이 그대로 **계약 보험료에 반영**됩니다.
- 이건 제가 **ADR로 못박은 도메인 규칙**(조건부 승인은 심사에만, 할증은 최종 보험료에)이 코드로 살아있다는 뜻.
- 승인 즉시 **계약이 자동 생성**(ADR 0005)되어 고객이 증권을 받습니다.

<div class="badge">도메인 규칙 = 살아있는 코드</div>

<small>※ 청구/사고/배정 화면은 아직 목업이라 동작하는 척 시연하지 않습니다(정직). 백엔드 동작은 테스트로 보강.</small>

---

<!-- _class: statement -->

# 데모는 됩니다. 본론은 **"어떻게 통제했나"** 입니다.

<p class="sub">교수님이 보시는 건 '됐냐'가 아니라 'AI를 통제했냐'.</p>

---

## 일부러 4단계로 진화 — 설계는 내가, 구현만 AI

<div class="timeline">
  <div class="stage">
    <h3>1. TUI</h3>
    <ul><li>Scanner 콘솔</li><li>도메인만, Controller·Service 없음</li><li>Main 1800줄</li></ul>
    <span class="hash">8720dfa</span>
  </div>
  <div class="stage">
    <h3>2. +DB</h3>
    <ul><li>distribution 프로젝트</li><li>JDBC + DAO, H2</li><li>ORM 없음, 여전히 콘솔</li></ul>
    <span class="hash">~/distribution</span>
  </div>
  <div class="stage">
    <h3>3. 웹/분산</h3>
    <ul><li>REST 도입</li><li>분산 프로그래밍 단계화</li></ul>
    <span class="hash">web</span>
  </div>
  <div class="stage">
    <h3>4. Spring</h3>
    <ul><li>insurance (현재)</li><li>Controller→Service→Repository(JPA)</li><li>println 도메인에 0개</li></ul>
    <span class="hash">af491b0 → Epic0~4</span>
  </div>
</div>

**클래스·유스케이스 다이어그램은 제가 직접** 그렸고, AI는 그걸 구현만. 단계가 바뀌어도 도메인 모델은 불변.

---

## AI 통제 장치 4 + 일정한 레이어 패턴

<div class="flow">
  <div class="node">웹 화면<small>1:1</small></div>
  <div class="arrow">→</div>
  <div class="node">Controller</div>
  <div class="arrow">→</div>
  <div class="node">Service</div>
  <div class="arrow">→</div>
  <div class="node">Domain<small>규칙</small></div>
  <div class="arrow">→</div>
  <div class="node">Repository</div>
  <div class="arrow">→</div>
  <div class="node db">DB</div>
</div>

- **일정한 패턴**: 모든 도메인이 예외 없이 위 흐름 · Entity→DTO는 `XxxResponse.from(entity)` 정적 팩토리
- **CONTEXT.md 용어집**: user/account/policyholder 동의어 혼용 차단
- **ADR 9개**: 되돌리기 어려운 결정만 — AI가 아니라 내가 결정
- **TDD 게이트**: 실패 테스트 없이 프로덕션 코드 금지

<div class="badge">도메인 클래스 System.out = 0 (grep)</div>

---

## 통제의 결정타 — 다이어그램 ↔ 코드 **1:1**

| 변경 | 코드 현실 | 결정 근거 |
|---|---|---|
| ClaimStatus 확장 | +COMPLETED, FAILED | ADR 0007 |
| ApplicationStatus 확장 | +CANCELLED | 취소 흐름 |
| 지급심사 대상 일반화 | BenefitPaymentReview → **Claim(추상)** | ADR 0009 |
| 배정 방식 분기 | 복잡의료=자동 / 자동차=수동 | `206bf25` |
| AccidentRecord 미보유 | 집계값으로 갈음 | 단순화 |

**AI가 멋대로 바꾼 게 아니라 — 내가 결정 → 문서가 따라옴.** 벗어난 곳은 전부 ADR로 기록 후 다이어그램에 역반영.

---

## 단점: AI가 삑사리 난 곳 → 어떻게 잡았나

<div class="cards">
  <div class="card">
    <h3>① 성급한 추상화</h3>
    <p>DB도 없는데 컨트롤러부터 분리 <code>3fc0ce3</code></p>
    <p class="fix">→ 실익 없어 Revert <code>3e57d68</code></p>
  </div>
  <div class="card">
    <h3>② UC와 어긋난 구현</h3>
    <p>자동차 배정을 자동으로 <code>bd2d7ce</code></p>
    <p class="fix">→ UC상 수동이 맞아 정정 <code>206bf25</code></p>
  </div>
  <div class="card">
    <h3>③ 레이어 일탈 (정직)</h3>
    <p>ProfileController가 Repository 직접 호출</p>
    <p class="fix">→ 패턴 점검으로 발견·인정</p>
  </div>
  <div class="card">
    <h3>④ 용어 표류·도메인 오염</h3>
    <p>동의어 혼용, 도메인에 println 넣으려 함</p>
    <p class="fix">→ 용어집·ADR로 사전 차단</p>
  </div>
</div>

**AI는 빠르지만 방향은 못 잡는다. 방향은 사람의 게이트가 잡는다.**

---

<!-- _class: lead -->
<!-- _footer: '' -->

# AI는 통제 가능하다 —
## 설계·용어집·ADR·TDD가 **사람 손에 있을 때만**

<br>

**이 프로젝트의 git 히스토리가 곧 통제의 로그다.**

<small>감사합니다. 질문 받겠습니다.</small>
