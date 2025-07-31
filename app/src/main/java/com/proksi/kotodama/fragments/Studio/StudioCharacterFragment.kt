package com.proksi.kotodama.fragments.Studio

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentStudioCharacterBinding
import com.proksi.kotodama.adapters.CategoryAdapter
import com.proksi.kotodama.adapters.studio.CharacterAdapter
import com.proksi.kotodama.viewmodel.HomeViewModel
import com.proksi.kotodama.viewmodel.StudioViewModel

class StudioCharacterFragment : Fragment() {

    private lateinit var design:FragmentStudioCharacterBinding
    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapterVoice: CharacterAdapter
    private val viewModelStudio: StudioViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentStudioCharacterBinding.inflate(inflater,container,false)

        design.recyclerViewCategories.layoutManager=
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        design.recyclerViewVoices.layoutManager=
            GridLayoutManager(requireContext(),3, GridLayoutManager.VERTICAL,false)

        design.closeBtn.setOnClickListener {
            findNavController().navigate(R.id.action_studioCharacterFragment_to_studioTextFragment)
        }

        setupVoicesAdapter()

        val categoryList = viewModel.getCategoryList()
        val adapterCategory = CategoryAdapter(this.requireContext(),categoryList)
        design.recyclerViewCategories.adapter=adapterCategory

        adapterCategory.setOnCategoryClickListener(object : CategoryAdapter.OnCategoryClickListener {
            override fun onCategoryClick(category: String) {
                viewModel.getVoicesByCategory(category, requireContext())
            }
        })


        return design.root
    }



    private fun setupVoicesAdapter() {

        adapterVoice = CharacterAdapter(requireContext(), emptyList()){
            viewModelStudio.setVoice(it)
            findNavController().navigate(R.id.action_studioCharacterFragment_to_studioTextFragment)

        }

        design.recyclerViewVoices.adapter = adapterVoice


        viewModel.data.observe(viewLifecycleOwner) { voicesList ->
            if (voicesList != null) {
                adapterVoice.updateData(voicesList)
            } else {
                Log.d("Observer", "Voices List is null")
            }
        }

        viewModel.fetchVoices("all", requireContext())
    }


}