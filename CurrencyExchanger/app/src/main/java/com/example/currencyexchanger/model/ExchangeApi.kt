package com.example.currencyexchanger.model

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeApi {

    @GET("currency/commercial/exchange/{fromAmount}-{fromCurrency}/{toCurrency}/latest")
    fun exchange(@Path("fromAmount") fromAmount: Float,
                 @Path("fromCurrency") fromCurrency: String,
                 @Path("toCurrency") toCurrency: String): Single<Wallet>
}