package com.example.ecocycle

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.ecocycle.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        firebaseAuth = FirebaseAuth.getInstance()

        binding.txtDaftar.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.edEmail.editText?.text.toString()
            val password1 = binding.edPassword.editText?.text.toString()

            if (email.isNotEmpty() && password1.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password1)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {

                            val sharedPref = getSharedPreferences("login_pref", MODE_PRIVATE)
                            val editor = sharedPref.edit()
                            editor.putBoolean("isLoggedIn", true)
                            editor.apply()

                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finishAffinity()

                        } else {
                            Toast.makeText(this, "Login Gagal", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Semua Form Harus Terisi", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onBackPressed() {
        moveTaskToBack(true)
        super.onBackPressed()
    }
}