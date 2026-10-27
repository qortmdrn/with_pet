# Vehicle SDK 정리 (주행 상태 감지 중심)

> 조사일: 2026-10-26. 가이드와 릴리즈 노트 기준 최신 **Vehicle v2.3.2**(Vehicle Service v2.3.21 기준). 현재 사용 중인 버전과 같음.
> `B = https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle`
> `A = B/vehicle/ai.pleos.playground.vehicle.api`

## 0. 결론: DRIVING/PARKED 판별
- **Vehicle SDK에는 기어(Gear), 차속(Speed), 주차 브레이크를 조회하는 공개 도메인이 없음.**
  - 도메인 목록: Brake, CarInfo, Display, Door, DrivingMode, EvBattery, Fuel, HVAC, Light, Odometer, Safety, Seat, SideMirror, SteeringWheel, Tire, Toll, TurnSignal, Window, Wiper — `B/intro`, `B/vehicle/ai.pleos.playground.vehicle/vehicle/entry`
  - `Vehicle` 클래스 KDoc 예시에 `vehicle.getGear()` / `GearSelectionListener` / `VehicleGear`가 나오지만, **멤버 목록에는 `getGear()`가 없음** → 문서의 오래된 예시로 판단(추정). 사용하지 말 것 — `B/vehicle/ai.pleos.playground.vehicle/vehicle/entry`
  - v2.3.2에서 **`VehicleSpeedListener`와 `onSpeedUpdated`가 삭제됨** — https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015
  - `Brake`는 **브레이크 페달** 눌림 여부(`getCurrentBrakePedalStatus(onComplete: (Boolean?) -> Unit, onFailed)`)만 제공하고 주차 브레이크는 없음 — `A/brake/entry`
  - `PARKING_BRAKE` / `ELECTRONIC_PARKING_BRAKE`는 `Safety` telltale 값으로만 존재하고, telltale API는 protection level이 **signature|privileged**라 3rd party가 쓸 수 없음 — `A/safety/get-telltales-info`
  - `DrivingMode`는 ECO/NORMAL/SPORT 드라이브 모드와 크루즈 상태(`getCruiseControlTargetSpeed: Float?`)만 제공하며, 주행 여부와는 무관 — `A/driving-mode/entry`
- **공식 권장 경로는 `CarUxRestrictionsManager`** (현재 `PleosDrivingStateProvider` 구현과 일치): 앱은 플랫폼이 내려주는 CarUxRestrictions를 기준으로 주행 중 UX를 제한해야 함 — https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide
  - 주행 상황 구분(Parking / Idling / Driving)과 지역별 제한 표 — https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy
- 보조 수단: AAOS `CarPropertyManager`와 `VehiclePropertyIds`(예: GEAR_SELECTION, PERF_VEHICLE_SPEED, PARKING_BRAKE_ON)는 "차량 모델별로 지원 여부를 확인하고 쓰라"는 안내만 있음. 또한 "SDK가 제공하지 않는 특수 프로퍼티가 필요하면 직접 구현하기 전에 기술 지원팀에 검토를 요청하라"고 돼 있음 — https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability
  - 이때 필요한 `android.car.permission.CAR_SPEED` / `CAR_POWERTRAIN` 등이 3rd party에 허용되는지는 **문서에 없음**.
- 참고: `Odometer.getTripCurrentDrivingTime(onComplete: (Int?) -> Unit /*분*/, onFailed)` — "현재 주행 세션의 주행 시간(분)". 연속 주행 시간 보조 지표로 쓸 수 있음. 권한은 `CAR_DRIVE` — `A/odometer/get-trip-current-driving-time`

## 1. 의존성·권한·승인
- `ai.pleos.playground:Vehicle:2.3.2` — `B/project-setup`
- 권한 형식: 조회는 `pleos.car.permission.CAR_*`, 제어는 `pleos.car.permission.CONTROL_CAR_*` — `B/vehicle-permission`
  - Window·Wiper는 `CAR_WINDOWS`, HVAC는 `CAR_CLIMATE`, DrivingMode·Brake·Odometer는 `CAR_DRIVE`, Safety·Tire는 `CAR_SAFETY` 등
  - 대부분 protection level normal. Safety telltale과 `setSmartGlassState`는 signature|privileged.
- **심사 기준: "Vehicle SDK 퍼미션은 Pleos App Market과 사전 협의·승인된 경우에만 선언할 수 있고, 승인되지 않은 퍼미션을 선언한 앱은 심사를 통과할 수 없음"** — https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability#sdk-api-review
- My Project에서 "Vehicle State"(조회)는 신청 없이 사용 가능하고, "**Vehicle Control SDK**"는 "준비 중(Coming Soon)"으로 표기돼 있음 → `CONTROL_CAR_CLIMATE` / `CONTROL_CAR_WINDOWS` 사용은 **승인이 필요한 리스크**가 있음 — https://document.pleos.ai/docs/pleos-only/create-my-project

## 2. 호출 규칙
- `Vehicle(context)` → `initialize()` → `getXxx()`로 도메인 객체를 얻음 → capability/get/set/register → `release()` — `B/obtain-domain-object`
- IPC 비동기 방식이라 결과는 `onComplete` / `onFailed` 람다로 옴. **비동기 콜백이 끝나기 전에 `release()`를 호출하면 안 됨** — `B/intro`
- 콜백 스레드는 **문서에 없음**.
- `onFailed(Exception)`에서 `VehicleException`이면 `errorCode` 확인: `ERROR_NOT_SUPPORTED_PROPERTY`, `ERROR_NOT_SUPPORTED_AREA`, `ERROR_NOT_SUPPORTED_VALUE_TYPE`, `ERROR_SECURITY_PERMISSION`, `ERROR_SERVICE_CONNECTION_FAILURE`, `ERROR_INTERNAL_SERVICE`, **`ERROR_DRIVING_RESTRICTED`(주행 중 제한)**, `ERROR_EMPTY_RESULT`, `ERROR_UNDEFINED` — `B/vehicle-error-code`
- capability: `CapabilityConfig.accessType`(NONE/READ/WRITE/READ_WRITE)와 `Map<Area, AreaBounds>`(minValue/maxValue) — `B/check-capability`
- v2.3.2 breaking change: `WindowArea` 등 Area 클래스가 내용 기반 `equals/hashCode`를 갖게 됨. `checkHvacFanSpeedCapabilities`는 `Map<SeatArea, AreaBounds<Int>>`를 반환 — 릴리즈 노트

## 3. Window 관련 (우리 가정 "position 최소값 = 완전히 닫힘" 검증)
- `checkWindowPositionCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, AreaBounds<Int>>) -> Unit, onFailed)`. 구 오버로드(Map만 반환)는 deprecated — `A/window/check-window-position-capabilities`
- `getCurrentWindowPosition(area, onComplete: (Int?) -> Unit, onFailed)`, `setWindowPosition(area, value: Int, onComplete: (WindowArea, Int?) -> Unit = {_,_->}, onFailed = {})` — 권한은 각각 `CAR_WINDOWS`, `CONTROL_CAR_WINDOWS`
- **Window 문서에는 min/max의 의미(닫힘/열림)가 없음.** 비슷한 Door 문서에는 "Min is typically 0 for fully closed, 최대(완전 열림)는 차량마다 다름"이라고 적혀 있음 — `A/door/set-door-position`
- AAOS 표준 `WINDOW_POS` 정의(외부 문서, 추정 근거)는 min = 완전히 닫힘, max = 완전히 열림. 단, 틸트·벤트가 되는 창(선루프 등)은 **음수 범위가 벤트 열림**이고 0이 닫힘임. → `minValue < 0`인 area가 있으면 우리 가정이 틀리게 됨. 닫힘을 `max(minValue, 0)`으로 매핑하는 방어 처리를 권장(추정).
- 공조(HVAC)와 관련해 우리 가정과 충돌하는 문서 내용은 찾지 못함.

## 4. Wiper·창문 잠금 (뒷창문 환기 상황별 제한 검토용)
> 조사일: 2026-10-27. API 레퍼런스 페이지 본문이 스크립트로 렌더링돼 가져오지 못해서 **로컬 SDK(`Vehicle-2.3.2.aar`)의 클래스 시그니처와 `@RequiresPermission` 어노테이션**으로 확인함. 각 값의 의미 설명은 문서로 확인하지 못함.

- 도메인 객체: `Vehicle.getWiper(): Wiper` (콜백 값의 null 가능 여부는 바이트코드로 확인 못 함)
- 조회는 모두 `pleos.car.permission.CAR_WINDOWS`(**이미 선언한 권한, 추가 승인 불필요**). `setWiperSwitch`만 `CONTROL_CAR_WINDOWS`
  - `checkWiperStateCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, List<WiperState>>) -> Unit, onFailed)`
  - `checkWiperSwitchCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, List<WiperSwitch>>) -> Unit, onFailed)`
  - `getCurrentWiperState(area: WindowArea, onComplete: (WiperState) -> Unit, onFailed)`
  - `getCurrentWiperSwitch(area: WindowArea, onComplete: (WiperSwitch) -> Unit, onFailed)`
  - `registerWiperState(area, WiperStateListener)` / `unregisterWiperState` — `onWiperStateUpdated(WiperState)`. **스위치 변경 리스너는 없음**(스위치는 조회로만)
- `area`는 `WindowArea(WindowValue.FRONT_WINDSHIELD)` (그 외 `REAR_WINDSHIELD` 등)
- `WiperState`: `OTHER`, `OFF`, `ON`, `SERVICE` → 와이퍼가 실제로 움직이는지(추정, AAOS `WINDSHIELD_WIPERS_STATE`와 같은 구성)
- `WiperSwitch`: `OFF`, `MIST`, `INTERMITTENT_LEVEL_1~5`, `CONTINUOUS_LEVEL_1~5`, `AUTO`, `SERVICE`, `OTHER` → 운전자가 둔 스위치 위치(추정, AAOS `WINDSHIELD_WIPERS_SWITCH`)
- **비 감지 센서(rain sensor) API는 없음.** 비는 와이퍼로 간접 추정만 가능
  - `CONTINUOUS_LEVEL_*` = 비가 많이 온다고 볼 근거가 가장 강함
  - `AUTO`는 강도를 알 수 없음 → `WiperState == ON`이면 비가 온다고만 판단
  - `MIST`(1회 닦기)·워셔액 분사 때도 `ON`이 잠깐 들어올 수 있음(추정) → 일정 시간 이상 지속될 때만 판단
- 창문 잠금: `Window.setWindowLock(area, Boolean)`(`CONTROL_CAR_WINDOWS`), `getCurrentWindowLockState(area, (Boolean) -> Unit)`·`checkWindowLockCapabilities`·`registerWindowLockState`(`CAR_WINDOWS`). 뒷창문을 연 동안 반려동물이 창문 스위치를 밟는 것을 막는 데 쓸 수 있음(차량 지원 여부는 capability로 확인)
- 에뮬레이터·실차에서 확인할 것: capability가 실제로 지원으로 나오는지, `AUTO`일 때 `WiperState`가 비 강도에 따라 바뀌는지, 콜백 지연
