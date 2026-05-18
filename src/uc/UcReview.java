package uc;

import claim.Claim;
import claim.HealthInsuranceClaim;
import common.enums.EApplicationStatus;
import common.enums.EClaimStatus;
import common.enums.EReviewResult;
import contract.InsuranceApplication;
import external.AccidentHistory;
import external.AccidentRecord;
import payment.BenefitPayment;
import product.InsuranceProduct;
import review.BenefitPaymentReview;
import review.EnrollmentReview;
import user.InsuranceEmployee;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UcReview {

    public static void uc12BenefitPaymentReview(Scanner scanner) {
        System.out.println("\n=== 보험금 지급 심사 ===");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        List<BenefitPaymentReview> pending = new ArrayList<>();
        for (BenefitPaymentReview r : AppData.benefitReviews) {
            if (r.getResult() == null) pending.add(r);
        }

        if (pending.isEmpty()) {
            System.out.println("심사 대기 중인 건이 없습니다.");
            return;
        }

        System.out.println("\n번호 | 접수번호   | 청구인   | 청구 금액       | 청구 사유                | 접수일");
        System.out.println("-----|-----------|---------|----------------|------------------------|----------");
        for (int i = 0; i < pending.size(); i++) {
            BenefitPaymentReview r = pending.get(i);
            String claimId = r.getClaimId();
            HealthInsuranceClaim claim = findHealthClaim(claimId);
            if (claim == null) continue;
            System.out.printf("%-4d | %-9s | %-7s | %,13d원 | %-22s | %s%n",
                    i + 1, claimId, AppData.currentHolder.getName(),
                    claim.getRequestAmount(), claim.getClaimReason(),
                    sdf.format(claim.getClaimDate()));
        }

        System.out.print("\n심사할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > pending.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        BenefitPaymentReview review = pending.get(choice - 1);

        if (review.isLocked()) {
            System.out.println("현재 담당자 [" + review.getAssignedStaffId() + "]가 처리 중인 건입니다.");
            return;
        }
        review.lock();

        HealthInsuranceClaim claim = findHealthClaim(review.getClaimId());

        System.out.println("\n[청구 상세 정보]");
        System.out.println("접수번호  : " + claim.getClaimId());
        System.out.println("청구인    : " + AppData.currentHolder.getName());
        System.out.println("청구 금액 : " + String.format("%,d", claim.getRequestAmount()) + "원");
        System.out.println("청구 사유 : " + claim.getClaimReason());
        System.out.println("접수일    : " + sdf.format(claim.getClaimDate()));
        System.out.println("제출 서류 : " + claim.getDocuments());

        System.out.println("\n[담당자 정보]");
        System.out.println("담당자 ID : " + review.getAssignedStaffId());
        System.out.println("담당자명  : " + review.getAssignedStaffName());

        System.out.println("\n[심사 결과 선택]");
        System.out.println("1. 승인  2. 반려");
        System.out.print("선택: ");
        int resultChoice = scanner.nextInt();
        scanner.nextLine();

        if (resultChoice == 2) {
            System.out.print("반려 사유 입력: ");
            String reason = scanner.nextLine().trim();
            review.reject(reason);
            System.out.println("\n=== 심사 결과: 반려 ===");
            System.out.println("사유: " + reason);
            System.out.println("보험가입자에게 반려 사유를 포함한 결과 통보를 발송하였습니다.");
        } else {
            System.out.print("지급 승인 금액 입력 (청구 금액: " + String.format("%,d", claim.getRequestAmount()) + "원): ");
            int approvedAmount = scanner.nextInt();
            scanner.nextLine();

            while (true) {
                System.out.println("\n심사 확정하시겠습니까? (1.확정 / 2.취소): ");
                int confirm = scanner.nextInt();
                scanner.nextLine();
                if (confirm == 2) {
                    review.unlock();
                    System.out.println("심사가 취소되었습니다.");
                    return;
                }
                if (confirm == 1) break;
            }

            BenefitPayment payment = review.approve(approvedAmount, AppData.currentHolder.getBankAccount());
            claim.setStatus(EClaimStatus.APPROVED);
            claim.setPaidAmount(approvedAmount);

            System.out.println("\n=== 심사 결과: 승인 ===");
            System.out.println("지급 번호 : " + payment.getPaymentId());
            System.out.println("지급 금액 : " + String.format("%,d", payment.getPaidAmount()) + "원");
            System.out.println("지급 계좌 : " + AppData.currentHolder.getBankAccount());
            System.out.println("지급 일시 : " + sdf.format(payment.getPaidAt()));
            System.out.println("\n보험가입자에게 심사 결과 통보 이메일/문자를 발송하였습니다.");
        }

        review.unlock();
    }

    public static void uc14AssignStaff(Scanner scanner) {
        System.out.println("\n=== 담당자 지정 ===");

        List<BenefitPaymentReview> pending = new ArrayList<>();
        for (BenefitPaymentReview r : AppData.benefitReviews) {
            if (r.getResult() == null) pending.add(r);
        }

        if (pending.isEmpty()) {
            System.out.println("담당자 배정이 필요한 심사 건이 없습니다.");
            return;
        }

        System.out.println("\n번호 | 심사번호   | 청구번호   | 현재 담당자");
        System.out.println("-----|-----------|-----------|------------");
        for (int i = 0; i < pending.size(); i++) {
            BenefitPaymentReview r = pending.get(i);
            System.out.printf("%-4d | %-9s | %-9s | %s(%s)%n",
                    i + 1, r.getReviewId(), r.getClaimId(),
                    r.getAssignedStaffName(), r.getAssignedStaffId());
        }

        System.out.print("\n배정할 건 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > pending.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }
        BenefitPaymentReview review = pending.get(choice - 1);

        System.out.println("\n1. 자동 배정  2. 수동 배정 (A1)");
        System.out.print("선택: ");
        int mode = scanner.nextInt();
        scanner.nextLine();

        InsuranceEmployee assigned = null;

        if (mode == 1) {
            for (InsuranceEmployee e : AppData.employees) {
                if (assigned == null || e.getCurrentLoad() < assigned.getCurrentLoad()) {
                    assigned = e;
                }
            }
            if (assigned == null) {
                System.out.println("배정 가능한 담당자가 없습니다. 관리자에게 알림을 발송하였습니다.");
                return;
            }
            System.out.println("\n[자동 배정 결과]");
            System.out.println("선정 기준: 현재 업무량 최소");
            System.out.println("배정 직원: " + assigned.getName() + " (" + assigned.getDepartment() + ", 현재 " + assigned.getCurrentLoad() + "건)");
        } else {
            System.out.println("\n[배정 가능한 직원 목록]");
            System.out.println("번호 | 이름   | 부서         | 현재 담당 건수");
            System.out.println("-----|-------|-------------|---------------");
            for (int i = 0; i < AppData.employees.size(); i++) {
                InsuranceEmployee e = AppData.employees.get(i);
                System.out.printf("%-4d | %-5s | %-11s | %d건%n",
                        i + 1, e.getName(), e.getDepartment(), e.getCurrentLoad());
            }
            if (AppData.employees.isEmpty()) {
                System.out.println("배정 가능한 담당자가 없습니다. 관리자에게 알림을 발송하였습니다.");
                return;
            }
            System.out.print("\n배정할 직원 번호: ");
            int empChoice = scanner.nextInt();
            scanner.nextLine();
            if (empChoice < 1 || empChoice > AppData.employees.size()) {
                System.out.println("잘못된 입력입니다.");
                return;
            }
            assigned = AppData.employees.get(empChoice - 1);
        }

        review.assignStaff(assigned);
        System.out.println("\n\"새 심사 건이 배정되었습니다.\" 알림을 [" + assigned.getName() + "]에게 발송하였습니다.");
        System.out.println("\n[청구 건 담당자 정보]");
        System.out.println("청구번호  : " + review.getClaimId());
        System.out.println("담당자    : " + review.getAssignedStaffName() + " (" + review.getAssignedStaffId() + ")");
        System.out.println("부서      : " + assigned.getDepartment());
    }

    public static void uc13EnrollmentReview(Scanner scanner) {
        System.out.println("\n=== 보험 가입 심사 ===");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

        List<InsuranceApplication> pending = new ArrayList<>();
        for (InsuranceApplication a : AppData.applications) {
            if (a.getStatus() == EApplicationStatus.PENDING) pending.add(a);
        }

        if (pending.isEmpty()) {
            System.out.println("심사 대기 중인 가입 신청이 없습니다.");
            return;
        }

        System.out.println("\n번호 | 접수번호   | 가입자명 | 보험 종류      | 상품ID | 접수일시");
        System.out.println("-----|-----------|---------|--------------|--------|------------------");
        for (int i = 0; i < pending.size(); i++) {
            InsuranceApplication a = pending.get(i);
            String type = a.getVehicleInfo() != null ? "자동차보험" : "의료보험";
            System.out.printf("%-4d | %-9s | %-7s | %-12s | %-6s | %s%n",
                    i + 1, a.getApplicationId(), a.getHolderName(), type, a.getProductId(),
                    sdf.format(a.getAppliedAt()));
        }

        System.out.print("\n심사할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > pending.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        InsuranceApplication app = pending.get(choice - 1);

        EnrollmentReview existingReview = findEnrollmentReview(app.getApplicationId());
        if (existingReview != null && existingReview.isLocked()) {
            System.out.println("현재 담당자 [" + AppData.currentEmployee.getEmployeeId() + "]가 처리 중인 건입니다.");
            return;
        }

        EnrollmentReview review = existingReview != null ? existingReview
                : new EnrollmentReview("ERV-" + System.currentTimeMillis(), app.getApplicationId());
        if (existingReview == null) AppData.enrollmentReviews.add(review);
        review.lock();

        System.out.println("\n[신청 상세 정보]");
        System.out.println("접수번호  : " + app.getApplicationId());
        System.out.println("가입자명  : " + app.getHolderName());
        System.out.println("주민번호  : " + AppData.currentHolder.getSsn());
        System.out.println("생년월일  : " + AppData.currentHolder.getBirthDate());

        boolean isCarInsurance = app.getVehicleInfo() != null;
        if (isCarInsurance) {
            System.out.println("\n[차량 정보]");
            System.out.println("차량번호 : " + app.getVehicleInfo().getVehicleNumber());
            System.out.println("차량종류 : " + app.getVehicleInfo().getVehicleType());
        } else if (app.getMedicalHistory() != null) {
            System.out.println("\n[의료 고지 항목]");
            System.out.println("기저질환 : " + app.getMedicalHistory().getConditions());
            System.out.println("진료기록 : " + app.getMedicalHistory().getHospitalRecords());
        }

        if (isCarInsurance) {
            System.out.println("\n[UC15 확장: 자동차 사고 이력 조회 중...]");
            AccidentHistory history = review.fetchAccidentHistory(AppData.currentHolder.getSsn());
            if (history == null) {
                System.out.println("정보 조회 실패. 1.재시도  2.직접 입력 전환");
                System.out.print("선택: ");
                int retry = scanner.nextInt();
                scanner.nextLine();
                if (retry == 1) {
                    history = review.fetchAccidentHistory(AppData.currentHolder.getSsn());
                }
            }
            if (history != null) {
                System.out.println("\n[참조 정보 — 금융감독원 사고 이력]");
                System.out.println("사고 건수     : " + history.getAccidentCount() + "건");
                System.out.println("지급액 합계   : " + String.format("%,d", history.getTotalPaidAmount()) + "원");
                System.out.println("면허 상태     : " + history.getLicenseStatus());
                java.text.SimpleDateFormat dateSdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                for (AccidentRecord r : history.getRecords()) {
                    System.out.println("  - " + dateSdf.format(r.getAccidentDate())
                            + " / " + r.getAccidentType()
                            + " / " + String.format("%,d", r.getPaidAmount()) + "원");
                }
            }
        }

        System.out.println("\n[심사 결과 선택]");
        System.out.println("1. 승인  2. 조건부 승인  3. 반려  4. 부서 협의 요청");
        System.out.print("선택: ");
        int resultChoice = scanner.nextInt();
        scanner.nextLine();

        if (resultChoice == 4) {
            System.out.print("협의 사유 입력: ");
            String reason = scanner.nextLine().trim();
            System.out.println("대상 부서 선택: 1.의료심사팀  2.자동차심사팀");
            System.out.print("선택: ");
            int deptChoice = scanner.nextInt();
            scanner.nextLine();
            String dept = deptChoice == 1 ? "의료심사팀" : "자동차심사팀";
            AppData.currentEmployee.requestDeptConsultation(review.getApplicationId(), reason);
            System.out.println("\n[" + dept + "]에 협의 요청이 전송되었습니다.");
            System.out.println("해당 건의 상태가 '협의 중'으로 변경되었습니다.");
            review.unlock();
            return;
        }

        EReviewResult reviewResult;
        String comment = "";
        int finalPremium = 0;

        if (resultChoice == 2) {
            System.out.print("할증율 입력 (예: 20 → +20%): ");
            double rate = scanner.nextDouble();
            scanner.nextLine();
            System.out.print("할증 사유 입력: ");
            comment = scanner.nextLine().trim();

            InsuranceProduct product = findProduct(app.getProductId());
            int base = product != null ? product.getBasePremium() : 0;
            finalPremium = review.applySurcharge(rate, base);

            System.out.println("\n[할증 보험료 재계산]");
            System.out.println("기본 보험료   : " + String.format("%,d", base) + "원");
            System.out.printf("할증율        : +%.0f%%%n", rate);
            System.out.println("최종 보험료   : " + String.format("%,d", finalPremium) + "원");
            System.out.println("\n최종 확정하시겠습니까? (1.확정 / 2.취소)");
            System.out.print("선택: ");
            int confirm = scanner.nextInt();
            scanner.nextLine();
            if (confirm != 1) {
                review.unlock();
                System.out.println("심사가 취소되었습니다.");
                return;
            }
            reviewResult = EReviewResult.CONDITIONAL;
        } else if (resultChoice == 3) {
            System.out.print("반려 사유 입력: ");
            comment = scanner.nextLine().trim();
            reviewResult = EReviewResult.REJECTED;
        } else {
            reviewResult = EReviewResult.APPROVED;
        }

        review.confirm(reviewResult, comment);
        app.setStatus(reviewResult == EReviewResult.REJECTED
                ? EApplicationStatus.REJECTED
                : EApplicationStatus.APPROVED);

        System.out.println("\n=== 심사가 완료되었습니다. ===");
        System.out.println("심사 결과 : " + reviewResult);
        if (reviewResult == EReviewResult.CONDITIONAL) {
            System.out.println("최종 보험료: " + String.format("%,d", finalPremium) + "원");
        }
        System.out.println("가입자에게 안내 이메일/문자를 발송하였습니다.");
        review.unlock();
    }

    private static HealthInsuranceClaim findHealthClaim(String claimId) {
        for (Claim c : AppData.claims) {
            if (c instanceof HealthInsuranceClaim && c.getClaimId().equals(claimId)) {
                return (HealthInsuranceClaim) c;
            }
        }
        return null;
    }

    private static EnrollmentReview findEnrollmentReview(String applicationId) {
        for (EnrollmentReview r : AppData.enrollmentReviews) {
            if (r.getApplicationId().equals(applicationId)) return r;
        }
        return null;
    }

    private static InsuranceProduct findProduct(String productId) {
        for (product.HealthInsuranceProduct p : AppData.healthProducts) {
            if (p.getProductId().equals(productId)) return p;
        }
        for (product.CarInsuranceProduct p : AppData.carProducts) {
            if (p.getProductId().equals(productId)) return p;
        }
        return null;
    }
}
