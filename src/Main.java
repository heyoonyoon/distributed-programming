import claim.Claim;
import claim.CarAccidentReport;
import claim.HealthInsuranceClaim;
import common.enums.EClaimComplexity;
import common.enums.EClaimStatus;
import payment.BenefitPayment;
import user.Policyholder;
import contract.InsuranceApplication;
import common.vo.MedicalHistory;
import common.vo.VehicleInfo;
import product.CarInsuranceProduct;
import product.CoverageItem;
import product.HealthInsuranceProduct;
import product.InsuranceProduct;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Main {

    static List<HealthInsuranceProduct> healthProducts = new ArrayList<>();
    static List<CarInsuranceProduct> carProducts = new ArrayList<>();
    static List<Claim> claims = new ArrayList<>();
    static Policyholder currentHolder = new Policyholder(
            "U001", "홍길동", "hong@example.com", "01012345678",
            "9001011234567", "서울시 강남구 테헤란로 123", "110-123-456789");

    static {
        HealthInsuranceProduct h1 = new HealthInsuranceProduct("H001", "실속 의료보험", "입원·수술 보장 기본형", 30000, 60);
        h1.addCoverageItem(new CoverageItem("HC001", "입원비", 5000000, 100000));
        h1.addCoverageItem(new CoverageItem("HC002", "수술비", 3000000, 50000));
        healthProducts.add(h1);

        HealthInsuranceProduct h2 = new HealthInsuranceProduct("H002", "프리미엄 의료보험", "암·중증질환 포함 종합형", 80000, 180);
        h2.addCoverageItem(new CoverageItem("HC003", "암 진단비", 50000000, 0));
        h2.addCoverageItem(new CoverageItem("HC004", "입원비", 10000000, 50000));
        h2.addCoverageItem(new CoverageItem("HC005", "수술비", 5000000, 0));
        healthProducts.add(h2);

        CarInsuranceProduct c1 = new CarInsuranceProduct("C001", "기본 자동차보험", "대인·대물 기본 보장", 50000, "승용", "본인");
        c1.addCoverageItem(new CoverageItem("CC001", "대인 배상", 100000000, 0));
        c1.addCoverageItem(new CoverageItem("CC002", "대물 배상", 20000000, 200000));
        carProducts.add(c1);

        CarInsuranceProduct c2 = new CarInsuranceProduct("C002", "가족 자동차보험", "가족 운전자 전체 보장 종합형", 90000, "승용", "가족");
        c2.addCoverageItem(new CoverageItem("CC003", "대인 배상", 200000000, 0));
        c2.addCoverageItem(new CoverageItem("CC004", "대물 배상", 50000000, 100000));
        c2.addCoverageItem(new CoverageItem("CC005", "자기 차량 손해", 30000000, 500000));
        carProducts.add(c2);

        claims.add(new HealthInsuranceClaim("CLM-001", new Date(System.currentTimeMillis() - 5L*24*60*60*1000),
                150000, EClaimStatus.IN_REVIEW, "서울대병원", "J00",
                "독감으로 인한 입원 치료", "진단서, 입원확인서, 영수증", 0));
        claims.add(new CarAccidentReport("CLM-002", new Date(System.currentTimeMillis() - 2L*24*60*60*1000),
                500000, EClaimStatus.PENDING, "서울시 강남구", "쌍방", "12가3456",
                "교차로 쌍방 추돌 사고", "사고확인서, 수리견적서", 0));
        claims.add(new HealthInsuranceClaim("CLM-003", new Date(System.currentTimeMillis() - 30L*24*60*60*1000),
                300000, EClaimStatus.APPROVED, "연세병원", "K20",
                "위염 치료 및 내시경 검사", "진단서, 영수증", 270000));
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== 보험 시스템 ===");
            System.out.println("1. 보험 상품 조회");
            System.out.println("2. 보험 가입 요청");
            System.out.println("3. 보상 처리 현황 확인");
            System.out.println("4. 보상 이력 조회");
            System.out.println("5. 의료보험 청구");
            System.out.println("6. 개인 정보 수정");
            System.out.println("7. 미납 내역 조회");
            System.out.println("8. 계약 내용 조회");
            System.out.println("9. 자동차사고 접수");
            System.out.println("10. 보험료 납부");
            System.out.println("11. 보험 처리 실익 분석");
            System.out.println("12. 보험금 지급 심사");
            System.out.println("13. 보험 가입 심사");
            System.out.println("14. 담당자 지정");
            System.out.println("15. 자동차 사고 이력 조회");
            System.out.println("16. 미납 고지서 발송");
            System.out.println("17. 보험금 지급");
            System.out.println("0. 종료");
            System.out.print("선택: ");

            int choice = scanner.nextInt();
            switch (choice) {
                case 0: return;
                case 1: uc01InsuranceProductInquiry(scanner); break;
                case 2: uc02InsuranceApplication(scanner); break;
                case 3: uc03ClaimStatusCheck(scanner); break;
                case 4: uc04ClaimHistoryInquiry(scanner); break;
                case 5: uc05HealthInsuranceClaim(scanner); break;
                case 6: uc06UpdatePersonalInfo(scanner); break;
                case 7: System.out.println("UC07 미구현"); break;
                case 8: System.out.println("UC08 미구현"); break;
                case 9: System.out.println("UC09 미구현"); break;
                case 10: System.out.println("UC10 미구현"); break;
                case 11: System.out.println("UC11 미구현"); break;
                case 12: System.out.println("UC12 미구현"); break;
                case 13: System.out.println("UC13 미구현"); break;
                case 14: System.out.println("UC14 미구현"); break;
                case 15: System.out.println("UC15 미구현"); break;
                case 16: System.out.println("UC16 미구현"); break;
                case 17: System.out.println("UC17 미구현"); break;
                default: System.out.println("잘못된 입력입니다.");
            }
        }
    }

    private static void uc01InsuranceProductInquiry(Scanner scanner) {
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
            products = healthProducts;
        } else if (typeChoice == 2) {
            products = carProducts;
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

    private static void uc02InsuranceApplication(Scanner scanner) {
        System.out.println("\n=== 보험 가입 요청 ===");

        // 상품 선택 (UC01 선행 조건 반영: 종류 → 목록 → 선택)
        System.out.println("1. 의료보험  2. 자동차보험");
        System.out.print("보험 종류 선택: ");
        int typeChoice = scanner.nextInt();
        scanner.nextLine();

        List<? extends InsuranceProduct> products;
        boolean isCar;
        if (typeChoice == 1) {
            products = healthProducts;
            isCar = false;
        } else if (typeChoice == 2) {
            products = carProducts;
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

        // 1단계: 가입 신청 시작
        System.out.println("\n[" + selectedProduct.getProductName() + "] 가입 신청을 시작합니다.");

        // 2단계: 개인정보 입력란 출력
        System.out.println("\n--- 개인정보 입력 ---");

        // 3단계: 개인정보 입력 (E1: 형식 오류 시 다시 입력)
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

        // 4단계: 보험 종류별 추가 정보 입력란 출력
        VehicleInfo vehicleInfo = null;
        MedicalHistory medicalHistory = null;

        if (isCar) {
            // 4단계: 자동차 추가 정보 입력 (E2: 필수 항목 누락 시 다시 입력)
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
            // 4단계: 의료 추가 정보 입력 (E2: 필수 항목 누락 시 다시 입력)
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

        // 6단계: 최종 확인 화면 출력
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

        // 7단계: 최종 제출
        System.out.print("\n최종 제출하시겠습니까? (1.예 / 2.아니오): ");
        int confirm = scanner.nextInt();
        scanner.nextLine();
        if (confirm != 1) {
            System.out.println("가입 신청이 취소되었습니다.");
            return;
        }

        // 8단계: 접수 번호 생성, PENDING 상태로 기록
        String applicationId = "APP-" + System.currentTimeMillis();
        InsuranceApplication application = new InsuranceApplication(
                applicationId, selectedProduct.getProductId(), name, vehicleInfo, medicalHistory);

        // 9단계: 접수 완료 안내 출력
        System.out.println("\n=== 접수 완료 ===");
        System.out.println("접수 번호     : " + application.getApplicationId());
        System.out.println("상태          : 심사 대기 (PENDING)");
        System.out.println("예상 처리 기간: 영업일 기준 3~5일");
        System.out.println("안내 이메일   : " + email + " 으로 발송되었습니다.");
        System.out.println("안내 문자     : " + phone + " 으로 발송되었습니다.");
    }

    private static void uc03ClaimStatusCheck(Scanner scanner) {
        System.out.println("\n=== 보상 처리 현황 ===");

        // 2단계: 진행 중인 보상 처리 목록 필터링 (PENDING, IN_REVIEW)
        List<Claim> inProgress = new ArrayList<>();
        for (Claim c : claims) {
            EClaimStatus s = c.getStatus();
            if (s == EClaimStatus.PENDING || s == EClaimStatus.IN_REVIEW) {
                inProgress.add(c);
            }
        }

        // A1: 진행 중인 건 없음
        if (inProgress.isEmpty()) {
            System.out.println("현재 진행 중인 보상 처리 건이 없습니다.");
            return;
        }

        // 2단계: 목록 출력 (접수번호, 접수일, 보험 종류, 현재 상태)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        System.out.println("\n번호 | 접수번호   | 접수일     | 보험 종류        | 현재 상태");
        System.out.println("-----|-----------|------------|-----------------|----------");
        for (int i = 0; i < inProgress.size(); i++) {
            Claim c = inProgress.get(i);
            System.out.printf("%-4d | %-9s | %s | %-15s | %s%n",
                    i + 1, c.getClaimId(), sdf.format(c.getClaimDate()),
                    c.getClaimType(), statusLabel(c.getStatus()));
        }

        // 3단계: 상세보기 선택
        System.out.print("\n상세보기할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > inProgress.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        Claim selected = inProgress.get(choice - 1);

        // 4단계: 처리 단계별 진행 현황 출력
        System.out.println("\n=== 처리 진행 현황: " + selected.getClaimId() + " ===");
        printProgressFlow(selected.getStatus());

        // 5단계: 예상 처리 기간 및 담당자 연락처 출력
        System.out.println("\n예상 처리 기간: " + expectedDuration(selected.getStatus()));
        System.out.println("담당자        : " + selected.getAssignedStaff());
        System.out.println("담당자 연락처 : " + selected.getStaffContact());
    }

    private static String statusLabel(EClaimStatus status) {
        switch (status) {
            case PENDING:   return "접수 대기";
            case IN_REVIEW: return "심사 중";
            case APPROVED:  return "승인 완료";
            case REJECTED:  return "반려";
            default:        return status.name();
        }
    }

    private static void printProgressFlow(EClaimStatus current) {
        String[] steps = {"접수 완료", "담당자 배정", "심사 중", "완료"};
        int currentStep;
        switch (current) {
            case PENDING:   currentStep = 1; break;
            case IN_REVIEW: currentStep = 2; break;
            case APPROVED:
            case REJECTED:  currentStep = 3; break;
            default:        currentStep = 0;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.length; i++) {
            if (i == currentStep) {
                sb.append("[▶ ").append(steps[i]).append("]");
            } else if (i < currentStep) {
                sb.append("[✓ ").append(steps[i]).append("]");
            } else {
                sb.append("[ ").append(steps[i]).append("]");
            }
            if (i < steps.length - 1) sb.append(" → ");
        }
        System.out.println(sb);
    }

    private static String expectedDuration(EClaimStatus status) {
        switch (status) {
            case PENDING:   return "영업일 기준 1~2일 (담당자 배정 대기 중)";
            case IN_REVIEW: return "영업일 기준 2~3일 (심사 진행 중)";
            default:        return "처리 완료";
        }
    }

    private static void uc04ClaimHistoryInquiry(Scanner scanner) {
        System.out.println("\n=== 보상 이력 조회 ===");

        // 2단계: 조회 기간 선택 화면 출력 (기본값: 최근 1년)
        System.out.println("조회 기간을 선택하세요.");
        System.out.println("1. 최근 1개월  2. 최근 3개월  3. 최근 6개월  4. 최근 1년(기본)  5. 전체");
        System.out.print("선택 (기본값 4): ");
        String input = scanner.nextLine().trim();
        int periodChoice = input.isEmpty() ? 4 : Integer.parseInt(input);

        // 3단계: 조회 기간 설정
        long now = System.currentTimeMillis();
        long fromMillis;
        String periodLabel;
        switch (periodChoice) {
            case 1: fromMillis = now - 30L*24*60*60*1000;  periodLabel = "최근 1개월"; break;
            case 2: fromMillis = now - 90L*24*60*60*1000;  periodLabel = "최근 3개월"; break;
            case 3: fromMillis = now - 180L*24*60*60*1000; periodLabel = "최근 6개월"; break;
            case 5: fromMillis = 0;                         periodLabel = "전체"; break;
            default: fromMillis = now - 365L*24*60*60*1000; periodLabel = "최근 1년"; break;
        }

        // 4단계: 해당 기간 보상 이력 목록 출력
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        List<Claim> history = new ArrayList<>();
        for (Claim c : claims) {
            if (c.getClaimDate().getTime() >= fromMillis) {
                history.add(c);
            }
        }

        System.out.println("\n[조회 기간: " + periodLabel + "]");

        // A1: 이력 없음
        if (history.isEmpty()) {
            System.out.println("해당 기간의 보상 이력이 없습니다.");
            return;
        }

        System.out.println("\n번호 | 처리일     | 보험 종류        | 청구 금액     | 지급 금액     | 처리 결과");
        System.out.println("-----|------------|-----------------|--------------|--------------|----------");
        for (int i = 0; i < history.size(); i++) {
            Claim c = history.get(i);
            System.out.printf("%-4d | %s | %-15s | %,9d원 | %,9d원 | %s%n",
                    i + 1, sdf.format(c.getClaimDate()), c.getClaimType(),
                    c.getRequestAmount(), c.getPaidAmount(), statusLabel(c.getStatus()));
        }

        // 5단계: 상세보기 선택
        System.out.print("\n상세보기할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > history.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        // 6단계: 상세 내용 출력 (청구 사유, 제출 서류, 심사 결과, 지급 내역)
        Claim selected = history.get(choice - 1);
        System.out.println("\n=== 상세 내용: " + selected.getClaimId() + " ===");
        System.out.println("보험 종류  : " + selected.getClaimType());
        System.out.println("접수일     : " + sdf.format(selected.getClaimDate()));
        System.out.println("청구 사유  : " + selected.getClaimReason());
        System.out.println("제출 서류  : " + selected.getDocuments());
        System.out.println("심사 결과  : " + statusLabel(selected.getStatus()));
        System.out.println("청구 금액  : " + String.format("%,d", selected.getRequestAmount()) + "원");
        System.out.println("지급 금액  : " + String.format("%,d", selected.getPaidAmount()) + "원");
        if (selected.getPaidAmount() > 0) {
            System.out.println("지급 방법  : 등록 계좌 자동 이체");
        } else if (selected.getStatus() == EClaimStatus.PENDING || selected.getStatus() == EClaimStatus.IN_REVIEW) {
            System.out.println("지급 내역  : 심사 진행 중 (미지급)");
        } else {
            System.out.println("지급 내역  : 지급 없음 (반려)");
        }
    }

    private static void uc05HealthInsuranceClaim(Scanner scanner) {
        System.out.println("\n=== 의료보험 청구 ===");

        // 2단계: 청구 정보 입력란 출력
        System.out.println("\n--- 청구 정보 입력 ---");

        // 3단계: 청구 정보 입력 (E1: 파일 형식 오류 시 재입력)
        System.out.print("청구 사유: ");
        String claimReason = scanner.nextLine().trim();

        System.out.print("진료일 (YYYYMMDD): ");
        String treatmentDate;
        while (true) {
            treatmentDate = scanner.nextLine().trim();
            if (treatmentDate.matches("\\d{8}")) break;
            System.out.println("올바른 형식으로 입력해 주세요. (YYYYMMDD 8자리)");
            System.out.print("진료일 (YYYYMMDD): ");
        }

        System.out.print("병원명: ");
        String hospitalName = scanner.nextLine().trim();

        int requestAmount;
        while (true) {
            System.out.print("청구 금액 (원): ");
            String amtInput = scanner.nextLine().trim();
            try {
                requestAmount = Integer.parseInt(amtInput.replace(",", ""));
                if (requestAmount > 0) break;
            } catch (NumberFormatException ignored) {}
            System.out.println("올바른 금액을 입력해 주세요.");
        }

        // 증빙 서류 첨부 (E1: 허용 확장자 검사, 재입력)
        String documents;
        while (true) {
            System.out.println("증빙 서류 파일명 입력 (허용: PDF, JPG, PNG)");
            System.out.print("파일명: ");
            documents = scanner.nextLine().trim();
            String lower = documents.toLowerCase();
            if (lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".png")) break;
            System.out.println("지원하지 않는 파일 형식입니다. (허용: PDF, JPG, PNG)");
        }

        // 4단계: 청구 신청
        System.out.println("\n청구 신청 중...");

        // 5단계: 청구 정보 저장 + 복잡도 자동 판별 (50만원 초과 = COMPLEX)
        String claimId = "CLM-" + String.format("%03d", claims.size() + 1);
        EClaimComplexity complexity = requestAmount > 500000 ? EClaimComplexity.COMPLEX : EClaimComplexity.SIMPLE;
        EClaimStatus initialStatus = complexity == EClaimComplexity.SIMPLE ? EClaimStatus.APPROVED : EClaimStatus.PENDING;

        HealthInsuranceClaim newClaim = new HealthInsuranceClaim(
                claimId, new Date(), requestAmount, initialStatus,
                hospitalName, "AUTO", claimReason, documents, 0);
        newClaim.setComplexity(complexity);
        claims.add(newClaim);

        System.out.println("청구 접수 번호: " + claimId);
        System.out.println("복잡도 판별 결과: " + (complexity == EClaimComplexity.SIMPLE ? "간단한 청구" : "복잡한 청구"));

        if (complexity == EClaimComplexity.SIMPLE) {
            // 6단계: 간단한 청구 → UC17 즉시 지급 수행
            int paidAmount = (int)(requestAmount * 0.9);
            BenefitPayment payment = new BenefitPayment("PAY-" + System.currentTimeMillis(), paidAmount, "***-***-123456");
            payment.transfer();

            // 7단계: 처리 결과 통보
            System.out.println("\n=== 보험금 즉시 지급 완료 ===");
            System.out.println("지급 금액  : " + String.format("%,d", payment.getPaidAmount()) + "원");
            System.out.println("지급 계좌  : " + "***-***-123456");
            System.out.println("처리 결과를 이메일/문자로 발송하였습니다.");
        } else {
            // A1: 복잡한 청구 → 심사 대기 + UC14 담당자 배정
            // A1-1: 심사 대기 상태 (이미 PENDING으로 생성됨)
            // A1-2: UC14 담당자 배정
            System.out.println("\n=== 담당자 배정 (UC14) ===");
            System.out.println("담당자    : 홍길동 심사관");
            System.out.println("연락처    : 02-1234-5678");
            // A1-3: 안내 발송
            System.out.println("\n담당자 배정 후 심사를 진행합니다.");
            System.out.println("처리 현황은 '보상 처리 현황' 메뉴에서 확인하실 수 있습니다.");
            System.out.println("안내를 이메일/문자로 발송하였습니다.");
        }
    }

    private static void uc06UpdatePersonalInfo(Scanner scanner) {
        System.out.println("\n=== 개인정보 수정 ===");

        // 2단계: 본인 인증 화면 출력
        System.out.println("\n--- 본인 인증 ---");
        System.out.println("인증번호 123456이 " + currentHolder.getPhone() + "로 발송되었습니다.");

        // 3단계: 본인 인증 (E1: 실패 시 재시도, 3회 실패 시 차단)
        int authFailCount = 0;
        while (true) {
            System.out.print("인증번호 입력: ");
            String code = scanner.nextLine().trim();
            if (code.equals("123456")) break;
            authFailCount++;
            if (authFailCount >= 3) {
                System.out.println("본인 인증에 3회 연속 실패하였습니다. 해당 기능이 10분간 차단됩니다.");
                return;
            }
            System.out.println("본인 인증에 실패하였습니다. 다시 시도해 주세요. (" + authFailCount + "/3)");
        }

        // 4단계: 현재 등록된 개인정보 출력
        System.out.println("\n--- 현재 등록 정보 ---");
        System.out.println("연락처  : " + currentHolder.getPhone());
        System.out.println("주소    : " + currentHolder.getAddress());
        System.out.println("이메일  : " + currentHolder.getEmail());
        System.out.println("계좌번호: " + currentHolder.getBankAccount());

        // 5단계: 수정할 항목 변경 (E2: 저장 실패 시뮬레이션 없음 — 항상 성공)
        System.out.println("\n--- 수정할 정보 입력 (변경 없으면 엔터) ---");

        System.out.print("연락처 [" + currentHolder.getPhone() + "]: ");
        String newPhone = scanner.nextLine().trim();
        if (!newPhone.isEmpty()) {
            if (!newPhone.matches("\\d{10,11}")) {
                System.out.println("올바른 형식으로 입력해 주세요. (10~11자리 숫자)");
                System.out.println("저장에 실패하였습니다. 잠시 후 다시 시도해 주세요.");
                return;
            }
        }

        System.out.print("주소 [" + currentHolder.getAddress() + "]: ");
        String newAddress = scanner.nextLine().trim();

        System.out.print("이메일 [" + currentHolder.getEmail() + "]: ");
        String newEmail = scanner.nextLine().trim();
        if (!newEmail.isEmpty() && !newEmail.contains("@")) {
            System.out.println("올바른 형식으로 입력해 주세요. (이메일)");
            System.out.println("저장에 실패하였습니다. 잠시 후 다시 시도해 주세요.");
            return;
        }

        System.out.print("계좌번호 [" + currentHolder.getBankAccount() + "]: ");
        String newAccount = scanner.nextLine().trim();

        // 6단계: 변경된 정보 저장 + 변경 기록
        if (!newPhone.isEmpty())   currentHolder.setPhone(newPhone);
        if (!newAddress.isEmpty()) currentHolder.setAddress(newAddress);
        if (!newEmail.isEmpty())   currentHolder.setEmail(newEmail);
        if (!newAccount.isEmpty()) currentHolder.setBankAccount(newAccount);

        // 7단계: 완료 메시지 + 변경 완료 알림 발송
        System.out.println("\n개인정보가 성공적으로 변경되었습니다.");
        System.out.println("변경 완료 알림을 " + currentHolder.getPhone() + " 및 " + currentHolder.getEmail() + "로 발송하였습니다.");
    }
}
