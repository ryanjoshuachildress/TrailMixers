package com.ryanjoshuachildress.trailmixers

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.ryanjoshuachildress.trailmixers.models.BoardSize
import com.ryanjoshuachildress.trailmixers.utils.*
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "CreateActivity"
        private const val PICK_PHOTOS_REQUEST_CODE = 655
        private const val READ_EXTERNAL_PHOTOS_CODE = 345
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14

    }

    private lateinit var adaptor: ImagePickerAdaptor
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pbUploading: ProgressBar

    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()

    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)

        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics (0 / $numImagesRequired)"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btnSave.setOnClickListener{
            saveDataToFirebase()

        }

        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))

        etGameName.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                shouldEnableSaveButton()
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                shouldEnableSaveButton()
            }

            override fun afterTextChanged(p0: Editable?) {
                shouldEnableSaveButton()
            }

        })

        adaptor = ImagePickerAdaptor(this, chosenImageUris, boardSize, object: ImagePickerAdaptor.ImageClickListener {
            override fun onPlaceholderClicked() {
                if(isPermissionGranted(this@CreateActivity,  READ_PHOTOS_PERMISSION)) {
                    launchIntentForPhotos()
                } else
                {
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION,
                        READ_EXTERNAL_PHOTOS_CODE)
                }
            }

        })
        rvImagePicker.adapter = adaptor
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())

    }

    private fun saveDataToFirebase() {
        btnSave.isEnabled = false
        val customGameName = etGameName.text.toString()
        Log.i(TAG, "saveDataToFirebase")
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if(document != null && document.data != null){
                AlertDialog.Builder(this)
                    .setTitle("Name Taken")
                    .setMessage("A game already exists with the name '$customGameName'. Please choose another")
                    .setPositiveButton("Ok", null)
                    .show()
                btnSave.isEnabled = true

            }else{
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener{exception ->
            Log.e(TAG, "Encountered error while saving memory game", exception)
            Toast.makeText(this, "Encountered error while saving memory game",Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }
    }


    private fun handleImageUploading(gameName: String) {
        pbUploading.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()
        for ((index: Int, photoUri: Uri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "Images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference: StorageReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask ->
                    Log.i(
                        CreateActivity.TAG,
                        "Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}"
                    )
                    photoReference.downloadUrl

                }.addOnCompleteListener { downloadUrlTask ->
                    if (!downloadUrlTask.isSuccessful) {
                        Log.e(
                            CreateActivity.TAG,
                            "Exception with Firebase storage",
                            downloadUrlTask.exception
                        )
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        return@addOnCompleteListener

                    }
                    if (didEncounterError) {
                        pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    val downLoadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downLoadUrl)
                    pbUploading.progress = uploadedImageUrls.size *100 / chosenImageUris.size
                    Log.i(
                        CreateActivity.TAG,
                        "Finished uploading $photoUri, num Uploaded ${uploadedImageUrls.size}"
                    )
                    if (uploadedImageUrls.size == chosenImageUris.size) {
                        handleAllImagesUploaded(gameName, uploadedImageUrls)

                    }
                }

        }
    }

     private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {

        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                pbUploading.visibility = View.GONE
                if(!gameCreationTask.isSuccessful){
                    Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                    Toast.makeText(this, "Failed Game Creation", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG, "Successfully created $gameName")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! Let's play your game '$gameName")
                    .setPositiveButton("OK") { _,_ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }

    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap: Bitmap = if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source :ImageDecoder.Source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
             MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG,"Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG,"Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == READ_EXTERNAL_PHOTOS_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForPhotos()
            }else {
                Toast.makeText(this, "In order to create a custom game, you need to provide access to you photos", Toast.LENGTH_LONG).show()
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTOS_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTOS_REQUEST_CODE || resultCode != Activity.RESULT_OK || data == null){
            Log.w(TAG, "Did not get data back from the launched activity, user likley cancled the selection flow")
            return
        }
        val selectedUri: Uri? = data.data
        val clipData: ClipData? = data.clipData

        if(clipData != null) {
            Log.i(TAG, "clipData numImages ${clipData.itemCount}: $clipData")
            for(i in 0 until clipData.itemCount) {
                val clipItem: ClipData.Item = clipData.getItemAt(i)
                if (chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
        }else if (selectedUri != null){
            Log.i(TAG, "data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        adaptor.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics (${chosenImageUris.size} / $numImagesRequired)"
        btnSave.isEnabled = shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {

        if(chosenImageUris.size != numImagesRequired){
            return false
        }
      //  if(etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH) {
       //     return false
       // }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        if (etGameName.text.isBlank() || etGameName.text.length < 5) {
            return false
        }
        return super.onOptionsItemSelected(item)
    }
}