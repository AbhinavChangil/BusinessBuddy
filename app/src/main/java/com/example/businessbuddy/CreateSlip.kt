package com.example.businessbuddy

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.businessbuddy.databinding.ActivityCreateSlipBinding
import com.example.businessbuddy.model.Slip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class CreateSlip : AppCompatActivity() {
    private val binding: ActivityCreateSlipBinding by lazy {
        ActivityCreateSlipBinding.inflate(layoutInflater)
    }

    // Variables to save data to the database
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var slipReference: DatabaseReference
    private lateinit var slipCountReference: DatabaseReference
    private lateinit var availableSlipNumbersReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        val myCalendar = Calendar.getInstance()

        // Set default date to today's date
        updateLabel(myCalendar)

        val datePicker = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateLabel(myCalendar)
        }

        // Open date picker on clicking date
        binding.edtDate.setOnClickListener {
            DatePickerDialog(this, datePicker, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Initialize auth and database
        database = FirebaseDatabase.getInstance()
        slipReference = database.reference.child("slips")
        slipCountReference = database.reference.child("slipCount")
        availableSlipNumbersReference = database.reference.child("availableSlipNumbers")

        // Fetch and set the current slip number
        fetchAndSetCurrentSlipNumber()

        // Clicking save button
        binding.btnSave.setOnClickListener {
            val slipNumber = binding.numberEditText.text.toString().trim()
            val slipDate = binding.edtDate.text.toString().trim()
            val slipName = binding.NameEditText.text.toString().trim()
            val slipVehicleNo = binding.VehicleEditText.text.toString().trim()
            val slipItem = binding.ItemEditText.text.toString().trim()
            val slipQuantity = binding.quantityEditText.text.toString().trim()
            val slipAmount = binding.amountEditText.text.toString().trim()

            if (slipNumber.isEmpty() || slipDate.isEmpty() || slipName.isEmpty() || slipVehicleNo.isEmpty() || slipItem.isEmpty() || slipQuantity.isEmpty() || slipAmount.isEmpty()) {
                showToast("Please Fill the Details!")
            } else {
                saveSlip(slipNumber, slipDate, slipName, slipVehicleNo, slipItem, slipQuantity, slipAmount)
            }
        }
    }

    // Fetch current slip number and automatically update slip number
    private fun fetchAndSetCurrentSlipNumber() {
        slipCountReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentCount = snapshot.getValue(Int::class.java) ?: 0
                val slipNumber = currentCount + 1

                // Check for unused slip numbers
                availableSlipNumbersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(availableSnapshot: DataSnapshot) {
                        if (availableSnapshot.exists()) {
                            val availableSlipNumbers = availableSnapshot.children.mapNotNull { it.key?.toInt() }
                            val minUnusedSlipNumber = availableSlipNumbers.minOrNull()

                            if (minUnusedSlipNumber != null && minUnusedSlipNumber < slipNumber) {
                                binding.numberEditText.setText(minUnusedSlipNumber.toString())
                            } else {
                                binding.numberEditText.setText(slipNumber.toString())
                            }
                        } else {
                            binding.numberEditText.setText(slipNumber.toString())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showToast("Failed to fetch available slip numbers: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to fetch slip count: ${error.message}")
            }
        })
    }

    // Save slip data in database
    private fun saveSlip(slipNumber: String, slipDate: String, slipName: String, slipVehicleNo: String, slipItem: String, slipQuantity: String, slipAmount: String) {
        val slip = Slip(slipNumber, slipDate, slipName, slipVehicleNo, slipItem, slipQuantity, slipAmount)
        slipReference.child(slipNumber).setValue(slip).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                availableSlipNumbersReference.child(slipNumber).removeValue().addOnCompleteListener { removeTask ->
                    if (removeTask.isSuccessful) {
                        generateAndSavePdf(slip)
                        updateSlipCount()
                        showToast("Data saved successfully")
                        clearFields()
                        fetchAndSetCurrentSlipNumber()
                    } else {
                        showToast("Failed to remove slip number from available list")
                    }
                }
            } else {
                showToast("Failed to save data")
            }
        }
    }

    private fun updateSlipCount() {
        slipCountReference.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var currentCount = currentData.getValue(Int::class.java) ?: 0
                currentCount += 1
                currentData.value = currentCount
                fetchAndSetCurrentSlipNumber()
                return Transaction.success(currentData)
            }

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                if (databaseError != null) {
                    showToast("Failed to update slip count: ${databaseError.message}")
                }
            }
        })
    }

    private fun generateAndSavePdf(slip: Slip) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = android.graphics.Paint()
        paint.textSize = 20f
        paint.color = Color.BLACK

        canvas.drawText("${binding.companyName.text}", 20f, 25f, paint)
        canvas.drawText("Slip Number: ${slip.slipNumber}", 80f, 100f, paint)
        canvas.drawText("Date: ${slip.slipDate}", 80f, 125f, paint)
        canvas.drawText("Name: ${slip.slipName}", 80f, 150f, paint)
        canvas.drawText("Vehicle No: ${slip.slipVehicleNo}", 80f, 175f, paint)
        canvas.drawText("Item: ${slip.slipItem}", 80f, 200f, paint)
        canvas.drawText("Quantity: ${slip.slipQuantity}", 80f, 225f, paint)
        canvas.drawText("Amount: ${slip.slipAmount}", 80f, 250f, paint)

        pdfDocument.finishPage(page)

        val filePath = "${getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}/Slip_${slip.slipNumber}.pdf"
        val file = File(filePath)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            slip.slipNumber?.let { uploadPdfToFirebase(file, it) }
        } catch (e: IOException) {
            e.printStackTrace()
            showToast("Failed to save PDF")
        }
    }

    private fun uploadPdfToFirebase(file: File, slipNumber: String) {
        val pdfUri = Uri.fromFile(file)
        val pdfReference = FirebaseStorage.getInstance().reference.child("slips/${file.name}")

        pdfReference.putFile(pdfUri).addOnSuccessListener {
            pdfReference.downloadUrl.addOnSuccessListener { uri ->
                val pdfUrl = uri.toString()
                updateSlipWithPdfUrl(slipNumber, pdfUrl)
            }
        }.addOnFailureListener {
            showToast("Failed to upload PDF")
        }
    }

    private fun updateSlipWithPdfUrl(slipNumber: String, pdfUrl: String) {
        slipReference.child(slipNumber).child("pdfUrl").setValue(pdfUrl).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast("PDF URL saved successfully")
            } else {
                showToast("Failed to save PDF URL")
            }
        }
    }

    // Clear edit texts
    private fun clearFields() {
        binding.NameEditText.setText("")
        binding.VehicleEditText.setText("")
        binding.ItemEditText.setText("")
        binding.quantityEditText.setText("")
        binding.amountEditText.setText("")
        binding.amountEditText.clearFocus()
    }

    // Update date
    private fun updateLabel(myCalendar: Calendar?) {
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.ROOT)
        if (myCalendar != null) {
            binding.edtDate.text = sdf.format(myCalendar.time)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
