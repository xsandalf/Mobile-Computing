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
        val profilePics = resources.obtainTypedArray(R.array.profile_pics)
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

        setPfpOnClickListeners(pfpViews)

        // Set onClickListeners for buttons
        // Switch to edit mode
        val editProfileButton: Button = findViewById(R.id.editProfile)
        editProfileButton.setOnClickListener {
            // Change to edit mode
            changeHiddenViewVisibility(hiddenViews, View.VISIBLE, hiddenEditViews)
            changeToEditMode(profilePic, profileUsername, profileEmail, editProfileButton, pfpViews)
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
                        currentUser = updateUserNoPassword(users!!, currentUser, profileUsername.text.toString(), profileEmail.text.toString())
                        LoginActivity.CurrentUser.updateUser(currentUser)
                        isSuccess = true
                        isReady = true
                    } else if (getPasswordsMatch(currentUser, oldProfilePassword.text.toString(), newProfilePassword.text.toString(), confirmProfilePassword.text.toString())) {
                        currentUser = updateUserPassword(users!!, currentUser, profileUsername.text.toString(), profileEmail.text.toString(), confirmProfilePassword.text.toString())
                        LoginActivity.CurrentUser.updateUser(currentUser)
                        isSuccess = true
                        isReady = true
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
                showToast("Saved changes")
                // Change to profile mode
                changeHiddenViewVisibility(hiddenViews, View.INVISIBLE, hiddenEditViews)
                changeToProfileMode(profilePic, profileUsername, profileEmail, editProfileButton)
            } else {
                showToast("Password error, cancelling changes")
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

    private fun setPfpOnClickListeners(pfpViews: Array<ImageView>) {
        for (pic in pfpViews) {
            pic.setOnClickListener {
                pfpViews[chosenPfp].imageAlpha = 255
                chosenPfp = pic.getTag().toString().toInt()
                pic.imageAlpha = 100
            }
        }
    }

    private fun changeHiddenViewVisibility(hiddenViews: Array<View>, visibility: Int, hiddenEditViews: Array<EditText>) {
        for (view in hiddenViews) {
            view.visibility = visibility
        }
        for (view in hiddenEditViews) {
            view.setText("")
        }
    }

    private fun getPasswordsMatch(currentUser: MainActivity.User, oldPassword: String, newPassword: String, confirmPassword: String): Boolean {
        return SignupActivity.hashPassword(oldPassword) == currentUser.password && newPassword == confirmPassword && confirmPassword != ""
    }

    private fun updateUserNoPassword(users: MainActivity.UserDao, currentUser: MainActivity.User, username: String, email: String): MainActivity.User {
        users.updateUser(
            currentUser.uid,
            username,
            email,
            currentUser.password,
            chosenPfp,
            currentUser.rememberMe
        )

        return users.findByUid(currentUser.uid)
    }

    private fun updateUserPassword(users: MainActivity.UserDao, currentUser: MainActivity.User, username: String, email: String, password: String): MainActivity.User {
        users.updateUser(
            currentUser.uid,
            username,
            email,
            SignupActivity.hashPassword(password),
            chosenPfp,
            0
        )

        return users.findByUid(currentUser.uid)
    }

    private fun changeToEditMode(profilePic: ImageView, profileUsername: EditText, profileEmail: EditText, editProfileButton: Button, pfpViews: Array<ImageView>) {
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

    private fun changeToProfileMode(profilePic: ImageView, profileUsername: EditText, profileEmail: EditText, editProfileButton: Button) {
        profilePic.visibility = View.VISIBLE
        profileUsername.inputType = InputType.TYPE_NULL
        profileUsername.isEnabled = false
        profileEmail.inputType = InputType.TYPE_NULL
        profileEmail.isEnabled = false
        editProfileButton.visibility = View.VISIBLE
        val profilePics = resources.obtainTypedArray(R.array.profile_pics)
        profilePic.setImageDrawable(ContextCompat.getDrawable(this, profilePics.getResourceId(chosenPfp, 0)))
        profilePics.recycle()
    }

    private fun showToast(text: String) {
        Toast.makeText(
            this,
            text,
            Toast.LENGTH_SHORT
        ).show()
    }
}