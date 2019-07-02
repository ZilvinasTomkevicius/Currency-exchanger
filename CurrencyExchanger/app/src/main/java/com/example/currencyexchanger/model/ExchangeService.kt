package com.example.currencyexchanger.model

import com.example.currencyexchanger.di.DaggerApiComponent
import io.reactivex.Single
import javax.inject.Inject

class ExchangeService {

    @Inject
    lateinit var api: ExchangeApi

    init {
        DaggerApiComponent.create().inject(this)
    }

    fun getExchangeResult(fromAmount: Float, fromCurrency: String, toCurrency: String): Single<Wallet> {
        return api.exchange(fromAmount, fromCurrency, toCurrency)
    }
}