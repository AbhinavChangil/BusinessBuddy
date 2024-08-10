package com.example.businessbuddy.model

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class SlipDetails() : Serializable{
    var slipAmount: String? = null
    var slipDate: String? = null
    var slipItem:String?=null
    var slipName: String? = null
    var slipDriver: String? = null
    var slipNumber: String? = null
    var slipQuantity: String? = null
    var slipVehicleNo: String? = null

    constructor(parcel: Parcel) : this() {
        slipAmount = parcel.readString()
        slipDate = parcel.readString()
        slipItem = parcel.readString()
        slipName = parcel.readString()
        slipDriver = parcel.readString()
        slipNumber = parcel.readString()
        slipQuantity = parcel.readString()
        slipVehicleNo = parcel.readString()
    }

    fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(slipAmount)
        parcel.writeString(slipDate)
        parcel.writeString(slipItem)
        parcel.writeString(slipName)
        parcel.writeString(slipDriver)
        parcel.writeString(slipNumber)
        parcel.writeString(slipQuantity)
        parcel.writeString(slipVehicleNo)
    }

    fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SlipDetails> {
        override fun createFromParcel(parcel: Parcel): SlipDetails {
            return SlipDetails(parcel)
        }

        override fun newArray(size: Int): Array<SlipDetails?> {
            return arrayOfNulls(size)
        }
    }
}