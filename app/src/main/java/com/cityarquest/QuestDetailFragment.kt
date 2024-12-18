package com.cityarquest

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cityarquest.data.models.Quest
import com.cityarquest.databinding.FragmentQuestDetailBinding
import com.cityarquest.ui.viewmodel.QuestsViewModel

class QuestDetailFragment : Fragment() {

    private var _binding: FragmentQuestDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuestsViewModel by viewModels()
    private var questId: String? = null
    private var currentQuest: Quest? = null

    companion object {
        private const val ARG_QUEST_ID = "arg_quest_id"
        fun newInstance(questId: String): QuestDetailFragment {
            val fragment = QuestDetailFragment()
            val args = Bundle()
            args.putString(ARG_QUEST_ID, questId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questId = arguments?.getString(ARG_QUEST_ID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.quests.observe(viewLifecycleOwner) { quests ->
            currentQuest = quests.find { it.id == questId }
            currentQuest?.let { quest ->
                binding.questTitle.text = quest.title
                binding.questDescription.text = quest.description
                binding.questDifficulty.text = "Difficulty: ${quest.difficulty}"
                val basePoints = if (quest.isSuperQuest) quest.difficulty*20 else quest.difficulty*10
                binding.questPoints.text = "Points: $basePoints"
                // Показываем картинку квеста (можно хранить имя картинки в Quest)
                binding.questImage.setImageResource(quest.imageResId)
            }
        }

        viewModel.loadQuests()

        binding.startArButton.setOnClickListener {
            currentQuest?.id?.let {
                (activity as? MainActivity)?.navigateToARView(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val title = currentQuest?.title ?: "Quest Details"
        (activity as? AppCompatActivity)?.supportActionBar?.title = title
        // Можно иконку по условию
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
