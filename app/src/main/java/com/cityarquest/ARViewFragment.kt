package com.cityarquest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cityarquest.databinding.FragmentArViewBinding
import com.cityarquest.data.models.Quest
import com.cityarquest.ui.viewmodel.ProfileViewModel
import com.cityarquest.ui.viewmodel.QuestsViewModel

class ARViewFragment : Fragment() {

    private var _binding: FragmentArViewBinding? = null
    private val binding get() = _binding!!
    private var questId: String? = null

    private val questsViewModel: QuestsViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    private var startTime = 0L
    private var timeLimit = 60000L // 60 секунд по умолчанию
    private var currentQuest: Quest? = null
    private var timer: CountDownTimer? = null

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1001
        private const val ARG_QUEST_ID = "arg_quest_id"

        fun newInstance(questId: String): ARViewFragment {
            val fragment = ARViewFragment()
            val args = Bundle()
            args.putString(ARG_QUEST_ID, questId)
            fragment.arguments = args
            return fragment
        }

        fun getNeighborhood(lat: Double, lon: Double): String {
            // Возвращаем фиктивное название района для примера.
            return "Downtown"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questId = arguments?.getString(ARG_QUEST_ID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
        _binding = FragmentArViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        checkCameraPermission()

        // Подписываемся на список квестов, чтобы найти текущий квест и настроить логику
        questsViewModel.quests.observe(viewLifecycleOwner) { questList ->
            currentQuest = questList.find { it.id == questId }
            currentQuest?.let { quest ->
                // Определяем базовые очки
                val basePoints = if (quest.isSuperQuest) quest.difficulty * 20 else quest.difficulty * 10

                // Если супер-квест, уменьшим время
                if (quest.isSuperQuest) {
                    timeLimit = 30000L // 30 секунд для супер-квеста
                }

                // Запоминаем время старта
                startTime = System.currentTimeMillis()
                startTimer(timeLimit)

                // Кнопка сканирования
                binding.scanButton.setOnClickListener {
                    // Предположим моментально успешное сканирование
                    timer?.cancel()
                    val endTime = System.currentTimeMillis()
                    val spentTimeSec = (endTime - startTime) / 1000
                    val neighborhood = getNeighborhood(quest.latitude, quest.longitude)

                    // Завершение квеста
                    profileViewModel.completeQuest(quest, basePoints, spentTimeSec, neighborhood)

                    // Показываем тост с результатом
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.quest_completed_points, basePoints, spentTimeSec),
                        Toast.LENGTH_LONG
                    ).show()

                    // Возвращаемся назад
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }

        // Загружаем квесты, если не загружены
        questsViewModel.loadQuests()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.ar_experience)
    }

    private fun startTimer(millis: Long) {
        timer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000).toInt()
                binding.timerText.text = getString(R.string.time_left, sec)
            }

            override fun onFinish() {
                Toast.makeText(requireContext(), getString(R.string.quest_failed_timeup), Toast.LENGTH_LONG).show()
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }.start()
    }

    private fun checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        _binding = null
    }
}
