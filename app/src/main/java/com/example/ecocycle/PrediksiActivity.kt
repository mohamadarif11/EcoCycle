package com.example.ecocycle

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.ecocycle.databinding.ActivityPrediksiBinding
import com.google.firebase.firestore.FirebaseFirestore
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PrediksiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrediksiBinding
    private lateinit var tflite: Interpreter
    private var pilihGambar: Bitmap? = null

    companion object {
        private const val AMBIL_GAMBAR = 2
        private const val KAMERA_REQUEST_CODE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrediksiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Deteksi Limbah"

        binding.btnGaleri.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, AMBIL_GAMBAR)
        }
        val db = FirebaseFirestore.getInstance()

        binding.btnKamera.setOnClickListener {
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)

            if (intent.resolveActivity(packageManager) != null) {

                startActivityForResult(intent, KAMERA_REQUEST_CODE)
            } else {
                Toast.makeText(this, "Aplikasi kamera tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPredictLimbah.setOnClickListener {
            if (pilihGambar != null) {
                predictWaste(pilihGambar!!)

                val predictedLabel = binding.resultPrediksi.text.toString()

                getUrlGambarTong(predictedLabel,
                    onSuccess = { urlGambarTong ->
                        Glide.with(this)
                            .load(urlGambarTong)
                            .into(binding.ivTong)
                    },
                    onError = {
                        binding.ivTong.visibility = View.GONE
                    }
                )

                getUrlGambarKonten(predictedLabel,
                    { urlGambar1, urlGambar2 ->
                        if (urlGambar1.isNotEmpty()) {
                            Glide.with(this)
                                .load(urlGambar1)
                                .into(binding.imageArtikel)
                        }

                        if (urlGambar2.isNotEmpty()) {
                            Glide.with(this)
                                .load(urlGambar2)
                                .into(binding.imageVideo)
                        }

                    },

                    {
                        Toast.makeText(
                            this,
                            "Konten Gambar Artikel dan Video Kosong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                val collectionRef = db.collection("waste_management")
                val documentRef = collectionRef.document(predictedLabel)

                documentRef.get().addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val dataLimbah = InfoLimbah.getDataLimbah(predictedLabel, document)

                        if (dataLimbah != null) {
                            showWasteManagementInfo(
                                dataLimbah.properties[0] ?: "", // deskripsi1
                                dataLimbah.properties[1] ?: "", // deskripsi2
                                dataLimbah.properties[2] ?: "", // keterangan
                                dataLimbah.properties[3] ?: "", // deskripsiArtikel
                                dataLimbah.properties[4] ?: "", // deskripsiVideo
                                dataLimbah.properties[5] ?: "", // urlArtikel
                                dataLimbah.properties[6] ?: ""  // urlVideo
                            )
                        } else {
                            Toast.makeText(
                                this,
                                "Informasi pengelolaan sampah tidak tersedia",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(this, "Dokumen tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Gagal mengambil data: $exception", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this, "Pilih atau ambil gambar terlebih dahulu", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        try {
            tflite = Interpreter(loadModelFile())
        } catch (e: IOException) {
            e.printStackTrace()
        }

        binding.btnInfo.setOnClickListener {
            val predictedLabel = binding.resultPrediksi.text.toString()

            if (predictedLabel.isEmpty()) {
                Toast.makeText(this, "Lokasi Tidak Tersedia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val collectionRef = db.collection("waste_management")
            val documentRef = collectionRef.document(predictedLabel)

            documentRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {

                    val coordinatesList: List<String>? = getCoordinatesForWasteType(predictedLabel)
                    if (coordinatesList != null) {
                        for (coordinates in coordinatesList) {
                            val gmmIntentUri =
                                Uri.parse("geo:$coordinates?q=$predictedLabel")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            startActivity(mapIntent)
                        }
                    }
                }
            }
        }

    }

    private fun getCoordinatesForWasteType(wasteType: String): List<String>? {
        return when (wasteType) {
            "Sampah Organik" -> listOf(
                "-6.980530652245236, 107.6309643074904",
                "-7.002040262977176, 107.53254354403096",
                "-6.989261506260085, 107.56567419005793"
            )

            "Sampah Plastik" -> listOf(
                "-7.021999259098582, 107.33698491331306",
                "-6.85568767465889, 107.5310981203401",
                "-6.915676651325288, 107.55513071331303"
            )

            "Sampah Kayu" -> listOf(
                "-6.909485366003757, 107.58053525879336",
                "-6.8685840790572446, 107.55718931133394",
                "-6.784043629631465, 107.64302000052304"
            )

            "Sampah Besi" -> listOf(
                "-7.07101046263962, 107.72321176012386",
                "-6.926526324038491, 107.65111398120499",
                "-6.442246390490268, 107.12628148406078"
            )

            "Non Recycle" -> listOf(
                "-6.89920206322882, 107.72939023182545",
                "-6.475319752145567, 106.96067808639415",
                "-6.555273789003339, 106.6981512434752"
            )

            else -> null
        }
    }

    private fun showWasteManagementInfo(
        deskripsi1: String,
        deskripsi2: String,
        keterangan: String,
        deskripsiArtikel: String,
        deskripsiVideo: String,
        urlArtikel: String?,
        urlVideo: String?
    ) {
        val info = "$deskripsi1\n\n$deskripsi2"
        binding.tvWasteManagementInfo.text = info
        binding.keteranganLimbah.text = keterangan
        binding.deskripsiArtikel.text = deskripsiArtikel
        binding.deskripsiVideo.text = deskripsiVideo

        binding.btnInfo.visibility = View.VISIBLE

        binding.cardArtikel.visibility =
            if (deskripsiArtikel.isNotEmpty()) View.VISIBLE else View.GONE
        binding.cardArtikel.setOnClickListener {
            if (!urlArtikel.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlArtikel))
                startActivity(intent)
            } else {
                Toast.makeText(this, "URL artikel tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardVideo.visibility =
            if (deskripsiVideo.isNotEmpty()) View.VISIBLE else View.GONE
        binding.cardVideo.setOnClickListener {
            if (!urlVideo.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlVideo))
                startActivity(intent)
            } else {
                Toast.makeText(this, "URL video tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tflite.close()
    }

    private fun loadModelFile(): MappedByteBuffer {
        val modelPath = "model.tflite"
        val assetManager = assets
        val fileDescriptor = assetManager.openFd(modelPath)
        val fileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun predictWaste(bitmap: Bitmap) {
        if (!::tflite.isInitialized) {
            Toast.makeText(this, "Model TFLite belum dimuat", Toast.LENGTH_SHORT).show()
            return
        }

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val inputBuffer = ByteBuffer.allocateDirect(224 * 224 * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.rewind()
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resizedBitmap.getPixel(x, y)
                inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
                inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
                inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
            }
        }

        val output = Array(1) { FloatArray(5) }
        tflite.run(inputBuffer, output)

        val probabilities = output[0]
        val labels =
            arrayOf("Sampah Organik", "Sampah Plastik", "Sampah Kayu", "Sampah Besi", "Non Recycle")
        var maxIndex = 0
        var maxPeluang = 0.0f
        for (i in probabilities.indices) {
            if (probabilities[i] > maxPeluang) {
                maxIndex = i
                maxPeluang = probabilities[i]
            }
        }
        val predictedLabel = labels[maxIndex]

        binding.resultPrediksi.text = predictedLabel

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            AMBIL_GAMBAR -> {
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        val selectedImageUri = data.data
                        pilihGambar = BitmapFactory.decodeStream(selectedImageUri?.let {
                            contentResolver.openInputStream(
                                it
                            )
                        })
                        binding.ivPredictLimbah.setImageBitmap(pilihGambar)
                    }
                }
            }

            KAMERA_REQUEST_CODE -> {
                if (resultCode == RESULT_OK && data != null) {
                    val imageBitmap = data.extras?.get("data") as Bitmap
                    pilihGambar = imageBitmap
                    binding.ivPredictLimbah.setImageBitmap(pilihGambar)
                }
            }
        }
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

    private fun getUrlGambarTong(label: String, onSuccess: (String) -> Unit, onError: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("waste_management")
        val documentRef = collectionRef.document(label)
        documentRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val urlGambarTong = document.getString("gambar_tong")
                onSuccess(urlGambarTong ?: "")
            } else {
                onError()
            }
        }.addOnFailureListener {
            onError()
        }
    }

    private fun getUrlGambarKonten(
        label: String,
        onSuccess: (String, String) -> Unit,
        onError: () -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("waste_management")
        val documentRef = collectionRef.document(label)
        documentRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val urlGambar1 = document.getString("gambar_artikel")
                val urlGambar2 = document.getString("gambar_video")
                if (urlGambar1 != null && urlGambar2 != null) {
                    onSuccess(urlGambar1, urlGambar2)
                } else {
                    onError()
                }
            } else {
                onError()
            }
        }.addOnFailureListener {
            onError()
        }
    }
}