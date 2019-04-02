package com.key_group.gps_finder.tracker

object LocationTracker {

    private val EMPTY_TRACKER = EmptyILocationTracker()

    private var trackerI: ILocationTracker = EMPTY_TRACKER

    fun setTracker(trackerI: ILocationTracker) {
        LocationTracker.trackerI = trackerI
    }

    fun getTracker(): ILocationTracker {
        return trackerI
    }

    fun reset() {
        trackerI = EMPTY_TRACKER
    }

    class EmptyILocationTracker : ILocationTracker {
        override fun onEventTracked(event: TrackEvents) { }
    }
}
