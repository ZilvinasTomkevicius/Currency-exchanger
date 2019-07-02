package com.example.currencyexchanger.model

import androidx.room.*
import io.reactivex.Single

@Dao
interface WalletDao {

    //wallet_table queries
    @Query("SELECT * FROM Wallet_table")
    fun getWalletList(): Single<List<Wallet>>

    @Query("UPDATE Wallet_table set amount = :amount where currency = :currency")
    fun updateWalletList(amount: Float?, currency: String?)

    @Insert
    fun insertNewCurrency(vararg wallet: Wallet)

    //convert_counter table queries
    @Query("SELECT * FROM Counter_table")
    fun getConversionCount(): Single<ConvertCounter>

    @Insert
    fun insertCounter(vararg convertCounter: ConvertCounter)

    @Query("UPDATE Counter_table set times_converted = :conversion")
    fun updateConvertsCounter(conversion: Int)
}