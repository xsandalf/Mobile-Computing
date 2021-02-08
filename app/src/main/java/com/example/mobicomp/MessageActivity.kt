package com.example.mobicomp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar


class MessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        setSupportActionBar(findViewById(R.id.toolbar))

        val placeholderArray = arrayOf(
            "Number 1",
            "Number 2",
            "Number 3"
        )

        val listView : ListView = findViewById(R.id.message_list)
        listView.adapter = placeholderAdapter(this, placeholderArray)

        // Set onClickListeners for buttons
        val myProfileButton : Button = findViewById(R.id.my_profile)
        myProfileButton.setOnClickListener {
            // Change to login activity
            val intent = Intent(this, ProfileActivity::class.java).apply {
                // Room for putExtra()
            }
            startActivity(intent)
        }

        val logOutButton : Button = findViewById(R.id.log_out)
        logOutButton.setOnClickListener {
            // Change to sign up activity
            val intent = Intent(this, MainActivity::class.java).apply {
                // Room for putExtra()
            }
            startActivity(intent)
        }

        //Use this to create new reminder in HW2
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(
                view,
                "Create new reminder will be implemented in HW2",
                Snackbar.LENGTH_LONG
            )
                .setAction("Action", null).show()
        }
    }

    class placeholderAdapter(val context: Context, private val list: Array<String>) : BaseAdapter() {

        private val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val currentUser : MainActivity.User = LoginActivity.currentUser.getCurrentUser()
        val chosenPfp = currentUser.picId

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
            val view = layoutInflater.inflate(R.layout.placeholder_item, p2, false)
            val pfp : ImageView = view.findViewById(R.id.profilePic)
            var profilePics = context.resources.obtainTypedArray(R.array.profile_pics)
            pfp.setImageDrawable(ContextCompat.getDrawable(context, profilePics.getResourceId(chosenPfp, 0)))
            profilePics.recycle()
            val text : TextView = view.findViewById(R.id.textBox)
            text.text = getItem(p0) as CharSequence

            return view
        }

    }
}