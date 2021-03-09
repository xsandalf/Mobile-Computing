package com.example.mobicomp

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.room.*
import androidx.room.Entity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MessageActivity : AppCompatActivity() {

    lateinit var listView : ListView
    lateinit var showAll : CheckBox
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Get reminders from Database
        val db = ReminderUtils.getDatabase(this)
        var isReady = false
        var reminders : List<Reminder> = getReminders(db!!)

        listView = findViewById(R.id.message_list)
        this.registerForContextMenu(listView)
        listView.adapter = ReminderAdapter(this,
            reminders.toMutableList() as ArrayList<Reminder>
        )

        showAll = findViewById(R.id.showAll)
        showAll.setOnCheckedChangeListener { _,b ->
            (listView.adapter as ReminderAdapter).isChecked = b
            (listView.adapter as ReminderAdapter).notifyDataSetChanged()
        }

        // Initialise location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            // Real phone, might return null
            if (location != null) {
                CoronaLocation.lat = location.latitude
                CoronaLocation.long = location.longitude
                (listView.adapter as ReminderAdapter).notifyDataSetChanged()
            }
        }

        // Set onClickListeners for buttons
        val myProfileButton: Button = findViewById(R.id.my_profile)
        myProfileButton.setOnClickListener {
            // Change to login activity
            val intent = Intent(this, ProfileActivity::class.java).apply {
                // Room for putExtra()
            }
            startActivity(intent)
        }

        val logOutButton: Button = findViewById(R.id.log_out)
        logOutButton.setOnClickListener {
            // Change to sign up activity
            val intent = Intent(this, MainActivity::class.java).apply {
                // Room for putExtra()
            }
            isReady = false
            val users = MainActivity.DatabaseUtils.getDatabase(this)!!.userDao()
            GlobalScope.launch {
                val user = LoginActivity.CurrentUser.getCurrentUser()
                users.updateUser(user.uid, user.username, user.email, user.password, user.picId, 0)
                isReady = true
            }
            while (!isReady) {
                // Stupidest shit ever to wait for database like this but it works :D
            }
            startActivity(intent)
        }

        val setLocationButton: Button = findViewById(R.id.setLocation)
        setLocationButton.setOnClickListener {
            MapsFragment.CurrentLocation.initLocation(CoronaLocation.lat, CoronaLocation.long)
            val mapFragment = MapsFragment()
            // Custom listener for retrieving marker location
            mapFragment.setOnLocationChangedListener(object :
                MapsFragment.OnLocationChangedListener {
                override fun onLocationChanged() {
                    CoronaLocation.lat = MapsFragment.CurrentLocation.latitude
                    CoronaLocation.long = MapsFragment.CurrentLocation.longitude
                    (listView.adapter as ReminderAdapter).notifyDataSetChanged()
                }
            })
            mapFragment.show(supportFragmentManager, "corona_location")
        }

        // Create AlertDialog for adding a new reminder
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val dialogFragment = ReminderDialogFragment()
            dialogFragment.setOnReminderChangedListener(object :
                ReminderDialogFragment.OnReminderChangedListener {
                override fun onReminderChanged() {
                    // Get reminders from db
                    reminders = getReminders(db!!)

                    // Refresh changed reminders to ListView Adapter
                    refreshListView(reminders!!)
                }
            })
            dialogFragment.show(supportFragmentManager, "create_reminder")
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info : AdapterView.AdapterContextMenuInfo = (menuInfo as AdapterView.AdapterContextMenuInfo)

        //menu.setHeaderTitle(selectedWord);
        v?.id?.let { menu?.add(0, it, 0, R.string.edit) }
        v?.id?.let { menu?.add(0, it, 1, R.string.delete) }
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            val info : AdapterView.AdapterContextMenuInfo = (item.menuInfo as AdapterView.AdapterContextMenuInfo)
            val reminder : Reminder= listView.adapter.getItem(info.position) as Reminder
            lateinit var reminders : List<Reminder>
            val db = ReminderUtils.getDatabase(this)
            if (item.title == getString(R.string.edit)) {
                // Edit reminder
                val dialogFragment = ReminderDialogFragment()
                dialogFragment.setEditableReminder(reminder)
                dialogFragment.setOnReminderChangedListener(object :
                    ReminderDialogFragment.OnReminderChangedListener {
                    override fun onReminderChanged() {
                        // Get reminders from Database
                        reminders = getReminders(db!!)

                        // Refresh changed reminders to ListView Adapter
                        refreshListView(reminders)
                    }
                })
                dialogFragment.show(supportFragmentManager, "edit_reminder")
            } else if (item.title == getString(R.string.delete)) {
                // Delete image used for the reminder
                if (reminder.pic_path != null) {
                    File(reminder.pic_path).delete()
                }
                // Delete reminder
                reminders = deleteAndRefreshReminders(db!!, reminder)

                // Refresh changed reminders to ListView Adapter
                refreshListView(reminders)
            }
        } else {
            return false
        }
        return true
    }

    private fun getReminders(db: AppDatabase): List<Reminder> {
        var isReady = false
        lateinit var reminders: List<Reminder>
        // Get reminders from Database
        GlobalScope.launch {
            reminders = db?.reminderDAO()?.getAll()!!
            isReady = true
        }

        while (!isReady) {
            // Stupidest shit ever to wait for database like this but it works :D
        }

        return reminders
    }

    private fun deleteAndRefreshReminders(db: AppDatabase, reminder: Reminder): List<Reminder> {
        var isSuccess = false
        var isReady = false
        lateinit var reminders: List<Reminder>
        GlobalScope.launch {
            db.reminderDAO().delete(reminder)
            reminders = db.reminderDAO().getAll()
            isReady = true
            isSuccess = true
        }

        while (!isReady) {
            // Stupidest shit ever to wait for database like this but it works :D
        }

        if (isSuccess) {
            Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Oops something went wrong", Toast.LENGTH_SHORT).show()
        }
        return reminders
    }

    private fun refreshListView(reminders: List<Reminder>) {
        val list : ArrayList<Reminder> = (listView.adapter as ReminderAdapter).getItems()
        list.clear()
        list.addAll(reminders)
        (listView.adapter as ReminderAdapter).notifyDataSetChanged()
    }

    class ReminderAdapter(val context: Context, private val list: ArrayList<Reminder>) : BaseAdapter() {

        private val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        private val currentUser : MainActivity.User = LoginActivity.CurrentUser.getCurrentUser()
        private val chosenPfp = currentUser.picId
        var isChecked = false

        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(p0: Int): Any {
            return list[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        fun getItems() : ArrayList<Reminder> {
            return list
        }

        // Tries to imitate triggering a Geofence
        private fun virtualGeofence(context: Context, reminder: Reminder) {
            if (reminder.reminder_time < System.currentTimeMillis()) {
                val intent = Intent(context, GeofenceReceiver::class.java).apply {
                    // Room for putExtra()
                    putExtra("tag", reminder.creation_time.toString())
                    putExtra("time", reminder.reminder_time)
                    putExtra("message", reminder.message)
                    putExtra(
                        "location",
                        context.getString(
                            R.string.location_format2,
                            reminder.location_y,
                            reminder.location_x
                        )
                    )
                    putExtra("icon", reminder.icon)
                }

                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT).send()
            }
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            // Init views and set values from the reminder
            val reminder = list[p0]

            var results = FloatArray(5)
            Location.distanceBetween(CoronaLocation.lat, CoronaLocation.long, reminder.location_y, reminder.location_x, results)

            if (!isChecked && (reminder.reminder_time > System.currentTimeMillis() || results[0] > 200)) {
                return View(context)
            } else if (reminder.reminder_time > System.currentTimeMillis() || results[0] < 200) {
                virtualGeofence(context, reminder)
            }
            val view = layoutInflater.inflate(R.layout.reminder_item, p2, false)
            val icon: ImageView = view.findViewById(R.id.icon)
            val pic: ImageView = view.findViewById(R.id.pic)
            val message: TextView = view.findViewById(R.id.message)
            val creationTime: TextView = view.findViewById(R.id.creation)
            val reminderTime: TextView = view.findViewById(R.id.reminder)
            val location: TextView = view.findViewById(R.id.location)
            val icons = context.resources.obtainTypedArray(R.array.reminder_icons)
            icon.setImageDrawable(
                ContextCompat.getDrawable(context, icons.getResourceId(reminder.icon, 0))
            )
            icons.recycle()
            // If picture is not available, user profile picture instead
            if (reminder.pic_path != null && reminder.pic_path != "null") {
                //setPic(pic, reminder.pic_path)
                pic.setImageBitmap(BitmapFactory.decodeFile(reminder.pic_path))
            } else {
                val profilePics = context.resources.obtainTypedArray(R.array.profile_pics)
                pic.setImageDrawable(ContextCompat.getDrawable(context, profilePics.getResourceId(chosenPfp, 0)))
                profilePics.recycle()
            }
            message.text = reminder.message
            creationTime.text = context.getString(R.string.creation_format, SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(reminder.creation_time)))
            reminderTime.text = context.getString(R.string.reminder_format, SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(reminder.reminder_time)))
            location.text = context.getString(R.string.location_format2, reminder.location_y, reminder.location_x)

            return view
        }
    }

    @Entity
    data class Reminder(
        @PrimaryKey(autoGenerate = true) val uid: Int,
        @ColumnInfo(name = "message") val message: String,
        @ColumnInfo(name = "location_x") val location_x: Double,
        @ColumnInfo(name = "location_y") val location_y: Double,
        @ColumnInfo(name = "reminder_time") val reminder_time: Long,
        @ColumnInfo(name = "creation_time") val creation_time: Long,
        @ColumnInfo(name = "creator_id") val creator_id: Int,
        @ColumnInfo(name = "reminder_seen") val reminder_seen: Long,
        @ColumnInfo(name = "icon") val icon: Int,
        @ColumnInfo(name = "pic_path") val pic_path: String?,
        @ColumnInfo(name = "calendar_event") val calendar_event: Long?
    )

    @Dao
    interface ReminderDAO {
        @Query("SELECT * FROM reminder")
        fun getAll(): List<Reminder>

        @Query("SELECT * FROM reminder WHERE uid LIKE :uid LIMIT 1")
        fun findByUid(uid: Int): Reminder

        @Insert
        fun insertAll(vararg reminders: Reminder)

        @Query("UPDATE Reminder SET message = :message, location_x = :location_x, location_y = :location_y, reminder_time = :reminder_time, icon = :icon, pic_path = :pic_path, calendar_event = :calendar_event WHERE uid = :uid")
        fun updateReminder(
            uid: Int,
            message: String,
            location_x: Double,
            location_y: Double,
            reminder_time: Long,
            icon: Int,
            pic_path: String?,
            calendar_event: Long?
        ): Int

        @Delete
        fun delete(reminder: Reminder)
    }

    @Database(entities = arrayOf(Reminder::class), version = 1)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun reminderDAO(): ReminderDAO
    }

    // No idea if this is the best method, but the interface isn't very good
    // and I don't have time to spend figuring it out
    object ReminderUtils {
        private var db: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase? {
            if (db == null) {
                db = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java, "Reminder-Database"
                ).build()
            }
            return db
        }
    }

    object CoronaLocation {
        var lat: Double = 65.0464
        var long: Double = 25.4317
    }
}