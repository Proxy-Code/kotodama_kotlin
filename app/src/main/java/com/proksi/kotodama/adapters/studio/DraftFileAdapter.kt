package com.proksi.kotodama.adapters.studio

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.firebase.firestore.FirebaseFirestore
import com.kotodama.tts.databinding.CardviewDraftFileBinding
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.DraftFileModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class DraftFileAdapter(private var mContext: Context,
                       private var list:MutableList<DraftFileModel>,
                       private val onItemClick: (DraftFileModel) -> Unit
):

    RecyclerView.Adapter<DraftFileAdapter.ViewHolder>(){
    private var uid: String? = null

    @OptIn(DelicateCoroutinesApi::class)
    inner class ViewHolder(var design: CardviewDraftFileBinding) : RecyclerView.ViewHolder(design.root){
            init {
                GlobalScope.launch {
                    DataStoreManager.getUid(mContext).collect { uidValue ->
                        uid = uidValue
                    }
                }
            }
    }
    
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(list:MutableList<DraftFileModel>) {
        this.list = list
        notifyDataSetChanged()
       
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(mContext)
        val design = CardviewDraftFileBinding.inflate(layoutInflater,parent,false)

        return ViewHolder(design)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]

        holder.design.fileName.text = item.name

        val imageUrls = item.conversation?.mapNotNull { it.imageUrl } ?: emptyList()

        if(imageUrls.isEmpty()){
            holder.design.imagesContainer.visibility = View.GONE
            holder.design.imgFolder.visibility = View.VISIBLE
        }else{
            holder.design.imagesContainer.visibility = View.VISIBLE
            holder.design.imgFolder.visibility = View.GONE

            val marginBetweenImages = -20

            holder.design.imagesContainer.removeAllViews()

            imageUrls.take(3).forEachIndexed { index, imageUrl ->
                val imageView = ImageView(holder.itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp).apply {
                        if (index != 0) {
                            marginStart = marginBetweenImages.dp
                        }
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    clipToOutline = true

                    translationZ = (imageUrls.size - index).toFloat() // Öndeki resim daha yüksek Z-index'te olur
                }

                Glide.with(holder.itemView.context)
                    .load(imageUrl)
                    .transform(RoundedCorners(16))
                    .into(imageView)

                holder.design.imagesContainer.addView(imageView)
            }
        }

        holder.design.root.setOnClickListener {
            onItemClick(item)
        }
    }

    val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    fun deleteItem(position: Int) {


        val deletedItem = list[position]

        list.removeAt(position)
        notifyItemRemoved(position)

        Log.d("DraftFileAdapter", deletedItem.id)

        val userId = uid ?: return
        Log.d("DraftFileAdapter", userId)

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .collection("studio")
            .document(deletedItem.id)
            .delete()
            .addOnSuccessListener {
                Log.d("DraftFileAdapter", "DocumentSnapshot successfully deleted!")
            }
            .addOnFailureListener { e ->
                Log.w("DraftFileAdapter", "Error deleting document", e)
            }
    }

}