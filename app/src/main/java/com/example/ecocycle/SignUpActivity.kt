package com.example.ecocycle

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import com.example.ecocycle.databinding.ActivityEditProfileBinding
import com.example.ecocycle.databinding.ActivitySignUpBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        hapusError()

        binding.btnSignup.setOnClickListener {
            val email = binding.edEmail.editText?.text.toString()
            val password1 = binding.edPassword1.editText?.text.toString()
            val nama = binding.edNama.editText?.text.toString()
            val noTelp = binding.edNohp.editText?.text.toString()
            val polaNoTelp = Regex("^08[0-9]{8,11}\$")
            val nomorValid = polaNoTelp.matches(noTelp)

            if (email.isNotEmpty() && password1.isNotEmpty()) {
                if (isNetworkConnected()) {
                    if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        if (nomorValid) {
                            if (password1.length >= 8) {
                                firebaseAuth.createUserWithEmailAndPassword(email, password1)
                                    .addOnCompleteListener { signUpTask ->
                                        if (signUpTask.isSuccessful) {
                                            val userId = firebaseAuth.currentUser?.uid
                                            val userRef = firebaseFirestore.collection("user")
                                                .document(userId!!)
                                            val userData = hashMapOf(
                                                "nama" to nama, "email" to email, "nomor" to noTelp
                                            )
                                            userRef.set(userData).addOnSuccessListener {
                                                Toast.makeText(
                                                    this,
                                                    "Pendaftaran Akun Berhasil",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                val intent = Intent(this, LoginActivity::class.java)
                                                intent.flags =
                                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                finish()
                                            }.addOnFailureListener {
                                                Toast.makeText(
                                                    this,
                                                    "Gagal Membuat Akun : ${it.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                            } else {
                                binding.edPassword1.error = "Password Minimal 8 Karakter"
                            }
                        } else {
                            binding.edNohp.error = "Nomor Tidak Valid"
                        }

                    } else {
                        binding.edEmail.error = "Email Tidak Valid"
                    }

                } else {
                    Toast.makeText(
                        this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT
                    ).show()
                }

            } else {
                Toast.makeText(this, "Semua Form Harus Terisi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities != null && (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(
                    NetworkCapabilities.TRANSPORT_CELLULAR
                ))
    }

    private fun hapusError() {
        binding.emailForm.setOnClickListener {
            binding.edEmail.error = null
        }


        binding.noForm.setOnClickListener {
            binding.edNohp.error = null
        }

        binding.passForm.setOnClickListener {
            binding.edPassword1.error = null
        }
    }


}