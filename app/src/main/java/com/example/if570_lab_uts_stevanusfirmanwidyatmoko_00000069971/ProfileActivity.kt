package com.example.if570_lab_uts_stevanusfirmanwidyatmoko_00000069971

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Inisialisasi Firestore dan FirebaseAuth
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Mengaitkan komponen UI
        val homeIcon: ImageView = findViewById(R.id.homeIcon)
        val historyIcon: ImageView = findViewById(R.id.historyIcon)
        val nameField = findViewById<EditText>(R.id.name)
        val idNumberField = findViewById<EditText>(R.id.nim)
        val saveButton = findViewById<Button>(R.id.saveProfile)
        val logoutButton = findViewById<Button>(R.id.logoutButton) // Mengaitkan tombol logout

        // Aksi ketika ikon Home diklik
        homeIcon.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Aksi ketika ikon History diklik
        historyIcon.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        // Isi field dengan data saat ini dari Firestore (kalau ada)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        nameField.setText(document.getString("name") ?: "")
                        idNumberField.setText(document.getString("nim") ?: "")
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error fetching profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Aksi ketika tombol Save diklik
        saveButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val nim = idNumberField.text.toString().trim()

            // Validasi input
            if (name.isEmpty() || nim.isEmpty()) {
                Toast.makeText(this, "Nama dan NIM tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userMap = mapOf(
                "name" to name,
                "nim" to nim
            )

            // Update data pengguna di Firestore
            currentUser?.let {
                db.collection("users").document(it.uid)
                    .update(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menyimpan profil: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Aksi ketika tombol Logout diklik
        logoutButton.setOnClickListener {
            // Simpan nama activity terakhir yang dikunjungi
            saveLastVisitedActivity(HomeActivity::class.java.simpleName) // Contoh, bisa disesuaikan

            auth.signOut() // Logout dari Firebase Auth
            Toast.makeText(this, "Berhasil Logout", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java) // Ganti dengan activity login Anda
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the activity stack
            startActivity(intent)
            finish() // Tutup activity saat ini
        }
    }

    private fun saveLastVisitedActivity(activityName: String) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("lastActivity", activityName)
            apply()
        }
    }
}
