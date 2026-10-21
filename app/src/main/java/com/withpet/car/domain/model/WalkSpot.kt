package com.withpet.car.domain.model

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
