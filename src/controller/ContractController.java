package controller;

import claim.Claim;
import common.DataStorage;
import common.enums.EClaimStatus;
import common.enums.EContractStatus;
import contract.InsuranceContract;
import product.CoverageItem;
import product.InsuranceProduct;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class ContractController {
    public static void uc08ContractInquiry(Scanner scanner) {
        System.out.println("\n=== 계약 내용 조회 ===");

        // 2단계: 유효 계약 목록 출력 (ACTIVE만)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        List<InsuranceContract> active = new ArrayList<>();
        for (InsuranceContract c : DataStorage.contracts) {
            if (c.getStatus() == EContractStatus.ACTIVE) active.add(c);
        }

        // A1: 유효 계약 없음
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

        // 3단계: 상세보기 선택
        System.out.print("\n상세보기할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > active.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        InsuranceContract selected = active.get(choice - 1);

        // 4단계: 상세 내용 출력
        System.out.println("\n=== 계약 상세 정보 ===");
        System.out.println("계약번호  : " + selected.getContractId());
        System.out.println("상품명    : " + selected.getProductName());
        System.out.println("보험 종류 : " + selected.getProductType());
        System.out.println("계약 기간 : " + sdf.format(selected.getStartDate()) + " ~ " + sdf.format(selected.getEndDate()));
        System.out.println("월 보험료 : " + String.format("%,d", selected.getMonthlyPremium()) + "원");
        System.out.println("납입 방법 : " + selected.getPaymentMethod());
        System.out.println("수익자    : " + selected.getBeneficiary());
        System.out.println("특약 사항 : " + selected.getSpecialTerms());

        // 보장 항목 출력 (상품 데이터에서 조회)
        System.out.println("\n[보장 항목]");
        List<? extends InsuranceProduct> productList =
                selected.getProductType().equals("의료보험") ? DataStorage.healthProducts : DataStorage.carProducts;
        for (InsuranceProduct p : productList) {
            if (p.getProductName().equals(selected.getProductName())) {
                for (CoverageItem item : p.getCoverageItems()) {
                    System.out.printf("  - %s | 보장 한도: %,d원 | 면책금액: %,d원%n",
                            item.getItemName(), item.getCoverageLimit(), item.getDeductible());
                }
                break;
            }
        }

        // 5~6단계: 계약서 다운로드 (선택 사항)
        System.out.print("\n계약서를 다운로드하시겠습니까? (1.예 / 2.아니오): ");
        int dl = scanner.nextInt();
        scanner.nextLine();
        if (dl == 1) {
            System.out.println("계약서 파일이 생성되었습니다: " + selected.getContractId() + "_계약서.pdf");
        }
    }

    public static void uc11ProfitAnalysis(Scanner scanner) {
        System.out.println("\n=== 보험 처리 실익 분석 ===");

        // 2단계: 분석 대상 계약 선택 화면
        if (DataStorage.contracts.isEmpty()) {
            System.out.println("보유 중인 계약이 없습니다.");
            return;
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        System.out.println("\n[계약 목록]");
        System.out.println("번호 | 계약명           | 시작일     | 월 보험료");
        System.out.println("-----|-----------------|------------|----------");
        for (int i = 0; i < DataStorage.contracts.size(); i++) {
            InsuranceContract c = DataStorage.contracts.get(i);
            System.out.printf("%-4d | %-15s | %s | %,d원%n",
                    i + 1, c.getProductName(), sdf.format(c.getStartDate()), c.getMonthlyPremium());
        }

        // 3단계: 계약 선택
        System.out.print("\n분석할 계약 번호 (0.뒤로): ");
        int contractChoice = scanner.nextInt();
        scanner.nextLine();
        if (contractChoice == 0) return;
        if (contractChoice < 1 || contractChoice > DataStorage.contracts.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }
        InsuranceContract selected = DataStorage.contracts.get(contractChoice - 1);

        // A1: 가입 기간 6개월 미만 체크
        long diffMs = new Date().getTime() - selected.getStartDate().getTime();
        long diffMonths = diffMs / (1000L * 60 * 60 * 24 * 30);
        if (diffMonths < 6) {
            System.out.println("분석에 필요한 데이터가 충분하지 않습니다. 6개월 이후 이용 가능합니다.");
            return;
        }

        // 3단계: 분석 기간 선택
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

        // 4단계: 분석 결과 계산
        int totalPaid = (int)(selected.getMonthlyPremium() * analysisMonths);

        Date periodStart = new Date(new Date().getTime() - analysisMonths * 30L * 24 * 60 * 60 * 1000);
        int totalReceived = 0;
        for (Claim claim : DataStorage.claims) {
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

        // ASCII 바 차트
        int maxBar = 30;
        int paidBar  = totalPaid  > 0 ? Math.max(1, (int)((double) totalPaid  / Math.max(totalPaid, totalReceived) * maxBar)) : 0;
        int recvBar  = totalReceived > 0 ? Math.max(1, (int)((double) totalReceived / Math.max(totalPaid, totalReceived) * maxBar)) : 0;

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

        // 5단계: 유사 가입자 대비 비교
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
