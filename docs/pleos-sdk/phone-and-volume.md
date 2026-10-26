# 전화 발신(블루투스 폰)과 미디어 볼륨 제어

> 조사일: 2026-10-26. pleos-only 문서 전체(1,961페이지, SDK API Reference 포함)를 키워드(전화, 통화, dialer, telephony, ACTION_DIAL/CALL, 블루투스, AudioManager, 볼륨, volume, STREAM_MUSIC)로 검색함.

## 1. 전화 발신
**결론: Pleos Connect SDK(Vehicle, NaviHelper, TTS, STT, FusedLocation)에는 전화·다이얼러·블루투스 통화 API가 없음.**
- Vehicle SDK 도메인 목록에 Telephony·Phone이 없음 — https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro
- 하드웨어 접근 정책에 "모바일 전용 API는 차량에서 제공되지 않거나 제한될 수 있음. **제한될 수 있는 기능: Telephony(전화)**, SMS, 일부 센서, 캘린더, 주소록. 대응 전략: 필수가 아닌 기능은 제거하거나, 지원 여부를 확인한 뒤 동작하는 Optional 구조로 설계" — https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture
- 표준 Android `Intent.ACTION_DIAL` / `ACTION_CALL`(AAOS Dialer와 블루투스 HFP)을 쓰는 것에 대한 허용·금지 안내는 **문서에 없음**.
  - 따라서 `ACTION_DIAL`(번호만 채워 Dialer를 띄움, 권한 불필요)을 `resolveActivity`로 확인한 뒤 실행하고, 실패하면 번호를 크게 표시하는 Optional 구조가 정책 문구에 맞음(추정). `ACTION_CALL`은 `CALL_PHONE` 위험 권한이 필요하고 심사 리스크가 있음(추정).
  - Pleos Connect 기본 Dialer가 있는지, 블루투스 폰 연결 상태를 조회하는 방법은 **문서에 없음**. 에뮬레이터 v2.1.0에서 "블루투스 동글 연동 제공 종료"라 에뮬레이터 검증도 어려움 — https://document.pleos.ai/docs/pleos-only/release-note/emulator-261015
- 주행 규제: 다이얼패드는 `UX_RESTRICTIONS_NO_DIALPAD(1)` 대상이라 주행 중 제한됨(북미 표 기준, 한국 표도 같은 형식) — https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy
- 심사: 앱 등록 정보에 "전화, 연락처 등의 접근 권한"을 개인정보 처리방침에 기재해야 함 — https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-app-registration-info
- Pleos Platform SDK(`ai.pleos:pleos-manager-stubs-system`)는 **승인된 privileged 앱 전용** System API라 3rd party 앱에는 해당 없음. 이 SDK에도 telephony 도메인은 보이지 않음 — https://document.pleos.ai/api-reference/pleos-platform-sdk/get-started

## 2. 미디어 볼륨 제어
**결론: SDK에 시스템·미디어 볼륨을 설정하는 API가 없음.**
- 유일한 "볼륨" API는 Vehicle `Toll` 도메인의 **ETCS(하이패스) 음성 안내 볼륨**(`get/set/registerEtcsVoiceGuidanceVolume`, 권한 `CAR_TOLL`)뿐이고 미디어 볼륨과는 무관함 — https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/toll/set-etcs-voice-guidance-volume
- 가이드 문서들은 볼륨을 **오디오 포커스 정책**으로만 다룸: AAOS CarAudioContext(MUSIC, NAVIGATION, VOICE_COMMAND, CALL 등) 단위로 볼륨 그룹을 관리하고, `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`을 받으면 앱이 스스로 볼륨을 줄이도록(duck) 함 — https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development
- "운전에 중요한 오디오 안내가 재생될 때는 오디오 포커스 정책에 따라 미디어를 일시정지하거나 볼륨을 조정" — https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline
- 심사: "앱의 핵심 기능과 관련 없는 시스템 설정을 변경하도록 제안·요구하면 안 됨" — https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability
- `AudioManager.setStreamVolume(STREAM_MUSIC, …)`로 시스템 볼륨을 바꾸는 것에 대한 허용·금지 안내는 **문서에 없음**. AAOS에서는 볼륨이 CarAudioManager의 볼륨 그룹(`android.car.permission.CAR_CONTROL_AUDIO_VOLUME`, signature 권한)으로 관리되므로, 3rd party가 `AudioManager`로 시스템 볼륨을 바꾸면 무시되거나 실패할 가능성이 높음(추정, AAOS 일반 지식).
- 실현 가능한 대안(추정): 앱이 재생하는 소리(안정 사운드 등)의 **자기 플레이어 볼륨**(`ExoPlayer.volume` / `MediaPlayer.setVolume`)만 조절함. 즉 `MediaVolumeControl`을 "앱 내 재생 볼륨"으로 재정의.

## 3. 결정 (2026-10-26)
- 전화: `ACTION_DIAL`로 다이얼러에 번호만 채운다 (`PleosPhoneDialer`). 자동 발신하지 않는다.
- 볼륨: 앱 재생 볼륨만 조절한다 (`AppPlaybackVolume` → `CalmPlaybackService`의 플레이어).

## 4. 요약
| 기능 | SDK API | 표준 Android 경로 | 판정 |
|---|---|---|---|
| 블루투스 폰으로 발신 | 없음 | `ACTION_DIAL`(문서상 허용 여부 미기재) | Optional 구조로 시도 가능(추정). 실패 시 번호 표시 |
| 미디어(시스템) 볼륨 설정 | 없음 | AudioManager (AAOS에서 제한적) | SDK로는 불가. 앱 내 플레이어 볼륨으로 대체 |
