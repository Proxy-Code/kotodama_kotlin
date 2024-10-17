package com.proksi.kotodama.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kotodama.tts.R
import com.kotodama.tts.databinding.CardViewImagesBinding
import com.proksi.kotodama.models.Image

class ImagesAdapter (
    private var mContext: Context,
    private val items:List<String>,
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<ImagesAdapter.Viewholder>(){
    inner class Viewholder(design: CardViewImagesBinding) : RecyclerView.ViewHolder(design.root){
        var design: CardViewImagesBinding
        init {
            this.design=design
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagesAdapter.Viewholder {
        val layoutInflater = LayoutInflater.from(mContext)
        val design= CardViewImagesBinding.inflate(layoutInflater,parent,false)
        return Viewholder(design)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val song=items[position]
        Glide.with(holder.itemView.context)
            .load(song)
            .placeholder(R.drawable.icon_kotodama)
            .error(R.drawable.icon_kotodama)
            .into(holder.design.cardImgView)

        holder.itemView.setOnClickListener {
            onImageClick(song)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

}

