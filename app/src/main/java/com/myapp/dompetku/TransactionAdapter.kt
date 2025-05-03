package com.myapp.dompetku

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.myapp.dompetku.databinding.TransactionListItemBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val transactionList: ArrayList<TransactionModel>
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    private lateinit var listener: OnItemClickListener

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TransactionListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactionList[position]

        holder.binding.tvTransactionTitle.text = transaction.title
        holder.binding.tvCategory.text = transaction.category

        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date(transaction.date!!))
        holder.binding.tvDate.text = formattedDate

        holder.binding.tvAmount.text = transaction.amount.toString()

        if (transaction.type == 1) {
            holder.binding.tvAmount.setTextColor(Color.parseColor("#ff9f1c"))
            holder.binding.typeIcon.setImageResource(holder.getDrawableId("ic_moneyout_svgrepo_com"))
        } else {
            holder.binding.tvAmount.setTextColor(Color.parseColor("#2ec4b6"))
            holder.binding.typeIcon.setImageResource(holder.getDrawableId("ic_moneyin_svgrepo_com"))
        }
    }

    override fun getItemCount(): Int = transactionList.size

    class ViewHolder(
        val binding: TransactionListItemBinding,
        listener: OnItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }

        fun getDrawableId(name: String): Int {
            return binding.root.context.resources.getIdentifier(name, "drawable", binding.root.context.packageName)
        }
    }
}
