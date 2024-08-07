package com.example.businessbuddy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.businessbuddy.databinding.ActivitySignUpBinding
import com.example.businessbuddy.model.AdminData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database

class SignUp : AppCompatActivity(){
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var email:String
    private lateinit var name:String
    private lateinit var password:String
    private lateinit var phoneNumber:String
    private lateinit var googleSignInClient:GoogleSignInClient


    private val binding: ActivitySignUpBinding by lazy {
        ActivitySignUpBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)


        //Auth Initialization
        auth = FirebaseAuth.getInstance()

        //Database initialization
        database = Firebase.database.reference

        //Google Launcher setup
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,googleSignInOptions)


        binding.button2.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }

        binding.alreadyhavebutton.setOnClickListener {
            startActivity(Intent(this,Login::class.java))
        }

        binding.createAccountButton.setOnClickListener {
            email = binding.signUpEmail.text.toString()
            password = binding.signUpPassword.text.toString()
            name = binding.signUpName.text.toString()
            phoneNumber = binding.signUpPhone.text.toString()

            if(email.isBlank()||password.isBlank()||name.isBlank()||phoneNumber.isBlank()){
                Toast.makeText(this,"Please fill All the Details",Toast.LENGTH_SHORT).show()
            }
            else{
                createAccount(email,password)
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener {
                task->
            if(task.isSuccessful){
                Toast.makeText(this,"Account Created Successfully!",Toast.LENGTH_SHORT).show()
                saveUserData()
                startActivity(Intent(this,MainActivity::class.java))
                finish()
            }
            else{
                Toast.makeText(this, "Account Creation Failed!",Toast.LENGTH_SHORT).show()
                Log.d("Account","createAccount: Failure",task.exception)
            }
        }
    }

    private fun saveUserData() {
        ///Taking the data from the edit text
        email = binding.signUpEmail.text.toString()
        password = binding.signUpPassword.text.toString()
        name = binding.signUpName.text.toString()
        phoneNumber = binding.signUpPhone.text.toString()
        val userData = AdminData(name, email, password,phoneNumber)
        val userId= FirebaseAuth.getInstance().currentUser!!.uid

        ///Save the data to the Firebase
        database.child("admin").child(userId).setValue(userData)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result->    //lemda with name Result
        if(result.resultCode== Activity.RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if(task.isSuccessful) {
                val account: GoogleSignInAccount = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { authtask->
                    if(authtask.isSuccessful){
                        Toast.makeText(this, "Successfully Logged In with Google!",Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    else{
                        Toast.makeText(this, "Google SignIn Failed!",Toast.LENGTH_SHORT).show()
                        Log.d("Account Creation","createAccount: Failure",task.exception)
                    }
                }
            }
            else{
                Toast.makeText(this, "Google SignIn Failed!",Toast.LENGTH_SHORT).show()
                Log.d("GoogleSignIn", "Google signIn failed!", task.exception)
            }
        }
        else{
            Toast.makeText(this, "Google SignIn Failed!",Toast.LENGTH_SHORT).show()

        }

    }
}