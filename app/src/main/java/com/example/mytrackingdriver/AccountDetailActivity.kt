package com.example.mytrackingdriver

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import com.example.mytrackingdriver.databinding.ActivityAccountDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AccountDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountDetailBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        getUser()
        checkUser()

        binding.btnEdit.setOnClickListener {
            startActivity(Intent(this, EditAccountActivity::class.java))
            finish()
        }

        setupView()
    }

    private fun getUser() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Driver")
        dbRef.child(auth.uid!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val email = "${snapshot.child("email").value}"
                    val name = "${snapshot.child("name").value}"
                    val password = "${snapshot.child("password").value}"
                    val phone = "${snapshot.child("phone").value}"



                    binding.edtEmail.text = email
                    binding.edtNama.text = name
                    binding.edtPassword.text = password
                    binding.edtPhone.text = phone

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun checkUser() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupView(){
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