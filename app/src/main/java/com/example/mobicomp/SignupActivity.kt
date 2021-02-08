package com.example.mobicomp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
                Toast.makeText(this, "Please give username, email, and password", Toast.LENGTH_SHORT).show()
            } else {
                // Store new user to Database
                var userName : String = signUpUsername.text.toString()
                var isSuccess = false
                var isReady = false
                if (db != null) {
                    val users = db.userDao()
                    GlobalScope.launch {
                        // Implement password hashing later
                        if (users.findByUsername(userName) == null) {
                            if (users.findByEmail(signUpEmail.text.toString()) == null) {
                                users.insertAll(MainActivity.User(0, userName, signUpEmail.text.toString(), signUpPassword.text.toString(),0))
                                isSuccess = true
                                isReady = true
                            }
                        }
                    }
                }

                while (!isReady) {
                    // Stupidest shit ever to wait for database like this but it works :D
                    Log.d("lol", "stuck")
                }

                if (isSuccess) {
                    Toast.makeText(this, "User created", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        // Room for putExtra()
                        putExtra("Username", userName)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Username/email already in use", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
