package com.example.mobicomp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create default user
        val db = DatabaseUtils.getDatabase(this)
        val users = db?.userDao()
        createDefaultUser(users!!)

        // Ask user for necessary permissions
        askPermissions()

        // Set onClickListeners for buttons
        val logInButton : Button = findViewById(R.id.logIn)
        logInButton.setOnClickListener {
            val rememberMe = getUserRememberMe(users)
            sendNewIntent(rememberMe)
        }

        val signUpButton : Button = findViewById(R.id.signUp)
        signUpButton.setOnClickListener {
            // Change to sign up activity
            val intent = Intent(this, SignupActivity::class.java).apply {
                // Room for putExtra()
            }
            startActivity(intent)
        }
    }

    private fun createDefaultUser(users: UserDao) {
        var isReady = false
        GlobalScope.launch {
            if (users.findByUsername("root") == null) {
                users.insertAll(User(0, "root", "root", SignupActivity.hashPassword("root"), 0, 0))
            }
            isReady = true
        }
        while (!isReady) {
            // Stupidest shit ever to wait for database like this but it works :D
        }
    }

    private fun askPermissions() {
        // I could chain all these together but it is hard to read, so I'm keeping them separate
        // I don't care if the user has to press allow multiple times
        // Ask Location permissions
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 123)
        }
        // Ask Location permissions
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 123)
        }
        // Ask Location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 12345
                )
            }
        }
        // Ask Calendar permissions
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR), 1)
        }
        // Ask Calendar permissions
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_CALENDAR), 1)
        }
    }

    private fun getUserRememberMe(users: UserDao): Boolean {
        // Find if user has set RememberMe
        var isReady = false
        var foundUser = false
        GlobalScope.launch {
            val user = users.findRememberMe(1)
            if (user != null) {
                LoginActivity.CurrentUser.initUser(user)
                foundUser = true
            }
            isReady = true
        }

        while (!isReady) {
            // Stupidest shit ever to wait for database like this but it works :D
        }
        return foundUser
    }

    private fun sendNewIntent(foundUser: Boolean) {
        lateinit var intent: Intent
        if (foundUser) {
            // If user has set RememberMe, jump straight to MessageActivity
            intent = Intent(this, MessageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.apply {
                // Room for putExtra()
            }
        } else {
            // Change to login activity
            intent = Intent(this, LoginActivity::class.java).apply {
                // Room for putExtra()
            }
        }
        startActivity(intent)
    }

    @Entity
    data class User(
        @PrimaryKey(autoGenerate = true) val uid: Int,
        @ColumnInfo(name = "username") val username: String,
        @ColumnInfo(name = "email") val email: String,
        @ColumnInfo(name = "password") val password: String,
        @ColumnInfo(name = "pic_id") val picId: Int,
        @ColumnInfo(name = "remember_me") val rememberMe: Int
    )

    @Dao
    interface UserDao {
        @Query("SELECT * FROM user")
        fun getAll(): List<User>

        @Query("SELECT * FROM user WHERE uid IN (:userIds)")
        fun loadAllByIds(userIds: IntArray): List<User>

        @Query("SELECT * FROM user WHERE uid LIKE :uid LIMIT 1")
        fun findByUid(uid: Int): User

        @Query("SELECT * FROM user WHERE username LIKE :username LIMIT 1")
        fun findByUsername(username: String): User

        @Query("SELECT * FROM user WHERE email LIKE :email LIMIT 1")
        fun findByEmail(email: String): User

        @Query("SELECT * FROM user WHERE remember_me LIKE :rememberMe LIMIT 1")
        fun findRememberMe(rememberMe: Int): User

        @Insert
        fun insertAll(vararg users: User)

        @Query("UPDATE User SET username = :username, email = :email, password = :password, pic_id = :picId, remember_me = :rememberMe WHERE uid = :uid")
        fun updateUser(uid: Int, username: String, email: String, password: String, picId: Int, rememberMe: Int): Int

        @Delete
        fun delete(user: User)
    }

    @Database(entities = arrayOf(User::class), version = 1)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun userDao(): UserDao
    }

    // No idea if this is the best method, but the interface isn't very good
    // and I don't have time to spend figuring it out
    object DatabaseUtils {
        private var db : AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase? {
            if (db == null) {
                db = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java, "User-Database"
                ).build()
            }
            return db
        }
    }
}
