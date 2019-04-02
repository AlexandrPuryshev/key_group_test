package com.key_group.gps_finder.tracker

interface ILocationTracker {
    fun onEventTracked(event: TrackEvents)
}
