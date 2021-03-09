package com.example.mobicomp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker (appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.d("Tag", inputData.getLong("tag", 0L).toString())
        Log.d("Time", inputData.getLong("time", 0L).toString())
        Log.d("Message", inputData.getString("message"))
        Log.d("Location", inputData.getString("location"))
        Log.d("Icon", inputData.getInt("icon", 0).toString())

        val reminderIcons = applicationContext.resources.obtainTypedArray(R.array.reminder_icons)

        // Create intent to open LoginActivity
        val intent = Intent(applicationContext, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

        // Create notification
        val builder = NotificationCompat.Builder(applicationContext, "mobicomp")
            .setSmallIcon(reminderIcons.getResourceId(inputData.getInt("icon", 0), 0))
            .setContentTitle(inputData.getString("message"))
            .setContentText(inputData.getString("location"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        reminderIcons.recycle()

        // Publish notification
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(inputData.getLong("tag", 0L).toInt(), builder.build())
        }

        return Result.success()
    }

}