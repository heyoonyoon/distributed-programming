# Epic 0 — 기반(사용자 · 인증 · 개인정보 수정) 설계

- 작성일: 2026-06-03 (취조 반영: 2026-06-03)
- 범위: 보험 시스템의 토대가 되는 사용자 도메인, 로그인 인증, UC06(개인정보 수정)
- 후속: 이 spec → grill-with-docs(취조 완료) → writing-plans → 구현
- 관련 문서: `CONTEXT.md`(용어), `docs/adr/0001-diagram-methods-are-responsibilities.md`

---

## 1. 배경 / 목적

보험 모노레포의 17개 유스케이스 중 거의 전부가 "로그인 상태"를 선행조건으로 가진다.
따라서 **사용자 도메인 + 인증**이 모든 것의 토대(Epic 0)다. 이 Epic이 완성되어야
이후 가입·심사·청구 Epic이 "로그인한 가입자/직원"을 전제로 쌓일 수 있다.

이 프로젝트는 **학교 발표용 데모(약 10분)** 가 목표다. 따라서 실제 서비스 수준의
완결성보다, **데모에서 보여줄 흐름이 진짜로 동작하는 것**을 우선한다. 데모에서
보여주지 않을 화면(예: 회원가입)은 구현하지 않고 가정으로 대체한다.

## 2. 범위 (Scope)

### 포함 ✅
- 사용자 도메인 계층: `User`(abstract) → `Policyholder`, `InsuranceEmployee`
- 로그인 / 로그아웃 (JWT 기반)
- 시드 계정 (코드 기반 자동 삽입, 비밀번호 bcrypt 해시)
- UC06 개인정보 수정 (외부 본인인증은 목(mock)으로 "되는 척")

### 제외 ❌ (이유)
- **회원가입 기능** — 데모에서 보여주지 않음. 계정은 이미 존재한다고 가정하고 시드로 대체.
- **변경 이력 테이블/클래스** — 클래스 다이어그램에 없고, 이력을 조회하는 유스케이스도 없음.
  UC06 후행조건의 "기록을 남긴다"는 **로그 한 줄**로 충족(테이블·클래스 추가 없음).
- **로그아웃 서버측 토큰 무효화(블랙리스트)** — 무상태 JWT라 프론트가 토큰을 삭제하는 것으로 충분.
- **UC06 E1(본인인증 실패·3회 연속 실패 시 10분 차단)** — 목이 항상 성공하므로 실패 흐름을
  시연할 수 없고 데모 범위 밖. 본인인증은 "통과하는 단계"로만 보여준다.
- **역할 기반 접근 차단(RBAC)** — JWT에 `userType`은 싣되, 실제 차단은 직원 전용 UC가
  등장하는 후속 Epic에서 도입.
- **다이어그램의 String 식별자(`userId`, `employeeId`)** — PK는 `Long` 자동증가 사용.

## 3. 결정 사항 (Key Decisions)

| 주제 | 결정 | 근거 |
|------|------|------|
| 인증 방식 | **JWT (무상태)** | 분산 프로그래밍 수업 맥락 어필. 코드는 우리가 작성하므로 부담 감수. |
| 로그아웃 | **무상태 — 프론트 토큰 삭제** | 데모용으로 충분. 서버 블랙리스트 불필요. |
| 계정 생성 | **코드 시드(`CommandLineRunner`)** | DB 초기화돼도 재실행으로 복구. bcrypt 해시를 코드가 자동 처리(DataGrip 수동 INSERT의 비밀번호 문제 회피). |
| JPA 상속 전략 | **SINGLE_TABLE** | User 3형제가 공통 필드 다수 공유. 한 테이블 + `user_type` 구분 컬럼이 가장 단순, JOIN 없음. |
| 식별자(PK) | **`Long id` 자동증가** | 로그인은 이메일로 하므로 사람이 읽는 식별번호 불필요. 다이어그램의 `userId`/`employeeId`(String)는 PK로 쓰지 않으며 데모에 불필요하여 두지 않는다. |
| login/logout 위치 | **`AuthService`가 담당, 엔티티엔 `checkPassword`만** | 다이어그램의 `User.login()/logout()`은 개념적 책임 표현(ADR 0001). JWT 발급·검증은 여러 자원을 조율하므로 Service. 로그아웃은 무상태라 서버 구현 없음. |
| 개인정보 수정 메서드 | **메서드 분리** | `User.updateContact(email, phone)`(다이어그램 그대로 유지) + `Policyholder.updateProfile(address, bankAccount)` 신설. 오버로딩 대신 이름으로 의도 명확화. |
| 권한/역할 | **JWT에 `userType` 적재, 차단은 후속 Epic** | Epic 0엔 직원 전용 기능이 없어 RBAC는 과함. 토큰엔 역할을 심어두고 실제 접근 차단은 직원 UC가 나오는 Epic에서 도입. |
| UC06 본인인증 | **목(mock) 인터페이스 — 항상 성공** | 외부 휴대폰/공동인증서 연동은 데모에서 낭비. 인터페이스만 두고 항상 성공 반환. |
| UC06 변경 이력 | **로그 기록(방법 A)** | 클래스·테이블 0개. 후행조건 문구만 가볍게 충족. |
| DB | **도커 로컬 MySQL** | 개발자가 도커로 띄움. 시드는 이 환경에서도 동일하게 동작. |

> ERD는 이번에 처음 설계하는 것이라 자유롭게 결정 가능.
> 단 **클래스 다이어그램은 고정**(grill에서 준수 여부 취조) — 새 도메인 클래스를 만들지 않는다.

## 4. 아키텍처

### 계층 규약 (엄수)
```
Controller → DTO → Service → Entity(도메인) → Repository(DAO) → DB
```
- 계층 건너뛰기 금지 (컨트롤러가 레포지토리 직접 호출 X)
- 엔티티를 컨트롤러 응답으로 직접 노출 금지 → 반드시 DTO 변환

### 패키지 구조 (`com.distribution.insurance`)
```
domain/user        User(abstract, @Inheritance SINGLE_TABLE),
                   Policyholder, InsuranceEmployee  ← JPA 엔티티 + 도메인 메서드
repository         UserRepository (findByEmail 등)
service            AuthService          로그인 → JWT 발급
                   ProfileService       UC06 개인정보 수정
                   IdentityVerificationService(인터페이스)
                   MockIdentityVerification(되는 척 구현)
web/dto            LoginRequest, TokenResponse,
                   UpdateProfileRequest, ProfileResponse
web/controller     AuthController(/auth/login, /auth/logout)
                   ProfileController(/me 조회·수정)
security           JwtTokenProvider(발급/검증), JwtAuthenticationFilter,
                   SecurityConfig, PasswordEncoder(bcrypt)
config             DataSeeder(CommandLineRunner) — 계정 없으면 시드
```

### 클래스 → JPA 매핑 규칙
| UML 클래스 다이어그램 | Spring/JPA |
|----------------------|-----------|
| 클래스 | `@Entity` 클래스 (원칙: 클래스 1 = 테이블 1) |
| `abstract` 클래스 | `abstract class` + `@Inheritance` |
| 속성(`# 필드`) | 컬럼(멤버 변수) |
| 메서드(`+ 오퍼레이션`) | 순수 상태변경 → 엔티티 메서드 / 흐름 조율 → 서비스 |
| `extends` | 자바 `extends` |

상속 결과: **자바 클래스 3개(User/Policyholder/InsuranceEmployee) → DB 테이블 1개(`users`)**,
`user_type` 디스크리미네이터 컬럼으로 구분. PK는 `Long id` 자동증가
(다이어그램의 `userId`/`employeeId` String 식별자는 사용하지 않음).

엔티티 메서드 배치(ADR 0001 적용):
- `User.checkPassword(raw)` — 비밀번호 일치 여부(순수 상태 비교). 로그인 조율은 `AuthService`.
- `User.updateContact(email, phone)` — 다이어그램 그대로.
- `Policyholder.updateProfile(address, bankAccount)` — 가입자 고유정보 수정(신설).
- `login()/logout()`은 엔티티에 두지 않음 — 인증은 `AuthService`/JWT 필터 책임.

```
users 테이블 (SINGLE_TABLE)
┌────┬────────┬──────────┬─────────┬─────────────┐
│ id │ name   │ ssn      │ dept    │ user_type   │
├────┼────────┼──────────┼─────────┼─────────────┤
│ 1  │ 홍길동 │ 901103.. │ (null)  │ POLICYHOLDER│
│ 2  │ 김직원 │ (null)   │ 심사팀  │ EMPLOYEE    │
└────┴────────┴──────────┴─────────┴─────────────┘
```

## 5. 핵심 흐름 (비즈니스 로직)

### 5.1 로그인 — `POST /auth/login`
1. `UserRepository.findByEmail(email)` → 없으면 인증 실패
2. `passwordEncoder.matches(입력비번, 저장해시)` → 불일치 시 실패
3. 통과 → `JwtTokenProvider`가 JWT 발급 (payload: userId, userType, 만료시각)
4. `TokenResponse{token}` 반환
이후 모든 보호 요청은 `Authorization: Bearer <token>` 헤더 사용.

### 5.2 로그아웃 — `POST /auth/logout`
- 무상태. 서버는 토큰을 폐기하지 않음. 프론트가 저장된 토큰을 삭제(데모 충분).

### 5.3 JWT 인증 필터
- 매 요청에서 `Authorization` 헤더 토큰 검증 → 유효하면 SecurityContext에 인증정보 세팅
- 위조·만료 토큰 → 401 Unauthorized

### 5.4 UC06 개인정보 수정 — `PUT /me/profile`
1. JWT에서 userId 추출 → `Policyholder` 조회
2. `IdentityVerificationService.verify()` 호출 (목 → 항상 성공. E1 실패 흐름은 범위 밖)
3. 두 도메인 메서드를 순서대로 호출:
   - `user.updateContact(email, phone)` — 부모(User) 메서드, 이메일·전화
   - `policyholder.updateProfile(address, bankAccount)` — 자식(Policyholder) 메서드, 주소·계좌
4. 저장
   - **E2**: 저장 실패 시 "잠시 후 다시 시도" 메시지 + 입력값 유지
5. 변경 성공 → **로그 기록 한 줄**(이력 충족) → `ProfileResponse` 반환

## 6. 테스트 전략 (TDD — Red → Green → Refactor)

- **AuthService**
  - 올바른 비밀번호 → JWT 발급 성공
  - 틀린 비밀번호 → 인증 실패
  - 존재하지 않는 이메일 → 인증 실패
- **JwtTokenProvider / 필터**
  - 유효 토큰 → 통과 + 인증정보 세팅
  - 위조·만료 토큰 → 거부(401)
- **ProfileService (UC06)**
  - 정상 수정 → 변경 반영 (updateContact + updateProfile 둘 다)
  - 저장 실패 → E2 처리, 입력값 유지
  - (E1 본인인증 실패 흐름은 범위 밖이라 테스트 없음)
- **DataSeeder**
  - 시드 계정 없을 때 삽입 / 이미 있으면 중복 삽입 안 함

테스트 실행: `cd backend && ./gradlew test`

## 7. 시드 데이터 (데모 계정)

- Policyholder 2~3명 (예: 홍길동 등) — bcrypt 해시 비밀번호
- InsuranceEmployee 1~2명 (심사팀 직원)
- `DataSeeder`가 앱 시작 시 "이메일로 조회 → 없으면 삽입" 방식으로 멱등 처리

## 8. 미해결/후속 (이 Epic 밖)
- 직원 전용 기능(심사 등)은 Epic 2 이후에서 다룸. Epic 0는 직원 **계정·로그인**까지만.
- 마일스톤(GitHub)은 첫 plan 확정 시점에 사용자 승인 후 생성 예정.
