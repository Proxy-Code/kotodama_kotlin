package com.proksi.kotodama.adapters.studio

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.bumptech.glide.Glide
import com.google.api.LogDescriptor
import com.google.firebase.firestore.FirebaseFirestore
import com.kotodama.tts.R
import com.kotodama.tts.databinding.CardviewConversationBinding
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.ConversationModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ConversationAdapter(private var mContext: Context,
                          private var list:MutableList<ConversationModel>,
                          private var id:String,
                          private val onItemClick: (ConversationModel) -> Unit
):

    RecyclerView.Adapter<ConversationAdapter.ViewHolder>(){

    private var uid: String? = null

    @OptIn(DelicateCoroutinesApi::class)
    inner class ViewHolder(var design: CardviewConversationBinding) : RecyclerView.ViewHolder(design.root){
       init {
           Log.d("isgenerete", "init")

           GlobalScope.launch {
               DataStoreManager.getUid(mContext).collect { uidValue ->
                   uid = uidValue
               }
           }
       }
    }

    private var playingPosition = -1
    private var currentMediaPlayer: MediaPlayer? = null

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(list:MutableList<ConversationModel>) {
        Log.d("isgenerete", "updatedata called}")
        this.list = list
        playingPosition = -1
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(mContext)
        val design = CardviewConversationBinding.inflate(layoutInflater,parent,false)

        return ViewHolder(design)
    }

    override fun getItemCount(): Int {

        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val lottieView = holder.design.ltAnimation
        lottieView.addValueCallback(
            KeyPath("**", "Fill 1"),
            LottieProperty.COLOR,
            LottieValueCallback(Color.parseColor("#6A29CF"))
        )
        lottieView.playAnimation()

        val item = list[position]

        holder.design.converstaionText.text = item.text

        if (position == playingPosition) {
            holder.design.playBtn.setImageResource(R.drawable.baseline_pause_circle_24)
        } else {
            holder.design.playBtn.setImageResource(R.drawable.baseline_play_circle_24)
        }


        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.icon_kotodama) // Eğer resim yüklenemezse, bir placeholder göster
            .error(R.drawable.icon_kotodama)
            .into(holder.design.imgConversation)

        var mediaPlayer: MediaPlayer? = null
        var isPlaying = false

        if(item.isGenerating){
            holder.design.ltAnimation.visibility = View.VISIBLE
            holder.design.playBtn.visibility = View.GONE

        }else{
            holder.design.playBtn.visibility = View.VISIBLE
            holder.design.ltAnimation.visibility = View.GONE
        }

        holder.design.playBtn.setOnClickListener {
            if (!isPlaying) {
                if(item.soundUrl !== ""){
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(item.soundUrl)
                        prepareAsync()
                        setOnPreparedListener {
                            start()
                            holder.design.playBtn.setImageResource(R.drawable.baseline_pause_circle_24)
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            holder.design.playBtn.setImageResource(R.drawable.baseline_play_circle_24)
                            isPlaying = false
                            release()
                        }
                    }
                }
            } else {
                mediaPlayer?.let {
                    it.stop()
                    it.release()
                }
                holder.design.playBtn.setImageResource(R.drawable.baseline_play_circle_24)
                isPlaying = false
            }
        }

        holder.design.root.setOnClickListener {
            onItemClick(item)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun playAllSequentially(startIndex: Int = 0) {
        if (startIndex >= list.size) {
            playingPosition = -1
            notifyDataSetChanged()
            return
        }

        val item = list[startIndex]

        currentMediaPlayer?.release()
        currentMediaPlayer = MediaPlayer().apply {
            setDataSource(item.soundUrl)
            prepareAsync()
            setOnPreparedListener {
                start()
                val previousPosition = playingPosition
                playingPosition = startIndex
                if (previousPosition != -1) notifyItemChanged(previousPosition)
                notifyItemChanged(playingPosition)
            }
            setOnCompletionListener {
                playAllSequentially(startIndex + 1)
            }
        }
    }

    fun stopPlayback() {
        currentMediaPlayer?.stop()
        currentMediaPlayer?.release()
        currentMediaPlayer = null

        val previousPosition = playingPosition
        playingPosition = -1
        if (previousPosition != -1) notifyItemChanged(previousPosition)
    }

    fun deleteItem(position: Int) {
        val deletedItem = list[position]

        list.removeAt(position)
        notifyItemRemoved(position)

        val userId = uid ?: return

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .collection("studio")
            .document(id)
            .collection("conversation")
            .document(deletedItem.id)
            .delete()
            .addOnSuccessListener {
                Log.d("LibraryAdapter", "DocumentSnapshot successfully deleted!")
            }
            .addOnFailureListener { e ->
                Log.w("LibraryAdapter", "Error deleting document", e)
            }

    }

    fun getList(): MutableList<ConversationModel> = list
}