package com.distribution.insurance.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    /** 비밀번호 일치 여부(순수 도메인 비교) — 로그인 조율은 AuthService(ADR 0001). */
    public boolean checkPassword(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.password);
    }
}
