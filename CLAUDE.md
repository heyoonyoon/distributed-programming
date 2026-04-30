# 프로젝트 지침

## 핵심 원칙

설계 문서가 곧 법이다. Claude는 설계된 것만 구현한다.
추가 기능, 추가 레이어, 추가 기술은 절대 금지.

## 기술 제약 (절대 위반 금지)

- 웹(Spring, REST, HTTP) 사용 금지
- 데이터베이스(JPA, JDBC, SQL) 사용 금지
- Docker, 클라우드 배포 금지
- 외부 라이브러리 추가 금지
- 허용: 순수 Java + Scanner + System.out.println + switch/case

## 구현 2단계 원칙

**Phase 1 (프로젝트 최초 1회 / 세션 최초 1회가 아님!!!!)**: @docs/class_diagram/total_class_diagram.md 읽고 → 클래스 전부 구현, 이것은 내가 클래스를 구현하라고 할 때만 진행된다.  total_class_diagram를 읽고자 할 때는 반드시 사용자의 허락을 받아야 한다.
**Phase 2 (UC별 반복)**: UC 시나리오 읽고 → 기존 클래스 사용해서 main()에 case 추가,
Phase 2에서 total_class_diagram.md를 절대로 읽지 않는다.

## main() 구조

```java
public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);

    while (true) {
        System.out.println("\n=== 보험 시스템 ===");
        System.out.println("1. 보험 상품 조회");
        System.out.println("2. 보험 가입 요청");
        // ... UC 순서대로
        System.out.println("0. 종료");
        System.out.print("선택: ");

        int choice = scanner.nextInt();
        switch (choice) {
            case 0: return;
            case 1: uc01_보험상품조회(scanner); break;
            case 2: uc02_보험가입요청(scanner); break;
            // ...
            default: System.out.println("잘못된 입력입니다.");
        }
    }
}
```

## UC 구현 절차 (매번 이 순서대로)
1. 해당 UC 시나리오 파일 읽기 (@docs/usecases/UC0N.md)
2. 항상 로드: @docs/class_diagram/00_overv1iew.md (목록과 관계만)
3. UC 구현 시: 00_overview.md의 UC별 필요 파일 매핑 참고 → 해당 파일만 추가로 읽기
- total_class_diagram는 usecase 구현시 절대로 읽어선 안된다. 
4. 구현 계획 사용자에게 보고 (승인 후 시작)
5. 시나리오 flow 그대로 구현 (flow 변경 절대 금지)
6. 구현 완료 후 수기 테스트 체크리스트 출력
7. docs/progress.md 업데이트

## 구현 완료 시 출력 형식

```
=== 구현 완료: UC0N [유스케이스명] ===

[변경 파일]
- Main.java: case N 추가
- [파일명]: [변경 내용]

[시나리오 ↔ 코드 매핑]
시나리오 1단계 → [메서드명]
시나리오 2단계 → [메서드명]



📝 progress.md 업데이트 완료: UC0N [구현✅]
```

## progress.md 업데이트 규칙

- 구현 완료 시: 구현 열 ⬜ → ✅
- 파일 위치: @docs/progress.md

## 추가 규칙

@.claude/rules/no-extra.md
@.claude/rules/uc-workflow.md