package com.proksi.kotodama.fragments.Studio

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentIntroBinding
import com.proksi.kotodama.adapters.studio.DraftFileAdapter
import com.proksi.kotodama.dialogs.DialogNewDraft
import com.proksi.kotodama.models.ConversationModel
import com.proksi.kotodama.models.DraftFileModel
import com.proksi.kotodama.models.SwipeGesture
import com.proksi.kotodama.viewmodel.StudioViewModel


class FragmentIntro : Fragment() {

    private lateinit var design:FragmentIntroBinding
    private var TAG = "FragmentIntro"
    private val viewModel: StudioViewModel by activityViewModels()


    private val adapter by lazy {
        DraftFileAdapter(requireContext(), mutableListOf()){
            viewModel.setDraft(it)
            findNavController().navigate(R.id.action_fragmentIntro_to_studioAddLineFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentIntroBinding.inflate(inflater,container,false)

        fetchDrafts()
        setupUI()

        return design.root
    }

    private fun setupUI(){

        design.newDraftBtn.setOnClickListener {
            val dialog = DialogNewDraft(
                context = requireContext(),
                onCreateClick = { input ->

                    if (viewModel.draftNames.contains(input)) {
                        Toast.makeText(context, "Bu isimde draft zaten var!", Toast.LENGTH_SHORT).show()
                        return@DialogNewDraft
                    }

                    val db = FirebaseFirestore.getInstance()
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@DialogNewDraft

                    val draftData = hashMapOf(
                        "name" to input,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "isGenerating" to true,
                        "libraryTaskId" to "",
                        "exportUrl" to "",
                        "processedAt" to null
                    )

                    db.collection("users")
                        .document(userId)
                        .collection("studio")
                        .add(draftData)
                        .addOnSuccessListener { documentRef ->
                            Log.d(TAG, "Draft eklendi: ${documentRef.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Hata oluştu", e)
                        }
                }
            )
            dialog.show()
        }

        val swipeGesture = object : SwipeGesture(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                    adapter.deleteItem(viewHolder.adapterPosition)
                }
            }
        }

        val touchHelper = ItemTouchHelper(swipeGesture)
        touchHelper.attachToRecyclerView(design.rvDraft)

//        val lottieView = design.ltAnimation
//        lottieView.addValueCallback(
//            KeyPath("**", "Fill 1"),
//            LottieProperty.COLOR,
//            LottieValueCallback(Color.parseColor("#7100E2"))
//        )
//        lottieView.playAnimation()


    }

    private fun fetchDrafts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .collection("studio")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { result, error ->
                if (error != null) {
                    Log.w(TAG, "Gerçek zamanlı dinleme hatası", error)
                    return@addSnapshotListener
                }

                if (result != null) {
                    val draftList = mutableListOf<DraftFileModel>()

                    if (result.isEmpty) {
                        design.rvDraft.visibility = View.GONE
                        design.noDataView.visibility = View.VISIBLE
                        adapter.updateData(mutableListOf())
                        return@addSnapshotListener
                    }

                    var loadedCount = 0
                    val totalCount = result.size()

                    for (document in result) {
                        val id = document.id

                        db.collection("users")
                            .document(userId)
                            .collection("studio")
                            .document(id)
                            .collection("conversation")
//                            .limit(1)
                            .get()
                            .addOnSuccessListener { convSnap ->

                                val conversationList = if (!convSnap.isEmpty) {
                                    convSnap.documents.map { convDoc ->
                                        ConversationModel(
                                            id = convDoc.id,
                                            imageUrl = convDoc.getString("imageUrl") ?: "",
                                            soundUrl = convDoc.getString("soundUrl") ?: "",
                                            text = convDoc.getString("text") ?: "",
                                            order = convDoc.getLong("order")?.toInt() ?: 0,
                                            isGenerating = convDoc.getBoolean("isGenerating") ?: false,
                                            processedAt = convDoc.getTimestamp("processedAt"),
                                            createdAt = convDoc.getTimestamp("createdAt"),
                                            requestIds = convDoc.get("request_ids") as? List<String> ?: emptyList(),
                                            soundSampleId = convDoc.getString("sound_sample_id") ?: ""
                                        )
                                    }
                                } else null


                                val draft = DraftFileModel(
                                    id = id,
                                    createdAt = document.getTimestamp("createdAt"),
                                    exportUrl = document.getString("exportUrl") ?: "",
                                    isGenerating = document.getBoolean("isGenerating") ?: false,
                                    name = document.getString("name") ?: "",
                                    libraryTaskId = document.getString("libraryTaskId") ?: "",
                                    processedAt = document.getTimestamp("processedAt"),
                                    conversation = conversationList
                                )

                                draftList.add(draft)
                                loadedCount++

                                if (loadedCount == totalCount) {
                                    design.rvDraft.visibility = View.VISIBLE
                                    design.noDataView.visibility = View.GONE
                                    design.rvDraft.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                                    design.rvDraft.adapter = adapter
                                    viewModel.setDraftNames(draftList.map { it.name })
                                    adapter.updateData(draftList)
                                }
                            }
                    }
                }
            }
    }


}