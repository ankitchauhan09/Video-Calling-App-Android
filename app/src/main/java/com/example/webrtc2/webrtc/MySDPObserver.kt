package com.example.webrtc2.webrtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class MySDPObserver : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {

    }

    override fun onSetSuccess() {
    }

    override fun onCreateFailure(p0: String?) {
    }

    override fun onSetFailure(p0: String?) {
    }
}