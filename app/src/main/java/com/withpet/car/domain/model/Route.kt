package com.withpet.car.domain.model

data class NamedPoint(val point: GeoPoint, val name: String)

data class RouteInfo(
    val destination: NamedPoint,
    val waypoints: List<NamedPoint>,
)
