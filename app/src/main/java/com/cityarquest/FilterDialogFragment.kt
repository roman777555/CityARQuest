package com.cityarquest

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class FilterDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(currentRadius: Double, currentDifficulty: Int, onApply: (Double, Int) -> Unit): FilterDialogFragment {
            val f = FilterDialogFragment()
            f.radius = currentRadius
            f.minDifficulty = currentDifficulty
            f.onApply = onApply
            return f
        }
    }

    private var radius: Double = 10.0
    private var minDifficulty = 1
    private var onApply: ((Double, Int) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(android.R.layout.simple_list_item_2, null)
        // Можно сделать более красивый layout

        // Упростим пример: используем просто два EditText
        val radiusEdit = EditText(requireContext())
        radiusEdit.hint = "Radius in miles"
        radiusEdit.setText(radius.toString())

        val difficultyEdit = EditText(requireContext())
        difficultyEdit.hint = "Min difficulty"
        difficultyEdit.setText(minDifficulty.toString())

        val layout = android.widget.LinearLayout(requireContext())
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.addView(radiusEdit)
        layout.addView(difficultyEdit)

        return AlertDialog.Builder(requireContext())
            .setTitle("Filter Quests")
            .setView(layout)
            .setPositiveButton("Apply") { _, _ ->
                val r = radiusEdit.text.toString().toDoubleOrNull() ?: radius
                val d = difficultyEdit.text.toString().toIntOrNull() ?: minDifficulty
                onApply?.invoke(r, d)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}
