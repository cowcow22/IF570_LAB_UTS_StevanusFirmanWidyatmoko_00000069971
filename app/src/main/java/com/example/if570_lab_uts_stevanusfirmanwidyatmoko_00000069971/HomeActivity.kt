package com.example.if570_lab_uts_stevanusfirmanwidyatmoko_00000069971

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.Storage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var dateTimeTextView: TextView
    private lateinit var attendanceButton: ImageButton
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null
    private var hasCheckedIn: Boolean = false
    private var hasCheckedOut: Boolean = false

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val CAMERA_INTENT_REQUEST_CODE = 101
        private const val STATUS_NOT_CHECKED_IN = "Belum Absen"
        private const val STATUS_CHECKED_IN = "Masuk"
        private const val STATUS_CHECKED_OUT = "Pulang"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inisialisasi komponen UI
        dateTimeTextView = findViewById(R.id.dateTimeTextView)
        attendanceButton = findViewById(R.id.attendanceButton)
        val profileIcon: ImageView = findViewById(R.id.profileIcon)
        val historyIcon: ImageView = findViewById(R.id.historyIcon)

        // Inisialisasi Firebase Firestore dan Firebase Auth
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Dapatkan userId dari Firebase Authentication
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            dateTimeTextView.text = getCurrentDateTime()

            // Log the authenticated user's UID for debugging
            Log.d("User", "UID: ${currentUser.uid}")

            profileIcon.setOnClickListener {
                startActivity(Intent(this, ProfileActivity::class.java))
            }

            historyIcon.setOnClickListener {
                startActivity(Intent(this, HistoryActivity::class.java))
            }

            checkAttendanceStatus()
        } else {
            Log.w("Auth", "User not authenticated") // Log jika pengguna belum login
            showToast("Pengguna belum login!")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        attendanceButton.setOnClickListener {
            updateAttendanceStatus(userId ?: "")
        }

        // Check camera permissions at the start
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun updateAttendanceStatus(userId: String) {
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val status = document.getString("status") ?: STATUS_NOT_CHECKED_IN

                when (status) {
                    STATUS_NOT_CHECKED_IN -> {
                        showToast("Anda belum melakukan absen hari ini.")
                        userRef.update("status", STATUS_CHECKED_IN)
                            .addOnSuccessListener {
                                showToast("Status berhasil diperbarui menjadi 'Masuk'.")
                                openCamera()
                            }
                            .addOnFailureListener { e -> showToast("Gagal memperbarui status: ${e.message}") }
                    }
                    STATUS_CHECKED_IN -> {
                        userRef.update("status", STATUS_CHECKED_OUT)
                            .addOnSuccessListener {
                                showToast("Status berhasil diperbarui menjadi 'Pulang'.")
                                openCamera()
                            }
                            .addOnFailureListener { e -> showToast("Gagal memperbarui status: ${e.message}") }
                    }
                    STATUS_CHECKED_OUT -> {
                        // Notifikasi ketika sudah absen masuk dan pulang
                        showToast("Anda sudah absen masuk dan pulang, silahkan tunggu besok.")
                    }
                }
            } else {
                Log.d("HomeActivity", "Dokumen tidak ditemukan!")
            }
        }.addOnFailureListener { e -> Log.w("HomeActivity", "Gagal mendapatkan dokumen: ", e) }
    }

    private fun resetStatusIfNewDay(userRef: DocumentReference) {
        val currentDate = getCurrentDateString()

        userRef.get().addOnSuccessListener { document ->
            val lastAttendanceDate = document.getString("lastAttendanceDate")
            if (lastAttendanceDate != currentDate) {
                userRef.update("status", STATUS_NOT_CHECKED_IN, "lastAttendanceDate", currentDate)
                    .addOnSuccessListener { showToast("Status berhasil direset menjadi 'Belum Absen'.") }
                    .addOnFailureListener { e -> showToast("Gagal mereset status: ${e.message}") }
            }
        }
    }

    private fun saveAttendanceToFirestore(photo: Bitmap?, status: String) {
        // Check if the photo is null
        if (photo == null) {
            Log.e("PhotoError", "The photo bitmap is null.")
            showToast("Photo is null.")
            return
        }

        // Save the photo to the gallery
        saveImageToGallery(photo)

        // Get the userId from the current user
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showToast("User ID is null. Please log in again.")
            return
        }

        // Construct the storage reference path
        val fileName = "${userId}_${System.currentTimeMillis()}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference
        val photoRef = storageRef.child("attendance_photos/$fileName")

        // Log the file path for debugging
        Log.d("UploadPath", "Uploading photo to: ${photoRef.path}")

        // Convert the photo to byte array
        val baos = ByteArrayOutputStream()
        photo.compress(Bitmap.CompressFormat.JPEG, 50, baos) // Use lower quality for upload
        val photoData = baos.toByteArray()

        // Start the upload with retry logic
        uploadPhotoWithRetry(photoData, photoRef, userId, status)
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        // Save the image to the gallery
        val savedImageURL: String = MediaStore.Images.Media.insertImage(
            contentResolver,
            bitmap,
            "Image_${System.currentTimeMillis()}",
            "Image taken by app"
        )
        Log.d("GallerySave", "Image saved to gallery: $savedImageURL")
    }

    private fun uploadPhotoWithRetry(photoData: ByteArray, photoRef: StorageReference, userId: String, status: String, retries: Int = 3) {
        Log.d("UploadDebug", "Uploading photo of size: ${photoData.size} bytes")
        photoRef.putBytes(photoData)
            .addOnSuccessListener { taskSnapshot ->
                photoRef.downloadUrl.addOnSuccessListener { uri ->
                    // Prepare attendance data to save to Firestore
                    val attendanceData = hashMapOf(
                        "userId" to userId,
                        "date" to getCurrentDateString(),
                        "status" to status,
                        "photoUrl" to uri.toString(),
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )

                    // Save attendance data to Firestore
                    db.collection("attendance").add(attendanceData)
                        .addOnSuccessListener {
                            showToast("Absensi $status berhasil disimpan")
                            if (status == STATUS_CHECKED_IN) {
                                hasCheckedIn = true
                            } else if (status == STATUS_CHECKED_OUT) {
                                hasCheckedOut = true
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirestoreError", "Gagal menyimpan absensi: ${e.message}", e)
                            showToast("Gagal menyimpan absensi: ${e.message}")
                        }
                }.addOnFailureListener { e ->
                    Log.e("UploadError", "Failed to get image URL: ${e.localizedMessage}", e)
                    showToast("Gagal mendapatkan URL gambar: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("UploadError", "Failed to upload image: ${e.localizedMessage}", e)
                if (retries > 0) {
                    Log.e("UploadError", "Upload failed. Retrying... ($retries retries left)")
                    uploadPhotoWithRetry(photoData, photoRef, userId, status, retries - 1)
                } else {
                    showToast("Gagal mengupload gambar: ${e.message}")
                }
            }
    }


    private fun checkAttendanceStatus() {
        val currentDate = getCurrentDateString()

        userId?.let { uid ->
            db.collection("attendance")
                .whereEqualTo("userId", uid)
                .whereEqualTo("date", currentDate)
                .get()
                .addOnSuccessListener { documents ->
                    var status: String? = null

                    if (documents.isEmpty) {
                        status = STATUS_NOT_CHECKED_IN
                    } else {
                        for (document in documents) {
                            status = document.getString("status")
                            when (status) {
                                STATUS_CHECKED_IN -> hasCheckedIn = true
                                STATUS_CHECKED_OUT -> hasCheckedOut = true
                            }
                        }
                    }

                    if (status == STATUS_NOT_CHECKED_IN) {
                        requestAttendance()
                    }
                }
                .addOnFailureListener { e -> showToast("Gagal mengecek status absen: ${e.message}") }
        }
    }

    private fun requestAttendance() {
        updateAttendanceStatus(userId ?: "")
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == CAMERA_INTENT_REQUEST_CODE) {
            val photo = data?.extras?.get("data") as Bitmap

            val attendanceStatus = when {
                !hasCheckedIn -> STATUS_CHECKED_IN // Pengguna baru akan melakukan absen masuk
                !hasCheckedOut -> STATUS_CHECKED_OUT
                else -> return // Jika sudah absen masuk dan pulang, keluar dari fungsi
            }

            saveAttendanceToFirestore(photo, attendanceStatus)
        }
    }


    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_INTENT_REQUEST_CODE)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
