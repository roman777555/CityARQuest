package com.cityarquest.data

import android.content.Context
import android.util.Log
import com.cityarquest.data.models.Quest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object LocalDataSource {

    private const val EARTH_RADIUS_KM = 6371.0
    private const val LAT_LON_PRECISION = 4

    // Кэш результатов и максимальный радиус
    private var cachedQuests = mutableListOf<Quest>()
    private var cachedMaxRadius = 0.0

    /**
     * Генерирует список квестов, учитывая категории (Food, Nature, Culture, Historical).
     * При расширении радиуса делаем новые запросы.
     */
    suspend fun generateQuests(
        context: Context,
        userLat: Double,
        userLon: Double,
        radiusKm: Double,
        apiKey: String
    ): List<Quest> = withContext(Dispatchers.IO) {
        val radiusMeters = (radiusKm * 1000).toInt()

        // Если радиус стал больше - делаем запросы по категориям, пополняем кэш
        if (radiusKm > cachedMaxRadius) {
            val newPlaces = fetchPlacesForAllCategories(userLat, userLon, radiusMeters, apiKey)
            cachedQuests.addAll(newPlaces)
            // Удаляем дубликаты по ID
            cachedQuests = cachedQuests.distinctBy { it.id }.toMutableList()
            cachedMaxRadius = radiusKm
        }

        // Возвращаем объекты, что попадают в заданный радиус
        cachedQuests.filter {
            calculateDistance(userLat, userLon, it.latitude, it.longitude) <= radiusKm
        }
    }

    /**
     * Собираем объекты для разных категорий, чтобы охватить интересы разных людей.
     *
     * Пример:
     *  1) Food:    [type=restaurant, type=cafe]
     *  2) Nature:  [type=park]
     *  3) Culture: [type=museum, type=tourist_attraction]
     *  4) Historical: [type=monument] или keyword=historical|ruins
     *
     * Ограничим максимум объектов на категорию, чтобы не перегружать одинаковыми местами.
     */
    private suspend fun fetchPlacesForAllCategories(
        userLat: Double,
        userLon: Double,
        radiusMeters: Int,
        apiKey: String
    ): List<Quest> {

        val allPlaces = mutableListOf<Quest>()

        // 1. Food (restaurant, cafe)
        val foodPlaces = fetchPlacesMultiTypes(
            userLat, userLon, radiusMeters, apiKey,
            listOf("restaurant", "cafe")
        )
        allPlaces.addAll(limitByCategory(foodPlaces, 5))

        // 2. Nature (park)
        val naturePlaces = fetchPlacesMultiTypes(
            userLat, userLon, radiusMeters, apiKey,
            listOf("park")
        )
        allPlaces.addAll(limitByCategory(naturePlaces, 3))

        // 3. Culture (museum, tourist_attraction)
        val culturePlaces = fetchPlacesMultiTypes(
            userLat, userLon, radiusMeters, apiKey,
            listOf("museum", "tourist_attraction")
        )
        allPlaces.addAll(limitByCategory(culturePlaces, 4))

        // 4. Historical (монумент, + keyword=historical|ruins)
        //    Можно сделать 2 запроса: один с type=monument, второй c keyword=historical|ruins
        val historicalMonument = fetchPlacesByType(
            userLat, userLon, radiusMeters, apiKey, "monument"
        )
        // "Keyword" запрос
        val historicalKeyword = fetchPlacesByKeyword(
            userLat, userLon, radiusMeters, apiKey, "historical|ruins"
        )

        val histAll = (historicalMonument + historicalKeyword).distinctBy { it.id }
        allPlaces.addAll(limitByCategory(histAll, 4))

        // Возвращаем общий список (без дубликатов)
        return allPlaces.distinctBy { it.id }
    }

    /**
     * Делаем несколько запросов: для каждого type в typeList.
     * Объединяем результаты (каждый type может дать до 3 страниц).
     */
    private suspend fun fetchPlacesMultiTypes(
        userLat: Double,
        userLon: Double,
        radiusMeters: Int,
        apiKey: String,
        typeList: List<String>
    ): List<Quest> {
        val places = mutableListOf<Quest>()
        for (t in typeList) {
            val result = fetchPlacesByType(userLat, userLon, radiusMeters, apiKey, t)
            places.addAll(result)
        }
        return places
    }

    /**
     * Запрос Nearby Search по type=... c пагинацией (до 3 страниц).
     */
    private suspend fun fetchPlacesByType(
        userLat: Double,
        userLon: Double,
        radiusMeters: Int,
        apiKey: String,
        typeParam: String
    ): List<Quest> {
        val result = mutableListOf<Quest>()
        var pageToken: String? = null
        var pageCount = 0
        val maxPages = 3

        do {
            val urlString = if (pageToken == null) {
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                        "location=$userLat,$userLon&radius=$radiusMeters&type=$typeParam&key=$apiKey"
            } else {
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                        "pagetoken=$pageToken&key=$apiKey"
            }

            val (places, nextToken) = loadPlacesPage(urlString)
            result.addAll(places)

            pageToken = nextToken
            pageCount++

            if (pageToken != null && pageCount < maxPages) {
                delay(2000) // Google рекомендует подождать 2 сек
            }
        } while (pageToken != null && pageCount < maxPages)

        return result
    }

    /**
     * Запрос Nearby Search c keyword=... (вместо type=...).
     * Можно использовать для "historical|ruins".
     */
    private suspend fun fetchPlacesByKeyword(
        userLat: Double,
        userLon: Double,
        radiusMeters: Int,
        apiKey: String,
        keyword: String
    ): List<Quest> {
        val result = mutableListOf<Quest>()
        var pageToken: String? = null
        var pageCount = 0
        val maxPages = 3

        do {
            val urlString = if (pageToken == null) {
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                        "location=$userLat,$userLon&radius=$radiusMeters&keyword=$keyword&key=$apiKey"
            } else {
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                        "pagetoken=$pageToken&key=$apiKey"
            }

            val (places, nextToken) = loadPlacesPage(urlString)
            result.addAll(places)

            pageToken = nextToken
            pageCount++

            if (pageToken != null && pageCount < maxPages) {
                delay(2000)
            }
        } while (pageToken != null && pageCount < maxPages)

        return result
    }

    /**
     * Подгружаем одну "страницу" Nearby Search, возвращаем (список Quest, next_page_token).
     * Логика сложности => меньше красных: рейтинг < 3.0 = Hard,
     * а также если есть "abandoned", "ruins", "historical" в types => Hard.
     */
    private fun loadPlacesPage(urlString: String): Pair<List<Quest>, String?> {
        val places = mutableListOf<Quest>()
        var nextPageToken: String? = null

        (URL(urlString).openConnection() as? HttpsURLConnection)?.apply {
            requestMethod = "GET"
            connect()

            inputStream.bufferedReader().use { reader ->
                val response = reader.readText()
                val json = JSONObject(response)

                val status = json.optString("status", "UNKNOWN")
                if (status != "OK") {
                    Log.w("LocalDataSource", "NearbySearch status=$status")
                }

                nextPageToken = json.optString("next_page_token", null)
                val results = json.optJSONArray("results") ?: return@use

                for (i in 0 until results.length()) {
                    val place = results.getJSONObject(i)
                    val name = place.optString("name", "Unknown Place")
                    val geometry = place.optJSONObject("geometry")
                    val location = geometry?.optJSONObject("location")
                    val lat = location?.optDouble("lat", 0.0) ?: 0.0
                    val lon = location?.optDouble("lng", 0.0) ?: 0.0
                    val rating = place.optDouble("rating", 0.0)

                    val typesArray = place.optJSONArray("types") ?: org.json.JSONArray()
                    val types = mutableListOf<String>()
                    for (j in 0 until typesArray.length()) {
                        types.add(typesArray.getString(j).lowercase())
                    }

                    val (difficulty, points) = calculateDifficultyAndPoints(rating, types)
                    val questType = getPrimaryType(types)

                    val quest = Quest(
                        id = "quest_${lat.roundTo(LAT_LON_PRECISION)}_${lon.roundTo(LAT_LON_PRECISION)}",
                        title = name,
                        type = questType,
                        description = generateQuestDescription(questType, name),
                        difficulty = difficulty,
                        latitude = lat.roundTo(LAT_LON_PRECISION),
                        longitude = lon.roundTo(LAT_LON_PRECISION),
                        isSuperQuest = (difficulty == 3),
                        points = points,
                        fullDescription = "Discover more about $name and its history.",
                        arHints = "Use AR to find hidden treasures at $name.",
                        imageUrl = place.optString("icon", null) // Или другой источник URL
                    )
                    places.add(quest)
                }
            }
            disconnect()
        }
        return places to nextPageToken
    }


    /**
     * Берём не более N объектов из списка (например, 5).
     * Можно сортировать, например, по рейтингу (у нас rating в difficulty не хранится).
     * Если хотите, можно сначала сортировать places по расстоянию или по title.
     */
    private fun limitByCategory(places: List<Quest>, maxCount: Int): List<Quest> {
        // Например, сортируем по названию (алфавит), а потом берём первые maxCount.
        val sorted = places.sortedBy { it.title }
        return sorted.take(maxCount)
    }

    /**
     * Возвращаем более человеческий тип для описания (Restaurant, Cafe, Park, Museum, Tourist Attraction),
     * если ни одного не нашли, пишем Unknown.
     */
    private fun getPrimaryType(types: List<String>): String {
        val priority = listOf("restaurant", "cafe", "park", "museum", "tourist_attraction", "monument")
        for (t in priority) {
            if (types.contains(t)) {
                return t.replaceFirstChar { it.uppercase() }
            }
        }
        return "Unknown"
    }

    /**
     * Генерация описания по типу
     */
    private fun generateQuestDescription(type: String, name: String): String {
        return when (type.lowercase()) {
            "restaurant" -> "Discover the best dishes at $name."
            "cafe" -> "Enjoy a relaxing time at $name."
            "park" -> "Feel the nature in $name."
            "museum" -> "Explore the exhibitions at $name."
            "tourist attraction" -> "Experience the allure of $name."
            "monument" -> "Learn about the history of $name."
            else -> "Explore the intriguing location: $name."
        }
    }

    /**
     * Логика для меньше красных (Hard).
     * - Hard, если rating < 3.0, или types содержит abandoned, ruins, historical
     * - Easy, если rating >= 4.5
     * - Medium — всё остальное
     */
    private fun calculateDifficultyAndPoints(
        rating: Double,
        types: List<String>
    ): Pair<Int, Int> {
        val isAbandoned = types.contains("abandoned") || types.contains("abandoned_place")
        val isRuins = types.contains("ruins")
        val isHistorical = types.contains("historical") || types.contains("historical_site")

        val difficulty = when {
            rating < 3.0 || isAbandoned || isRuins || isHistorical -> 3
            rating >= 4.5 -> 1
            else -> 2
        }

        val points = when (difficulty) {
            1 -> 10
            2 -> 20
            3 -> 30
            else -> 20
        }
        return difficulty to points
    }

    /**
     * Вычисляем расстояние между точками (км) по формуле гаверсина
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    private fun Double.roundTo(decimals: Int): Double {
        return String.format("%.${decimals}f", this).toDouble()
    }
}
