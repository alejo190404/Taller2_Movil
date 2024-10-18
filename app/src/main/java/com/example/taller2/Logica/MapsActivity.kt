package com.example.taller2.Logica

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.LocationRequest
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.taller2.Datos.Data
import com.example.taller2.Datos.MyLocation
import com.example.taller2.R
import com.example.taller2.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.Writer
import java.util.Date
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MapsActivity : AppCompatActivity() {
    lateinit var binding: ActivityMapsBinding
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    lateinit var mapController: IMapController
    lateinit var roadManager: RoadManager
    val latitude = 4.627931
    val longitude = -74.0639135
    val startPoint = GeoPoint(latitude, longitude)
    val lastPoint = GeoPoint(latitude, longitude)
    lateinit var location: Location
    private val localizaciones = JSONArray()
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightSensorListener: SensorEventListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapController = binding.osmMap.controller


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mLocationRequest = createLocationRequest()

        Configuration.getInstance().setUserAgentValue(applicationContext.packageName)

        binding.osmMap.setTileSource(TileSourceFactory.MAPNIK)
        binding.osmMap.setMultiTouchControls(true)

        binding.osmMap.overlays.add(createOverlayEvents())

        roadManager = OSRMRoadManager(this, "ANDROID")
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        mLocationCallback = object  : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                location = locationResult.lastLocation!!
                if (location != null) {
                    Log.i("MAPaS", distance(location.latitude, location.longitude, lastPoint.latitude, lastPoint.longitude).toString())
                    if (distance(location.latitude, location.longitude, lastPoint.latitude, lastPoint.longitude) >= 30){
                        createPointUpdate(location.latitude, location.longitude, "")
                        lastPoint.latitude = location.latitude
                        lastPoint.longitude = location.longitude
                        writeJSONObject(location)
                    }
                    else {

                    }
                }
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        lightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values[0] < 100) {
                    binding.osmMap.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)

                } else {
                    binding.osmMap.overlayManager.tilesOverlay.setColorFilter(null)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        val mGeocoder = Geocoder(this)

        binding.text.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND){
                val addressString = (v as EditText).text.toString()

                try {
                    val addresses = mGeocoder.getFromLocationName(addressString, 2)
                    if (!addresses.isNullOrEmpty()) {
                        val addressResult = addresses[0]
                        val position = GeoPoint(addressResult.latitude, addressResult.longitude)
                        if (binding.osmMap != null) {
                            binding.osmMap.overlays.add(createMarker(position, geoCoderSearchLatLang(position), null, R.drawable.baseline_adjust_24))
                            drawRoute(GeoPoint(location), position)
                        }
                    } else {
                        Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                // Return true to indicate the action was handled
                true
            } else {
                // Return false to let other listeners handle the event if needed
                false
            }
        }

        pedirPermiso(this, Manifest.permission.ACCESS_FINE_LOCATION, "Se necesita para que la aplicacion funcione", Data.MY_PERMISSION_REQUEST_FINE_LOCATION)

    }

    private var roadOverlay: Polyline? = null

    private fun drawRoute(start: GeoPoint, finish: GeoPoint) {
        val routePoints = ArrayList<GeoPoint>()
        routePoints.add(start)
        routePoints.add(finish)
        createPointUpdate(finish.latitude, finish.longitude, "")
        val road = roadManager.getRoad(routePoints)
        Log.i("OSM_acticity", "Route length: ${road.mLength} klm")
        Toast.makeText(this, "Distancia de la ruta: ${"%.2f".format(road.mLength)} km", Toast.LENGTH_SHORT).show()
        Log.i("OSM_acticity", "Duration: ${road.mDuration / 60} min")
        if (binding.osmMap != null) {
            roadOverlay?.let { binding.osmMap.overlays.remove(it) }
            roadOverlay = RoadManager.buildRoadOverlay(road)
            roadOverlay?.outlinePaint?.color = Color.RED
            roadOverlay?.outlinePaint?.strokeWidth = 10f
            binding.osmMap.overlays.add(roadOverlay)
            // Calculate midpoint
            val midLat = (start.latitude + finish.latitude) / 2
            val midLon = (start.longitude + finish.longitude) / 2
            val midPoint = GeoPoint(midLat, midLon)

            // Center the map on the midpoint
            binding.osmMap.controller.setCenter(midPoint)

            // Adjust the zoom level based on the distance
            val distance = road.mLength
            val zoomLevel = calculateZoomLevel(distance)
            binding.osmMap.controller.setZoom(zoomLevel)
        }
    }

    private fun calculateZoomLevel(distance: Double): Double {
        return when {
            distance < 0.1 -> 20.0  // Very close, street-level view
            distance < 0.5 -> 19.0  // Short distance, close-up view
            distance < 1 -> 18.0    // Less than 1 km, still close
            distance < 2 -> 17.0    // Around 1-2 km, neighborhood level
            distance < 5 -> 16.0    // 2-5 km, city-level view
            distance < 10 -> 15.0   // 5-10 km, see a larger area of the city
            distance < 20 -> 14.0   // 10-20 km, overview of a city area
            distance < 30 -> 13.0   // 20-30 km, multiple city neighborhoods
            distance < 50 -> 12.0   // 30-50 km, city and outskirts
            distance < 75 -> 11.0   // 50-75 km, zoomed out city region
            distance < 100 -> 10.0  // 75-100 km, see larger regions
            distance < 150 -> 9.0   // 100-150 km, inter-city connections
            distance < 250 -> 8.0   // 150-250 km, larger city-to-city view
            distance < 500 -> 7.0   // 250-500 km, covering multiple cities
            distance < 1000 -> 6.0  // 500-1000 km, a view covering part of a country
            distance < 2000 -> 5.0  // 1000-2000 km, country-level
            distance < 4000 -> 4.0  // 2000-4000 km, regional level
            distance < 8000 -> 3.0  // 4000-8000 km, continent-level
            else -> 2.0             // Very far distance, global view
        }
    }


    private fun createOverlayEvents(): MapEventsOverlay {
        val overlayEventos = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                return false
            }
            override fun longPressHelper(p: GeoPoint): Boolean {
                longPressOnMap(p)
                return true
            }
        })
        return overlayEventos
    }

    private var longPressedMarker: Marker? = null
    private fun longPressOnMap(p: GeoPoint) {
        longPressedMarker = null
        longPressedMarker = createMarker(p, geoCoderSearchLatLang(p), null, R.drawable.baseline_adjust_24)
        longPressedMarker?.let { binding.osmMap.overlays.add(it) }
        drawRoute(GeoPoint(location), p)
    }

    private fun geoCoderSearchLatLang(latLng: GeoPoint): String? {
        val mGeocoder = Geocoder(this)
        return mGeocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)?.get(0)?.getAddressLine(0)
    }
    private fun createMarker(p: GeoPoint, title: String?, desc: String?, iconID: Int): Marker? {
        var marker: Marker? = null
        if (binding.osmMap != null) {
            marker = Marker(binding.osmMap)
            title?.let { marker.title = it }
            desc?.let { marker.subDescription = it }
            if (iconID != 0) {
                val myIcon = resources.getDrawable(iconID, this.theme)
                marker.icon = myIcon
            }
            marker.position = p
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        return marker
    }

    fun createPointSearch(lat: Double, lon: Double, title: String){
//        binding.osmMap.overlays.removeAt(binding.osmMap.overlays.size - 1)
        val markerPoint = GeoPoint(lat, lon)
        val marker = Marker(binding.osmMap)
        marker.title = title
        val myIcon = resources.getDrawable(R.drawable.baseline_adjust_24, theme)
        marker.icon = myIcon
        marker.position = markerPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        binding.osmMap.overlays.add(marker)
        mapController.setCenter(markerPoint)
        mapController.setZoom(18.0)
    }

    fun createPointUpdate(lat: Double, lon: Double, title: String){
//        binding.osmMap.overlays.removeAt(binding.osmMap.overlays.size - 1)
        val markerPoint = GeoPoint(lat, lon)
        val marker = Marker(binding.osmMap)
        marker.title = title
        val myIcon = resources.getDrawable(R.drawable.baseline_adjust_24, theme)
        marker.icon = myIcon
        marker.position = markerPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        binding.osmMap.overlays.add(marker)
        mapController.setCenter(markerPoint)
        mapController.setZoom(18.0)
    }

    private fun pedirPermiso(context: Activity, permiso: String, justificacion: String, idCode: Int) {

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ){

            ActivityCompat.requestPermissions(context, arrayOf(permiso), idCode)

        }
        else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            startLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.osmMap.onResume()
        val mapController: IMapController = binding.osmMap.controller
        mapController.setCenter(this.startPoint)
        mapController.setZoom(18.0)
        sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    override fun onPause() {
        super.onPause()
        binding.osmMap.onPause()
        sensorManager.unregisterListener(lightSensorListener)
    }

    var previousLocation: GeoPoint = GeoPoint(0, 0)
    private fun startLocationUpdates() {
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            &&
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)){
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            mapController.setCenter(this.startPoint)
            mapController.setZoom(18.0)
        }
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

    private fun createLocationRequest(): LocationRequest {
        val locationRequest = com.google.android.gms.location.LocationRequest.create()
            .setInterval(5000)
            .setFastestInterval(5000)
            .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)

        return locationRequest
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode){
            Data.MY_PERMISSION_REQUEST_FINE_LOCATION -> {
                //If request is cancelled, the result arrrays are empty
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    startLocationUpdates()

                }
                return
            }
            else -> {
                binding.textoError.text="FUNCIONALIDADES LIMITADAS"
            }
        }
    }

    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = (Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2))
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        val RADIUS_OF_EARTH_KM = 6371.0
        val resultInKilometers = RADIUS_OF_EARTH_KM * c

        // Convert kilometers to meters
        return (resultInKilometers * 1000 * 100.0).roundToInt() / 100.0
    }

    private fun writeJSONObject(location: Location) {
        localizaciones.put(MyLocation(Date(System.currentTimeMillis()), location.latitude, location.longitude).toJSON())
        var output: Writer?
        val filename = "locations.json"
        try {
            val file = File(baseContext.getExternalFilesDir(null), filename)
            Log.i("Ubicaciondelarchivo", "${file}")
            output = BufferedWriter(FileWriter(file))
            output.write(localizaciones.toString())
            output.close()
            Toast.makeText(applicationContext, "Location saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
//Log error
        }
    }



}