# 개인정보는 Policyholder가 소유하고 InsuranceApplication은 참조만 한다

UC02는 가입 시 이름·생년월일·주민등록번호·연락처·이메일 입력을 나열하지만, 이 값들은 로그인한 Policyholder가 이미 보유한다. InsuranceApplication에 이 값들을 스냅샷으로 복제하지 않고 `applicant`(Policyholder) 참조만 둔다. 심사 화면이나 사고이력 조회(ssn)는 `applicant`에서 읽는다. Application 고유 데이터는 가입 종류별 추가정보(VehicleInfo / MedicalHistory)뿐이다.

## Considered Options

- **참조(채택)**: 중복 제거, 단일 출처. 단점: 프로필이 바뀌면 "신청 당시 값"이 보존되지 않음.
- **스냅샷**: 신청 시점 개인정보를 Application에 박제 → 감사/심사 무결성. 단점: 5개 필드 중복, 동기화 부담.

이 프로젝트 규모에서 감사 무결성 요구가 없어 참조를 택했다. 추후 무결성이 필요해지면 스냅샷 컬럼을 추가하는 방향으로 되돌릴 수 있다.
