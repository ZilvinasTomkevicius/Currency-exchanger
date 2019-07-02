package com.example.currencyexchanger.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.currencyexchanger.model.ConvertCounter
import com.example.currencyexchanger.model.Wallet
import com.example.currencyexchanger.model.WalletDao

@Database(entities = [(Wallet::class), (ConvertCounter::class)], version = 1)
abstract class Database: RoomDatabase() {
    abstract fun walletDao(): WalletDao
}