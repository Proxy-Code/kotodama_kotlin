package com.proksi.kotodama.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.proksi.kotodama.databinding.CardViewImagesBinding
import com.proksi.kotodama.models.Image

class ImagesAdapter( var mContext: Context, val items:MutableList<Image>) :
    RecyclerView.Adapter<ImagesAdapter.Viewholder>(){

    inner class Viewholder(design: CardViewImagesBinding) : RecyclerView.ViewHolder(design.root){
        var design: CardViewImagesBinding
        init {
            this.design=design
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagesAdapter.Viewholder{
        val layoutInflater = LayoutInflater.from(mContext)
        val design= CardViewImagesBinding.inflate(layoutInflater,parent,false)

        return Viewholder(design)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val image=items[position]

        Glide.with(holder.itemView.context)
            .load(image.img)
            .into(holder.design.img)

    }
}