package com.proksi.kotodama.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.proksi.kotodama.fragments.BoardingOneFragment
import com.proksi.kotodama.fragments.BoardingThreeFragment
import com.proksi.kotodama.fragments.BoardingTwoFragment

class OnboardingFragmentAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BoardingOneFragment()
            1 -> BoardingTwoFragment()
            2 -> BoardingThreeFragment()
            else -> BoardingOneFragment()
        }
    }
}