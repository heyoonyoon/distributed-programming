# 0008. 보험금 송금은 PaymentGateway와 분리한 BenefitTransferGateway 포트로 둔다

보험금 지급(UC17)의 외부 송금을 기존 `PaymentGateway`에 메서드로 얹지 않고, 별도 포트 `BenefitTransferGateway`(+ `MockBenefitTransferGateway`)로 신설한다.

이유: 두 연동은 자금 흐름과 입력이 반대다.
- `PaymentGateway.charge(PaymentMethod, amount, paymentInfo)` — 가입자→보험사 **수납(인출)**. 결제수단(카드·자동이체)이 입력.
- `BenefitTransferGateway.transfer(bankAccount, amount)` — 보험사→가입자 **지급(송금)**. 가입자 계좌번호가 입력.

`PaymentMethod`는 수납 수단이지 지급 계좌가 아니므로 charge() 시그니처에 보험금 송금을 끼우면 의미가 왜곡된다.

## Considered Options

- **채택(포트 분리)**: 각 포트가 한 방향·한 입력만 책임진다. Mock도 독립적으로 성공/실패를 시뮬레이션. 단점: 외부 연동 추상화가 둘로 늘어난다.
- **PaymentGateway에 transfer() 추가**: 외부 연동 진입점을 하나로. 단점: charge/transfer가 한 인터페이스에 섞여 PaymentMethod 기반 수납과 계좌 기반 지급이 한 곳에서 충돌하고, 구현체가 두 책임을 동시에 떠안는다.

되돌리기 비교적 가능하나, 지급 흐름(BenefitPayoutService)이 이 포트에 묶이므로 결정을 기록해 둔다.
