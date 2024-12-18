package com.cityarquest

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cityarquest.databinding.FragmentProfileBinding
import com.cityarquest.ui.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by viewModels()

    companion object {
        fun newInstance() = ProfileFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        profileViewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.userNameText.text = name
        }

        profileViewModel.userPoints.observe(viewLifecycleOwner) { points ->
            binding.userPointsText.text = "Points: $points"
        }

        profileViewModel.completedQuests.observe(viewLifecycleOwner) { list ->
            val count = list.size
            binding.completedCountText.text = "Completed: $count quests"
            binding.averageDifficultyText.text = "Avg Difficulty: %.1f".format(profileViewModel.getAverageDifficulty())
        }

        profileViewModel.badges.observe(viewLifecycleOwner) { badges ->
            if (badges.isEmpty()) {
                binding.badgesText.text = "Badges: None"
            } else {
                binding.badgesText.text = "Badges: ${badges.joinToString(", ")}"
            }
        }

        binding.editNameButton.setOnClickListener {
            showEditNameDialog()
        }
    }

    private fun showEditNameDialog() {
        val editText = EditText(requireContext())
        editText.hint = "Enter your name"

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    profileViewModel.updateUserName(newName) // Исправленный метод
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Profile"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
