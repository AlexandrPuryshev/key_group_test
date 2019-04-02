package com.key_group.gps_finder.geocoder

import android.location.Address
import io.reactivex.Observable

interface IGeocoderInteractorDataSource {
    fun getFromLocation(latitude: Double, longitude: Double): Observable<List<Address>>
}
