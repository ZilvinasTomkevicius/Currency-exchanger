package com.example.currencyexchanger.di

import com.example.currencyexchanger.model.ExchangeService
import com.example.currencyexchanger.viewmodel.CurrencyExchangeViewModel
import dagger.Component

@Component(modules = [ApiModule::class])
interface ApiComponent {

    fun inject(viewModel: CurrencyExchangeViewModel)

    fun inject(service: ExchangeService)
}