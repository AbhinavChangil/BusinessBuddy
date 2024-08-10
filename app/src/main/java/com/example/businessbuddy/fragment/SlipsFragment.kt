package com.example.businessbuddy.fragment


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.businessbuddy.R
import com.example.businessbuddy.SlipKiDetails
import com.example.businessbuddy.adapter.CreatedSlipsAdapter
import com.example.businessbuddy.databinding.FragmentSlipsBinding
import com.example.businessbuddy.databinding.SlipsItemBinding
import com.example.businessbuddy.model.SlipDetails
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener



class SlipsFragment: Fragment(), CreatedSlipsAdapter.OnItemClicked {
    private lateinit var binding: FragmentSlipsBinding
    private var listOfSlips: ArrayList<SlipDetails> = arrayListOf()
    private lateinit var adapter: CreatedSlipsAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSlipsBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        retrieveSlipData()



        return binding.root
    }

    private fun setRecyclerView() {
        binding.rvSlips.layoutManager = LinearLayoutManager(requireContext())
        adapter = CreatedSlipsAdapter(
            requireContext(),
            listOfSlips,
            this
        )
        binding.rvSlips.adapter = adapter
    }

    private fun retrieveSlipData() {
        val slipReference = database.reference.child("slips")

        val sortingQuery = slipReference.orderByChild("slipDate")
        sortingQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (buySnapshot in snapshot.children) {
                    val slipItems = buySnapshot.getValue(SlipDetails::class.java)
                    slipItems?.let {
                        listOfSlips.add(it)
                    }
                }
                listOfSlips.reverse()
                if (listOfSlips.isNotEmpty()) {

                    //setup the recycler view
                    setRecyclerView()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                }
        })

    }

    override fun OnItemClickListener(position: Int) {
        val intent = Intent(requireContext(), SlipKiDetails::class.java)
        val slipDetails = listOfSlips[position]
        intent.putExtra("SlipNumber", slipDetails.slipNumber)
        intent.putExtra("SlipDate", slipDetails.slipDate)
        intent.putExtra("SlipName", slipDetails.slipName)
        intent.putExtra("SlipVehicleNo", slipDetails.slipVehicleNo)
        intent.putExtra("SlipItem", slipDetails.slipItem)
        intent.putExtra("SlipQuantity", slipDetails.slipQuantity)
        intent.putExtra("SlipAmount", slipDetails.slipAmount)
        startActivity(intent)
    }

    override fun OnItemAcceptClickListener(position: Int) {

    }

    override fun OnItemDispatchClickListener(position: Int) {
        TODO("Not yet implemented")
    }


}