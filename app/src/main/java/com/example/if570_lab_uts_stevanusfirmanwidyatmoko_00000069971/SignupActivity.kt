package com.example.if570_lab_uts_stevanusfirmanwidyatmoko_00000069971

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var nimEditText: EditText
    private lateinit var signupButton: Button
    private lateinit var loginTextView: TextView
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Inisialisasi komponen UI
        emailEditText = findViewById(R.id.signupEmailEditText)
        passwordEditText = findViewById(R.id.signupPasswordEditText)
        nameEditText = findViewById(R.id.signupNameEditText)
        nimEditText = findViewById(R.id.signupNimEditText)
        signupButton = findViewById(R.id.signupButton)
        loginTextView = findViewById(R.id.loginTextView)

        // Inisialisasi Firebase Auth dan Firestore
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Aksi tombol daftar
        signupButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val name = nameEditText.text.toString()
            val nim = nimEditText.text.toString()

            // Validasi inputan nama dan NIM
            if (name.isEmpty() || nim.isEmpty()) {
                Toast.makeText(this, "Nama dan NIM harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Proses signup dengan Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Dapatkan UID dari pengguna yang baru terdaftar
                        val userId = mAuth.currentUser?.uid
                        if (userId != null) {
                            // Simpan data pengguna (nama, NIM, email) ke Firestore
                            val userMap = hashMapOf(
                                "name" to name,
                                "nim" to nim,
                                "email" to email,
                                "status" to "Belum Absen"
                            )

                            // Menyimpan data pengguna ke collection "users" di Firestore
                            db.collection("users").document(userId).set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Sign Up Berhasil", Toast.LENGTH_SHORT).show()
                                    // Pindah ke halaman utama setelah berhasil sign up
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Gagal menyimpan data pengguna: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // Jika signup gagal, tampilkan pesan error
                        Toast.makeText(this, "Sign Up Gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Aksi untuk berpindah ke halaman login
        loginTextView.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
