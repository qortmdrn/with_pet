# 앱 심사 가이드: 음성(TTS/STT/Gleo)과 SDK 권한 관련 항목

> 조사일: 2026-10-26. `R = https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide`

## 1. SDK·API 사용 심사 — `R/review-functionality-performance-stability#sdk-api-review`
- 차량 상태를 바꾸거나 운전 안전에 영향을 줄 수 있는 SDK·API에는 별도 심사 기준이 적용됨.
- **Vehicle SDK 퍼미션은 Pleos App Market과 사전 협의·승인된 경우에만 선언할 수 있음. 승인되지 않은 퍼미션을 선언하면 심사 불통과.**
  → 현재 `pleos` 매니페스트의 `CONTROL_CAR_CLIMATE`, `CONTROL_CAR_WINDOWS`, `CAR_CLIMATE`, `CAR_WINDOWS`는 사전 승인이 필요함.
- TTS/STT/NaviHelper 권한에 대한 별도 심사 조항은 **문서에 없음**(Vehicle SDK만 명시).
- 관련 내용: My Project의 SDK 사용 신청에서 NaviHelper와 Vehicle State는 바로 사용 가능, TTS·STT·FusedLocation·Vehicle Control은 "준비 중" — https://document.pleos.ai/docs/pleos-only/create-my-project

## 2. 완성도·성능 (음성 기능에 해당하는 것) — `R/review-functionality-performance-stability`
- 미디어가 정상 출력돼야 함(예: "앱 사운드가 끊어짐"은 오류 사례). TTS 앞부분이 잘리는 것을 막으려면 150–200ms pre-roll 무음 권장 — https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide
- 버튼 반응은 2초 이내, 넘으면 진행 중임을 UI로 표시. 앱 실행과 콘텐츠 로드는 10초 이내. 터치 응답은 0.25초 이내.
- 테스트용 더미 데이터나 임시 콘텐츠 금지. **현재 "안정 사운드 임시 음원"(커밋 56d449f)은 제출 전 교체 필요**(추정 해당).
- 백그라운드에서 돌아오면 마지막 상태를 복원해야 함.
- 핵심 기능과 무관한 시스템 설정 변경을 요구하면 안 됨(볼륨 관련 → `phone-and-volume.md`).

## 3. 개인정보 — `R/review-app-registration-info`, https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/security-privacy
- 개인정보 처리방침에 **마이크, 전화, 연락처, GPS** 등 접근 권한을 기재해야 함.
- 위치나 **음성**을 요구할 때는 왜 필요한지 명확히 설명하고, 동의 흐름을 간단하게 설계할 것.
- 서버 모드 STT/TTS를 쓰면 음성이나 텍스트가 서버로 전송됨 → 처리방침에 반영(추정).

## 4. 운전자 주의 분산과 주행 규제
- 주행 중 터치를 최소화하고 **음성 명령**이나 물리 버튼과 연계하며, 텍스트 입력보다 프리셋·최근 항목·음성 입력을 우선할 것 — https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline
- 주행 중 "검색"은 **음성으로만** 허용(북미 표 기준). 키보드와 다이얼패드는 제한 — https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy
- CarUxRestrictions를 따라야 함 — https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide

## 5. 오디오 포커스 (TTS 출력·안정 사운드) — https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development
- Pleos Connect 오디오 포커스 정책을 반드시 따를 것.
- Gleo(VOICE_COMMAND)가 호출돼 `AUDIOFOCUS_LOSS_TRANSIENT`를 받으면 **즉시 재생을 멈추고**, 필요하면 포커스를 반납(abandon)해야 함.
- `..._CAN_DUCK`이면 duck(볼륨 감쇄)할 것.

## 6. Gleo Interface 관련
- App Schema는 Pleos의 **가이드라인 검토 후 승인되어야 활성화**됨(제출 경로는 문서에 없음). description에 Gleo 동작을 조작하거나 다른 앱 사용을 억제하는 문구 금지 — https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema , https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability
- schema의 `version`은 마켓에 등록된 앱 버전과 맞춰야 함.

## 7. 카테고리
- 앱 카테고리와 실제 기능이 다르면 반려됨 — `R/review-functionality-performance-stability`
