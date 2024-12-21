package com.cityarquest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.cityarquest.data.models.Quest
import com.cityarquest.databinding.FragmentMapBinding
import com.cityarquest.ui.viewmodel.QuestsViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val questsViewModel: QuestsViewModel by viewModels()
    private var gMap: GoogleMap? = null

    private var selectedRadiusKm: Double = 2.0
    private val activeMarkers = mutableMapOf<String, Marker>()

    companion object {
        private const val TAG = "MapFragment"
        fun newInstance() = MapFragment()
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableMyLocation()
            } else {
                showPermissionDeniedMessage()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupDistanceFilter()

        // Наблюдаем за списком квестов
        questsViewModel.quests.observe(viewLifecycleOwner) { quests ->
            Log.d(TAG, "Received ${quests.size} quests")
            showLoading(false)
            updateMapMarkers(quests)
        }
    }

    private fun setupDistanceFilter() {
        val spinner = binding.distanceFilterSpinner
        spinner.setSelection(0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRadiusKm = when (position) {
                    0 -> 2.0
                    1 -> 5.0
                    2 -> 10.0
                    else -> 2.0
                }
                Log.d(TAG, "Selected radius: $selectedRadiusKm km")
                loadQuestsForCurrentLocation(selectedRadiusKm)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onMapReady(map: GoogleMap) {
        gMap = map
        gMap?.uiSettings?.isZoomControlsEnabled = true
        gMap?.setInfoWindowAdapter(CustomInfoWindowAdapter())

        // При клике на info window → показать детали квеста
        gMap?.setOnInfoWindowClickListener { marker ->
            val quest = marker.tag as? Quest ?: return@setOnInfoWindowClickListener
            // Переходим к детальному фрагменту
            (activity as? MainActivity)?.navigateToQuestDetail(quest.id)
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            enableMyLocation()
        }
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(requireContext(), getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            gMap?.isMyLocationEnabled = true
            moveToCurrentLocationAndLoadQuests()
        }
    }

    private fun moveToCurrentLocationAndLoadQuests() {
        showLoading(true)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                Log.d(TAG, "Moved camera to user's location: $latLng")
                loadQuestsForRadius(location.latitude, location.longitude, selectedRadiusKm)
            } else {
                showLoading(false)
                Toast.makeText(requireContext(), getString(R.string.failed_to_obtain_location), Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            showLoading(false)
            Toast.makeText(requireContext(), getString(R.string.error_obtaining_location, it.message ?: "Unknown error"), Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error getting location", it)
        }
    }

    private fun loadQuestsForCurrentLocation(radiusKm: Double) {
        showLoading(true)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                loadQuestsForRadius(location.latitude, location.longitude, radiusKm)
            } else {
                showLoading(false)
                Toast.makeText(requireContext(), getString(R.string.failed_to_obtain_location), Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            showLoading(false)
            Toast.makeText(requireContext(), getString(R.string.error_obtaining_location, it.message ?: "Unknown error"), Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error getting location", it)
        }
    }

    private fun loadQuestsForRadius(userLat: Double, userLon: Double, radiusKm: Double) {
        lifecycleScope.launch {
            showLoading(true)
            questsViewModel.loadQuests(requireContext(), userLat, userLon, radiusKm)
            showLoading(false)
        }
    }

    private fun updateMapMarkers(quests: List<Quest>) {
        Log.d(TAG, "Updating map markers")
        val newIds = quests.map { it.id }.toSet()
        val oldIds = activeMarkers.keys.toSet()

        val toAdd = quests.filter { it.id !in oldIds }
        val toRemove = oldIds.filter { it !in newIds }

        toAdd.forEach { quest ->
            val marker = gMap?.addMarker(
                MarkerOptions()
                    .position(LatLng(quest.latitude, quest.longitude)) // Используем latitude и longitude
                    .title("${quest.title} (${quest.type})") // Используем type
                    .snippet("Difficulty: ${getDifficultyText(quest.difficulty)}, Points: ${quest.points}")
                    .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColorByDifficulty(quest.difficulty)))
            )
            marker?.tag = quest // Устанавливаем tag для маркера
            if (marker != null) {
                activeMarkers[quest.id] = marker
            }
        }

        toRemove.forEach { questId ->
            activeMarkers[questId]?.remove()
            activeMarkers.remove(questId)
        }
    }


    private fun getMarkerColorByDifficulty(difficulty: Int): Float {
        return when (difficulty) {
            1 -> BitmapDescriptorFactory.HUE_GREEN
            2 -> BitmapDescriptorFactory.HUE_ORANGE
            3 -> BitmapDescriptorFactory.HUE_RED
            else -> BitmapDescriptorFactory.HUE_BLUE
        }
    }

    private fun getDifficultyText(difficulty: Int): String {
        return when (difficulty) {
            1 -> getString(R.string.difficulty_easy)
            2 -> getString(R.string.difficulty_medium)
            3 -> getString(R.string.difficulty_hard)
            else -> getString(R.string.unknown)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Кастомный InfoWindow
     */
    private inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {

        private val window: View = layoutInflater.inflate(R.layout.custom_info_window, null)

        private fun render(marker: Marker, view: View) {
            val quest = marker.tag as? Quest
            val titleTextView: TextView = view.findViewById(R.id.title)
            val snippetTextView: TextView = view.findViewById(R.id.snippet)

            titleTextView.text = marker.title
            snippetTextView.text = marker.snippet
                ?: "Difficulty: ${getDifficultyText(quest?.difficulty ?: 0)}"
        }

        override fun getInfoWindow(marker: Marker): View? {
            render(marker, window)
            // Возвращаем window, чтобы был чёрный фон
            return window
        }

        override fun getInfoContents(marker: Marker): View? {
            // Если getInfoWindow не null, тогда getInfoContents можно вернуть null
            return null
        }
    }
}
