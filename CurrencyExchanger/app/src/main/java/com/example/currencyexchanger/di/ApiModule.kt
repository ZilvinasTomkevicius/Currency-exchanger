package com.example.currencyexchanger.di

import com.example.currencyexchanger.model.ExchangeApi
import com.example.currencyexchanger.model.ExchangeService
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

@Module
class ApiModule {

    private val BASE_URL = "http://api.evp.lt"

    @Provides
    fun providesExchangeApi(): ExchangeApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(ExchangeApi::class.java)
    }

    @Provides
    fun provideExchangeService(): ExchangeService {
        return ExchangeService()
    }
}