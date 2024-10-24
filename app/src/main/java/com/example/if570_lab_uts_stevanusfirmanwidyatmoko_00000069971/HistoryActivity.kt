package com.example.if570_lab_uts_stevanusfirmanwidyatmoko_00000069971

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var attendanceAdapter: AttendanceAdapter
    private val db = FirebaseFirestore.getInstance() // Inisialisasi Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val homeIcon: ImageView = findViewById(R.id.homeIcon)
        val profileIcon: ImageView = findViewById(R.id.profileIcon)

        // Tombol untuk navigasi
        homeIcon.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.recyclerView)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inisialisasi adapter dengan data kosong
        attendanceAdapter = AttendanceAdapter(emptyList())
        recyclerView.adapter = attendanceAdapter

        // Ambil data absensi dari Firestore
        fetchAttendanceData()
    }

    // Ambil data absensi dari Firestore berdasarkan userId yang sedang login
    private fun fetchAttendanceData() {
        val currentUser = FirebaseAuth.getInstance().currentUser // Ambil pengguna saat ini
        if (currentUser != null) { // Pastikan pengguna terautentikasi
            db.collection("attendance")
                .whereEqualTo("userId", currentUser.uid) // Filter berdasarkan userId
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val attendanceList = mutableListOf<Attendance>()
                    for (document in documents) {
                        val date = document.getString("date")
                        val status = document.getString("status")
                        val photoUrl = document.getString("photoUrl")
                        val timestamp = document.getTimestamp("timestamp")
                        if (date != null && status != null && photoUrl != null && timestamp != null) {
                            val time = formatTimestampToTime(timestamp.toDate())
                            attendanceList.add(Attendance(date, status, photoUrl, time))
                        }
                    }
                    attendanceAdapter.updateData(attendanceList) // Update adapter dengan data baru
                }
                .addOnFailureListener { e ->
                    Log.w("FirestoreError", "Error getting documents: ", e)
                }
        } else {
            Log.w("AuthCheck", "User is not authenticated.")
        }
    }


    // Fungsi untuk memformat timestamp menjadi jam
    private fun formatTimestampToTime(date: Date): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()) // Format jam:menit
        return sdf.format(date)
    }

}