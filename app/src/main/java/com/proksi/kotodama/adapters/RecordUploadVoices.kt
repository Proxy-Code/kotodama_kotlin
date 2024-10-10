package com.proksi.kotodama.adapters



import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
import com.kotodama.app.R
import com.kotodama.app.databinding.CardviewUploadrecordedVoicesBinding
import com.proksi.kotodama.models.AudioRecord
import com.proksi.kotodama.viewmodel.CloneViewModel

import java.io.File
import java.io.IOException

class RecordUploadVoices(var mContext: Context,
                         val items:MutableList<AudioRecord>,
                         private val itemClickListener: OnItemClickListener,
                         private val viewModel: CloneViewModel,
                         private val dataUpdatedListener: OnDataUpdatedListener,

                         ) :

    RecyclerView.Adapter<RecordUploadVoices.Viewholder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPaused: Boolean = true

    interface OnItemClickListener {
        fun onItemClick(item: AudioRecord)
    }

    interface OnDataUpdatedListener {
        fun onDataUpdated(newItems: List<AudioRecord>)
    }
    inner class Viewholder(design: CardviewUploadrecordedVoicesBinding) : RecyclerView.ViewHolder(design.root){
        var design: CardviewUploadrecordedVoicesBinding

        init {
            this.design=design
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecordUploadVoices.Viewholder {
        val layoutInflater = LayoutInflater.from(mContext)
        val design= CardviewUploadrecordedVoicesBinding.inflate(layoutInflater,parent,false)
        return Viewholder(design)
    }

    override fun onBindViewHolder(holder: RecordUploadVoices.Viewholder, position: Int) {
        val item = items[position]
        holder.design.songName.text="Record ${ position + 1 } "

        val durationTime=formatDuration(item.duration)

        holder.design.playButton.setOnClickListener {
            playVoice(item.path, holder.design.playIcon)
        }

        holder.design.deleteRecord.setOnClickListener {
            deleteAudioRecord(position)
        }
        holder.design.duration.text=durationTime
    }

    override fun getItemCount(): Int {
        Log.d("CloneVoice", "getItemCount: ${items.size} ")
        return items.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<AudioRecord>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
        dataUpdatedListener.onDataUpdated(newItems)
    }


    private fun playVoice(audioFilePath: String, playButton: ImageView) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                isPaused = false
                try {
                    setDataSource(audioFilePath) // Set the data source from the passed AudioRecord
                    prepare()
                    start()
                    playButton.setImageResource(R.drawable.icon_pause) // Show pause icon when playing
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                setOnCompletionListener {
                    playButton.setImageResource(R.drawable.icon_play) // Change icon back to play on completion
                    isPaused = true
                    mediaPlayer?.reset() // Release resources
                    mediaPlayer = null
                }
            }
        } else if (isPaused) {
            mediaPlayer?.start()
            playButton.setImageResource(R.drawable.icon_pause)
            isPaused = false
        } else {
            mediaPlayer?.pause()
            playButton.setImageResource(R.drawable.icon_play)
            isPaused = true
        }
    }

    private fun deleteAudioRecord(position: Int) {
        val audioFilePath = items[position].path

        // Attempt to delete the file from storage
        val file = File(audioFilePath)
        if (file.exists()) {
            file.delete()
        }

        // Remove the item from the list
        val deletedItem = items.removeAt(position)

        // Notify the adapter about the removed item and update the data
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, items.size)

        // Call updateData to refresh the UI
        updateData(items)

        // Update the ViewModel by removing the deleted item from the audioFilePaths list
        viewModel.removeAudioFilePath(deletedItem)
    }


    fun formatDuration(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        // Formatting minutes and seconds to ensure they are two digits
        val totalDuration = items.sumOf { it.duration }
        val progress = (totalDuration / 1000).toInt()

        return String.format("%02d:%02d", minutes, seconds)
    }

    fun stopPlayingAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.reset() // Reset the MediaPlayer to release resources
                it.release()
                mediaPlayer = null

            }
        }
    }
}