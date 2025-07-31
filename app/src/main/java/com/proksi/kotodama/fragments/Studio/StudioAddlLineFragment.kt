package com.proksi.kotodama.fragments.Studio

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentStudioAddlLineBinding
import com.proksi.kotodama.adapters.studio.ConversationAdapter
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.ConversationModel
import com.proksi.kotodama.models.DraftFileModel
import com.proksi.kotodama.models.SwipeGesture
import com.proksi.kotodama.viewmodel.StudioViewModel
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class StudioAddLineFragment : Fragment() {

    private lateinit var design:FragmentStudioAddlLineBinding
    private val viewModel: StudioViewModel by activityViewModels()
    private lateinit var item: DraftFileModel
    private lateinit var adapter: ConversationAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentStudioAddlLineBinding.inflate(inflater,container,false)

        item = viewModel.getDraft()!!

        adapter = ConversationAdapter(requireContext(),mutableListOf(), item.id) {
            viewModel.setConversation(it)
            findNavController().navigate(R.id.action_studioAddLineFragment_to_studioTextFragment)
        }

        setupUI()

        viewModel.setStudioId(item.id)

        val swipeGesture = object : SwipeGesture(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                    adapter.deleteItem(viewHolder.adapterPosition)
                }
            }
        }

        val touchHelper = ItemTouchHelper(swipeGesture)
        touchHelper.attachToRecyclerView(design.rvConversation)


        return design.root
    }

    private fun setupUI(){

        design.fileName.text = item.name

        listenConversationsRealTime()

        item.conversation?.takeIf { it.isNotEmpty() }?.let { conversationList ->
            design.rvConversation.visibility = View.VISIBLE
            design.rvConversation.layoutManager = LinearLayoutManager(requireContext())
            design.rvConversation.adapter = adapter
            val list = conversationList.toMutableList()
            adapter.updateData(list)
        } ?: run {
            design.rvConversation.visibility = View.GONE
        }

        design.addAnotherLine.setOnClickListener{
            findNavController().navigate(R.id.action_studioAddLineFragment_to_studioTextFragment)
        }

        design.closeBtn.setOnClickListener {
            adapter.stopPlayback()
            findNavController().navigate(R.id.action_studioAddLineFragment_to_fragmentIntro)
        }

        design.playAllBtn.setOnClickListener {
            adapter.playAllSequentially()
        }

        design.exportBtn.setOnClickListener{
            item.conversation?.let { it1 -> exportStudio(it1.toMutableList()) }
        }
    }

    fun exportStudio(conversations: List<ConversationModel>) {

        val notReady = conversations.any { it.isGenerating }
        if (notReady) {
            Log.d("Export", "Bazı konuşmalar hâlâ oluşturuluyor. Export yapılamaz.")
            return
        }

        Log.d("dosya", "$conversations")

        val user = FirebaseAuth.getInstance().currentUser ?: return

        design.ltAnimation.visibility = View.VISIBLE
        design.textBtn.visibility = View.GONE

        // 3. Firebase token al
        user.getIdToken(true).addOnCompleteListener { task ->
           // exporting.value = false
            if (!task.isSuccessful) {
                println("Token alma hatası")
                return@addOnCompleteListener
            }

            val idToken = task.result?.token ?: return@addOnCompleteListener

            // 4. JSON payload
            val payload = JSONObject().apply {
                put("studio_id", item.id)
                put("idToken", idToken)
            }

            // 5. HTTP isteği (OkHttp kullanılabilir)
            val client = OkHttpClient()
            val mediaType = "application/json".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://api.kotodama.app/studio/export")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            // 6. İstek gönder
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("Export error: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()

                    Log.d("export", "onResponse:${responseBody} ")
                    if (response.isSuccessful){
                        Log.d("export", "onResponse:${response.message} ")
                        requireActivity().runOnUiThread {
                            findNavController().navigate(R.id.action_studioAddLineFragment_to_libraryFragment)
                        }
                    }else{
                        Log.d("export", "onResponse: ${response.message}")
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Export hatası", Toast.LENGTH_SHORT).show()
                        }
                    }


                }
            })
        }
    }

    private fun listenConversationsRealTime() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()


        db.collection("users")
            .document(user.uid)
            .collection("studio")
            .document(item.id)
            .collection("conversation")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Listen failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val conversationList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ConversationModel::class.java)?.copy(id = doc.id)
                    }
                    adapter.updateData(conversationList.toMutableList())
                    design.rvConversation.visibility = View.VISIBLE
                } else {
                    adapter.updateData(mutableListOf())
                    design.rvConversation.visibility = View.GONE
                }
            }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        adapter.stopPlayback()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopPlayback()
    }



}