package com.myapp.dompetku

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.util.Pair
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.myapp.dompetku.databinding.ActivityExportDataBinding
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExportData : AppCompatActivity() {

    private lateinit var binding: ActivityExportDataBinding

    private var dateStart: Long = 0
    private var dateEnd: Long = 0

    private val TAG = "ExcelUtil"
    private lateinit var cell: Cell
    private lateinit var workbook: Workbook
    private lateinit var sheet: Sheet
    private lateinit var headerCellStyle: CellStyle

    private var user = Firebase.auth.currentUser
    private val uid = user?.uid
    private var dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference(uid!!)

    private val PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private lateinit var fileName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExportDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        backButtonClicked()
        setInitDateRange()
        dateRangePicker()

        binding.exportBtn.setOnClickListener {
            fileName = "Catat_Uang_" + convertDateFileName(dateStart, dateEnd) + ".xls"
            if (checkPermissionsAtRuntime()) {
                exportDataIntoWorkbook()
            } else {
                inflateAlertDialog()
                Snackbar.make(it, "Permission is not granted", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun exportDataIntoWorkbook() {
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.e(TAG, "Storage not available or read only")
            return
        }

        workbook = HSSFWorkbook()
        setHeaderCellStyle()

        sheet = workbook.createSheet("Transactions")
        listOf(230, 230, 400, 400, 400, 500).forEachIndexed { index, width ->
            sheet.setColumnWidth(index, (15 * width))
        }

        setHeaderRow()
        fillDataIntoExcel()
    }

    private fun storeExcelInStorage() {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        var fileOutputStream: FileOutputStream? = null
        var isExcelGenerated = false

        try {
            fileOutputStream = FileOutputStream(file)
            workbook.write(fileOutputStream)
            Log.e(TAG, "Writing file $file")
            isExcelGenerated = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save file: ", e)
        } finally {
            fileOutputStream?.close()
        }

        if (isExcelGenerated) {
            Snackbar.make(binding.root, "Excel file exported to Downloads", Snackbar.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Export failed!", Toast.LENGTH_LONG).show()
        }
    }

    private fun fillDataIntoExcel() {
        val transactionList = arrayListOf<TransactionModel>()

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactionList.clear()
                if (snapshot.exists()) {
                    for (transactionSnap in snapshot.children) {
                        val data = transactionSnap.getValue(TransactionModel::class.java)
                        if (data != null && data.date!! > dateStart - 86400000 && data.date!! <= dateEnd) {
                            transactionList.add(data)
                        }
                    }

                    if (transactionList.isEmpty()) {
                        Snackbar.make(binding.root, "No transactions in this date range.", Snackbar.LENGTH_LONG).show()
                    } else {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                        transactionList.forEachIndexed { i, transaction ->
                            val row = sheet.createRow(i + 1)
                            row.createCell(0).setCellValue(sdf.format(Date(transaction.date!!)))
                            row.createCell(1).setCellValue(if (transaction.type == 1) "Expense" else "Income")
                            row.createCell(2).setCellValue(transaction.amount.toString())
                            row.createCell(3).setCellValue(transaction.title)
                            row.createCell(4).setCellValue(transaction.category)
                            row.createCell(5).setCellValue(transaction.note)
                        }
                        storeExcelInStorage()
                    }
                } else {
                    Snackbar.make(binding.root, "No transaction data found.", Snackbar.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun isExternalStorageAvailable() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
    private fun isExternalStorageReadOnly() = Environment.MEDIA_MOUNTED_READ_ONLY == Environment.getExternalStorageState()

    private fun setHeaderCellStyle() {
        headerCellStyle = workbook.createCellStyle()
        headerCellStyle.fillForegroundColor = HSSFColor.ORANGE.index
        headerCellStyle.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
        headerCellStyle.alignment = CellStyle.ALIGN_CENTER
    }

    private fun setHeaderRow() {
        val row = sheet.createRow(0)
        val headers = listOf("Date", "Type", "Amount", "Title", "Category", "Note")
        headers.forEachIndexed { index, title ->
            val cell = row.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerCellStyle
        }
    }

    private fun setInitDateRange() {
        val currentDate = Date()
        val cal = Calendar.getInstance().apply { time = currentDate }

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH))
        dateStart = cal.time.time

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        dateEnd = cal.time.time

        binding.dateRangeEt.hint = "This Month"
    }

    private fun dateRangePicker() {
        binding.dateRangeEt.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date")
                .setSelection(Pair(dateStart, dateEnd))
                .build()

            datePicker.show(supportFragmentManager, "DatePicker")

            datePicker.addOnPositiveButtonClickListener {
                val dateString = datePicker.selection.toString().filter { it.isDigit() }
                dateStart = dateString.substring(0, 13).toLong()
                dateEnd = dateString.substring(13).toLong()
                binding.dateRangeEt.hint = convertDate(dateStart, dateEnd)
            }
        }
    }

    private fun convertDate(start: Long, end: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        return "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
    }

    private fun convertDateFileName(start: Long, end: Long): String {
        val sdf = SimpleDateFormat("ddMMyyyy", Locale.ENGLISH)
        return "${sdf.format(Date(start))}_${sdf.format(Date(end))}"
    }

    private fun backButtonClicked() {
        binding.backBtn.setOnClickListener { finish() }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, 25)
    }

    private fun checkPermissionsAtRuntime(): Boolean {
        for (permission in PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun inflateAlertDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Mandatory")
            .setMessage("Please enable all permissions via Settings")
            .setPositiveButton("OKAY") { dialogInterface: DialogInterface, _ ->
                launchAppSettings()
                dialogInterface.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun launchAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, 20)
    }

    override fun onStart() {
        super.onStart()
        requestPermissions()
    }
}
