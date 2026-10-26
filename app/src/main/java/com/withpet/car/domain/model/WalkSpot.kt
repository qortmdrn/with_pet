package com.withpet.car.domain.model

/** 산책 스팟 (공공데이터 "반려동물 동반 가능 문화시설"의 여행지 중 산책할 만한 곳) */
data class WalkSpot(
    val id: String,
    val name: String,
    val type: WalkSpotType,
    val location: GeoPoint,
    val address: String,
    val leashRequired: Boolean,
    /** 입장 가능 크기 원문. null이면 제한 정보 없음 */
    val sizeLimit: String?,
    /** 제한사항 원문. "목줄, 배변봉투" 등 */
    val restriction: String?,
    /** 입장료 원문. null이면 무료 */
    val fee: String?,
    val hoursText: String?,
    val closedDaysText: String?,
    /** 비어 있으면 운영시간 미확인 */
    val weeklyHours: List<OpeningHours>,
    val closedOnHolidays: Set<HolidayKind> = emptySet(),
) {
    val largeDogAllowed: Boolean get() = sizeLimit == null || sizeLimit == "모두 가능" || sizeLimit.startsWith("대형")
}

enum class WalkSpotType { DOG_PARK, PARK, TRAIL, WATERSIDE, REST_AREA }
