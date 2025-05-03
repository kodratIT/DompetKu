package com.myapp.dompetku.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.myapp.dompetku.databinding.FragmentTransactionBinding
import com.myapp.dompetku.*
import java.util.*
import kotlin.collections.ArrayList

class TransactionFragment : Fragment() {

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionList: ArrayList<TransactionModel>
    private lateinit var dbRef: DatabaseReference
    private val user = Firebase.auth.currentUser

    private var selectedType: String = "All Type"
    private var selectedTimeSpan: String = "All Time"
    private var dateStart: Long = 0
    private var dateEnd: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeUI()
        setupSpinners()
        setupRecyclerView()
        fetchTransactions()

        binding.swipeRefresh.setOnRefreshListener {
            fetchTransactions()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.exportButton.setOnClickListener {
            val intent = Intent(requireContext(), ExportData::class.java)
            startActivity(intent)
        }
    }

    private fun initializeUI() {
        val email = user?.email
        val userName = user?.displayName
        val name = userName ?: email?.substringBefore("@") ?: "User"
        binding.userNameTV.text = "Hi, $name!"
    }

    private fun setupSpinners() {
        val context = requireContext()
        val packageName = context.packageName

        val typeList = arrayOf("All Type", "Expense", "Income")
        val timeSpanList = arrayOf("All Time", "This Month", "This Week", "Today")

        // Ambil ID layout spinner item dan dropdown secara dinamis
        val spinnerItemLayout = resources.getIdentifier("selected_spinner", "layout", packageName)
        val dropdownLayout = resources.getIdentifier("simple_list_item_1", "layout", "android")

        // Adapter Type
        val typeAdapter = ArrayAdapter(context, spinnerItemLayout, typeList)
        typeAdapter.setDropDownViewResource(dropdownLayout)
        binding.typeSpinner.adapter = typeAdapter

        // Adapter Time Span
        val timeSpanAdapter = ArrayAdapter(context, spinnerItemLayout, timeSpanList)
        timeSpanAdapter.setDropDownViewResource(dropdownLayout)
        binding.timeSpanSpinner.adapter = timeSpanAdapter

        // Listener Spinner Jenis Transaksi
        binding.typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedType = typeList[position]
                fetchTransactions()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Listener Spinner Rentang Waktu
        binding.timeSpanSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedTimeSpan = timeSpanList[position]
                when (selectedTimeSpan) {
                    "This Month" -> setDateRange(Calendar.DAY_OF_MONTH)
                    "This Week" -> setDateRange(Calendar.DAY_OF_WEEK)
                    "Today" -> {
                        val currentTime = System.currentTimeMillis()
                        dateStart = currentTime
                        dateEnd = currentTime
                    }
                    else -> {
                        dateStart = 0
                        dateEnd = 0
                    }
                }
                fetchTransactions()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setDateRange(field: Int) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        cal.set(field, cal.getActualMinimum(field))
        dateStart = cal.timeInMillis

        cal.set(field, cal.getActualMaximum(field))
        dateEnd = cal.timeInMillis
    }

    private fun setupRecyclerView() {
        binding.rvTransaction.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransaction.setHasFixedSize(true)
        transactionList = arrayListOf()
    }

    private fun fetchTransactions() {
        binding.shimmerFrameLayout.startShimmer()
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.rvTransaction.visibility = View.GONE
        binding.tvNoData.visibility = View.GONE
        binding.noDataImage.visibility = View.GONE
        binding.tvNoDataTitle.visibility = View.GONE
        binding.visibilityNoData.visibility = View.GONE

        val uid = Firebase.auth.currentUser?.uid ?: return
        Log.d("FETCH", "Fetching data for UID: $uid")

        dbRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("transactions")

        val query: Query = dbRef.orderByChild("invertedDate")

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactionList.clear()
                Log.d("FETCH", "Snapshot exists: ${snapshot.exists()}")

                if (snapshot.exists()) {
                    for (transactionSnap in snapshot.children) {
                        val transaction = transactionSnap.getValue(TransactionModel::class.java)
                        Log.d("FETCH", "Transaction raw data: ${transactionSnap.value}")

                        transaction?.let {
                            Log.d("FETCH", "Parsed Transaction: $it")

                            val withinDateRange = when (selectedTimeSpan) {
                                "All Time" -> true
                                else -> it.date?.let { date ->
                                    date > dateStart - 86400000 && date <= dateEnd
                                } ?: false
                            }

                            val matchesType = when (selectedType) {
                                "All Type" -> true
                                "Expense" -> it.type == 1
                                "Income" -> it.type == 2
                                else -> false
                            }

                            if (withinDateRange && matchesType) {
                                Log.d("FETCH", "Transaction added to list: ${it.title}")
                                transactionList.add(it)
                            }
                        }
                    }

                    if (transactionList.isEmpty()) {
                        Log.d("FETCH", "No transactions match filter.")
                        binding.noDataImage.visibility = View.VISIBLE
                        binding.tvNoDataTitle.visibility = View.VISIBLE
                        binding.visibilityNoData.visibility = View.VISIBLE
                        binding.visibilityNoData.text =
                            "There is no $selectedType data $selectedTimeSpan"
                    } else {
                        Log.d("FETCH", "Total transactions shown: ${transactionList.size}")
                        val adapter = TransactionAdapter(transactionList)
                        adapter.setOnItemClickListener(object : TransactionAdapter.OnItemClickListener {
                            override fun onItemClick(position: Int) {
                                val transaction = transactionList[position]
                                val intent = Intent(requireContext(), TransactionDetails::class.java).apply {
                                    putExtra("transactionID", transaction.transactionID)
                                    putExtra("type", transaction.type)
                                    putExtra("title", transaction.title)
                                    putExtra("category", transaction.category)
                                    putExtra("amount", transaction.amount)
                                    putExtra("date", transaction.date)
                                    putExtra("note", transaction.note)
                                }
                                startActivity(intent)
                            }
                        })
                        binding.rvTransaction.adapter = adapter
                        binding.rvTransaction.visibility = View.VISIBLE
                        adapter.notifyDataSetChanged()

                    }
                } else {
                    Log.d("FETCH", "Snapshot empty. No data found.")
                    binding.noDataImage.visibility = View.VISIBLE
                    binding.tvNoDataTitle.visibility = View.VISIBLE
                    binding.tvNoData.visibility = View.VISIBLE
                }

                binding.shimmerFrameLayout.stopShimmer()
                binding.shimmerFrameLayout.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FETCH", "Database error: ${error.message}")
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
