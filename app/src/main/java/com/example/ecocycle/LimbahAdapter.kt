package com.example.ecocycle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecocycle.databinding.ItemLimbahBinding
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class LimbahAdapter(options: FirestoreRecyclerOptions<Limbah>) :
    FirestoreRecyclerAdapter<Limbah, LimbahAdapter.ViewHolder>(options) {

    val hasResults = MutableLiveData<Boolean>()

    override fun onDataChanged() {
        super.onDataChanged()
        hasResults.value = itemCount > 0
    }

    private var onItemClickListener: ((String) -> Unit)? = null

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LimbahAdapter.ViewHolder {
        val binding = ItemLimbahBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LimbahAdapter.ViewHolder, position: Int, model: Limbah) {
        holder.bind(model)
    }

    inner class ViewHolder(private val binding: ItemLimbahBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(limbah: Limbah) {
            binding.tvAlamat.text = limbah.alamat
            Glide.with(binding.root)
                .load(limbah.image)
                .into(binding.ivCompany)

            val judul = limbah.judul.capitalize()
            binding.tvJudul.text = judul

            itemView.setOnClickListener {
                onItemClickListener?.invoke(snapshots.getSnapshot(adapterPosition).id)
            }

        }
    }

    fun updateAllDataToLowercase() {
        val limbahCollection = FirebaseFirestore.getInstance().collection("limbah")

        limbahCollection.get().addOnSuccessListener { documents ->
            for (document in documents) {
                document.reference.update("judul", (document.get("judul") as? String)?.lowercase())
            }
        }
    }

    fun setFilter(query: String?) {
        val filteredOptions = if (!query.isNullOrBlank()) {
            FirestoreRecyclerOptions.Builder<Limbah>()
                .setQuery(getFilteredQuery(query), Limbah::class.java)
                .build()
        } else {
            FirestoreRecyclerOptions.Builder<Limbah>()
                .setQuery(getBaseQuery(), Limbah::class.java)
                .build()
        }
        updateOptions(filteredOptions)
    }

    private fun getFilteredQuery(query: String): Query {
        val baseQuery = FirebaseFirestore.getInstance().collection("limbah")
        return baseQuery
            .whereGreaterThanOrEqualTo("judul", query)
            .whereLessThanOrEqualTo("judul", query + "\uf8ff")
    }

    private fun getBaseQuery(): Query {
        return FirebaseFirestore.getInstance().collection("limbah").orderBy("judul")
    }
}
