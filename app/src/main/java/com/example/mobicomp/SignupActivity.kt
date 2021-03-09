package com.example.mobicomp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.security.MessageDigest


class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initiate database
        val db = MainActivity.DatabaseUtils.getDatabase(this)

        // Initiate views
        val signUpUsername : EditText = findViewById(R.id.signUpUsername)
        val signUpEmail : EditText = findViewById(R.id.signUpEmail)
        val signUpPassword : EditText = findViewById(R.id.signUpPassword)

        // Set onClickListeners for button
        val signUpButton : Button = findViewById(R.id.signUp)
        signUpButton.setOnClickListener {
            // Change to login activity
            // Check if sign up name, email or password field is empty, not the best way to do this but good enough
            if (signUpUsername.text.toString() == "" || signUpEmail.text.toString() == "" || signUpPassword.text.toString() == "") {
                showToast("Please give username, email, and password")
            } else {
                // Store new user to Database
                val isSuccess = createNewUser(db!!, signUpUsername.text.toString(), signUpEmail.text.toString(), signUpPassword.text.toString())

                if (isSuccess) {
                    showToast("User created")
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        // Room for putExtra()
                        putExtra("Username", signUpUsername.text.toString())
                    }
                    startActivity(intent)
                } else {
                    showToast("Username/email already in use")
                }
            }
        }
    }

    private fun createNewUser(db: MainActivity.AppDatabase, userName: String, email: String, password: String): Boolean {
        var isSuccess = false
        var isReady = false
        val users = db.userDao()
        GlobalScope.launch {
            if (users.findByUsername(userName) == null) {
                if (users.findByEmail(email) == null) {
                    users.insertAll(MainActivity.User(0, userName, email, hashPassword(password),0, 0))
                    isSuccess = true
                    isReady = true
                }
            }
        }

        while (!isReady) {
            // Stupidest shit ever to wait for database like this but it works :D
        }

        return isSuccess
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val messageDigest = MessageDigest.getInstance("MD5")
        fun hashPassword(password: String): String {
            return BigInteger(1, messageDigest.digest(password.toByteArray())).toString(16).padStart(32, '0')
        }
    }
}
