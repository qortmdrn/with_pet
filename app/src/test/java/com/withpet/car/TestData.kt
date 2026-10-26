package com.withpet.car

import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.Hospital
import com.withpet.car.domain.model.OpeningHours
import com.withpet.car.domain.model.WalkSpot
import com.withpet.car.domain.model.WalkSpotType
import java.time.DayOfWeek
import java.time.LocalTime

val CITY_HALL = GeoPoint(37.5663, 126.9779)

/** 기준점에서 북쪽으로 [km]만큼 떨어진 지점 (위도 1도 ≈ 111km) */
fun GeoPoint.north(km: Double) = GeoPoint(lat + km / 111.0, lng)

fun hospital(
    id: String,
    location: GeoPoint = CITY_HALL,
    is24Hours: Boolean = false,
    hours: List<OpeningHours> = emptyList(),
) = Hospital(
    id = id,
    name = "테스트 동물병원 $id",
    location = location,
    address = "주소",
    phone = "010-0000-0000",
    is24Hours = is24Hours,
    weeklyHours = hours,
    isPartner = false,
)

fun everyDay(open: String, close: String) =
    DayOfWeek.entries.map { OpeningHours(it, LocalTime.parse(open), LocalTime.parse(close)) }

fun spot(
    id: String,
    location: GeoPoint,
    largeDog: Boolean = false,
    type: WalkSpotType = WalkSpotType.PARK,
    hours: List<OpeningHours> = everyDay("00:00", "00:00"),
) = WalkSpot(
    id = id,
    name = "스팟 $id",
    type = type,
    location = location,
    address = "주소",
    leashRequired = true,
    sizeLimit = if (largeDog) "모두 가능" else "소형",
    restriction = null,
    fee = null,
    hoursText = null,
    closedDaysText = null,
    weeklyHours = hours,
)
