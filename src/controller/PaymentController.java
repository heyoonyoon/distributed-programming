package controller;

import common.DataStorage;
import common.enums.EPaymentMethod;
import contract.InsuranceContract;
import common.enums.EContractStatus;
import contract.Notice;
import contract.Payment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class PaymentController {
    public static void uc07UnpaidInquiry(Scanner scanner) {
        System.out.println("\n=== 미납 내역 조회 ===");

        // 2단계: 미납 보험료 목록 출력
        // A1: 미납 내역 없음
        if (DataStorage.notices.isEmpty()) {
            System.out.println("미납된 보험료가 없습니다.");
            return;
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        System.out.println("\n번호 | 계약명           | 납부 기한  | 미납 금액    | 연체 일수 | 연체 이자   | 경고");
        System.out.println("-----|-----------------|------------|-------------|---------|------------|-----");
        for (int i = 0; i < DataStorage.notices.size(); i++) {
            Notice n = DataStorage.notices.get(i);
            System.out.printf("%-4d | %-15s | %s | %,9d원 | %6d일 | %,8d원 | %s%n",
                    i + 1, n.getContractName(), sdf.format(n.getDueDate()),
                    n.getDueAmount(), n.getOverdueDays(), n.getInterest(),
                    n.isTerminationWarning() ? "⚠ 계약 해지 위험" : "-");
        }

        // 3단계: 특정 미납 건 상세 확인
        System.out.print("\n상세 확인할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > DataStorage.notices.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        Notice selected = DataStorage.notices.get(choice - 1);

        // 4단계: 납부 방법 안내 + 바로 납부하기
        System.out.println("\n=== 미납 상세 ===");
        System.out.println("계약명    : " + selected.getContractName());
        System.out.println("납부 기한 : " + sdf.format(selected.getDueDate()));
        System.out.println("미납 금액 : " + String.format("%,d", selected.getDueAmount()) + "원");
        System.out.println("연체 일수 : " + selected.getOverdueDays() + "일");
        System.out.println("연체 이자 : " + String.format("%,d", selected.getInterest()) + "원");
        System.out.println("총 납부액 : " + String.format("%,d", selected.getDueAmount() + selected.getInterest()) + "원");
        if (selected.isTerminationWarning()) {
            System.out.println("⚠ 연체 30일 초과 — 계약이 해지될 수 있습니다.");
        }
        System.out.println("\n[납부 방법]");
        System.out.println("  1. 카드 결제   2. 계좌 이체   3. 자동 이체");
        System.out.print("바로 납부하시겠습니까? (1.예 / 2.아니오): ");
        int pay = scanner.nextInt();
        scanner.nextLine();
        if (pay == 1) {
            System.out.println("UC10 보험료 납부 메뉴로 이동하세요. (메뉴 10번)");
        }
    }

    public static void uc10PayPremium(Scanner scanner) {
        System.out.println("\n=== 보험료 납부 ===");

        // 2단계: 납부 예정 보험료 목록 출력 (ACTIVE 계약 기준)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        List<InsuranceContract> activeContracts = new ArrayList<>();
        for (InsuranceContract c : DataStorage.contracts) {
            if (c.getStatus() == EContractStatus.ACTIVE) activeContracts.add(c);
        }

        if (activeContracts.isEmpty()) {
            System.out.println("납부 대상 보험료가 없습니다.");
            return;
        }

        System.out.println("\n번호 | 계약명           | 납부 기한  | 납부 금액");
        System.out.println("-----|-----------------|------------|----------");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
        String dueDate = sdf.format(cal.getTime());
        for (int i = 0; i < activeContracts.size(); i++) {
            InsuranceContract c = activeContracts.get(i);
            System.out.printf("%-4d | %-15s | %s | %,d원%n",
                    i + 1, c.getProductName(), dueDate, c.getMonthlyPremium());
        }

        // 3단계: 납부할 건 선택
        System.out.print("\n납부할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > activeContracts.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }
        InsuranceContract selectedContract = activeContracts.get(choice - 1);

        // 결제 수단 선택
        System.out.println("\n결제 수단 선택");
        System.out.println("1. 신용카드  2. 계좌이체  3. 자동이체 등록");
        System.out.print("선택: ");
        int methodChoice = scanner.nextInt();
        scanner.nextLine();

        // A1: 자동이체 등록
        if (methodChoice == 3) {
            System.out.println("\n--- 자동이체 등록 ---");
            System.out.print("출금 계좌번호: ");
            String autoAccount = scanner.nextLine().trim();
            System.out.print("출금일 (매월 며칠, 1~28): ");
            int autoDay = scanner.nextInt();
            scanner.nextLine();
            System.out.println("자동이체가 등록되었습니다. (계좌: " + autoAccount + ", 매월 " + autoDay + "일)");
            System.out.println("등록 확인 안내를 이메일/문자로 발송하였습니다.");
            return;
        }

        EPaymentMethod method = methodChoice == 1 ? EPaymentMethod.CARD : EPaymentMethod.TRANSFER;

        // 4단계: 결제 정보 입력 + 납부하기 (E1: 결제 실패 시 재시도)
        while (true) {
            if (method == EPaymentMethod.CARD) {
                System.out.print("카드번호 (16자리): ");
                String cardNum = scanner.nextLine().trim();
                if (!cardNum.replace("-", "").matches("\\d{16}")) {
                    System.out.println("결제에 실패하였습니다. (사유: 유효하지 않은 카드번호)");
                    System.out.print("다시 시도하시겠습니까? (1.예 / 2.아니오): ");
                    if (scanner.nextInt() != 1) { scanner.nextLine(); return; }
                    scanner.nextLine();
                    continue;
                }
            } else {
                System.out.print("출금 계좌번호: ");
                String bankNum = scanner.nextLine().trim();
                if (bankNum.isEmpty()) {
                    System.out.println("결제에 실패하였습니다. (사유: 계좌번호 누락)");
                    System.out.print("다시 시도하시겠습니까? (1.예 / 2.아니오): ");
                    if (scanner.nextInt() != 1) { scanner.nextLine(); return; }
                    scanner.nextLine();
                    continue;
                }
            }
            break;
        }

        // 5단계: 결제 처리 + 납부 내역 기록
        String paymentId = "PAY-" + System.currentTimeMillis();
        Payment payment = new Payment(paymentId, selectedContract.getMonthlyPremium(), method);
        payment.process();
        common.vo.Receipt receipt = payment.getReceipt();

        // 미납 고지서가 있으면 제거
        DataStorage.notices.removeIf(n -> n.getContractName().equals(selectedContract.getProductName()));

        // 6단계: 납부 완료 영수증 발송
        System.out.println("\n=== 납부 완료 ===");
        System.out.println("영수증 번호: " + receipt.getPaymentId());
        System.out.println("납부 계약  : " + selectedContract.getProductName());
        System.out.println("납부 금액  : " + String.format("%,d", receipt.getAmount()) + "원");
        System.out.println("납부 수단  : " + (method == EPaymentMethod.CARD ? "신용카드" : "계좌이체"));
        System.out.println("납부 일시  : " + sdf.format(receipt.getPaidAt()));
        System.out.println("납부 완료 영수증을 이메일/문자로 발송하였습니다.");
    }

    public static void uc16SendUnpaidNotice(Scanner scanner) {
        System.out.println("\n=== 미납 고지서 발송 (시스템 자동 스케줄러) ===");

        // 1~2단계: 미납 건 조회
        System.out.println("\n[미납 내역 확인 중...]");
        if (DataStorage.notices.isEmpty()) {
            System.out.println("현재 미납 건이 없습니다.");
            return;
        }

        System.out.println("\n[미납 대상 계약 목록]");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        for (Notice n : DataStorage.notices) {
            String warning = n.isTerminationWarning() ? " [해지 예고]" : "";
            System.out.println("- [" + n.getNoticeId() + "] " + n.getContractName()
                    + " | 미납액: " + String.format("%,d", n.getDueAmount()) + "원"
                    + " | 연체일수: " + n.getOverdueDays() + "일" + warning);
        }

        // 3~4단계: 고지서 생성 및 발송
        System.out.println("\n[고지서 발송 시작]");
        int successCount = 0;
        int failCount = 0;
        for (Notice n : DataStorage.notices) {
            // 3단계: 고지서 구성
            System.out.println("\n▶ [" + n.getNoticeId() + "] " + n.getContractName());
            System.out.println("  납부 기한  : " + sdf.format(n.getDueDate()));
            System.out.println("  미납 금액  : " + String.format("%,d", n.getDueAmount()) + "원");
            System.out.println("  연체 이자  : " + String.format("%,d", n.getInterest()) + "원");
            System.out.println("  납부 방법  : 계좌이체 / 카드 결제 / 자동이체 등록");

            // A1: 30일 초과 → 해지 예고 문구
            if (n.isTerminationWarning()) {
                System.out.println("  ⚠ 연체 " + n.getOverdueDays() + "일 초과: 계약이 해지될 수 있습니다.");
                System.out.println("  → 보험사 담당자에게 별도 알림을 전송하였습니다.");
            }

            // 4단계: 발송 (E1: 최대 3회 재시도)
            boolean sent = false;
            for (int attempt = 1; attempt <= 3; attempt++) {
                sent = n.send(DataStorage.currentHolder.getEmail(), DataStorage.currentHolder.getPhone());
                if (sent) break;
                System.out.println("  발송 실패 (시도 " + attempt + "/3)...");
            }

            // 5단계: 발송 기록
            if (sent) {
                System.out.println("  → " + DataStorage.currentHolder.getEmail() + " / "
                        + DataStorage.currentHolder.getPhone() + " 발송 완료 (" + sdf.format(new Date()) + ")");
                successCount++;
            } else {
                // E1-2: 관리자 알림
                System.out.println("  → 발송 실패: 시스템 관리자에게 알리고 실패 기록을 남겼습니다.");
                failCount++;
            }
        }

        System.out.println("\n[발송 완료] 성공: " + successCount + "건 / 실패: " + failCount + "건");
    }
}
