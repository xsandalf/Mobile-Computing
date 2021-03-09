package com.example.mobicomp

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    var chosenPfp : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initiate database
        val db = MainActivity.DatabaseUtils.getDatabase(this)

        // Get current user
        var currentUser : MainActivity.User = LoginActivity.CurrentUser.getCurrentUser()
        chosenPfp = currentUser.picId

        // Initiate views
        // View Binding might be better but I'm old school Java man:D
        val profilePic : ImageView = findViewById(R.id.profilePic)
        var profilePics = resources.obtainTypedArray(R.array.profile_pics)
        profilePic.setImageDrawable(ContextCompat.getDrawable(this, profilePics.getResourceId(chosenPfp, 0)))
        profilePics.recycle()
        val profileUsername : EditText = findViewById(R.id.profileUsername)
        val profileEmail : EditText = findViewById(R.id.profileEmail)
        val changePassword : TextView = findViewById(R.id.changePassword)
        val oldPassword : TextView = findViewById(R.id.oldPassword)
        val oldProfilePassword : EditText = findViewById(R.id.oldProfilePassword)
        val newPassword : TextView = findViewById(R.id.newPassword)
        val newProfilePassword : EditText = findViewById(R.id.newProfilePassword)
        val confirmPassword : TextView = findViewById(R.id.confirmPassword)
        val confirmProfilePassword : EditText = findViewById(R.id.confirmProfilePassword)
        val saveChangesButton : Button = findViewById(R.id.saveChange)
        val cancelChangesButton : Button = findViewById(R.id.cancelChange)
        val pfpChoice1 : ImageView = findViewById(R.id.pfpChoice1)
        val pfpChoice2 : ImageView = findViewById(R.id.pfpChoice2)
        val pfpChoice3 : ImageView = findViewById(R.id.pfpChoice3)
        val pfpChoice4 : ImageView = findViewById(R.id.pfpChoice4)
        val pfpChoice5 : ImageView = findViewById(R.id.pfpChoice5)

        profileUsername.setText(currentUser.username)
        profileEmail.setText(currentUser.email)

        // Help arrays
        val hiddenViews = arrayOf(
            changePassword,
            oldPassword,
            oldProfilePassword,
            newPassword,
            newProfilePassword,
            confirmPassword,
            confirmProfilePassword,
            saveChangesButton,
            cancelChangesButton,
            pfpChoice1,
            pfpChoice2,
            pfpChoice3,
            pfpChoice4,
            pfpChoice5
        )

        val hiddenEditViews = arrayOf(
            oldProfilePassword,
            newProfilePassword,
            confirmProfilePassword
        )

        val pfpViews = arrayOf(
            pfpChoice1,
            pfpChoice2,
            pfpChoice3,
            pfpChoice4,
            pfpChoice5
        )

        for (pic in pfpViews) {
            pic.setOnClickListener {
                pfpViews[chosenPfp].imageAlpha = 255
                chosenPfp = pic.getTag().toString().toInt()
                pic.imageAlpha = 100
            }
        }

        // Set onClickListeners for buttons
        // Switch to edit mode
        val editProfileButton: Button = findViewById(R.id.editProfile)
        editProfileButton.setOnClickListener {
            // Change to edit mode
            for (view in hiddenViews) {
                view.visibility = View.VISIBLE
            }
            for (view in hiddenEditViews) {
                view.setText("")
            }
            profilePic.visibility = View.INVISIBLE
            profileUsername.inputType = InputType.TYPE_CLASS_TEXT
            profileUsername.isEnabled = true
            profileEmail.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            profileEmail.isEnabled = true
            editProfileButton.visibility = View.GONE
            for (pic in pfpViews) {
                pic.imageAlpha = 255
            }
            pfpViews[chosenPfp].imageAlpha = 100
        }

        // Save changes
        saveChangesButton.setOnClickListener {
            var isSuccess = false
            var isReady = false
            val users = db?.userDao()
            // Store changes to Database
            if (db != null) {
                GlobalScope.launch {
                    if (oldProfilePassword.text.toString() == "" && newProfilePassword.text.toString() == "" && confirmProfilePassword.text.toString() == "") {
                        if (users != null) {
                            users.updateUser(
                                currentUser.uid,
                                profileUsername.text.toString(),
                                profileEmail.text.toString(),
                                currentUser.password,
                                chosenPfp,
                                currentUser.rememberMe
                            )
                            currentUser = users.findByUid(currentUser.uid)
                            LoginActivity.CurrentUser.updateUser(currentUser)
                            isSuccess = true
                            isReady = true
                        }
                    } else if (SignupActivity.hashPassword(oldProfilePassword.text.toString()) == currentUser.password
                        && newProfilePassword.text.toString() == confirmProfilePassword.text.toString()
                        && confirmProfilePassword.text.toString() != "") {
                        if (users != null) {
                            users.updateUser(
                                currentUser.uid,
                                profileUsername.text.toString(),
                                profileEmail.text.toString(),
                                SignupActivity.hashPassword(confirmProfilePassword.text.toString()),
                                chosenPfp,
                                0
                            )
                            currentUser = users.findByUid(currentUser.uid)
                            LoginActivity.CurrentUser.updateUser(currentUser)
                            isSuccess = true
                            isReady = true
                        }
                    } else {
                        isSuccess = false
                        isReady = true
                    }
                }
            }
            while (!isReady) {
                // Stupidest shit ever to wait for database like this but it works :D
            }
            if (isSuccess) {
                Toast.makeText(
                    this,
                    "Saved changes",
                    Toast.LENGTH_SHORT
                ).show()
                // Change to profile mode
                for (view in hiddenViews) {
                    view.visibility = View.INVISIBLE
                }
                for (view in hiddenEditViews) {
                    view.setText("")
                }
                profilePic.visibility = View.VISIBLE
                profileUsername.inputType = InputType.TYPE_NULL
                profileUsername.isEnabled = false
                profileEmail.inputType = InputType.TYPE_NULL
                profileEmail.isEnabled = false
                editProfileButton.visibility = View.VISIBLE
                profilePics = resources.obtainTypedArray(R.array.profile_pics)
                profilePic.setImageDrawable(ContextCompat.getDrawable(this, profilePics.getResourceId(chosenPfp, 0)))
                profilePics.recycle()
            } else {
                Toast.makeText(
                    this,
                    "Password error, cancelling changes",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Cancel changes
        cancelChangesButton.setOnClickListener {
            // Change to profile mode
            for (view in hiddenViews) {
                view.visibility = View.INVISIBLE
            }
            for (view in hiddenEditViews) {
                view.setText("")
            }
            profilePic.visibility = View.VISIBLE
            profileUsername.inputType = InputType.TYPE_NULL
            profileUsername.isEnabled = false
            profileEmail.inputType = InputType.TYPE_NULL
            profileEmail.isEnabled = false
            editProfileButton.visibility = View.VISIBLE
        }
    }
}