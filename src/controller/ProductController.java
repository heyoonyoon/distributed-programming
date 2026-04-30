package controller;

import common.DataStorage;
import product.CoverageItem;
import product.InsuranceProduct;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ProductController {
    public static void uc01InsuranceProductInquiry(Scanner scanner) {
        // 2단계: 보험 종류 선택 화면 출력
        System.out.println("\n=== 보험 상품 조회 ===");
        System.out.println("1. 의료보험");
        System.out.println("2. 자동차보험");
        System.out.print("보험 종류 선택: ");

        int typeChoice = scanner.nextInt();
        scanner.nextLine();

        // 3단계: 종류 선택
        List<? extends InsuranceProduct> products;
        if (typeChoice == 1) {
            products = DataStorage.healthProducts;
        } else if (typeChoice == 2) {
            products = DataStorage.carProducts;
        } else {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        // E1: 조회 결과 없음
        if (products.isEmpty()) {
            System.out.println("해당 조건에 맞는 보험 상품이 없습니다.");
            return;
        }

        // A1: 검색 필터
        System.out.println("\n필터를 사용하시겠습니까? (1.예 / 2.아니오)");
        System.out.print("선택: ");
        int filterChoice = scanner.nextInt();
        scanner.nextLine();

        int minPremium = 0, maxPremium = Integer.MAX_VALUE;
        String keyword = "";

        if (filterChoice == 1) {
            // A1-1: 필터 입력
            System.out.print("월 보험료 최솟값 (원, 없으면 0): ");
            minPremium = scanner.nextInt();
            System.out.print("월 보험료 최댓값 (원, 없으면 0): ");
            maxPremium = scanner.nextInt();
            if (maxPremium == 0) maxPremium = Integer.MAX_VALUE;
            scanner.nextLine();
            System.out.println("보장 항목 키워드 (없으면 엔터)");
            if (typeChoice == 1) {
                System.out.print("  선택 가능: 입원비, 수술비, 암 진단비 > ");
            } else {
                System.out.print("  선택 가능: 대인 배상, 대물 배상, 자기 차량 손해 > ");
            }
            keyword = scanner.nextLine().trim();
        }

        // A1-2: 필터 적용 후 목록 출력 / 4단계: 상품 목록 출력
        final int fMin = minPremium, fMax = maxPremium;
        final String fKeyword = keyword;
        List<InsuranceProduct> filtered = new ArrayList<>();
        for (InsuranceProduct p : products) {
            if (p.getBasePremium() < fMin || p.getBasePremium() > fMax) continue;
            if (!fKeyword.isEmpty()) {
                boolean found = false;
                for (CoverageItem item : p.getCoverageItems()) {
                    if (item.getItemName().contains(fKeyword)) { found = true; break; }
                }
                if (!found && !p.getDescription().contains(fKeyword)) continue;
            }
            filtered.add(p);
        }

        if (filtered.isEmpty()) {
            System.out.println("해당 조건에 맞는 보험 상품이 없습니다.");
            return;
        }

        System.out.println("\n--- 상품 목록 ---");
        for (int i = 0; i < filtered.size(); i++) {
            InsuranceProduct p = filtered.get(i);
            System.out.printf("%d. [%s] %s | 월 보험료: %,d원%n",
                    i + 1, p.getProductId(), p.getProductName(), p.getBasePremium());
            System.out.println("   보장 내용: " + p.getDescription());
        }

        // 5단계: 상세보기 선택
        System.out.print("\n상세보기할 상품 번호 (0.뒤로): ");
        int detailChoice = scanner.nextInt();
        scanner.nextLine();

        if (detailChoice == 0) return;
        if (detailChoice < 1 || detailChoice > filtered.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        // 6단계: 상세 정보 출력
        InsuranceProduct selected = filtered.get(detailChoice - 1);
        System.out.println("\n=== 상품 상세 정보 ===");
        System.out.println("상품 ID  : " + selected.getProductId());
        System.out.println("상품명   : " + selected.getProductName());
        System.out.println("설명     : " + selected.getDescription());
        System.out.println("월 보험료: " + String.format("%,d", selected.getBasePremium()) + "원");
        System.out.println("보험료 산출 기준: " + selected.getPremiumBasis());
        System.out.println("\n[보장 항목]");
        for (CoverageItem item : selected.getCoverageItems()) {
            System.out.printf("  - %s | 보장 한도: %,d원 | 면책금액: %,d원%n",
                    item.getItemName(), item.getCoverageLimit(), item.getDeductible());
        }
    }

    public static InsuranceProduct findProduct(String productId) {
        for (product.HealthInsuranceProduct p : DataStorage.healthProducts) {
            if (p.getProductId().equals(productId)) return p;
        }
        for (product.CarInsuranceProduct p : DataStorage.carProducts) {
            if (p.getProductId().equals(productId)) return p;
        }
        return null;
    }
}
