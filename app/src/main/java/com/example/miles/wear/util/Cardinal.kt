package com.example.miles.wear.util

/** Cardinal compass directions derived from a bearing in degrees (0-360). */
enum class Cardinal(val title: String) {
    N("North"), NE("Northeast"), E("East"), SE("Southeast"),
    S("South"), SW("Southwest"), W("West"), NW("Northwest");

    companion object {
        fun fromDegrees(bearingDeg: Float): Cardinal {
            val normalized = ((bearingDeg % 360f) + 360f) % 360f
            val index = ((normalized + 22.5f) / 45f).toInt() % 8
            return values()[index]
        }
    }
}