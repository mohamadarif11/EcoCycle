package com.example.ecocycle

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.ecocycle.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private val kamera = 1
    private val galeri = 2
    private var previousProfileImageUri: Uri? = null
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profil"

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        val currentUser = firebaseAuth.currentUser
        val userDocRef = currentUser?.uid?.let {
            firebaseFirestore.collection("user").document(it)
        }

        previousProfileImageUri = loadPreviousProfileImageUriFromFirestore()

        userDocRef?.get()?.addOnSuccessListener {
            if (it.exists()) {
                val nama = it.getString("nama")
                val noHp = it.getString("nomor")
                val fotoProfil = it.getString("profileGambarUrl")

                binding.edtNama.setText(nama)
                binding.edtNomorTelepon.setText(noHp)

                if (!fotoProfil.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(fotoProfil)
                        .into(binding.profileImage)
                    binding.profileImage.tag = fotoProfil
                }
            }
        }

        binding.btnGantiProfil.setOnClickListener {
            pilihSumberGambar()
        }

        binding.btnSimpan.setOnClickListener {
            simpanPerubahan()
        }
    }

    private fun pilihSumberGambar() {
        val options = arrayOf("Kamera", "Galeri")

        AlertDialog.Builder(this)
            .setTitle("Pilih Sumber Gambar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> ambilDenganKamera()
                    1 -> ambilDariGaleri()
                }
            }
            .show()
    }

    private fun ambilDenganKamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, kamera)
        } else {
            Toast.makeText(this, "Aplikasi kamera tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ambilDariGaleri() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, galeri)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                kamera -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap?
                    if (imageBitmap != null) {
                        selectedImageUri = getImageUriFromBitmap(imageBitmap)
                        Glide.with(this).load(selectedImageUri).into(binding.profileImage)
                    }
                }

                galeri -> {
                    selectedImageUri = data?.data
                    Glide.with(this).load(selectedImageUri).into(binding.profileImage)
                }
            }
        }
    }

    private fun simpanPerubahan() {
        selectedImageUri?.let { uri ->
            gantiFotoProfil(uri) {
                simpanDataPengguna()
            }
        }
    }

    private fun simpanDataPengguna() {
        val namaBaru = binding.edtNama.text.toString()
        val nomorBaru = binding.edtNomorTelepon.text.toString()
        val gambarProfilBaru =  binding.profileImage.tag?.toString() ?: ""

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userRef = firebaseFirestore.collection("user").document(currentUser.uid)
            val userData = mutableMapOf<String, Any>()
            userData["nama"] = namaBaru
            userData["nomor"] = nomorBaru
            userData["profileGambarUrl"] = gambarProfilBaru

            if (isNetworkConnected()) {
                userRef.update(userData).addOnSuccessListener {
                    Toast.makeText(this, "Perubahan disimpan", Toast.LENGTH_SHORT).show()

                    val resultIntent = Intent()
                    resultIntent.putExtra("nama", namaBaru)
                    resultIntent.putExtra("nomor", nomorBaru)
                    resultIntent.putExtra("profileGambarUrl", gambarProfilBaru)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Gagal menyimpan perubahan: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun gantiFotoProfil(gambarUri: Uri, callback: () -> Unit) {
        val storageReference = FirebaseStorage.getInstance().reference
        val imageRef = storageReference.child("profile_images/${firebaseAuth.currentUser?.uid}.jpg")

        val byte = ByteArrayOutputStream()
        val file = MediaStore.Images.Media.getBitmap(contentResolver, gambarUri)
        file.compress(Bitmap.CompressFormat.JPEG, 100, byte)
        val imageData = byte.toByteArray()

        val uploadTask = imageRef.putBytes(imageData)
        uploadTask.addOnSuccessListener { uploadTaskSnapshot ->
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val gambarUrl = uri.toString()

                simpanUrlGambar(gambarUrl) {
                    callback.invoke()
                }
            }
        }
    }

    private fun simpanUrlGambar(gambarUrl: String, callback: () -> Unit) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val userRef = firebaseFirestore.collection("user").document(userId)
            val userData = hashMapOf<String, Any?>("profileGambarUrl" to gambarUrl)

            userRef.update(userData)
                .addOnSuccessListener {
                    binding.profileImage.tag = gambarUrl
                    callback.invoke()
                }
        }
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            contentResolver,
            bitmap,
            "Title",
            null
        )
        return Uri.parse(path)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                confirmBatalPerubahan()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        confirmBatalPerubahan()
    }

    private fun confirmBatalPerubahan() {
        AlertDialog.Builder(this)
            .setTitle("Batalkan Perubahan")
            .setMessage("Apakah Anda yakin ingin membatalkan perubahan?")
            .setPositiveButton("Ya") { _, _ ->
                finish()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private fun loadPreviousProfileImageUriFromFirestore(): Uri? {
        return previousProfileImageUri
    }

}