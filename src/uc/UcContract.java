package uc;

import claim.Claim;
import common.enums.EClaimStatus;
import common.enums.EContractStatus;
import common.enums.EPaymentMethod;
import contract.InsuranceContract;
import contract.Notice;
import contract.Payment;
import product.CoverageItem;
import product.InsuranceProduct;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class UcContract {

    public static void uc07UnpaidInquiry(Scanner scanner) {
        System.out.println("\n=== 미납 내역 조회 ===");

        if (AppData.notices.isEmpty()) {
            System.out.println("미납된 보험료가 없습니다.");
            return;
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        System.out.println("\n번호 | 계약명           | 납부 기한  | 미납 금액    | 연체 일수 | 연체 이자   | 경고");
        System.out.println("-----|-----------------|------------|-------------|---------|------------|-----");
        for (int i = 0; i < AppData.notices.size(); i++) {
            Notice n = AppData.notices.get(i);
            System.out.printf("%-4d | %-15s | %s | %,9d원 | %6d일 | %,8d원 | %s%n",
                    i + 1, n.getContractName(), sdf.format(n.getDueDate()),
                    n.getDueAmount(), n.getOverdueDays(), n.getInterest(),
                    n.isTerminationWarning() ? "⚠ 계약 해지 위험" : "-");
        }

        System.out.print("\n상세 확인할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > AppData.notices.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        Notice selected = AppData.notices.get(choice - 1);
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

    public static void uc08ContractInquiry(Scanner scanner) {
        System.out.println("\n=== 계약 내용 조회 ===");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        List<InsuranceContract> active = new ArrayList<>();
        for (InsuranceContract c : AppData.contracts) {
            if (c.getStatus() == EContractStatus.ACTIVE) active.add(c);
        }

        if (active.isEmpty()) {
            System.out.println("현재 유효한 계약이 없습니다.");
            return;
        }

        System.out.println("\n번호 | 계약번호  | 상품명           | 종류      | 계약 기간                        | 월 보험료");
        System.out.println("-----|----------|-----------------|---------|--------------------------------|----------");
        for (int i = 0; i < active.size(); i++) {
            InsuranceContract c = active.get(i);
            System.out.printf("%-4d | %-8s | %-15s | %-7s | %s ~ %s | %,d원%n",
                    i + 1, c.getContractId(), c.getProductName(), c.getProductType(),
                    sdf.format(c.getStartDate()), sdf.format(c.getEndDate()), c.getMonthlyPremium());
        }

        System.out.print("\n상세보기할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > active.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        InsuranceContract selected = active.get(choice - 1);
        System.out.println("\n=== 계약 상세 정보 ===");
        System.out.println("계약번호  : " + selected.getContractId());
        System.out.println("상품명    : " + selected.getProductName());
        System.out.println("보험 종류 : " + selected.getProductType());
        System.out.println("계약 기간 : " + sdf.format(selected.getStartDate()) + " ~ " + sdf.format(selected.getEndDate()));
        System.out.println("월 보험료 : " + String.format("%,d", selected.getMonthlyPremium()) + "원");
        System.out.println("납입 방법 : " + selected.getPaymentMethod());
        System.out.println("수익자    : " + selected.getBeneficiary());
        System.out.println("특약 사항 : " + selected.getSpecialTerms());

        System.out.println("\n[보장 항목]");
        List<? extends InsuranceProduct> productList =
                selected.getProductType().equals("의료보험") ? AppData.healthProducts : AppData.carProducts;
        for (InsuranceProduct p : productList) {
            if (p.getProductName().equals(selected.getProductName())) {
                for (CoverageItem item : p.getCoverageItems()) {
                    System.out.printf("  - %s | 보장 한도: %,d원 | 면책금액: %,d원%n",
                            item.getItemName(), item.getCoverageLimit(), item.getDeductible());
                }
                break;
            }
        }

        System.out.print("\n계약서를 다운로드하시겠습니까? (1.예 / 2.아니오): ");
        int dl = scanner.nextInt();
        scanner.nextLine();
        if (dl == 1) {
            System.out.println("계약서 파일이 생성되었습니다: " + selected.getContractId() + "_계약서.pdf");
        }
    }

    public static void uc10PayPremium(Scanner scanner) {
        System.out.println("\n=== 보험료 납부 ===");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        List<InsuranceContract> activeContracts = new ArrayList<>();
        for (InsuranceContract c : AppData.contracts) {
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

        System.out.print("\n납부할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > activeContracts.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }
        InsuranceContract selectedContract = activeContracts.get(choice - 1);

        System.out.println("\n결제 수단 선택");
        System.out.println("1. 신용카드  2. 계좌이체  3. 자동이체 등록");
        System.out.print("선택: ");
        int methodChoice = scanner.nextInt();
        scanner.nextLine();

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

        String paymentId = "PAY-" + System.currentTimeMillis();
        Payment payment = new Payment(paymentId, selectedContract.getMonthlyPremium(), method);
        payment.process();
        common.vo.Receipt receipt = payment.getReceipt();

        AppData.notices.removeIf(n -> n.getContractName().equals(selectedContract.getProductName()));

        System.out.println("\n=== 납부 완료 ===");
        System.out.println("영수증 번호: " + receipt.getPaymentId());
        System.out.println("납부 계약  : " + selectedContract.getProductName());
        System.out.println("납부 금액  : " + String.format("%,d", receipt.getAmount()) + "원");
        System.out.println("납부 수단  : " + (method == EPaymentMethod.CARD ? "신용카드" : "계좌이체"));
        System.out.println("납부 일시  : " + sdf.format(receipt.getPaidAt()));
        System.out.println("납부 완료 영수증을 이메일/문자로 발송하였습니다.");
    }

    public static void uc11ProfitAnalysis(Scanner scanner) {
        System.out.println("\n=== 보험 처리 실익 분석 ===");

        if (AppData.contracts.isEmpty()) {
            System.out.println("보유 중인 계약이 없습니다.");
            return;
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        System.out.println("\n[계약 목록]");
        System.out.println("번호 | 계약명           | 시작일     | 월 보험료");
        System.out.println("-----|-----------------|------------|----------");
        for (int i = 0; i < AppData.contracts.size(); i++) {
            InsuranceContract c = AppData.contracts.get(i);
            System.out.printf("%-4d | %-15s | %s | %,d원%n",
                    i + 1, c.getProductName(), sdf.format(c.getStartDate()), c.getMonthlyPremium());
        }

        System.out.print("\n분석할 계약 번호 (0.뒤로): ");
        int contractChoice = scanner.nextInt();
        scanner.nextLine();
        if (contractChoice == 0) return;
        if (contractChoice < 1 || contractChoice > AppData.contracts.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }
        InsuranceContract selected = AppData.contracts.get(contractChoice - 1);

        long diffMs = new Date().getTime() - selected.getStartDate().getTime();
        long diffMonths = diffMs / (1000L * 60 * 60 * 24 * 30);
        if (diffMonths < 6) {
            System.out.println("분석에 필요한 데이터가 충분하지 않습니다. 6개월 이후 이용 가능합니다.");
            return;
        }

        System.out.println("\n[분석 기간 선택]");
        System.out.println("1. 전체 기간  2. 최근 6개월  3. 최근 1년  4. 최근 2년");
        System.out.print("선택: ");
        int periodChoice = scanner.nextInt();
        scanner.nextLine();

        long analysisMonths;
        String periodLabel;
        switch (periodChoice) {
            case 2: analysisMonths = 6;  periodLabel = "최근 6개월"; break;
            case 3: analysisMonths = 12; periodLabel = "최근 1년";   break;
            case 4: analysisMonths = 24; periodLabel = "최근 2년";   break;
            default: analysisMonths = diffMonths; periodLabel = "전체 기간 (" + diffMonths + "개월)"; break;
        }
        if (analysisMonths > diffMonths) analysisMonths = diffMonths;

        int totalPaid = (int)(selected.getMonthlyPremium() * analysisMonths);

        Date periodStart = new Date(new Date().getTime() - analysisMonths * 30L * 24 * 60 * 60 * 1000);
        int totalReceived = 0;
        for (Claim claim : AppData.claims) {
            if (claim.getStatus() == EClaimStatus.APPROVED && claim.getPaidAmount() > 0) {
                if (!claim.getClaimDate().before(periodStart)) {
                    totalReceived += claim.getPaidAmount();
                }
            }
        }

        int netBenefit = totalReceived - totalPaid;
        double profitRate = totalPaid > 0 ? (double) totalReceived / totalPaid * 100 : 0;

        System.out.println("\n=== 보험 처리 실익 분석 결과 ===");
        System.out.println("계약명    : " + selected.getProductName());
        System.out.println("분석 기간 : " + periodLabel);
        System.out.println();

        int maxBar = 30;
        int paidBar = totalPaid > 0 ? Math.max(1, (int)((double) totalPaid / Math.max(totalPaid, totalReceived) * maxBar)) : 0;
        int recvBar = totalReceived > 0 ? Math.max(1, (int)((double) totalReceived / Math.max(totalPaid, totalReceived) * maxBar)) : 0;

        System.out.println("[납입 보험료] " + repeat("█", paidBar) + " " + String.format("%,d원", totalPaid));
        System.out.println("[수령 보험금] " + repeat("█", recvBar) + " " + String.format("%,d원", totalReceived));
        System.out.println();
        System.out.printf("실익 금액 : %s%,d원%n", netBenefit >= 0 ? "+" : "", netBenefit);
        System.out.printf("실익률    : %.1f%%%n", profitRate);

        if (netBenefit >= 0) {
            System.out.println("→ 납입 보험료 대비 보험금 수령이 많습니다. (이익)");
        } else {
            System.out.println("→ 아직 수령 보험금이 납입 보험료보다 적습니다.");
        }

        System.out.println("\n[유사 가입자 비교]");
        double avgProfitRate = 45.0;
        System.out.printf("유사 가입자 평균 실익률 : %.1f%%%n", avgProfitRate);
        System.out.printf("나의 실익률             : %.1f%%%n", profitRate);
        if (profitRate >= avgProfitRate) {
            System.out.println("→ 유사 가입자 평균보다 높은 실익률입니다.");
        } else {
            System.out.println("→ 유사 가입자 평균보다 낮은 실익률입니다.");
        }
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
