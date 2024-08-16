package com.proksi.kotodama.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.proksi.kotodama.BaseFragment
import com.proksi.kotodama.R
import com.proksi.kotodama.adapters.CategoryAdapter
import com.proksi.kotodama.adapters.VoicesAdapter
import com.proksi.kotodama.databinding.FragmentHomeBinding
import com.proksi.kotodama.models.Category
import com.proksi.kotodama.models.Voice
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var design: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentHomeBinding.inflate(inflater, container, false)

        design.recyclerViewCategories.layoutManager=
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        design.recyclerViewVoices.layoutManager=
            GridLayoutManager(requireContext(),3, GridLayoutManager.VERTICAL,false)


        val categoryList = getCategoryList()
        val adapterCategory = CategoryAdapter(this.requireContext(),categoryList)
        design.recyclerViewCategories.adapter=adapterCategory

        val voicesList = getVoicesList()
        val adapterVoice = VoicesAdapter(this.requireContext(),voicesList,design.selectedImg)
        design.recyclerViewVoices.adapter=adapterVoice

        design.imageCrown.setOnClickListener{
            showPremiumDialogBox()
        }


        return design.root
    }

    private fun getCategoryList(): List<Category> {
        val categoryData = listOf(
            Triple("all", R.string.all, R.drawable.micro),
            Triple("trends", R.string.trends, R.drawable.trends),
            Triple("new", R.string.newCtg, R.drawable.neww),
            Triple("musicians", R.string.musicians, R.drawable.musicnota),
            Triple("tv-shows", R.string.tvShows, R.drawable.tv),
            Triple("actors", R.string.actors, R.drawable.actor),
            Triple("sports", R.string.sports, R.drawable.sport),
            Triple("fictional", R.string.fictional, R.drawable.fictional),
            Triple("rap", R.string.rap, R.drawable.rap),
            Triple("games", R.string.game, R.drawable.game),
            Triple("anime", R.string.anime, R.drawable.anime),
            Triple("kpop", R.string.kpop, R.drawable.kpop),
            Triple("random", R.string.random, R.drawable.random)
        )
        return categoryData.map { (id, text, image) ->
            Category(id, text, image)
        }
    }
    private fun getVoicesList(): List<Voice> {
        val manuelVoice = Voice("Create Your Voice",R.drawable.create_voice)
        val voicesData= listOf(
            Pair("Artist",  R.drawable.boy),
            Pair("Artist",  R.drawable.boy),
            Pair("Artist",  R.drawable.boy),
            Pair("Artist",  R.drawable.fotow22),
            Pair("Artist",  R.drawable.fotow22),
            Pair("Artist",  R.drawable.fotow22),
            Pair("kanye west",  R.drawable.fotow22),
            Pair("Artist",  R.drawable.fotow22),
            Pair("Artist",  R.drawable.fotow22),
            Pair("Artist",  R.drawable.fotow22),
            Pair("Artist",  R.drawable.fotow22),
            Pair("Artist",  R.drawable.fotow22),
            Pair("Artist",  R.drawable.fotow22),

        )
        return mutableListOf(manuelVoice).apply {
            addAll(voicesData.map { (title, image) ->
                Voice(title, image)
            })
        }
    }
    private fun showPremiumDialogBox(){

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_premium)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.getWindow()?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        dialog.show()

        val closeBtnPremium = dialog.findViewById<ImageView>(R.id.closeButton)
        closeBtnPremium.setOnClickListener {
            dialog.dismiss()
            showFinalOffer()
        }
    }

    private fun showFinalOffer(){

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_final_offer)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.getWindow()?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        val title = dialog.findViewById<TextView>(R.id.finalTitle)
        val paint = title.paint
        val width = paint.measureText(title.text.toString())
        title.paint.shader= LinearGradient(
            0f,0f,width,title.textSize, intArrayOf(
                Color.parseColor("#7100E2"),
                Color.parseColor("#8E05C2"),
            ), null, Shader.TileMode.REPEAT
        )


        dialog.show()

        val closeBtnPremium = dialog.findViewById<ImageView>(R.id.closeButton)
        closeBtnPremium.setOnClickListener {
            dialog.dismiss()
        }
    }

}