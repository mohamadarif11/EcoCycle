package com.example.ecocycle

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.ecocycle.databinding.FragmentSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class SettingFragment : Fragment() {
    private lateinit var binding: FragmentSettingBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore

    private var cachedNama: String? = null
    private var cachedNomor: String? = null
    private var cachedGambarUrl: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?

    ): View? {
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        binding.cardUpload.setOnClickListener {
            val intent = Intent(requireContext(), UploadActivity::class.java)
            startActivity(intent)
        }

        binding.cardLogout.setOnClickListener {
            firebaseAuth.signOut()

            val sharedPref = requireContext().getSharedPreferences("login_pref", MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean("isLoggedIn", false)
            editor.apply()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {

            displayCachedData()

            val userRef = firebaseFirestore.collection("user").document(userId)
            userRef.get().addOnSuccessListener {
                if (it.exists()) {

                    displayDataFromFirebase(it)

                }
            }
        }

        binding.btnEdit.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val namaBaru = data?.getStringExtra("nama")
            val nomorTeleponBaru = data?.getStringExtra("nomorTelepon")
            val gambarProfilBaru = data?.getStringExtra("profileGambarUrl")

            binding.profileName.text = namaBaru
            binding.profileNomor.text = nomorTeleponBaru

            if (!gambarProfilBaru.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(gambarProfilBaru)
                    .into(binding.profileImage)
            }
        }
    }

    private fun displayCachedData() {

        displayData(cachedNama, cachedNomor, cachedGambarUrl)
    }

    private fun displayDataFromFirebase(documentSnapshot: DocumentSnapshot) {

        val nama = documentSnapshot.getString("nama")
        val nomor = documentSnapshot.getString("nomor")
        val gambarUrl = documentSnapshot.getString("profileGambarUrl")


        cachedNama = nama
        cachedNomor = nomor
        cachedGambarUrl = gambarUrl


        displayData(nama, nomor, gambarUrl)
    }

    private fun displayData(nama: String?, nomor: String?, gambarUrl: String?) {
        binding.profileName.text = nama
        binding.profileNomor.text = nomor

        if (!gambarUrl.isNullOrEmpty()) {
            Glide.with(requireContext()).load(gambarUrl).into(binding.profileImage)
        }
    }


    override fun onResume() {
        super.onResume()

        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val userRef = firebaseFirestore.collection("user").document(userId)
            userRef.get().addOnSuccessListener {
                if (it.exists()) {
                    val nama = it.getString("nama")
                    val nomor = it.getString("nomor")

                    binding.profileName.text = nama
                    binding.profileNomor.text = nomor

                    val gambarUrl = it.getString("profileGambarUrl")
                    if (!gambarUrl.isNullOrEmpty()) {
                        Glide.with(requireContext()).load(gambarUrl)
                            .into(binding.profileImage)
                    }
                }
            }
        }
    }
}