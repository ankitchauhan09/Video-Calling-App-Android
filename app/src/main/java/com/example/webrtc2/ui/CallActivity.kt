package com.example.webrtc2.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.webrtc2.R
import com.example.webrtc2.databinding.ActivityCallBinding
import com.example.webrtc2.service.MainService
import com.example.webrtc2.service.MainServiceRepository
import com.example.webrtc2.webrtc.RTCAudioManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallActivity : AppCompatActivity(), MainService.EndCallListener {

    private lateinit var binding: ActivityCallBinding

    private var target: String? = null
    private var isVideoCall: Boolean = true
    private var isCaller: Boolean = true

    private var isMicrophoneMuted = false
    private var isCameraMuted = false
    private var isSpeakerMode = true
    private var isScreenCasting = false
    private lateinit var requestScreenCaptureLauncher : ActivityResultLauncher<Intent>
    @Inject
    lateinit var serviceRepository: MainServiceRepository

    override fun onStart() {
        super.onStart()
//        requestScreenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
//            result ->
//            if(result.resultCode == Activity.RESULT_OK){
//                val intent = result.data
//                MainService.screenPermissionIntent = intent
//                isScreenCasting = true
//                updateUiToScreenCaptureIsOn()
//                serviceRepository.toggleScreenShare(true)
//            }
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        init()
    }

    private fun init() {
        intent.getStringExtra("target")?.let {
            this.target = it
        } ?: kotlin.run {
            finish()
        }

        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        isCaller = intent.getBooleanExtra("isCaller", true)

        binding.apply {
            if (!isVideoCall) {
                toggleCameraButton.isVisible = false
//                screenShareButton.isVisible = false
                switchCameraButton.isVisible = false
            }

            MainService.remoteSurfaceView = remoteView
            MainService.localSurfaceView = localView

            serviceRepository.setupViews(isVideoCall, isCaller, target!!)

            endCallButton.setOnClickListener {
                serviceRepository.sendEndCall()
            }

            switchCameraButton.setOnClickListener {
                serviceRepository.switchCamera()
            }

        }

        setUpCameraToggleClicked()
        setUpMicToggleClicked()
        setupToggleAudioDevice()
//        setupScreenCasting()
        MainService.endCallListener = this
    }

    private fun setUpMicToggleClicked() {
        binding.apply {
            toggleMicrophoneButton.setOnClickListener {
                if (!isMicrophoneMuted) {
                    //we should mute our mic
                    //1. send command to repository
                    serviceRepository.toggleAudio(false)
                    isMicrophoneMuted = true
                    //2.update ui to mic is muted
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_on)
                } else {
                    //we should set it back to normal
                    //1. send commant to repository to make it back to normal status

                    serviceRepository.toggleAudio(true)
                    isMicrophoneMuted = false
                    //2. update ui
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_off)
                }
            }


        }
    }

    private fun setUpCameraToggleClicked() {
        binding.apply {
            toggleCameraButton.setOnClickListener {
                if (!isCameraMuted) {
                    serviceRepository.toggleVideo(true)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_on)
                } else {
                    serviceRepository.toggleVideo(false)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_off)
                }
                isCameraMuted = !isCameraMuted
            }
        }
    }

    private fun setupToggleAudioDevice() {
        binding?.apply {
            toggleAudioDevice.setOnClickListener {
                if (isSpeakerMode) {
                    //we should set it to earpiece
                    toggleAudioDevice.setImageResource(R.drawable.ic_speaker)
                    //command to switch between source
                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.EARPIECE.name)
                } else {
                    toggleAudioDevice.setImageResource(R.drawable.ic_ear)
                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE.name)
                }
                isSpeakerMode = !isSpeakerMode
            }
        }
    }

//    private fun setupScreenCasting() {
//        binding.apply {
//            screenShareButton.setOnClickListener {
//                if (!isScreenCasting){
//                    //we have to start casting
//                    AlertDialog.Builder(this@CallActivity)
//                        .setTitle("Screen Casting")
//                        .setMessage("You sure to start casting ?")
//                        .setPositiveButton("Yes"){dialog,_ ->
//                            //start screen casting process
//                            startScreenCapture()
//                            dialog.dismiss()
//                        }.setNegativeButton("No") {dialog,_ ->
//                            dialog.dismiss()
//                        }.create().show()
//                }else{
//                    //we have to end screen casting
//                    isScreenCasting = false
//                    updateUiToScreenCaptureIsOff()
//                    serviceRepository.toggleScreenShare(false)
//                }
//            }
//
//        }
//    }
//
//    private fun startScreenCapture() {
//        val mediaProjectionManager = application.getSystemService(
//            Context.MEDIA_PROJECTION_SERVICE
//        ) as MediaProjectionManager
//
//        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
//        requestScreenCaptureLauncher.launch(captureIntent)
//
//    }

    private fun updateUiToScreenCaptureIsOn(){
        binding.apply {
            localView.isVisible = false
            switchCameraButton.isVisible = false
            toggleCameraButton.isVisible = false
//            screenShareButton.setImageResource(R.drawable.ic_stop_screen_share)
        }

    }

    private fun updateUiToScreenCaptureIsOff() {
        binding.apply {
            localView.isVisible = true
            switchCameraButton.isVisible = true
            toggleCameraButton.isVisible = true
//            screenShareButton.setImageResource(R.drawable.ic_screen_share)
        }
    }


    override fun onCallEnded() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        MainService.remoteSurfaceView?.release()
        MainService.remoteSurfaceView = null
        MainService.localSurfaceView?.release()
        MainService.localSurfaceView = null
    }


}