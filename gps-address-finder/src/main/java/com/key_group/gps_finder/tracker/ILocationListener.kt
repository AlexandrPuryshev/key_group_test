package com.key_group.gps_finder.tracker

import android.location.Location

/**
 * Created by alex on 15.03.18.
 */

interface ILocationListener {
    fun onCurrentLocation(currentLocation: Location)
}
