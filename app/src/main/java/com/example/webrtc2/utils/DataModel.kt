package com.example.webrtc2.utils

enum class DataModelType{
    StartAudioCall, StartVideoCall, Offer, Answer, IceCandidate, EndCall
}

data class DataModel(
    var sender : String?= null,
    var target : String,
    var type : DataModelType,
    var data : String? = null,
    var timeStamp : Long = System.currentTimeMillis()
)

fun DataModel.isValid() : Boolean{
    return System.currentTimeMillis() - this.timeStamp < 60000
}