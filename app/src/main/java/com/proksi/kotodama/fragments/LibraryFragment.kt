package com.proksi.kotodama.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentLibraryBinding
import com.kotodama.app.databinding.FragmentNullFilesBinding
import com.proksi.kotodama.adapters.LibraryAdapter
import com.proksi.kotodama.models.SwipeGesture
import com.proksi.kotodama.viewmodel.UserLibraryViewModel


class LibraryFragment : Fragment() {

    private val viewModel: UserLibraryViewModel by viewModels()
    private lateinit var design: FragmentLibraryBinding
    private lateinit var adapter: LibraryAdapter
    private lateinit var navController: NavController


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentLibraryBinding.inflate(inflater, container, false)
        navController = findNavController()

        design.rvLibrary.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        adapter = LibraryAdapter(requireContext(), mutableListOf(), lifecycleScope )

        design.rvLibrary.adapter = adapter

        viewModel.libraryItems.observe(viewLifecycleOwner, Observer { libraryItems ->
            Log.d("fetch items", "onCreateView: ")
            if (libraryItems.isEmpty()) {
                navController.navigate(R.id.nullFilesFragment)
            } else {
                adapter.updateItems(libraryItems)
            }
        })

        viewModel.fetchUserLibrary(this.requireContext())

        val swipeGesture = object : SwipeGesture(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {


                    adapter.deleteItem(viewHolder.adapterPosition)
                }
            }
        }

        val touchHelper = ItemTouchHelper(swipeGesture)
        touchHelper.attachToRecyclerView(design.rvLibrary)


        return design.root
    }


}