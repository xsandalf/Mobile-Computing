package com.example.mobicomp

import android.app.Dialog
import android.graphics.Color

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.properties.Delegates

class MapsFragment : DialogFragment() {

    private lateinit var mapView: View
    private lateinit var onLocationChangedListener: OnLocationChangedListener

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        val latLng = LatLng(CurrentLocation.latitude, CurrentLocation.longitude)
        var marker = googleMap.addMarker(MarkerOptions().position(latLng).title("Current location"))
        var circle = googleMap.addCircle(CircleOptions().center(latLng).strokeColor(Color.argb(50, 70, 70, 70)).fillColor(Color.argb(70, 150, 150, 150)).radius(200.0)
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        googleMap.setOnMapLongClickListener {
            marker.remove()
            marker = googleMap.addMarker(
                MarkerOptions().position(it).title(getString(R.string.chosen_location))
            )
            CurrentLocation.setLocation(it.latitude, it.longitude)
            circle.remove()
            circle = googleMap.addCircle(
                CircleOptions()
                    .center(it)
                    .strokeColor(Color.argb(50, 70, 70, 70))
                    .fillColor(Color.argb(70, 150, 150, 150))
                    .radius(200.0)
            )
        }
    }

    fun setOnLocationChangedListener(onLocationChangedListener: OnLocationChangedListener) {
        this.onLocationChangedListener = onLocationChangedListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { it ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)

            // Get layout inflater
            val inflater = requireActivity().layoutInflater

            // Set title
            builder.setTitle(R.string.reminder_location)

            // Self-inflate layout
            mapView = inflater.inflate(R.layout.fragment_maps, null)

            // Set custom view and create/cancel buttons
            builder.setView(mapView)
                .setPositiveButton(R.string.set) { _, _ ->
                    onLocationChangedListener.onLocationChanged()
                }
                .setNegativeButton(R.string.cancel){ _, _ ->
                    CurrentLocation.resetLocation()
                }

            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Return self-inflated view to make sure onViewCreated is called
        return mapView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = parentFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove mapFragment to prevent inflation exception
        val mapFragment = parentFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        if (mapFragment != null) {
            parentFragmentManager.beginTransaction().remove(mapFragment).commit()
        }
    }

    object CurrentLocation {
        var latitude by Delegates.notNull<Double>()
        var longitude by Delegates.notNull<Double>()
        private var locLat by Delegates.notNull<Double>()
        private var locLong by Delegates.notNull<Double>()
        fun initLocation(lat: Double, long: Double) {
            latitude = lat
            longitude = long
            locLat = lat
            locLong = long
        }
        fun setLocation(lat: Double, long: Double) {
            latitude = lat
            longitude = long
        }
        fun resetLocation() {
            latitude = locLat
            longitude = locLong
        }
    }

    interface OnLocationChangedListener {
        fun onLocationChanged()
    }
}