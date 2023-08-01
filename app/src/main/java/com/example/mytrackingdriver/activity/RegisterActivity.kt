package com.example.mytrackingdriver.activity

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.example.mytrackingdriver.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRegis.setOnClickListener {
            val name = binding.edtNama.text.toString()
            val email = binding.edtEmail.text.toString()
            val phone = binding.edtPhone.text.toString()
            val password = binding.edtPassword.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() && password.isNotEmpty()) {

                if(password.length >= 6) {

                    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->

                        if (task.isSuccessful) {
                            val userUpdate = userProfileChangeRequest {
                                displayName = name
                            }
                            val user = task.result.user
                            user!!.updateProfile(userUpdate)
                                .addOnSuccessListener {
                                    val timestamp = System.currentTimeMillis()
                                    val uid = auth.uid

                                    val hashMap: HashMap<String, Any?> = HashMap()

                                    hashMap["uid"] = uid
                                    hashMap["name"] = name
                                    hashMap["email"] = email
                                    hashMap["phone"] = phone
                                    hashMap["password"] = password
                                    hashMap["timestamp"] = timestamp

                                    val dbRef = FirebaseDatabase.getInstance().getReference("Driver")
                                    dbRef.child(uid!!)
                                        .setValue(hashMap)
                                        .addOnSuccessListener {
                                            val intent = Intent(this , LoginActivity::class.java)
                                            startActivity(intent)
                                        }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this , "Register Failed" , Toast.LENGTH_SHORT).show()
                                }

                        }else {
                            Toast.makeText(this , task.exception.toString() , Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this , "Password must be atleast 6 characters long", Toast.LENGTH_SHORT).show()
                }
            }
            else {
                Toast.makeText(this , "Empty Field are not allowed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.txtSignin.setOnClickListener {
            val intent = Intent(this , LoginActivity::class.java)
            startActivity(intent)
        }
        setupView()
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