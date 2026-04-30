package controller;

import common.DataStorage;
import common.enums.EReviewResult;
import common.vo.MedicalHistory;
import common.vo.VehicleInfo;
import contract.InsuranceApplication;
import external.AccidentHistory;
import product.InsuranceProduct;
import review.EnrollmentReview;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ApplicationController {
    public static void uc02InsuranceApplication(Scanner scanner) {
        System.out.println("\n=== 보험 가입 요청 ===");

        // 상품 선택 (UC01 선행 조건 반영: 종류 → 목록 → 선택)
        System.out.println("1. 의료보험  2. 자동차보험");
        System.out.print("보험 종류 선택: ");
        int typeChoice = scanner.nextInt();
        scanner.nextLine();

        List<? extends InsuranceProduct> products;
        boolean isCar;
        if (typeChoice == 1) {
            products = DataStorage.healthProducts;
            isCar = false;
        } else if (typeChoice == 2) {
            products = DataStorage.carProducts;
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
        DataStorage.applications.add(application);

        // 9단계: 접수 완료 안내 출력
        System.out.println("\n=== 접수 완료 ===");
        System.out.println("접수 번호     : " + application.getApplicationId());
        System.out.println("상태          : 심사 대기 (PENDING)");
        System.out.println("예상 처리 기간: 영업일 기준 3~5일");
        System.out.println("안내 이메일   : " + email + " 으로 발송되었습니다.");
        System.out.println("안내 문자     : " + phone + " 으로 발송되었습니다.");
    }

    public static void uc13EnrollmentReview(Scanner scanner) {
        System.out.println("\n=== 보험 가입 심사 ===");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

        // 2단계: 심사 대기 목록 출력
        List<InsuranceApplication> pending = new ArrayList<>();
        for (InsuranceApplication a : DataStorage.applications) {
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
            System.out.println("현재 담당자 [" + DataStorage.currentEmployee.getEmployeeId() + "]가 처리 중인 건입니다.");
            return;
        }

        EnrollmentReview review = existingReview != null ? existingReview
                : new EnrollmentReview("ERV-" + System.currentTimeMillis(), app.getApplicationId());
        if (existingReview == null) DataStorage.enrollmentReviews.add(review);
        review.lock();

        // 4단계: 상세 정보 출력
        System.out.println("\n[신청 상세 정보]");
        System.out.println("접수번호  : " + app.getApplicationId());
        System.out.println("가입자명  : " + app.getHolderName());
        System.out.println("주민번호  : " + DataStorage.currentHolder.getSsn());
        System.out.println("생년월일  : " + DataStorage.currentHolder.getBirthDate());

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
            AccidentHistory history = review.fetchAccidentHistory(DataStorage.currentHolder.getSsn());
            if (history == null) {
                // E1: 외부 연동 실패
                System.out.println("정보 조회 실패. 1.재시도  2.직접 입력 전환");
                System.out.print("선택: ");
                int retry = scanner.nextInt();
                scanner.nextLine();
                if (retry == 1) {
                    history = review.fetchAccidentHistory(DataStorage.currentHolder.getSsn());
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
            DataStorage.currentEmployee.requestDeptConsultation(review.getApplicationId(), reason);
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

            InsuranceProduct product = ProductController.findProduct(app.getProductId());
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
        for (EnrollmentReview r : DataStorage.enrollmentReviews) {
            if (r.getApplicationId().equals(applicationId)) return r;
        }
        return null;
    }
}
