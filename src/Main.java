import uc.AppData;
import uc.UcClaim;
import uc.UcContract;
import uc.UcProduct;
import uc.UcReview;
import uc.UcSystem;
import uc.UcUser;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        AppData data = new AppData();
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
                case 1:  UcProduct.uc01InsuranceProductInquiry(scanner); break;
                case 2:  UcProduct.uc02InsuranceApplication(scanner); break;
                case 3:  UcClaim.uc03ClaimStatusCheck(scanner); break;
                case 4:  UcClaim.uc04ClaimHistoryInquiry(scanner); break;
                case 5:  UcClaim.uc05HealthInsuranceClaim(scanner); break;
                case 6:  UcUser.uc06UpdatePersonalInfo(scanner); break;
                case 7:  UcContract.uc07UnpaidInquiry(scanner); break;
                case 8:  UcContract.uc08ContractInquiry(scanner); break;
                case 9:  UcClaim.uc09CarAccidentReport(scanner); break;
                case 10: UcContract.uc10PayPremium(scanner); break;
                case 11: UcContract.uc11ProfitAnalysis(scanner); break;
                case 12: UcReview.uc12BenefitPaymentReview(scanner); break;
                case 13: UcReview.uc13EnrollmentReview(scanner); break;
                case 14: UcReview.uc14AssignStaff(scanner); break;
                case 15: UcSystem.uc15AccidentHistoryInquiry(scanner); break;
                case 16: UcSystem.uc16SendUnpaidNotice(scanner); break;
                case 17: UcSystem.uc17BenefitPayment(scanner); break;
                default: System.out.println("잘못된 입력입니다.");
            }
        }
    }
}
