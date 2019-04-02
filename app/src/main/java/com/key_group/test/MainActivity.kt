package com.key_group.test

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Address
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.key_group.gps_finder.activity.*
import com.key_group.gps_finder.tracker.ILocationTracker
import com.key_group.gps_finder.tracker.LocationTracker
import com.key_group.gps_finder.tracker.TrackEvents
import kotlinx.android.synthetic.main.activity_main.*

private const val MAP_BUTTON_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeButtons()
        initializeLocationGpsTracker()
    }

    private fun initializeButtons() {
        map_button.setOnClickListener {
            val locationIntentStandard = LocationFinderActivity.Builder()
                    .withGoogleTimeZoneEnabled()
                    .withZoomEnabled()
                    .build(applicationContext)
            locationIntentStandard.putExtra("test", "this is a test")
            startActivityForResult(locationIntentStandard, MAP_BUTTON_REQUEST_CODE)
        }
        map_button_with_style.setOnClickListener {
            val locationIntentWithStyle = LocationFinderActivity.Builder()
                    .withMapStyle(R.raw.map_night_style)
                    .withZoomEnabled()
                    .build(applicationContext)
            startActivityForResult(locationIntentWithStyle, MAP_BUTTON_REQUEST_CODE)
        }
        map_button_with_hide_city_and_zip.setOnClickListener {
            val locationIntentHidden = LocationFinderActivity.Builder()
                    .withCityHidden()
                    .withZipCodeHidden()
                    .shouldReturnOkOnBackPressed()
                    .build(applicationContext)
            startActivityForResult(locationIntentHidden, MAP_BUTTON_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Log.d("Result: ", "ok")
            if (requestCode == MAP_BUTTON_REQUEST_CODE) {
                val latitude = data.getDoubleExtra(LATITUDE, 0.0)
                Log.d("Широта: ", latitude.toString())
                val longitude = data.getDoubleExtra(LONGITUDE, 0.0)
                Log.d("Долгота: ", longitude.toString())
                val address = data.getStringExtra(LOCATION_ADDRESS)
                Log.d("Адрес: ", address.toString())
                val postalcode = data.getStringExtra(ZIPCODE)
                Log.d("Zip-code: ", postalcode.toString())
                val bundle = data.getBundleExtra(TRANSITION_BUNDLE)
                if (bundle.containsKey("test")) {
                    Log.d("Bundle text: ", bundle.getString("test"))
                }
                val fullAddress = data.getParcelableExtra<Address>(ADDRESS)
                if (fullAddress != null) {
                    Log.d("Полный адрес: ", fullAddress.toString())
                }
                val timeZoneId = data.getStringExtra(TIME_ZONE_ID)
                if (timeZoneId != null) {
                    Log.d("id тайм зоны", timeZoneId)
                }
                val timeZoneDisplayName = data.getStringExtra(TIME_ZONE_DISPLAY_NAME)
                if (timeZoneDisplayName != null) {
                    Log.d("Имя тайм зоны", timeZoneDisplayName)
                }
            }
        }
        if (resultCode == Activity.RESULT_CANCELED) {
            Log.d("Result: ", "cancelled")
        }
    }

    private fun initializeLocationGpsTracker() {
        LocationTracker.setTracker(MyGpsTrackerI(this))
    }

    private class MyGpsTrackerI(private val context: Context) : ILocationTracker {
        override fun onEventTracked(event: TrackEvents) {
            Toast.makeText(context, "Event: " + event.eventName, Toast.LENGTH_SHORT).show()
        }
    }
}
