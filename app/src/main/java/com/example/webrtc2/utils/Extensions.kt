package com.example.webrtc2.utils

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.permissionx.guolindev.PermissionX

fun AppCompatActivity.getCameraAndMicPermission(success: () -> Unit) {
    PermissionX.init(this)
        .permissions(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        .request { allGranted, _, _ ->
            if (allGranted) {
                success()
            } else {
                Toast.makeText(this, "Camera and microphone is required", Toast.LENGTH_SHORT).show()
            }
        }
}