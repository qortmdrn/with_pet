# Gleo Interface (Gleo AI 음성 명령 → 앱 function calling)

> 조사일: 2026-10-26. `G = https://document.pleos.ai/docs/pleos-only/app-integration-guide`
> 원문 페이지: `G/guide/core-concepts`, `G/guide/app-capability`, `G/guide/action_callback`, `G/guide/examples`, `G/guide/deliver-schema`, `G/use-cases/radio-app`

## 0. 결론: STT와 Gleo 중 무엇을 쓸까

| 명령 | 권장 경로 | 이유 |
|---|---|---|
| Emergency("강아지 응급이야", "동물병원 찾아줘") | **Gleo Interface** | 사용자가 먼저 꺼내는 전역 명령. Gleo가 의도를 파악해 앱을 호출함 |
| PetModeOn / PetModeOff | **Gleo Interface** (function 1개 + enum 파라미터) | 같은 이유 |
| Confirm / Reject("응", "아니") | **SpeechToText**(앱이 질문 후 `request()`) 또는 Gleo callback의 `status:"asking"` 흐름 | 앱 화면 맥락에 대한 단답이라 STT가 단순함 |

- STT 문서에 "음성 명령 기능은 제공하지 않으니 Gleo Interface를 쓰라"고 명시돼 있음 — https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro
- Gleo에서는 **별도 SDK/라이브러리가 필요 없음**. 표준 Android Intent와 Broadcast, 그리고 `assets/schema.json`만 쓰면 됨 — `G/guide/examples`, `G/guide/deliver-schema`
- 제약: actionCalls **최대 3개**, description은 **영어**, **스키마 검토·승인 후에만 활성화됨**. 그래서 승인 전 개발 단계에서는 adb로 Intent를 직접 보내 테스트해야 함(추정).
- 사용자 발화 문구(예: "강아지 응급이야")를 고정 트리거로 등록하는 기능은 **없음**. LLM이 description을 보고 판단함 — `G/guide/core-concepts`

## 1. 동작 개념 — `G/guide/core-concepts`
- OpenAI function calling과 비슷하고, Android용으로 확장됨.
- 흐름: 사용자 발화 → Gleo AI(LLM)가 function call 생성 → **Gleo Action**(Intent)으로 앱 호출 → 앱이 **Callback**(Broadcast)으로 결과 회신 → Gleo가 최종 응답(음성/텍스트) 생성.
- 이 가이드는 "개발 진행에 따라 계속 개선될 예정"이라고 적혀 있음. 변경 가능성 있음.

## 2. App Schema (`assets/schema.json`) — `G/guide/app-capability`
필드:
- `packageName`(string), `appDescription`(string, 영어로 1–2문장), `version`(string, **Pleos 앱 마켓에 등록된 앱 버전**)
- `actionCalls`(array, **최대 3**): 각 항목은 `{ "type": "function", "function": { ... } }`
  - `function.name`: **대문자 + 밑줄**(예: `ADD_REMINDER`)이어야 하고 고유해야 함. 이 이름이 그대로 **Intent action**이 됨
  - `function.description`: 영어. 예시 문장을 넣지 말 것. 다른 앱을 쓰지 못하게 유도하는 문구 금지
  - `function.parameters`: `{ "type":"object", "properties": {...}, "required": [...] }`
    - property type은 `string | number | boolean | array` 중 **하나**만 가능(`["string","null"]` 같은 배열 형태 불가). `enum`은 문자열만 허용
    - `required`에는 필수 파라미터만 넣음
  - `component`: `"activity"`(기본값, `startActivity`) 또는 `"service"`(`sendBroadcast`, UI 없음)
- 위치 관련 주의: 필드 설명에는 `component`가 `function`과 같은 레벨처럼 나열돼 있지만, **모든 JSON 예시에서는 `function` 객체 안**(`parameters` 다음)에 있음. 예시를 따를 것 — `G/guide/app-capability`, `G/use-cases/radio-app`
- 예시에만 `"invokeWord": "리마인더"` 필드가 있고 필드 설명에는 **없음**. 의미는 문서에 없음(앱 호출어로 추정).

우리 앱 스키마 초안 (추정 설계, 문서 형식을 따름):
```json
{
  "packageName": "com.withpet.car",
  "appDescription": "A pet travel companion app that manages pet comfort mode and finds emergency animal hospitals while driving.",
  "version": "<마켓 등록 버전>",
  "actionCalls": [
    { "type": "function", "function": {
        "name": "FIND_EMERGENCY_VET",
        "description": "Shows open animal hospitals nearby for a pet emergency and offers to call or navigate.",
        "parameters": { "type": "object", "properties": {}, "required": [] },
        "component": "activity" } },
    { "type": "function", "function": {
        "name": "SET_PET_MODE",
        "description": "Turns the in-car pet comfort mode on or off.",
        "parameters": { "type": "object",
          "properties": { "state": { "type": "string", "enum": ["ON", "OFF"], "description": "Desired pet mode state." } },
          "required": ["state"] },
        "component": "service" } }
  ]
}
```
(파라미터가 없는 function이 허용되는지는 **문서에 없음**.)

## 3. 스키마 전달·등록 — `G/guide/deliver-schema`
1. 파일 이름은 반드시 `schema.json`, 위치는 `assets/schema.json`. flavor를 나눠 쓰려면 `app/src/pleos/assets/schema.json`에 두면 됨(추정).
2. 당사(Pleos)가 가이드라인 준수 여부를 **검토하고, 승인 후 자동 활성화**. 제출 경로나 채널은 **문서에 없음**("이 문서의 저자에게 문의"라고만 적혀 있음).
3. 각 function마다 매니페스트에 action filter를 선언:
```xml
<activity android:name=".YourActivity">
  <intent-filter>
    <action android:name="FIND_EMERGENCY_VET" />
    <category android:name="android.intent.category.DEFAULT" />
  </intent-filter>
</activity>
```
   `component:"service"`일 때 BroadcastReceiver의 매니페스트 선언 예시(exported 여부, intent-filter)는 **문서에 없음**. 예시 코드에는 `BroadcastReceiver` 클래스만 나옴. Gleo가 `setPackage`로 명시적 broadcast를 보내므로 매니페스트에 `<receiver>` + action intent-filter를 선언하면 수신될 것으로 추정.
4. Gleo AI 앱은 **부팅 시 한 번** 모든 앱의 `assets/schema.json`을 스캔해 DB에 저장함. 앱을 설치·삭제할 때도 다시 읽음(AAOS).

## 4. Gleo Action 수신 형식 — `G/guide/examples`
- Gleo 내부 JSON: `{ "packageName", "action", "actionParameters": {...} }`
- Intent 변환 규칙:
  - `intent.action = <function name>`, `intent.setPackage(<packageName>)`
  - 파라미터: 본문에는 "key-value마다 putExtra"라고 돼 있지만, **샘플 코드는 `"ACTION_PARAM"` extra 하나에 JSON 문자열 전체**를 넣음. 수신 예시(라디오 앱 포함)도 모두 `getString("ACTION_PARAM")`을 `JSONObject`로 파싱함. → `ACTION_PARAM`을 우선 파싱하고 개별 extra도 대비하는 방어 구현을 권장(추정).
  - 주의: 샘플 송신 값이 `"\"{...}\""`처럼 따옴표로 한 겹 더 감싸져 있음. 파싱 전에 바깥 따옴표를 벗겨야 할 수도 있음(추정).
- `component:"activity"`는 `context.startActivity(intent)`로, `"service"`는 `context.sendBroadcast(intent)`로 전달됨.
- Activity가 이미 떠 있을 때 `onNewIntent`로 받는지는 **문서에 없음**. 라디오 예시는 `handleIntent(intent)` 패턴만 보여 줌. launchMode에 따라 `onNewIntent`도 처리해야 함(추정).

## 5. Callback (앱 → Gleo) — `G/guide/action_callback`, `G/guide/examples`
Broadcast로 보냄:
```kotlin
Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").apply {
    putExtra("packageName", packageName)
    putExtra("response", jsonObject.toString())
}.also { sendBroadcast(it) }
```
`response` JSON:
- `functionName`(필수): App Schema의 function name과 같아야 함
- `status`(필수): `"success" | "asking" | "fail"`
  - `asking`: 필수 파라미터가 빠졌거나 **사용자 확인이 필요할 때** 사용. Confirm/Reject 흐름에 활용할 수 있음
- `message`(필수): **영어** 완전한 문장. 고유명사 등은 다른 언어도 섞어 쓸 수 있음
- `mode`(선택, 기본값 `"voice"`): `"voice" | "beep" | "silent"`. 앱이 직접 TTS를 재생하면 `"silent"`
- `data`(선택이지만 강력 권장): **JSON 객체 배열을 문자열로** 넣음(예: `"[{\"name\":\"…\"}]"`)
- **타임아웃: Gleo는 7초 뒤 Action을 취소함** → 7초 안에 callback을 보내야 함
- 수신 측 권한이나 대상 패키지 지정(setPackage)은 **문서에 없음**. 샘플은 암시적 broadcast임.

## 6. 그 밖의 정책
- Music/Audio 앱 가이드에 Gleo 연동은 "앱 심사를 통과해 마켓에 출시되면 사용자가 바로 사용 가능"이라고 적혀 있음 — https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/music-audio-app-developement-guide
- Gleo가 호출되면 앱은 `AUDIOFOCUS_LOSS_TRANSIENT`를 받음 → 재생 중이던 소리(안정 사운드 등)를 즉시 멈춰야 함 — https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development
- LLM SDK는 종료됨. "Gleo AI 연동이 필요하면 Gleo Interface 사용" — https://document.pleos.ai/docs/pleos-only/release-note/playground-261015
- 에뮬레이터에서 Gleo AI가 동작하는지는 **문서에 없음**.
