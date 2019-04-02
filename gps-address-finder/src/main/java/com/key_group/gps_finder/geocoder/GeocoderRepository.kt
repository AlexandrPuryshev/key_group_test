package com.key_group.gps_finder.geocoder

import android.location.Address
import com.google.android.gms.maps.model.LatLng
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

private const val RETRY_COUNT = 3

class GeocoderRepository(
        private val androidIGeocoder: IGeocoderInteractorDataSource,
        private val googleIGeocoder: IGeocoderInteractorDataSource
) {
    fun getFromLocation(latLng: LatLng): Observable<List<Address>> {
        return androidIGeocoder
                .getFromLocation(latLng.latitude, latLng.longitude)
                .subscribeOn(Schedulers.newThread())
                .retry(RETRY_COUNT.toLong())
                .onErrorResumeNext(googleIGeocoder.getFromLocation(latLng.latitude, latLng.longitude))
    }
}
