package com.proksi.kotodama.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kotodama.tts.databinding.LanguageCardViewBinding
import com.proksi.kotodama.models.Language

class LanguageAdapter(private var mContext: Context,
                      private val items:List<Language>,
                      private val itemClickListener: OnItemClickListener

) : RecyclerView.Adapter<LanguageAdapter.Viewholder>(){

    interface OnItemClickListener {
        fun onItemClick(item: Language)
    }

    inner class Viewholder(design: LanguageCardViewBinding) : RecyclerView.ViewHolder(design.root){
        var design:LanguageCardViewBinding
        init {
            this.design=design
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        val layoutInflater = LayoutInflater.from(mContext)
        val design= LanguageCardViewBinding.inflate(layoutInflater,parent,false)
        return Viewholder(design)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val language=items[position]
        holder.design.languageName.text = language.name
        holder.itemView.setOnClickListener{
            itemClickListener.onItemClick(language)
        }

        Glide.with(holder.itemView.context)
            .load(language.icon)
            .into(holder.design.languageFlag)


    }


}

