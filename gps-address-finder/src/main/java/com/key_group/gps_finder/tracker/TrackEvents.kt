package com.key_group.gps_finder.tracker

enum class TrackEvents(val eventName: String) {
    GOOGLE_API_CONNECTION_FAILED("Connection Failed"),
    ON_LOAD_LOCATION("Location Find"),
    ON_LOCALIZED_ME("Click on localize me"),
    RESULT_OK("Return location"),
    CANCEL("Return without location")
}
