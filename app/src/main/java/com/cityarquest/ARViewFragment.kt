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
import androidx.activity.result.contract.ActivityResultContracts
import com.cityarquest.R
import com.cityarquest.data.models.Quest
import com.cityarquest.databinding.FragmentArViewBinding
import com.cityarquest.ui.viewmodel.QuestsViewModel

class ARViewFragment : Fragment() {

    private var _binding: FragmentArViewBinding? = null
    private val binding get() = _binding!!

    private val questsViewModel: QuestsViewModel by viewModels({ requireActivity() })

    private var questId: String? = null
    private var currentQuest: Quest? = null

    private var timer: CountDownTimer? = null
    private var isScanning = false
    private var timeLimit = 60000L // 60 секунд по умолчанию

    companion object {
        private const val ARG_QUEST_ID = "arg_quest_id"
        fun newInstance(questId: String): ARViewFragment {
            val fragment = ARViewFragment()
            val args = Bundle()
            args.putString(ARG_QUEST_ID, questId)
            fragment.arguments = args
            return fragment
        }
    }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(requireContext(), getString(R.string.camera_permission_denied), Toast.LENGTH_LONG).show()
            } else {
                // Разрешение есть, можно продолжать инициализацию AR
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questId = arguments?.getString(ARG_QUEST_ID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentArViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        checkCameraPermission()

        // Наблюдаем за списком квестов
        questsViewModel.quests.observe(viewLifecycleOwner) { list ->
            currentQuest = list.find { it.id == questId }
            currentQuest?.let { quest ->
                // Если супер-квест, ставим 30 сек
                if (quest.isSuperQuest) {
                    timeLimit = 30000L
                }

                // Покажем подсказки в instructionsText, если есть arHints
                val hints = quest.arHints ?: getString(R.string.ar_instructions)
                binding.instructionsText.text = hints

                startTimer(timeLimit)
            }
        }

        // Кнопка "Сканировать объект"
        binding.scanButton.setOnClickListener {
            if (!isScanning) {
                isScanning = true
                questsViewModel.addPoints(10)
                Toast.makeText(requireContext(), "Сканирование успешно! +10 очков", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Уже отсканировано!", Toast.LENGTH_SHORT).show()
            }
        }

        // Кнопка "Завершить квест"
        binding.completeQuestButton.setOnClickListener {
            questsViewModel.completeActiveQuest(20)
            Toast.makeText(requireContext(), "Квест завершён! +20 очков!", Toast.LENGTH_SHORT).show()
            // Возвращаемся назад
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun startTimer(millis: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000).toInt()
                binding.timerText.text = getString(R.string.time_left, sec)
            }

            override fun onFinish() {
                Toast.makeText(requireContext(), getString(R.string.quest_failed_timeup), Toast.LENGTH_LONG).show()
                // Завершаем без очков
                questsViewModel.completeActiveQuest(0)
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }.start()
    }

    private fun checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.ar_experience)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        _binding = null
    }
}
