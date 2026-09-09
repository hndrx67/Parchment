package org.hndrx.parchment.settings

enum class ReadingMode { CONTINUOUS_VERTICAL, CONTINUOUS_HORIZONTAL, PAGED_VERTICAL, PAGED_HORIZONTAL }
enum class ReaderColor { LIGHT, DARK, SEPIA }
enum class FitMode { WIDTH, PAGE }
enum class ScreenOrientation { AUTOMATIC, PORTRAIT, LANDSCAPE }
enum class AppTheme { SYSTEM, LIGHT, DARK }
enum class AppPalette(val label: String) { PARCHMENT("Parchment"), OLED_BLACK("OLED Black"), MINT_GREEN("Mint Green"), HAZE_PURPLE("Haze Purple"), OCEAN_BLUE("Ocean Blue"), ROSE("Rose") }

data class ReaderPreferences(
    val readingMode: ReadingMode = ReadingMode.CONTINUOUS_VERTICAL,
    val readerColor: ReaderColor = ReaderColor.LIGHT,
    val fitMode: FitMode = FitMode.WIDTH,
    val orientation: ScreenOrientation = ScreenOrientation.AUTOMATIC,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val pageSpacing: Int = 12,
    val pageMargin: Int = 10,
    val brightness: Float = -1f,
    val tapNavigation: Boolean = true,
    val keepScreenOn: Boolean = true,
    val fullscreen: Boolean = false,
    val invertPdfColors: Boolean = false,
    val showPageNumber: Boolean = true,
    val defaultGridLayout: Boolean = true,
    val animationsEnabled: Boolean = true,
    val gridSize: Int = 150,
    val publicMode: Boolean = false,
    val storageDirectory: String = "",
    val palette: AppPalette = AppPalette.PARCHMENT,
    val collectionCards: Boolean = true
)

data class LibraryPreferences(val query: String = "", val sort: String = "RECENTLY_ADDED", val favoritesOnly: Boolean = false,
    val tags: Set<String> = emptySet(), val grid: Boolean = true)
