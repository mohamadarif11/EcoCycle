package com.example.ecocycle

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.example.ecocycle.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val homeFragment = HomeFragment()
    private val settingFragment = SettingFragment()
    private lateinit var homeItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        homeItem = binding.bottomNav.menu.findItem(R.id.home)
        firebaseAuth = FirebaseAuth.getInstance()

        openFragment(homeFragment)

        binding.bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    openFragment(homeFragment)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.setting -> {
                    openFragment(settingFragment)
                    return@setOnNavigationItemSelectedListener true
                }
            }
            false
        }
    }

    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.frame_container)
        if (fragment is HomeFragment) {
            if (firebaseAuth.currentUser != null) {
                finishAffinity()
            } else {
                finishAffinity()
            }
        } else {
            openFragment(homeFragment)
            homeItem.isChecked = true
        }
    }

}