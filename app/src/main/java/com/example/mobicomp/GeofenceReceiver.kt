package com.example.mobicomp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("Triggered", "Triggered")
        if (context != null) {
            val geoEvent = GeofencingEvent.fromIntent(intent!!)

            // Retrieve data from intent
            val tag = intent.getStringExtra("tag")
            val time = intent.getLongExtra("time", 0)
            val message = intent.getStringExtra("message")
            val location = intent.getStringExtra("location")
            val icon = intent.getIntExtra("icon", 0)
            Log.d("Tag", tag!!)
            Log.d("Time", time.toString())
            Log.d("Message", message!!)
            Log.d("Location", location!!)
            Log.d("Icon", icon.toString())

            if (geoEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geoEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                // Remove geofence and show notification
                if (time < System.currentTimeMillis()) {
                    ReminderDialogFragment.removeGeofence(context, tag)
                    ReminderDialogFragment.queueNotification(context, tag.toLong(), time, message, location, icon)
                }
            } else if (time < System.currentTimeMillis()) {
                    ReminderDialogFragment.queueNotification(context, tag.toLong(), time, message, location, icon)
            }
        }
    }
}