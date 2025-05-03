package com.myapp.dompetku

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.myapp.dompetku.databinding.ActivityMainBinding
import com.myapp.dompetku.fragments.AccountFragment
import com.myapp.dompetku.fragments.TransactionFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val transactionFragment = TransactionFragment()
    private val accountFragment = AccountFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Set default item selected and initial fragment
        val defaultItemId = resources.getIdentifier("ic_transaction", "id", packageName)
        val transactionFragmentContainerId = resources.getIdentifier("fl_wrapper", "id", packageName)

        binding.chipAppBar.setItemSelected(defaultItemId, true)
        supportFragmentManager.beginTransaction()
            .replace(transactionFragmentContainerId, transactionFragment)
            .commit()

        binding.chipAppBar.setOnItemSelectedListener { itemId ->
            when (itemId) {
                resources.getIdentifier("ic_transaction", "id", packageName) -> {
                    makeCurrentFragment(transactionFragment)
                }
                resources.getIdentifier("ic_account", "id", packageName) -> {
                    makeCurrentFragment(accountFragment)
                }
            }
            true
        }
    }

    private fun makeCurrentFragment(fragment: Fragment) {
        val containerId = resources.getIdentifier("fl_wrapper", "id", packageName)
        supportFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .commit()
    }

    // Dipanggil dari floatingActionButton di XML dengan onClick="floating_button"
    fun floating_button(view: View) {
        startActivity(Intent(this, InsertionActivity::class.java))
    }
}
