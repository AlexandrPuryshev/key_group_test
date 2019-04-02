package com.key_group.gps_finder.geocoder

import android.location.Address
import android.location.Geocoder
import io.reactivex.Observable
import java.io.IOException

private const val MAX_RESULTS = 5

class AndroidIGeocoderDataSource(private val geocoder: Geocoder) : IGeocoderInteractorDataSource {

    override fun getFromLocation(latitude: Double, longitude: Double): Observable<List<Address>> {
        return Observable.create { emitter ->
            try {
                emitter.onNext(geocoder.getFromLocation(latitude, longitude, MAX_RESULTS))
                emitter.onComplete()
            } catch (e: IOException) {
                emitter.tryOnError(e)
            }
        }
    }
}
