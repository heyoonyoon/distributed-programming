package uc;

import claim.Claim;
import claim.HealthInsuranceClaim;
import common.enums.EClaimStatus;
import contract.Notice;
import external.AccidentHistory;
import external.AccidentRecord;
import payment.BenefitPayment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class UcSystem {

    public static void uc15AccidentHistoryInquiry(Scanner scanner) {
        System.out.println("\n=== 자동차 사고 이력 조회 (금융감독원 연동) ===");

        System.out.print("조회할 주민등록번호 입력: ");
        scanner.nextLine();
        String ssn = scanner.nextLine().trim();

        System.out.println("\n[금융감독원 사고 이력 조회 중...]");
        AccidentHistory history = AccidentHistory.fetch(ssn);

        if (history == null) {
            System.out.println("정보 조회 실패.");
            System.out.println("1. 재시도  2. 직접 입력 전환");
            System.out.print("선택: ");
            int retry = scanner.nextInt();
            scanner.nextLine();
            if (retry == 1) {
                history = AccidentHistory.fetch(ssn);
            } else {
                System.out.println("\n[직접 입력]");
                System.out.print("사고 건수: ");
                int accidentCount = scanner.nextInt();
                System.out.print("지급액 합계: ");
                int totalPaid = scanner.nextInt();
                scanner.nextLine();
                System.out.print("면허 상태 (VALID/SUSPENDED/REVOKED): ");
                String licenseStatus = scanner.nextLine().trim();
                history = new AccidentHistory(ssn, accidentCount, totalPaid, licenseStatus);
            }
        }

        if (history == null) {
            System.out.println("사고 이력 조회에 실패하였습니다.");
            return;
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        System.out.println("\n=== 사고 이력 조회 결과 ===");
        System.out.println("주민등록번호 : " + ssn);
        System.out.println("사고 건수     : " + history.getAccidentCount() + "건");
        System.out.println("지급액 합계   : " + String.format("%,d", history.getTotalPaidAmount()) + "원");
        System.out.println("면허 상태     : " + history.getLicenseStatus());
        if (!history.getRecords().isEmpty()) {
            System.out.println("\n[사고 상세 이력]");
            for (AccidentRecord r : history.getRecords()) {
                System.out.println("  - 날짜: " + sdf.format(r.getAccidentDate())
                        + " / 유형: " + r.getAccidentType()
                        + " / 지급액: " + String.format("%,d", r.getPaidAmount()) + "원");
            }
        }
        System.out.println("\n조회가 완료되었습니다.");
    }

    public static void uc16SendUnpaidNotice(Scanner scanner) {
        System.out.println("\n=== 미납 고지서 발송 (시스템 자동 스케줄러) ===");

        System.out.println("\n[미납 내역 확인 중...]");
        if (AppData.notices.isEmpty()) {
            System.out.println("현재 미납 건이 없습니다.");
            return;
        }

        System.out.println("\n[미납 대상 계약 목록]");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        for (Notice n : AppData.notices) {
            String warning = n.isTerminationWarning() ? " [해지 예고]" : "";
            System.out.println("- [" + n.getNoticeId() + "] " + n.getContractName()
                    + " | 미납액: " + String.format("%,d", n.getDueAmount()) + "원"
                    + " | 연체일수: " + n.getOverdueDays() + "일" + warning);
        }

        System.out.println("\n[고지서 발송 시작]");
        int successCount = 0;
        int failCount = 0;
        for (Notice n : AppData.notices) {
            System.out.println("\n▶ [" + n.getNoticeId() + "] " + n.getContractName());
            System.out.println("  납부 기한  : " + sdf.format(n.getDueDate()));
            System.out.println("  미납 금액  : " + String.format("%,d", n.getDueAmount()) + "원");
            System.out.println("  연체 이자  : " + String.format("%,d", n.getInterest()) + "원");
            System.out.println("  납부 방법  : 계좌이체 / 카드 결제 / 자동이체 등록");

            if (n.isTerminationWarning()) {
                System.out.println("  ⚠ 연체 " + n.getOverdueDays() + "일 초과: 계약이 해지될 수 있습니다.");
                System.out.println("  → 보험사 담당자에게 별도 알림을 전송하였습니다.");
            }

            boolean sent = false;
            for (int attempt = 1; attempt <= 3; attempt++) {
                sent = n.send(AppData.currentHolder.getEmail(), AppData.currentHolder.getPhone());
                if (sent) break;
                System.out.println("  발송 실패 (시도 " + attempt + "/3)...");
            }

            if (sent) {
                System.out.println("  → " + AppData.currentHolder.getEmail() + " / "
                        + AppData.currentHolder.getPhone() + " 발송 완료 (" + sdf.format(new Date()) + ")");
                successCount++;
            } else {
                System.out.println("  → 발송 실패: 시스템 관리자에게 알리고 실패 기록을 남겼습니다.");
                failCount++;
            }
        }

        System.out.println("\n[발송 완료] 성공: " + successCount + "건 / 실패: " + failCount + "건");
    }

    public static void uc17BenefitPayment(Scanner scanner) {
        System.out.println("\n=== 보험금 지급 (시스템 자동) ===");

        List<HealthInsuranceClaim> approvedClaims = new ArrayList<>();
        for (Claim c : AppData.claims) {
            if (c instanceof HealthInsuranceClaim && c.getStatus() == EClaimStatus.APPROVED) {
                HealthInsuranceClaim hc = (HealthInsuranceClaim) c;
                if (hc.getPaidAmount() > 0) {
                    approvedClaims.add(hc);
                }
            }
        }

        if (approvedClaims.isEmpty()) {
            System.out.println("지급 대기 중인 승인 청구 건이 없습니다.");
            return;
        }

        System.out.println("\n[지급 대기 청구 목록]");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (HealthInsuranceClaim hc : approvedClaims) {
            System.out.println("- [" + hc.getClaimId() + "] 지급 예정액: "
                    + String.format("%,d", hc.getPaidAmount()) + "원");
        }

        System.out.println("\n[보험금 송금 처리 중...]");
        for (HealthInsuranceClaim hc : approvedClaims) {
            System.out.println("\n▶ 청구 ID: " + hc.getClaimId());
            System.out.println("  지급 금액 : " + String.format("%,d", hc.getPaidAmount()) + "원");
            System.out.println("  계좌 정보 : " + AppData.currentHolder.getBankAccount());

            String paymentId = "BPY-" + System.currentTimeMillis();
            BenefitPayment bp = new BenefitPayment(paymentId, hc.getPaidAmount(), AppData.currentHolder.getBankAccount());
            boolean transferred = bp.transfer();

            if (!transferred) {
                System.out.println("  ✗ 지급 실패: 계좌 오류 또는 은행 시스템 문제");
                System.out.println("  → 보험사 직원에게 송금 실패 알림 전송");
                System.out.println("  → 보험사 담당자에게 송금 실패 알림 전송 완료");
            } else {
                System.out.println("  ✓ 송금 완료");
                System.out.println("  지급 ID   : " + bp.getPaymentId());
                System.out.println("  지급 일시  : " + sdf.format(bp.getPaidAt()));
                System.out.println("  입금 계좌  : " + bp.getBankAccount());
                bp.notifyPolicyholder(AppData.currentHolder.getEmail(), AppData.currentHolder.getPhone());
                hc.setStatus(EClaimStatus.APPROVED);
            }
        }

        System.out.println("\n[보험금 지급 처리 완료]");
    }
}
