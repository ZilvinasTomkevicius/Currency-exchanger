package com.example.currencyexchanger.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.Room
import com.example.currencyexchanger.db.Database
import com.example.currencyexchanger.di.DaggerApiComponent
import com.example.currencyexchanger.model.*
import com.example.currencyexchanger.view.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class CurrencyExchangeViewModel: ViewModel() {

    /*
    database initalization
     */
    private fun provideDatabase(): Database {
        return Room.databaseBuilder(
            MainActivity.applicationContext(),
            Database::class.java,
            "Wallet"
        ).build()
    }

    /*
    ExchangeService injection
     */
    @Inject
    lateinit var exchangeService: ExchangeService

    init {
        DaggerApiComponent.create().inject(this)
    }

    /*
    Variables
     */
    private val disposable = CompositeDisposable()
    val database = provideDatabase()

    val walletList = MutableLiveData<List<Wallet>>()
    val exchangeData = MutableLiveData<Exchange>()
    val exchangeResult = MutableLiveData<Wallet>()
    val exchangeError = MutableLiveData<Boolean>()
    val loading = MutableLiveData<Boolean>()
    val showResults = MutableLiveData<Boolean>()
    val walletUpdated = MutableLiveData<Boolean>()
    val baseDataUploaded = MutableLiveData<Boolean>()

    val conversionCount = MutableLiveData<ConvertCounter>()
    val conversionCountRetrieved = MutableLiveData<Boolean>()
    val commisionFee = MutableLiveData<Float>()

    val dataFromDatabaseRetrieved = MutableLiveData<Boolean>()

    val FREE_EXCHANGE_LIMIT = 200f
    val FREE_EXCHANGE_COUNT = 5
    val COMMISSION_FEE = 0.007f

    /*
    Refreshing data from database
     */
    fun refresh() {
        retrieveDataFromDatabase()
        getConversionCount()
    }

    /*
    Currency conversion task
     */
    fun convert(exchange: Exchange) {
        loading.value = true
        exchangeData.value = exchange
        commisionFee.value = calculateCommisionFee(exchangeData.value!!.fromAmount)
        disposable.add(exchangeService.getExchangeResult(exchange.fromAmount!!, exchange.fromCurrency!!, exchange.toCurrency!!)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: DisposableSingleObserver<Wallet>() {
                override fun onSuccess(value: Wallet?) {
                    exchangeResult.value = value
                    showResults.value = true
                    exchangeError.value = false
                    loading.value = false
                }

                override fun onError(e: Throwable?) {
                    exchangeError.value = true
                    loading.value = false
                    showResults.value = false
                }
            }))
    }

    fun checkIfPassToDatabase(): Boolean {
        for(w in walletList.value!!) {
            if(w.currency.equals(exchangeData.value!!.fromCurrency))
                if(!w.amount!!.minus(exchangeData.value!!.fromAmount!!.plus(commisionFee.value!!)).compareTo(0).equals(-1))
                    return true
        }
        return false
    }

    // ================== Database management ================

    /*
    Database update
     */
    fun updateValuesForDatabase(fromAmount: Float, fromCurrency: String, toAmount: Float, toCurrency: String) {
        for(w in walletList.value!!) {
            if(w.currency.equals(fromCurrency))
                w.amount = w.amount!!.minus(fromAmount.plus(commisionFee.value!!))
            if(w.currency.equals(toCurrency))
                w.amount = w.amount!!.plus(toAmount)
        }
        updateDatabaseWallet()
    }

    private fun updateDatabaseWallet() {
        showResults.value = false
        walletUpdated.value = false
        disposable.add(io.reactivex.Observable.fromCallable { updateWalletList() }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe())
        walletUpdated.value = true
    }

    private fun updateWalletList() {
        for(w in walletList.value!!) {
            database.walletDao().updateWalletList(w.amount!!, w.currency)
        }
    }

    /*
    Data retrieval from database
     */
    private fun retrieveDataFromDatabase() {
       disposable.add(database.walletDao().getWalletList()
           .subscribeOn(Schedulers.newThread())
           .observeOn(AndroidSchedulers.mainThread())
           .subscribeWith(object: DisposableSingleObserver<List<Wallet>>() {
               override fun onSuccess(value: List<Wallet>?) {
                   walletList.value = value
                   dataFromDatabaseRetrieved.value = true
                   loading.value = false
               }

               override fun onError(e: Throwable?) {
                   dataFromDatabaseRetrieved.value = false
                   loading.value = false
               }
           }))
    }


    /*
    Adding new currency
     */
    fun addNewCurrency(wallet: Wallet?) {
        disposable.add(io.reactivex.Observable.fromCallable { database.walletDao().insertNewCurrency(wallet!!) }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe())
    }

    /*
    Uploading base data on the first start of the app
     */
    fun uploadFirstData(walletList: List<Wallet>) {
        disposable.add(io.reactivex.Observable.fromCallable { addWalletList(walletList) }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe())
        baseDataUploaded.value = true
    }

    private fun addWalletList(walletList: List<Wallet>) {
        for (w in walletList) {
            database.walletDao().insertNewCurrency(w)
        }
    }

    /*
    Updating conversion count in order to follow commissions
     */
    fun updateConversionCount() {
        conversionCount.value!!.times_converted = conversionCount.value!!.times_converted.plus(1)
        disposable.add(io.reactivex.Observable.fromCallable { database.walletDao().updateConvertsCounter(conversionCount.value!!.times_converted) }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe())
    }

    private fun getConversionCount() {
        disposable.add(database.walletDao().getConversionCount()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: DisposableSingleObserver<ConvertCounter>() {
                override fun onSuccess(value: ConvertCounter?) {
                   conversionCount.value = value
                    conversionCountRetrieved.value = true
                }

                override fun onError(e: Throwable?) {
                    conversionCountRetrieved.value = false
                    conversionCount.value = ConvertCounter(null, 0)
                }
            }))
    }

    /*
    Inserting the first commission count at the start of the app
     */
    fun insertFirstConversionCount() {
        disposable.add(io.reactivex.Observable.fromCallable { database.walletDao().insertCounter(ConvertCounter(null, 0) )}
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe())
    }

    /*
    Calclulating commission if needed
     */
    private fun calculateCommisionFee(amount: Float?): Float? {
        if(checkForCommissionFee(amount))
            return amount!!.times(COMMISSION_FEE)
        return 0f
    }

    /*
    Checking if commission is needed. YOU CAN ALTER COMMISSION CONDITIONS HERE
     */
    private fun checkForCommissionFee(amount: Float?): Boolean {
        if(conversionCount.value!!.times_converted.compareTo(FREE_EXCHANGE_COUNT).equals(1) && amount!!.compareTo(FREE_EXCHANGE_LIMIT).equals(1)) {
            return true
        } else if(amount!!.compareTo(FREE_EXCHANGE_LIMIT).equals(-1)) {
            return false
        }
        return false
    }

    /*
    clearing disposable
     */
    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}