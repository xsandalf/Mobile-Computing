package com.example.mobicomp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.room.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set onClickListeners for buttons
        val logInButton : Button = findViewById(R.id.logIn)
        logInButton.setOnClickListener {
            // Change to login activity
            val intent = Intent(this, LoginActivity::class.java).apply {
                // Room for putExtra()
            }
            startActivity(intent)
        }

        val signUpButton : Button = findViewById(R.id.signUp)
        signUpButton.setOnClickListener {
            // Change to sign up activity
            val intent = Intent(this, SignupActivity::class.java).apply {
                // Room for putExtra()
            }
            startActivity(intent)
        }

        // Create default user
        var isReady = false
        val db = DatabaseUtils.getDatabase(this)
        val users = db?.userDao()
        if (users != null) {
            GlobalScope.launch {
                if (users.findByUsername("root") == null) {
                    users.insertAll(User(0, "root", "root", "root", 0))
                }
                isReady = true
            }
        }
        while (isReady) {
            // Stupidest shit ever to wait for database like this but it works :D
        }
    }

    @Entity
    data class User(
        @PrimaryKey(autoGenerate = true) val uid: Int,
        @ColumnInfo(name = "username") val username: String,
        @ColumnInfo(name = "email") val email: String,
        @ColumnInfo(name = "password") val password: String,
        @ColumnInfo(name = "pic_id") val picId: Int
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

        @Insert
        fun insertAll(vararg users: User)

        @Query("UPDATE User SET username = :username, email = :email, password = :password, pic_id = :picId WHERE uid = :uid")
        fun updateUser(uid: Int, username: String, email: String, password: String, picId: Int): Int

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
        var db : AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase? {
            if (db == null) {
                db = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java, "database-name"
                ).build()
            }
            return db
        }
    }
}
