package com.example.webrtc2.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.webrtc2.R
import com.example.webrtc2.databinding.ActivityLoginBinding
import com.example.webrtc2.repository.MainRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var view: ActivityLoginBinding

    @Inject
    lateinit var mainRepository: MainRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        view = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(view.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
    }

    private fun init() {
        view.apply {
            loginButton.setOnClickListener {
                mainRepository.login(
                    view.usernameTextField.text.toString(),
                    view.passwordTextField.text.toString()
                ) { status, errorMessage ->
                    if (status) {
                        startActivity(
                            Intent(this@LoginActivity, MainActivity::class.java).putExtra(
                                "username",
                                view.usernameTextField.text.toString()
                            )
                        )
                    } else {
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}