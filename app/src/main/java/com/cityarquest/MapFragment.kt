package com.cityarquest

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cityarquest.data.models.Quest
import com.cityarquest.databinding.FragmentMapBinding
import com.cityarquest.ui.viewmodel.QuestsViewModel
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlin.math.*

// Простая функция для расчета расстояния между координатами:
fun distanceInMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 3958.8 // радиус земли в милях
    val dLat = Math.toRadians(lat2-lat1)
    val dLon = Math.toRadians(lon2-lon1)
    val a = sin(dLat/2)*sin(dLat/2)+cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(dLon/2)*sin(dLon/2)
    val c = 2*atan2(sqrt(a), sqrt(1-a))
    return R*c
}

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuestsViewModel by viewModels()
    private var gMap: GoogleMap? = null

    private var currentRadiusMiles = 10.0
    private var currentMinDifficulty = 1

    private val userLat = 40.7128
    private val userLon = -74.0060

    companion object {
        fun newInstance() = MapFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Чтобы добавить меню с фильтром
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        viewModel.quests.observe(viewLifecycleOwner) { quests ->
            if (gMap != null && quests != null) {
                addQuestMarkers(filterQuests(quests))
            }
        }

        viewModel.loadQuests()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Quests Nearby"
        (activity as? AppCompatActivity)?.supportActionBar?.setIcon(null) // Можно кастомизировать
        // Можно попробовать custom view для Toolbar, но для примера оставим так
    }

    private fun filterQuests(all: List<Quest>): List<Quest> {
        return all.filter { quest ->
            val dist = distanceInMiles(userLat, userLon, quest.latitude, quest.longitude)
            dist <= currentRadiusMiles && quest.difficulty >= currentMinDifficulty
        }
    }

    private fun addQuestMarkers(quests: List<Quest>) {
        gMap?.clear()
        for (quest in quests) {
            val color = when {
                quest.difficulty <= 1 -> BitmapDescriptorFactory.HUE_GREEN
                quest.difficulty == 2 -> BitmapDescriptorFactory.HUE_YELLOW
                else -> BitmapDescriptorFactory.HUE_RED
            }
            val marker = gMap?.addMarker(
                MarkerOptions()
                    .position(LatLng(quest.latitude, quest.longitude))
                    .title(quest.title)
                    .icon(BitmapDescriptorFactory.defaultMarker(color))
            )
            marker?.tag = quest.id
        }
    }

    override fun onMapReady(map: GoogleMap) {
        gMap = map
        gMap?.uiSettings?.isZoomControlsEnabled = true
        gMap?.setOnMarkerClickListener { marker ->
            val questId = marker.tag as? String
            questId?.let {
                (activity as? MainActivity)?.navigateToQuestDetail(it)
            }
            true
        }

        val defaultLocation = LatLng(userLat, userLon)
        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14f))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val filterItem = menu.add("Filter")
        filterItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        filterItem.setIcon(R.drawable.ic_filter)
        filterItem.icon?.setTint(resources.getColor(android.R.color.white, null))
        filterItem.setOnMenuItemClickListener {
            FilterDialogFragment.newInstance(currentRadiusMiles, currentMinDifficulty) { radius, difficulty ->
                currentRadiusMiles = radius
                currentMinDifficulty = difficulty
                viewModel.quests.value?.let {
                    addQuestMarkers(filterQuests(it))
                }
            }.show(parentFragmentManager, "FilterDialog")
            true
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
