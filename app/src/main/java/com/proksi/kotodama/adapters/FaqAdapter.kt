package com.proksi.kotodama.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kotodama.app.databinding.FaqsCardViewBinding
import com.proksi.kotodama.models.Faqs

class FaqAdapter(var mContext: Context, val items:MutableList<Faqs>) :
    RecyclerView.Adapter<FaqAdapter.Viewholder>(){
    inner class Viewholder(design: FaqsCardViewBinding) : RecyclerView.ViewHolder(design.root){
        var design: FaqsCardViewBinding
        init {
            this.design=design
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqAdapter.Viewholder {
        val layoutInflater = LayoutInflater.from(mContext)
        val design= FaqsCardViewBinding.inflate(layoutInflater,parent,false)
        return Viewholder(design)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: FaqAdapter.Viewholder, position: Int) {
        val faq=items[position]
        holder.design.faqQuestion.text=holder.itemView.context.getString(faq.question)
        holder.design.faqDesc.text=holder.itemView.context.getString(faq.desc)
        holder.design.expandedView.visibility=if(faq.expand) View.VISIBLE else View.GONE
        holder.design.cardLayout.setOnClickListener(){
            faq.expand = !faq.expand
            notifyDataSetChanged()
        }

    }
}