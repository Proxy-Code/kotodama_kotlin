package com.proksi.kotodama.adapters.studio

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kotodama.tts.R
import com.kotodama.tts.databinding.CardViewVoicesBinding
import com.kotodama.tts.databinding.CardviewDraftFileBinding
import com.proksi.kotodama.models.DraftFileModel
import com.proksi.kotodama.models.VoiceModel

class CharacterAdapter(private var mContext: Context,
                       var list: List<VoiceModel>,
                       private val onItemClick: (VoiceModel) -> Unit
):

    RecyclerView.Adapter<CharacterAdapter.ViewHolder>(){

    inner class ViewHolder(var design: CardViewVoicesBinding) : RecyclerView.ViewHolder(design.root){
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateData(list:List<VoiceModel>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(mContext)
        val design = CardViewVoicesBinding.inflate(layoutInflater,parent,false)

        return ViewHolder(design)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]

        holder.design.textViewArtist.text = item.name

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.plus_frame_subs)
            .error(R.drawable.plus_frame_subs)
            .into(holder.design.cardImgView)

        holder.design.root.setOnClickListener {
            onItemClick(item)
        }
    }
}