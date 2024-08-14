package com.proksi.kotodama.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.proksi.kotodama.R
import com.proksi.kotodama.databinding.CardViewVoicesBinding
import com.proksi.kotodama.models.Voice

class VoicesAdapter(var mContext: Context,
                    val items: List<Voice>,
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
                    if (selectedVoice.title == "Create Your Voice") {
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
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val voice = items[position]

        Glide.with(holder.itemView.context)
            .load(voice.img)
            .into(holder.design.cardImgView)

        holder.design.textViewArtist.text=voice.title

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
                .load(selectedVoice.img)
                .into(selectedImg as ImageView)

        } else {
            selectedImg.visibility=View.GONE
        }
    }
}

