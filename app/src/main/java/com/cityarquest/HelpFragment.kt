package com.cityarquest

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cityarquest.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {
    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater:LayoutInflater,container:ViewGroup?,savedInstanceState:Bundle?):View {
        _binding = FragmentHelpBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view:View,savedInstanceState:Bundle?) {
        binding.helpText.text = "Welcome to City AR Quest!\n\n" +
                "1. Explore your city and find quests nearby.\n" +
                "2. Filter quests by difficulty and distance.\n" +
                "3. Start a quest and follow AR instructions.\n" +
                "4. Earn points and badges.\n\n" +
                "Enjoy!"

        binding.helpImage.setImageResource(R.drawable.help_illustration) // иллюстрация в drawable
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Help"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
