# 위드펫 (WithPet)

플레오스 커넥트(Android Automotive) 차량용 반려동물 동승 케어 앱 MVP. 명세: [docs/withpet-dev-spec.md](docs/withpet-dev-spec.md)

## 빌드·실행

```bash
./gradlew assembleFakeDebug            # SDK 없이 실행 (기본)
./gradlew assemblePleosDebug           # Pleos Connect SDK 연동 (Pleos Connect Emulator 필요)
./gradlew testFakeDebugUnitTest        # 단위 테스트 (JUnit5)
./gradlew connectedFakeDebugAndroidTest  # UI 테스트 (에뮬레이터 필요)
```

debug APK는 아이콘 라이브러리 때문에 60MB 이상이다. 콜드 스타트 측정이나 저장 공간이 작은 에뮬레이터에는 `assembleFakeRelease`(R8 적용, 약 4MB, 디버그 서명)를 쓴다.

## 구조

| 패키지 | 역할 |
| --- | --- |
| `domain` | 모델, Repository 인터페이스, UseCase, 순수 로직 (진료 상태, 응급 정렬, 휴식 알림 규칙) |
| `data` | 공공데이터 변환 JSON(`assets/data`: 병원·동반 장소), 목업 JSON(`assets/mock`: 산책·플레이리스트), Room(프로필), DataStore(설정) |
| `platform` | 차량·내비·위치·전화·주행·음성 어댑터 인터페이스와 Fake 구현 |
| `media` | `CalmPlaybackService`(MediaSessionService) |
| `ui` | Compose 화면 |
| `src/fake`, `src/pleos` | flavor별 어댑터 바인딩, 디버그 메뉴(fake 전용) |

## 디버그 메뉴 (fake)

상단 벌레 아이콘. 위치 프리셋(위치 미수신 포함), 정차/주행 전환, 연속 주행 시간, 휴대폰 연결, 창문 미지원 차량, 샘플 경로, 음성 명령 버튼.

## 명세 대비 추가·변경

- `VehicleControl.rearWindowOpenPercent()`: 펫 모드 OFF 시 창문 복원값을 읽기 위해 추가. 값을 모르면 창문을 닫는다.
- `MediaVolumeControl`, `SpeechOutput` 어댑터: 볼륨 제어·TTS 방식이 확인 필요(13장)라 인터페이스로 분리.
- 휴식 알림 문구의 "10분 뒤"는 쉼터까지의 ETA로 계산한다.
- 휴식 알림 [추가] 시 경로가 없으면 해당 장소로 길 안내를 시작한다.
- 주행 제한 = 앱 주행 상태(DRIVING) **또는** 시스템 `CarUxRestrictions`의 DO 요구.

## Pleos SDK 연동 (M9, `src/pleos`)

42dot SDK 라이선스에 동의하고 `pleos` flavor에만 SDK를 넣었다. `fake` 빌드에는 SDK가 포함되지 않는다.

| 어댑터 | SDK | 구현 |
| --- | --- | --- |
| `PleosVehicleControl` | Vehicle 2.3.2 | 온도·풍량: 지원 좌석 구역 전체에 설정. 뒷창문: 2열 창문, 0~100%를 차량 위치 범위로 변환. 쓰기 권한이 없거나 지원 구역이 없으면 `UnsupportedFeature` |
| `PleosNavigationController` | NaviHelper 3.0.5 | `requestRoute` / `addWaypoint`(최대 2곳). 장소 검색은 `requestNavigationScreen(SearchPlace)`. ETA 조회 API가 없어 직선거리 추정으로 대체 |
| `PleosLocationProvider` | FusedLocation 1.1.3 | 위치 구독, 실패 시 "위치 미수신" 처리 |
| `PleosDrivingStateProvider` | (Android) | 공개 주행 상태 API가 없어 `CarUxRestrictions` DO 요구를 주행 중으로 본다 |
| 전화·볼륨·음성 | — | 공개 API/연동 방식 미확인 → 미지원 응답, UI는 안내형으로 대체 |

선언 권한(최소): `CAR_CLIMATE`, `CONTROL_CAR_CLIMATE`, `CAR_WINDOWS`, `CONTROL_CAR_WINDOWS`, `NAVI_ROUTE`, `FUSED_LOCATION`.

### 에뮬레이터에서 확인할 것

- 창문 위치 최솟값이 '완전히 닫힘'인지 (`WindowPositionMapping`의 가정)
- `requestRoute`에 POI ID 없이(빈 문자열) 좌표만으로 경로가 잡히는지
- 경로 요청 직후 `onNavigationStatus`가 `FREE_DRIVE`를 먼저 보내 활성 경로가 지워지지 않는지
- `Vehicle.initialize()` 직후 바로 호출해도 되는지 (서비스 바인딩 지연)
- `requestNavigationScreen(SearchPlace("애견동반 카페"))`가 내비 검색 화면을 앞으로 띄우는지, 검색 결과가 쓸 만한지 (키워드 조정 필요할 수 있음)

## 공공데이터 (병원·동반 장소)

`app/src/main/assets/data/`의 JSON은 공공데이터 "반려동물 동반 가능 문화시설" CSV를 변환한 것이다. 원본 CSV(30MB)는 저장소에 넣지 않는다(`.gitignore`). 원본이 바뀌면 `data/`에 넣고 다시 만든다.

```bash
python3 scripts/build_public_data.py data/facilities_with_pet.csv app/src/main/assets/data
./gradlew testFakeDebugUnitTest --tests '*AssetTest*'   # 개수·좌표·전화번호 형식·운영시간 해석률 검증
```

- `hospitals.json`: 동물병원 (CSV에 같은 병원이 약 3번씩 반복되어 중복 제거 후 약 4,500곳, 1.1MB). 응급 모드에서 쓴다.
- `pet_places.json`: 카페·식당 / 여행지 / 문화시설 중 동반 가능(Y)만 (약 1,900곳, 700KB). 약국·펜션은 제외.
- 운영시간·휴무일은 원문 그대로 두고 앱의 `OpeningHoursParser`가 해석한다 (동반 장소 약 93%, 병원 약 59% — 병원은 운영시간 "정보없음"이 많고, "N째주" 휴진은 미확인으로 처리).
- 출처 표기와 이용 조건(공공누리 유형 등)은 원본 제공처 기준으로 확인 필요. 화면에는 출처 문구를 표시한다.

## 남은 일 (13장 확인 필요)

- Pleos Connect Emulator 2.1.0 설치 후 위 항목 검증 (Android Studio SDK Manager → SDK Update Sites, 기획서 링크 참조)
- 전화 발신, 미디어 볼륨, Gleo AI(TTS/STT) 연동 방식
- 응급 원터치 안내 유의점: 진료 중 병원이 있으면 응급 버튼 한 번에 **전화까지 자동 발신**된다. `PleosPhoneDialer`를 실제로 연동하면
  - 발신은 블루투스로 연결된 운전자 휴대폰 회선으로 나가야 한다 (병원에는 운전자 번호가 표시됨). 차량 통신 모듈 회선이 아닌지 확인
  - 오터치로 의도치 않은 발신이 생길 수 있다. 필요하면 자동 선택 시 길 안내만 하고 전화는 버튼으로 거는 방식 검토
  - "다른 병원 보기"로 다시 고르면 두 번째 발신이 나가며, 먼저 건 통화는 앱이 끊지 않는다
  - 병원 번호는 **실제 번호**(`assets/data/hospitals.json`)다. 지금은 fake 빌드는 토스트만, pleos 빌드는 미구현이라 발신되지 않지만, 다이얼러를 실제로 연동하는 순간 테스트 중에도 실제 병원으로 전화가 나간다. 연동 전에 테스트용 발신 차단 방안을 정할 것
  - 공휴일 휴진은 반영하지 못해 공휴일에 닫은 병원이 "진료 중"으로 나올 수 있다 (자동 선택 대상이 될 수 있음)
- 안정 사운드 음원: 지금은 `scripts/generate_calm_sounds.py`로 합성한 임시 음원(곡당 60초 루프, AAC 64kbps, 총 약 4MB). 출시 전 실제 차량 스피커로 들어 보고, 필요하면 라이선스 음원이나 작곡으로 교체
- 패키지명 확정, 릴리스 서명
