package com.example.ecocycle

import com.google.firebase.firestore.DocumentSnapshot

object InfoLimbah {

    fun getDataLimbah(label: String, document: DocumentSnapshot): DataLimbah? {
        val requiredProperties = when (label) {
            "Sampah Plastik", "Sampah Besi", "Sampah Kayu", "Sampah Organik", "Non Recycle" -> listOf(
                "deskripsi1", "deskripsi2", "keterangan", "deskripsi_artikel",
                "deskripsi_video", "url_artikel", "url_video",
            )
            else -> null
        }

        return requiredProperties?.let { props ->
            buildDataLimbah(document, props)
        }
    }

    private fun buildDataLimbah(document: DocumentSnapshot, properties: List<String>): DataLimbah {
        return DataLimbah(
            properties.map { document.getString(it) }.toTypedArray()
        )
    }

    data class DataLimbah(
        val properties: Array<String?>
    )

}