# TextToSpeech SDK 정리 (휴식 알림 음성 안내용)

> 조사일: 2026-10-26. 가이드 기준 버전 **TextToSpeech v3.3.1**.
> API Reference 페이지는 클라이언트 렌더링이라 WebFetch로는 본문이 보이지 않습니다. 사이트 JS 번들(Dokka HTML)에서 원문을 직접 뽑아 확인했습니다.
> 약어: `B = https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech`
> `R = B/text-to-speech/ai.pleos.playground.tts/text-to-speech`

## 1. 의존성

- 저장소: `maven("https://nexus-playground.pleos.ai/repository/maven-releases/")` (지금 쓰는 저장소와 같음) — `B/importing-sdk`
- 좌표: `ai.pleos.playground:TextToSpeech:3.3.1` — `B/importing-sdk`
- 2026-10-19 SDK 릴리즈 노트 기준 최신 버전도 v3.3.1 — https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015
- `libs.versions.toml`에는 아직 없음. 추가 필요.

## 2. 매니페스트 권한

```xml
<uses-permission android:name="pleos.car.permission.TTS_SERVICE" />
```
- 초기화, 리스너 등록, 앱 등록, speak 등 모든 API에 필요. 빠지면 `PERMISSION_DENIED` — `B/declaring-permissions`
- 모든 메서드에 `@RequiresPermission("pleos.car.permission.TTS_SERVICE")`가 붙어 있음. 예외로 `setLocale`·`setSpeakerType`은 어노테이션이 없지만 KDoc에는 "Requires TTS_PERMISSION"이라고 적혀 있음 — `R/entry`, `R/set-locale`
- protection level: **문서에 없음** (Vehicle·Navi 권한은 normal로 명시돼 있지만 TTS 권한은 표기가 없음)

## 3. 모드와 앱 등록(자격 증명)

| Mode | 동작 | 앱 등록 |
|---|---|---|
| `HYBRID` (기본값) | 서버 스트리밍을 우선 쓰고, 네트워크가 실패하면 온디바이스로 자동 전환 | 서버 합성을 쓰려면 필요 |
| `SERVER` | 서버만 사용. 네트워크가 실패하면 onError | 필요 |
| `ON_DEVICE` | 차량 온디바이스 엔진 사용 | **불필요** |

출처: `B/intro`, `B/app-registration`, `B/text-to-speech/ai.pleos.playground.tts.constant/mode/entry`

- `registerApp(clientID, clientSecret, listener)`에 넣는 값은 Pleos Playground → Workspace → **My Project** → 프로젝트 정보 → **SDK > 클라이언트 ID / 클라이언트 시크릿**. 이 값은 "TextToSpeech, SpeechToText처럼 서버 연결이 필요한 SDK용"이라고 안내돼 있음 — https://document.pleos.ai/docs/pleos-only/create-my-project
- 같은 페이지의 SDK 사용 신청 절에는 "FusedLocation, TextToSpeech, SpeechToText, Vehicle Control SDK = **준비 중(Coming Soon)**, 신청 불가"라고 적혀 있음. 문서끼리 상충하므로 실제 콘솔에서 확인해야 함 — 같은 URL
- 자격 증명을 넣는 위치(meta-data, BuildConfig 등)는 **문서에 없음**. 런타임 인자로만 넘김. 소스에 하드코딩하지 말고 `local.properties`에서 BuildConfig로 주입하는 방식을 권장(추정·권장).
- 등록 결과는 `OnServerConnectionListener.onConnected()` / `onFailed(ErrorType)`로 옴. 서버 모드에서는 **onConnected 이후에 speak**해야 함 — `B/app-registration`, `B/start-and-stop`

**결론: `Mode.ON_DEVICE`로 쓰면 자격 증명 없이 바로 구현할 수 있음.** HYBRID로 품질을 올리려면 My Project 자격 증명이 필요함.

## 4. API 시그니처 (`ai.pleos.playground.tts`)

```kotlin
class TextToSpeech(context: Context, mode: Mode = Mode.HYBRID)        // R/entry
fun initialize()                                                     // R/initialize
fun initialize(listener: OnInitializeListener)
fun registerApp(clientID: String, clientSecret: String, listener: OnServerConnectionListener) // R/register-app
fun addEventListener(listener: EventListener)                        // R/add-event-listener
fun removeEventListener(listener: EventListener)
fun setLocale(locale: Locale)                                        // R/set-locale
fun setSpeakerType(type: String)                                     // R/set-speaker-type
fun setAudioAttributes(audioAttributes: AudioAttributes)             // R/set-audio-attributes
fun setAudioZoneId(zoneId: Int)                                      // R/set-audio-zone-id
fun speak(text: String, isFinal: Boolean = true)                     // R/speak
fun stop()                                                           // R/stop
fun release()                                                        // R/release
companion object { val version: String }
```

리스너 (`ai.pleos.playground.tts.listener`) — `B/text-to-speech/ai.pleos.playground.tts.listener/...`
```kotlin
interface OnInitializeListener { fun onSuccess(); fun onFailure(errorType: ErrorType) }
interface OnServerConnectionListener { fun onConnected(); fun onFailed(errorType: ErrorType) }
interface EventListener {
    fun onReady()            // speak 요청을 받음
    fun onStart()            // 재생 시작
    fun onUpdatedRms(rms: Double)
    fun onDone()             // 정상 완료
    fun onStop()             // stop()으로 중지됨
    fun onError(errorType: ErrorType)
}
```
- EventListener 메서드는 모두 abstract라서 전부 구현해야 함 — `R/../ai.pleos.playground.tts.listener/event-listener/entry`
- enum은 `ai.pleos.playground.tts.constant.Mode`, `ai.pleos.playground.tts.constant.ErrorType`

### 화자(speaker)
- 한국어 화자: `seoyoon`, `eunwoo`, `haeun`, `sanghyeok`. 호출하지 않으면 앱에 저장된 값 → Gleo AI 설정 → `haeun` 순으로 적용 — `R/set-speaker-type`

### Locale
- 지원: `ko-KR`, `en-US`, `fr-CA`, `es-US`, `es-MX` — `B/set-locale`
- `setLocale(Locale.KOREA)`를 speak 전에 호출. 호출하지 않으면 앱에서 이전에 설정한 값을 쓰고, 그것도 없으면 시스템 locale을 씀 — `B/set-locale`

## 5. 에러 코드 (`ErrorType`) — `B/error-code`
`AUDIO_ERROR`, `INVALID_REQUEST_ERROR`, `MIC_ERROR`, `NETWORK_ERROR`, `NOT_SUPPORTED_API`, `ON_DEVICE_ENGINE_ERROR`, `PERMISSION_DENIED`, `SERVER_INTERNAL_ERROR`, `TIMEOUT`, `UNAUTHENTICATED`, `UNAVAILABLE_LANGUAGE`, `UNKNOWN`
- 새 값이 추가될 수 있으니 `else` 분기를 꼭 둘 것 — `B/error-code`

## 6. 스레딩과 수명주기
- 콜백이 어느 스레드에서 오는지는 **문서에 없음**. 바인더 스레드일 수 있다고 보고 `Dispatchers.Main`이나 StateFlow로 넘겨 처리할 것(추정).
- 객체는 컴포넌트 수명주기에 맞춰 재사용. 종료 시 `removeEventListener` → `release()` 순서. release 후에는 새 객체를 만들고 다시 initialize해야 함 — `B/initialization-and-release`
- 관련 동시성 정책(speak 중 다시 speak하면 큐에 쌓이는지 끊기는지)은 **문서에 없음**.

## 7. `SpeechOutput.speak(text)` 어댑터 최소 순서 (문서 순서를 그대로 옮김)
1. `TextToSpeech(appContext, Mode.ON_DEVICE)` (자격 증명을 확보하면 HYBRID + registerApp)
2. `initialize(OnInitializeListener)` → onSuccess가 오면 `isAvailable() = true`
3. `addEventListener(listener)`, `setLocale(Locale.KOREA)`
4. `speak(text)` 후 `onDone`에서 `Result.success`, `onError`에서 `Result.failure`, `onStop`에서는 취소 처리. 코루틴은 `suspendCancellableCoroutine`으로 감싸고 취소되면 `stop()`(설계 추정)
5. 앱이 종료되면 `removeEventListener` → `release()`

## 8. 관련 가이드
- 음성 안내 앞에 150–200ms 무음(pre-roll)을 두라고 권장 — https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide. TTS SDK가 무음을 넣어 주는지는 문서에 없음.
- 오디오 포커스: TTS SDK가 포커스를 직접 잡는지는 **문서에 없음**. 오디오 포커스 정책 문서 — https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development
- 호환성: "TextToSpeech SDK, SpeechToText SDK — 전체 호환" — https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability
