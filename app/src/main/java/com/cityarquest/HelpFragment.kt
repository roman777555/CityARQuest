package com.cityarquest

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.cityarquest.R
import com.cityarquest.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {
    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.helpText.text = """
            Добро пожаловать в Городской AR-Квест!

            Основные возможности:
            1. На карте вы видите точки квестов. Нажмите на метку, чтобы узнать детали квеста.
            2. Можно начать только один квест одновременно. Если у вас уже есть активный квест, 
               вы должны завершить его или отказаться, прежде чем начать новый.
            3. При начале квеста можно перейти в AR-режим: 
               - Нажмите "Сканировать" для проверки объектов, за каждое распознавание +10 очков.
               - Если время заканчивается — квест считается проваленным.
            4. Завершая квест, вы получаете дополнительные очки.
            5. Суммарные очки копятся в вашем профиле.

            Приятной игры!
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
