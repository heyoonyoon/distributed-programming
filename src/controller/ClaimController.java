package controller;

import claim.CarAccidentReport;
import claim.Claim;
import claim.HealthInsuranceClaim;
import common.DataStorage;
import common.enums.EClaimComplexity;
import common.enums.EClaimStatus;
import contract.InsuranceContract;
import common.enums.EContractStatus;
import external.AccidentHistory;
import payment.BenefitPayment;
import review.BenefitPaymentReview;
import user.InsuranceEmployee;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class ClaimController {
    public static void uc03ClaimStatusCheck(Scanner scanner) {
        System.out.println("\n=== 보상 처리 현황 ===");

        // 2단계: 진행 중인 보상 처리 목록 필터링 (PENDING, IN_REVIEW)
        List<Claim> inProgress = new ArrayList<>();
        for (Claim c : DataStorage.claims) {
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

    public static void uc04ClaimHistoryInquiry(Scanner scanner) {
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
        for (Claim c : DataStorage.claims) {
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

    public static void uc05HealthInsuranceClaim(Scanner scanner) {
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
        String claimId = "CLM-" + String.format("%03d", DataStorage.claims.size() + 1);
        EClaimComplexity complexity = requestAmount > 500000 ? EClaimComplexity.COMPLEX : EClaimComplexity.SIMPLE;
        EClaimStatus initialStatus = complexity == EClaimComplexity.SIMPLE ? EClaimStatus.APPROVED : EClaimStatus.PENDING;

        HealthInsuranceClaim newClaim = new HealthInsuranceClaim(
                claimId, new Date(), requestAmount, initialStatus,
                hospitalName, "AUTO", claimReason, documents, 0);
        newClaim.setComplexity(complexity);
        DataStorage.claims.add(newClaim);

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

    public static void uc09CarAccidentReport(Scanner scanner) {
        System.out.println("\n=== 자동차사고 접수 ===");

        // 선행 조건: 유효한 자동차보험 계약 확인
        InsuranceContract carContract = null;
        for (InsuranceContract c : DataStorage.contracts) {
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
        String claimId = "CLM-" + String.format("%03d", DataStorage.claims.size() + 1);
        String reason = accidentType + " 사고 (" + accidentDateTime + ", " + location + ")";
        String docs = photoFile.isEmpty() ? "없음" : photoFile;
        CarAccidentReport report = new CarAccidentReport(
                claimId, new Date(), requestAmount, EClaimStatus.PENDING,
                location, accidentType, vehicleNumber, reason, docs, 0);
        DataStorage.claims.add(report);

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

    public static void uc12BenefitPaymentReview(Scanner scanner) {
        System.out.println("\n=== 보험금 지급 심사 ===");

        // 2단계: 심사 대기 목록 출력
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        List<BenefitPaymentReview> pending = new ArrayList<>();
        for (BenefitPaymentReview r : DataStorage.benefitReviews) {
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
                    i + 1, claimId, DataStorage.currentHolder.getName(),
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
        System.out.println("청구인    : " + DataStorage.currentHolder.getName());
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
            BenefitPayment payment = review.approve(approvedAmount, DataStorage.currentHolder.getBankAccount());

            // claim 상태 업데이트
            claim.setStatus(EClaimStatus.APPROVED);
            claim.setPaidAmount(approvedAmount);

            System.out.println("\n=== 심사 결과: 승인 ===");
            System.out.println("지급 번호 : " + payment.getPaymentId());
            System.out.println("지급 금액 : " + String.format("%,d", payment.getPaidAmount()) + "원");
            System.out.println("지급 계좌 : " + DataStorage.currentHolder.getBankAccount());
            System.out.println("지급 일시 : " + sdf.format(payment.getPaidAt()));

            // 10단계: 결과 통보
            System.out.println("\n보험가입자에게 심사 결과 통보 이메일/문자를 발송하였습니다.");
        }

        review.unlock();
    }

    public static void uc14AssignStaff(Scanner scanner) {
        System.out.println("\n=== 담당자 지정 ===");

        // 1단계: 심사 대기 청구 건 목록
        List<BenefitPaymentReview> pending = new ArrayList<>();
        for (BenefitPaymentReview r : DataStorage.benefitReviews) {
            if (r.getResult() == null) pending.add(r);
        }

        if (pending.isEmpty()) {
            System.out.println("담당자 배정이 필요한 심사 건이 없습니다.");
            return;
        }

        System.out.println("\n번호 | 심사번호   | 청구번호   | 현재 담당자");
        System.out.println("-----|-----------|-----------|------------");
        for (int i = 0; i < pending.size(); i++) {
            BenefitPaymentReview r = pending.get(i);
            System.out.printf("%-4d | %-9s | %-9s | %s(%s)%n",
                    i + 1, r.getReviewId(), r.getClaimId(),
                    r.getAssignedStaffName(), r.getAssignedStaffId());
        }

        System.out.print("\n배정할 건 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > pending.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }
        BenefitPaymentReview review = pending.get(choice - 1);

        // 2단계: 자동 배정 or 수동 배정 선택
        System.out.println("\n1. 자동 배정  2. 수동 배정 (A1)");
        System.out.print("선택: ");
        int mode = scanner.nextInt();
        scanner.nextLine();

        InsuranceEmployee assigned = null;

        if (mode == 1) {
            // 자동 배정: 업무량 가장 적은 직원 선정
            for (InsuranceEmployee e : DataStorage.employees) {
                if (assigned == null || e.getCurrentLoad() < assigned.getCurrentLoad()) {
                    assigned = e;
                }
            }
            if (assigned == null) {
                // E1: 배정 가능한 직원 없음
                System.out.println("배정 가능한 담당자가 없습니다. 관리자에게 알림을 발송하였습니다.");
                return;
            }
            System.out.println("\n[자동 배정 결과]");
            System.out.println("선정 기준: 현재 업무량 최소");
            System.out.println("배정 직원: " + assigned.getName() + " (" + assigned.getDepartment() + ", 현재 " + assigned.getCurrentLoad() + "건)");
        } else {
            // A1: 수동 배정 — 직원 목록 출력
            System.out.println("\n[배정 가능한 직원 목록]");
            System.out.println("번호 | 이름   | 부서         | 현재 담당 건수");
            System.out.println("-----|-------|-------------|---------------");
            for (int i = 0; i < DataStorage.employees.size(); i++) {
                InsuranceEmployee e = DataStorage.employees.get(i);
                System.out.printf("%-4d | %-5s | %-11s | %d건%n",
                        i + 1, e.getName(), e.getDepartment(), e.getCurrentLoad());
            }
            if (DataStorage.employees.isEmpty()) {
                System.out.println("배정 가능한 담당자가 없습니다. 관리자에게 알림을 발송하였습니다.");
                return;
            }
            System.out.print("\n배정할 직원 번호: ");
            int empChoice = scanner.nextInt();
            scanner.nextLine();
            if (empChoice < 1 || empChoice > DataStorage.employees.size()) {
                System.out.println("잘못된 입력입니다.");
                return;
            }
            assigned = DataStorage.employees.get(empChoice - 1);
        }

        // 3단계: 담당자 연결 저장
        review.assignStaff(assigned);

        // 4단계: 담당자에게 알림 발송
        System.out.println("\n\"새 심사 건이 배정되었습니다.\" 알림을 [" + assigned.getName() + "]에게 발송하였습니다.");

        // 5단계: 담당자 정보 출력
        System.out.println("\n[청구 건 담당자 정보]");
        System.out.println("청구번호  : " + review.getClaimId());
        System.out.println("담당자    : " + review.getAssignedStaffName() + " (" + review.getAssignedStaffId() + ")");
        System.out.println("부서      : " + assigned.getDepartment());
    }

    public static void uc17BenefitPayment(Scanner scanner) {
        System.out.println("\n=== 보험금 지급 (시스템 자동) ===");

        // 1단계: 승인된 청구 건 확인
        List<HealthInsuranceClaim> approvedClaims = new ArrayList<>();
        for (Claim c : DataStorage.claims) {
            if (c instanceof HealthInsuranceClaim && c.getStatus() == EClaimStatus.APPROVED) {
                HealthInsuranceClaim hc = (HealthInsuranceClaim) c;
                if (hc.getPaidAmount() > 0) {
                    approvedClaims.add(hc);
                }
            }
        }

        if (approvedClaims.isEmpty()) {
            System.out.println("지급 대기 중인 승인 청구 건이 없습니다.");
            return;
        }

        System.out.println("\n[지급 대기 청구 목록]");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (HealthInsuranceClaim hc : approvedClaims) {
            System.out.println("- [" + hc.getClaimId() + "] 지급 예정액: "
                    + String.format("%,d", hc.getPaidAmount()) + "원");
        }

        // 2~5단계: 각 건 송금 처리
        System.out.println("\n[보험금 송금 처리 중...]");
        for (HealthInsuranceClaim hc : approvedClaims) {
            System.out.println("\n▶ 청구 ID: " + hc.getClaimId());
            System.out.println("  지급 금액 : " + String.format("%,d", hc.getPaidAmount()) + "원");
            System.out.println("  계좌 정보 : " + DataStorage.currentHolder.getBankAccount());

            // 2단계: 송금 요청
            String paymentId = "BPY-" + System.currentTimeMillis();
            BenefitPayment bp = new BenefitPayment(paymentId, hc.getPaidAmount(), DataStorage.currentHolder.getBankAccount());
            boolean transferred = bp.transfer();

            if (!transferred) {
                // E1: 송금 실패
                System.out.println("  ✗ 지급 실패: 계좌 오류 또는 은행 시스템 문제");
                System.out.println("  → 보험사 직원에게 송금 실패 알림 전송");
                System.out.println("  → 보험사 담당자에게 송금 실패 알림 전송 완료");
            } else {
                // 3단계: 지급 정보 저장
                System.out.println("  ✓ 송금 완료");
                System.out.println("  지급 ID   : " + bp.getPaymentId());
                System.out.println("  지급 일시  : " + sdf.format(bp.getPaidAt()));
                System.out.println("  입금 계좌  : " + bp.getBankAccount());

                // 4단계: 가입자 안내
                bp.notifyPolicyholder(DataStorage.currentHolder.getEmail(), DataStorage.currentHolder.getPhone());

                // 5단계: 청구 상태 변경
                hc.setStatus(EClaimStatus.APPROVED);
            }
        }

        System.out.println("\n[보험금 지급 처리 완료]");
    }

    public static void uc15AccidentHistoryInquiry(Scanner scanner) {
        System.out.println("\n=== 자동차 사고 이력 조회 (금융감독원 연동) ===");

        // 2단계: 주민등록번호 기반 외부 기관 조회
        System.out.print("조회할 주민등록번호 입력: ");
        scanner.nextLine();
        String ssn = scanner.nextLine().trim();

        System.out.println("\n[금융감독원 사고 이력 조회 중...]");
        AccidentHistory history = AccidentHistory.fetch(ssn);

        if (history == null) {
            // E1: 외부 연동 실패
            System.out.println("정보 조회 실패.");
            System.out.println("1. 재시도  2. 직접 입력 전환");
            System.out.print("선택: ");
            int retry = scanner.nextInt();
            scanner.nextLine();
            if (retry == 1) {
                history = AccidentHistory.fetch(ssn);
            } else {
                // E1-3: 직접 입력 전환
                System.out.println("\n[직접 입력]");
                System.out.print("사고 건수: ");
                int accidentCount = scanner.nextInt();
                System.out.print("지급액 합계: ");
                int totalPaid = scanner.nextInt();
                scanner.nextLine();
                System.out.print("면허 상태 (VALID/SUSPENDED/REVOKED): ");
                String licenseStatus = scanner.nextLine().trim();
                history = new AccidentHistory(ssn, accidentCount, totalPaid, licenseStatus);
            }
        }

        if (history == null) {
            System.out.println("사고 이력 조회에 실패하였습니다.");
            return;
        }

        // 4단계: 조회된 정보 출력
        System.out.println("\n=== 사고 이력 조회 결과 ===");
        System.out.println("주민등록번호 : " + ssn);
        System.out.println("사고 건수     : " + history.getAccidentCount() + "건");
        System.out.println("지급액 합계   : " + String.format("%,d", history.getTotalPaidAmount()) + "원");
        System.out.println("면허 상태     : " + history.getLicenseStatus());
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        if (!history.getRecords().isEmpty()) {
            System.out.println("\n[사고 상세 이력]");
            for (external.AccidentRecord r : history.getRecords()) {
                System.out.println("  - 날짜: " + sdf.format(r.getAccidentDate())
                        + " / 유형: " + r.getAccidentType()
                        + " / 지급액: " + String.format("%,d", r.getPaidAmount()) + "원");
            }
        }
        System.out.println("\n조회가 완료되었습니다.");
    }

    private static HealthInsuranceClaim findHealthClaim(String claimId) {
        for (Claim c : DataStorage.claims) {
            if (c instanceof HealthInsuranceClaim && c.getClaimId().equals(claimId)) {
                return (HealthInsuranceClaim) c;
            }
        }
        return null;
    }
}
