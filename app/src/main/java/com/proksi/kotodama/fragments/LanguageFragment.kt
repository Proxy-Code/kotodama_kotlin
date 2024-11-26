package com.proksi.kotodama.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentLanguageBinding
import com.proksi.kotodama.MainActivity
import com.proksi.kotodama.models.Language
import com.proksi.kotodama.adapters.LanguageAdapter
import com.proksi.kotodama.dataStore.LanguagePreferences
import kotlinx.coroutines.launch
import java.util.Locale


class LanguageFragment : Fragment() , LanguageAdapter.OnItemClickListener  {

    private lateinit var design: FragmentLanguageBinding
    private lateinit var adapter: LanguageAdapter
    private lateinit var languagePreferences: LanguagePreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentLanguageBinding.inflate(inflater,container,false)
        design.languageRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        languagePreferences = LanguagePreferences(this.requireContext())


        val languageList = ArrayList<Language>()

        val k1 =  Language("en","English", R.drawable.flagus)
        val k2 =  Language("ja","日本語",  R.drawable.flagjapan)
        val k3 =  Language("en","English (UK)", R.drawable.flagengland)
        val k4 =  Language("de","Deutsch",R.drawable.flaggermany)
        val k5 =  Language("fr","Français", R.drawable.flagfrance)
        val k6 =  Language("es","Español",R.drawable.flagspain)
        val k7 =  Language("ko","한국어",R.drawable.flagsouthkorea)
        val k8 =  Language("zh","中文 (简体)", R.drawable.flagchina)
        val k9 =  Language("zh","中文 (繁體)", R.drawable.flagchina)
        val k10 = Language("ru","Русский", R.drawable.flagrussia)
        val k11 =  Language("pt","Português", R.drawable.flagportugal)
        val k12 =  Language("vi","Tiếng Việt", R.drawable.flagvietnam)
        val k13 =  Language("ar","العربية", R.drawable.flagsaudiarabia)
        val k14 =  Language("in","हिन्दी",R.drawable.flagindia)
        val k16 =  Language("tr","Türkçe", R.drawable.flagturkey)
     //   val k27 =  Language("uk","Українська", R.drawable.flagukraine)


        languageList.add(k1)
        languageList.add(k2)
        languageList.add(k3)
        languageList.add(k4)
        languageList.add(k5)
        languageList.add(k6)
        languageList.add(k7)
        languageList.add(k8)
        languageList.add(k9)
        languageList.add(k10)
        languageList.add(k11)
        languageList.add(k12)
        languageList.add(k13)
        languageList.add(k14)
        languageList.add(k16)


        adapter = LanguageAdapter(requireContext(), languageList, this)
        design.languageRv.adapter = adapter


        return design.root
    }


    override fun onItemClick(item:Language) {
        Log.d("LANGUAGE", "${item.id} ")
        lifecycleScope.launch {
            languagePreferences.setLanguage(item.id)
            updateLanguage(item.id)
            restartApp()
        }
    }


    private fun restartApp() {
        lifecycleScope.launch {
            //  delay(500) // optional delay to ensure configuration is updated
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun updateLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        config.setLayoutDirection(locale)
        // Refresh the current fragment to apply the language change
        parentFragmentManager.beginTransaction().detach(this).attach(this).commit()

    }
}