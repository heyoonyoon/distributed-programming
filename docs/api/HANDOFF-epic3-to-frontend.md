# 핸드오프 — Epic 3 → 프론트엔드(옆 pane)

> 작성: 백엔드 담당(Claude, 이 pane). 대상: 프론트엔드 작업 pane.
> 이 문서 + `docs/api/epic3-contract-billing.md`(API 명세 + 비즈니스 로직)를 단일 출처로 연동하세요.

## 1. 전달물
- **API 명세서 + 비즈니스 로직**: `docs/api/epic3-contract-billing.md`
  - 계약 조회(UC08), 미납 조회(UC07), 납부(UC10), 미납 고지서 스케줄러(UC16) 전부 포함.
  - 엔드포인트/요청·응답 JSON/상태코드/에러 매핑/도메인 규칙(온더플라이 청구 계산, FIFO 충당, 연 10% 연체이자, 결제 성공·실패 응답 규약)까지 명세.
- Epic 0~2 API도 같은 폴더(`docs/api/epic0-*`, `epic1-*`, `epic2-*`)에 있습니다.

## 2. ⚠️ 작업 전 필수: git worktree 따로 파세요
지금 커밋이 자꾸 겹칩니다. **같은 워킹 디렉터리에서 동시에 작업하지 말고, 본인 전용 worktree에서 작업하세요.**

```bash
# 레포 루트에서 (예: 프론트엔드 작업용 worktree를 형제 디렉터리에 생성)
cd /Users/heeyoon/Desktop/insurance
git fetch origin
git worktree add ../insurance-frontend -b <작업브랜치명> origin/main
cd ../insurance-frontend
# 이제 여기서만 작업/커밋. 백엔드 pane의 워킹트리와 분리됩니다.
```
- 브랜치명은 이슈 규약대로 `<이슈번호>-<slug>` (CLAUDE.md).
- 작업이 끝나 worktree가 더 필요 없으면 `git worktree remove ../insurance-frontend`.

## 3. ⚠️ 작업 순서: 지금까지 한 것부터 PR → 머지 → 그다음 진행
1. **먼저, 지금까지 작업한 것을 원격에 PR로 올리고 머지까지 끝내세요.**
   ```bash
   git push -u origin <현재브랜치>
   gh pr create --base main --fill
   gh pr merge --squash --delete-branch   # 또는 리뷰 후 머지
   ```
2. 머지로 main이 깨끗해진 뒤, 위 2번처럼 **새 worktree/브랜치를 origin/main 기준으로 다시 파서** 다음 작업을 진행하세요.
3. **다음 작업이 끝나면 그것도 똑같이 PR 생성 → 머지까지** 완료하세요.
   - 즉: (지금 것 PR+머지) → (새 브랜치에서 작업) → (PR+머지) 반복.
   - 한 이슈 = 한 브랜치 = 한 PR (여러 작업을 한 PR에 섞지 말 것).

## 4. 연동 시 주의(요약)
- 인증: 모든 `/contracts/**`는 `Authorization: Bearer <JWT>` 필요, **ROLE_POLICYHOLDER 전용**. 토큰 없음 401, 권한·타인계약 403, 없는 계약 404.
- 라우팅: `/contracts/unpaid`·`/contracts/payable`는 고정 경로(`/contracts/{id}`와 구분).
- 납부 실패(E1)는 HTTP **200** + 본문 `status="FAILED"`로 옵니다(에러 코드가 아님). 화면에서 `status`로 분기하세요.
- 계약 상세의 `paymentMethod`는 자동이체 등록 전 `"미등록"`, 등록 후 `"AUTO_DEBIT"`.
- 계약서 다운로드는 현재 `.txt`(octet-stream) stub입니다(실 PDF 아님).
- 미납 고지서(UC16)는 사용자 API가 없습니다(서버 스케줄러).
