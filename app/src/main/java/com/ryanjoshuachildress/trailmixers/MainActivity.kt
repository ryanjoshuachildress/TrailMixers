package com.ryanjoshuachildress.trailmixers

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.ryanjoshuachildress.trailmixers.utils.EXTRA_BOARD_SIZE

class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "MainActivity"

    }

    private lateinit var btnMemoryGame: Button
    private lateinit var btnLaunchAuth: Button

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnMemoryGame = findViewById(R.id.btnMemoryGame)

        btnMemoryGame.setOnClickListener{
            launchMemoryGame()
        }

        btnLaunchAuth = findViewById(R.id.btnLaunchAuth)

        btnLaunchAuth.setOnClickListener{
            launchAuth()
        }

    }

    private fun launchAuth() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun launchMemoryGame() {
        val intent = Intent(this, MemoryGameActivity::class.java)
        startActivity(intent)
    }
}