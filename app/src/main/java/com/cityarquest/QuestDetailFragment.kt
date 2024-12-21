package com.cityarquest

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.cityarquest.R
import com.cityarquest.data.models.Quest
import com.cityarquest.databinding.FragmentQuestDetailBinding
import com.cityarquest.ui.viewmodel.QuestsViewModel

class QuestDetailFragment : Fragment() {

    private var _binding: FragmentQuestDetailBinding? = null
    private val binding get() = _binding!!

    private val questsViewModel: QuestsViewModel by viewModels({ requireActivity() })

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Подписываемся на изменения списка квестов
        questsViewModel.quests.observe(viewLifecycleOwner) { quests ->
            currentQuest = quests.find { it.id == questId }
            currentQuest?.let { quest ->
                // Заполняем UI
                binding.questTitle.text = quest.title
                binding.questDescription.text = quest.description
                binding.questDifficulty.text = getString(R.string.difficulty_level, quest.difficulty)
                binding.questPoints.text = getString(R.string.quest_points, quest.points)

                // Если есть поле fullDescription
                binding.questFullDescription.text = quest.fullDescription
                    ?: getString(R.string.no_full_description)

                // Загружаем картинку (при наличии imageUrl)
                if (!quest.imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(quest.imageUrl)
                        .placeholder(R.drawable.ic_quests)
                        .into(binding.questImage)
                }

                // Обновляем заголовок тулбара
                (activity as? AppCompatActivity)?.supportActionBar?.title = quest.title
            }
        }

        // Кнопка "Начать квест"
        binding.startArButton.setOnClickListener {
            currentQuest?.let { quest ->
                confirmStartQuest(quest)
            }
        }
    }

    private fun confirmStartQuest(quest: Quest) {
        val active = questsViewModel.activeQuest.value
        if (active != null && active.id != quest.id) {
            // Уже активен другой квест
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirmation))
                .setMessage(getString(R.string.quest_already_active))
                .setPositiveButton("Yes") { _, _ ->
                    questsViewModel.completeActiveQuest()
                    questsViewModel.startQuest(quest)
                    navigateToARView(quest.id)
                }
                .setNegativeButton("No", null)
                .show()
        } else {
            // Нет активного или тот же
            questsViewModel.startQuest(quest)
            navigateToARView(quest.id)
        }
    }

    private fun navigateToARView(questId: String) {
        (activity as? MainActivity)?.navigateToARView(questId)
    }

    override fun onResume() {
        super.onResume()
        // Если вдруг хотим ставить заголовок тут
        val title = currentQuest?.title ?: getString(R.string.quest_details)
        (activity as? AppCompatActivity)?.supportActionBar?.title = title
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
