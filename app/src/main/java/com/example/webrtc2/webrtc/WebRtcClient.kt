    package com.example.webrtc2.webrtc

    import android.content.Context
    import android.content.Intent
    import android.media.projection.MediaProjection
    import android.util.DisplayMetrics
    import android.util.Log
    import android.view.WindowManager
    import com.example.webrtc2.utils.DataModel
    import com.example.webrtc2.utils.DataModelType
    import com.google.gson.Gson
    import org.webrtc.AudioTrack
    import org.webrtc.Camera2Enumerator
    import org.webrtc.CameraVideoCapturer
    import org.webrtc.DefaultVideoDecoderFactory
    import org.webrtc.DefaultVideoEncoderFactory
    import org.webrtc.EglBase
    import org.webrtc.IceCandidate
    import org.webrtc.MediaConstraints
    import org.webrtc.MediaStream
    import org.webrtc.PeerConnection
    import org.webrtc.PeerConnectionFactory
    import org.webrtc.ScreenCapturerAndroid
    import org.webrtc.SessionDescription
    import org.webrtc.SurfaceTextureHelper
    import org.webrtc.SurfaceViewRenderer
    import org.webrtc.VideoCapturer
    import org.webrtc.VideoTrack
    import java.lang.IllegalStateException
    import javax.inject.Inject
    import javax.inject.Singleton

        @Singleton
        class WebRtcClient @Inject constructor(
            private val context: Context,
            private val gson: Gson
        ) {

            private lateinit var username: String

            //class variables
            var listener: Listener? = null

            //webrtc variables
            private val eglBaseContext = EglBase.create().eglBaseContext
            private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
            private var peerConnection: PeerConnection? = null
            private val iceServer = listOf(
                PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
                    .setUsername("83eebabf8b4cce9d5dbcb649")
                    .setPassword("2D7JvfkOQtBdYW3R").createIceServer()
            )
            private var surfaceTextureHelper: SurfaceTextureHelper? = null
            private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
            private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
            private val videoCapturer = getVideoCapturer(context)

            //call variables
            private var localStream: MediaStream? = null
            private lateinit var localSurfaceView: SurfaceViewRenderer
            private lateinit var remoteSurfaceView: SurfaceViewRenderer
            private var localTrackId = ""
            private var localStreamId = ""
            private var localAudioTrack: AudioTrack? = null
            private var localVideoTrack: VideoTrack? = null
            private val mediaConstraint = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            }


            private var permissionIntent: Intent? = null
            private var screenCapturer: VideoCapturer? = null
            private val localScreenVideoSource by lazy {
                peerConnectionFactory.createVideoSource(false)
            }
            private var localScreenShareTrack : VideoTrack?= null

            init {
                initPeerConnectionFactory()
            }

            private fun initPeerConnectionFactory() {
                val options = PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                    .createInitializationOptions()

                PeerConnectionFactory.initialize(options)
            }

            private fun createPeerConnectionFactory(): PeerConnectionFactory {
                return PeerConnectionFactory.builder()
                    .setVideoDecoderFactory(
                        DefaultVideoDecoderFactory(eglBaseContext)
                    )
                    .setVideoEncoderFactory(
                        DefaultVideoEncoderFactory(eglBaseContext, true, true)
                    ).setOptions(PeerConnectionFactory.Options().apply {
                        disableEncryption = false
                        disableNetworkMonitor = false
                    }).createPeerConnectionFactory()
            }

            fun initRemoteSurface(view: SurfaceViewRenderer) {
                this.remoteSurfaceView = view
                initSurfaceView(view)
            }


            fun initializeWebrtcClient(
                username: String, observer: PeerConnection.Observer
            ) {
                this.username = username
                localTrackId = "${username}_track"
                localStreamId = "${username}_stream"
                peerConnection = createPeerConnection(observer)
            }

            private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
                return peerConnectionFactory.createPeerConnection(iceServer, observer)
            }

            private fun initSurfaceView(view: SurfaceViewRenderer) {
                view.run {
                    setMirror(false)
                    setEnableHardwareScaler(true)
                    init(eglBaseContext, null)
                }
            }

            fun initLocalSurfaceView(local: SurfaceViewRenderer, isVideoCall: Boolean) {
                this.localSurfaceView = local
                initSurfaceView(local)
                startLocalStreaming(local, isVideoCall)
            }

            private fun startLocalStreaming(local: SurfaceViewRenderer, videoCall: Boolean) {
                localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
                if (videoCall) {
                    startCapturingCamera(local)
                }
                localAudioTrack =
                    peerConnectionFactory.createAudioTrack(localTrackId + "_audio", localAudioSource)
                localStream?.addTrack(localAudioTrack)
                peerConnection?.addStream(localStream)
            }

            private fun startCapturingCamera(local: SurfaceViewRenderer) {
                surfaceTextureHelper = SurfaceTextureHelper.create(
                    Thread.currentThread().name, eglBaseContext
                )

                videoCapturer.initialize(
                    surfaceTextureHelper, context, localVideoSource.capturerObserver
                )
                videoCapturer.startCapture(720, 480, 20)

                localVideoTrack =
                    peerConnectionFactory.createVideoTrack(localTrackId + "_video", localVideoSource)
                localVideoTrack?.addSink(local)
                localStream?.addTrack(localVideoTrack)
            }

            private fun getVideoCapturer(context: Context): CameraVideoCapturer =
                Camera2Enumerator(context).run {
                    deviceNames.find {
                        isFrontFacing(it)
                    }?.let {
                        createCapturer(it, null)
                    } ?: throw IllegalStateException()
                }

            private fun stopCapturingCamera() {
                videoCapturer.dispose()
                localVideoTrack?.removeSink(localSurfaceView)
                localSurfaceView.clearImage()
                localStream?.removeTrack(localVideoTrack)
                localVideoTrack?.dispose()
            }


            //connection negotiatio0n section
            fun call(target: String) {
                peerConnection?.createOffer(object : MySDPObserver() {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        super.onCreateSuccess(p0)
                        peerConnection?.setLocalDescription(object : MySDPObserver() {
                            override fun onSetSuccess() {
                                super.onSetSuccess()
                                listener?.onTransferEventToSocket(
                                    DataModel(
                                        type = DataModelType.Offer,
                                        sender = username,
                                        target = target,
                                        data = p0?.description
                                    )
                                )
                            }
                        }, p0)
                    }
                }, mediaConstraint)
            }

            fun answer(target: String) {
                peerConnection?.createAnswer(object : MySDPObserver() {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        super.onCreateSuccess(p0)
                        peerConnection?.setLocalDescription(object : MySDPObserver() {
                            override fun onSetSuccess() {
                                super.onSetSuccess()
                                listener?.onTransferEventToSocket(
                                    DataModel(
                                        type = DataModelType.Answer,
                                        sender = username,
                                        target = target,
                                        data = p0?.description
                                    )
                                )
                            }
                        }, p0)
                    }
                }, mediaConstraint)
            }

            fun onRemoteSesscionReceived(sessionDescription: SessionDescription) {
                peerConnection?.setRemoteDescription(MySDPObserver(), sessionDescription)
            }

            fun addIceCandidateToPeer(iceCandidate: IceCandidate) {
                peerConnection?.addIceCandidate(iceCandidate)
            }

            fun sendIceCandidate(target: String, iceCandidate: IceCandidate) {
                addIceCandidateToPeer(iceCandidate)
                listener?.onTransferEventToSocket(
                    DataModel(
                        type = DataModelType.IceCandidate,
                        sender = username,
                        target = target,
                        data = gson.toJson(iceCandidate)
                    )
                )

            }

            fun closeConnection() {
                try {
                    videoCapturer.dispose()
                    screenCapturer?.dispose()
                    localStream?.dispose()
                    peerConnection?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            fun switchCamera() {
                videoCapturer.switchCamera(null)
            }

            fun toggleAudio(shouldbeMuted: Boolean) {
                if (shouldbeMuted) {
                    Log.d("microphone_status", "removing audio track : shouldbeMuted : $shouldbeMuted")
                    localStream?.removeTrack(localAudioTrack)
                } else {
                    Log.d("microphone_status", "adding audio track : should be muted     : $shouldbeMuted")
                    localStream?.addTrack(localAudioTrack)
                }
            }

            fun toggleVideo(shouldbeMuted: Boolean) {
                try {
                    if (shouldbeMuted) {
                        stopCapturingCamera()
                    } else {
                        startCapturingCamera(localSurfaceView)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            fun setPermissionIntent(screenPermissionIntent: Intent) {
                this.permissionIntent = screenPermissionIntent
            }
            fun startScreenCapturing() {
                val displayMetrics = DisplayMetrics()
                val windowsManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowsManager.defaultDisplay.getMetrics(displayMetrics)

                val screenWidthPixels = displayMetrics.widthPixels
                val screenHeightPixels = displayMetrics.heightPixels

                val surfaceTextureHelper = SurfaceTextureHelper.create(
                    Thread.currentThread().name,eglBaseContext
                )

                screenCapturer = createScreenCapturer()
                screenCapturer!!.initialize(
                    surfaceTextureHelper,context,localScreenVideoSource.capturerObserver
                )
                screenCapturer!!.startCapture(screenWidthPixels,screenHeightPixels,15)

                localScreenShareTrack =
                    peerConnectionFactory.createVideoTrack(localTrackId+"_video",localScreenVideoSource)
                localScreenShareTrack?.addSink(localSurfaceView)
                localStream?.addTrack(localScreenShareTrack)
                peerConnection?.addStream(localStream)

            }

            private fun createScreenCapturer():VideoCapturer {
                return ScreenCapturerAndroid(permissionIntent, object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        Log.d("permissions", "onStop: permission of screen casting is stopped")
                    }
                })
            }

            fun stopScreenCapturing() {
                screenCapturer?.stopCapture()
                screenCapturer?.dispose()
                localScreenShareTrack?.removeSink(localSurfaceView)
                localSurfaceView.clearImage()
                localStream?.removeTrack(localScreenShareTrack)
                localScreenShareTrack?.dispose()

            }
            interface Listener {
                fun onTransferEventToSocket(data: DataModel)
            }

        }