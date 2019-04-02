package com.key_group.gps_finder.geocoder

import android.location.Address
import android.location.Location

import com.google.android.gms.maps.model.LatLng
import java.util.TimeZone

interface IGeocoderView {
    fun showLoadLocationError()
    fun showLastLocation(location: Location)
    fun didGetLastLocation()
    fun showLocationInfo(address: Pair<Address, TimeZone?>)
    fun willGetLocationInfo(latLng: LatLng)
    fun didGetLocationInfo()
    fun showGetLocationInfoError()

    class NullViewI : IGeocoderView {
        override fun showLoadLocationError() {}
        override fun showLastLocation(location: Location) {}
        override fun didGetLastLocation() {}
        override fun showLocationInfo(address: Pair<Address, TimeZone?>) {}
        override fun willGetLocationInfo(latLng: LatLng) {}
        override fun didGetLocationInfo() {}
        override fun showGetLocationInfoError() {}
    }
}
