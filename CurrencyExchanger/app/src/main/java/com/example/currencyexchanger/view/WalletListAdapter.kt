package com.example.currencyexchanger.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.currencyexchanger.R
import com.example.currencyexchanger.model.Wallet
import kotlinx.android.synthetic.main.currency_item.view.*

/*
Adapter for displaying account data in recycler view
 */
class WalletListAdapter (var wallet: ArrayList<Wallet>) : RecyclerView.Adapter<WalletListAdapter.WalletViewHolder>() {
    fun updateWallet(newWallet: List<Wallet>) {
        wallet.clear()
        wallet.addAll(newWallet)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = WalletViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.currency_item, parent, false)
    )

    override fun getItemCount() = wallet.size

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        holder.bind(wallet[position])
    }

    class WalletViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val currency = view.currency_text
        private val amount = view.amount_text

        fun bind(wallet: Wallet) {
            currency.text = wallet.currency
            amount.text = wallet.amount.toString()
        }
    }
}