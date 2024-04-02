package com.example.webrtc2.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.webrtc2.R
import com.example.webrtc2.adapters.MainCallViewAdapter
import com.example.webrtc2.databinding.ActivityMainBinding
import com.example.webrtc2.repository.MainRepository
import com.example.webrtc2.service.MainService
import com.example.webrtc2.service.MainServiceRepository
import com.example.webrtc2.utils.DataModel
import com.example.webrtc2.utils.DataModelType
import com.example.webrtc2.utils.getCameraAndMicPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainCallViewAdapter.Listener, MainService.Listener {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private var username: String? = null
    private var mainCallViewAdapter: MainCallViewAdapter? = null

    @Inject
    lateinit var mainRepository: MainRepository

    @Inject
    lateinit var mainServiceRepository: MainServiceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
    }

    fun init() {
        username = intent.getStringExtra("username")
        if (username == null)
            finish()
        subscribeObservers()
        startMyService()
    }

    private fun startMyService() {
        mainServiceRepository.startService(username!!)
    }

    private fun setupRecyclerView() {
        mainCallViewAdapter = MainCallViewAdapter(this)
        var layoutManager = LinearLayoutManager(this)
        binding.recyclerView.apply {
            setLayoutManager(layoutManager)
            adapter = mainCallViewAdapter
        }
    }

    private fun subscribeObservers() {
        setupRecyclerView()
        MainService.listener = this
        mainRepository.observeUsersStatus {
            Log.d(TAG, "subscriberObservers : $it")
            mainCallViewAdapter?.updateList(it)
        }
    }

    override fun onAudioCallClicked(username: String) {
        //check if permission of mic and audio is taken
        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username, false) {
                if (it) {
                    startActivity(
                        Intent(this@MainActivity, CallActivity::class.java).apply {
                            putExtra("target", username)
                            putExtra("isVideoCall", false)
                            putExtra("isCaller", true)
                        }
                    )
                }
            }
        }
    }

    override fun onVideoCallClicked(username: String) {
//check if permission of mic and audio is taken
        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username, true) {
                if (it) {
                    startActivity(
                        Intent(this@MainActivity, CallActivity::class.java).apply {
                            putExtra("target", username)
                            putExtra("isVideoCall", true)
                            putExtra("isCaller", true)
                        }
                    )
                }
            }
        }
    }

    override fun onCallReceived(data: DataModel) {
        runOnUiThread {
            binding.apply {
                val isVideoCall = data.type == DataModelType.StartVideoCall
                val isVideoCallText = if (isVideoCall) "video" else "Audio"
                incomingCalText.text = "${data.sender} is $isVideoCallText calling you.."
                incomingCalText.isVisible = true
                linearLayout.isVisible = true
                acceptButton.setOnClickListener {
                    getCameraAndMicPermission {
                        incomingCalText.isVisible = false
                        linearLayout.isVisible = false
                        startActivity(
                            Intent(this@MainActivity, CallActivity::class.java)
                                .apply {
                                    putExtra("target", data.sender)
                                    putExtra(
                                        "isVideoCall", isVideoCall
                                    )
                                    putExtra("isCaller", false)
                                }
                        )
                    }
                }
                declineButton.setOnClickListener {
                    incomingCalText.isVisible = false
                    linearLayout.isVisible = false
                }
            }
        }
    }
}