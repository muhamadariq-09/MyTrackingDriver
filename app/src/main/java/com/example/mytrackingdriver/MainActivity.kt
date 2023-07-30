package com.example.mytrackingdriver

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.mytrackingdriver.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private var dbReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("Users")
    private lateinit var mMap: GoogleMap
    private var isTracking = false
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val geofenceRadius = 500.0
    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        checkUser()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        dbReference = Firebase.database.reference
        dbReference.addValueEventListener(locListener)


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.custom_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.profile -> {
                startActivity(Intent(this@MainActivity, AccountDetailActivity::class.java))
                return true
            }

            R.id.logOut -> {
                auth.signOut()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        getMyLocation()
        createLocationRequest()
        createLocationCallback()

        binding.buttonsatu.setOnClickListener {
            if (!isTracking) {
                updateTrackingStatus(true)
                startLocationUpdates()
            } else {
                updateTrackingStatus(false)
                stopLocationUpdates()
            }
        }
    }

    private val requestBackgroundLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getMyLocation()
            }
        }

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q


    @TargetApi(Build.VERSION_CODES.Q)
    private val requestLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (runningQOrLater) {
                    requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    getMyLocation()
                }
            }
        }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun checkForegroundAndBackgroundLocationPermission(): Boolean {
        val foregroundLocationApproved = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }


    @SuppressLint("MissingPermission")
    private val locListener = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists()){
                val rootRef = FirebaseDatabase.getInstance().reference
                val usersRef = rootRef.child("Users")
                usersRef.get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        for (userSnapshot in task.result!!.children) {
                            val lat = userSnapshot.child("userlocation/latitude")
                                .getValue(Double::class.java)
                            val lng = userSnapshot.child("userlocation/longitude")
                                .getValue(Double::class.java)

                            if (lat != null && lng != null) {

                                val latLng = LatLng(lat, lng)
                                mMap.addMarker(
                                    MarkerOptions().position(latLng)
                                        .title("The user is currently here")
                                )
                                val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                                //update the camera with the CameraUpdate object
                                mMap.moveCamera(update)
                                mMap.addCircle(
                                    CircleOptions()
                                        .center(latLng)
                                        .radius(geofenceRadius)
                                        .fillColor(0x22FF0000)
                                        .strokeColor(Color.RED)
                                        .strokeWidth(3f)
                                )


                                geofencingClient = LocationServices.getGeofencingClient(applicationContext)

                                val geofence = Geofence.Builder()
                                    .setRequestId("Users")
                                    .setCircularRegion(
                                        lat,
                                        lng,
                                        geofenceRadius.toFloat()
                                    )
                                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_ENTER)
                                    .setLoiteringDelay(1000)
                                    .build()

                                val geofencingRequest = GeofencingRequest.Builder()
                                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                                    .addGeofence(geofence)
                                    .build()

                                geofencingClient.removeGeofences(geofencePendingIntent).run {
                                    addOnCompleteListener {
                                        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                                            addOnSuccessListener {
                                                Toast.makeText(this@MainActivity, "Geofencing added", Toast.LENGTH_SHORT).show()
                                            }
                                            addOnFailureListener {
                                                Toast.makeText(this@MainActivity, "Geofencing not added : ${it.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }


                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "No Location Found",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    }
                }


            }

            else {
                // if location is null , log an error message
                Log.e(TAG, "user location cannot be found")
                Toast.makeText(this@MainActivity, "user location cannot be found", Toast.LENGTH_SHORT).show()
            }


        }
        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(applicationContext, "Could not read from database", Toast.LENGTH_LONG).show()
        }

    }




    @SuppressLint("MissingPermission")
    private fun getMyLocation() {
        if (checkForegroundAndBackgroundLocationPermission()) {
            fusedLocationClient.lastLocation.addOnCompleteListener {
                val location = it.result //obtain location

                val user = FirebaseAuth.getInstance().currentUser
                val databaseRef: DatabaseReference = Firebase.database.reference
                val locationlogging = Location(location.latitude, location.longitude)
                databaseRef.child("Driver").child(user!!.uid).child("location").setValue(locationlogging)
                if (location != null) {

                    mMap.isMyLocationEnabled = true
                } else {
                    Toast.makeText(this, "No Location Found", Toast.LENGTH_SHORT).show()
                }


            }

        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }



    private val resolutionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            when (result.resultCode) {
                RESULT_OK ->
                    Log.i(TAG, "onActivityResult: All location settings are satisfied.")
                RESULT_CANCELED ->
                    Toast.makeText(
                        this,
                        "Anda harus mengaktifkan GPS untuk menggunakan aplikasi ini!",
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(1)
            maxWaitTime = TimeUnit.SECONDS.toMillis(1)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        client.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                getMyLocation()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        resolutionLauncher.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Toast.makeText(this, sendEx.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation
                for (location in locationResult.locations) {
                    Log.d(TAG, "onLocationResult: " + location.latitude + ", " + location.longitude)


                }
            }
        }
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (exception: SecurityException) {
            Log.e(TAG, "Error : " + exception.message)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (isTracking) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun updateTrackingStatus(newStatus: Boolean) {
        isTracking = newStatus
        if (isTracking) {
            binding.buttonsatu.text = getString(R.string.find_passenger)
        } else {
            binding.buttonsatu.text = getString(R.string.stop_find)
        }
    }





    private fun checkUser() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }





    companion object {
        private const val TAG = "MainActivity"
    }


}