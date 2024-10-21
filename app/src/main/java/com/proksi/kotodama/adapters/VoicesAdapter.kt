package com.proksi.kotodama.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kotodama.tts.R
import com.kotodama.tts.databinding.CardViewVoicesBinding
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.VoiceModel

class VoicesAdapter(var mContext: Context,
                    var items: List<VoiceModel>,
                    val selectedImg: View,
                    val isSubscribed: Boolean,
                    val hasClone:Boolean,
                    val dataStoreManager : DataStoreManager,
                    viewLifecycleOwner: LifecycleOwner,
                    val voiceSelectedListener: OnVoiceSelectedListener ) :
    RecyclerView.Adapter<VoicesAdapter.ViewHolder>(){

    var selectedPosition=-1

    interface OnVoiceSelectedListener {
        fun onVoiceSelected(voice: VoiceModel)
        fun deleteClone(cloneId: String, context: Context)  // Add this method for deletion
    }

    interface OnCategoryClickListener {
      //  fun onCategoryClick(position: Int, item: VoiceModel)
    }

    private var onCategoryClickListener: OnCategoryClickListener? = null


    fun setOnCategoryClickListener(listener: OnCategoryClickListener) {
        onCategoryClickListener = listener
    }

    inner class ViewHolder(design: CardViewVoicesBinding) : RecyclerView.ViewHolder(design.root){
        var design: CardViewVoicesBinding

        init {
            this.design=design
            design.cardImgView.setOnClickListener{
                val preiousSelectedPosition = selectedPosition
                selectedPosition = if (selectedPosition==adapterPosition) -1 else adapterPosition

                notifyItemChanged(preiousSelectedPosition)
                notifyItemChanged(selectedPosition)

                updateViewVisibility()
                if (selectedPosition != -1) {
                    val selectedVoice = items[selectedPosition]
                    if (selectedVoice.name == "Create Your Voice") {
                        val navController = Navigation.findNavController(itemView)
                        navController.navigate(R.id.action_homeFragment_to_voiceLabNameFragment)
                    }
                    voiceSelectedListener.onVoiceSelected(selectedVoice)
                } else {
                    voiceSelectedListener.onVoiceSelected(
                        VoiceModel(
                            name = "",
                            id = "",
                            imageUrl = "",
                            createdAt = Timestamp.now(),
                            model_name = "Sample Model",
                            category = emptyList(),
                            allTimeCounter = 0,
                            weeklyCounter = 0,
                            charUsedCount = 0
                        )
                    )
                }


            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val layoutInflater = LayoutInflater.from(mContext)
        val design=CardViewVoicesBinding.inflate(layoutInflater,parent,false)

        return ViewHolder(design)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val voice = items[position]

        if (voice.id=="create_voice"){
                Log.d("create voice da", "onBindViewHolder: ")
                //val imgResId = if (isSubscribed) R.drawable.sing_ai_subscribed else R.drawable.sing_ai
            val imgResId = if (isSubscribed) R.drawable.plus_frame_subs else R.drawable.plus_clone_unsubs
                Glide.with(holder.itemView.context)
                    .load(imgResId)
                    .into(holder.design.cardImgView)
                holder.design.textViewArtist.text=voice.name
        } else {
            if(voice.isClone){
                if(voice.id=="create_voice"){
                    holder.design.topRightImage.visibility=View.GONE
                }else{
                    holder.design.topRightImage.visibility=View.VISIBLE
                }

            }else{
                holder.design.topRightImage.visibility=View.GONE
            }
            Glide.with(holder.itemView.context)
                .load(voice.imageUrl)
                .placeholder(R.drawable.icon_kotodama) // Eğer resim yüklenemezse, bir placeholder göster
                .error(R.drawable.icon_kotodama)
                .into(holder.design.cardImgView)

            holder.design.textViewArtist.text=voice.name

            if (position == selectedPosition) {
                holder.design.cardImgView.setStrokeColorResource(R.color.main_purple)
            } else {
                holder.design.cardImgView.setStrokeColorResource(android.R.color.transparent)
            }

            holder.design.topRightImage.setOnClickListener {
                // Show a confirmation dialog to delete the clone
                val builder = AlertDialog.Builder(holder.itemView.context)
                builder.setTitle("Delete Clone")
                builder.setMessage("Are you sure you want to delete this voice?")

                builder.setPositiveButton("Delete") { dialog, _ ->
                    // Call the ViewModel's delete function to delete from Firestore
                    voiceSelectedListener.deleteClone(voice.id, holder.itemView.context)
                    dialog.dismiss()
                }

                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }

                val dialog: AlertDialog = builder.create()
                dialog.show()
            }

        }

    }
    private fun updateViewVisibility() {
        if (selectedPosition != -1) {
            selectedImg.visibility=View.VISIBLE

            val selectedVoice = items[selectedPosition]
            Glide.with(selectedImg.context)
                .load(selectedVoice.imageUrl)
                .transform(RoundedCorners(12))
                .into(selectedImg as ImageView)


        } else {
            selectedImg.visibility=View.GONE
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newVoicesList: List<VoiceModel>) {
        this.items = newVoicesList
        notifyDataSetChanged()
    }



}

