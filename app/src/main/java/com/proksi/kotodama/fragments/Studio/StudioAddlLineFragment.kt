package com.proksi.kotodama.fragments.Studio

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
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
import com.proksi.kotodama.viewmodel.PaywallViewModel
import com.proksi.kotodama.viewmodel.StudioViewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
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
import java.util.Collections
import kotlin.properties.Delegates


class StudioAddLineFragment : Fragment() {

    private lateinit var design:FragmentStudioAddlLineBinding
    private val viewModel: StudioViewModel by activityViewModels()
    private lateinit var item: DraftFileModel
    private lateinit var adapter: ConversationAdapter
    private var isReorderMode = false
    private lateinit var uid:String
    private lateinit var dataStoreManager: DataStoreManager
    private val viewModelPaywall: PaywallViewModel by activityViewModels()
    private var isSubscribed by Delegates.notNull<Boolean>()


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

        dataStoreManager = DataStoreManager

        lifecycleScope.launch {
            DataStoreManager.getUid(requireContext()).collect { uidValue ->
                if (uidValue != null) {
                    uid = uidValue
                }
            }
        }

        lifecycleScope.launch {
            dataStoreManager.getSubscriptionStatusKey(this@StudioAddLineFragment.requireContext()).collect { isActive ->
                isSubscribed = isActive
            }
        }

        val lottieView = design.ltAnimation
        lottieView.addValueCallback(
            KeyPath("**", "Fill 1"),
            LottieProperty.COLOR,
            LottieValueCallback(Color.parseColor("#FFFFFF"))
        )
        lottieView.playAnimation()

        val swipeGesture = object : SwipeGesture(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                    adapter.deleteItem(viewHolder.adapterPosition)
                }
            }
        }

        val touchHelper = ItemTouchHelper(swipeGesture)
        touchHelper.attachToRecyclerView(design.rvConversation)

        val gestureCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, // sadece yukarı-aşağı sürükleme
            ItemTouchHelper.LEFT // sola kaydırma
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (!isReorderMode) return false // sıralama modu değilse engelle

                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition

                Collections.swap(adapter.getList(), fromPos, toPos)
                adapter.notifyItemMoved(fromPos, toPos)
                return true
            }


            override fun isLongPressDragEnabled(): Boolean {
                return isReorderMode
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                // Drag işlemi bittikten sonra listeyi Firestore'a gönder
                if (isReorderMode) {
                    val sortedList = adapter.getList()
                    uploadSortedListToFirestore(sortedList, item.id)
                }
            }

        }

        val touchHelper2 = ItemTouchHelper(gestureCallback)
        touchHelper2.attachToRecyclerView(design.rvConversation)


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
            if(isSubscribed){
                item.conversation?.let {
                    it1 -> exportStudio(it1.toMutableList())
                }
            }else{
                callPaywall("subs")
            }
        }

        design.reBtn.setOnClickListener {
            isReorderMode = !isReorderMode
            Toast.makeText(requireContext(), if (isReorderMode) "Sürükleyerek yeniden sırala" else "Sıralama kapalı", Toast.LENGTH_SHORT).show()
        }

        val textView =design.fileName
        val editText = design.editText
        val saveButton = design.saveBtn

        textView.setOnClickListener {
            editText.setText(textView.text)
            textView.visibility = View.GONE
            editText.visibility = View.VISIBLE
            saveButton.visibility = View.VISIBLE
        }


        saveButton.setOnClickListener {
            val newName = editText.text.toString().trim()
            if (newName.isNotEmpty()) {
                if (newName.isNotEmpty()) {
                    textView.text = newName
                    updateDraftFileNameInFirestore(newName)
                }
            }
            editText.visibility = View.GONE
            saveButton.visibility = View.GONE
            textView.visibility = View.VISIBLE
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
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Export hatası", Toast.LENGTH_SHORT).show()
                    }
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
            .orderBy("order", Query.Direction.ASCENDING)
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

    fun uploadSortedListToFirestore(sortedList: List<ConversationModel>, studioId: String) {

        val userId = uid ?: return

        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("users")
            .document(userId)
            .collection("studio")
            .document(studioId)
            .collection("conversation")


        sortedList.forEachIndexed { index, item ->
            val data = hashMapOf(
                "order" to index
            )

            collectionRef.document(item.id)
                .update(data as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d("FirebaseUpdate", "Updated ${item.id} with order $index")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseUpdate", "Error updating ${item.id}", e)
                }
        }
    }

    private fun callPaywall(item: String){
        design.paywallComposeView.disposeComposition()
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error -> Log.e("Paywall", "Error: $error") },
            onSuccess = { offerings ->

                val offeringId = when (item) {
                    "subs" -> "second_paywall_yearly_weekly"
                    "character" -> "extra-characters"
                    "slot" -> "Voice_Clone"
                    else -> null
                }
                val offering = offeringId?.let { offerings[offeringId] }

                design.paywallComposeView.apply {

                    setContent {
                        val paywallState by viewModelPaywall.paywallState.collectAsState()

                        if (paywallState is PaywallViewModel.PaywallState.Visible) {
                            if (offering != null) {
                                RevenueCatPaywall(
                                    offering,
                                    onDismiss = { viewModelPaywall.hidePaywall()
                                        design.paywallComposeView.visibility= View.GONE
                                    }
                                )
                            }
                        }
                    }
                }
                viewModelPaywall.showPaywall()
            }
        )
    }

    @Composable
    private fun RevenueCatPaywall(offering: Offering, onDismiss: () -> Unit) {
        val listener = remember {
            object : PaywallListener {
                override fun onPurchaseCompleted(customerInfo: CustomerInfo, transaction: StoreTransaction) {
                    lifecycleScope.launch {
                        dataStoreManager.saveSubscriptionStatus(requireContext(), true)
                    }
                    onDismiss()
                }
                override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                    if (customerInfo.entitlements["Subscription"]?.isActive == true) {
                        lifecycleScope.launch {
                            dataStoreManager.saveSubscriptionStatus(requireContext(), true)
                        }
                        onDismiss()
                    }
                }
            }
        }

        PaywallDialog(
            PaywallDialogOptions.Builder()
                .setRequiredEntitlementIdentifier("Subscription")
                .setOffering(offering)
                .setListener(listener)
                .build()
        )
    }

    private fun updateDraftFileNameInFirestore(newName: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("studio")
            .document(item.id)

        docRef.update("name", newName)
            .addOnSuccessListener {
                Log.d("Firestore", "Dosya adı güncellendi: $newName")
                item = item.copy(name = newName) // local item'ı da güncelle
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Dosya adı güncellenemedi", e)
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