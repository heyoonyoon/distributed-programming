package uc;

import contract.InsuranceApplication;
import common.vo.MedicalHistory;
import common.vo.VehicleInfo;
import product.CarInsuranceProduct;
import product.CoverageItem;
import product.HealthInsuranceProduct;
import product.InsuranceProduct;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UcProduct {

    public static void uc01InsuranceProductInquiry(Scanner scanner) {
        System.out.println("\n=== 보험 상품 조회 ===");
        System.out.println("1. 의료보험");
        System.out.println("2. 자동차보험");
        System.out.print("보험 종류 선택: ");

        int typeChoice = scanner.nextInt();
        scanner.nextLine();

        List<? extends InsuranceProduct> products;
        if (typeChoice == 1) {
            products = AppData.healthProducts;
        } else if (typeChoice == 2) {
            products = AppData.carProducts;
        } else {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        if (products.isEmpty()) {
            System.out.println("해당 조건에 맞는 보험 상품이 없습니다.");
            return;
        }

        System.out.println("\n필터를 사용하시겠습니까? (1.예 / 2.아니오)");
        System.out.print("선택: ");
        int filterChoice = scanner.nextInt();
        scanner.nextLine();

        int minPremium = 0, maxPremium = Integer.MAX_VALUE;
        String keyword = "";

        if (filterChoice == 1) {
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

        System.out.print("\n상세보기할 상품 번호 (0.뒤로): ");
        int detailChoice = scanner.nextInt();
        scanner.nextLine();

        if (detailChoice == 0) return;
        if (detailChoice < 1 || detailChoice > filtered.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

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

    public static void uc02InsuranceApplication(Scanner scanner) {
        System.out.println("\n=== 보험 가입 요청 ===");

        System.out.println("1. 의료보험  2. 자동차보험");
        System.out.print("보험 종류 선택: ");
        int typeChoice = scanner.nextInt();
        scanner.nextLine();

        List<? extends InsuranceProduct> products;
        boolean isCar;
        if (typeChoice == 1) {
            products = AppData.healthProducts;
            isCar = false;
        } else if (typeChoice == 2) {
            products = AppData.carProducts;
            isCar = true;
        } else {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        System.out.println("\n--- 상품 목록 ---");
        for (int i = 0; i < products.size(); i++) {
            InsuranceProduct p = products.get(i);
            System.out.printf("%d. [%s] %s | 월 보험료: %,d원%n",
                    i + 1, p.getProductId(), p.getProductName(), p.getBasePremium());
        }
        System.out.print("가입할 상품 번호 (0.뒤로): ");
        int productChoice = scanner.nextInt();
        scanner.nextLine();
        if (productChoice == 0) return;
        if (productChoice < 1 || productChoice > products.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }
        InsuranceProduct selectedProduct = products.get(productChoice - 1);

        System.out.println("\n[" + selectedProduct.getProductName() + "] 가입 신청을 시작합니다.");
        System.out.println("\n--- 개인정보 입력 ---");

        String name;
        while (true) {
            System.out.print("이름: ");
            name = scanner.nextLine().trim();
            if (!name.isEmpty()) break;
            System.out.println("올바른 형식으로 입력해 주세요. (이름)");
        }

        String birthDate;
        while (true) {
            System.out.print("생년월일 (YYYYMMDD): ");
            birthDate = scanner.nextLine().trim();
            if (birthDate.matches("\\d{8}")) break;
            System.out.println("올바른 형식으로 입력해 주세요. (생년월일: YYYYMMDD 8자리)");
        }

        String ssn;
        while (true) {
            System.out.print("주민등록번호 (13자리, - 없이): ");
            ssn = scanner.nextLine().trim();
            if (ssn.matches("\\d{13}")) break;
            System.out.println("올바른 형식으로 입력해 주세요. (주민등록번호: 13자리 숫자)");
        }

        String phone;
        while (true) {
            System.out.print("연락처 (숫자만, 예: 01012345678): ");
            phone = scanner.nextLine().trim();
            if (phone.matches("\\d{10,11}")) break;
            System.out.println("올바른 형식으로 입력해 주세요. (연락처: 10~11자리 숫자)");
        }

        String email;
        while (true) {
            System.out.print("이메일: ");
            email = scanner.nextLine().trim();
            if (email.contains("@")) break;
            System.out.println("올바른 형식으로 입력해 주세요. (이메일)");
        }

        VehicleInfo vehicleInfo = null;
        MedicalHistory medicalHistory = null;

        if (isCar) {
            System.out.println("\n--- 자동차 정보 입력 ---");
            String vehicleNumber, vehicleType, modelYear, drivingExp;
            while (true) {
                System.out.print("차량번호: ");
                vehicleNumber = scanner.nextLine().trim();
                System.out.print("차종 (승용/화물/이륜): ");
                vehicleType = scanner.nextLine().trim();
                System.out.print("연식 (예: 2020): ");
                modelYear = scanner.nextLine().trim();
                System.out.print("운전 경력 (년): ");
                drivingExp = scanner.nextLine().trim();
                if (!vehicleNumber.isEmpty() && !vehicleType.isEmpty() && !modelYear.isEmpty() && !drivingExp.isEmpty()) break;
                System.out.println("필수 입력 항목을 모두 작성해 주세요.");
            }
            vehicleInfo = new VehicleInfo(vehicleNumber, vehicleType + " " + modelYear + "년식 운전경력" + drivingExp + "년");
        } else {
            System.out.println("\n--- 의료 정보 입력 ---");
            String conditions, hospitalRecords, medications;
            while (true) {
                System.out.print("현재 병력 사항 (없으면 '없음'): ");
                conditions = scanner.nextLine().trim();
                System.out.print("과거 입원 이력 (없으면 '없음'): ");
                hospitalRecords = scanner.nextLine().trim();
                System.out.print("복용 중인 약물 (없으면 '없음'): ");
                medications = scanner.nextLine().trim();
                if (!conditions.isEmpty() && !hospitalRecords.isEmpty() && !medications.isEmpty()) break;
                System.out.println("필수 입력 항목을 모두 작성해 주세요.");
            }
            medicalHistory = new MedicalHistory(conditions, hospitalRecords + " / 복용약: " + medications);
        }

        System.out.println("\n=== 최종 확인 ===");
        System.out.println("상품명  : " + selectedProduct.getProductName());
        System.out.println("이름    : " + name);
        System.out.println("생년월일: " + birthDate);
        System.out.println("주민번호: " + ssn.substring(0, 6) + "-*******");
        System.out.println("연락처  : " + phone);
        System.out.println("이메일  : " + email);
        if (isCar && vehicleInfo != null) {
            System.out.println("차량번호: " + vehicleInfo.getVehicleNumber());
            System.out.println("차량정보: " + vehicleInfo.getVehicleType());
        } else if (medicalHistory != null) {
            System.out.println("병력    : " + medicalHistory.getConditions());
            System.out.println("입원이력: " + medicalHistory.getHospitalRecords());
        }
        System.out.println("월 보험료: " + String.format("%,d", selectedProduct.getBasePremium()) + "원");

        System.out.print("\n최종 제출하시겠습니까? (1.예 / 2.아니오): ");
        int confirm = scanner.nextInt();
        scanner.nextLine();
        if (confirm != 1) {
            System.out.println("가입 신청이 취소되었습니다.");
            return;
        }

        String applicationId = "APP-" + System.currentTimeMillis();
        InsuranceApplication application = new InsuranceApplication(
                applicationId, selectedProduct.getProductId(), name, vehicleInfo, medicalHistory);

        System.out.println("\n=== 접수 완료 ===");
        System.out.println("접수 번호     : " + application.getApplicationId());
        System.out.println("상태          : 심사 대기 (PENDING)");
        System.out.println("예상 처리 기간: 영업일 기준 3~5일");
        System.out.println("안내 이메일   : " + email + " 으로 발송되었습니다.");
        System.out.println("안내 문자     : " + phone + " 으로 발송되었습니다.");
    }
}
