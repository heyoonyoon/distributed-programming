package uc;

import java.util.Scanner;

public class UcUser {

    public static void uc06UpdatePersonalInfo(Scanner scanner) {
        System.out.println("\n=== 개인정보 수정 ===");
        System.out.println("\n--- 본인 인증 ---");
        System.out.println("인증번호 123456이 " + AppData.currentHolder.getPhone() + "로 발송되었습니다.");

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

        System.out.println("\n--- 현재 등록 정보 ---");
        System.out.println("연락처  : " + AppData.currentHolder.getPhone());
        System.out.println("주소    : " + AppData.currentHolder.getAddress());
        System.out.println("이메일  : " + AppData.currentHolder.getEmail());
        System.out.println("계좌번호: " + AppData.currentHolder.getBankAccount());

        System.out.println("\n--- 수정할 정보 입력 (변경 없으면 엔터) ---");

        System.out.print("연락처 [" + AppData.currentHolder.getPhone() + "]: ");
        String newPhone = scanner.nextLine().trim();
        if (!newPhone.isEmpty()) {
            if (!newPhone.matches("\\d{10,11}")) {
                System.out.println("올바른 형식으로 입력해 주세요. (10~11자리 숫자)");
                System.out.println("저장에 실패하였습니다. 잠시 후 다시 시도해 주세요.");
                return;
            }
        }

        System.out.print("주소 [" + AppData.currentHolder.getAddress() + "]: ");
        String newAddress = scanner.nextLine().trim();

        System.out.print("이메일 [" + AppData.currentHolder.getEmail() + "]: ");
        String newEmail = scanner.nextLine().trim();
        if (!newEmail.isEmpty() && !newEmail.contains("@")) {
            System.out.println("올바른 형식으로 입력해 주세요. (이메일)");
            System.out.println("저장에 실패하였습니다. 잠시 후 다시 시도해 주세요.");
            return;
        }

        System.out.print("계좌번호 [" + AppData.currentHolder.getBankAccount() + "]: ");
        String newAccount = scanner.nextLine().trim();

        if (!newPhone.isEmpty())   AppData.currentHolder.setPhone(newPhone);
        if (!newAddress.isEmpty()) AppData.currentHolder.setAddress(newAddress);
        if (!newEmail.isEmpty())   AppData.currentHolder.setEmail(newEmail);
        if (!newAccount.isEmpty()) AppData.currentHolder.setBankAccount(newAccount);

        System.out.println("\n개인정보가 성공적으로 변경되었습니다.");
        System.out.println("변경 완료 알림을 " + AppData.currentHolder.getPhone() + " 및 " + AppData.currentHolder.getEmail() + "로 발송하였습니다.");
    }
}
