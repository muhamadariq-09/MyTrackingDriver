package com.example.mytrackingdriver

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.example.mytrackingdriver.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()


        binding.txtRegis.setOnClickListener {
            val intent = Intent(this , RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmail.text.toString()
            val password = binding.edtPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {

                    if (it.isSuccessful) {
                        checkUser()
                    }else {
                        Toast.makeText(this , it.exception.toString() , Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else {
                Toast.makeText(this , "Empty Field are not allowed", Toast.LENGTH_SHORT).show()
            }
        }

        setupView()
    }

    private fun checkUser() {
        val firebaseUser = auth.currentUser!!

        val dbRef = FirebaseDatabase.getInstance().getReference("Driver")
        dbRef.child(firebaseUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    startActivity(Intent(this@LoginActivity , MainActivity::class.java))
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
    }

    override fun onStart() {
        super.onStart()

        if(auth.currentUser != null) {
            val intent = Intent(this , MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupView() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        supportActionBar?.hide()
    }
}