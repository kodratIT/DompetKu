package com.myapp.dompetku

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.myapp.dompetku.databinding.ActivityForgotPasswordBinding
class ForgotPassword : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.forgotPassBtn.setOnClickListener {
            val email = binding.emailForgotPass.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter email address", Toast.LENGTH_LONG).show()
            } else {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Check your email! (Including Spam)", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            Toast.makeText(this, task.exception?.message ?: "Error occurred", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }
}
