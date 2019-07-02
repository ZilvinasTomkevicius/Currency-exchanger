package com.example.currencyexchanger.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Wallet_table")
data class Wallet(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long?,

    @SerializedName("amount")
    @ColumnInfo(name = "amount") var amount: Float?,

    @SerializedName("currency")
    @ColumnInfo(name = "currency") val currency: String
)

@Entity(tableName = "Counter_table")
data class ConvertCounter(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long?,
    @ColumnInfo(name = "times_converted") var times_converted: Int
)

data class Exchange(

    @SerializedName("fromAmount")
    var fromAmount: Float?,
    @SerializedName("fromCurrency")
    var fromCurrency: String?,
    @SerializedName("toCurrency")
    var toCurrency: String?
)