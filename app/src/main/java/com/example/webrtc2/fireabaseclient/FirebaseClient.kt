        package com.example.webrtc2.fireabaseclient

        import android.util.Log
        import android.widget.Toast
        import com.example.webrtc2.utils.DataModel
        import com.example.webrtc2.utils.FirebaseFieldNames.LATEST_EVENTS
        import com.example.webrtc2.utils.FirebaseFieldNames.PASSWORD
        import com.example.webrtc2.utils.FirebaseFieldNames.STATUS
        import com.example.webrtc2.utils.MyEventListener
        import com.example.webrtc2.utils.UserStatus
        import com.google.firebase.database.DataSnapshot
        import com.google.firebase.database.DatabaseReference
        import com.google.gson.Gson
        import java.lang.Exception
        import java.lang.reflect.Executable
        import javax.inject.Inject
        import javax.inject.Singleton

        @Singleton
        class FirebaseClient @Inject constructor(
            private val dbReference: DatabaseReference,
            private val gson: Gson
        ) {

            var currentUsername: String? = null
            private fun setUsername(username: String) {
                this.currentUsername = username
            }


            fun login(username: String, password: String, done: (Boolean, String?) -> Unit) {
                dbReference.addListenerForSingleValueEvent(object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)

                        //check if the user exists or not
                        if (snapshot.hasChild(username)) {
                            //user exists initiate login
                            val dbPassword = snapshot.child(username).child(PASSWORD).value

                            if (password == dbPassword) {
                                dbReference.child(username).child(STATUS).setValue(UserStatus.ONLINE)
                                    .addOnCompleteListener {
                                        setUsername(username)
                                        done(true, null)
                                    }
                                    .addOnFailureListener {
                                        done(false, it.message.toString())
                                    }
                            } else {
                                done(false, "Password is wrong")
                            }
                        } else {
                            //user not present, register and continue
                            dbReference.child(username).child(PASSWORD).setValue(password)
                                .addOnCompleteListener {
                                    dbReference.child(username).child(STATUS).setValue(UserStatus.ONLINE)
                                        .addOnSuccessListener {
                                            setUsername(username)
                                            done(true, null)
                                        }
                                        .addOnFailureListener {
                                            done(false, it.message.toString())
                                        }
                                }
                                .addOnFailureListener {
                                    done(false, it.message.toString())
                                }
                        }
                    }
                })
            }

            fun observeUserStatus(status: (List<Pair<String, String>>) -> Unit) {

                dbReference.addValueEventListener(object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val list = snapshot.children.filter {
                            it.key != currentUsername
                        }.map {
                            it.key!! to it.child(STATUS).value.toString()
                        }
                        status(list)
                    }
                })
            }

            fun subscribeLatestEvents(listener: Listener) {
                try {
                    dbReference.child(currentUsername!!).child(LATEST_EVENTS).addValueEventListener(
                        object : MyEventListener() {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                super.onDataChange(snapshot)
                                val event = try {
                                    gson.fromJson(snapshot.value.toString(), DataModel::class.java)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    null
                                }
                                event?.let {
                                    listener.onLatestEventReceived(it)
                                }
                            }
                        }
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            fun sendMessageToOtherClient(message: DataModel, success: (Boolean) -> Unit) {
                val convertedMessage = gson.toJson(message.copy(sender = currentUsername))
                dbReference.child(message.target).child(LATEST_EVENTS).setValue(convertedMessage)
                    .addOnCompleteListener {
                        success(true)
                    }
                    .addOnFailureListener {
                        success(false)
                    }
            }

            fun changeMyStatus(status: UserStatus) {
                dbReference.child(currentUsername!!).child(STATUS).setValue(status.name)
            }

            fun clearLatestEvent() {
                dbReference.child(currentUsername!!).child(LATEST_EVENTS).setValue(null)
            }

            interface Listener {
                fun onLatestEventReceived(event: DataModel)
            }

        }