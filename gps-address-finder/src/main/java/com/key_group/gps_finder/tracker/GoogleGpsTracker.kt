package com.key_group.gps_finder.tracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.key_group.gps_finder.R
import com.key_group.gps_finder.permissions.PermissionUtils

class GoogleGpsTracker(private var context: Context, private var fusedLocationProviderClient: FusedLocationProviderClient) : LocationCallback() {

    private lateinit var locationListener: ILocationListener
    private var updateInterval: Long = 3000  /* 3 secs */
    private var isRequestingLocations = false
    private var locationRequest: LocationRequest? = null

    fun setUpdateInterval(updateInterval: Long) {
        this.updateInterval = updateInterval
    }

    fun disconnect() {
        if (isRequestingLocations) {
            fusedLocationProviderClient.removeLocationUpdates(this)
            isRequestingLocations = false
        }
    }

    private fun checkPermission(): Boolean {
        if (!PermissionUtils.isLocationPermissionGranted(context)) {
            Toast.makeText(context, context.getString(R.string.toast_error_permission_location), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    fun setLocationListener(locationListener: ILocationListener) {
        this.locationListener = locationListener
    }

    private fun createLocationRequest() = LocationRequest().apply {
        this.interval = updateInterval
        this.fastestInterval = updateInterval
        this.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        this.smallestDisplacement = 50f
    }

    fun startLocationUpdates() {
        if (!isRequestingLocations) {
            locationRequest = createLocationRequest()
            val builder = LocationSettingsRequest.Builder()
            val locationSettingsRequest = builder
                    .addLocationRequest(locationRequest!!)
                    .build()

            val settingsClient = LocationServices.getSettingsClient(context)
            settingsClient.checkLocationSettings(locationSettingsRequest)

            if (checkPermission()) {
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, this,  Looper.myLooper())
            }
        }
    }


    override fun onLocationResult(result: LocationResult?) {
        val location = result!!.lastLocation
        locationListener.onCurrentLocation(location)
    }


}