package com.family.farecompare.domain.model

enum class RideProvider(val displayName: String, val packageName: String) {
    UBER("Uber", "com.ubercab"),
    OLA("Ola", "com.olacabs.customer"),
    RAPIDO("Rapido", "com.rapido.passenger")
}
