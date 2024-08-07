package com.example.businessbuddy

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.businessbuddy.databinding.ActivityCreateSlipBinding
import com.example.businessbuddy.model.Slip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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

        //open date picker on clicking date
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

    //fetch current slip number and automatically update slip number
    private fun fetchAndSetCurrentSlipNumber() {
        availableSlipNumbersReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val availableSlipNumbers = snapshot.children.mapNotNull { it.key?.toInt() }
                    if (availableSlipNumbers.isNotEmpty()) {
                        binding.numberEditText.setText(availableSlipNumbers.min().toString())
                        return
                    }
                }
                slipCountReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentCount = snapshot.getValue(Int::class.java) ?: 0
                        binding.numberEditText.setText(currentCount.toString())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showToast("Failed to fetch slip number: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to fetch available slip numbers: ${error.message}")
            }
        })
    }

    //save slip data in database
    private fun saveSlip(slipNumber: String, slipDate: String, slipName: String, slipVehicleNo: String, slipItem: String, slipQuantity: String, slipAmount: String) {
        val slip = Slip(slipNumber, slipDate, slipName, slipVehicleNo, slipItem, slipQuantity, slipAmount)
        slipReference.child(slipNumber).setValue(slip).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                availableSlipNumbersReference.child(slipNumber).removeValue()
                showToast("Data saved successfully")
                clearFields()
                fetchAndSetCurrentSlipNumber()
            } else {
                showToast("Failed to save data")
            }
        }
    }

//    // function to delete slip
//    private fun deleteSlip(slipNumber: String) {
//        slipReference.child(slipNumber).removeValue().addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                availableSlipNumbersReference.child(slipNumber).setValue(true)
//                showToast("Slip deleted successfully")
//            } else {
//                showToast("Failed to delete slip")
//            }
//        }
//    }

    //clear edit texts
    private fun clearFields() {
        binding.NameEditText.setText("")
        binding.VehicleEditText.setText("")
        binding.ItemEditText.setText("")
        binding.quantityEditText.setText("")
        binding.amountEditText.setText("")
    }

    //update date
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

