package com.key_group.gps_finder.geocoder

import android.annotation.SuppressLint
import android.location.Address
import com.google.android.gms.maps.model.LatLng
import com.key_group.gps_finder.geocoder.timezone.GoogleTimeZoneDataSource
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider
import java.util.*

private const val RETRY_COUNT = 3

class GeocoderPresenter @JvmOverloads constructor(
        private val locationProvider: ReactiveLocationProvider,
        private val geocoderRepository: GeocoderRepository,
        private val googleTimeZoneDataSource: GoogleTimeZoneDataSource? = null,
        private val scheduler: Scheduler = AndroidSchedulers.mainThread()
) {

    private var viewI: IGeocoderView? = null
    private val nullView = IGeocoderView.NullViewI()
    private val compositeDisposable = CompositeDisposable()

    init {
        this.viewI = nullView
    }

    fun setUI(IGeocoderView: IGeocoderView) {
        this.viewI = IGeocoderView
    }

    fun stop() {
        this.viewI = nullView
        compositeDisposable.clear()
    }

    fun getLastKnownLocation() {
        @SuppressLint("MissingPermission")
        val disposable = locationProvider.lastKnownLocation
                .retry(RETRY_COUNT.toLong())
                .subscribe({ viewI!!.showLastLocation(it) },
                        { },
                        { viewI!!.didGetLastLocation() }
                )
        compositeDisposable.add(disposable)
    }

    fun getInfoFromLocation(latLng: LatLng) {
        viewI!!.willGetLocationInfo(latLng)
        val disposable = geocoderRepository.getFromLocation(latLng)
                .observeOn(scheduler)
                .retry(RETRY_COUNT.toLong())
                .filter { addresses -> !addresses.isEmpty() }
                .map { addresses -> addresses[0] }
                .flatMap { address -> returnTimeZone(address) }
                .subscribe({ pair: Pair<Address, TimeZone?> -> viewI!!.showLocationInfo(pair) },
                        { viewI!!.showGetLocationInfoError() },
                        { viewI!!.didGetLocationInfo() })
        compositeDisposable.add(disposable)
    }

    private fun returnTimeZone(address: Address): ObservableSource<out Pair<Address, TimeZone?>>? {
        return Observable.just(
                Pair(address, googleTimeZoneDataSource?.getTimeZone(address.latitude, address.longitude))
        ).onErrorReturn { Pair(address, null) }
    }
}
