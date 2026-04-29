import java.util.Scanner;

public class Main {
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
                case 1: System.out.println("UC01 미구현"); break;
                case 2: System.out.println("UC02 미구현"); break;
                case 3: System.out.println("UC03 미구현"); break;
                case 4: System.out.println("UC04 미구현"); break;
                case 5: System.out.println("UC05 미구현"); break;
                case 6: System.out.println("UC06 미구현"); break;
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
}
