package com.example.mobicomp

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.PendingIntent
import android.content.*
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.CalendarContract
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit



class ReminderDialogFragment : DialogFragment() {

    private lateinit var reminderPic : ImageView
    private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geoFencingClient: GeofencingClient
    lateinit var currentPhotoPath : String
    private lateinit var onReminderChangedListener: OnReminderChangedListener
    private var editable = false
    private lateinit var editReminder : MessageActivity.Reminder

    @SuppressLint("MissingPermission")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { it ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)

            // Get layout inflater
            val inflater = requireActivity().layoutInflater;

            // Get GeoFencing client
            geoFencingClient = LocationServices.getGeofencingClient(requireActivity())

            // Set title
            if (editable) {
                builder.setTitle(R.string.edit_reminder)
            } else {
                builder.setTitle(R.string.create_new_reminder)
            }

            // Inflate custom view
            val dialogView : View = inflater.inflate(R.layout.fragment_reminder_dialog, null)

            // Init message view
            val messageText : EditText = dialogView.findViewById(R.id.reminderMessage)
            if (editable) {
                messageText.setText(editReminder.message)
            }

            // Set Date and Time
            val dateText : TextView = dialogView.findViewById(R.id.reminderDate)
            val timeText : TextView = dialogView.findViewById(R.id.reminderTime)
            if (editable) {
                dateText.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                    editReminder.reminder_time
                )
                timeText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(editReminder.reminder_time)
            } else {
                dateText.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                timeText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            }

            // Create button for choosing reminder date
            val dateButton : Button = dialogView.findViewById(R.id.reminderChooseDate)
            dateButton.setOnClickListener {
                val dateBuilder = AlertDialog.Builder(requireContext())
                val datePicker = DatePicker(requireContext())
                val date : List<String> = dateText.text.split("/")
                datePicker.updateDate(date[2].toInt(), date[1].toInt() - 1, date[0].toInt())
                dateBuilder.setTitle("Choose date")
                dateBuilder.setView(datePicker)
                dateBuilder.setNegativeButton(R.string.cancel, null)
                dateBuilder.setPositiveButton(R.string.set) { _, _ ->
                    dateText.text = getString(
                        R.string.date_format,
                        datePicker.dayOfMonth,
                        datePicker.month + 1,
                        datePicker.year
                    )
                }
                dateBuilder.show()
            }

            // Create button for choosing reminder time
            val timeButton : Button = dialogView.findViewById(R.id.reminderChooseTime)
            timeButton.setOnClickListener {
                val timeBuilder = AlertDialog.Builder(requireContext())
                val timePicker = TimePicker(requireContext())
                val time : List<String> = timeText.text.split(":")
                timePicker.hour = time[0].toInt()
                timePicker.minute = time[1].toInt()
                timePicker.setIs24HourView(true)
                timeBuilder.setTitle("Choose time")
                timeBuilder.setView(timePicker)
                timeBuilder.setNegativeButton(R.string.cancel, null)
                timeBuilder.setPositiveButton(R.string.set) { _, _ ->
                    timeText.text = getString(
                        R.string.time_format,
                        timePicker.hour,
                        timePicker.minute
                    )
                }
                timeBuilder.show()
            }

            // Spinner for choosing reminder icon
            val iconSpinner : Spinner = dialogView.findViewById(R.id.reminderIconSpinner)
            iconSpinner.adapter = SpinnerIconAdapter(
                requireContext(),
                resources.getIntArray(R.array.reminder_icons)
            )
            if (editable) {
                iconSpinner.setSelection(editReminder.icon)
            }

            // ImageView to hold reminder photo
            reminderPic = dialogView.findViewById(R.id.reminderPic)
            if (editable && editReminder.pic_path != null) {
                currentPhotoPath = editReminder.pic_path!!
                reminderPic.setImageBitmap(BitmapFactory.decodeFile(editReminder.pic_path))
            }

            // Button for taking a reminder photo
            val picButton : Button = dialogView.findViewById(R.id.reminderTakePic)
            picButton.setOnClickListener {
                dispatchTakePictureIntent()
            }

            // Init location TextView
            val locationText : TextView = dialogView.findViewById(R.id.reminderLocation)

            // Initialise location client
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(
                requireActivity()
            )

            // Emulator
            if (editable) {
                MapsFragment.CurrentLocation.initLocation(
                    editReminder.location_y,
                    editReminder.location_x
                )
            } else {
                // MapsFragment.CurrentLocation.initLocation(65.0464, 25.4317)
                MapsFragment.CurrentLocation.initLocation(MessageActivity.CoronaLocation.lat, MessageActivity.CoronaLocation.long)
            }
            locationText.text = getString(
                R.string.location_format,
                MapsFragment.CurrentLocation.latitude,
                MapsFragment.CurrentLocation.longitude
            )

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                // Real phone, might return null
                if (editable) {
                    MapsFragment.CurrentLocation.initLocation(
                        editReminder.location_y,
                        editReminder.location_x
                    )
                } else if (location != null) {
                    MapsFragment.CurrentLocation.initLocation(
                        location.latitude,
                        location.longitude
                    )
                    locationText.text = getString(
                        R.string.location_format,
                        location.latitude,
                        location.longitude
                    )
                // If location data is not available, use virtual location
                } else {
                    MapsFragment.CurrentLocation.initLocation(MessageActivity.CoronaLocation.lat, MessageActivity.CoronaLocation.long)
                    locationText.text = getString(R.string.location_format, MessageActivity.CoronaLocation.lat, MessageActivity.CoronaLocation.long)
                }
            }

            // Button for opening Google Maps
            val mapButton : Button = dialogView.findViewById(R.id.reminderChooseLocation)
            mapButton.setOnClickListener {
                val mapFragment = MapsFragment()
                // Custom listener for retrieving marker location
                mapFragment.setOnLocationChangedListener(object :
                    MapsFragment.OnLocationChangedListener {
                    override fun onLocationChanged() {
                        locationText.text = getString(
                            R.string.location_format,
                            MapsFragment.CurrentLocation.latitude,
                            MapsFragment.CurrentLocation.longitude
                        )
                    }
                })
                mapFragment.show(parentFragmentManager, "choose_location")
            }

            // Checkbox for choosing if Calendar entry is created
            val calendarCheck : CheckBox = dialogView.findViewById(R.id.reminderCalendar)
            if (editable && editReminder.calendar_event != null) {
                calendarCheck.isChecked = true
            }

            // Checkbox for choosing if notification is created
            val notificationCheck : CheckBox = dialogView.findViewById(R.id.reminderNotification)

            // Set custom view and create/cancel buttons
            builder.setView(dialogView)
                .setPositiveButton(R.string.create) { _, _ ->
                    // User clicked OK button
                    Log.d("Message", messageText.text.toString())
                    Log.d("Date", dateText.text.toString())
                    Log.d("Time", timeText.text.toString())
                    Log.d("Icon", iconSpinner.selectedItemPosition.toString())
                    if (this::currentPhotoPath.isInitialized) {
                        Log.d("Pic", currentPhotoPath)
                    } else {
                        Log.d("Pic", "Not Chosen")
                    }
                    Log.d("Location", locationText.text.toString())
                    Log.d("Calendar", calendarCheck.isChecked.toString())

                    // Turn creation and reminder time in to millis
                    val date : List<String> = dateText.text.split("/")
                    val time : List<String> = timeText.text.split(":")
                    val calendar = Calendar.getInstance()
                    val creationTime = calendar.timeInMillis
                    calendar.set(
                        date[2].toInt(),
                        date[1].toInt() - 1,
                        date[0].toInt(),
                        time[0].toInt(),
                        time[1].toInt()
                    )
                    val reminderTime = calendar.timeInMillis

                    // Get photo path
                    var path : String? = null
                    if (this::currentPhotoPath.isInitialized) {
                        path = currentPhotoPath
                        if (editable) {
                            if (editReminder.pic_path != null && currentPhotoPath != editReminder.pic_path) {
                                File(editReminder.pic_path).delete()
                            }
                        }
                    }

                    // Create calender event and get its ID
                    var eventID : Long? = null
                    if (calendarCheck.isChecked) {
                        if (editable) {
                            eventID = editCalendarEvent(9, editReminder)
                        } else {
                            // 9 is my personal calendar
                            eventID = createCalendarEvent(9, reminderTime, messageText.text.toString(), locationText.text.toString())
                            //createCalendarEvent()
                        }
                        requestCalendarSync()
                    }

                    // Queue notification
                    if (editable) {
                        if (notificationCheck.isChecked) {
                            queueNotification(requireContext(), editReminder.creation_time, reminderTime, messageText.text.toString(),
                                              getString(R.string.location_format2,MapsFragment.CurrentLocation.latitude, MapsFragment.CurrentLocation.longitude), iconSpinner.selectedItemPosition)
                        } else {
                            removeNotification(requireContext(), editReminder.creation_time)
                        }
                    } else if (notificationCheck.isChecked) {
                            queueNotification(requireContext(), creationTime, reminderTime, messageText.text.toString(),
                                              getString(R.string.location_format2,MapsFragment.CurrentLocation.latitude, MapsFragment.CurrentLocation.longitude), iconSpinner.selectedItemPosition)
                    }

                    // Create Geofencing event
                    if (editable) {
                        removeGeofence(requireContext(), editReminder.creation_time.toString())
                        queueGeofence(requireContext(), editReminder.creation_time, reminderTime, messageText.text.toString(),
                                      getString(R.string.location_format2,MapsFragment.CurrentLocation.latitude, MapsFragment.CurrentLocation.longitude), iconSpinner.selectedItemPosition)
                    } else {
                        queueGeofence(requireContext(), creationTime, reminderTime, messageText.text.toString(),
                            getString(R.string.location_format2,MapsFragment.CurrentLocation.latitude, MapsFragment.CurrentLocation.longitude), iconSpinner.selectedItemPosition)
                    }

                    // Store new reminder in to Room Database
                    val db = MessageActivity.ReminderUtils.getDatabase(requireContext())
                    val isSuccess = saveReminderToDB(db!!, messageText.text.toString(), reminderTime, creationTime, iconSpinner.selectedItemPosition, path, eventID)

                    if (isSuccess) {
                        if (editable) {
                            showToast("Reminder edited")
                        } else {
                            showToast("Reminder created")
                        }
                        onReminderChangedListener.onReminderChanged()
                    } else {
                        showToast("Oops something went wrong")
                    }

                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    // User cancelled the dialog
                    if (this::currentPhotoPath.isInitialized) {
                        if (editable) {
                            if (editReminder.pic_path != null && currentPhotoPath != editReminder.pic_path) {
                                File(editReminder.pic_path).delete()
                            }
                        } else {
                            File(currentPhotoPath).delete()
                        }
                        removeNotification(requireContext(), editReminder.creation_time)
                    }
                }

            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun saveReminderToDB(db: MessageActivity.AppDatabase, message: String, reminderTime: Long, creationTime: Long, icon: Int, path: String?, eventID: Long?): Boolean {
        var isSuccess = false
        var isReady = false
        GlobalScope.launch {
            if (editable) {
                db.reminderDAO().updateReminder(
                    editReminder.uid,
                    message,
                    MapsFragment.CurrentLocation.longitude,
                    MapsFragment.CurrentLocation.latitude,
                    reminderTime,
                    icon,
                    path,
                    eventID
                )
            } else {
                db.reminderDAO().insertAll(
                    MessageActivity.Reminder(
                        0,
                        message,
                        MapsFragment.CurrentLocation.longitude,
                        MapsFragment.CurrentLocation.latitude,
                        reminderTime,
                        creationTime,
                        LoginActivity.CurrentUser.getCurrentUser().uid,
                        creationTime,
                        icon,
                        path,
                        eventID
                    )
                )
            }
            isReady = true
            isSuccess = true
        }

        while (!isReady) {
            // Stupidest shit ever to wait for database like this but it works :D
        }

        return isSuccess
    }

    private fun showToast(text: String) {
        Toast.makeText(
            requireContext(),
            text,
            Toast.LENGTH_SHORT
        ).show()
    }

    // Default camera function provided by Google
    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    // Default camera function provided by Google
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(requireContext().packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Toast.makeText(
                        requireContext(),
                        "Unable to save photo",
                        Toast.LENGTH_SHORT
                    ).show()
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    // Get the photo and downscale it to a thumbnail
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            setPic(reminderPic, currentPhotoPath)
        }
    }

    // Default function from Google to downscale an image
    private fun setPic(imageView: ImageView, filePath: String) {
        // Get the dimensions of the View
        val targetW: Int = imageView.width
        val targetH: Int = imageView.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(filePath)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.max(1, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }
        BitmapFactory.decodeFile(filePath, bmOptions)?.also { bitmap ->
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun createCalendarEvent(id : Int, start : Long, title : String, location : String): Long? {
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.CALENDAR_ID, id)
            put(CalendarContract.Events.EVENT_TIMEZONE, "Europe/Helsinki")
            put(CalendarContract.Events.DURATION, "+P1H")
            put(CalendarContract.Events.EVENT_LOCATION, location)
        }
        val contentResolver = requireContext().contentResolver
        val uri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val eventID: Long? = uri?.lastPathSegment?.toLong()
        return eventID
    }

    private fun editCalendarEvent(id: Int, reminder: MessageActivity.Reminder): Long? {
        val eventID: Long = reminder.calendar_event!!
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, reminder.reminder_time)
            put(CalendarContract.Events.TITLE, reminder.message)
            put(CalendarContract.Events.CALENDAR_ID, id)
            put(CalendarContract.Events.EVENT_TIMEZONE, "Europe/Helsinki")
            put(CalendarContract.Events.EVENT_LOCATION, requireContext().getString(R.string.location_format, reminder.location_y, reminder.location_x))
        }
        val updateUri: Uri = ContentUris.withAppendedId(
            CalendarContract.Events.CONTENT_URI,
            eventID
        )
        val contentResolver = requireContext().contentResolver
        contentResolver.update(updateUri, values, null, null)
        return editReminder.calendar_event
    }

    private fun requestCalendarSync() {
        val aM = AccountManager.get(requireContext())
        val accounts = aM.accounts
        for (account in accounts) {
            val isSyncable = ContentResolver.getIsSyncable(account, CalendarContract.AUTHORITY)
            if (isSyncable > 0) {
                val extras = Bundle()
                extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                ContentResolver.requestSync(accounts[0], CalendarContract.AUTHORITY, extras)
            }
        }
    }

    private fun removeNotification(context: Context, tag: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag.toString())
    }

    @SuppressLint("MissingPermission")
    private fun queueGeofence(context: Context, tag : Long, time : Long, message : String, location : String, icon : Int) {
        val geofence = Geofence.Builder()
            .setRequestId(tag.toString())
            .setCircularRegion(MapsFragment.CurrentLocation.latitude, MapsFragment.CurrentLocation.longitude, 200000.0f)
            .setExpirationDuration((10 * 24 * 60 * 60 * 1000).toLong())
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(10 * 1000)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(context, GeofenceReceiver::class.java).apply {
            // Room for putExtra()
            putExtra("tag", tag.toString())
            putExtra("time", time)
            putExtra("message", message)
            putExtra("location", location)
            putExtra("icon", icon)
        }

        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        geoFencingClient.addGeofences(geofenceRequest, pendingIntent)
    }

    companion object {
        fun removeGeofence(context: Context, id: String) {
            LocationServices.getGeofencingClient(context).removeGeofences(mutableListOf(id))
        }

        fun queueNotification(context: Context, tag : Long, time : Long, message : String, location : String, icon : Int) {
            val notificationParams = Data.Builder()
                .putLong("tag", tag)
                .putLong("time", time)
                .putString("message", message)
                .putString("location", location)
                .putInt("icon", icon)
                .build()

            var notificationTime = 0L
            if (time > System.currentTimeMillis())
                notificationTime = time - System.currentTimeMillis()

            val notificationRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(notificationParams)
                .setInitialDelay(notificationTime, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(tag.toString(), ExistingWorkPolicy.REPLACE, notificationRequest)
        }
    }

    fun setEditableReminder(reminder: MessageActivity.Reminder) {
        editable = true
        editReminder = reminder
    }

    fun setOnReminderChangedListener(onReminderChangedListener: OnReminderChangedListener) {
        this.onReminderChangedListener = onReminderChangedListener
    }

    interface OnReminderChangedListener {
        fun onReminderChanged()
    }
}

class SpinnerIconAdapter(val context: Context, private val list: IntArray) : BaseAdapter() {

    private val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(p0: Int): Any {
        return list[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val view = layoutInflater.inflate(R.layout.reminder_icon, p2, false)
        val icon : ImageView = view.findViewById(R.id.icon)
        var reminderIcons = context.resources.obtainTypedArray(R.array.reminder_icons)
        icon.setImageDrawable(
            ContextCompat.getDrawable(
                context, reminderIcons.getResourceId(
                    p0,
                    0
                )
            )
        )
        reminderIcons.recycle()
        return view
    }
}