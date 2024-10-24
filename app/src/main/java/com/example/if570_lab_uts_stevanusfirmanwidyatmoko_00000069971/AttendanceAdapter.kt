package com.example.if570_lab_uts_stevanusfirmanwidyatmoko_00000069971

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Model untuk data absensi
data class Attendance(
    val date: String,
    val status: String,
    val photoUrl: String, // URL foto dari Firestore
    val time: String // Jam absensi
)

class AttendanceAdapter(private var attendanceList: List<Attendance>) :
    RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val attendance = attendanceList[position]
        holder.bind(attendance)
    }

    override fun getItemCount(): Int = attendanceList.size

    // Fungsi untuk memperbarui data dalam adapter
    fun updateData(newAttendanceList: List<Attendance>) {
        attendanceList = newAttendanceList
        notifyDataSetChanged() // Beri tahu adapter untuk merender ulang data
    }


    // ViewHolder untuk RecyclerView
    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewPhoto: ImageView = itemView.findViewById(R.id.imageViewPhoto)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        private val textViewStatus: TextView = itemView.findViewById(R.id.textViewStatus)

        fun bind(attendance: Attendance) {
            textViewDate.text = attendance.date
            textViewTime.text = attendance.time // Set jam absensi
            textViewStatus.text = attendance.status

            // Menggunakan Glide atau library serupa untuk memuat foto dari URL
            Glide.with(itemView.context)
                .load(attendance.photoUrl)
                .into(imageViewPhoto)
        }
    }
}
