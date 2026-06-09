# CLAUDE.md

보험(insurance) 모노레포. 한 레포에 백엔드(Spring)와 프론트엔드(React)를 함께 둔다.

## 레포 구조
```
insurance/
├── CLAUDE.md
├── docs/                 ← 공용 도메인 문서 (백/프론트 둘 다 참고)
├── backend/              ← Spring Boot 4 / Java 21 (com.distribution.insurance)
└── frontend/             ← React (추후 생성)
```

백엔드 스택: Spring Data JPA, Spring Security, Validation, Spring MVC, MySQL, Lombok, JUnit5.
백엔드 테스트 실행: `cd backend && ./gradlew test`.

## 개발 방법론 (DDD + Superpowers + grill-with-docs)

이 프로젝트는 아래 파이프라인을 따른다. 발산 → 취조(수렴) → 계획 → 구현 순서이며,
각 단계는 정해진 위치에 산출물을 남긴다.

```
brainstorming → grill-with-docs → writing-plans → executing-plans → finishing-a-development-branch
   (spec)         (CONTEXT/ADR)      (plan)          (구현·TDD)
```

### 1. brainstorming (superpowers)
- 아이디어를 발산하고 설계를 확정한다.
- 산출물: 검증·승인된 설계 문서 → `docs/superpowers/specs/YYYY-MM-DD-<주제>-design.md` (git 커밋).
- 하드 게이트: 설계를 제시하고 사용자가 승인하기 전까지 코드/스캐폴딩 금지.
- 끝나면 자동으로 writing-plans로 넘어가려 하므로, **여기서 멈추고 먼저 grill-with-docs를 실행한다.**

### 2. grill-with-docs (취조 / 수렴)
- brainstorming이 만든 spec을 도메인 모델(유스케이스·클래스 다이어그램)에 대고 한 질문씩 취조한다.
- 도메인 모델 참고 문서: `docs/usecases/` (UC01~UC17), `docs/class_diagram/` (00_overview ~ 07_external, total_class_diagram).
- 산출물(이것만 수정한다):
  - `CONTEXT.md` (루트) — 순수 용어집(glossary). 구현 디테일·스펙 금지. 용어 1~2문장 정의 + `_Avoid_:` 동의어.
  - `docs/adr/NNNN-<slug>.md` — 되돌리기 어렵고 / 맥락 없으면 의아하고 / 진짜 트레이드오프인 결정만 기록.
- **grill은 spec과 plan 문서를 고치지 않는다.** 취조로 결정이 바뀌면 사용자/Claude가 직접 spec에 반영한다.
- 취조 종료 후 반드시: 바뀐 결정을 spec 문서에 반영한다.

### 3. writing-plans (superpowers)
- 확정된 spec으로 TDD 구조의 단계별 구현 계획서를 만든다.
- 산출물: `docs/superpowers/plans/YYYY-MM-DD-<기능명>.md`.
- **writing-plans는 자동으로 spec/CONTEXT.md/ADR을 읽지 않는다.** 호출 시 항상 아래 3개를 먼저 읽고 준수하도록 명시한다:
  - `docs/superpowers/specs/`의 해당 spec 문서 — 계획의 단일 입력(범위·도메인 모델·API·에러규약·테스트 전략).
  - `CONTEXT.md`의 용어를 코드 네이밍에 그대로 사용 (동의어 혼용 금지. 예: Policyholder로 정했으면 account/user 금지).
  - `docs/adr/`의 결정을 위반하지 않음.
- 각 task는 bite-sized step(테스트 작성 → 실패 확인 → 최소 구현 → 통과 확인 → 커밋)으로 구성된다.

### 4. executing-plans (superpowers)
- plan 문서를 실행 대본으로 삼아 step을 그대로 따라간다 = TDD 자동 수행.
- **실행 방식은 항상 Subagent-Driven(superpowers:subagent-driven-development)으로 한다 — 매번 묻지 말 것.**
  태스크별 새 서브에이전트 + 2단계 리뷰(spec 준수 → 코드 품질). Inline 실행은 사용자가 명시적으로 요청할 때만.
- 막히면 추측하지 말고 멈추고 사용자에게 질문한다.
- main/master에서 직접 구현 시작 금지 — 브랜치/worktree에서 작업.

### 5. TDD (test-driven-development) — 철칙
- **실패하는 테스트 없이 프로덕션 코드 금지.** 테스트보다 코드를 먼저 썼으면 지우고 다시 시작.
- Red(실패 테스트) → Verify Red(실패 확인) → Green(최소 구현) → Verify Green → Refactor.
- 보험 도메인 규칙(만기·갱신·청구 조건 등)은 클래스 다이어그램 관계를 테스트로 먼저 박는다.
- 테스트 실행: `cd backend && ./gradlew test`.

### 6. 마무리
- 완료 선언 전 verification-before-completion 또는 `/verify`로 실제 동작 확인.
- 코드 리뷰는 **Codex 교차 리뷰 1단계**로 한다:
  - 모든 태스크가 끝난 뒤 `codex exec`에 `git diff main...HEAD`를 물려 리뷰.
  - 플러그인(`openai/codex-plugin-cc`) 설치 시 `/codex:review`(또는 `/codex:adversarial-review`).
  - (**superpowers:requesting-code-review 별도 수행 불필요** — 태스크별 2단계 리뷰(spec·품질)가 이미 커버. 최종 Claude 리뷰는 중복이라 제거.)
- Codex 리뷰가 끝난 후 finishing-a-development-branch 로 브랜치를 마무리한다.

## 문서 위치 규약 (프로젝트 루트 기준)
- `CONTEXT.md` — 루트
- `docs/usecases/UC01.md` ~ `UC17.md` — 유스케이스 시나리오
- `docs/class_diagram/` — 클래스 다이어그램(00_overview가 인덱스, total_class_diagram이 전체)
- `docs/adr/NNNN-<slug>.md` — 결정 기록
- `docs/superpowers/specs/` — 설계 문서(spec)
- `docs/superpowers/plans/` — 구현 계획서(plan)

## Git / GitHub 워크플로우 (작업 단위 규약)
- **GitHub 마일스톤 = Epic.** 빌드 순서의 각 에픽(Epic 0~4)을 마일스톤 하나로 둔다.
- **이슈 = 작업 단위.** 각 작업을 이슈로 등록하고 해당 에픽 마일스톤에 매단다.
- **이슈마다 브랜치를 만들어 작업한다.** 브랜치명: `<이슈번호>-<slug>` (예: `1-epic0-user-auth`).
- **이슈 브랜치 하나 = PR 하나.** 한 이슈의 작업이 끝나면 그 브랜치로 PR을 올리고, PR이 머지되면 이슈가 닫힌다. 여러 이슈를 한 브랜치/PR에 섞지 않는다.
- main/master에서 직접 구현 시작 금지 — 항상 이슈 브랜치에서 작업한다.

## pane 간 전달(핸드오프) 규약
- **옆 pane(다른 작업자)에게 넘기는 산출물(API 명세서·비즈니스 로직·핸드오프 노트 등)은 "전달"이지 "배포"가 아니다.** 같은 머신·같은 레포이므로 **로컬 파일로 디스크에 두면 그게 전달이다**(예: `docs/api/`).
- **이 핸드오프 산출물을 원격(origin)에 push하거나 PR을 만들지 마라.** "전달해라/넘겨라"는 원격 반영 요청이 아니다. 사용자가 명시적으로 "원격에 올려/PR 만들어"라고 할 때만 원격에 반영한다.
- 핸드오프 노트에 적는 PR·머지·워크트리 지시는 **받는 쪽(옆 pane)의 작업 절차**에 대한 것이지, 핸드오프 문서 자체를 원격에 올리라는 뜻이 아니다.
- 여러 pane이 동시에 한 워킹트리에서 커밋해 충돌하지 않도록, 각 pane은 **별도 git worktree**에서 작업한다.
- **옆 pane에게 알릴 때는 `cmux`로 직접 보낸다(알아서 cmux 사용).** 산출물은 `docs/`에 로컬 파일로 두고, 그 위치와 작업 지시를 `cmux send`로 옆 pane 터미널에 전달한다:
  - 대상 찾기: `cmux tree` / `cmux list-workspaces`로 옆 pane(surface)을 확인.
  - 전송: `cmux send --surface <surface-ref> '<메시지>'` 후 `cmux send-key --surface <surface-ref> enter`로 제출.
  - 메시지에는 문서 경로(예: `docs/api/...`)와 핵심 지시(worktree 분리, PR→머지 워크플로우)를 담는다. 원격 push가 아니라 cmux 메시지가 "전달"이다.

## 항상 지킬 것
- 코드/문서 네이밍은 `CONTEXT.md` 용어집을 단일 출처로 따른다.
- 계획 수립 시 CONTEXT.md 용어 + docs/adr 결정을 항상 준수한다(자동 아님 — 명시적으로 적용).
- 커밋·푸시는 사용자가 요청할 때만 한다.
