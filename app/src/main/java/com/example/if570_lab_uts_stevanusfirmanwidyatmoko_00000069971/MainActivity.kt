package com.example.if570_lab_uts_stevanusfirmanwidyatmoko_00000069971

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signupTextView: TextView
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        signupTextView = findViewById(R.id.signupTextView)
        mAuth = FirebaseAuth.getInstance()

        // Check if the user is already logged in
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            // User is signed in, redirect to the last activity
            redirectToLastActivity()
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login Berhasil", Toast.LENGTH_SHORT).show()
                        redirectToLastActivity() // Redirect to the last activity
                    } else {
                        Toast.makeText(this, "Login Gagal", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        signupTextView.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun redirectToLastActivity() {
        // Retrieve the last activity from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val lastActivity = sharedPreferences.getString("lastActivity", "HomeActivity") // Default to HomeActivity

        // Redirect based on the last activity
        when (lastActivity) {
            "ProfileActivity" -> {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            "HistoryActivity" -> {
                startActivity(Intent(this, HistoryActivity::class.java))
            }
            "HomeActivity" -> {
                startActivity(Intent(this, HomeActivity::class.java))
            }
            else -> {
                startActivity(Intent(this, HomeActivity::class.java)) // Fallback
            }
        }
        finish() // Close MainActivity
    }
}
