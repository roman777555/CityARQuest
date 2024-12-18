package com.cityarquest

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.cityarquest.databinding.FragmentCompletedQuestsBinding
import com.cityarquest.ui.viewmodel.ProfileViewModel
import com.cityarquest.data.repository.CompletedQuestData


class CompletedQuestsFragment : Fragment() {

    private var _binding: FragmentCompletedQuestsBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by viewModels()

    private val adapter = CompletedQuestsAdapter()

    companion object {
        fun newInstance() = CompletedQuestsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCompletedQuestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.completedQuestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.completedQuestsRecyclerView.adapter = adapter

        profileViewModel.completedQuests.observe(viewLifecycleOwner) { completed ->
            if (completed.isEmpty()) {
                // Можно показать TextView "No completed quests yet."
            }
            adapter.submitList(completed)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.completed_quests)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CompletedQuestsAdapter : androidx.recyclerview.widget.ListAdapter<CompletedQuestData, CompletedQuestsViewHolder>(DiffCallbackCompleted()) {
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CompletedQuestsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_completed_quest, parent, false)
        return CompletedQuestsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompletedQuestsViewHolder, position: Int) {
        val quest = getItem(position)
        holder.bind(quest)
    }
}


class CompletedQuestsViewHolder(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
    private val title = itemView.findViewById<android.widget.TextView>(R.id.completedQuestTitle)
    fun bind(q: CompletedQuestData) {
        title.text = "${q.title}\nNeighborhood: ${q.neighborhood}\nPoints: ${q.pointsEarned}\nTime: ${q.spentTimeSec}s"
    }
}



class DiffCallbackCompleted : androidx.recyclerview.widget.DiffUtil.ItemCallback<CompletedQuestData>() {
    override fun areItemsTheSame(oldItem: CompletedQuestData, newItem: CompletedQuestData) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: CompletedQuestData, newItem: CompletedQuestData) = oldItem == newItem
}
