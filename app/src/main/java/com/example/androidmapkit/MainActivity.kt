package com.example.androidmapkit

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.androidmapkit.databinding.ActivityMainBinding
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.LocationManager
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener


class MainActivity : AppCompatActivity() {

    private val REQUEST_LOCATION_PERMISSION = 1
    private val entryPoint = Point(54.71, 20.51)     //Калининград
    private val entryZoom = 14F

    private lateinit var binding: ActivityMainBinding

    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject

    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setApiKey(savedInstanceState)
        MapKitFactory.initialize(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val traffic = MapKitFactory.getInstance().createTrafficLayer(binding.mapview.mapWindow)
        binding.showTrafficBTN.setOnClickListener {
            if (traffic.isTrafficVisible) {
                traffic.isTrafficVisible = false
                showToast("Отображение трафика отключено")
            } else {
                traffic.isTrafficVisible = true
                showToast("Отображение трафика включено")
            }
        }

        locationManager = MapKitFactory.getInstance().createLocationManager()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            getLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        fusedLocationClient.lastLocation
            .addOnCompleteListener(this, OnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val taskResult = task.result
                    val myLocation = Point(taskResult.latitude, taskResult.longitude)
                    resumeMainProgram(myLocation)
                } else {
                    showToast("Разрешение точного местоположения не предоставлено")
                }
            })
    }

    private fun resumeMainProgram(location: Point) {
        setMarker(location)
        moveToPosition(location)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            } else {
                resumeMainProgram(entryPoint)
            }
        }
    }

    private fun setApiKey(savedInstanceState: Bundle?) {
        val haveApiKey = savedInstanceState?.getBoolean("haveApiKey") == true
        if (!haveApiKey) MapKitFactory.setApiKey(Utils.MAPKIT_API_KEY)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("haveApiKey", true)
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapview.onStart()
    }

    override fun onStop() {
        binding.mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    private fun moveToPosition(position: Point) {
        binding.mapview.map.move(
            CameraPosition(
                position,
                entryZoom, 0F, 0F
            ),
            Animation(Animation.Type.SMOOTH, 5f),
            null
        )
    }

    private fun setMarker(coordinates: Point) {
        val markerIcon = android.R.drawable.ic_menu_mylocation
        mapObjectCollection = binding.mapview.map.mapObjects
        placemarkMapObject = mapObjectCollection.addPlacemark(
            coordinates,
            ImageProvider.fromResource(this, markerIcon)
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}