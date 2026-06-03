# Epic 0 — 사용자·인증·UC06 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 보험 시스템의 토대 — User 도메인(상속) + JWT 로그인 + UC06 개인정보 수정을 TDD로 구현한다.

**Architecture:** Spring Boot 4 / JPA. `User`(abstract, SINGLE_TABLE 상속) → `Policyholder`/`InsuranceEmployee`. 로그인은 `AuthService`가 비밀번호 검증 후 JWT 발급(무상태), `JwtAuthenticationFilter`가 매 요청 토큰 검증. UC06은 `ProfileService`가 본인인증 목 통과 후 엔티티 도메인 메서드를 호출. 계층 규약 `Controller→DTO→Service→Entity→Repository→DB` 엄수.

**Tech Stack:** Spring Boot 4.0.6, Java 21, Spring Data JPA, Spring Security, jjwt 0.12.x, MySQL(런타임)/H2(테스트), Lombok, JUnit5.

---

## 준수 사항 (이 계획 전체에 적용)

- **용어집(`CONTEXT.md`) 단일 출처.** 코드 네이밍은 `User` / `Policyholder` / `InsuranceEmployee`만 사용. `account`, `member`, `customer`, `가입자`, `담당자` 등 동의어 금지.
- **ADR 0001 준수.** 다이어그램의 `login()/logout()`은 엔티티 메서드로 만들지 않는다. 인증 조율은 `AuthService`/필터. 엔티티엔 순수 도메인 메서드(`checkPassword`, `updateContact`, `updateProfile`)만.
- **PK는 `Long id` 자동증가.** 다이어그램의 `userId`/`employeeId`(String)는 만들지 않는다.
- **UC06 E1(본인인증 실패·차단)은 범위 밖.** 본인인증 목은 항상 성공.
- 테스트 실행: `cd backend && ./gradlew test`

## 파일 구조 (`backend/src/main/java/com/distribution/insurance/`)

```
domain/user/User.java                 abstract @Entity, SINGLE_TABLE 상속 루트, 공통 필드+checkPassword/updateContact
domain/user/Policyholder.java         @Entity @DiscriminatorValue("POLICYHOLDER"), ssn/birthDate/address/bankAccount + updateProfile
domain/user/InsuranceEmployee.java    @Entity @DiscriminatorValue("EMPLOYEE"), department/currentLoad
repository/UserRepository.java        JpaRepository<User,Long> + findByEmail
security/JwtTokenProvider.java        JWT 발급/검증
security/JwtAuthenticationFilter.java OncePerRequestFilter, 토큰→SecurityContext
security/SecurityConfig.java          SecurityFilterChain, PasswordEncoder 빈
service/AuthService.java              login(email,password)→토큰
service/IdentityVerificationService.java  인터페이스 verify()
service/MockIdentityVerification.java     항상 성공 구현
service/ProfileService.java           UC06 개인정보 수정
web/dto/LoginRequest.java             {email, password}
web/dto/TokenResponse.java            {token}
web/dto/UpdateProfileRequest.java     {email, phone, address, bankAccount}
web/dto/ProfileResponse.java          {name, email, phone, address, bankAccount}
web/controller/AuthController.java    POST /auth/login, POST /auth/logout
web/controller/ProfileController.java GET /me, PUT /me/profile
config/DataSeeder.java                CommandLineRunner 시드 계정
```

테스트는 `backend/src/test/java/com/distribution/insurance/` 아래 동일 패키지에 배치.

---

## Task 0: 의존성·설정·도커 MySQL 준비

**Files:**
- Modify: `backend/build.gradle.kts`
- Modify: `backend/src/main/resources/application.yaml`
- Create: `backend/src/test/resources/application.yaml`
- Create: `docker-compose.yml` (레포 루트)

- [ ] **Step 1: JWT + H2 의존성 추가**

`backend/build.gradle.kts`의 `dependencies { }` 블록에 추가:

```kotlin
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    testRuntimeOnly("com.h2database:h2")
```

- [ ] **Step 2: 운영 datasource + JWT 설정**

`backend/src/main/resources/application.yaml` 전체를 교체:

```yaml
spring:
  application:
    name: insurance
  datasource:
    url: jdbc:mysql://localhost:3306/insurance?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: insurance
    password: insurance
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

jwt:
  secret: "demo-secret-key-please-change-this-to-a-long-enough-value-32bytes!!"
  expiration-millis: 3600000
```

- [ ] **Step 3: 테스트용 H2 설정 (도커 없이 테스트 가능하게)**

`backend/src/test/resources/application.yaml` 생성:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

jwt:
  secret: "test-secret-key-please-change-this-to-a-long-enough-value-32byte!!"
  expiration-millis: 3600000
```

- [ ] **Step 4: 도커 MySQL 정의**

레포 루트 `docker-compose.yml` 생성:

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: insurance-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: insurance
      MYSQL_USER: insurance
      MYSQL_PASSWORD: insurance
    ports:
      - "3306:3306"
    volumes:
      - insurance-mysql-data:/var/lib/mysql

volumes:
  insurance-mysql-data:
```

- [ ] **Step 5: 빌드 검증**

Run: `cd backend && ./gradlew build -x test`
Expected: BUILD SUCCESSFUL (의존성 해석 성공)

- [ ] **Step 6: 커밋**

```bash
git add backend/build.gradle.kts backend/src/main/resources/application.yaml backend/src/test/resources/application.yaml docker-compose.yml
git commit -m "chore(epic0): JWT·H2 의존성, datasource·JWT 설정, 도커 MySQL 추가"
```

---

## Task 1: User 상속 계층 엔티티 + Repository

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/domain/user/User.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/user/Policyholder.java`
- Create: `backend/src/main/java/com/distribution/insurance/domain/user/InsuranceEmployee.java`
- Create: `backend/src/main/java/com/distribution/insurance/repository/UserRepository.java`
- Test: `backend/src/test/java/com/distribution/insurance/repository/UserRepositoryTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`UserRepositoryTest.java`:

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void findByEmail_저장된_Policyholder를_찾는다() {
        Policyholder saved = userRepository.save(new Policyholder(
                "홍길동", "hong@test.com", "010-1111-2222", "hashed-pw",
                "901103-1234567", LocalDate.of(1990, 11, 3), "서울시", "111-222-333"));

        Optional<com.distribution.insurance.domain.user.User> found =
                userRepository.findByEmail("hong@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get()).isInstanceOf(Policyholder.class);
    }

    @Test
    void findByEmail_없는_이메일이면_빈값() {
        assertThat(userRepository.findByEmail("none@test.com")).isEmpty();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests UserRepositoryTest`
Expected: 컴파일 실패 (User/Policyholder/UserRepository 미존재)

- [ ] **Step 3: User 추상 엔티티 작성**

`User.java`:

```java
package com.distribution.insurance.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;
    private String password;

    protected User(String name, String email, String phone, String password) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    /** 연락처(이메일·전화) 수정 — 다이어그램 그대로. */
    public void updateContact(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }
}
```

- [ ] **Step 4: Policyholder 엔티티 작성**

`Policyholder.java`:

```java
package com.distribution.insurance.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("POLICYHOLDER")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Policyholder extends User {

    private String ssn;
    private LocalDate birthDate;
    private String address;
    private String bankAccount;

    public Policyholder(String name, String email, String phone, String password,
                        String ssn, LocalDate birthDate, String address, String bankAccount) {
        super(name, email, phone, password);
        this.ssn = ssn;
        this.birthDate = birthDate;
        this.address = address;
        this.bankAccount = bankAccount;
    }

    /** 가입자 고유정보(주소·계좌) 수정 — 신설(ADR 0001: 도메인 메서드). */
    public void updateProfile(String address, String bankAccount) {
        this.address = address;
        this.bankAccount = bankAccount;
    }
}
```

- [ ] **Step 5: InsuranceEmployee 엔티티 작성**

`InsuranceEmployee.java`:

```java
package com.distribution.insurance.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("EMPLOYEE")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class InsuranceEmployee extends User {

    private String department;
    private int currentLoad;

    public InsuranceEmployee(String name, String email, String phone, String password,
                             String department, int currentLoad) {
        super(name, email, phone, password);
        this.department = department;
        this.currentLoad = currentLoad;
    }
}
```

- [ ] **Step 6: UserRepository 작성**

`UserRepository.java`:

```java
package com.distribution.insurance.repository;

import com.distribution.insurance.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

- [ ] **Step 7: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests UserRepositoryTest`
Expected: PASS (2 tests). H2 `users` 테이블에 `user_type` 컬럼으로 단일 테이블 저장 확인.

- [ ] **Step 8: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/domain backend/src/main/java/com/distribution/insurance/repository backend/src/test/java/com/distribution/insurance/repository
git commit -m "feat(epic0): User 상속 계층(SINGLE_TABLE) + UserRepository"
```

---

## Task 2: 비밀번호 인코더 + checkPassword 도메인 메서드

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java` (PasswordEncoder 빈만 먼저)
- Modify: `backend/src/main/java/com/distribution/insurance/domain/user/User.java`
- Test: `backend/src/test/java/com/distribution/insurance/domain/user/UserPasswordTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`UserPasswordTest.java`:

```java
package com.distribution.insurance.domain.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserPasswordTest {

    PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void checkPassword_올바른_비번이면_true() {
        Policyholder user = new Policyholder("홍길동", "h@test.com", "010", encoder.encode("1234"),
                "ssn", LocalDate.now(), "addr", "acc");
        assertThat(user.checkPassword("1234", encoder)).isTrue();
    }

    @Test
    void checkPassword_틀린_비번이면_false() {
        Policyholder user = new Policyholder("홍길동", "h@test.com", "010", encoder.encode("1234"),
                "ssn", LocalDate.now(), "addr", "acc");
        assertThat(user.checkPassword("9999", encoder)).isFalse();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests UserPasswordTest`
Expected: 컴파일 실패 (`checkPassword` 미존재)

- [ ] **Step 3: User에 checkPassword 추가**

`User.java`의 `updateContact` 위에 import 추가하고 메서드 추가:

```java
import org.springframework.security.crypto.password.PasswordEncoder;
```

```java
    /** 비밀번호 일치 여부(순수 도메인 비교) — 로그인 조율은 AuthService(ADR 0001). */
    public boolean checkPassword(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.password);
    }
```

- [ ] **Step 4: PasswordEncoder 빈 정의 (SecurityConfig 초안)**

`SecurityConfig.java`:

```java
package com.distribution.insurance.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests UserPasswordTest`
Expected: PASS (2 tests)

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java backend/src/main/java/com/distribution/insurance/domain/user/User.java backend/src/test/java/com/distribution/insurance/domain/user/UserPasswordTest.java
git commit -m "feat(epic0): PasswordEncoder 빈 + User.checkPassword 도메인 메서드"
```

---

## Task 3: JwtTokenProvider (발급·검증)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/security/JwtTokenProvider.java`
- Test: `backend/src/test/java/com/distribution/insurance/security/JwtTokenProviderTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`JwtTokenProviderTest.java`:

```java
package com.distribution.insurance.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    JwtTokenProvider provider = new JwtTokenProvider(
            "test-secret-key-please-change-this-to-a-long-enough-value-32byte!!",
            3600000L);

    @Test
    void 토큰을_발급하고_파싱하면_userId와_userType이_복원된다() {
        String token = provider.createToken(42L, "POLICYHOLDER");

        assertThat(provider.validate(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(42L);
        assertThat(provider.getUserType(token)).isEqualTo("POLICYHOLDER");
    }

    @Test
    void 위조된_토큰은_검증에_실패한다() {
        String token = provider.createToken(42L, "POLICYHOLDER");
        assertThat(provider.validate(token + "tampered")).isFalse();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests JwtTokenProviderTest`
Expected: 컴파일 실패 (`JwtTokenProvider` 미존재)

- [ ] **Step 3: JwtTokenProvider 구현**

`JwtTokenProvider.java`:

```java
package com.distribution.insurance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-millis}") long expirationMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String createToken(Long userId, String userType) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userType", userType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public String getUserType(String token) {
        return parse(token).get("userType", String.class);
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests JwtTokenProviderTest`
Expected: PASS (2 tests)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/security/JwtTokenProvider.java backend/src/test/java/com/distribution/insurance/security/JwtTokenProviderTest.java
git commit -m "feat(epic0): JwtTokenProvider 발급·검증"
```

---

## Task 4: AuthService (로그인 → 토큰)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/AuthService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/AuthServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`AuthServiceTest.java`:

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.domain.user.User;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    UserRepository userRepository = mock(UserRepository.class);
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    JwtTokenProvider tokenProvider = new JwtTokenProvider(
            "test-secret-key-please-change-this-to-a-long-enough-value-32byte!!", 3600000L);
    AuthService authService = new AuthService(userRepository, encoder, tokenProvider);

    private User policyholder() {
        return new Policyholder("홍길동", "h@test.com", "010", encoder.encode("1234"),
                "ssn", LocalDate.now(), "addr", "acc");
    }

    @Test
    void 올바른_비번이면_검증가능한_토큰을_발급한다() {
        when(userRepository.findByEmail("h@test.com")).thenReturn(Optional.of(policyholder()));

        String token = authService.login("h@test.com", "1234");

        assertThat(tokenProvider.validate(token)).isTrue();
    }

    @Test
    void 틀린_비번이면_예외() {
        when(userRepository.findByEmail("h@test.com")).thenReturn(Optional.of(policyholder()));
        assertThatThrownBy(() -> authService.login("h@test.com", "9999"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 없는_이메일이면_예외() {
        when(userRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login("none@test.com", "1234"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests AuthServiceTest`
Expected: 컴파일 실패 (`AuthService` 미존재)

- [ ] **Step 3: AuthService 구현**

`AuthService.java`:

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.user.User;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!user.checkPassword(rawPassword, passwordEncoder)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String userType = user.getClass().getSimpleName().equals("InsuranceEmployee")
                ? "EMPLOYEE" : "POLICYHOLDER";
        return tokenProvider.createToken(user.getId(), userType);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests AuthServiceTest`
Expected: PASS (3 tests)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/service/AuthService.java backend/src/test/java/com/distribution/insurance/service/AuthServiceTest.java
git commit -m "feat(epic0): AuthService 로그인→JWT 발급"
```

---

## Task 5: JwtAuthenticationFilter + SecurityConfig 체인

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/security/JwtAuthenticationFilter.java`
- Modify: `backend/src/main/java/com/distribution/insurance/security/SecurityConfig.java`
- Test: `backend/src/test/java/com/distribution/insurance/security/JwtAuthenticationFilterTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`JwtAuthenticationFilterTest.java`:

```java
package com.distribution.insurance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    JwtTokenProvider provider = new JwtTokenProvider(
            "test-secret-key-please-change-this-to-a-long-enough-value-32byte!!", 3600000L);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void 유효한_토큰이면_SecurityContext에_인증이_설정된다() throws Exception {
        String token = provider.createToken(7L, "POLICYHOLDER");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(7L);
        assertThat(auth.getAuthorities().toString()).contains("ROLE_POLICYHOLDER");
    }

    @Test
    void 토큰이_없으면_인증이_설정되지_않는다() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        filter.doFilter(req, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests JwtAuthenticationFilterTest`
Expected: 컴파일 실패 (`JwtAuthenticationFilter` 미존재)

- [ ] **Step 3: JwtAuthenticationFilter 구현**

`JwtAuthenticationFilter.java`:

```java
package com.distribution.insurance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (tokenProvider.validate(token)) {
                Long userId = tokenProvider.getUserId(token);
                String userType = tokenProvider.getUserType(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + userType)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 4: SecurityConfig에 필터 체인 추가**

`SecurityConfig.java`를 교체:

```java
package com.distribution.insurance.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;

    public SecurityConfig(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests JwtAuthenticationFilterTest`
Expected: PASS (2 tests)

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/security
git commit -m "feat(epic0): JWT 인증 필터 + 무상태 SecurityFilterChain"
```

---

## Task 6: AuthController (/auth/login, /auth/logout) + DTO

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/TokenResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/controller/AuthController.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/AuthControllerTest.java`

- [ ] **Step 1: 실패하는 테스트 작성 (MockMvc 통합)**

`AuthControllerTest.java`:

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(new Policyholder("홍길동", "h@test.com", "010", encoder.encode("1234"),
                "ssn", LocalDate.now(), "addr", "acc"));
    }

    @Test
    void 로그인_성공시_토큰을_반환한다() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"h@test.com\",\"password\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void 틀린_비번이면_401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"h@test.com\",\"password\":\"9999\"}"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests AuthControllerTest`
Expected: 컴파일 실패 (DTO/컨트롤러 미존재)

- [ ] **Step 3: DTO 작성**

`LoginRequest.java`:

```java
package com.distribution.insurance.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {
}
```

`TokenResponse.java`:

```java
package com.distribution.insurance.web.dto;

public record TokenResponse(String token) {
}
```

- [ ] **Step 4: AuthController 작성 (전역 예외 → 401 포함)**

`AuthController.java`:

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.service.AuthService;
import com.distribution.insurance.web.dto.LoginRequest;
import com.distribution.insurance.web.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return new TokenResponse(authService.login(request.email(), request.password()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // 무상태 JWT: 서버는 토큰을 폐기하지 않는다. 프론트가 토큰을 삭제(ADR/spec).
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadCredentials(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests AuthControllerTest`
Expected: PASS (2 tests)

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/web
git commit -m "feat(epic0): AuthController 로그인/로그아웃 + DTO"
```

---

## Task 7: IdentityVerificationService 목

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/IdentityVerificationService.java`
- Create: `backend/src/main/java/com/distribution/insurance/service/MockIdentityVerification.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/MockIdentityVerificationTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`MockIdentityVerificationTest.java`:

```java
package com.distribution.insurance.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockIdentityVerificationTest {

    IdentityVerificationService service = new MockIdentityVerification();

    @Test
    void 본인인증_목은_항상_성공한다() {
        assertThat(service.verify(1L)).isTrue();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests MockIdentityVerificationTest`
Expected: 컴파일 실패 (타입 미존재)

- [ ] **Step 3: 인터페이스 + 목 구현**

`IdentityVerificationService.java`:

```java
package com.distribution.insurance.service;

public interface IdentityVerificationService {
    /** 본인인증 수행. 외부 연동(휴대폰/공동인증서)의 자리표시자. */
    boolean verify(Long userId);
}
```

`MockIdentityVerification.java`:

```java
package com.distribution.insurance.service;

import org.springframework.stereotype.Component;

@Component
public class MockIdentityVerification implements IdentityVerificationService {
    @Override
    public boolean verify(Long userId) {
        return true; // 데모: 항상 성공. 실패 흐름(UC06 E1)은 범위 밖.
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests MockIdentityVerificationTest`
Expected: PASS (1 test)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/service/IdentityVerificationService.java backend/src/main/java/com/distribution/insurance/service/MockIdentityVerification.java backend/src/test/java/com/distribution/insurance/service/MockIdentityVerificationTest.java
git commit -m "feat(epic0): 본인인증 목(항상 성공)"
```

---

## Task 8: ProfileService (UC06 개인정보 수정)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/service/ProfileService.java`
- Test: `backend/src/test/java/com/distribution/insurance/service/ProfileServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`ProfileServiceTest.java`:

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    UserRepository userRepository = mock(UserRepository.class);
    IdentityVerificationService identityVerification = mock(IdentityVerificationService.class);
    ProfileService profileService = new ProfileService(userRepository, identityVerification);

    private Policyholder policyholder() {
        return new Policyholder("홍길동", "old@test.com", "010-old", "pw",
                "ssn", LocalDate.of(1990, 1, 1), "옛주소", "옛계좌");
    }

    @Test
    void 본인인증_통과시_연락처와_가입자정보가_모두_갱신된다() {
        Policyholder ph = policyholder();
        when(identityVerification.verify(1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(ph));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        profileService.updateProfile(1L, "new@test.com", "010-new", "새주소", "새계좌");

        assertThat(ph.getEmail()).isEqualTo("new@test.com");
        assertThat(ph.getPhone()).isEqualTo("010-new");
        assertThat(ph.getAddress()).isEqualTo("새주소");
        assertThat(ph.getBankAccount()).isEqualTo("새계좌");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests ProfileServiceTest`
Expected: 컴파일 실패 (`ProfileService` 미존재)

- [ ] **Step 3: ProfileService 구현**

`ProfileService.java`:

```java
package com.distribution.insurance.service;

import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository userRepository;
    private final IdentityVerificationService identityVerification;

    public ProfileService(UserRepository userRepository,
                          IdentityVerificationService identityVerification) {
        this.userRepository = userRepository;
        this.identityVerification = identityVerification;
    }

    @Transactional
    public Policyholder updateProfile(Long userId, String email, String phone,
                                      String address, String bankAccount) {
        if (!identityVerification.verify(userId)) {
            throw new IllegalStateException("본인 인증에 실패하였습니다.");
        }
        Policyholder policyholder = (Policyholder) userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        policyholder.updateContact(email, phone);
        policyholder.updateProfile(address, bankAccount);
        userRepository.save(policyholder);

        // UC06 후행조건: 변경 기록을 남긴다(로그).
        log.info("개인정보 변경: userId={} email={} phone={}", userId, email, phone);
        return policyholder;
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests ProfileServiceTest`
Expected: PASS (1 test)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/service/ProfileService.java backend/src/test/java/com/distribution/insurance/service/ProfileServiceTest.java
git commit -m "feat(epic0): ProfileService UC06 개인정보 수정"
```

---

## Task 9: ProfileController (GET /me, PUT /me/profile) + DTO

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/UpdateProfileRequest.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/dto/ProfileResponse.java`
- Create: `backend/src/main/java/com/distribution/insurance/web/controller/ProfileController.java`
- Test: `backend/src/test/java/com/distribution/insurance/web/controller/ProfileControllerTest.java`

- [ ] **Step 1: 실패하는 테스트 작성 (인증 토큰 포함)**

`ProfileControllerTest.java`:

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokenProvider;

    Long userId;
    String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        Policyholder ph = userRepository.save(new Policyholder(
                "홍길동", "old@test.com", "010-old", encoder.encode("1234"),
                "ssn", LocalDate.of(1990, 1, 1), "옛주소", "옛계좌"));
        userId = ph.getId();
        token = tokenProvider.createToken(userId, "POLICYHOLDER");
    }

    @Test
    void 토큰없이_접근하면_401() throws Exception {
        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void 개인정보를_수정하면_변경값이_반영된다() throws Exception {
        mockMvc.perform(put("/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"email\":\"new@test.com\",\"phone\":\"010-new\","
                                + "\"address\":\"새주소\",\"bankAccount\":\"새계좌\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@test.com"))
                .andExpect(jsonPath("$.address").value("새주소"));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests ProfileControllerTest`
Expected: 컴파일 실패 (DTO/컨트롤러 미존재)

- [ ] **Step 3: DTO 작성**

`UpdateProfileRequest.java`:

```java
package com.distribution.insurance.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank String email,
        @NotBlank String phone,
        @NotBlank String address,
        @NotBlank String bankAccount) {
}
```

`ProfileResponse.java`:

```java
package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.user.Policyholder;

public record ProfileResponse(
        String name, String email, String phone, String address, String bankAccount) {

    public static ProfileResponse from(Policyholder p) {
        return new ProfileResponse(p.getName(), p.getEmail(), p.getPhone(),
                p.getAddress(), p.getBankAccount());
    }
}
```

- [ ] **Step 4: ProfileController 작성**

`ProfileController.java`:

```java
package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.service.ProfileService;
import com.distribution.insurance.web.dto.ProfileResponse;
import com.distribution.insurance.web.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    public ProfileController(ProfileService profileService, UserRepository userRepository) {
        this.profileService = profileService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ProfileResponse me(@AuthenticationPrincipal Long userId) {
        Policyholder p = (Policyholder) userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return ProfileResponse.from(p);
    }

    @PutMapping("/me/profile")
    public ProfileResponse update(@AuthenticationPrincipal Long userId,
                                  @Valid @RequestBody UpdateProfileRequest request) {
        Policyholder updated = profileService.updateProfile(
                userId, request.email(), request.phone(), request.address(), request.bankAccount());
        return ProfileResponse.from(updated);
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests ProfileControllerTest`
Expected: PASS (2 tests)

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/web
git commit -m "feat(epic0): ProfileController GET /me, PUT /me/profile (UC06)"
```

---

## Task 10: DataSeeder (시드 계정)

**Files:**
- Create: `backend/src/main/java/com/distribution/insurance/config/DataSeeder.java`
- Test: `backend/src/test/java/com/distribution/insurance/config/DataSeederTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`DataSeederTest.java`:

```java
package com.distribution.insurance.config;

import com.distribution.insurance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class DataSeederTest {

    @Test
    void run_계정이_없으면_시드하고_다시_실행해도_중복삽입하지_않는다() throws Exception {
        // given: 인메모리 대용으로 fake repository 대신 실제 동작 검증은 통합으로,
        // 여기서는 멱등성 핵심 로직(이메일 존재 시 skip)을 검증한다.
        UserRepository repo = org.mockito.Mockito.mock(UserRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        DataSeeder seeder = new DataSeeder(repo, encoder);

        org.mockito.Mockito.when(repo.findByEmail(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Optional.empty());

        seeder.run();

        // 시드 대상(가입자 2 + 직원 1 = 3) 저장 호출
        org.mockito.Mockito.verify(repo, org.mockito.Mockito.times(3))
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void run_이미_존재하면_저장하지_않는다() throws Exception {
        UserRepository repo = org.mockito.Mockito.mock(UserRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        DataSeeder seeder = new DataSeeder(repo, encoder);

        org.mockito.Mockito.when(repo.findByEmail(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Optional.of(
                        new com.distribution.insurance.domain.user.Policyholder(
                                "x", "x", "x", "x", "x",
                                java.time.LocalDate.now(), "x", "x")));

        seeder.run();

        org.mockito.Mockito.verify(repo, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any());
        assertThat(true).isTrue();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests DataSeederTest`
Expected: 컴파일 실패 (`DataSeeder` 미존재)

- [ ] **Step 3: DataSeeder 구현**

`DataSeeder.java`:

```java
package com.distribution.insurance.config;

import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        seedPolicyholder("hong@test.com", "홍길동", "010-1111-1111",
                "900101-1234567", LocalDate.of(1990, 1, 1), "서울시 강남구", "110-111-111111");
        seedPolicyholder("kim@test.com", "김보험", "010-2222-2222",
                "850505-2345678", LocalDate.of(1985, 5, 5), "부산시 해운대구", "220-222-222222");
        seedEmployee("staff@test.com", "이심사", "010-9999-9999", "심사팀");
    }

    private void seedPolicyholder(String email, String name, String phone,
                                  String ssn, LocalDate birthDate, String address, String bankAccount) {
        if (userRepository.findByEmail(email).isPresent()) return;
        userRepository.save(new Policyholder(
                name, email, phone, encoder.encode("1234"), ssn, birthDate, address, bankAccount));
    }

    private void seedEmployee(String email, String name, String phone, String department) {
        if (userRepository.findByEmail(email).isPresent()) return;
        userRepository.save(new InsuranceEmployee(
                name, email, phone, encoder.encode("1234"), department, 0));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests DataSeederTest`
Expected: PASS (2 tests)

- [ ] **Step 5: 전체 테스트 회귀 확인**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL (모든 테스트 통과)

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/distribution/insurance/config backend/src/test/java/com/distribution/insurance/config
git commit -m "feat(epic0): DataSeeder 데모 시드 계정(멱등)"
```

---

## Task 11: 수동 검증 (도커 MySQL + 실제 기동)

**Files:** (코드 변경 없음 — 검증만)

- [ ] **Step 1: 도커 MySQL 기동**

Run: `docker compose up -d`
Expected: `insurance-mysql` 컨테이너 실행

- [ ] **Step 2: 앱 기동**

Run: `cd backend && ./gradlew bootRun`
Expected: 기동 로그에 시드 동작, `users` 테이블 생성

- [ ] **Step 3: 로그인 → 토큰 확인**

Run:
```bash
curl -s -X POST localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"hong@test.com","password":"1234"}'
```
Expected: `{"token":"eyJ..."}`

- [ ] **Step 4: 토큰으로 내 정보 조회**

Run:
```bash
TOKEN=<위에서 받은 토큰>
curl -s localhost:8080/me -H "Authorization: Bearer $TOKEN"
```
Expected: 홍길동 프로필 JSON

- [ ] **Step 5: 개인정보 수정(UC06)**

Run:
```bash
curl -s -X PUT localhost:8080/me/profile -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"new@test.com","phone":"010-0000","address":"새주소","bankAccount":"999-999"}'
```
Expected: 변경된 프로필 JSON + 서버 로그에 "개인정보 변경" 한 줄

- [ ] **Step 6: 정리/커밋 (필요 시 README/docs 갱신)**

Epic 0 완료. `finishing-a-development-branch`로 마무리 검토.

---

## Self-Review 결과

- **Spec 커버리지:** 로그인/로그아웃(Task 4·6), JWT(Task 3·5), 시드(Task 10), UC06 수정+본인인증 목+로그 이력(Task 7·8·9), SINGLE_TABLE 상속+Long PK(Task 1) — spec 결정 테이블 전 항목이 태스크에 매핑됨. 제외 항목(회원가입/E1/이력테이블/RBAC)은 의도적으로 태스크 없음.
- **플레이스홀더:** 없음. 모든 step에 실제 코드/명령 포함.
- **타입 일관성:** `updateContact(email,phone)`, `updateProfile(address,bankAccount)`, `checkPassword(raw,encoder)`, `createToken(Long,String)`, `getUserId/getUserType`, `verify(Long)`, `updateProfile(userId,email,phone,address,bankAccount)` 시그니처가 정의-사용 간 일치.
- **용어:** User/Policyholder/InsuranceEmployee만 사용, 금지 동의어 없음.
