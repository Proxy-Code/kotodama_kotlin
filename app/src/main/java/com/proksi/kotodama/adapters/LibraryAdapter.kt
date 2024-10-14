package com.proksi.kotodama.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.kotodama.app.R
import com.kotodama.app.databinding.CardViewLibraryBinding
import com.kotodama.app.databinding.FaqsCardViewBinding
import com.proksi.kotodama.dataStore.DataStoreManager.getUid
import com.proksi.kotodama.models.Faqs
import com.proksi.kotodama.models.UserLibrary
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

class LibraryAdapter(var mContext: Context, val items: MutableList<UserLibrary>, private val lifecycleScope: LifecycleCoroutineScope,) :
    RecyclerView.Adapter<LibraryAdapter.Viewholder>() {

    private var expandedPosition = -1  // Genişletilmiş item'ın pozisyonunu takip eder
    private var currentlyPlayingPosition: Int? = null  // Çalan medyanın pozisyonunu takip eder
    private var mediaPlayer: MediaPlayer? = null  // MediaPlayer örneği
    private val handler = Handler(Looper.getMainLooper())  // UI güncelleme için Handler
    private var uid: String? = null

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

        Glide.with(holder.itemView.context)
            .load(item.image)
            .into(holder.design.cardImgView)

        // Accordion layout'un görünürlüğünü kontrol et
        if (position == expandedPosition) {
            holder.design.accordionLayout.visibility = View.VISIBLE
        } else {
            holder.design.accordionLayout.visibility = View.GONE
        }

        // Name'e tıklanınca accordion'u aç/kapa yap
        holder.design.layout.setOnClickListener {
            if (expandedPosition == position) {
                expandedPosition = -1  // Aynı öğeye tıklandıysa kapat
                notifyItemChanged(position)

                // Eğer oynayan bir medya varsa durdur ve sıfırla
                if (currentlyPlayingPosition == position) {
                    stopPlaying()
                    currentlyPlayingPosition = null
                    holder.design.seekbar.progress = 0
                    holder.design.startTime.text = formatTime(0)
                }
            } else {
                val previousExpandedPosition = expandedPosition
                expandedPosition = position
                notifyItemChanged(previousExpandedPosition)  // Önceki açık olanı kapat
                notifyItemChanged(position)  // Yeni tıklanılanı aç

                // Eğer çalan bir medya varsa durdur
                if (currentlyPlayingPosition != -1 && currentlyPlayingPosition != position) {
                    stopPlaying()
                    currentlyPlayingPosition?.let { it1 -> notifyItemChanged(it1) }  // Artık null kontrolüne gerek yok
                    currentlyPlayingPosition = -1
                }
            }
        }

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


        // SeekBar ve süreyi sıfırla (eğer bu item oynatılmıyorsa)
        if (currentlyPlayingPosition != position) {
            holder.design.seekbar.progress = 0
            holder.design.startTime.text = formatTime(0)
        }

        // Şarkı oluşturuluyorsa progress bar'ı göster
        if (item.isGenerating) {
            holder.design.progressBar.visibility = View.VISIBLE
            holder.design.progressBar.isIndeterminate = true
            holder.design.sendButton.visibility = View.GONE
        } else {
            holder.design.progressBar.visibility = View.GONE
            holder.design.sendButton.visibility=View.VISIBLE
        }
    }

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
                    handler.postDelayed(this, 1000)  // Her saniye güncelle
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

}
