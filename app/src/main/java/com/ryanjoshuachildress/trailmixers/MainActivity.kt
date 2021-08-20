package com.ryanjoshuachildress.trailmixers

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ryanjoshuachildress.trailmixers.models.BoardSize
import com.ryanjoshuachildress.trailmixers.models.MemoryGame
import com.ryanjoshuachildress.trailmixers.models.UserImageList
import com.ryanjoshuachildress.trailmixers.utils.EXTRA_BOARD_SIZE
import com.ryanjoshuachildress.trailmixers.utils.EXTRA_GAME_NAME
import com.squareup.picasso.Picasso
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 248

    }

    private lateinit var memoryGame: MemoryGame
    private lateinit var rvBoard: RecyclerView
    private lateinit var clRoot: CoordinatorLayout
    private lateinit var adapter: MemoryBoardAdapter
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private var mAuth: FirebaseAuth? = null

    private var boardSize: BoardSize = BoardSize.EASY

    private val db = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null






    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth?.currentUser
        if(currentUser != null){
            setupBoard();
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)


        setupBoard()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.mi_refresh -> {
                if(memoryGame.getNumMoves()>0 && !memoryGame.haveWonGame()){
                    showAlertDialog("Qui your current game?",null,View.OnClickListener {
                        setupBoard()
                    })
                }else{
                setupBoard()
                }
                return true

            }
            R.id.mi_NewSize -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_Custom -> {
                showCreationDialog()
                return true

            }
            R.id.mi_dowload -> {
                showDownloadDialog()
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board, null)
        showAlertDialog("Fetch memory game", boardDownloadView, View.OnClickListener {
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val customGameName : String? =data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName == null) {
                Log.e(TAG, "Got null custom game from CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun downloadGame(CustomGameName: String) {
            db.collection("games").document(CustomGameName).get().addOnSuccessListener { document ->
                val userImageList = document.toObject(UserImageList::class.java)
                if(userImageList?.images == null){
                    Log.e(TAG, "Invalid  custom game from Firestore")
                    Snackbar.make(clRoot, "Sorry, we couldn't find any such game, '$CustomGameName", Snackbar.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val numCards = userImageList.images.size * 2
                boardSize = BoardSize.getByValue(numCards)
                customGameImages = userImageList.images
                for( imageUrl in userImageList.images) {
                    Picasso.get().load(imageUrl).fetch()
                }
                Snackbar.make(clRoot,"You're now playing '$CustomGameName'!", Snackbar.LENGTH_LONG).show()
                gameName = CustomGameName
                setupBoard()

            }.addOnFailureListener{  exception ->
                Log.e(TAG, "Exception when retrieving game", exception)

            }
        }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)

        showAlertDialog("Create your own memory board",boardSizeView,View.OnClickListener {
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId){

                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent,CREATE_REQUEST_CODE)

        })
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Chose New Size",boardSizeView,View.OnClickListener {
            boardSize = when (radioGroupSize.checkedRadioButtonId){

                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setupBoard()
        })
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("OK") {_,_ ->
                positiveClickListener.onClick(null)

            }.show()

    }

    private fun setupBoard()
    {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when(boardSize){
            BoardSize.EASY -> {
                tvNumMoves.text = "Easy: 4 x 2"
                tvNumPairs.text = "Pairs: 0 / 4"

            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Medium: 6 x 3"
                tvNumPairs.text = "Pairs: 0 / 9"

            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Hard: 6 x 4"
                tvNumPairs.text = "Pairs: 0 / 12"

            }
        }
        memoryGame = MemoryGame(boardSize, customGameImages)

        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))

        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, object: MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int){
                Log.i(TAG, "Card Clicked $position")
                updateGameWithFlip(position)
            }
        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this,boardSize.getWidth())

    }
    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.haveWonGame()){
            Snackbar.make(clRoot, "You already won", Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFaceUp(position)){
            Snackbar.make(clRoot, "Invalid move", Snackbar.LENGTH_SHORT).show()
            return
        }
        if(memoryGame.flipCard(position)) {
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full)
            ) as Int
            tvNumPairs.setTextColor(color)
            tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"

            if (memoryGame.haveWonGame()){
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.BLUE,Color.MAGENTA)).oneShot()
                Snackbar.make(clRoot, "You won! Congratulations", Snackbar.LENGTH_LONG).show()
            }
            Log.i(TAG, "Found a match! Num Pairs found: ${memoryGame.numPairsFound}")
        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}