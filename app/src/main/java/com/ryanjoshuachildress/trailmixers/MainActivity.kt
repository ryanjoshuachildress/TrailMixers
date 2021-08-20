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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnMemoryGame = findViewById(R.id.btnMemoryGame)

        btnMemoryGame.setOnClickListener{
            launchMemoryGame()
        }

    }

    private fun launchMemoryGame() {
        val intent = Intent(this, MemoryGameActivity::class.java)
        startActivity(intent)
    }
}