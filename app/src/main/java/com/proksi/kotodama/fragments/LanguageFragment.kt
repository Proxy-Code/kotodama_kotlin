package com.proksi.kotodama.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentLanguageBinding

import com.proksi.kotodama.adapters.LanguageAdapter
import com.proksi.kotodama.models.Language
import kotlinx.coroutines.launch


class LanguageFragment : Fragment() , LanguageAdapter.OnItemClickListener  {

    private lateinit var design: FragmentLanguageBinding
    private lateinit var adapter: LanguageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentLanguageBinding.inflate(inflater,container,false)
        design.languageRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)


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
        val k18 =  Language("ms","Bahasa Melayu", R.drawable.flagmalaysia)
        val k11 =  Language("pt","Português", R.drawable.flagportugal)
        val k12 =  Language("vi","Tiếng Việt", R.drawable.flagvietnam)
        val k13 =  Language("ar","العربية", R.drawable.flagsaudiarabia)
        val k14 =  Language("in","हिन्दी",R.drawable.flagindia)
       // val k16 =  Language("tr","Türkçe", R.drawable.flagturkey)
        val k17 =  Language("it","Italiano", R.drawable.flagitaly)
        val k24 =  Language("el","Ελληνικά", R.drawable.flaggreece)
        val k25 =  Language("hr","Hrvatski", R.drawable.flagcroatia)
        val k19 =  Language("nl","Nederlands", R.drawable.flagnetherlands)
        val k15 =  Language("mc","Bahasa Indonesia",R.drawable.flagindonesia)
        val k28 =  Language("cs","Čeština", R.drawable.flahcek)
        val k20 =  Language("no","Norsk", R.drawable.flagnorway)
        val k21 =  Language("pl","Polski", R.drawable.flagpoland)
        val k22 =  Language("ro","Română", R.drawable.flagromania)
        val k23 =  Language("sk","Slovenčina", R.drawable.flagslovakia)
        val k29 =  Language("fi","Suomi", R.drawable.flagfinland)
        val k26 =  Language("hu","Magyar", R.drawable.flaghungary)
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
        languageList.add(k15)
       // languageList.add(k16)
        languageList.add(k17)
        languageList.add(k18)
        languageList.add(k19)
        languageList.add(k20)
        languageList.add(k21)
        languageList.add(k22)
        languageList.add(k23)
        languageList.add(k24)
        languageList.add(k25)
        languageList.add(k26)
       // languageList.add(k27)
        languageList.add(k28)
        languageList.add(k29)

        adapter = LanguageAdapter(requireContext(), languageList, this)
        design.languageRv.adapter = adapter


        return design.root
    }

    override fun onItemClick(item: Language) {
        Log.d("LANGUAGE", "${item.id} ")

    }
}