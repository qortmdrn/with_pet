# 위드펫(WithPet) MVP 개발 명세서

> 플레오스 커넥트(Pleos Connect) 차량용 반려동물 동승 케어 앱
> 작성일: 2026-10-22 · 원본 기획서: 위드펫(WithPet) 서비스 기획서

---

## 0. 이 문서를 읽는 Claude Code에게

- 이 문서는 MVP 구현 기준이다. 문서에 없는 기능은 만들지 않는다.
- **바로 코딩하지 말고**, 먼저 11장 작업 순서에 따라 구현 계획을 제시하고 사용자 승인을 받은 뒤 마일스톤 단위로 진행한다.
- 플레오스 SDK의 정확한 클래스명·메서드명은 이 문서에 확정되어 있지 않다. 추측해서 SDK를 직접 호출하지 말고, 5장의 **어댑터 인터페이스 뒤에 숨긴다.** SDK가 아직 설치되지 않았으면 Fake 구현으로 개발을 진행한다.
- SDK 문서가 프로젝트에 있으면(`docs/pleos-sdk/`) 그것을 우선 참고한다. 없으면 사용자에게 요청한다.
- 13장 "확인 필요" 항목에 부딪히면 임의로 결정하지 말고 사용자에게 묻는다.

---

## 1. 프로젝트 개요

| 항목 | 내용 |
| --- | --- |
| 앱 이름 | 위드펫 (WithPet), 가명 |
| 플랫폼 | 플레오스 커넥트 (Android Automotive OS 기반 차량 인포테인먼트) |
| 배포 | 플레오스 커넥트 앱 마켓 |
| 사용자 | 반려동물과 차로 이동하는 운전자와 동승자 |
| 핵심 가치 | 응급 시 진료 중인 동물병원 연결, 산책·배변 장소 안내, 멀미 완화·심리 안정 |

---

## 2. MVP 범위

### 포함 (MVP)

| ID | 기능 | 요약 |
| --- | --- | --- |
| F1 | 응급 모드 | 진료 중 동물병원 최대 3곳 표시 → 선택 시 전화 연결 + 길 안내 동시 시작 |
| F2 | 산책 스팟 | 현재 위치 주변 반려견 놀이터·공원·산책로를 거리순 표시, 경유지/목적지 설정 |
| F3 | 휴식 알림 | 연속 주행 시간 기준으로 경로상 쉼터 제안, 승인 시 경유지 추가 |
| F4 | 안정 사운드 | 상황별 플레이리스트 재생 (출발·장거리·병원 가는 길·귀가) |
| F5 | 펫 모드 | 원터치로 공조 온도·풍량, 뒷창문 소폭 개방, 볼륨 하향, 안정 사운드 재생 |
| F6 | 안전 탑승 체크 | 펫 모드 시작 시 이동장·안전벨트 체크 안내 |
| F7 | 반려동물 프로필 | 이름, 종, 나이, 체중, 사진 (로컬 저장) |

### 제외 (MVP 이후)

광고, 도착 예고 전송, 펫 진료 카드 전송, 주행 코칭, 멀미 일지, 모바일 앱 동기화, 음성 펫 비서(LLM), 차내 방치 경고, 커머스·구독, 영상 재생, 병원 파트너 콘솔, 실서버 연동.

> MVP의 병원·장소 데이터는 **로컬 목업 JSON**으로 시작한다. 서버 연동은 데이터 소스 인터페이스 뒤에서 나중에 교체한다.

---

## 3. 기술 스택과 전제

| 영역 | 선택 | 비고 |
| --- | --- | --- |
| 언어 | Kotlin | |
| UI | Jetpack Compose | 차량 화면 대응. 플레오스 디자인 가이드 확인 후 조정 |
| 아키텍처 | MVVM + 단방향 데이터 흐름, Clean Architecture 3계층 | 아래 4장 |
| DI | Hilt | |
| 비동기 | Kotlin Coroutines + Flow | |
| 로컬 저장 | DataStore(설정), Room(프로필) | |
| 미디어 | AndroidX Media3 (ExoPlayer + MediaSession) | 차량 미디어 시스템과 연동되도록 MediaSession 사용 |
| 주행 제한 | Android Automotive `CarUxRestrictionsManager` | 주행 중 UI 제한 상태 구독 |
| 차량 기능 | Pleos Connect SDK (Vehicle, NaviHelper, Fused Location, Gleo AI) | 어댑터로 감쌈 |
| 테스트 | JUnit5, MockK, Turbine, Compose UI Test | |
| 빌드 | Gradle (Kotlin DSL), 버전 카탈로그 | |
| 개발 환경 | Android Studio + Pleos Connect Emulator (AVD) | 사용자가 직접 설치 |

**플레오스 Connect SDK 모듈 (공개 문서 기준)**

- Vehicle SDK: 차량 상태 조회·제어. HVAC, Window, Seat, Door, Brake, DrivingMode, Safety 등 API 그룹 제공
- NaviHelper SDK: 내장 내비게이션 제어 (목적지 설정, 경유지 추가·삭제, 경로 정보 조회)
- Gleo AI SDK: 음성 인식(STT), 음성 합성(TTS), LLM
- Fused Location SDK: 차량 위치
- Sync SDK: 모바일과 데이터 동기화 (MVP 제외)

---

## 4. 아키텍처

### 4-1. 계층

```
ui (Compose 화면, ViewModel)
  ↓
domain (UseCase, 도메인 모델, Repository 인터페이스)
  ↓
data (Repository 구현, 목업 데이터 소스)
platform (Pleos SDK·Android 시스템 어댑터: 인터페이스 + Real/Fake 구현)
```

- `ui`는 `domain`만 의존한다.
- `domain`은 Android·Pleos SDK에 의존하지 않는다 (순수 Kotlin).
- `platform`의 Real 구현만 Pleos SDK를 import한다. SDK 교체·부재 시 Fake로 대체 가능해야 한다.
- 빌드 변형: `fake` flavor (SDK 없이 실행, 기본값), `pleos` flavor (실제 SDK 연결).

### 4-2. 폴더 구조

```
withpet/
├── app/
│   └── src/
│       ├── main/java/com/withpet/car/
│       │   ├── WithPetApp.kt
│       │   ├── MainActivity.kt
│       │   ├── di/                      # Hilt 모듈
│       │   ├── ui/
│       │   │   ├── theme/
│       │   │   ├── components/          # EmergencyButton, BigActionButton, DrivingAwareList 등
│       │   │   ├── home/
│       │   │   ├── emergency/
│       │   │   ├── walk/
│       │   │   ├── calm/
│       │   │   ├── profile/
│       │   │   └── settings/
│       │   ├── domain/
│       │   │   ├── model/
│       │   │   ├── repository/
│       │   │   └── usecase/
│       │   ├── data/
│       │   │   ├── repository/
│       │   │   ├── local/               # Room, DataStore
│       │   │   └── mock/                # 목업 JSON 로더
│       │   ├── platform/
│       │   │   ├── vehicle/             # VehicleControl (+ Fake, Pleos)
│       │   │   ├── navigation/          # NavigationController
│       │   │   ├── location/            # LocationProvider
│       │   │   ├── phone/               # PhoneDialer
│       │   │   ├── driving/             # DrivingStateProvider
│       │   │   └── voice/               # VoiceCommandSource
│       │   └── media/                   # CalmPlaybackService (MediaSessionService)
│       ├── main/assets/mock/            # hospitals.json, walk_spots.json, playlists.json
│       ├── main/res/raw/                # 안정 사운드 샘플 음원 (저작권 확보된 것만)
│       ├── fake/                        # Fake 어댑터 바인딩
│       └── pleos/                       # Pleos SDK 어댑터 바인딩
├── docs/
│   ├── withpet-dev-spec.md              # 이 문서
│   └── pleos-sdk/                       # (사용자가 넣을) SDK 문서 사본
└── gradle/libs.versions.toml
```

---

## 5. 플랫폼 어댑터 인터페이스

아래 시그니처를 기준으로 만든다. Real(Pleos) 구현은 SDK 문서를 확인한 뒤 작성하고, 지원하지 않는 기능은 `Result.failure(UnsupportedFeature)`를 반환해 UI가 안내형으로 대체하게 한다.

```kotlin
// platform/vehicle
interface VehicleControl {
    suspend fun setCabinTemperature(celsius: Float): Result<Unit>
    suspend fun setFanLevel(level: Int): Result<Unit>
    suspend fun setRearWindowVentilation(openPercent: Int): Result<Unit>  // 0~100
    fun cabinTemperature(): Flow<Float?>
}

// platform/navigation
interface NavigationController {
    suspend fun startGuidance(destination: GeoPoint, name: String): Result<Unit>
    suspend fun addWaypoint(point: GeoPoint, name: String): Result<Unit>
    suspend fun estimateEtaMinutes(destination: GeoPoint): Result<Int>
    fun activeRoute(): Flow<RouteInfo?>   // 경로 없으면 null
}

// platform/location
interface LocationProvider {
    fun currentLocation(): Flow<GeoPoint>
}

// platform/phone
interface PhoneDialer {
    fun isCallCapable(): Flow<Boolean>      // 휴대폰 블루투스 연결 여부
    suspend fun call(phoneNumber: String): Result<Unit>
}

// platform/driving
interface DrivingStateProvider {
    fun state(): Flow<DrivingState>          // PARKED, DRIVING
    fun continuousDrivingMinutes(): Flow<Int>
}

// platform/voice  (MVP: 정해진 명령어만. LLM 사용 안 함)
interface VoiceCommandSource {
    fun commands(): Flow<VoiceCommand>
}

enum class DrivingState { PARKED, DRIVING }

sealed interface VoiceCommand {
    data object Emergency : VoiceCommand
    data object PetModeOn : VoiceCommand
    data object PetModeOff : VoiceCommand
    data object Confirm : VoiceCommand
    data object Reject : VoiceCommand
}

class UnsupportedFeature(message: String) : Exception(message)
```

**Fake 구현 요구사항**

- `FakeLocationProvider`: 서울 시청(37.5663, 126.9779) 고정, 디버그 메뉴에서 위치 변경 가능
- `FakeDrivingStateProvider`: 디버그 메뉴에서 PARKED/DRIVING 전환, 연속 주행 시간 임의 설정
- `FakePhoneDialer`: 호출 시 로그 출력 + 토스트, 연결 여부 토글 가능
- `FakeNavigationController`: 목적지·경유지를 메모리에 저장, ETA는 직선거리 ÷ 40km/h로 계산
- `FakeVehicleControl`: 설정값을 메모리에 저장하고 Flow로 노출

---

## 6. 도메인 모델

```kotlin
data class GeoPoint(val lat: Double, val lng: Double)

data class Hospital(
    val id: String,
    val name: String,
    val location: GeoPoint,
    val address: String,
    val phone: String,
    val is24Hours: Boolean,
    val weeklyHours: List<OpeningHours>,   // 비어 있으면 진료 시간 미확인
    val isPartner: Boolean,
)

data class OpeningHours(val dayOfWeek: DayOfWeek, val open: LocalTime, val close: LocalTime)

enum class OpenStatus { OPEN, CLOSED, UNKNOWN }

data class HospitalCandidate(
    val hospital: Hospital,
    val status: OpenStatus,
    val etaMinutes: Int,
)

data class WalkSpot(
    val id: String,
    val name: String,
    val type: WalkSpotType,
    val location: GeoPoint,
    val leashRequired: Boolean,
    val hasWasteBin: Boolean,
    val hasWater: Boolean,
    val largeDogAllowed: Boolean,
)

enum class WalkSpotType { DOG_PARK, PARK, TRAIL, OPEN_FIELD, REST_AREA }

data class PetProfile(
    val id: Long,
    val name: String,
    val species: Species,
    val breed: String?,
    val birthYear: Int?,
    val weightKg: Float?,
    val photoUri: String?,
)

enum class Species { DOG, CAT, OTHER }

data class PetModeSettings(
    val targetTempC: Float = 22f,        // 기본값, 사용자 설정 가능 (수의 자문 후 확정)
    val fanLevel: Int = 2,
    val rearWindowOpenPercent: Int = 10,
    val mediaVolumePercent: Int = 30,
    val autoPlayCalmSound: Boolean = true,
)

data class RestAlertSettings(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 90,       // 기본값, 설정 가능
)

enum class CalmSituation { DEPARTURE, LONG_DRIVE, TO_HOSPITAL, RETURN_HOME }
```

---

## 7. 화면별 명세

### 공통 UI 규칙

- 모든 화면 상단에 **응급 버튼 고정** (빨간색, 최소 터치 영역 76dp 이상).
- `DrivingState.DRIVING`일 때:
  - 화면당 주요 버튼 3개 이하, 텍스트는 한 줄 이하
  - 스크롤 목록은 최대 3개 항목만 표시하고 "정차 후 더 보기" 표시
  - 텍스트 입력 화면(프로필·설정)은 진입 불가, "정차 후 이용해 주세요" 안내
- 실제 주행 제한은 `CarUxRestrictionsManager`의 상태를 따른다. 위 규칙은 그보다 엄격한 앱 자체 기준이다.
- 다크 테마 기본, 고대비. 플레오스 디자인 가이드가 있으면 그것을 우선한다.

### 7-1. 홈 (`ui/home`)

| 요소 | 동작 |
| --- | --- |
| 반려동물 카드 | 대표 프로필 사진·이름. 없으면 "반려동물 등록" (정차 시만) |
| 펫 모드 토글 | ON 시 F5 실행 + F6 체크 안내 다이얼로그 |
| 바로가기 3개 | 산책 스팟 / 안정 사운드 / 응급 |
| 진행 중 상태 | 펫 모드 ON이면 "실내 22℃ · 안정 사운드 재생 중" 한 줄 |

### 7-2. 응급 모드 (`ui/emergency`) — F1

**진입:** 응급 버튼 터치 또는 `VoiceCommand.Emergency`

**동작**

1. 현재 위치 기준 반경 20km 이내 병원을 조회한다.
2. 각 병원의 `OpenStatus`를 현재 시각으로 계산한다 (`is24Hours`면 OPEN, `weeklyHours` 비면 UNKNOWN).
3. ETA를 계산한다 (`NavigationController.estimateEtaMinutes`, 실패 시 직선거리 기반 추정).
4. 정렬: OPEN 먼저 → UNKNOWN → (CLOSED 제외). 같은 그룹 안에서는 ETA 오름차순. 최대 3곳.
5. 각 항목: 병원명, ETA(분), 24시 배지, 진료 상태 ("진료 중" / "진료 시간 미확인 · 전화 확인 필요").
6. 항목 선택 시 **동시에**: `PhoneDialer.call()` + `NavigationController.startGuidance()`.
7. 휴대폰 미연결 시: 전화 대신 병원 번호를 크게 표시하고 "휴대폰을 연결하면 바로 전화할 수 있어요" 안내. 길 안내는 그대로 시작.
8. 결과가 0곳이면: 반경을 50km로 넓혀 재검색. 그래도 없으면 "근처에 진료 중인 병원을 찾지 못했어요"와 가장 가까운 병원(진료 상태 무관) 1곳 표시.

**금지 사항**

- 응급 모드 화면에는 광고·추천·팝업을 일절 표시하지 않는다.
- "진단", "처방", "진료해 드려요" 등 원격 진료로 오해될 문구를 쓰지 않는다. 버튼 문구는 "병원에 전화하고 길 안내".

**수용 기준**

- 응급 버튼 터치 후 목록 표시까지 2초 이내 (목업 데이터 기준)
- 병원 선택부터 전화·길 안내 시작까지 터치 1회
- 주행 중에도 이 화면은 전체 기능 사용 가능

### 7-3. 산책 스팟 (`ui/walk`) — F2

| 요소 | 동작 |
| --- | --- |
| 필터 칩 | 전체 / 반려견 놀이터 / 공원 / 산책로 / 대형견 가능 (주행 중 숨김) |
| 목록 | 거리순. 아이콘으로 목줄 필수·배변봉투함·음수대 표시 |
| 항목 액션 | "경유지로 추가" (경로 있을 때) / "길 안내" (경로 없을 때) |

수용 기준: 주행 중에는 가장 가까운 3곳만, 필터 없이 표시.

### 7-4. 휴식 알림 — F3 (화면이 아닌 백그라운드 로직 + 오버레이 카드)

1. `continuousDrivingMinutes() >= intervalMinutes`이고 펫 모드가 ON이면 트리거.
2. 활성 경로가 있으면 경로 진행 방향 앞쪽 15km 이내 `WalkSpot` 중 가장 가까운 1곳, 없으면 현재 위치 반경 5km 내 1곳을 고른다.
3. 오버레이 카드: "OO분 주행했어요. 10분 뒤 [장소명]에서 쉬어 갈까요?" + [추가] [나중에]
4. TTS 사용 가능하면 같은 문장을 읽고 `Confirm`/`Reject` 음성 응답을 받는다.
5. [추가] → `addWaypoint()`. [나중에] → 30분 뒤 재알림. 한 주행에서 거절 2회 시 그 주행에는 더 이상 알리지 않는다.
6. 정차(PARKED) 10분 이상이면 연속 주행 시간 초기화.

### 7-5. 안정 사운드 (`ui/calm`, `media/`) — F4

- `CalmSituation`별 플레이리스트 (assets/mock/playlists.json).
- 재생은 `MediaSessionService`에서 수행해 앱 화면을 벗어나도 계속 재생되고 차량 미디어 컨트롤과 연동된다.
- 주행 중 화면: 상황 선택 버튼 4개 + 재생/정지만.
- 영상은 MVP에서 제외.
- 샘플 음원은 저작권이 확보된 파일만 사용. 없으면 무음 placeholder 파일과 TODO 주석으로 둔다.

### 7-6. 펫 모드 — F5, F6 (`domain/usecase/StartPetModeUseCase`)

ON 시 순서대로 실행하고, 각 단계 실패는 무시하되 결과를 모아 한 줄로 보고한다.

1. `setCabinTemperature(targetTempC)`
2. `setFanLevel(fanLevel)`
3. `setRearWindowVentilation(rearWindowOpenPercent)`
4. 미디어 볼륨을 `mediaVolumePercent`로 (AudioManager 또는 차량 API, 확인 필요)
5. `autoPlayCalmSound`면 `DEPARTURE` 플레이리스트 재생
6. 안전 탑승 체크 다이얼로그: "반려동물이 이동장 또는 전용 안전벨트에 있나요? 운전석에 안고 운전하는 것은 도로교통법 위반이에요." + [확인]

OFF 시: 창문 원위치(진입 전 값 저장해 두었다가 복원), 안정 사운드 정지. 공조는 변경하지 않는다.

지원하지 않는 차량 기능은 "이 차량에서는 창문 자동 조절을 지원하지 않아요"처럼 안내.

### 7-7. 프로필·설정 (`ui/profile`, `ui/settings`) — F7

- 정차 중에만 진입 가능.
- 프로필: 여러 마리 등록 가능, 대표 1마리 지정.
- 설정: 펫 모드 기본값(온도·풍량·창문·볼륨·자동 재생), 휴식 알림(ON/OFF, 간격 60/90/120분).
- 디버그 메뉴 (`fake` flavor에서만): 위치, 주행 상태, 연속 주행 시간, 휴대폰 연결 여부 조작.

---

## 8. 음성 명령 (MVP)

| 발화 예시 | 명령 | 동작 |
| --- | --- | --- |
| "강아지 응급이야", "동물병원 찾아줘" | Emergency | 응급 모드 진입 |
| "펫 모드 켜줘" | PetModeOn | 펫 모드 ON |
| "펫 모드 꺼줘" | PetModeOff | 펫 모드 OFF |
| "응", "추가해" | Confirm | 휴식 알림 수락 |
| "아니", "나중에" | Reject | 휴식 알림 거절 |

플레오스의 Gleo AI(STT) 연동 방식과 시스템 음성 비서와의 관계(앱이 직접 듣는지, Gleo가 앱으로 넘겨주는지)는 확인 필요. MVP에서는 `VoiceCommandSource` Fake로 버튼 트리거를 대신 제공한다.

---

## 9. 비기능 요구사항

| 항목 | 기준 |
| --- | --- |
| 안전 | 7장 공통 UI 규칙 준수, 응급 모드 광고 금지, 원격 진료 오해 문구 금지 |
| 성능 | 앱 콜드 스타트 2초 이내, 응급 목록 2초 이내 (목업 기준) |
| 오프라인 | 목업 데이터는 로컬이므로 네트워크 없이 전 기능 동작 |
| 개인정보 | 위치는 기기 안에서만 사용, 외부 전송 없음 (MVP). 위치 권한 요청 시 사용 목적 표시 |
| 접근성 | 텍스트 최소 24sp (주행 중 화면), 색만으로 상태 구분 금지 (배지 텍스트 병기) |
| 로그 | 개인정보·위치 좌표를 로그에 남기지 않음 |

---

## 10. 목업 데이터

`app/src/main/assets/mock/`에 생성한다. **실제 병원명·전화번호를 쓰지 않는다.** 이름은 "테스트 동물병원 A", 번호는 `010-0000-000X` 형식의 가짜 값.

- `hospitals.json`: 서울 시청 반경 15km 안에 12곳. 24시 3곳, 진료 시간 미확인 2곳, 야간 마감 7곳. 제휴(`isPartner`) 4곳.
- `walk_spots.json`: 같은 반경에 15곳. 유형별로 고르게, 대형견 가능 5곳.
- `playlists.json`: 상황별 2~3곡, 음원은 `res/raw` 파일명 참조.

---

## 11. 작업 순서 (마일스톤)

각 마일스톤이 끝나면 빌드·테스트를 통과시키고 사용자에게 결과를 보고한 뒤 다음으로 넘어간다.

| 순서 | 마일스톤 | 완료 기준 |
| --- | --- | --- |
| M0 | 프로젝트 골격 | Gradle 설정, `fake`/`pleos` flavor, Hilt, Compose 테마, 빈 화면 내비게이션, 빌드 성공 |
| M1 | 도메인·목업 데이터 | 6장 모델, Repository 인터페이스·목업 구현, JSON 로더, 단위 테스트 |
| M2 | 플랫폼 어댑터 Fake | 5장 인터페이스 + Fake 구현 + 디버그 메뉴 |
| M3 | 응급 모드 | 7-2 전체, 정렬·필터 로직 단위 테스트, UI 테스트 |
| M4 | 펫 모드·안전 체크 | 7-6, 7-1 홈 |
| M5 | 안정 사운드 | 7-5, MediaSessionService |
| M6 | 산책 스팟·휴식 알림 | 7-3, 7-4 |
| M7 | 프로필·설정 | 7-7, Room·DataStore |
| M8 | 주행 제한 적용 | `CarUxRestrictionsManager` 연동, 7장 공통 규칙 전 화면 점검 |
| M9 | Pleos 어댑터 | SDK 문서를 확인하고 `pleos` flavor 어댑터 구현, 에뮬레이터 검증 (SDK·문서 준비 후) |

---

## 12. 테스트

- **단위 테스트 필수:** 진료 상태 계산(요일·자정 넘김·24시), 응급 정렬, 휴식 알림 트리거 조건, 펫 모드 실행·복원 로직
- **UI 테스트:** 응급 버튼 → 목록 → 선택 시 Fake 다이얼러·내비 호출 확인, 주행 중 목록 3개 제한 확인
- **경계 케이스:** 위치 미수신, 병원 0곳, 휴대폰 미연결, 차량 기능 미지원, 자정 직전·직후 영업 상태

---

## 13. 확인 필요 (사람이 결정하거나 자료를 줘야 하는 것)

- [ ] 플레오스 플레이그라운드 개발자 등록, SDK·에뮬레이터 설치, SDK 문서 사본을 `docs/pleos-sdk/`에 저장
- [ ] Vehicle SDK로 서드파티 앱이 공조·창문을 **제어**할 수 있는 권한 범위
- [ ] 앱에서 블루투스 연결 휴대폰으로 바로 발신하는 방식 (시스템 다이얼러 인텐트 사용 여부)
- [ ] 미디어 볼륨 제어 방식 (Android AudioManager vs 차량 API)
- [ ] Gleo AI 음성 명령을 서드파티 앱이 받는 방식
- [ ] 앱 마켓 심사 기준과 플레오스 UI 디자인 가이드 (주행 중 UI 제한 포함)
- [ ] 펫 모드 기본 온도 등 수치에 대한 수의사 자문
- [ ] 패키지명 확정 (현재 `com.withpet.car`는 임시)
- [ ] 안정 사운드 음원 확보 (자체 제작 또는 라이선스)

---

## 14. Claude Code 첫 요청 예시

```
docs/withpet-dev-spec.md를 끝까지 읽어줘.
아직 코드는 쓰지 말고, M0~M2 구현 계획(만들 파일 목록, 의존성, 순서)을 먼저 보여줘.
13장에서 지금 결정이 필요한 항목이 있으면 같이 물어봐.
```
