package com.key_group.gps_finder.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.GeoApiContext
import com.key_group.gps_finder.R
import com.key_group.gps_finder.geocoder.*
import com.key_group.gps_finder.geocoder.api.AddressBuilder
import com.key_group.gps_finder.geocoder.api.NetworkClient
import com.key_group.gps_finder.geocoder.timezone.GoogleTimeZoneDataSource
import com.key_group.gps_finder.permissions.PermissionUtils
import com.key_group.gps_finder.tracker.GoogleGpsTracker
import com.key_group.gps_finder.tracker.ILocationListener
import com.key_group.gps_finder.tracker.LocationTracker
import com.key_group.gps_finder.tracker.TrackEvents
import kotlinx.android.synthetic.main.activity_location_find.*
import kotlinx.android.synthetic.main.layout_address.*
import kotlinx.android.synthetic.main.layout_coordinates.*
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider
import java.util.*

const val LATITUDE = "latitude"
const val LONGITUDE = "longitude"
const val ZIPCODE = "zipcode"
const val ADDRESS = "address"
const val LOCATION_ADDRESS = "location_address"
const val UPDATED_INTERVAL = "updated_interval"
const val TRANSITION_BUNDLE = "transition_bundle"
const val LAYOUTS_TO_HIDE = "layouts_to_hide"
const val BACK_PRESSED_RETURN_OK = "back_pressed_return_ok"
const val ENABLE_LOCATION_PERMISSION_REQUEST = "enable_location_permission_request"
const val ENABLE_GOOGLE_TIME_ZONE = "enable_google_time_zone"
const val ENABLE_ZOOM = "enable_zoom"
const val TIME_ZONE_ID = "time_zone_id"
const val TIME_ZONE_DISPLAY_NAME = "time_zone_display_name"
const val MAP_STYLE = "map_style"
private const val GEOLOC_API_KEY = "geoloc_api_key"
private const val LOCATION_KEY = "location_key"
private const val OPTIONS_HIDE_STREET = "street"
private const val OPTIONS_HIDE_CITY = "city"
private const val OPTIONS_HIDE_ZIPCODE = "zipcode"
private const val CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000
private const val DEFAULT_ZOOM: Float = 16F
private const val WIDER_ZOOM: Float = 6F

class LocationFinderActivity : AppCompatActivity(),
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        IGeocoderView {

    private var map: GoogleMap? = null
    private var googleApiClient: GoogleApiClient? = null
    private var mCurrentLocation: Location? = null
    private var geocoderPresenter: GeocoderPresenter? = null

    private var hasWiderZoom = false
    private val bundle = Bundle()
    private var selectedAddress: Address? = null
    private var isLocationInformedFromBundle = false
    private var isStreetVisible = true
    private var isCityVisible = true
    private var isZipCodeVisible = true
    private var shouldReturnOkOnBackPressed = false
    private var isZoomEnabled = false
    private var enableLocationPermissionRequest = true
    private var isGoogleTimeZoneEnabled = true
    private var googleGpsTracker: GoogleGpsTracker? = null
    private var apiInteractor: GoogleIGeocoderDataSource? = null
    private var mapStyle: Int? = null
    private var defaultZoom: Float = 0F
    private var updatedInterval: Long = 0L
    private lateinit var timeZone: TimeZone


    private val locationAddress: String
        get() {
            var locationAddress = ""
            if (street != null && !street!!.text.toString().isEmpty()) {
                locationAddress = street!!.text.toString()
            }
            if (city != null && !city!!.text.toString().isEmpty()) {
                if (!locationAddress.isEmpty()) {
                    locationAddress += ", "
                }
                locationAddress += city!!.text.toString()
            }
            return locationAddress
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_find)
        setUpMainVariables()
        updateValuesFromBundle(savedInstanceState)
        checkLocationPermission()
        setUpFloatingButtons()
        buildGoogleApiClient()
    }

    private fun findUserLocation() {
        checkLocationPermission()
        googleGpsTracker!!.setLocationListener(object : ILocationListener {
            override fun onCurrentLocation(currentLocation: Location) {
                mCurrentLocation = currentLocation
                setCurrentPositionLocation()
                track(TrackEvents.ON_LOAD_LOCATION)
            }
        })
        googleGpsTracker!!.startLocationUpdates()
    }

    private fun checkLocationPermission() {
        if (enableLocationPermissionRequest && PermissionUtils.shouldRequestLocationStoragePermission(applicationContext)) {
            PermissionUtils.requestLocationPermission(this)
        }
    }

    private fun isLocationPermited(): Boolean {
        return PermissionUtils.isLocationPermissionGranted(this)
    }

    private fun track(event: TrackEvents) {
        LocationTracker.getTracker().onEventTracked(event)
    }

    private fun setUpMainVariables() {
        val geocoder = Geocoder(this, Locale.getDefault())
        apiInteractor = GoogleIGeocoderDataSource(NetworkClient(), AddressBuilder())
        googleGpsTracker = GoogleGpsTracker(applicationContext, LocationServices.getFusedLocationProviderClient(this))
        val geocoderRepository = GeocoderRepository(AndroidIGeocoderDataSource(geocoder), apiInteractor!!)
        val timeZoneDataSource = GoogleTimeZoneDataSource(GeoApiContext.Builder().apiKey(GoogleTimeZoneDataSource.getApiKey(this)).build())
        geocoderPresenter = GeocoderPresenter(ReactiveLocationProvider(applicationContext), geocoderRepository, timeZoneDataSource)
        geocoderPresenter!!.setUI(this)
        defaultZoom = if (hasWiderZoom) WIDER_ZOOM else DEFAULT_ZOOM
    }

    private fun setUpFloatingButtons() {
        val btnMyLocation = findViewById<FloatingActionButton>(R.id.float_button_gps_find)
        val btnZoomIn = findViewById<FloatingActionButton>(R.id.float_button_zoom_in)
        val btnZoomOut = findViewById<FloatingActionButton>(R.id.float_button_zoom_out)
        btnMyLocation.setOnClickListener {
            checkLocationPermission()
            geocoderPresenter!!.getLastKnownLocation()
            track(TrackEvents.ON_LOCALIZED_ME)
        }
        if (isZoomEnabled) {
            btnZoomIn.visibility = View.VISIBLE
            btnZoomOut.visibility = View.VISIBLE
            btnZoomIn.setOnClickListener {
                zoomInCamera()
            }
            btnZoomOut.setOnClickListener {
                zoomOutCamera()
            }
        } else {
            btnZoomIn.visibility = View.GONE
            btnZoomOut.visibility = View.GONE
        }
        val btnAcceptLocation = findViewById<FloatingActionButton>(R.id.btnAccept)
        btnAcceptLocation.setOnClickListener { returnCurrentPosition() }
    }

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        val transitionBundle = intent.extras
        if (transitionBundle != null) {
            getTransitionBundleParams(transitionBundle)
        }
        if (savedInstanceState != null) {
            getSavedInstanceParams(savedInstanceState)
        }
        updateAddressLayoutVisibility()
        if (updatedInterval != 0L) {
            googleGpsTracker!!.setUpdateInterval(updatedInterval)
        }
    }

    private fun setUpMapIfNeeded() {
        if (map == null) {
            (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (PermissionUtils.isLocationPermissionGranted(applicationContext)) {
            geocoderPresenter!!.getLastKnownLocation()
            setDefaultMapSettings()
        }
    }


    override fun onStart() {
        super.onStart()
        googleApiClient!!.connect()
        geocoderPresenter!!.setUI(this)
    }

    override fun onStop() {
        if (googleApiClient!!.isConnected) {
            googleApiClient!!.disconnect()
        }
        geocoderPresenter!!.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        setUpMapIfNeeded()
    }

    override fun onDestroy() {
        if (googleApiClient != null) {
            googleApiClient!!.unregisterConnectionCallbacks(this)
        }
        googleGpsTracker!!.disconnect()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (!shouldReturnOkOnBackPressed || isLocationInformedFromBundle) {
            setResult(Activity.RESULT_CANCELED)
            track(TrackEvents.CANCEL)
            finish()
        } else {
            returnCurrentPosition()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        if (map == null) {
            map = googleMap
            setMapStyle()
            findUserLocation()
            setDefaultMapSettings()
        }
    }

    override fun onConnected(savedBundle: Bundle?) {
        if (mCurrentLocation == null) {
            geocoderPresenter!!.getLastKnownLocation()
        }
    }

    override fun onConnectionSuspended(i: Int) {
        googleApiClient!!.connect()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST)
            } catch (e: IntentSender.SendIntentException) {
                track(TrackEvents.GOOGLE_API_CONNECTION_FAILED)
            }
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        if (mCurrentLocation != null) {
            savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation)
        }
        if (bundle.containsKey(TRANSITION_BUNDLE)) {
            savedInstanceState.putBundle(TRANSITION_BUNDLE, bundle.getBundle(TRANSITION_BUNDLE))
        }
        savedInstanceState.putBoolean(ENABLE_LOCATION_PERMISSION_REQUEST, enableLocationPermissionRequest)
        super.onSaveInstanceState(savedInstanceState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY)
        if (mCurrentLocation != null) {
            setCurrentPositionLocation()
        }
        if (savedInstanceState.containsKey(TRANSITION_BUNDLE)) {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState.getBundle(TRANSITION_BUNDLE))
        }
        if (savedInstanceState.containsKey(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = savedInstanceState.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST)
        }
    }

    private fun setNewPosition(latLng: LatLng) {
        if (mCurrentLocation == null) {
            mCurrentLocation = Location(getString(R.string.network_resource))
        }
        mCurrentLocation!!.latitude = latLng.latitude
        mCurrentLocation!!.longitude = latLng.longitude
        setCurrentPositionLocation()
    }

    private fun changeLocationInfoLayoutVisibility(visibility: Int) {
        location_info!!.visibility = visibility
    }

    private fun showCoordinatesLayout() {
        longitude!!.visibility = View.VISIBLE
        latitude!!.visibility = View.VISIBLE
        coordinates!!.visibility = View.VISIBLE
        street!!.visibility = View.GONE
        city!!.visibility = View.GONE
        zipCode!!.visibility = View.GONE
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    private fun showAddressLayout() {
        longitude!!.visibility = View.GONE
        latitude!!.visibility = View.GONE
        coordinates!!.visibility = View.GONE
        street!!.visibility = if (isStreetVisible) View.VISIBLE else View.INVISIBLE
        city!!.visibility = if (isCityVisible) View.VISIBLE else View.INVISIBLE
        zipCode!!.visibility = if (isZipCodeVisible) View.VISIBLE else View.INVISIBLE
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    private fun updateAddressLayoutVisibility() {
        street!!.visibility = if (isStreetVisible) View.VISIBLE else View.INVISIBLE
        city!!.visibility = if (isCityVisible) View.VISIBLE else View.INVISIBLE
        zipCode!!.visibility = if (isZipCodeVisible) View.VISIBLE else View.INVISIBLE
        longitude!!.visibility = View.VISIBLE
        latitude!!.visibility = View.VISIBLE
        coordinates!!.visibility = View.VISIBLE
    }

    override fun showLoadLocationError() {
        Toast.makeText(this, R.string.toast_load_location_error, Toast.LENGTH_LONG).show()
    }

    override fun willGetLocationInfo(latLng: LatLng) {
        changeLocationInfoLayoutVisibility(View.VISIBLE)
        resetLocationAddress()
        setCoordinatesInfo(latLng)
    }

    override fun showLastLocation(location: Location) {
        mCurrentLocation = location
    }

    override fun didGetLastLocation() {
        if (mCurrentLocation != null) {
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show()
                return
            }
            setUpMapIfNeeded()
        }
        setUpDefaultMapLocation()
    }

    override fun showLocationInfo(address: Pair<Address, TimeZone?>) {
        selectedAddress = address.first
        if (address.second != null) {
            timeZone = address.second!!
        }
        setLocationInfo(selectedAddress!!)
    }

    private fun setLocationEmpty() {
        this.street!!.text = ""
        this.city!!.text = ""
        this.zipCode!!.text = ""
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    override fun didGetLocationInfo() {
        showLocationInfoLayout()
    }

    override fun showGetLocationInfoError() {
        setLocationEmpty()
    }

    private fun showLocationInfoLayout() {
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    private fun getSavedInstanceParams(savedInstanceState: Bundle) {
        if (savedInstanceState.containsKey(TRANSITION_BUNDLE)) {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState.getBundle(TRANSITION_BUNDLE))
        } else {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState)
        }
        if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
            mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY)
        }
        setUpDefaultMapLocation()
        if (savedInstanceState.keySet().contains(LAYOUTS_TO_HIDE)) {
            setLayoutVisibilityFromBundle(savedInstanceState)
        }
        if (savedInstanceState.keySet().contains(GEOLOC_API_KEY)) {
            apiInteractor!!.setApiKey(savedInstanceState.getString(GEOLOC_API_KEY, ""))
        }
        if (savedInstanceState.keySet().contains(ENABLE_GOOGLE_TIME_ZONE)) {
            isGoogleTimeZoneEnabled = savedInstanceState.getBoolean(ENABLE_GOOGLE_TIME_ZONE, false)
        }
        if (savedInstanceState.keySet().contains(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = savedInstanceState.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST)
        }
        if (savedInstanceState.keySet().contains(ENABLE_ZOOM)) {
            isZoomEnabled = savedInstanceState.getBoolean(ENABLE_ZOOM)
        }
        if (savedInstanceState.keySet().contains(UPDATED_INTERVAL)) {
            updatedInterval = savedInstanceState.getLong(UPDATED_INTERVAL)
        }
        if (savedInstanceState.keySet().contains(MAP_STYLE)) {
            mapStyle = savedInstanceState.getInt(MAP_STYLE)
        }
    }

    private fun getTransitionBundleParams(transitionBundle: Bundle) {
        bundle.putBundle(TRANSITION_BUNDLE, transitionBundle)
        if (transitionBundle.keySet().contains(LATITUDE) && transitionBundle.keySet()
                        .contains(LONGITUDE)) {
            setLocationFromBundle(transitionBundle)
        }
        if (transitionBundle.keySet().contains(LAYOUTS_TO_HIDE)) {
            setLayoutVisibilityFromBundle(transitionBundle)
        }
        if (transitionBundle.keySet().contains(BACK_PRESSED_RETURN_OK)) {
            shouldReturnOkOnBackPressed = transitionBundle.getBoolean(BACK_PRESSED_RETURN_OK)
        }
        if (transitionBundle.keySet().contains(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = transitionBundle.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST)
        }
        if (transitionBundle.keySet().contains(GEOLOC_API_KEY)) {
            apiInteractor!!.setApiKey(transitionBundle.getString(GEOLOC_API_KEY, ""))
        }
        if (transitionBundle.keySet().contains(ENABLE_GOOGLE_TIME_ZONE)) {
            isGoogleTimeZoneEnabled = transitionBundle.getBoolean(ENABLE_GOOGLE_TIME_ZONE, false)
        }
        if (transitionBundle.keySet().contains(ENABLE_ZOOM)) {
            isZoomEnabled = transitionBundle.getBoolean(ENABLE_ZOOM)
        }
        if (transitionBundle.keySet().contains(UPDATED_INTERVAL)) {
            updatedInterval = transitionBundle.getLong(UPDATED_INTERVAL)
        }
        if (transitionBundle.keySet().contains(MAP_STYLE)) {
            mapStyle = transitionBundle.getInt(MAP_STYLE)
        }
    }

    private fun setLayoutVisibilityFromBundle(transitionBundle: Bundle) {
        val options = transitionBundle.getString(LAYOUTS_TO_HIDE)
        if (options != null && options.contains(OPTIONS_HIDE_STREET)) {
            isStreetVisible = false
        }
        if (options != null && options.contains(OPTIONS_HIDE_CITY)) {
            isCityVisible = false
        }
        if (options != null && options.contains(OPTIONS_HIDE_ZIPCODE)) {
            isZipCodeVisible = false
        }
    }

    private fun setLocationFromBundle(transitionBundle: Bundle) {
        if (mCurrentLocation == null) {
            mCurrentLocation = Location(getString(R.string.network_resource))
        }
        mCurrentLocation!!.latitude = transitionBundle.getDouble(LATITUDE)
        mCurrentLocation!!.longitude = transitionBundle.getDouble(LONGITUDE)
        setCurrentPositionLocation()
        isLocationInformedFromBundle = true
    }

    private fun setCoordinatesInfo(latLng: LatLng) {
        this.latitude!!.text = String.format("%s: %s", getString(R.string.latitude), latLng.latitude)
        this.longitude!!.text = String.format("%s: %s", getString(R.string.longitude), latLng.longitude)
        showCoordinatesLayout()
    }

    private fun resetLocationAddress() {
        street?.text = ""
        city?.text = ""
        zipCode?.text = ""
    }

    private fun setLocationInfo(address: Address) {
        street!!.text = getString(R.string.address_view_address) + " " + address.getAddressLine(0)
        city!!.text = getString(R.string.address_view_city) + " " + if (isStreetEqualsCity(address)) "" else address.locality
        zipCode!!.text = getString(R.string.address_view_zip_code) + " " + address.postalCode
        showAddressLayout()
    }

    private fun isStreetEqualsCity(address: Address): Boolean {
        return address.getAddressLine(0) == address.locality
    }

    private fun returnCurrentPosition() {
        when {
            mCurrentLocation != null -> {
                val returnIntent = Intent()
                returnIntent.putExtra(LATITUDE, mCurrentLocation!!.latitude)
                returnIntent.putExtra(LONGITUDE, mCurrentLocation!!.longitude)
                if (street != null && city != null) {
                    returnIntent.putExtra(LOCATION_ADDRESS, locationAddress)
                }
                if (zipCode != null) {
                    returnIntent.putExtra(ZIPCODE, zipCode!!.text)
                }
                returnIntent.putExtra(ADDRESS, selectedAddress)
                if (isGoogleTimeZoneEnabled && ::timeZone.isInitialized) {
                    returnIntent.putExtra(TIME_ZONE_ID, timeZone.id)
                    returnIntent.putExtra(TIME_ZONE_DISPLAY_NAME, timeZone.displayName)
                }
                returnIntent.putExtra(TRANSITION_BUNDLE, bundle.getBundle(TRANSITION_BUNDLE))
                setResult(Activity.RESULT_OK, returnIntent)
                track(TrackEvents.RESULT_OK)
            }
            else -> {
                setResult(Activity.RESULT_CANCELED)
                track(TrackEvents.CANCEL)
            }
        }
        finish()
    }


    private fun setMapStyle() {
        map?.let { googleMap ->
            mapStyle?.let { style ->
                val loadStyle = MapStyleOptions.loadRawResourceStyle(this, style)
                googleMap.setMapStyle(loadStyle)
            }
        }
    }

    private fun setDefaultMapSettings() {
        if (map != null) {
            map!!.mapType = MAP_TYPE_NORMAL
            map!!.uiSettings.isCompassEnabled = false
            map!!.uiSettings.isMyLocationButtonEnabled = false
            map!!.uiSettings.isMapToolbarEnabled = true
            map!!.uiSettings.isZoomControlsEnabled = false
            if (isLocationPermited()) {
                map!!.isMyLocationEnabled = true
            }
        }
    }

    private fun setUpDefaultMapLocation() {
        if (mCurrentLocation != null) {
            setCurrentPositionLocation()
        } else {
            hasWiderZoom = true
        }
    }

    private fun setCurrentPositionLocation() {
        if (mCurrentLocation != null) {
            val latLng = LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude)
            geocoderPresenter!!.getInfoFromLocation(latLng)
            centerCamera(latLng)
        }
    }

    private fun animateCamera(latLng: LatLng, zoom: Float) {
        val cameraPosition = CameraPosition.Builder().target(latLng).tilt(60F).zoom(zoom).build()
        hasWiderZoom = false
        map!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun centerCamera(latLng: LatLng) {
        if (map != null) {
            animateCamera(latLng, defaultZoom)
        }
    }

    private fun zoomInCamera() {
        if (map != null) {
            defaultZoom = map!!.cameraPosition.zoom + 1
            val latLng = LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude)
            animateCamera(latLng, defaultZoom)
        }
    }

    private fun zoomOutCamera() {
        if (map != null) {
            defaultZoom = map!!.cameraPosition.zoom - 1
            val latLng = LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude)
            animateCamera(latLng, defaultZoom)
        }
    }

    @Synchronized
    private fun buildGoogleApiClient() {
        val googleApiClientBuilder = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)

        googleApiClient = googleApiClientBuilder.build()
        googleApiClient!!.connect()
    }

    class Builder {
        private var locationLatitude: Double? = null
        private var locationLongitude: Double? = null
        private var layoutsToHide = ""
        private var shouldReturnOkOnBackPressed = false
        private var isZoomEnabled = false
        private var geolocApiKey: String? = null
        private var googleTimeZoneEnabled = false
        private var updatedGpsInterval: Long? = null
        private var mapStyle: Int? = null

        fun withFakeLocation(latitude: Double, longitude: Double): Builder {
            this.locationLatitude = latitude
            this.locationLongitude = longitude
            return this
        }

        fun shouldReturnOkOnBackPressed(): Builder {
            this.shouldReturnOkOnBackPressed = true
            return this
        }

        fun withStreetHidden(): Builder {
            this.layoutsToHide = String.format("%s|%s", layoutsToHide, OPTIONS_HIDE_STREET)
            return this
        }

        fun withZoomEnabled(): Builder {
            this.isZoomEnabled = true
            return this
        }

        fun withCityHidden(): Builder {
            this.layoutsToHide = String.format("%s|%s", layoutsToHide, OPTIONS_HIDE_CITY)
            return this
        }

        fun withZipCodeHidden(): Builder {
            this.layoutsToHide = String.format("%s|%s", layoutsToHide, OPTIONS_HIDE_ZIPCODE)
            return this
        }

        fun withUpdatedGpsInterval(milliseconds: Long): Builder {
            this.updatedGpsInterval = milliseconds
            return this
        }

        fun withGeolocApiKey(apiKey: String): Builder {
            this.geolocApiKey = apiKey
            return this
        }

        fun withGoogleTimeZoneEnabled(): Builder {
            this.googleTimeZoneEnabled = true
            return this
        }

        fun withMapStyle(@RawRes mapStyle: Int): Builder {
            this.mapStyle = mapStyle
            return this
        }

        fun build(context: Context): Intent {
            val intent = Intent(context, LocationFinderActivity::class.java)

            if (locationLatitude != null) {
                intent.putExtra(LATITUDE, locationLatitude!!)
            }
            if (locationLongitude != null) {
                intent.putExtra(LONGITUDE, locationLongitude!!)
            }
            if (updatedGpsInterval != null) {
                intent.putExtra(UPDATED_INTERVAL, updatedGpsInterval!!)
            }
            if (!layoutsToHide.isEmpty()) {
                intent.putExtra(LAYOUTS_TO_HIDE, layoutsToHide)
            }
            intent.putExtra(BACK_PRESSED_RETURN_OK, shouldReturnOkOnBackPressed)
            intent.putExtra(ENABLE_ZOOM, isZoomEnabled)
            if (geolocApiKey != null) {
                intent.putExtra(GEOLOC_API_KEY, geolocApiKey)
            }
            mapStyle?.let { style -> intent.putExtra(MAP_STYLE, style) }
            intent.putExtra(ENABLE_GOOGLE_TIME_ZONE, googleTimeZoneEnabled)
            return intent
        }
    }
}
