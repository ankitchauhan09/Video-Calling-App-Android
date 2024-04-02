package com.example.webrtc2.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.webrtc2.R
import com.example.webrtc2.databinding.MainCallRecyclerViewItemBinding
import com.example.webrtc2.utils.UserStatus

class MainCallViewAdapter(private val listener: MainCallViewAdapter.Listener) :
    RecyclerView.Adapter<MainCallViewAdapter.MainCallViewHolder>() {

    private var userList: List<Pair<String, String>>? = null
    fun updateList(list: List<Pair<String, String>>) {
        this.userList = list;
        Log.d("list updated", userList.toString())
        notifyDataSetChanged()
    }

    class MainCallViewHolder(private val binding: MainCallRecyclerViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context

        fun bind(
            user: Pair<String, String>,
            videoCallClicked: (String) -> Unit,
            audioCallClicked: (String) -> Unit
        ) {
                when (user.second) {
                    "ONLINE" -> {
                        binding.videoCallButton.isVisible = true
                        binding.audioCallButton.isVisible = true
                        Log.d("MainCallViewAdapter", "Setting click listeners for user: ${user.first}")

                        binding.videoCallButton.setOnClickListener {
                            videoCallClicked.invoke(user.first)
                            Log.d("clicked", "video")
                        }
                        binding.audioCallButton.setOnClickListener {
                            audioCallClicked.invoke(user.first)
                            Log.d("clicked", "audio")

                        }
                        binding.statusTv.setTextColor(context.resources.getColor(R.color.green, null))
                        binding.statusTv.text = "online"
                    }

                   "OFFLINE" -> {
                        binding.videoCallButton.isVisible = false
                        binding.audioCallButton.isVisible = false
                        binding.statusTv.setTextColor(context.resources.getColor(R.color.red, null))
                        binding.statusTv.text = "offline"
                    }

                    "IN_CALL" -> {
                        binding.videoCallButton.isVisible = false
                        binding.audioCallButton.isVisible = false
                        binding.statusTv.setTextColor(context.resources.getColor(R.color.yellow, null))
                        binding.statusTv.text = "in call"
                    }

                }

                binding.usernameTV.text = user.first

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainCallViewHolder {
        val binding = MainCallRecyclerViewItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MainCallViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return userList?.size ?: 0;
    }

    override fun onBindViewHolder(holder: MainCallViewHolder, position: Int) {
        userList?.let { list ->
            val user = list[position]
            holder.bind(user, {
                listener.onVideoCallClicked(user.first)
            }, {
                listener.onAudioCallClicked(user.first)
            })
        }
    }

    interface Listener {
        fun onVideoCallClicked(username: String)
        fun onAudioCallClicked(username: String)
    }

}