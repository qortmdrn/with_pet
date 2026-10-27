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
| 핵심 가치 | 응급 시 진료 중인 동물병원 연결, 산책·배변 장소 안내, 편안한 차량 내 환경(공조·창문·청각) 제공 |

### 타깃 선정 이유: "꼭 깔아야 하는 앱"

플레오스 앱 마켓에서 사용자를 모으려면 특정 타깃층이 "이 차를 탄다면 이 앱은 꼭 깔아야 한다"고 느낄 이유가 있어야 한다. 그래서 위드펫은 반려동물을 키우는 가구와 그 운전자를 타깃으로 한다.

- **범용 앱은 설치 동기가 약하다.** 현재 앱 마켓 입점 앱은 내비게이션, 음악·오디오 중심이고, 사용자가 이미 스마트폰으로 쓰는 서비스다.
- **반려동물 동반 운전에는 차 안에서만 생기는 문제가 있다.** 이동 중 응급, 장거리 이동 중 휴식, 쉬어 갈 장소 찾기는 운전 중 스마트폰으로 해결하기 어렵다.
- **타깃 규모가 크다.** 반려가구 591만 가구(전체의 26.7%), 반려견 가구 455만 가구 (KB경영연구소, 2024년 말 기준).
- **반복 사용된다.** 병원, 산책, 여행 등 반려동물과의 이동은 주기적으로 반복된다.

### 개발 우선순위에 주는 의미

- "꼭 깔아야 하는 이유"는 **차 안에서만 해결되는 기능**에서 나온다. 응급 모드(F1)와 펫 모드(F5)의 완성도를 가장 높게 둔다. 일정이 밀리면 산책 스팟 필터 같은 부가 요소부터 줄인다.
- 첫 실행에서 가치를 바로 느껴야 한다. 반려동물 등록 없이도 응급 모드·펫 모드·안정 사운드를 바로 쓸 수 있게 하고, 프로필 등록은 나중에 권유한다.
- 반복 사용을 고려해 펫 모드 설정값과 마지막 안정 사운드 상황을 기억해 다음 실행 때 그대로 적용한다.

---

## 2. MVP 범위

### 포함 (MVP)

| ID | 기능 | 요약 |
| --- | --- | --- |
| F1 | 응급 모드 | 진료 중 병원이 있으면 응급 버튼 한 번으로 가장 빨리 도착하는 곳에 전화 + 길 안내 바로 시작. 없으면 최대 3곳 표시 → 선택 시 동시 시작 |
| F2 | 산책 스팟 | 현재 위치 주변 반려견 놀이터·공원·산책로를 거리순 표시, 경유지/목적지 설정 |
| F3 | 휴식 알림 | 연속 주행 시간 기준으로 경로상 쉼터 제안, 승인 시 경유지 추가 |
| F4 | 안정 사운드 | 상황별 플레이리스트 재생 (출발·장거리·병원 가는 길·귀가) |
| F5 | 펫 모드 | 원터치로 공조 온도·풍량, 안정 사운드 볼륨 조절·재생. 뒷창문 소폭 개방은 사용자가 켠 경우에만 (선택 기능) |
| F6 | 안전 탑승 체크 | 펫 모드 시작 시 이동장·안전벨트 체크 안내 |
| F7 | 반려동물 프로필 | 이름, 종, 나이, 체중, 사진 (로컬 저장) |
| F8 | 반려동물 동반 장소 | 공공데이터 기반 동반 가능 카페·식당 / 여행지 / 문화시설을 영업 상태·거리순 표시, 터치 1회로 길 안내/경유지 추가. 목록 밖은 내장 내비 검색으로 연결 |

### 제외 (MVP 이후)

광고, 도착 예고 전송, 펫 진료 카드 전송, 주행 코칭, 멀미 일지, 모바일 앱 동기화, 음성 펫 비서(LLM), 차내 방치 경고, 커머스·구독, 영상 재생, 병원 파트너 콘솔, 실서버 연동.

> 병원·동반 장소·산책 스팟 데이터는 공공데이터 CSV를 변환한 **로컬 JSON**(`assets/data`)을 쓴다. 플레이리스트만 목업 JSON. 서버 연동은 데이터 소스 인터페이스 뒤에서 나중에 교체한다.

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
| 차량 기능 | Pleos Connect SDK (Vehicle, NaviHelper, FusedLocation, TextToSpeech, SpeechToText) + Gleo Interface | 어댑터로 감쌈. 정리본은 `docs/pleos-sdk/` |
| 테스트 | JUnit5, MockK, Turbine, Compose UI Test | |
| 빌드 | Gradle (Kotlin DSL), 버전 카탈로그 | |
| 개발 환경 | Android Studio + Pleos Connect Emulator (AVD) | 사용자가 직접 설치 |

**플레오스 Connect SDK 모듈 (공개 문서 기준)**

- Vehicle SDK: 차량 상태 조회·제어. HVAC, Window, Seat, Door, Brake, DrivingMode, Safety 등 API 그룹 제공
- NaviHelper SDK: 내장 내비게이션 제어 (목적지 설정, 경유지 추가·삭제, 경로 정보 조회)
- TextToSpeech·SpeechToText SDK (ON_DEVICE 모드). 음성 명령은 Gleo Interface(App Schema + Intent, Pleos 승인 필요). LLM SDK는 종료됨
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
│       ├── main/assets/mock/            # playlists.json (목업)
│       ├── main/assets/data/            # hospitals.json, pet_places.json, walk_spots.json (공공데이터 변환본, scripts/build_public_data.py)
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
    suspend fun searchPlace(keyword: String): Result<Unit>   // 내장 내비 장소 검색 화면 열기
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
    suspend fun listenForAnswer() {}   // 앱이 질문을 읽은 뒤 짧은 답을 한 번 듣는다 (Pleos STT는 앱이 시작해야 들음)
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
| 바로가기 | 산책 스팟 / 동반 장소 / 안정 사운드 (주행 중에도 표시). 정차 시 응급 / 설정 추가. 동반 장소를 주행 중에도 열기 위해 펫 모드 토글 포함 주요 버튼 4개로, 7장 공통 규칙(3개 이하)의 예외로 정했다 |
| 진행 중 상태 | 펫 모드 ON이면 "실내 22℃ · 안정 사운드 재생 중" 한 줄 |

### 7-2. 응급 모드 (`ui/emergency`) — F1

**진입:** 응급 버튼 터치 또는 `VoiceCommand.Emergency`

**데이터:** 공공데이터 "반려동물 동반 가능 문화시설"의 동물병원 (중복 제거 후 약 4,500곳, `assets/data/hospitals.json`). 앱 시작 때 미리 읽는다(`DataPreloader`). 진료 시간은 `OpeningHoursParser`의 strict 모드로 해석한다: "N째주"·"마지막주" 휴진이 있으면 UNKNOWN. 공휴일 휴진은 `KoreanHolidays`(대체공휴일 포함, 현재 2026~2027년)로 반영한다: "법정공휴일"이면 모든 공휴일, "설"·"추석"·"1월1일"이면 해당 공휴일에 CLOSED. "공휴일 다음날" 휴진은 UNKNOWN. 24시는 7일 모두 00:00~24:00인 병원. 전화번호가 없는 병원(약 4%)은 길 안내만 하고 "등록된 전화번호가 없어요"를 표시한다.

**동작**

1. 현재 위치 기준 반경 20km 이내 병원을 조회한다.
2. 각 병원의 `OpenStatus`를 현재 시각으로 계산한다 (`is24Hours`면 OPEN, `weeklyHours` 비면 UNKNOWN).
3. ETA를 계산한다 (`NavigationController.estimateEtaMinutes`, 실패 시 직선거리 기반 추정). 도심은 반경 안 병원이 수백 곳이므로 상태별로 직선거리가 가까운 10곳만 계산한다.
4. 정렬: OPEN 먼저 → UNKNOWN → (CLOSED 제외). 같은 그룹 안에서는 ETA 오름차순. 최대 3곳.
5. 각 항목: 병원명, ETA(분), 24시 배지, 진료 상태 ("진료 중" / "진료 시간 미확인 · 전화 확인 필요").
6. 항목 선택 시 **동시에**: `PhoneDialer.call()` + `NavigationController.startGuidance()`. Pleos에서는 `call()`이 다이얼러에 번호만 채우므로(`ACTION_DIAL`) 결과 화면에 번호와 "다이얼러에서 통화 버튼을 눌러 주세요"를 표시한다.
7. 휴대폰 미연결 시: 전화 대신 병원 번호를 크게 표시하고 "휴대폰을 연결하면 바로 전화할 수 있어요" 안내. 길 안내는 그대로 시작.
8. 결과가 0곳이면: 반경을 50km로 넓혀 재검색. 그래도 없으면 "근처에 진료 중인 병원을 찾지 못했어요"와 가장 가까운 병원(진료 상태 무관) 1곳 표시.
9. **원터치 안내:** 정렬 1순위가 OPEN이면 목록을 거치지 않고 그 병원으로 6번을 바로 실행한다. 결과 화면에 "가장 빨리 도착하는 진료 중 병원"을 표시하고, "다른 병원 보기"로 목록에 돌아갈 수 있다. 1순위가 UNKNOWN이거나 대체 병원만 있으면 자동으로 고르지 않고 목록을 보여 준다.

**금지 사항**

- 응급 모드 화면에는 광고·추천·팝업을 일절 표시하지 않는다.
- "진단", "처방", "진료해 드려요" 등 원격 진료로 오해될 문구를 쓰지 않는다. 버튼 문구는 "병원에 전화하고 길 안내".

**수용 기준**

- 응급 버튼 터치 후 결과(또는 목록) 표시까지 2초 이내
- 진료 중 병원이 있으면 응급 버튼부터 전화·길 안내 시작까지 터치 1회, 목록에서 고를 때도 병원 선택 1회
- 주행 중에도 이 화면은 전체 기능 사용 가능

### 7-3. 산책 스팟 (`ui/walk`) — F2

| 요소 | 동작 |
| --- | --- |
| 데이터 | 공공데이터 여행지 중 산책할 만한 곳 약 560곳 (`assets/data/walk_spots.json`): 반려견 놀이터·공원·산책로·물가(해변·호수)·쉼터. 사찰·마을 같은 관광지는 제외(동반 장소 > 여행지에서 보임) |
| 필터 칩 | 전체 / 반려견 놀이터 / 공원 / 산책로 / 대형견 가능 (주행 중 숨김) |
| 목록 | 반경 20km, 지금 닫힌 곳 제외, 거리순 최대 20곳. 정차 시 목줄 필수·입장료·운영시간(상시 개방이 아니면)·크기 제한·제한사항·주소 표시. 음수대·배변봉투함 정보는 원본에 없어 표시하지 않는다 |
| 항목 액션 | "경유지로 추가" (경로 있을 때) / "길 안내" (경로 없을 때) |

수용 기준: 주행 중에는 가장 가까운 3곳만, 필터 없이 표시.

### 7-4. 휴식 알림 — F3 (화면이 아닌 백그라운드 로직 + 오버레이 카드)

1. `continuousDrivingMinutes() >= intervalMinutes`이고 펫 모드가 ON이면 트리거.
2. 활성 경로가 있으면 경로 진행 방향 앞쪽 15km 이내 `WalkSpot` 중 가장 가까운 1곳, 없으면 현재 위치 반경 5km 내 1곳을 고른다. 지금 닫혀 있는 곳은 제외한다.
3. 오버레이 카드: "OO분 주행했어요. 10분 뒤 [장소명]에서 쉬어 갈까요?" + [추가] [나중에]
4. TTS 사용 가능하면 같은 문장을 읽고 `Confirm`/`Reject` 음성 응답을 받는다.
5. [추가] → `addWaypoint()`. [나중에] → 30분 뒤 재알림. 한 주행에서 거절 2회 시 그 주행에는 더 이상 알리지 않는다.
6. 정차(PARKED) 10분 이상이면 연속 주행 시간 초기화.

### 7-5. 안정 사운드 (`ui/calm`, `media/`) — F4

- 목적: 반려동물을 위한 **편안한 차량 내 청각 환경 제공**. 멀미 치료·불안 해소 같은 효과는 주장하지 않는다.
  - 근거: 음악이 반려견의 멀미를 줄인다는 동료 심사 연구는 없다. 보호소·견사에서 클래식 음악이 휴식 행동 증가와 관련 있었다는 보고는 있으나(Wells 2002, Kogan 2012, Bowman 2015·2017), 동물병원 환경에서는 결과가 엇갈리고, 리뷰 논문(Lindig 2020)도 권고를 내리기엔 근거가 부족하다고 결론지었다. 자체 합성 음원에는 이런 연구 결과를 적용할 수 없다.
  - 화면·스토어 설명·음성 안내·Gleo 스키마에 "멀미 완화", "불안 해소", "스트레스 감소", "진정 효과", "과학적으로 입증" 같은 표현을 쓰지 않는다. "잔잔한", "부드러운", "편안한 분위기"처럼 소리의 성격만 설명한다.
- `CalmSituation`별 플레이리스트 (assets/mock/playlists.json).
- 재생은 `MediaSessionService`에서 수행해 앱 화면을 벗어나도 계속 재생되고 차량 미디어 컨트롤과 연동된다.
- 주행 중 화면: 상황 선택 버튼 4개 + 재생/정지만.
- 마지막으로 선택한 상황을 DataStore에 저장하고, 다음 실행 시 기본 선택으로 보여준다.
- 영상은 MVP에서 제외.
- 샘플 음원은 저작권이 확보된 파일만 사용. 지금은 `scripts/generate_calm_sounds.py`로 합성한 임시 음원(느린 템포·중저음·고음 감쇠, 곡당 60초 루프)을 쓴다. 유튜브 등에서 추출한 음원은 쓰지 않는다.

### 7-6. 펫 모드 — F5, F6 (`domain/usecase/StartPetModeUseCase`)

ON 시 순서대로 실행하고, 각 단계 실패는 무시하되 결과를 모아 한 줄로 보고한다.

1. `setCabinTemperature(targetTempC)`
2. `setFanLevel(fanLevel)`
3. `rearWindowEnabled`일 때만 `setRearWindowVentilation(rearWindowOpenPercent)`. 개방률은 `REAR_WINDOW_MAX_PERCENT`(10%)로 제한한다. 비가 오면(`isRaining() == true`) 열지 않고 "비가 와서 뒷창문은 열지 않았어요"로 알린다. 열었으면 뒷창문을 잠근다(`setRearWindowLock(true)`, 미지원 차량은 조용히 건너뜀)
4. 안정 사운드(앱 재생) 볼륨을 `mediaVolumePercent`로. 차량 전체 미디어 볼륨은 서드파티 앱이 바꿀 수 없어(SDK에 API 없음) 앱 재생 볼륨만 조절한다 (`AppPlaybackVolume`)
5. `autoPlayCalmSound`면 `DEPARTURE` 플레이리스트 재생
6. 안전 탑승 체크 다이얼로그: "반려동물이 이동장 또는 전용 안전벨트에 있나요? 운전석에 안고 운전하는 것은 도로교통법 위반이에요." + [확인]

OFF 시: 안정 사운드 정지. 공조와 창문은 변경하지 않는다.
- 펫 모드가 연 뒷창문은 **자동으로 닫지 않는다**. 반려동물이 창틈에 머리를 대고 있으면 끼일 수 있고, 끼임 방지(자동 반전) 지원 여부는 차량마다 다르다.
- 대신 "펫 모드를 껐어요 · 뒷창문은 열려 있어요"로 알리고, 정차 중 홈에 [뒷창문 닫기] 버튼을 보여준다. 누르면 진입 전 값(저장해 둔 값, 모르면 닫힘)으로 되돌린다. 잠금은 진입 전에 풀려 있던 게 확실할 때만 풀고, 모르면 잠근 채로 둔다. 주행 중에는 버튼을 숨기고 차량의 창문 스위치를 쓴다.

#### 뒷창문 환기 (선택 기능)

창문은 펫 모드에서 유일하게 물리적인 사고(반려동물이 몸을 내밂, 뛰어내림, 끼임)로 이어질 수 있는 제어라 공조·볼륨과 분리한다.

- 기본값은 꺼짐(`rearWindowEnabled = false`). 설정에서 켤 때 "이동장·전용 안전벨트 사용, 폭우·고속도로에서는 꺼 두기" 안내 다이얼로그를 확인해야 켜진다.
- 개방률 5~10%. 실제 개방 폭(cm)은 차종마다 달라 실차에서 반려동물 머리가 빠지지 않는 폭인지 확인 후 상한을 조정한다 (13장).
- 비: 펫 모드를 **켤 때만** 확인한다. 비 감지 센서 API가 없어 앞 와이퍼(`getCurrentWiperSwitch`·`getCurrentWiperState`, 이미 선언한 `CAR_WINDOWS` 권한)로 추정한다 (`WiperRain`, `docs/pleos-sdk/vehicle.md` 4장).
  - 간헐·연속 스위치 → 비. AUTO → 실제 작동(ON)이면 비. OFF·MIST → 비 아님(단 작동 중이면 비로 본다). 둘 다 모르면 판단하지 않고 연다.
  - 펫 모드가 켜진 뒤 비가 오기 시작하는 경우는 앱이 알리거나 닫지 않는다. 사용자가 차량 창문 스위치로 직접 닫는다(설정에서 켤 때 안내).
- 창문 잠금: 열어 둔 동안 반려동물이 창문 스위치를 밟아 더 열리는 것을 막는다. 운전석 마스터 스위치로는 잠금 중에도 뒷창문을 조작할 수 있다고 가정한다(실차 확인).
- 고속 주행 제한은 보류. 차속 API는 SDK 2.3.2에서 삭제됐다. 반려동물이 창가에 있는지는 감지할 방법이 없다.

지원하지 않는 차량 기능은 "이 차량에서는 창문 자동 조절을 지원하지 않아요"처럼 안내.

### 7-7. 프로필·설정 (`ui/profile`, `ui/settings`) — F7

- 정차 중에만 진입 가능.
- 프로필: 여러 마리 등록 가능, 대표 1마리 지정.
- 설정: 펫 모드 기본값(온도·풍량·뒷창문 환기 사용 여부와 개방률·볼륨·자동 재생), 휴식 알림(ON/OFF, 간격 60/90/120분).
- 디버그 메뉴 (`fake` flavor에서만): 위치, 주행 상태, 연속 주행 시간, 휴대폰 연결 여부 조작.

### 7-8. 반려동물 동반 장소 (`ui/place`) — F8

**데이터:** 공공데이터 "반려동물 동반 가능 문화시설"(`data/facilities_with_pet.csv`) 중 카페·식당 / 여행지 / 문화시설(박물관·미술관·문예회관)이면서 `반려동물 동반 가능정보 = Y`인 곳. `scripts/build_public_data.py`로 `assets/data/pet_places.json`을 만들어 앱에 포함한다. 동물병원·약국·펜션은 제외.

**진입:** 홈의 "동반 장소" 버튼 (주행 중에도 표시, 7-1). 주행 중에는 종류 칩을 숨기고 마지막으로 고른 종류를 보여 준다

| 요소 | 동작 |
| --- | --- |
| 종류 칩 | 카페·식당 / 여행지 / 문화시설 (주행 중 숨김, 마지막 선택 유지) |
| 목록 | 반경 20km(없으면 50km). 영업 중 → 영업시간 확인 필요 → 영업 종료 순, 같은 그룹은 거리순. 최대 20곳 |
| 항목 | 이름, 거리, 영업 상태. 정차 시 대형견 가능, 운영시간·휴무 원문, 크기 제한·제한사항·추가 요금, 주소, 주차 |
| 항목 액션 | "경유지로 추가" (경로 있을 때) / "길 안내" (경로 없을 때). 터치 1회 |
| 내비에서 더 찾기 | 내장 내비 장소 검색 화면을 종류별 키워드로 연다 (`NavigationController.searchPlace`). 정차 시에만 |

**영업 상태:** `OpeningHoursParser`가 운영시간·휴무일 원문을 요일별 시간으로 바꾸고 `OpenStatusCalculator`로 계산한다. 계절별 시간·"일몰"·"변동" 등은 미확인. 공휴일·명절 휴무는 `KoreanHolidays`로 반영하고, "N째주" 휴무는 반영하지 않는다(원문을 함께 표시).

수용 기준: 주행 중에는 가장 가까운 3곳만 표시. 위치가 300m 이상 바뀔 때만 목록을 다시 계산한다.
유의: 원본 최종작성일이 2022~2025년이라 폐업·변경이 있을 수 있다. 화면에 출처와 "방문 전 확인" 안내를 표시한다.

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
| 성능 | 앱 콜드 스타트 2초 이내, 응급 목록 2초 이내 (공공데이터는 앱 시작 때 미리 로드) |
| 오프라인 | 병원·장소·산책 스팟·목업 데이터 모두 로컬이므로 네트워크 없이 전 기능 동작 |
| 개인정보 | 위치는 기기 안에서만 사용, 외부 전송 없음 (MVP). 위치 권한 요청 시 사용 목적 표시 |
| 접근성 | 텍스트 최소 24sp (주행 중 화면), 색만으로 상태 구분 금지 (배지 텍스트 병기) |
| 로그 | 개인정보·위치 좌표를 로그에 남기지 않음 |

---

## 10. 목업 데이터

`app/src/main/assets/mock/`에 생성한다. 병원은 목업 대신 공공데이터(7-2)를 쓴다. **병원 번호가 실제 번호이므로** fake 빌드의 `FakePhoneDialer`는 발신하지 않고 기록·토스트만 한다.

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

- [ ] 플레오스 플레이그라운드 개발자 등록, SDK·에뮬레이터 설치 (SDK 문서 정리본은 `docs/pleos-sdk/`에 저장함)
- [ ] 뒷창문 개방률 상한(10%)이 실차에서 반려동물 머리가 빠지지 않는 폭인지, 에뮬레이터·실차에서 와이퍼 스위치·상태가 읽히는지(AUTO일 때 비 강도에 따라 `WiperState`가 바뀌는지), 창문 잠금 중에도 운전석 마스터 스위치로 뒷창문을 닫을 수 있는지
- [ ] Vehicle SDK로 서드파티 앱이 공조·창문을 **제어**할 수 있는 권한 범위 → 앱 마켓 사전 승인 필요 (`docs/pleos-sdk/app-review.md`). 승인 요청 필요
- [x] 앱에서 블루투스 연결 휴대폰으로 바로 발신하는 방식 → SDK에 API 없음. `ACTION_DIAL`로 다이얼러에 번호만 채우기로 결정 (자동 발신 안 함). 실차에서 다이얼러·블루투스 동작 확인 필요
- [x] 미디어 볼륨 제어 방식 → SDK에 API 없음. 앱 재생 볼륨(안정 사운드)만 조절하기로 결정
- [x] Gleo AI 음성 명령을 서드파티 앱이 받는 방식 → Gleo Interface (`assets/schema.json` + Intent). 코드 완료, **스키마 승인 대기** (제출 경로 문의 필요)
- [ ] 앱 마켓 심사 기준과 플레오스 UI 디자인 가이드 (주행 중 UI 제한 포함)
- [ ] 펫 모드 기본 온도 등 수치에 대한 수의사 자문
- [ ] 패키지명 확정 (현재 `com.withpet.car`는 임시)
- [ ] 안정 사운드 음원 확보 (자체 제작 또는 라이선스) → 합성 임시 음원 적용. 출시용 확정 필요

---

## 14. Claude Code 첫 요청 예시

```
docs/withpet-dev-spec.md를 끝까지 읽어줘.
아직 코드는 쓰지 말고, M0~M2 구현 계획(만들 파일 목록, 의존성, 순서)을 먼저 보여줘.
13장에서 지금 결정이 필요한 항목이 있으면 같이 물어봐.
```
