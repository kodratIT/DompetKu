package com.myapp.dompetku

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.myapp.dompetku.databinding.ActivityTransactionDetailsBinding
import com.myapp.dompetku.databinding.UpdateDialogBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetails : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setValuesToViews()
    }

    private fun setupListeners() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.deleteData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setMessage("Do you want to delete this transaction?")
                .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                    deleteRecord(intent.getStringExtra("transactionID").toString())
                }
                .setNegativeButton("No", null)
                .show()
        }

        binding.updateData.setOnClickListener {
            openUpdateDialog(intent.getStringExtra("title").toString())
        }
    }

    private fun deleteRecord(transactionID: String) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference(uid).child(transactionID)

        dbRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Transaction Data Deleted", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Deleting Failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setValuesToViews() {
        val type = intent.getIntExtra("type", 0)
        val amount = intent.getDoubleExtra("amount", 0.0)
        val date = intent.getLongExtra("date", 0)

        binding.tvTitleDetails.text = intent.getStringExtra("title")
        binding.tvCategoryDetails.text = intent.getStringExtra("category")
        binding.tvNoteDetails.text = intent.getStringExtra("note")
        binding.tvAmountDetails.text = amount.toString()

        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
        binding.tvDateDetails.text = sdf.format(Date(date))

        val res = resources
        val pkg = packageName

        if (type == 1) {
            binding.tvTypeDetails.text = "Expense Transaction"
            binding.tvAmountDetails.setTextColor(Color.parseColor("#ff9f1c"))
            val bgExpenseId = res.getIdentifier("bg_details_expense", "drawable", pkg)
            binding.transactionDetailsTitle.setBackgroundResource(bgExpenseId)
        } else {
            binding.tvTypeDetails.text = "Income Transaction"
            binding.tvAmountDetails.setTextColor(Color.parseColor("#2ec4b6"))
            val bgIncomeId = res.getIdentifier("bg_details_income", "drawable", pkg)
            val colorId = res.getIdentifier("toscaSecondary", "color", pkg)

            binding.transactionDetailsTitle.setBackgroundResource(bgIncomeId)
            window.statusBarColor = ContextCompat.getColor(this, colorId)
        }
    }


    private fun openUpdateDialog(title: String) {
        val dialogBinding = UpdateDialogBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Updating $title's Transaction")
            .setView(dialogBinding.root)
            .create()

        val intentTitle = intent.getStringExtra("title")
        val intentAmount = intent.getDoubleExtra("amount", 0.0)
        val intentNote = intent.getStringExtra("note")
        val intentCategory = intent.getStringExtra("category")
        val intentDate = intent.getLongExtra("date", 0)
        val type = intent.getIntExtra("type", 0)
        val transactionID = intent.getStringExtra("transactionID") ?: return

        dialogBinding.titleUpdate.setText(intentTitle)
        dialogBinding.amountUpdate.setText(intentAmount.toString())
        dialogBinding.noteUpdate.setText(intentNote)
        dialogBinding.categoryUpdate.setText(intentCategory)

        // Set category adapter
        val list = if (type == 1) CategoryOptions.expenseCategory() else CategoryOptions.incomeCategory()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
        dialogBinding.categoryUpdate.setAdapter(adapter)

        // Set and handle date
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val dateObject = Date(intentDate)
        dialogBinding.dateUpdate.setText(sdf.format(dateObject))

        var dateUpdate = intentDate
        var invertedDate = dateUpdate * -1

        dialogBinding.dateUpdate.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = dateObject }
            DatePickerDialog(this,
                { _, year, month, day ->
                    val selectedDate = "$day/${month + 1}/$year"
                    dialogBinding.dateUpdate.setText(selectedDate)

                    val parsed = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse(selectedDate)
                    dateUpdate = parsed!!.time
                    invertedDate = dateUpdate * -1
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.updateButton.setOnClickListener {
            updateTransactionData(
                transactionID,
                type,
                dialogBinding.titleUpdate.text.toString(),
                dialogBinding.categoryUpdate.text.toString(),
                dialogBinding.amountUpdate.text.toString().toDouble(),
                dateUpdate,
                dialogBinding.noteUpdate.text.toString(),
                invertedDate
            )

            // Update view in detail page after update
            binding.tvTitleDetails.text = dialogBinding.titleUpdate.text.toString()
            binding.tvCategoryDetails.text = dialogBinding.categoryUpdate.text.toString()
            binding.tvNoteDetails.text = dialogBinding.noteUpdate.text.toString()
            binding.tvAmountDetails.text = dialogBinding.amountUpdate.text.toString()
            binding.tvDateDetails.text = sdf.format(Date(dateUpdate))

            Toast.makeText(this, "Transaction Data Updated", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateTransactionData(
        transactionID: String,
        type: Int,
        title: String,
        category: String,
        amount: Double,
        date: Long,
        note: String,
        invertedDate: Long
    ) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("transactions")
            .child(transactionID)

        val transaction = TransactionModel(transactionID, type, title, category, amount, date, note, invertedDate)
        dbRef.setValue(transaction)

    }
}
