package com.example.currencyexchanger.view

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.currencyexchanger.R
import com.example.currencyexchanger.model.Exchange
import com.example.currencyexchanger.model.Wallet
import com.example.currencyexchanger.viewmodel.CurrencyExchangeViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    //Variables
    lateinit var viewModel: CurrencyExchangeViewModel
    private val walletListAdapter = WalletListAdapter(arrayListOf())
    lateinit var exchangeEntity: Exchange
    var currencyList = ArrayList<String>(arrayListOf())

    val START_AMOUNT_USD = 1000f
    val START_AMOUNT_EUR = 0f
    val START_AMOUNT_JPY = 0f
    val CURRENCY_SHORTENING_LENGTH = 3

    /*
     initialization and provision for viewModel class
      */
    init {
        instance = this
    }

    companion object {
        private var instance: MainActivity? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        exchangeEntity = Exchange(null, null, null)
        viewModel = ViewModelProviders.of(this).get(CurrencyExchangeViewModel::class.java)
        viewModel.refresh()

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            exchange.isEnabled = true
            viewModel.refresh()
            observeUI()
        }

        walletList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = walletListAdapter
        }

        exchange.setOnClickListener{
            getDataAndExecute()
        }
        add_currency.setOnClickListener{
            showAddCurrencyDialog()
        }
        refresh_text_view.setOnClickListener{
            exchange.isEnabled = true
            viewModel.refresh()
            observeUI()
        }

        //spinners listener
        listenToUserChanges()
        observeUI()
        showBeginningDialog()
    }

    /*
    Observing UI changes
     */
    private fun observeUI() {
        viewModel.walletList.observe(this, Observer { data ->
            data?.let{
                if(it.isEmpty())
                    insertFirstData()
                else {
                    walletListAdapter.updateWallet(it)
                    exchange.isEnabled = true
                }
                if(currencyList.isEmpty() || it.size.compareTo(currencyList.size).equals(1)) {

                    getCurrenciesForSpinner()
                    initSpinner(exchange_from_spinner)
                    initSpinner(exchange_to_spinner)
                    viewModel.refresh()
                }
            }
        })

        viewModel.loading.observe(this, Observer { data ->
            data?.let{
                progressBar.visibility = if(it) View.VISIBLE else View.GONE
                if(it) {
                    load_data_error.visibility = View.GONE
                }
            }
        })

        viewModel.exchangeError.observe(this, Observer { data ->
            data?.let {
                load_data_error.visibility = if(it) View.VISIBLE else View.GONE
            }
        })

        viewModel.dataFromDatabaseRetrieved.observe(this, Observer { data ->
            data?.let {
                load_data_error.visibility = if(it) View.GONE else View.VISIBLE

                if(it) {
                    exchange.isEnabled = true
                }
            }
        })
    }

    private fun observeFirstDataInsertion() {
        viewModel.baseDataUploaded.observe(this, Observer { data ->
            data?.let {
                if(it) {
                    viewModel.refresh()
                }
            }
        })
    }

    private fun observeUpdatedDatabase() {
        viewModel.walletUpdated.observe(this, Observer { data ->
            data?.let {
                if(it) {
                    viewModel.refresh()
                    observeUI()
                }
            }
        })
    }

    /*
    Observing viewModel data after currency conversion
    */
    private fun observeExchangeResultAndUpdateDatabase() {
        viewModel.showResults.observe(this, object: Observer<Boolean> {
            override fun onChanged(t: Boolean?) {
                if(t!!) {
                    displayResultsDialog()
                    updateDatabase()
                    exchange.isEnabled = true
                    viewModel.showResults.removeObserver(this)
                }
            }
        })
    }

    /*
    Executing conversion
     */
    private fun getDataAndExecute() {
        if(!exchange_amount_edit_text.text.toString().equals("")) {
            exchangeEntity.fromAmount = exchange_amount_edit_text.text.toString().toFloat()
            if(checkIfConversionIsValid()) {
                exchange_amount_edit_text.clearComposingText()
                viewModel.convert(exchangeEntity)
                observeExchangeResultAndUpdateDatabase()
             }
        } else {
            Snackbar.make(swipeRefreshLayout, "Enter the amount.", Snackbar.LENGTH_SHORT).show()
        }
    }

    /*
    Checking conversion validity
     */
    private fun checkIfConversionIsValid(): Boolean {
        if(!exchangeEntity.fromCurrency.equals(exchangeEntity.toCurrency)) {
            for(w in viewModel.walletList.value!!) {
                if(w.currency.equals(exchangeEntity.fromCurrency)) {
                    if(w.amount!!.compareTo(0).equals(1)) {
                        if(exchangeEntity.fromAmount!!.compareTo(w.amount!!) <= 0) {
                            exchange.isEnabled = false
                            return true
                        } else
                            Snackbar.make(swipeRefreshLayout, "You don't have the amount in your wallet!", Snackbar.LENGTH_SHORT).show()                       
                    } else
                        Snackbar.make(swipeRefreshLayout, "You don't have the amount in your wallet!", Snackbar.LENGTH_SHORT).show()                    
                }
            }
        } else
            Snackbar.make(swipeRefreshLayout, "You can't convert to the same currency!", Snackbar.LENGTH_SHORT).show()
        return false
    }

    /*
    Executing database updates
     */
    private fun updateDatabase() {
        if(viewModel.checkIfPassToDatabase()) {
            viewModel.updateValuesForDatabase(viewModel.exchangeData.value!!.fromAmount!!,
                viewModel.exchangeData.value!!.fromCurrency!!,
                viewModel.exchangeResult.value!!.amount!!,
                viewModel.exchangeResult.value!!.currency)

            if(viewModel.exchangeData.value!!.fromAmount!!.compareTo(viewModel.FREE_EXCHANGE_LIMIT).equals(1)) {
                viewModel.updateConversionCount()
            }
            observeUpdatedDatabase()
        }
    }

    /*
    Currency spinners initialization and manegement
     */
    private fun initSpinner(spinner: Spinner) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.setAdapter(adapter)
    }

    private fun listenToUserChanges() {
        exchange_from_spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                exchangeEntity.fromCurrency = parent!!.getItemAtPosition(pos).toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        exchange_to_spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                exchangeEntity.toCurrency = parent!!.getItemAtPosition(pos).toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    /*
    Inserting base data to the database at the first launch of the app
     */
    private fun insertFirstData() {
        val walletList = ArrayList<Wallet>(arrayListOf())
        walletList.add(Wallet(null, START_AMOUNT_USD, "USD"))
        walletList.add(Wallet(null, START_AMOUNT_EUR, "EUR"))
        walletList.add(Wallet(null, START_AMOUNT_JPY, "JPY"))
        viewModel.uploadFirstData(walletList)
        viewModel.insertFirstConversionCount()
        observeFirstDataInsertion()
    }

    /*
    Inserting new currency
     */
    private fun insertNewCurrency(currency: String) {
        val wallet = Wallet(null, 0f, currency)
        viewModel.addNewCurrency(wallet)
        viewModel.refresh()
        observeUI()
    }

    /*
    Displaying conversion results
     */
    private fun displayResultsDialog() {
        showResultDialog(viewModel.exchangeData.value!!.fromAmount,
            viewModel.exchangeData.value!!.fromCurrency,
            viewModel.exchangeResult.value!!.amount,
            viewModel.exchangeResult.value!!.currency,
            viewModel.commisionFee.value!!)
    }

    /*
    Displaying dialog with refresh instruction at the start of the app
     */
    fun showBeginningDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("App instructions")
        builder.setMessage("- You can refresh the layout by scrolling down\n" +
                           "- Exchanges under ${viewModel.FREE_EXCHANGE_LIMIT} are free of charge\n" +
                           "- First ${viewModel.FREE_EXCHANGE_COUNT} exchanges over ${viewModel.FREE_EXCHANGE_LIMIT} are free of charge")
        builder.setPositiveButton("OK") { dialogInterface, i ->
            dialogInterface.dismiss()
        }
        builder.show()
    }

    /*
     Displaying conversion results
      */
    fun showResultDialog(fromAmount: Float?, fromCurrency: String?, toAmount: Float?, toCurrency: String?, comissionFee: Float?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Conversion results")
        if(viewModel.checkIfPassToDatabase())
            builder.setMessage("You have converted $fromAmount $fromCurrency to $toAmount $toCurrency. Commission Fee - $comissionFee $fromCurrency.")
        else
            builder.setMessage("Conversion was unsuccessful because the account can't be negative.")
        builder.setPositiveButton("OK") { dialogInterface, i ->
            dialogInterface.dismiss()
        }
        builder.show()
    }

    /*
    Displaying layout for currency addition
     */
    fun showAddCurrencyDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Add new currency")
        val dialogLayout = inflater.inflate(R.layout.add_new_currency_layout, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.add_currency_edit_text)
        builder.setView(dialogLayout)
        builder.setPositiveButton("Add") { dialogInterface, i ->
            if (!editText.text.toString().equals("")) {
                if (editText.text.toString().length.compareTo(CURRENCY_SHORTENING_LENGTH).equals(0))
                        insertNewCurrency(editText.text.toString().toUpperCase())
                else
                    Snackbar.make(swipeRefreshLayout, "The shortening must be in length of $CURRENCY_SHORTENING_LENGTH characters", Snackbar.LENGTH_LONG).show()
            } else
                Snackbar.make(swipeRefreshLayout, "Fill in the field.", Snackbar.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancel") { dialogInterface, i ->
            dialogInterface.dismiss()
        }
        builder.show()
    }

    /*
    Getting currency list for spinners
     */
    private fun getCurrenciesForSpinner() {
        for(w in viewModel.walletList.value!!) {
            if(!currencyList.contains(w.currency))
                currencyList.add(w.currency)
        }
    }
}
