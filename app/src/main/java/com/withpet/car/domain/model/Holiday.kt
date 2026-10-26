package com.withpet.car.domain.model

/** 휴무일 원문의 공휴일 표현과 맞추기 위한 공휴일 종류 */
enum class HolidayKind {
    NEW_YEAR,   // 1월 1일
    SEOLLAL,    // 설 연휴 (대체공휴일 포함)
    CHUSEOK,    // 추석 연휴 (대체공휴일 포함)
    OTHER,      // 그 밖의 공휴일·대체공휴일·선거일
}

/**
 * 해석한 영업시간.
 * @param weekly 요일별 영업시간. 비어 있으면 미확인.
 * @param closedOnHolidays 이 종류의 공휴일에는 쉰다 ("법정공휴일"이면 전부).
 */
data class OpeningSchedule(
    val weekly: List<OpeningHours>,
    val closedOnHolidays: Set<HolidayKind> = emptySet(),
)
