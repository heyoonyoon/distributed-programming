# 0001 — 클래스 다이어그램의 메서드는 "책임"이지 배치가 아니다

클래스 다이어그램에 그려진 메서드(예: `User.login()`, `Policyholder.applyInsurance()`)는
해당 도메인이 지는 **개념적 책임**을 뜻하며, 반드시 그 클래스의 인스턴스 메서드로
구현해야 한다는 의미가 아니다. 여러 객체나 외부 자원(JWT 토큰 등)을 조율하는 책임은
Service 계층이 맡고, 엔티티에는 순수 상태변경 메서드(`checkPassword`, `updateContact` 등)만 둔다.

계기: 다이어그램의 `User.login(email, password): boolean` / `logout()`을 JWT 무상태 인증
설계에 맞춰 `AuthService`로 옮기면서. 엔티티는 로그인이라는 책임을 "표현"하지만, 비밀번호
검증과 토큰 발급의 실제 조율은 서비스가 수행한다. 로그아웃은 무상태라 서버 구현이 없다.
