package com.example.mytrackingdriver.models

import com.google.firebase.database.IgnoreExtraProperties


@IgnoreExtraProperties
data class Location(
    var Latitude: Double = 0.0,
    var Longitude: Double = 0.0
)
