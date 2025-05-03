package com.myapp.dompetku

object CategoryOptions {

    fun expenseCategory(): ArrayList<String> {
        val listExpense = ArrayList<String>()
        listExpense.add("Iuran Warga")
        listExpense.add("Kegiatan Sosial")
        listExpense.add("Operasional RT")
        listExpense.add("Sumbangan")
        listExpense.add("Biaya Kebersihan")
        listExpense.add("Keamanan")
        listExpense.add("Perbaikan Fasilitas")
        listExpense.add("Dana Darurat")
        listExpense.add("Administrasi")
        listExpense.add("Pengeluaran Lainnya")

        return listExpense
    }

    fun incomeCategory(): ArrayList<String> {
        val listIncome = ArrayList<String>()
        listIncome.add("Iuran Bulanan")
        listIncome.add("Sumbangan Warga")
        listIncome.add("Dana Bantuan")
        listIncome.add("Hasil Kegiatan")
        listIncome.add("Pemasukan Lainnya")

        return listIncome
    }
}
