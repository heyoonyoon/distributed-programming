package uc;

import claim.Claim;
import claim.CarAccidentReport;
import claim.HealthInsuranceClaim;
import common.enums.EClaimComplexity;
import common.enums.EClaimStatus;
import contract.InsuranceContract;
import contract.Notice;
import contract.Payment;
import common.enums.EContractStatus;
import common.enums.EPaymentMethod;
import payment.BenefitPayment;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppData {

    public static List<HealthInsuranceProduct> healthProducts = new ArrayList<>();
    public static List<CarInsuranceProduct> carProducts = new ArrayList<>();
    public static List<Claim> claims = new ArrayList<>();
    public static List<Notice> notices = new ArrayList<>();
    public static List<InsuranceContract> contracts = new ArrayList<>();
    public static List<BenefitPaymentReview> benefitReviews = new ArrayList<>();
    public static List<InsuranceApplication> applications = new ArrayList<>();
    public static List<EnrollmentReview> enrollmentReviews = new ArrayList<>();
    public static List<InsuranceEmployee> employees = new ArrayList<>();
    public static InsuranceEmployee currentEmployee;
    public static Policyholder currentHolder = new Policyholder(
            "U001", "홍길동", "hong@example.com", "01012345678",
            "9001011234567", "서울시 강남구 테헤란로 123", "110-123-456789");

    static {
        employees.add(new InsuranceEmployee("E001", "김심사", "의료심사팀", "kim@insurance.com", "01011110001"));
        employees.add(new InsuranceEmployee("E002", "이검토", "자동차심사팀", "lee@insurance.com", "01022220002"));
        employees.add(new InsuranceEmployee("E003", "박확인", "의료심사팀", "park@insurance.com", "01033330003"));
        currentEmployee = employees.get(0);

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
}
