# 위드펫 (WithPet)

플레오스 커넥트(Android Automotive) 차량용 반려동물 동승 케어 앱 MVP. 명세: [docs/withpet-dev-spec.md](docs/withpet-dev-spec.md)

## 빌드·실행

```bash
./gradlew assembleFakeDebug            # SDK 없이 실행 (기본)
./gradlew assemblePleosDebug           # Pleos SDK 어댑터 (M9 전까지 모두 미지원 응답)
./gradlew testFakeDebugUnitTest        # 단위 테스트 (JUnit5)
./gradlew connectedFakeDebugAndroidTest  # UI 테스트 (에뮬레이터 필요)
```

debug APK는 아이콘 라이브러리 때문에 60MB 이상이다. 콜드 스타트 측정이나 저장 공간이 작은 에뮬레이터에는 `assembleFakeRelease`(R8 적용, 약 4MB, 디버그 서명)를 쓴다.

## 구조

| 패키지 | 역할 |
| --- | --- |
| `domain` | 모델, Repository 인터페이스, UseCase, 순수 로직 (진료 상태, 응급 정렬, 휴식 알림 규칙) |
| `data` | 목업 JSON(`assets/mock`) Repository, Room(프로필), DataStore(설정) |
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

## 남은 일 (13장 확인 필요)

- M9: `docs/pleos-sdk/`에 SDK 문서를 받은 뒤 `src/pleos/.../PleosAdapters.kt` 구현
- 안정 사운드 음원: 지금은 `res/raw/calm_placeholder_silence.wav`(무음) 하나를 모든 곡이 가리킨다
- 패키지명 확정, 릴리스 서명, 위치 권한 안내(실제 위치 SDK 연동 시)
