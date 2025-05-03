package com.myapp.dompetku

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.myapp.dompetku.databinding.ActivitySignUpBinding

class SignUp : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        setupListeners()
    }

    private fun setupListeners() {
        binding.signupBtn.setOnClickListener {
            signUpUser()
        }

        binding.haveAccount.setOnClickListener {
            finish() // kembali ke Login
        }
    }

    private fun signUpUser() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val confirmPassword = binding.passwordRetype.text.toString().trim()

        if (!isValidEmail(email)) {
            showToast("Invalid or empty email")
            return
        }

        if (password.isEmpty() || confirmPassword.isEmpty()) {
            showToast("Password fields cannot be empty")
            return
        }

        if (password != confirmPassword) {
            showToast("Passwords do not match")
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                binding.progressBar.visibility = View.GONE
                if (it.isSuccessful) {
                    showToast("Sign Up Successful")
                    goToMainActivity()
                } else {
                    showToast("Error: ${it.exception?.message}")
                }
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
