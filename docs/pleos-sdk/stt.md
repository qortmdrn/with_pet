# SpeechToText SDK 정리

> 조사일: 2026-10-26. 가이드 기준 버전 **SpeechToText v3.3.1**. API Reference는 사이트 JS 번들에서 원문을 추출함.
> `B = https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText`
> `R = B/speech-to-text/ai.pleos.playground.stt/speech-to-text`

## 0. 핵심 결론 (음성 명령 용도)
- STT는 **앱이 `request()`를 호출할 때만** 마이크를 열고 텍스트를 돌려줌. 웨이크업 워드나 상시 대기 기능은 **문서에 없음** — `R/request`, `B/start-and-stop`
- 문서에 "SpeechToText는 음성 명령 기능을 제공하지 않습니다. 인식한 명령으로 앱 기능을 실행하려면 Gleo Interface를 참고하세요"라고 명시돼 있음 — `B/intro`
- 따라서 "앱이 질문한 뒤 응/아니로 답받기" 같은 **앱 주도 단발 인식**에 적합함. 사용자가 먼저 말을 거는 전역 명령은 `gleo-interface.md` 참고.

## 1. 의존성
- `ai.pleos.playground:SpeechToText:3.3.1`, 저장소 `https://nexus-playground.pleos.ai/repository/maven-releases/` — `B/importing-sdk`
- 2026-10-19 릴리즈 노트 기준 최신 v3.3.1 — https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015

## 2. 권한
```xml
<uses-permission android:name="pleos.car.permission.STT_SERVICE" />
```
- 모든 STT API에 필요. 빠지면 `PERMISSION_DENIED` — `B/declaring-permissions`
- `android.permission.RECORD_AUDIO`가 필요한지는 **문서에 없음**. 마이크는 Speech Service가 연다고 설명돼 있음 — `B/intro`. MIC_ERROR가 나면 "센서 개인정보 보호 설정 확인"을 안내함 — `B/error-code`

## 3. 모드와 앱 등록
- `HYBRID`(기본값: 서버 우선, 실패하면 온디바이스), `SERVER`, `ON_DEVICE`. **ON_DEVICE만 쓰면 앱 등록이 필요 없음** — `B/intro`, `B/app-registration`
- `registerApp(clientID, clientSecret, OnServerConnectionListener)`는 TTS와 똑같고, 자격 증명도 My Project의 "SDK > 클라이언트 ID/시크릿"에서 가져옴 — https://document.pleos.ai/docs/pleos-only/create-my-project
- start-and-stop 페이지에 "서버 기반 모드에서는 앱 등록 **또는 UA 인증**이 필요"라는 문구가 있음. UA 인증이 무엇인지는 **문서에 없음** — `B/start-and-stop`
- 같은 create-my-project 페이지에 TTS/STT가 "준비 중(신청 불가)"으로 표기돼 있어 문서끼리 상충함. 콘솔에서 확인 필요.

## 4. API 시그니처 (`ai.pleos.playground.stt`) — `R/entry`
```kotlin
class SpeechToText(context: Context, mode: Mode = Mode.HYBRID)
fun initialize(); fun initialize(listener: OnInitializeListener)
fun registerApp(clientID: String, clientSecret: String, listener: OnServerConnectionListener)
fun addListener(listener: ResultListener); fun removeListener(listener: ResultListener)
fun setLocale(locale: Locale)
fun request()          // 마이크 열고 인식 시작
fun stop()             // 인식 중지, 마이크 닫음
fun sendAudio(bytes: ByteArray)   // ON_DEVICE 전용. 외부 PCM 입력. request() 불필요
fun completeAudioSend()           // ON_DEVICE 전용
fun release()
```
리스너 (`ai.pleos.playground.stt.listener`):
```kotlin
interface ResultListener {
    fun onReady()
    fun onStartedRecognition()
    fun onUpdated(stt: String, completed: Boolean)  // completed=true면 최종 결과
    fun onUpdatedEpdData(on: Long, off: Long)       // EPD 시작/끝 byte offset
    fun onUpdatedRms(rms: Double)
    fun onEndedRecognition()
    fun onError(errorType: ErrorType)
}
interface OnInitializeListener { fun onSuccess(); fun onFailure(errorType: ErrorType) }
interface OnServerConnectionListener { fun onConnected(); fun onFailed(errorType: ErrorType) }
```
출처: `B/result-listeners`, `B/speech-to-text/ai.pleos.playground.stt.listener/result-listener/entry`
- 리스너는 `request()`보다 **먼저** 등록해야 함 — `B/result-listeners`
- `stop()`을 부르면 `onEndedRecognition()`이 옴 — `B/start-and-stop`
- 인식 타임아웃이나 무음 종료 기준은 **문서에 없음** (EPD 콜백만 있음).
- `ai.pleos.playground.stt.data.InferenceSource { ON_DEVICE, SERVER }` enum은 있지만 공개 메서드에서 쓰이는 곳은 **문서에 없음**.

## 5. Locale
- `ko-KR`, `en-US`, `fr-CA`, `es-US`, `es-MX`. `setLocale(Locale.KOREA)`를 request 전에 호출. 기본값 규칙은 TTS와 같음 — `B/set-locale`

## 6. 에러 (`ai.pleos.playground.stt.constant.ErrorType`) — `B/error-code`
`AUDIO_ERROR`, `INVALID_REQUEST_ERROR`(중복 요청이나 호출 순서 오류 포함), `MIC_ERROR`, `NETWORK_ERROR`, `NOT_SUPPORTED_API`, `ON_DEVICE_ENGINE_ERROR`, `PERMISSION_DENIED`, `SERVER_INTERNAL_ERROR`, `TIMEOUT`, `UNAUTHENTICATED`, `UNAVAILABLE_LANGUAGE`, `UNKNOWN` — `else` 분기 필수.

## 7. 스레딩
- 콜백 스레드는 **문서에 없음**. Flow로 내보낼 때는 `callbackFlow` + `trySend` 사용을 권장(추정).

## 8. Confirm/Reject 인식 최소 순서 (추정 설계)
1. `SpeechToText(ctx, Mode.ON_DEVICE)` → `initialize(listener)` → `addListener` → `setLocale(KOREA)`
2. TTS로 질문 → `onDone` 후 `request()` (자기 TTS 음성이 인식되는 것을 막음. 추정)
3. `onUpdated(stt, completed=true)`에서 텍스트를 정규화하고 "응/추가해" → Confirm, "아니/나중에" → Reject로 키워드 매칭
4. 인식이 끝나거나 일정 시간이 지나면 `stop()`. 종료 시 `removeListener` → `release()`
- 단점: 사용자가 먼저 "강아지 응급이야"라고 말하는 상황은 STT만으로 받을 수 없음. 상시 `request()` 반복은 문서가 권장하는 패턴이 아니고, 심사와 프라이버시 리스크가 있음(추정).
