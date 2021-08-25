package com.ryanjoshuachildress.trailmixers

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "LOGIN_ACTIVITY"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var tvLoggedInName: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

//    public override fun onStart() {
//        super.onStart()
//        // Check if user is signed in (non-null) and update UI accordingly.
//        val currentUser = auth.currentUser
//        if(currentUser != null){
//            tvLoggedInName.text = auth.currentUser.toString()
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tvLoggedInName = findViewById(R.id.tvLoggedInName)
        btnLogin = findViewById(R.id.btnlogin)
        btnRegister = findViewById(R.id.btnRegister)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        auth = Firebase.auth

        btnRegister.setOnClickListener{
            launchRegister()
        }
        btnLogin.setOnClickListener{
            tryLogin(etEmail.text.toString(), etPassword.text.toString())
        }
    }

    private fun tryLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }

    }

    private fun updateUI(user: FirebaseUser?) {
        if(user != null) {
            tvLoggedInName.text = user.email.toString()
        }
    }

    private fun launchRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}