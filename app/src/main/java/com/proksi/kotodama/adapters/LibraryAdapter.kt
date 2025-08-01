package com.proksi.kotodama.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.marginStart
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import com.google.firebase.firestore.FirebaseFirestore
import com.kotodama.tts.R
import com.kotodama.tts.databinding.CardViewLibraryBinding
import com.kotodama.tts.databinding.FaqsCardViewBinding
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.dataStore.DataStoreManager.getUid
import com.proksi.kotodama.models.Faqs
import com.proksi.kotodama.models.UserLibrary
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException

class LibraryAdapter(var mContext: Context,
                     private val activity: Activity,
                     private val dataStoreManager:DataStoreManager,
                     val items: MutableList<UserLibrary>,
                     private val lifecycleScope: LifecycleCoroutineScope,) :
    RecyclerView.Adapter<LibraryAdapter.Viewholder>() {

    private var expandedPosition = -1  // Genişletilmiş item'ın pozisyonunu takip eder
    private var currentlyPlayingPosition: Int? = null  // Çalan medyanın pozisyonunu takip eder
    private var mediaPlayer: MediaPlayer? = null  // MediaPlayer örneği
    private val handler = Handler(Looper.getMainLooper())  // UI güncelleme için Handler
    private var uid: String? = null

    @OptIn(DelicateCoroutinesApi::class)
    inner class Viewholder(design: CardViewLibraryBinding) : RecyclerView.ViewHolder(design.root) {
        var design: CardViewLibraryBinding

        init {
            this.design = design

            GlobalScope.launch {
                getUid(mContext).collect { uidValue ->
                    uid = uidValue
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryAdapter.Viewholder {
        val layoutInflater = LayoutInflater.from(mContext)
        val design = CardViewLibraryBinding.inflate(layoutInflater, parent, false)
        return Viewholder(design)
    }

    override fun onBindViewHolder(holder: Viewholder, @SuppressLint("RecyclerView") position: Int) {
        val item = items[position]
        holder.design.name.text = item.name
        holder.design.descriptionText.text = item.text



        if(item.type=="studio"){

            val marginBetweenImages = -20

            holder.design.images.removeAllViews()
            holder.design.cardImgView.visibility = View.GONE
            holder.design.images.visibility = View.VISIBLE

            item.images?.take(3)?.forEachIndexed { index, imageUrl ->

                val imageView = ImageView(holder.itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp).apply {
                        if (index != 0) {
                            marginStart = marginBetweenImages.dp
                        }
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    clipToOutline = true

                    translationZ = (item.images!!.size - index).toFloat() // Öndeki resim daha yüksek Z-index'te olur
                }
                Glide.with(holder.itemView.context)
                    .load(imageUrl)
                    .transform(RoundedCorners(16))
                    .into(imageView)

                holder.design.images.addView(imageView)
            }
        }else{
            holder.design.cardImgView.visibility = View.VISIBLE
            holder.design.images.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(item.image)
                .into(holder.design.cardImgView)
        }

        if (position == expandedPosition) {
            holder.design.accordionLayout.visibility = View.VISIBLE
        } else {
            holder.design.accordionLayout.visibility = View.GONE
        }

        holder.design.layout.setOnClickListener {
            if(!item.isGenerating){
                lifecycleScope.launch {
                    if (dataStoreManager.isFeedbackShown(mContext)){
                        handleLayoutClick(position, holder)
                        Log.d("showFeedbackDialog", "${dataStoreManager.isFeedbackShown(mContext)} ")
                    }else{
                        Log.d("showFeedbackDialog", "${dataStoreManager.isFeedbackShown(mContext)} ")
                        showFeedbackDialog ()
                        handleLayoutClick(position, holder)
                    }
                }
            }
        }

        holder.design.textType.text = item.type

        holder.design.sendButton.setOnClickListener {
            lifecycleScope.launch {
                val file = downloadSong(item.soundUrl, item.name)
                file?.let {
                    shareSong(it)
                } ?: run {
                    Toast.makeText(mContext, "Failed to download song", Toast.LENGTH_SHORT).show()
                }
            }


        }

        // Play/Pause butonuna tıklanınca medya çalma işlemi
        holder.design.playButton.setOnClickListener {
            if (mediaPlayer == null || currentlyPlayingPosition != position) {
                startPlaying(item.soundUrl, holder, position)
            } else {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer?.pause()
                    holder.design.playButton.setImageResource(R.drawable.play_lib)
                } else {
                    mediaPlayer?.start()
                    holder.design.playButton.setImageResource(R.drawable.pause_lib)

                }
            }
        }

        holder.design.minusButton.setOnClickListener {
            mediaPlayer?.let {
                val currentPosition = it.currentPosition
                val newPosition = (currentPosition - 5000).coerceAtLeast(0)  // 5 saniye geri sar
                it.seekTo(newPosition)
                holder.design.seekbar.progress = newPosition
                holder.design.startTime.text = formatTime(newPosition)
            }
        }

        holder.design.plusButton.setOnClickListener {
            mediaPlayer?.let {
                val currentPosition = it.currentPosition
                val newPosition = (currentPosition + 5000).coerceAtMost(it.duration)  // 5 saniye ileri sar
                it.seekTo(newPosition)
                holder.design.seekbar.progress = newPosition
                holder.design.startTime.text = formatTime(newPosition)
            }
        }

        if (currentlyPlayingPosition != position) {
            holder.design.seekbar.progress = 0
            holder.design.startTime.text = formatTime(0)
        }

        if (item.isGenerating) {
            holder.design.progressBar.visibility = View.VISIBLE
            holder.design.progressBar.isIndeterminate = true
            holder.design.sendButton.visibility = View.GONE
            holder.design.textType.visibility = View.GONE
        } else {
            holder.design.progressBar.visibility = View.GONE
            holder.design.sendButton.visibility=View.VISIBLE
            holder.design.textType.visibility = View.VISIBLE
        }


    }


    val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()


    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<UserLibrary>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun startPlaying(soundUrl: String, holder: Viewholder, position: Int) {
        stopPlaying()  // Daha önce çalan medyayı durdur

        mediaPlayer = MediaPlayer().apply {
            setDataSource(soundUrl)
            prepare()
            start()
        }

        currentlyPlayingPosition = position
        holder.design.playButton.setImageResource(R.drawable.pause_lib)
        holder.design.seekbar.max = mediaPlayer?.duration ?: 0

        mediaPlayer?.setOnCompletionListener {
            //holder.design.seekbar.progress = 0
            holder.design.playButton.setImageResource(R.drawable.play_lib)
        }

        val runnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    val currentPosition = it.currentPosition
                    holder.design.seekbar.progress = currentPosition
                    holder.design.startTime.text = formatTime(currentPosition)
                    handler.postDelayed(this, 1)  // Her saniye güncelle
                }
            }
        }
        handler.post(runnable)

        holder.design.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    holder.design.startTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun stopPlaying() {
        mediaPlayer?.let {
            it.stop()
            it.reset()
            it.release()
        }
        mediaPlayer = null
    }

    private fun formatTime(ms: Int): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 1000) / 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun deleteItem(position: Int) {
        val deletedItem = items[position]

        items.removeAt(position)
        notifyItemRemoved(position)

        val userId = uid ?: return

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .collection("library")
            .document(deletedItem.id)
            .delete()
            .addOnSuccessListener {
                Log.d("LibraryAdapter", "DocumentSnapshot successfully deleted!")
            }
            .addOnFailureListener { e ->
                Log.w("LibraryAdapter", "Error deleting document", e)
            }

    }

    private suspend fun downloadSong(url: String, songName: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val client = OkHttpClient()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("Failed to download file: $response")

                val externalFilesDir = mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                val file = File(externalFilesDir, "$songName.mp3")
                val sink = file.sink().buffer()
                sink.writeAll(response.body!!.source())
                sink.close()
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    private fun shareSong(file: File) {
        val fileUri: Uri = FileProvider.getUriForFile(
            mContext,
            "${mContext.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "audio/mpeg"
        }

        mContext.startActivity(Intent.createChooser(shareIntent, "Share song"))
    }

    private fun showFeedbackDialog(){
        val manager = ReviewManagerFactory.create(mContext)
        lifecycleScope.launch {
            dataStoreManager.saveFeedbackShown(activity, true)
        }

        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                manager.launchReviewFlow(activity,reviewInfo)
                Log.d("showFeedbackDialog", "success $reviewInfo ")

            } else {
                @ReviewErrorCode val reviewErrorCode = (task.getException() as ReviewException).errorCode
                Log.d("showFeedbackDialog", "success $reviewErrorCode")

            }
        }
    }

    private fun handleLayoutClick(position: Int, holder: Viewholder) {
        if (expandedPosition == position) {
            expandedPosition = -1
            notifyItemChanged(position)

            if (currentlyPlayingPosition == position) {
                stopPlaying()
                currentlyPlayingPosition = null
                holder.design.seekbar.progress = 0
                holder.design.startTime.text = formatTime(0)
            }
        } else {
            val previousExpandedPosition = expandedPosition
            expandedPosition = position
            notifyItemChanged(previousExpandedPosition)
            notifyItemChanged(position)

            if (currentlyPlayingPosition != -1 && currentlyPlayingPosition != position) {
                stopPlaying()
                currentlyPlayingPosition?.let { notifyItemChanged(it) }
                currentlyPlayingPosition = -1
            }
        }
    }

}
