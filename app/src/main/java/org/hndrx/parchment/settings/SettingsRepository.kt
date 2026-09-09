package org.hndrx.parchment.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONObject
import org.json.JSONArray

private val Context.parchmentDataStore by preferencesDataStore("parchment_preferences")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val readingMode = stringPreferencesKey("reading_mode")
        val readerColor = stringPreferencesKey("reader_color")
        val fitMode = stringPreferencesKey("fit_mode")
        val orientation = stringPreferencesKey("orientation")
        val appTheme = stringPreferencesKey("app_theme")
        val pageSpacing = intPreferencesKey("page_spacing")
        val pageMargin = intPreferencesKey("page_margin")
        val brightness = floatPreferencesKey("reader_brightness")
        val tapNavigation = booleanPreferencesKey("tap_navigation")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val fullscreen = booleanPreferencesKey("fullscreen")
        val invertPdfColors = booleanPreferencesKey("invert_pdf_colors")
        val showPageNumber = booleanPreferencesKey("show_page_number")
        val defaultGridLayout = booleanPreferencesKey("default_grid_layout")
        val animationsEnabled = booleanPreferencesKey("animations_enabled")
        val gridSize = intPreferencesKey("grid_size")
        val publicMode = booleanPreferencesKey("public_mode")
        val storageDirectory = stringPreferencesKey("storage_directory")
        val palette = stringPreferencesKey("palette")
        val collectionCards = booleanPreferencesKey("collection_cards")
        val query = stringPreferencesKey("library_query")
        val sort = stringPreferencesKey("library_sort")
        val favorites = booleanPreferencesKey("library_favorites")
        val tags = stringSetPreferencesKey("library_tags")
        val profile = stringPreferencesKey("active_profile")
    }

    val preferences: Flow<ReaderPreferences> = context.parchmentDataStore.data.map { values ->
        ReaderPreferences(
            readingMode = values[Keys.readingMode].toEnum(ReadingMode.CONTINUOUS_VERTICAL),
            readerColor = values[Keys.readerColor].toEnum(ReaderColor.LIGHT),
            fitMode = values[Keys.fitMode].toEnum(FitMode.WIDTH),
            orientation = values[Keys.orientation].toEnum(ScreenOrientation.AUTOMATIC),
            appTheme = values[Keys.appTheme].toEnum(AppTheme.SYSTEM),
            pageSpacing = values[Keys.pageSpacing] ?: 12,
            pageMargin = values[Keys.pageMargin] ?: 10,
            brightness = values[Keys.brightness] ?: -1f,
            tapNavigation = values[Keys.tapNavigation] ?: true,
            keepScreenOn = values[Keys.keepScreenOn] ?: true,
            fullscreen = values[Keys.fullscreen] ?: false,
            invertPdfColors = values[Keys.invertPdfColors] ?: false,
            showPageNumber = values[Keys.showPageNumber] ?: true,
            defaultGridLayout = values[Keys.defaultGridLayout] ?: true,
            animationsEnabled = values[Keys.animationsEnabled] ?: true,
            gridSize = (values[Keys.gridSize] ?: 150).coerceIn(80, 400),
            publicMode = values[Keys.publicMode] ?: false,
            storageDirectory = values[Keys.storageDirectory] ?: "",
            palette = values[Keys.palette].toEnum(AppPalette.PARCHMENT),
            collectionCards = values[Keys.collectionCards] ?: true
        )
    }

    suspend fun update(value: ReaderPreferences) {
        context.parchmentDataStore.edit {
            it[Keys.readingMode] = value.readingMode.name
            it[Keys.readerColor] = value.readerColor.name
            it[Keys.fitMode] = value.fitMode.name
            it[Keys.orientation] = value.orientation.name
            it[Keys.appTheme] = value.appTheme.name
            it[Keys.pageSpacing] = value.pageSpacing
            it[Keys.pageMargin] = value.pageMargin
            it[Keys.brightness] = value.brightness
            it[Keys.tapNavigation] = value.tapNavigation
            it[Keys.keepScreenOn] = value.keepScreenOn
            it[Keys.fullscreen] = value.fullscreen
            it[Keys.invertPdfColors] = value.invertPdfColors
            it[Keys.showPageNumber] = value.showPageNumber
            it[Keys.defaultGridLayout] = value.defaultGridLayout
            it[Keys.animationsEnabled] = value.animationsEnabled
            it[Keys.gridSize] = value.gridSize.coerceIn(80, 400)
            it[Keys.publicMode] = value.publicMode
            it[Keys.storageDirectory] = value.storageDirectory
            it[Keys.palette] = value.palette.name
            it[Keys.collectionCards] = value.collectionCards
        }
    }

    val libraryPreferences = context.parchmentDataStore.data.map {
        LibraryPreferences(it[Keys.query] ?: "", it[Keys.sort] ?: "RECENTLY_ADDED", it[Keys.favorites] ?: false,
            it[Keys.tags] ?: emptySet(), it[Keys.defaultGridLayout] ?: true)
    }.distinctUntilChanged()
    val activeProfileId = context.parchmentDataStore.data.map { it[Keys.profile] ?: "default" }.distinctUntilChanged()
    suspend fun setProfile(id: String) { context.parchmentDataStore.edit { it[Keys.profile] = id } }
    suspend fun setQuery(query: String) { context.parchmentDataStore.edit { it[Keys.query] = query } }
    suspend fun setSort(sort: String) { context.parchmentDataStore.edit { it[Keys.sort] = sort } }
    suspend fun toggleFavorites() { context.parchmentDataStore.edit { it[Keys.favorites] = !(it[Keys.favorites] ?: false) } }
    suspend fun toggleGrid() { context.parchmentDataStore.edit { it[Keys.defaultGridLayout] = !(it[Keys.defaultGridLayout] ?: true) } }
    suspend fun toggleTag(tag: String) { context.parchmentDataStore.edit {
        val tags = it[Keys.tags] ?: emptySet(); it[Keys.tags] = if (tag in tags) tags - tag else tags + tag
    } }
    suspend fun clearTags() { context.parchmentDataStore.edit { it[Keys.tags] = emptySet() } }

    suspend fun exportConfig(): JSONObject = JSONObject().apply {
        context.parchmentDataStore.data.first().asMap().forEach { (key, value) -> put(key.name, if (value is Set<*>) JSONArray(value.toList()) else value) }
    }
    suspend fun restoreConfig(json: JSONObject) {
        // Only recognized preference keys are accepted from a backup.
        val parsed = mutablePreferencesOf()
        listOf(Keys.readingMode, Keys.readerColor, Keys.fitMode, Keys.orientation, Keys.appTheme, Keys.storageDirectory,
            Keys.palette, Keys.query, Keys.sort, Keys.profile).forEach { key -> if (json.has(key.name)) parsed[key] = json.getString(key.name) }
        listOf(Keys.tapNavigation, Keys.keepScreenOn, Keys.fullscreen, Keys.invertPdfColors, Keys.showPageNumber,
            Keys.defaultGridLayout, Keys.animationsEnabled, Keys.publicMode, Keys.collectionCards, Keys.favorites).forEach { key -> if (json.has(key.name)) parsed[key] = json.getBoolean(key.name) }
        listOf(Keys.pageSpacing, Keys.pageMargin, Keys.gridSize).forEach { key -> if (json.has(key.name)) parsed[key] = json.getInt(key.name).coerceIn(0, 400) }
        if (json.has(Keys.brightness.name)) parsed[Keys.brightness] = json.getDouble(Keys.brightness.name).toFloat().coerceIn(-1f, 1f)
        json.optJSONArray(Keys.tags.name)?.let { tags -> parsed[Keys.tags] = (0 until tags.length()).map { tags.getString(it) }.toSet() }
        context.parchmentDataStore.updateData { parsed }
    }

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
