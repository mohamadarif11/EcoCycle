package com.example.ecocycle

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import com.example.ecocycle.databinding.ActivityUploadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

class UploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadBinding
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var storageReference: StorageReference
    private lateinit var firebaseAuth: FirebaseAuth
    private var selectedImageUri: Uri? = null
    private var selectedProfilImageUri: Uri? = null

    companion object {
        private const val AMBIL_GAMBAR = 1
        private const val AMBIL_GAMBAR_PROFIL = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Unggah Limbah"

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        storageReference = FirebaseStorage.getInstance().reference

        binding.ivProfil.setOnClickListener {
            openProfilImage()
        }

        binding.btnGambar.setOnClickListener {
            openImage()
        }

        binding.btnUpload.setOnClickListener {
            uploadLimbah()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AMBIL_GAMBAR && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            binding.ivLimbah.setImageURI(selectedImageUri)
        } else if (requestCode == AMBIL_GAMBAR_PROFIL && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedProfilImageUri = data.data
            binding.ivProfil.setImageURI(selectedProfilImageUri)
        }
    }

    private fun openImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, AMBIL_GAMBAR)
    }

    private fun openProfilImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, AMBIL_GAMBAR_PROFIL)
    }

    private fun uploadLimbah() {
        val nama = binding.edpemilik.text.toString()
        val nomor = binding.edno.text.toString()
        val judul = binding.edJudul.text.toString()
        val alamat = binding.edAlamat.text.toString()
        val deskripsi = binding.edDeskripsi.text.toString()

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo == null || !networkInfo.isConnected) {
            showErrorMessage("Tidak ada koneksi internet. Harap cek koneksi internet Anda")
            return
        }

        if (judul.isEmpty() || alamat.isEmpty() || nama.isEmpty() || nomor.isEmpty() || deskripsi.isEmpty() || selectedImageUri == null || selectedProfilImageUri == null) {
            showErrorMessage("Harap lengkapi semua informasi")
            return
        }

        val imageFile = UUID.randomUUID().toString()
        val imageRef = storageReference.child("images/$imageFile")

        val uploadTask = imageRef.putFile(selectedImageUri!!)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                val image = downloadUri.toString()

                val profilImageRef =
                    storageReference.child("profile_images/${UUID.randomUUID()}")
                val uploadProfilTask = profilImageRef.putFile(selectedProfilImageUri!!)
                uploadProfilTask.continueWithTask { profilTask ->
                    if (!profilTask.isSuccessful) {
                        profilTask.exception?.let {
                            throw it
                        }
                    }
                    profilImageRef.downloadUrl
                }.addOnCompleteListener { profilTask ->
                    if (profilTask.isSuccessful) {
                        val profilDownloadUri = profilTask.result
                        val fotoProfil = profilDownloadUri.toString()
                        val limbah =
                            Limbah(
                                alamat,
                                image,
                                judul,
                                deskripsi,
                                nama,
                                nomor,
                                fotoProfil,
                            )
                        firebaseFirestore.collection("limbah").add(limbah)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Upload Limbah Berhasil",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()

                            }.addOnFailureListener {
                                Toast.makeText(this, "Gagal Upload Limbah", Toast.LENGTH_SHORT)
                                    .show()

                            }
                    } else {
                        Toast.makeText(this, "Kesalahan saat upload gambar", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                Toast.makeText(this, "Data Tidak Valid", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}