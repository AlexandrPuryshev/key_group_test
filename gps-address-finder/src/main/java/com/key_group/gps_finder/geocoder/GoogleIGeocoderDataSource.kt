package com.key_group.gps_finder.geocoder

import android.location.Address
import com.key_group.gps_finder.geocoder.api.AddressBuilder
import com.key_group.gps_finder.geocoder.api.NetworkClient
import io.reactivex.Observable
import org.json.JSONException
import java.util.*

private const val QUERY_LAT_LONG = "https://maps.googleapis.com/maps/api/geocode/json?latlng=%1\$f,%2\$f&key=%3\$s"

class GoogleIGeocoderDataSource(
        private val networkClient: NetworkClient,
        private val addressBuilder: AddressBuilder
) : IGeocoderInteractorDataSource {

    private var apiKey: String? = null

    fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    override fun getFromLocation(latitude: Double, longitude: Double): Observable<List<Address>> {
        return Observable.create { subscriber ->
            if (apiKey == null) {
                subscriber.onComplete()
            }
            try {
                val result = networkClient.requestFromLocationName(String.format(Locale.ENGLISH,
                        QUERY_LAT_LONG, latitude, longitude, apiKey))
                if (result != null) {
                    val addresses = addressBuilder.parseResult(result)
                    subscriber.onNext(addresses)
                }
                subscriber.onComplete()
            } catch (e: JSONException) {
                subscriber.onError(e)
            }
        }
    }
}
