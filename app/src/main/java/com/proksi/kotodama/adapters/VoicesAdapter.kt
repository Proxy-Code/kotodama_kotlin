package com.proksi.kotodama.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kotodama.app.R
import com.kotodama.app.databinding.CardViewVoicesBinding
import com.proksi.kotodama.models.Voice
import com.proksi.kotodama.models.VoiceModel

class VoicesAdapter(var mContext: Context,
                    var items: List<VoiceModel>,
                    val selectedImg: View) :
    RecyclerView.Adapter<VoicesAdapter.ViewHolder>(){

    var selectedPosition=-1

    interface OnVoiceCardInteractionListener {
        fun onVoiceCardSelected()
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
        Log.d("aaa", "getItemCount: $items.size")
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val voice = items[position]

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
    }
    private fun updateViewVisibility() {
        if (selectedPosition != -1) {
            selectedImg.visibility=View.VISIBLE
            val selectedVoice = items[selectedPosition]
            Glide.with(selectedImg.context)
                .load(selectedVoice.imageUrl)
                .into(selectedImg as ImageView)

        } else {
            selectedImg.visibility=View.GONE
        }
    }
    fun updateData(newVoicesList: List<VoiceModel>) {
        this.items = newVoicesList
        notifyDataSetChanged() // RecyclerView'ın verilerin güncellendiğini bilmesi için
    }
}

