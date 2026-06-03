# API 명세서 — Epic 0 (인증 · 개인정보)

보험 시스템 백엔드 Epic 0의 REST API. 이 문서는 **현재 구현된 동작 그대로**를 기술한다.
프론트엔드(React)는 이 명세를 단일 출처로 삼아 연동한다.

- Base URL: `http://localhost:8080`
- 인증: **JWT Bearer**. 보호된 엔드포인트는 헤더 `Authorization: Bearer <token>` 필요.
- 요청/응답 본문: `application/json`
- 용어: `Policyholder`(가입자), `InsuranceEmployee`(직원) — CONTEXT.md 따름.

---

## 1. 로그인

`POST /auth/login` — 공개(인증 불필요)

요청
```json
{ "email": "hong@test.com", "password": "1234" }
```

응답 `200 OK`
```json
{ "token": "eyJhbGciOiJIUzUxMiJ9...." }
```

오류
| 상황 | 상태 | 본문(문자열) |
|------|------|------|
| 이메일 없음 / 비밀번호 불일치 | `401 Unauthorized` | "이메일 또는 비밀번호가 올바르지 않습니다." |
| email/password 누락(blank) | `400 Bad Request` | (검증 오류) |

> 발급된 토큰 payload: `sub`=userId, `userType`=`POLICYHOLDER`|`EMPLOYEE`, 만료 1시간.

---

## 2. 로그아웃

`POST /auth/logout` — 공개

응답 `204 No Content` (본문 없음)

> 무상태 JWT라 서버는 토큰을 폐기하지 않는다. **클라이언트가 저장한 토큰을 삭제**해야 로그아웃이 완료된다.

---

## 3. 내 정보 조회

`GET /me` — 인증 필요(가입자 전용)

응답 `200 OK`
```json
{
  "name": "홍길동",
  "email": "hong@test.com",
  "phone": "010-1111-1111",
  "address": "서울시 강남구",
  "bankAccount": "110-111-111111"
}
```

오류
| 상황 | 상태 |
|------|------|
| 토큰 없음/위조/만료 | `401 Unauthorized` |
| 직원(InsuranceEmployee) 토큰으로 접근 | `403 Forbidden` |
| 사용자 없음 | `404 Not Found` |

---

## 4. 개인정보 수정 (UC06)

`PUT /me/profile` — 인증 필요(가입자 전용)

요청 (모든 필드 필수, blank 불가)
```json
{
  "email": "new@test.com",
  "phone": "010-7777-8888",
  "address": "서울시 송파구",
  "bankAccount": "222-333-444"
}
```

응답 `200 OK` — 수정된 전체 프로필(3번 응답과 동일 형태)
```json
{
  "name": "홍길동",
  "email": "new@test.com",
  "phone": "010-7777-8888",
  "address": "서울시 송파구",
  "bankAccount": "222-333-444"
}
```

오류
| 상황 | 상태 | 비고 |
|------|------|------|
| 토큰 없음/위조/만료 | `401` | |
| 직원 토큰으로 접근 | `403 Forbidden` | 가입자 전용 |
| 본인인증 실패 | `403 Forbidden` | (현재 목은 항상 성공) |
| 다른 사용자가 쓰는 이메일로 변경 | `409 Conflict` | "이미 사용 중인 이메일입니다." |
| 필드 누락(blank) | `400 Bad Request` | |
| 사용자 없음 | `404 Not Found` | |

> 본인인증(휴대폰/공동인증서)은 백엔드 목으로 항상 통과하므로, 프론트는 인증 단계 UI를 생략하거나 형식적 단계로만 둔다.

---

## 데모 시드 계정 (앱 시작 시 자동 생성, 비밀번호 모두 `1234`)

| 이메일 | 역할 | 이름 |
|--------|------|------|
| hong@test.com | Policyholder | 홍길동 |
| kim@test.com | Policyholder | 김보험 |
| staff@test.com | InsuranceEmployee | 이심사 |

---

## 인증 흐름 요약 (프론트 연동 가이드)

1. `POST /auth/login` → `token` 저장(localStorage 등).
2. 이후 모든 보호 요청에 `Authorization: Bearer <token>` 부착.
3. `401` 응답 시 → 토큰 폐기 후 로그인 화면으로.
4. 로그아웃 = 저장 토큰 삭제(+ 선택적으로 `POST /auth/logout` 호출).
