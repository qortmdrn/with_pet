package com.withpet.car.domain.model

data class PetProfile(
    val id: Long,
    val name: String,
    val species: Species,
    val breed: String?,
    val birthYear: Int?,
    val weightKg: Float?,
    val photoUri: String?,
)

enum class Species { DOG, CAT, OTHER }
