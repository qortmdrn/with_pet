package com.withpet.car.domain.model

/** 반려동물 동반 가능 장소 (공공데이터: 반려동물 동반 가능 문화시설) */
data class PetPlace(
    val id: String,
    val name: String,
    val category: PetPlaceCategory,
    val location: GeoPoint,
    val address: String,
    val phone: String?,
    val hasParking: Boolean,
    /** 입장 가능 크기 원문. "모두 가능", "소형", "10kg 이하" 등 */
    val sizeLimit: String?,
    /** 제한사항 원문. "목줄, 배변봉투", "야외만 반려동물 동반 가능" 등 */
    val restriction: String?,
    val extraFee: String?,
    /** 운영시간·휴무일 원문. 해석 결과([weeklyHours])와 함께 화면에 보여 준다. */
    val hoursText: String?,
    val closedDaysText: String?,
    /** 비어 있으면 영업시간 미확인 */
    val weeklyHours: List<OpeningHours>,
    /** 이 종류의 공휴일에는 휴무 */
    val closedOnHolidays: Set<HolidayKind> = emptySet(),
    /** 원본 데이터의 최종작성일 (yyyy-MM-dd) */
    val updatedAt: String,
) {
    val largeDogAllowed: Boolean get() = sizeLimit == null || sizeLimit == "모두 가능" || sizeLimit.startsWith("대형")
}

enum class PetPlaceCategory {
    CAFE,       // 카페·식당
    TRAVEL,     // 여행지 (공원·해변·관광지 등)
    CULTURE,    // 박물관·미술관·문예회관
}
