package uc;

import claim.Claim;
import claim.CarAccidentReport;
import claim.HealthInsuranceClaim;
import common.enums.EClaimComplexity;
import common.enums.EClaimStatus;
import payment.BenefitPayment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class UcClaim {

    public static void uc03ClaimStatusCheck(Scanner scanner) {
        System.out.println("\n=== 보상 처리 현황 ===");

        List<Claim> inProgress = new ArrayList<>();
        for (Claim c : AppData.claims) {
            EClaimStatus s = c.getStatus();
            if (s == EClaimStatus.PENDING || s == EClaimStatus.IN_REVIEW) {
                inProgress.add(c);
            }
        }

        if (inProgress.isEmpty()) {
            System.out.println("현재 진행 중인 보상 처리 건이 없습니다.");
            return;
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        System.out.println("\n번호 | 접수번호   | 접수일     | 보험 종류        | 현재 상태");
        System.out.println("-----|-----------|------------|-----------------|----------");
        for (int i = 0; i < inProgress.size(); i++) {
            Claim c = inProgress.get(i);
            System.out.printf("%-4d | %-9s | %s | %-15s | %s%n",
                    i + 1, c.getClaimId(), sdf.format(c.getClaimDate()),
                    c.getClaimType(), statusLabel(c.getStatus()));
        }

        System.out.print("\n상세보기할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > inProgress.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

        Claim selected = inProgress.get(choice - 1);
        System.out.println("\n=== 처리 진행 현황: " + selected.getClaimId() + " ===");
        printProgressFlow(selected.getStatus());
        System.out.println("\n예상 처리 기간: " + expectedDuration(selected.getStatus()));
        System.out.println("담당자        : " + selected.getAssignedStaff());
        System.out.println("담당자 연락처 : " + selected.getStaffContact());
    }

    public static void uc04ClaimHistoryInquiry(Scanner scanner) {
        System.out.println("\n=== 보상 이력 조회 ===");

        System.out.println("조회 기간을 선택하세요.");
        System.out.println("1. 최근 1개월  2. 최근 3개월  3. 최근 6개월  4. 최근 1년(기본)  5. 전체");
        System.out.print("선택 (기본값 4): ");
        String input = scanner.nextLine().trim();
        int periodChoice = input.isEmpty() ? 4 : Integer.parseInt(input);

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

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        List<Claim> history = new ArrayList<>();
        for (Claim c : AppData.claims) {
            if (c.getClaimDate().getTime() >= fromMillis) {
                history.add(c);
            }
        }

        System.out.println("\n[조회 기간: " + periodLabel + "]");

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

        System.out.print("\n상세보기할 번호 (0.뒤로): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 0) return;
        if (choice < 1 || choice > history.size()) {
            System.out.println("잘못된 입력입니다.");
            return;
        }

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
        System.out.println("\n--- 청구 정보 입력 ---");

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

        String documents;
        while (true) {
            System.out.println("증빙 서류 파일명 입력 (허용: PDF, JPG, PNG)");
            System.out.print("파일명: ");
            documents = scanner.nextLine().trim();
            String lower = documents.toLowerCase();
            if (lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".png")) break;
            System.out.println("지원하지 않는 파일 형식입니다. (허용: PDF, JPG, PNG)");
        }

        System.out.println("\n청구 신청 중...");

        String claimId = "CLM-" + String.format("%03d", AppData.claims.size() + 1);
        EClaimComplexity complexity = requestAmount > 500000 ? EClaimComplexity.COMPLEX : EClaimComplexity.SIMPLE;
        EClaimStatus initialStatus = complexity == EClaimComplexity.SIMPLE ? EClaimStatus.APPROVED : EClaimStatus.PENDING;

        HealthInsuranceClaim newClaim = new HealthInsuranceClaim(
                claimId, new Date(), requestAmount, initialStatus,
                hospitalName, "AUTO", claimReason, documents, 0);
        newClaim.setComplexity(complexity);
        AppData.claims.add(newClaim);

        System.out.println("청구 접수 번호: " + claimId);
        System.out.println("복잡도 판별 결과: " + (complexity == EClaimComplexity.SIMPLE ? "간단한 청구" : "복잡한 청구"));

        if (complexity == EClaimComplexity.SIMPLE) {
            int paidAmount = (int)(requestAmount * 0.9);
            BenefitPayment payment = new BenefitPayment("PAY-" + System.currentTimeMillis(), paidAmount, "***-***-123456");
            payment.transfer();

            System.out.println("\n=== 보험금 즉시 지급 완료 ===");
            System.out.println("지급 금액  : " + String.format("%,d", payment.getPaidAmount()) + "원");
            System.out.println("지급 계좌  : " + "***-***-123456");
            System.out.println("처리 결과를 이메일/문자로 발송하였습니다.");
        } else {
            System.out.println("\n=== 담당자 배정 (UC14) ===");
            System.out.println("담당자    : 홍길동 심사관");
            System.out.println("연락처    : 02-1234-5678");
            System.out.println("\n담당자 배정 후 심사를 진행합니다.");
            System.out.println("처리 현황은 '보상 처리 현황' 메뉴에서 확인하실 수 있습니다.");
            System.out.println("안내를 이메일/문자로 발송하였습니다.");
        }
    }

    public static void uc09CarAccidentReport(Scanner scanner) {
        System.out.println("\n=== 자동차사고 접수 ===");

        contract.InsuranceContract carContract = null;
        for (contract.InsuranceContract c : AppData.contracts) {
            if (c.getProductType().equals("자동차보험") && c.getStatus() == common.enums.EContractStatus.ACTIVE) {
                carContract = c;
                break;
            }
        }
        if (carContract == null) {
            System.out.println("유효한 자동차보험 계약이 없습니다.");
            return;
        }
        System.out.println("계약: [" + carContract.getContractId() + "] " + carContract.getProductName());

        System.out.println("\n--- 사고 정보 입력 ---");

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

        System.out.print("부상자가 있습니까? (1.예 / 2.아니오): ");
        int injuryChoice = scanner.nextInt();
        scanner.nextLine();
        boolean hasInjury = injuryChoice == 1;
        int injuredCount = 0;
        String injuryDetail = "";

        if (hasInjury || accidentType.equals("대인")) {
            System.out.println("\n--- 부상자 정보 입력 (A1) ---");
            System.out.print("부상자 수: ");
            injuredCount = scanner.nextInt();
            scanner.nextLine();
            System.out.print("부상 정도 (경상/중상/사망): ");
            injuryDetail = scanner.nextLine().trim();
        }

        String photoFile;
        while (true) {
            System.out.print("현장 사진 파일명 (예: photo.jpg, 없으면 엔터): ");
            photoFile = scanner.nextLine().trim();
            if (photoFile.isEmpty()) break;
            if (photoFile.toLowerCase().contains("big")) {
                System.out.println("파일 크기는 개당 10MB 이하여야 합니다.");
                continue;
            }
            break;
        }

        System.out.println("\n접수 처리 중...");

        String claimId = "CLM-" + String.format("%03d", AppData.claims.size() + 1);
        String reason = accidentType + " 사고 (" + accidentDateTime + ", " + location + ")";
        String docs = photoFile.isEmpty() ? "없음" : photoFile;
        CarAccidentReport report = new CarAccidentReport(
                claimId, new Date(), requestAmount, EClaimStatus.PENDING,
                location, accidentType, vehicleNumber, reason, docs, 0);
        AppData.claims.add(report);

        System.out.println("\n보험사 담당자에게 사고 접수 알림이 발송되었습니다.");
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

    public static String statusLabel(EClaimStatus status) {
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
}
