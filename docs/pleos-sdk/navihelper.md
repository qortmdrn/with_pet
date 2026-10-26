# NaviHelper SDK 정리 (v3.0.5)

> 조사일: 2026-10-26. 가이드와 릴리즈 노트 기준 최신 **v3.0.5**(현재 사용 중인 버전). API Reference는 사이트 JS 번들에서 원문을 추출함.
> `B = https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper`
> `H = B/navi-helper/ai.pleos.playground.navi.helper/navi-helper`
> `D = B/navi-helper/ai.pleos.playground.navi.data`, `C = B/navi-helper/ai.pleos.playground.navi.constants`
> `L = B/navi-helper/ai.pleos.playground.navi.helper.listener/navi-helper-event-listener`

## 1. 의존성·권한·자격 증명
- `ai.pleos.playground:NaviHelper:3.0.5` — `B/importing-sdk`
- 권한 (모두 protection level **Normal**) — `B/declaring-permissions`, `H/request-route`
  | 권한 | API |
  |---|---|
  | `pleos.car.permission.NAVI_ROUTE` | `get*Info()`, `requestTrip()` |
  | `pleos.car.permission.NAVI_ROUTE_SEARCH` | `requestRoute`, `requestRouteWithWaypoints`, `cancelRoute`, `requestReRoute`, `addWaypoint`, `removeWaypoint`, `changeRouteOption` |
  | `pleos.car.permission.NAVI_CUSTOM_MAP` | `requestNavigationScreen` |
  - 우리 `pleos` 매니페스트에는 지금 `NAVI_ROUTE`만 있음. `requestRoute`와 `SearchPlace`를 쓰려면 `NAVI_ROUTE_SEARCH`, `NAVI_CUSTOM_MAP`를 추가해야 함.
- 자격 증명이나 신청: My Project에서 "NaviHelper는 신청 없이 바로 사용 가능" — https://document.pleos.ai/docs/pleos-only/create-my-project
- 호환성: "내비게이션이 탑재된 차량에 한해 전체 호환" — https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability

## 2. 진입점 `class NaviHelper(context: Context)` — `H/entry`
```kotlin
fun initialize()
fun initialize(onConnected: () -> Unit, onFailed: (Throwable) -> Unit) // 첫 연결 성공 시 / 초기 재시도 소진 시
fun addListener(listener: NaviHelperEventListener)
fun removeListener(listener: NaviHelperEventListener)
fun release()
// NAVI_ROUTE_SEARCH
fun requestRoute(routeInfo: RouteInfo)
fun requestRouteWithWaypoints(request: RouteWithWaypointsRequest)
fun cancelRoute(); fun requestReRoute(); fun changeRouteOption(routeOption: RouteOption)
fun addWaypoint(requestWaypointInfo: RequestWaypointInfo); fun removeWaypoint(waypointIndex: WaypointIndex)
// NAVI_ROUTE
fun getDestinationInfo(); fun getRouteStateInfo(); fun getTBTInfo(); fun getWaypointInfo()
fun getCurrentLocationInfo(); fun getBookmarkInfo(); fun getRecentDestinationInfo(); fun getChargerOperatorInfo()
fun requestTrip(tripRequest: TripRequest)
// NAVI_CUSTOM_MAP
fun requestNavigationScreen(requestScreen: NavigationScreenRequest)
```
- 모든 조회와 요청은 **반환값 없이 리스너 콜백으로만** 결과가 옴. `requestRoute`가 성공했다고 탐색이 완료된 것은 아님 — `B/destinations`
- "실제로 오는 이벤트는 NaviProvider로 구현된 Navi 앱이 어떻게 응답하느냐에 따라 달라질 수 있음" — `H/request-route`, `H/request-trip`
- 리스너 콜백 스레드는 **문서에 없음**.

## 3. 리스너 `NaviHelperEventListener` (모두 `open`, 기본 구현 있음) — `L/entry`
| 콜백 | 비고 |
|---|---|
| `onRouteStarted(RouteStartInfo)` | 경로 탐색 시작. duration(초), distance(m), tollFare, name, remainDestinationChargingValue |
| `onRoutingCompleted()`, `onRouteCancelled()`, `onReRouteEnded()`, `onRouteOptionChanged(RouteOption)` | |
| `onDestinationChanged()`, `onDestinationArrived(DestinationArrivedInfo)` | |
| `onWaypointChanged(WaypointChangedInfo)`, `onWayPointArrived(WaypointArrivedInfo)`, `onWayPointInfo(List<WaypointInfo>)` | |
| `onDestinationInfo(DestinationInfo)` | `getDestinationInfo()` 응답 |
| `onRouteStateInfo(RouteStateInfo)` | `getRouteStateInfo()` 응답 |
| `onTBTInfo(List<TBTInfo>)` | |
| `onTripInfo(TripInfo)` | `requestTrip()` 응답 |
| `onDrivingInfo(DrivingInfo)` | 주행 중 목적지·경유지 남은 정보 (주기는 **문서에 없음**) |
| `onNavigationStatus(NavigationStatus)` | "주행 상태가 바뀔 때마다 수신" |
| `onCurrentRoadSpeedLimit(Int)` | 등록 후 **매초** 수신 |
| `onCurrentLocationInfo`, `onBookmarkInfo`, `onBookMarkChanged`, `onRecentDestinationInfo`, `onChargerOperatorInfo`, `onCameraAlert`, `onCautionPointInfo` | |
| `onError(NaviErrorCode)` | |

## 4. 남은 시간·ETA·거리 얻는 방법

### 4-1. 활성 경로(현재 안내 중)
- **`getDestinationInfo()` → `onDestinationInfo(DestinationInfo)`**
  `DestinationInfo(latitude, longitude, displayLatitude?, displayLongitude?, name, address, remainDistance: Int /*m*/, remainTime: Int /*초*/, tollFare: Int /*원*/, poiId, poiSubId?, poiProvider = "")` — `D/destination-info/entry`
  (v3.0.5에서 `totalDistance/totalTime`이 `remainDistance/remainTime`으로 이름이 바뀜 — https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015)
- **`onDrivingInfo(DrivingInfo)`** (푸시): `DrivingInfo(destination: DrivingState, waypoints: List<DrivingState>)`
  `DrivingState(name, duration: Int /*현재 위치→지점, 초*/, distance: Int /*m, "출발점으로부터"라고 기술됨*/, remainDestinationChargingValue, isChargingStation=false, ...)` — `D/driving-info/entry`, `D/driving-state/entry`
  - `distance` 설명이 "from the start"라서 남은 거리인지 누적 거리인지 모호함 → 남은 거리는 `DestinationInfo.remainDistance` 사용 권장(추정).
- `RouteStateInfo(termsAgreement, naviMode: NaviMode, isRouting: Boolean, routeOption, remainDestinationChargingInt, remainWaypointChargingInt, trafficStatus?)` — **시간·거리 필드 없음**. 경로 활성 여부(`isRouting`)와 모드만 확인용 — `D/route-state-info/entry`
- `TBTInfo(type: TurnType, distance: Double /*다음 회전까지 m*/, description: String)` — ETA와 무관 — `D/t-b-t-info/entry`

### 4-2. 임의 목적지(활성 경로가 아닌 곳)의 ETA → **`requestTrip` 사용 가능**
```kotlin
data class TripRequest(val latitude: Double, val longitude: Double, val poiId: String, val poiName: String, val poiSubId: String?)
// → onTripInfo(TripInfo)
data class TripInfo(val distance: Int /*m*/, val eta: Int /*초*/, val estimatedSocAtDest: Int, val energyConsumption: Double, val summary: TripSummary)
data class TripSummary(val trafficConditions: List<TrafficInfo>, val routePoints: List<RoutePointInfo>)
```
출처: `H/request-trip`, `D/trip-request/entry`, `D/trip-info/entry`, `D/trip-summary/entry`
- 권한은 `NAVI_ROUTE`(이미 선언돼 있음). 실패하면 `NaviErrorCode.CAUSE_SEARCH_TRIP`이 옴(추정 연결) — `C/navi-error-code/entry`
- `eta`는 "Estimated Time of Arrival (ETA) in seconds"라고만 적혀 있음. **소요 시간(초)인지 epoch 시각인지 문서에 명시 없음** → 소요 시간(초)으로 추정. 실기기·에뮬레이터 확인 필요.
- 활성 경로 없이도 동작하는지, 여러 건을 동시에 요청하면 응답을 어떻게 구분하는지(요청 ID 없음)는 **문서에 없음**. 응답에 요청 식별자가 없으므로 **한 번에 하나씩 순차 요청**해야 함(추정).
- `poiId`는 non-null `String`. 빈 문자열이 허용되는지는 **문서에 없음**.

## 5. `requestRoute`와 POI ID
```kotlin
data class RouteInfo(
  val latitude: Double, val longitude: Double,
  val displayLatitude: Double? = null, val displayLongitude: Double? = null,
  val poiName: String, val poiId: String, val address: String, val poiSubId: String,
  val routeOption: RouteOption = RouteOption.RECOMMENDED, val poiProvider: String = "", val isMobileRequest: Boolean = false)
```
출처: `D/route-info/entry`
- `poiId`, `poiSubId`, `address`는 **non-null 필수 인자**. **좌표만으로 요청할 때 빈 문자열을 넣어도 되는지는 문서에 없음**. 문서 예시는 모두 42dot POI ID(`"36khp37hh8gqcm5nqhe5"`, `poiSubId = "0"`)를 씀 — `B/destinations`
  - error-handling 체크리스트에 "좌표, **POI ID** 및 경유지 인덱스가 올바른지 확인"이 있음 → POI ID가 잘못되면 실패할 수 있음 — `B/error-handling`
  - 현재 코드(`PleosNavigationController.kt`)는 `poiId = ""`를 씀 → **에뮬레이터 검증 필요**. 공공데이터 병원에는 42dot POI ID가 없으므로 실패하면 대안으로 `SearchPlace(병원명)` 화면을 띄우는 것을 검토(추정).
- 좌표 인자 순서: v3.0.5부터 `latitude, longitude` 순서. **가이드의 Java 예시는 아직 이전 순서(longitude 먼저)로 남아 있으니 따라 쓰지 말 것** — https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015 , `B/destinations`
- `RouteOption`: `RECOMMENDED, SHORT_DISTANCE, SHORT_TIME, FREE, HIGHWAY, COMFORTABLE, SCHOOL_ZONE` — `B/routes`
- 경유지는 최대 2개(`WaypointIndex.FIRST/SECOND`, 전체 삭제는 `ALL`) — `B/waypoint-settings`

## 6. `requestNavigationScreen` / `SearchPlace` — `D/navigation-screen-request/entry`
`sealed class NavigationScreenRequest`의 하위 타입:
`Foreground`, `FavoriteMain`, `AddHomeFavorite`, `AddWorkFavorite`, `NearbyChargingStations`, `Settings`, `MoveToCurrentLocationOnMap`, `RecentDestinations`, `RouteOptionList`, `EditRoute`, `OverviewRoute(info: RouteOverviewShowInfo)`, `POIDetailPopup(poiId: String, poiProvider: String = "")`, **`SearchPlace(keyword: String)`**
- `SearchPlace`의 설명은 "Place search screen" 한 줄뿐. 검색 결과를 앱에 돌려주는 콜백은 **없음**(사용자가 내비 화면에서 직접 선택). 주행 중 검색 화면 잠금 등 동작은 **문서에 없음**. 단, 주행 규제 정책상 "내비게이션 검색"은 주행 중 키보드·필터링 제한 대상임 — https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy
- 권한은 `NAVI_CUSTOM_MAP`.

## 7. 내비 상태
- `NavigationStatus`: `INACTIVE`, `INITIALIZING`, `FREE_DRIVE`(경로 없이 지도만 표시), `ROUTE_DRIVE`(경로 안내 중) — `C/navigation-status/entry`
  - v3.0.5 breaking change: `onNavigationStatus`의 인자가 `RouteDriving`에서 `NavigationStatus`로 바뀜.
  - 주의: 이 값은 **내비 앱 상태**이지 차량의 주행·주차 상태가 아님.
- `NaviMode`(RouteStateInfo 안): `NOT_WORKING`, `SAFE_GUIDING`, `ROUTE_GUIDING`, `SIMULATION_GUIDING` — `C/navi-mode/entry`
- 활성 경로 여부를 확인하는 방법은 `onNavigationStatus == ROUTE_DRIVE` 또는 `RouteStateInfo.isRouting`.

## 8. 에러
- 초기화 실패: `initialize(onFailed = { throwable -> })` — `B/error-handling`
- `NaviErrorCode`: `CAUSE_LOW_BATTERY`, `CAUSE_SEARCH_ROUTE_OVER_DISTANCE`, `CAUSE_SEARCH_ROUTE`, `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE`, `CAUSE_SEARCH_TRIP` — `C/navi-error-code/entry`
  (가이드 표에는 `CAUSE_SEARCH_TRIP`이 빠져 있지만 API Reference에는 있음)

## 9. 정책 메모
- 단일 경로 정책: Active Route는 내비 앱 하나만 가질 수 있음. 우리 앱은 자체 경로 안내를 하지 말고 NaviHelper에 위임할 것 — https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide
