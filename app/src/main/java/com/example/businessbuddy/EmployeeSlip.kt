package com.example.businessbuddy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.businessbuddy.adapter.CreatedSlipsAdapter
import com.example.businessbuddy.databinding.ActivityEmployeeSlipBinding
import com.example.businessbuddy.model.SlipDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EmployeeSlip : AppCompatActivity(), CreatedSlipsAdapter.OnItemClicked {
    private val binding: ActivityEmployeeSlipBinding by lazy {
        ActivityEmployeeSlipBinding.inflate(layoutInflater)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var listOfSlips: ArrayList<SlipDetails> = arrayListOf()
    private lateinit var adapter: CreatedSlipsAdapter
    private  var userName1:String?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        //initialize database
        database = FirebaseDatabase.getInstance()
        //initialize auth
        auth = FirebaseAuth.getInstance()

        retrieveSlips()
    }


    private fun retrieveSlips() {
        val slipRef = database.reference.child("slips")
        val userId = auth.currentUser?.uid?:""
        val userRef = database.reference.child("admin").child(userId)
        val sortingQuery = slipRef.orderByChild("slipDate")


        userName1 = userId?.let {
            userRef.child("name").get().addOnSuccessListener { dataSnapshot ->
                userName1 = dataSnapshot.getValue(String::class.java)
                Log.d("UserName", "Name: $userName1")
            }.addOnFailureListener { exception ->
                Log.e("UserName", "Error: ${exception.message}")
            }
        }.toString()



        sortingQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (slipSnapshot in snapshot.children) {
                    val slipItems = slipSnapshot.getValue(SlipDetails::class.java)
                    slipItems?.let {
                        // Normalize the strings by converting to lowercase and removing spaces
                        val normalizedSlipName = it.slipDriver?.lowercase()?.replace("\\s".toRegex(), "")
                        val normalizedUserName = userName1?.lowercase()?.replace("\\s".toRegex(), "")

                        // Check if the normalized names match
                        if (normalizedSlipName == normalizedUserName) {
                            listOfSlips.add(it)
                        }
                    }
                }
                listOfSlips.reverse()
                if (listOfSlips.isNotEmpty()) {
                    // Setup the RecyclerView
                    setRecyclerView()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error if needed
                Log.e("RetrieveSlips", "Error: ${error.message}")
            }
        })
    }

    private fun setRecyclerView() {
        binding.rvSlips.layoutManager = LinearLayoutManager(this)
        adapter = CreatedSlipsAdapter(
            this,
            listOfSlips,
            this
        )
        binding.rvSlips.adapter = adapter
    }

    override fun OnItemClickListener(position: Int) {
        val intent = Intent(this, SlipKiDetails::class.java)
        val slipDetails = listOfSlips[position]
        intent.putExtra("SlipNumber", slipDetails.slipNumber)
        intent.putExtra("SlipDate", slipDetails.slipDate)
        intent.putExtra("SlipName", slipDetails.slipName)
        intent.putExtra("SlipDriver", slipDetails.slipDriver)
        intent.putExtra("SlipVehicleNo", slipDetails.slipVehicleNo)
        intent.putExtra("SlipItem", slipDetails.slipItem)
        intent.putExtra("SlipQuantity", slipDetails.slipQuantity)
        intent.putExtra("SlipAmount", slipDetails.slipAmount)
        startActivity(intent)
    }

    override fun OnItemAcceptClickListener(position: Int) {
        TODO("Not yet implemented")
    }

    override fun OnItemDispatchClickListener(position: Int) {
        TODO("Not yet implemented")
    }
}