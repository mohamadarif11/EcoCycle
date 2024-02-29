    package com.example.ecocycle

    import android.content.Intent
    import androidx.appcompat.app.AppCompatActivity
    import android.os.Bundle
    import android.os.Handler

    class SplashScreenActivity : AppCompatActivity() {
        private val duration = 2000L

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_splash_screen)

            supportActionBar?.hide()

            val sharedPref = getSharedPreferences("login_pref", MODE_PRIVATE)
            val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

            Handler().postDelayed({
                val intent = if (isLoggedIn) {
                    Intent(this, MainActivity::class.java)
                } else {
                    Intent(this, LoginActivity::class.java)
                }
                startActivity(intent)
                finish()
            }, duration)
        }
    }
