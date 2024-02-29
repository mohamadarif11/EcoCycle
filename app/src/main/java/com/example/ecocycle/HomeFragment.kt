package com.example.ecocycle

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import com.example.ecocycle.databinding.FragmentHomeBinding
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LimbahAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var connectivityManager: ConnectivityManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        binding.btnPrediksi.setOnClickListener {
            val intent = Intent(requireContext(), PrediksiActivity::class.java)
            startActivity(intent)
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (isNetworkConnected()) {
                    if (!newText.isNullOrBlank()) {
                        adapter.setFilter(newText)
                        adapter.notifyDataSetChanged()
                        binding.btnPrediksi.visibility = View.GONE

                    } else {
                        adapter.setFilter(null)
                        binding.btnPrediksi.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(requireContext(), "Tidak Ada Koneksi Internet", Toast.LENGTH_SHORT).show()
                }
                return true
            }

        })

        setupRecyclerView()
        observeSearchResult()

    }

    private fun isNetworkConnected(): Boolean {
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun observeSearchResult() {
        adapter.hasResults.observe(viewLifecycleOwner) { hasResults ->
            if (hasResults) {
                binding.textNoResults.visibility = View.GONE
            } else {
                binding.textNoResults.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRecyclerView() {
        val query = firestore.collection("limbah").orderBy("judul")

        val options = FirestoreRecyclerOptions.Builder<Limbah>()
            .setQuery(query, Limbah::class.java)
            .build()

        adapter = LimbahAdapter(options)
        val layoutManager = WrapContentGridLayoutManager(requireContext(), 2)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter
        adapter.updateAllDataToLowercase()

        adapter.setOnItemClickListener { limbahId ->

            val intent = Intent(requireContext(), DetailActivity::class.java)
            intent.putExtra("limbahId", limbahId)
            startActivity(intent)

        }

    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onResume() {
        super.onResume()
        adapter.startListening()
    }

    override fun onPause() {
        super.onPause()
        adapter.stopListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}





