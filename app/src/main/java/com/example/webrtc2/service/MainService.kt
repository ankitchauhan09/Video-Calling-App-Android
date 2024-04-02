    package com.example.webrtc2.service

    import android.annotation.SuppressLint
    import android.app.NotificationChannel
    import android.app.NotificationManager
    import android.app.Service
    import android.content.Intent
    import android.content.pm.ServiceInfo
    import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
    import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    import android.os.Build
    import android.os.IBinder
    import android.util.Log
    import android.widget.Toast
    import androidx.core.app.NotificationCompat
    import com.example.webrtc2.R
    import com.example.webrtc2.repository.MainRepository
    import com.example.webrtc2.utils.DataModel
    import com.example.webrtc2.utils.DataModelType
    import com.example.webrtc2.utils.isValid
    import com.example.webrtc2.webrtc.RTCAudioManager
    import dagger.hilt.android.AndroidEntryPoint
    import org.webrtc.SurfaceViewRenderer
    import javax.inject.Inject

    @AndroidEntryPoint
    class MainService : Service(), MainRepository.Listener {

        private val TAG = "MainService"


        private var isServiceRunning = false
        private var username: String? = null
        private lateinit var notificationManager: NotificationManager
        private var isPreviousCallStateVideo = true
        private lateinit var rtcAudioManager: RTCAudioManager


        companion object {
            var screenPermissionIntent: Intent? = null
            var listener: Listener? = null
            var localSurfaceView: SurfaceViewRenderer? = null
            var remoteSurfaceView: SurfaceViewRenderer? = null
            var endCallListener: EndCallListener? = null
        }

        interface EndCallListener {
            fun onCallEnded()
        }

        @Inject
        lateinit var mainRepository: MainRepository

        override fun onCreate() {
            super.onCreate()
            notificationManager = getSystemService(NotificationManager::class.java)
            rtcAudioManager = RTCAudioManager.create(this)
            rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        }

        override fun onBind(intent: Intent?): IBinder? {
            return null
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            intent?.let { incomingIntent ->
                when (incomingIntent.action) {
                    MainServiceAction.START_SERVICE.name -> handleStartService(incomingIntent)
                    MainServiceAction.SETUP_VIEWS.name -> handleSetupViews(incomingIntent)
                    MainServiceAction.END_CALL.name -> handleEndCall(incomingIntent)
                    MainServiceAction.SWITCH_CAMERA.name -> handleSwitchCamera(incomingIntent)
                    MainServiceAction.TOGGLE_AUDIO.name -> handleToggleAudio(incomingIntent)
                    MainServiceAction.TOGGLE_VIDEO.name -> handleToggleVideo(incomingIntent)
                    MainServiceAction.TOGGLE_AUDIO_DEVICE.name -> handleToggleAudioDevice(incomingIntent)
                    MainServiceAction.TOGGLE_SCREEN_SHARE.name -> handleToggleScreenShare(incomingIntent)
                    else -> Unit
                }
            }
            return START_STICKY
        }

        private fun handleToggleScreenShare(incomingIntent: Intent) {
            val isStarting = incomingIntent.getBooleanExtra("isStarting", true)
            if(isStarting){
    //            start screen share
            if(isPreviousCallStateVideo){
                mainRepository.toggleVideo(true)
            }
                mainRepository.setScreenCaptureIntent(screenPermissionIntent!!)
                mainRepository.toggleScreenShare(true)
            } else {
    mainRepository.toggleScreenShare(false)
                if(isPreviousCallStateVideo){
                    mainRepository.toggleVideo(false)
                }
            }
        }

        private fun handleToggleAudioDevice(incomingIntent: Intent) {

            val type = when (incomingIntent.getStringExtra("type")) {
                RTCAudioManager.AudioDevice.EARPIECE.name -> RTCAudioManager.AudioDevice.EARPIECE
                RTCAudioManager.AudioDevice.SPEAKER_PHONE.name -> RTCAudioManager.AudioDevice.SPEAKER_PHONE
                else -> null
            }
            type?.let {
                rtcAudioManager.setDefaultAudioDevice(it)
                rtcAudioManager.selectAudioDevice(it)

            }
        }

        private fun handleToggleVideo(incomingIntent: Intent) {
            val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted", true)
            this.isPreviousCallStateVideo = !shouldBeMuted
            mainRepository.toggleVideo(shouldBeMuted)
        }

        private fun handleToggleAudio(incomingIntent: Intent) {
            val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted", true)
            mainRepository.toggleAudio(shouldBeMuted)
        }

        private fun handleSwitchCamera(incomingIntent: Intent) {
            mainRepository.switchCamera()
        }

        private fun handleEndCall(incomingIntent: Intent) {
            //1. we have to send signal to other peer that the call has ended
            mainRepository.sendEndCall()
            //2. end our call process and restart our webrtc client
            endCallAndRestatRepository()
        }

        private fun endCallAndRestatRepository() {
            mainRepository.endCall()
            endCallListener?.onCallEnded()
            mainRepository.initWebrtcClient(username!!)
        }

        private fun handleSetupViews(incomingIntent: Intent) {
            val isCaller = incomingIntent.getBooleanExtra("isCaller", false)
            val isVideoCall = incomingIntent.getBooleanExtra("isVideoCall", true)
            val target = incomingIntent.getStringExtra("target")
            this.isPreviousCallStateVideo = isVideoCall
            mainRepository.setTarget(target!!)
            //initialize our widgets and start streaming our video and audio source

    //        mainRepository.setTarget(target!!)

            mainRepository.initLocalSurfaceView(localSurfaceView!!, isVideoCall)
            mainRepository.initRemoteSurfaceView(remoteSurfaceView!!)
            if (!isCaller) {
                mainRepository.startCall()
            }
    //        }
        }

        private fun handleStartService(incomingIntent: Intent) {
            //start our foreground service
            if (!isServiceRunning) {
                isServiceRunning = true
                username = incomingIntent.getStringExtra("username")
                startServiceWithNotification()

                mainRepository.listener = this
                mainRepository.initFirebase()
                Log.d("handle_username", "value : $username")
                mainRepository.initWebrtcClient(username!!)
            }
        }

        @SuppressLint("ForegroundServiceType")
        private fun startServiceWithNotification() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    "channel1", "foreground", NotificationManager.IMPORTANCE_HIGH
                )

                notificationManager.createNotificationChannel(notificationChannel)

                val notification = NotificationCompat.Builder(
                    this, "channel1"
                ).setSmallIcon(R.mipmap.ic_launcher)
                startForeground(1, notification.build())

            }
        }

        override fun onLatestEventReceived(data: DataModel) {
            if (data.isValid()) {
                when (data.type) {
                    DataModelType.StartAudioCall,
                    DataModelType.StartVideoCall -> {
                        listener?.onCallReceived(data)
                    }

                    else -> Unit
                }
            }
        }

        override fun endCall() {
            //we are receiving end call signal from remote
            endCallAndRestatRepository()
        }


        interface Listener {
            fun onCallReceived(data: DataModel)
        }
    }