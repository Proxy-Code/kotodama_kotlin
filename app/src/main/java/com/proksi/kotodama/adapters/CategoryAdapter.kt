package com.proksi.kotodama.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kotodama.tts.R
import com.kotodama.tts.databinding.CardViewCategoriesBinding
import com.proksi.kotodama.models.Category

class CategoryAdapter(var mContext: Context, val items: List<Category>) :
    RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedPosition=0
    private var lastSelectedPosition=-1

    interface OnCategoryClickListener {
        fun onCategoryClick(category: String)
    }

    private var onCategoryClickListener: OnCategoryClickListener? = null

    fun setOnCategoryClickListener(listener:  OnCategoryClickListener) {
        onCategoryClickListener = listener
    }

    inner class ViewHolder(design: CardViewCategoriesBinding) : RecyclerView.ViewHolder(design.root){
        var design:CardViewCategoriesBinding
        init {
            this.design=design
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(mContext)
        val design=CardViewCategoriesBinding.inflate(layoutInflater,parent,false)

        return ViewHolder(design)
    }

    override fun getItemCount(): Int {
       return items.size
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val item=items[position]
        val ctg=holder.design
        ctg.ctgName.text=mContext.getString(item.name)

        Glide.with(holder.itemView.context)
            .load(item.icon)
            .into(ctg.ctgImg)

        holder.design.root.setOnClickListener {
                lastSelectedPosition = selectedPosition
                selectedPosition = position
                onCategoryClickListener?.onCategoryClick(item.id)
                notifyDataSetChanged()
        }

        if (selectedPosition == position) {
            ImageViewCompat.setImageTintList(holder.design.ctgImg, ColorStateList.valueOf(mContext.getColor(R.color.white)))
            holder.design.cardViewCtg.setBackgroundResource(R.drawable.radius11_purple)
            holder.design.ctgName.setTextColor(ContextCompat.getColor(mContext, R.color.white))
        } else {
            ImageViewCompat.setImageTintList(holder.design.ctgImg, ColorStateList.valueOf(mContext.getColor(R.color.main_purple)))
            holder.design.cardViewCtg.setBackgroundResource(R.drawable.radius11_bg_white)
            holder.design.ctgName.setTextColor(ContextCompat.getColor(mContext, R.color.main_purple))

        }
    }


}
