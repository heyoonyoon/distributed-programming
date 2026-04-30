package controller;

import common.DataStorage;
import java.util.Scanner;

public class UserController {
    public static void uc06UpdatePersonalInfo(Scanner scanner) {
        System.out.println("\n=== 개인정보 수정 ===");

        // 2단계: 본인 인증 화면 출력
        System.out.println("\n--- 본인 인증 ---");
        System.out.println("인증번호 123456이 " + DataStorage.currentHolder.getPhone() + "로 발송되었습니다.");

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
        System.out.println("연락처  : " + DataStorage.currentHolder.getPhone());
        System.out.println("주소    : " + DataStorage.currentHolder.getAddress());
        System.out.println("이메일  : " + DataStorage.currentHolder.getEmail());
        System.out.println("계좌번호: " + DataStorage.currentHolder.getBankAccount());

        // 5단계: 수정할 항목 변경 (E2: 저장 실패 시뮬레이션 없음 — 항상 성공)
        System.out.println("\n--- 수정할 정보 입력 (변경 없으면 엔터) ---");

        System.out.print("연락처 [" + DataStorage.currentHolder.getPhone() + "]: ");
        String newPhone = scanner.nextLine().trim();
        if (!newPhone.isEmpty()) {
            if (!newPhone.matches("\\d{10,11}")) {
                System.out.println("올바른 형식으로 입력해 주세요. (10~11자리 숫자)");
                System.out.println("저장에 실패하였습니다. 잠시 후 다시 시도해 주세요.");
                return;
            }
        }

        System.out.print("주소 [" + DataStorage.currentHolder.getAddress() + "]: ");
        String newAddress = scanner.nextLine().trim();

        System.out.print("이메일 [" + DataStorage.currentHolder.getEmail() + "]: ");
        String newEmail = scanner.nextLine().trim();
        if (!newEmail.isEmpty() && !newEmail.contains("@")) {
            System.out.println("올바른 형식으로 입력해 주세요. (이메일)");
            System.out.println("저장에 실패하였습니다. 잠시 후 다시 시도해 주세요.");
            return;
        }

        System.out.print("계좌번호 [" + DataStorage.currentHolder.getBankAccount() + "]: ");
        String newAccount = scanner.nextLine().trim();

        // 6단계: 변경된 정보 저장 + 변경 기록
        if (!newPhone.isEmpty())   DataStorage.currentHolder.setPhone(newPhone);
        if (!newAddress.isEmpty()) DataStorage.currentHolder.setAddress(newAddress);
        if (!newEmail.isEmpty())   DataStorage.currentHolder.setEmail(newEmail);
        if (!newAccount.isEmpty()) DataStorage.currentHolder.setBankAccount(newAccount);

        // 7단계: 완료 메시지 + 변경 완료 알림 발송
        System.out.println("\n개인정보가 성공적으로 변경되었습니다.");
        System.out.println("변경 완료 알림을 " + DataStorage.currentHolder.getPhone() + " 및 " + DataStorage.currentHolder.getEmail() + "로 발송하였습니다.");
    }
}
