package com.example.businessbuddy.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.example.businessbuddy.databinding.ActivityCreateSlipBinding
import com.example.businessbuddy.databinding.SlipsItemBinding
import com.example.businessbuddy.model.SlipDetails

class CreatedSlipsAdapter(
    private val context: Context,
    private val SlipDetails: ArrayList<SlipDetails>,
    private val itemClicked: OnItemClicked
):RecyclerView.Adapter<CreatedSlipsAdapter.CreatedSlipsViewHolder>(){


    interface OnItemClicked {
        fun OnItemClickListener(position: Int)
        fun OnItemAcceptClickListener(position: Int)
        fun OnItemDispatchClickListener(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreatedSlipsViewHolder {
        return CreatedSlipsViewHolder(SlipsItemBinding.inflate(LayoutInflater.from(parent.context),parent, false))
    }

    override fun getItemCount(): Int = SlipDetails.size

    override fun onBindViewHolder(holder: CreatedSlipsViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class CreatedSlipsViewHolder(private val binding: SlipsItemBinding):RecyclerView.ViewHolder(binding.root){

        fun bind(position: Int) {
            val slip = SlipDetails[position]
            binding.apply {
                slipName.text = slip.slipName
                amountText.text = slip.slipAmount
                date.text = slip.slipDate

                itemView.setOnClickListener {
                    itemClicked.OnItemClickListener(position)
                }
            }
        }

    }

}