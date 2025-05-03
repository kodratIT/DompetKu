package com.myapp.dompetku

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.myapp.dompetku.databinding.ActivityInsertionBinding
import java.text.SimpleDateFormat
import java.util.*

class InsertionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInsertionBinding

    private var type: Int = 1
    private var amount: Double = 0.0
    private var date: Long = 0
    private var invertedDate: Long = 0
    private var isSubmitted: Boolean = false

    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsertionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFirebase()
        setupUI()
    }

    private fun setupFirebase() {
        val user = Firebase.auth.currentUser
        val uid = user?.uid
        if (uid != null) {
            dbRef = FirebaseDatabase.getInstance().getReference(uid)
        }
    }

    private fun setupUI() {
        // Set default date
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val currentDate = sdf.parse(sdf.format(System.currentTimeMillis()))
        date = currentDate!!.time

        val listExpense = CategoryOptions.expenseCategory()
        val expenseAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listExpense)
        binding.category.setAdapter(expenseAdapter)
        setBackgroundColor()

        binding.date.setOnClickListener {
            showDatePicker()
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.category.text.clear()
            if (checkedId == binding.rbExpense.id) {
                type = 1
                setBackgroundColor()
                binding.category.setAdapter(expenseAdapter)
            } else if (checkedId == binding.rbIncome.id) {
                type = 2
                setBackgroundColor()

                val listIncome = CategoryOptions.incomeCategory()
                val incomeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listIncome)
                binding.category.setAdapter(incomeAdapter)
            }
        }

        binding.saveButton.setOnClickListener {
            if (!isSubmitted) {
                saveTransactionData()
            } else {
                Snackbar.make(binding.root, "You have saved the transaction data", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setBackgroundColor() {
        val res = resources
        val pkg = packageName

        if (type == 1) {
            val selectedExpenseDrawable = res.getIdentifier("radio_selected_expense", "drawable", pkg)
            val notSelectedDrawable = res.getIdentifier("radio_not_selected", "drawable", pkg)
            val bgInsertExpense = res.getIdentifier("bg_insert_expense", "drawable", pkg)
            val orangeColor = res.getIdentifier("orangePrimary", "color", pkg)

            binding.rbExpense.setBackgroundResource(selectedExpenseDrawable)
            binding.rbIncome.setBackgroundResource(notSelectedDrawable)
            binding.toolbarLinear.setBackgroundResource(bgInsertExpense)
            binding.saveButton.backgroundTintList = ContextCompat.getColorStateList(this, orangeColor)
            window.statusBarColor = ContextCompat.getColor(this, orangeColor)

        } else {
            val selectedIncomeDrawable = res.getIdentifier("radio_selected_income", "drawable", pkg)
            val notSelectedDrawable = res.getIdentifier("radio_not_selected", "drawable", pkg)
            val bgInsertIncome = res.getIdentifier("bg_insert_income", "drawable", pkg)
            val toscaColor = res.getIdentifier("toscaSecondary", "color", pkg)

            binding.rbIncome.setBackgroundResource(selectedIncomeDrawable)
            binding.rbExpense.setBackgroundResource(notSelectedDrawable)
            binding.toolbarLinear.setBackgroundResource(bgInsertIncome)
            binding.saveButton.backgroundTintList = ContextCompat.getColorStateList(this, toscaColor)
            window.statusBarColor = ContextCompat.getColor(this, toscaColor)
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val dpd = DatePickerDialog(this,
            { _, year, month, dayOfMonth ->
                val selectedDate = "$dayOfMonth/${month + 1}/$year"
                binding.date.text = null
                binding.date.hint = selectedDate

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                date = sdf.parse(selectedDate)!!.time
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dpd.show()
    }

    private fun saveTransactionData() {
        val title = binding.title.text.toString()
        val category = binding.category.text.toString()
        val amountText = binding.amount.text.toString()
        val note = binding.note.text.toString()

        when {
            amountText.isEmpty() -> binding.amount.error = "Please enter Amount"
            title.isEmpty() -> binding.title.error = "Please enter Title"
            category.isEmpty() -> binding.category.error = "Please enter Category"
            else -> {
                amount = amountText.toDouble()
                invertedDate = date * -1

                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) {
                    Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                    return
                }

                val dbRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("transactions")

                val transactionID = dbRef.push().key!!
                val transaction = TransactionModel(
                    transactionID,
                    type,
                    title,
                    category,
                    amount,
                    date,
                    note,
                    invertedDate
                )

                dbRef.child(transactionID).setValue(transaction)
                    .addOnCompleteListener {
                        Toast.makeText(this, "Data Inserted Successfully", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { err ->
                        Toast.makeText(this, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                    }

                isSubmitted = true
            }
        }
    }

}
