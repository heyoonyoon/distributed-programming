import claim.Claim;
import claim.CarAccidentReport;
import claim.HealthInsuranceClaim;
import common.enums.EClaimComplexity;
import common.enums.EClaimStatus;
import common.enums.EReviewResult;
import contract.InsuranceContract;
import contract.Notice;
import contract.Payment;
import common.enums.EContractStatus;
import common.enums.EPaymentMethod;
import payment.BenefitPayment;
import external.AccidentHistory;
import review.BenefitPaymentReview;
import review.EnrollmentReview;
import user.InsuranceEmployee;
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
    static List<Notice> notices = new ArrayList<>();
    static List<InsuranceContract> contracts = new ArrayList<>();
    static List<BenefitPaymentReview> benefitReviews = new ArrayList<>();
    static List<InsuranceApplication> applications = new ArrayList<>();
    static List<EnrollmentReview> enrollmentReviews = new ArrayList<>();
    static InsuranceEmployee currentEmployee = new InsuranceEmployee("E001", "김심사", "심사부", "emp@insurance.com", "01099998888");
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
        contracts.add(new InsuranceContract("CON-001", "실속 의료보험", "의료보험",
                new Date(System.currentTimeMillis() - 365L*24*60*60*1000),
                new Date(System.currentTimeMillis() + 2*365L*24*60*60*1000),
                30000, "홍길동", "자동이체", "입원 특약 포함"));
        contracts.add(new InsuranceContract("CON-002", "기본 자동차보험", "자동차보험",
                new Date(System.currentTimeMillis() - 180L*24*60*60*1000),
                new Date(System.currentTimeMillis() + 185L*24*60*60*1000),
                50000, "홍길동", "카드 결제", "대물 확장 특약"));

        notices.add(new Notice("NTC-001", "실속 의료보험",
                new Date(System.currentTimeMillis() - 10L*24*60*60*1000), 30000, 10));
        notices.add(new Notice("NTC-002", "기본 자동차보험",
                new Date(System.currentTimeMillis() - 35L*24*60*60*1000), 50000, 35));

        claims.add(new HealthInsuranceClaim("CLM-003", new Date(System.currentTimeMillis() - 30L*24*60*60*1000),
                300000, EClaimStatus.APPROVED, "연세병원", "K20",
                "위염 치료 및 내시경 검사", "진단서, 영수증", 270000));

        HealthInsuranceClaim complexClaim = new HealthInsuranceClaim(
                "CLM-004", new Date(System.currentTimeMillis() - 3L*24*60*60*1000),
                2500000, EClaimStatus.IN_REVIEW, "삼성서울병원", "C34",
                "폐암 수술 및 항암 치료", "진단서, 수술확인서, 입원확인서, 영수증", 0);
        complexClaim.setComplexity(EClaimComplexity.COMPLEX);
        claims.add(complexClaim);

        benefitReviews.add(new BenefitPaymentReview("BRV-001", "CLM-004", "E001", "김심사"));

        applications.add(new InsuranceApplication("APP-001", "H001", "홍길동",
                null, new MedicalHistory("고혈압", "2023년 내과 진료 기록")));
        applications.add(new InsuranceApplication("APP-002", "C001", "홍길동",
                new VehicleInfo("12가3456", "승용"), null));
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
                case 7: uc07UnpaidInquiry(scanner); break;
                case 8: uc08ContractInquiry(scanner); break;
                case 9: uc09CarAccidentReport(scanner); break;
                case 10: uc10PayPremium(scanner); break;
                case 11: uc11ProfitAnalysis(scanner); break;
                case 12: uc12BenefitPaymentReview(scanner); break;
                case 13: uc13EnrollmentReview(scanner); break;
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

    private static void uc07UnpaidInquiry(Scanner scanner) {
        System.out.println("\n=== 미납 내역 조회 ===");

        // 2단계: 미납 보험료 목록 출력
        // A1: 미납 내역 없음
        if (notices.isEmpty()) {
            System.out.println("미납된 보험료가 없습니다.");
            return;
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        System.out.println("\n번호 | 계약명           | 납부 기한  | 미납 금액    | 연체 일수 | 연체 이자   | 경고");
        System.out.println("-----|-----------------|------------|-------------|---------|------------|-----");
        for (int i = 0; i < notices.size(); i++) {
            Notice n = notices.get(i);
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
        if (choice < 1 || choice > notices.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        Notice selected = notices.get(choice - 1);

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

    private static void uc08ContractInquiry(Scanner scanner) {
        System.out.println("\n=== 계약 내용 조회 ===");

        // 2단계: 유효 계약 목록 출력 (ACTIVE만)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        List<InsuranceContract> active = new ArrayList<>();
        for (InsuranceContract c : contracts) {
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
                selected.getProductType().equals("의료보험") ? healthProducts : carProducts;
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

    private static void uc09CarAccidentReport(Scanner scanner) {
        System.out.println("\n=== 자동차사고 접수 ===");

        // 선행 조건: 유효한 자동차보험 계약 확인
        InsuranceContract carContract = null;
        for (InsuranceContract c : contracts) {
            if (c.getProductType().equals("자동차보험") && c.getStatus() == EContractStatus.ACTIVE) {
                carContract = c;
                break;
            }
        }
        if (carContract == null) {
            System.out.println("유효한 자동차보험 계약이 없습니다.");
            return;
        }
        System.out.println("계약: [" + carContract.getContractId() + "] " + carContract.getProductName());

        // 2단계: 사고 정보 입력란 출력
        System.out.println("\n--- 사고 정보 입력 ---");

        // 3단계: 사고 정보 입력
        System.out.print("사고 일시 (YYYYMMDD HHMM): ");
        String accidentDateTime;
        while (true) {
            accidentDateTime = scanner.nextLine().trim();
            if (accidentDateTime.matches("\\d{8} \\d{4}")) break;
            System.out.println("올바른 형식으로 입력해 주세요. (예: 20260430 1430)");
            System.out.print("사고 일시 (YYYYMMDD HHMM): ");
        }

        System.out.print("사고 장소: ");
        String location = scanner.nextLine().trim();

        System.out.println("사고 유형 선택");
        System.out.println("  1. 단독  2. 쌍방  3. 대인  4. 기타");
        System.out.print("선택: ");
        int typeChoice = scanner.nextInt();
        scanner.nextLine();
        String accidentType;
        switch (typeChoice) {
            case 1: accidentType = "단독"; break;
            case 2: accidentType = "쌍방"; break;
            case 3: accidentType = "대인"; break;
            default: accidentType = "기타";
        }

        System.out.print("피해 차량 번호: ");
        String vehicleNumber = scanner.nextLine().trim();

        System.out.print("청구 예상 금액 (원): ");
        int requestAmount;
        while (true) {
            String amtInput = scanner.nextLine().trim();
            try {
                requestAmount = Integer.parseInt(amtInput.replace(",", ""));
                if (requestAmount > 0) break;
            } catch (NumberFormatException ignored) {}
            System.out.println("올바른 금액을 입력해 주세요.");
            System.out.print("청구 예상 금액 (원): ");
        }

        // 부상 여부 입력
        System.out.print("부상자가 있습니까? (1.예 / 2.아니오): ");
        int injuryChoice = scanner.nextInt();
        scanner.nextLine();
        boolean hasInjury = injuryChoice == 1;
        int injuredCount = 0;
        String injuryDetail = "";

        // A1: 대인사고 포함 시 부상자 정보 추가 입력
        if (hasInjury || accidentType.equals("대인")) {
            System.out.println("\n--- 부상자 정보 입력 (A1) ---");
            System.out.print("부상자 수: ");
            injuredCount = scanner.nextInt();
            scanner.nextLine();
            System.out.print("부상 정도 (경상/중상/사망): ");
            injuryDetail = scanner.nextLine().trim();
        }

        // 증빙 자료 첨부 (E1: 파일 크기 초과 시 재입력)
        String photoFile;
        while (true) {
            System.out.print("현장 사진 파일명 (예: photo.jpg, 없으면 엔터): ");
            photoFile = scanner.nextLine().trim();
            if (photoFile.isEmpty()) break;
            // 파일 크기 시뮬레이션: 파일명에 "big" 포함 시 초과로 처리
            if (photoFile.toLowerCase().contains("big")) {
                System.out.println("파일 크기는 개당 10MB 이하여야 합니다.");
                continue;
            }
            break;
        }

        // 4단계: 접수하기
        System.out.println("\n접수 처리 중...");

        // 5단계: 사고 접수 정보 저장 + 접수 번호 생성
        String claimId = "CLM-" + String.format("%03d", claims.size() + 1);
        String reason = accidentType + " 사고 (" + accidentDateTime + ", " + location + ")";
        String docs = photoFile.isEmpty() ? "없음" : photoFile;
        CarAccidentReport report = new CarAccidentReport(
                claimId, new Date(), requestAmount, EClaimStatus.PENDING,
                location, accidentType, vehicleNumber, reason, docs, 0);
        claims.add(report);

        // 6단계: 담당자 알림 발송
        System.out.println("\n보험사 담당자에게 사고 접수 알림이 발송되었습니다.");

        // 7단계: 접수 완료 안내
        System.out.println("\n=== 접수 완료 ===");
        System.out.println("접수 번호    : " + claimId);
        System.out.println("사고 유형    : " + accidentType);
        System.out.println("사고 장소    : " + location);
        if (hasInjury || accidentType.equals("대인")) {
            System.out.println("부상자 수    : " + injuredCount + "명 (" + injuryDetail + ")");
        }
        System.out.println("담당자 연락처: 02-1234-5678");
        System.out.println("향후 처리 절차: 담당자 배정 → 현장 조사 → 손해 사정 → 보험금 지급");
        System.out.println("안내를 이메일/문자로 발송하였습니다.");
    }

    private static void uc10PayPremium(Scanner scanner) {
        System.out.println("\n=== 보험료 납부 ===");

        // 2단계: 납부 예정 보험료 목록 출력 (ACTIVE 계약 기준)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        List<InsuranceContract> activeContracts = new ArrayList<>();
        for (InsuranceContract c : contracts) {
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
        notices.removeIf(n -> n.getContractName().equals(selectedContract.getProductName()));

        // 6단계: 납부 완료 영수증 발송
        System.out.println("\n=== 납부 완료 ===");
        System.out.println("영수증 번호: " + receipt.getPaymentId());
        System.out.println("납부 계약  : " + selectedContract.getProductName());
        System.out.println("납부 금액  : " + String.format("%,d", receipt.getAmount()) + "원");
        System.out.println("납부 수단  : " + (method == EPaymentMethod.CARD ? "신용카드" : "계좌이체"));
        System.out.println("납부 일시  : " + sdf.format(receipt.getPaidAt()));
        System.out.println("납부 완료 영수증을 이메일/문자로 발송하였습니다.");
    }

    private static void uc11ProfitAnalysis(Scanner scanner) {
        System.out.println("\n=== 보험 처리 실익 분석 ===");

        // 2단계: 분석 대상 계약 선택 화면
        if (contracts.isEmpty()) {
            System.out.println("보유 중인 계약이 없습니다.");
            return;
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        System.out.println("\n[계약 목록]");
        System.out.println("번호 | 계약명           | 시작일     | 월 보험료");
        System.out.println("-----|-----------------|------------|----------");
        for (int i = 0; i < contracts.size(); i++) {
            InsuranceContract c = contracts.get(i);
            System.out.printf("%-4d | %-15s | %s | %,d원%n",
                    i + 1, c.getProductName(), sdf.format(c.getStartDate()), c.getMonthlyPremium());
        }

        // 3단계: 계약 선택
        System.out.print("\n분석할 계약 번호 (0.뒤로): ");
        int contractChoice = scanner.nextInt();
        scanner.nextLine();
        if (contractChoice == 0) return;
        if (contractChoice < 1 || contractChoice > contracts.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }
        InsuranceContract selected = contracts.get(contractChoice - 1);

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
        for (Claim claim : claims) {
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

    private static void uc12BenefitPaymentReview(Scanner scanner) {
        System.out.println("\n=== 보험금 지급 심사 ===");

        // 2단계: 심사 대기 목록 출력
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        List<BenefitPaymentReview> pending = new ArrayList<>();
        for (BenefitPaymentReview r : benefitReviews) {
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
                    i + 1, claimId, currentHolder.getName(),
                    claim.getRequestAmount(), claim.getClaimReason(),
                    sdf.format(claim.getClaimDate()));
        }

        // 3단계: 심사할 건 선택
        System.out.print("\n심사할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > pending.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        BenefitPaymentReview review = pending.get(choice - 1);

        // E2: 다른 직원 처리 중 체크
        if (review.isLocked()) {
            System.out.println("현재 담당자 [" + review.getAssignedStaffId() + "]가 처리 중인 건입니다.");
            return;
        }
        review.lock();

        HealthInsuranceClaim claim = findHealthClaim(review.getClaimId());

        // 4단계: 상세 정보 출력
        System.out.println("\n[청구 상세 정보]");
        System.out.println("접수번호  : " + claim.getClaimId());
        System.out.println("청구인    : " + currentHolder.getName());
        System.out.println("청구 금액 : " + String.format("%,d", claim.getRequestAmount()) + "원");
        System.out.println("청구 사유 : " + claim.getClaimReason());
        System.out.println("접수일    : " + sdf.format(claim.getClaimDate()));
        System.out.println("제출 서류 : " + claim.getDocuments());

        // 5단계: 담당자 정보 표시 (UC14 include — 이미 배정 완료)
        System.out.println("\n[담당자 정보]");
        System.out.println("담당자 ID : " + review.getAssignedStaffId());
        System.out.println("담당자명  : " + review.getAssignedStaffName());

        // 6단계 → 7단계: 심사 결과 선택
        System.out.println("\n[심사 결과 선택]");
        System.out.println("1. 승인  2. 반려");
        System.out.print("선택: ");
        int resultChoice = scanner.nextInt();
        scanner.nextLine();

        if (resultChoice == 2) {
            // A1: 반려 처리
            System.out.print("반려 사유 입력: ");
            String reason = scanner.nextLine().trim();

            // E1: 저장 실패 시뮬레이션 → 실제로는 항상 성공
            review.reject(reason);

            // 8단계: 반려 결과 저장
            System.out.println("\n=== 심사 결과: 반려 ===");
            System.out.println("사유: " + reason);
            System.out.println("보험가입자에게 반려 사유를 포함한 결과 통보를 발송하였습니다.");
        } else {
            // 승인 처리
            System.out.print("지급 승인 금액 입력 (청구 금액: " + String.format("%,d", claim.getRequestAmount()) + "원): ");
            int approvedAmount = scanner.nextInt();
            scanner.nextLine();

            // E1: 재시도 루프
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

            // 8단계: 결과 저장
            // 9단계: UC17 확장 — 보험금 지급
            BenefitPayment payment = review.approve(approvedAmount, currentHolder.getBankAccount());

            // claim 상태 업데이트
            claim.setStatus(EClaimStatus.APPROVED);
            claim.setPaidAmount(approvedAmount);

            System.out.println("\n=== 심사 결과: 승인 ===");
            System.out.println("지급 번호 : " + payment.getPaymentId());
            System.out.println("지급 금액 : " + String.format("%,d", payment.getPaidAmount()) + "원");
            System.out.println("지급 계좌 : " + currentHolder.getBankAccount());
            System.out.println("지급 일시 : " + sdf.format(payment.getPaidAt()));

            // 10단계: 결과 통보
            System.out.println("\n보험가입자에게 심사 결과 통보 이메일/문자를 발송하였습니다.");
        }

        review.unlock();
    }

    private static void uc13EnrollmentReview(Scanner scanner) {
        System.out.println("\n=== 보험 가입 심사 ===");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

        // 2단계: 심사 대기 목록 출력
        List<InsuranceApplication> pending = new ArrayList<>();
        for (InsuranceApplication a : applications) {
            if (a.getStatus() == common.enums.EApplicationStatus.PENDING) pending.add(a);
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

        // 3단계: 심사할 건 선택
        System.out.print("\n심사할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > pending.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        InsuranceApplication app = pending.get(choice - 1);

        // E3: 중복 접근 체크
        EnrollmentReview existingReview = findEnrollmentReview(app.getApplicationId());
        if (existingReview != null && existingReview.isLocked()) {
            System.out.println("현재 담당자 [" + currentEmployee.getEmployeeId() + "]가 처리 중인 건입니다.");
            return;
        }

        EnrollmentReview review = existingReview != null ? existingReview
                : new EnrollmentReview("ERV-" + System.currentTimeMillis(), app.getApplicationId());
        if (existingReview == null) enrollmentReviews.add(review);
        review.lock();

        // 4단계: 상세 정보 출력
        System.out.println("\n[신청 상세 정보]");
        System.out.println("접수번호  : " + app.getApplicationId());
        System.out.println("가입자명  : " + app.getHolderName());
        System.out.println("주민번호  : " + currentHolder.getSsn());
        System.out.println("생년월일  : " + currentHolder.getBirthDate());

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

        // 5~6단계: 자동차보험인 경우 UC15 확장 — 사고 이력 조회
        if (isCarInsurance) {
            System.out.println("\n[UC15 확장: 자동차 사고 이력 조회 중...]");
            AccidentHistory history = review.fetchAccidentHistory(currentHolder.getSsn());
            if (history == null) {
                // E1: 외부 연동 실패
                System.out.println("정보 조회 실패. 1.재시도  2.직접 입력 전환");
                System.out.print("선택: ");
                int retry = scanner.nextInt();
                scanner.nextLine();
                if (retry == 1) {
                    history = review.fetchAccidentHistory(currentHolder.getSsn());
                }
            }
            if (history != null) {
                System.out.println("\n[참조 정보 — 금융감독원 사고 이력]");
                System.out.println("사고 건수     : " + history.getAccidentCount() + "건");
                System.out.println("지급액 합계   : " + String.format("%,d", history.getTotalPaidAmount()) + "원");
                System.out.println("면허 상태     : " + history.getLicenseStatus());
                java.text.SimpleDateFormat dateSdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                for (external.AccidentRecord r : history.getRecords()) {
                    System.out.println("  - " + dateSdf.format(r.getAccidentDate())
                            + " / " + r.getAccidentType()
                            + " / " + String.format("%,d", r.getPaidAmount()) + "원");
                }
            }
        }

        // 7단계: 심사 결과 선택 (A1, A2)
        System.out.println("\n[심사 결과 선택]");
        System.out.println("1. 승인  2. 조건부 승인  3. 반려  4. 부서 협의 요청");
        System.out.print("선택: ");
        int resultChoice = scanner.nextInt();
        scanner.nextLine();

        if (resultChoice == 4) {
            // A2: 부서 협의 요청
            System.out.print("협의 사유 입력: ");
            String reason = scanner.nextLine().trim();
            System.out.println("대상 부서 선택: 1.의료심사팀  2.자동차심사팀");
            System.out.print("선택: ");
            int deptChoice = scanner.nextInt();
            scanner.nextLine();
            String dept = deptChoice == 1 ? "의료심사팀" : "자동차심사팀";
            currentEmployee.requestDeptConsultation(review.getApplicationId(), reason);
            System.out.println("\n[" + dept + "]에 협의 요청이 전송되었습니다.");
            System.out.println("해당 건의 상태가 '협의 중'으로 변경되었습니다.");
            review.unlock();
            return;
        }

        EReviewResult reviewResult;
        String comment = "";
        int finalPremium = 0;

        if (resultChoice == 2) {
            // A1: 조건부 승인 — 할증
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
            // 반려
            System.out.print("반려 사유 입력: ");
            comment = scanner.nextLine().trim();
            reviewResult = EReviewResult.REJECTED;
        } else {
            reviewResult = EReviewResult.APPROVED;
        }

        // 8단계: 결과 저장
        review.confirm(reviewResult, comment);
        app.setStatus(reviewResult == EReviewResult.REJECTED
                ? common.enums.EApplicationStatus.REJECTED
                : common.enums.EApplicationStatus.APPROVED);

        // 9단계: 결과 통보
        System.out.println("\n=== 심사가 완료되었습니다. ===");
        System.out.println("심사 결과 : " + reviewResult);
        if (reviewResult == EReviewResult.CONDITIONAL) {
            System.out.println("최종 보험료: " + String.format("%,d", finalPremium) + "원");
        }
        System.out.println("가입자에게 안내 이메일/문자를 발송하였습니다.");
        review.unlock();
    }

    private static EnrollmentReview findEnrollmentReview(String applicationId) {
        for (EnrollmentReview r : enrollmentReviews) {
            if (r.getApplicationId().equals(applicationId)) return r;
        }
        return null;
    }

    private static InsuranceProduct findProduct(String productId) {
        for (HealthInsuranceProduct p : healthProducts) {
            if (p.getProductId().equals(productId)) return p;
        }
        for (CarInsuranceProduct p : carProducts) {
            if (p.getProductId().equals(productId)) return p;
        }
        return null;
    }

    private static HealthInsuranceClaim findHealthClaim(String claimId) {
        for (Claim c : claims) {
            if (c instanceof HealthInsuranceClaim && c.getClaimId().equals(claimId)) {
                return (HealthInsuranceClaim) c;
            }
        }
        return null;
    }
}
