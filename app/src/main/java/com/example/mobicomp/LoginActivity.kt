package com.example.mobicomp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // initiate database
        val db = MainActivity.DatabaseUtils.getDatabase(this)

        // Initiate views
        val loginName : EditText = findViewById(R.id.loginName)
        val loginPassword : EditText = findViewById(R.id.loginPassword)
        // Implement functionality later
        val rememberMe : CheckBox = findViewById(R.id.rememberMe)

        // Get intent
        val intent : Bundle? = intent.extras
        if (intent != null) {
            // Need to work on my Kotlin types
            val userName : String? = intent.getString("Username")
            // Automatically set username
            loginName.setText(userName)
        }

        // Set onClickListener for button
        val logInButton : Button = findViewById(R.id.logIn)
        logInButton.setOnClickListener {
            // Change to login activity
            // Check if login name or password field is empty, not the best way to do this but good enough
            if (loginName.text.toString().equals("") || loginPassword.text.toString().equals("")) {
                Toast.makeText(
                    this,
                    "Please give both username/email and password",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Check from Database for valid login
                if (db != null) {
                    val users = db.userDao()
                    var userName : MainActivity.User? = null
                    var email : MainActivity.User? = null
                    var isReady = false
                    GlobalScope.launch {
                        userName = users.findByUsername(loginName.text.toString())
                        email = users.findByEmail(loginName.text.toString())
                        isReady = true
                    }
                    while (!isReady) {
                        // Stupidest shit ever to wait for database like this but it works :D
                    }
                    // null for invalid login, stupid but works
                    // Clean up later
                    if (userName != null) {
                        if (userName!!.password == SignupActivity.hashPassword(loginPassword.text.toString())) {
                            CurrentUser.initUser(userName!!)
                            val intent = Intent(this, MessageActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            intent.apply {
                                // Room for putExtra()
                            }
                            if (rememberMe.isChecked) {
                                isReady = false
                                GlobalScope.launch {
                                    val user = users.findRememberMe(1)
                                    if (user != null) {
                                        users.updateUser(user.uid, user.username, user.email, user.password, user.picId, 0)
                                    }
                                    users.updateUser(userName!!.uid, userName!!.username, userName!!.email, userName!!.password, userName!!.picId, 1)
                                    isReady = true
                                }
                                while (!isReady) {
                                    // Stupidest shit ever to wait for database like this but it works :D
                                }
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                this,
                                "Wrong login, try again1",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else if (email != null) {
                        if (email!!.password == SignupActivity.hashPassword(loginPassword.text.toString())) {
                            CurrentUser.initUser(email!!)
                            val intent = Intent(this, MessageActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            intent.apply {
                                // Room for putExtra()
                            }
                            if (rememberMe.isChecked) {
                                isReady = false
                                GlobalScope.launch {
                                    val user = users.findRememberMe(1)
                                    if (user != null) {
                                        users.updateUser(user.uid, user.username, user.email, user.password, user.picId, 0)
                                    }
                                    users.updateUser(email!!.uid, email!!.username, email!!.email, email!!.password, email!!.picId, 1)
                                    isReady = true
                                }
                                while (!isReady) {
                                    // Stupidest shit ever to wait for database like this but it works :D
                                }
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                this,
                                "Wrong login, try again2",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Wrong login, try again3",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    object CurrentUser {
        private lateinit var currentUser : MainActivity.User
        fun initUser(user : MainActivity.User) {
            currentUser = user
        }
        @JvmName("getCurrentUser1")
        fun getCurrentUser() : MainActivity.User {
            return currentUser
        }
        fun updateUser(user : MainActivity.User) {
            currentUser = user
        }
    }
}
