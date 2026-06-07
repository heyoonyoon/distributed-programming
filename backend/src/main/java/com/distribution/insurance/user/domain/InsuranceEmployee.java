package com.distribution.insurance.user.domain;

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

    /** 새 심사 건 배정 시 업무량 증가(UC14). */
    public void assignWork() {
        this.currentLoad += 1;
    }

    /** 심사 건 해제 시 업무량 감소(재배정 시 이전 담당자 부하 회수). */
    public void releaseWork() {
        this.currentLoad = Math.max(0, this.currentLoad - 1);
    }
}
