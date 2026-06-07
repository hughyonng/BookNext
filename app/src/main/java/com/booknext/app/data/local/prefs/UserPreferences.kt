package com.booknext.app.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "booknext_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val API_KEY    = stringPreferencesKey("api_key")
        private val DARK_MODE  = booleanPreferencesKey("dark_mode")
        private val FONT_SIZE  = intPreferencesKey("font_size")
        private val FONT_FAMILY = stringPreferencesKey("font_family")
        private val LINE_SPACING = floatPreferencesKey("line_spacing")
        private val CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        private val THEME_ID = stringPreferencesKey("theme_id")
        private val EMPTY_FOLDERS = stringSetPreferencesKey("empty_folders")
        private val HAS_SEEN_WELCOME_KEY = booleanPreferencesKey("has_seen_welcome")
        // ── 可视选项 ─────────────────────────────────────
        private val TEXT_COLOR = stringPreferencesKey("text_color")
        private val BG_COLOR = stringPreferencesKey("bg_color")
        private val BG_IMAGE_PATH = stringPreferencesKey("bg_image_path")
        private val BG_IMAGE_ALPHA = floatPreferencesKey("bg_image_alpha")
        private val FONT_BOLD = booleanPreferencesKey("font_bold")
        private val FONT_ITALIC = booleanPreferencesKey("font_italic")
        private val FONT_UNDERLINE = booleanPreferencesKey("font_underline")
        private val FONT_SHADOW = booleanPreferencesKey("font_shadow")
        private val TEXT_JUSTIFY = booleanPreferencesKey("text_justify")
        private val CHINESE_LAYOUT = booleanPreferencesKey("chinese_layout")
        private val VERTICAL_MODE = booleanPreferencesKey("vertical_mode")
        private val SIMPLIFIED_TRAD = stringPreferencesKey("simplified_trad")
        private val PARA_SPACING = floatPreferencesKey("para_spacing")
        private val CHAR_SPACING = floatPreferencesKey("char_spacing")
        private val FONT_SCALE = floatPreferencesKey("font_scale")
        private val PADDING_H = intPreferencesKey("padding_h")
        private val PADDING_TOP = intPreferencesKey("padding_top")
        private val PADDING_BOTTOM = intPreferencesKey("padding_bottom")
        private val PAGE_ANIMATION = stringPreferencesKey("page_animation")
        // ── 控制选项 ─────────────────────────────────────
        private val TAP_LEFT_ACTION = stringPreferencesKey("tap_left_action")
        private val TAP_RIGHT_ACTION = stringPreferencesKey("tap_right_action")
        private val TAP_CENTER_ACTION = stringPreferencesKey("tap_center_action")
        private val LONG_PRESS_ACTION = stringPreferencesKey("long_press_action")
        private val VOLUME_UP_ACTION = stringPreferencesKey("volume_up_action")
        private val VOLUME_DOWN_ACTION = stringPreferencesKey("volume_down_action")
        private val TAP_ZONE = stringPreferencesKey("tap_zone")
        private val NINE_ZONE_CONFIG = stringPreferencesKey("nine_zone_config")
        private val SWIPE_LEFT_ACTION = stringPreferencesKey("swipe_left_action")
        private val SWIPE_RIGHT_ACTION = stringPreferencesKey("swipe_right_action")
        private val SWIPE_UP_ACTION = stringPreferencesKey("swipe_up_action")
        private val SWIPE_DOWN_ACTION = stringPreferencesKey("swipe_down_action")
        // ── 其他选项 ─────────────────────────────────────
        private val SHOW_STATUS_BAR = booleanPreferencesKey("show_status_bar")
        private val SHOW_NAV_BAR = booleanPreferencesKey("show_nav_bar")
        private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val EDGE_SWIPE_BRIGHTNESS = booleanPreferencesKey("edge_swipe_brightness")
        private val EDGE_SWIPE_FONT_SIZE = booleanPreferencesKey("edge_swipe_font_size")
        private val KEEP_LAST_LINE = booleanPreferencesKey("keep_last_line")
        private val SMART_INDENT = booleanPreferencesKey("smart_indent")
        private val REMOVE_EXTRA_BLANK = booleanPreferencesKey("remove_extra_blank")
        private val BLUE_LIGHT = booleanPreferencesKey("blue_light")
        private val BLUE_LIGHT_AMOUNT = floatPreferencesKey("blue_light_amount")
        private val READING_REMINDER = booleanPreferencesKey("reading_reminder")
        private val READING_REMINDER_MINS = intPreferencesKey("reading_reminder_mins")
        private val TTS_SPLIT_MODE = stringPreferencesKey("tts_split_mode")
        private val ALLOW_TILT_FLIP = booleanPreferencesKey("allow_tilt_flip")
        private val ALLOW_PINCH_FONT = booleanPreferencesKey("allow_pinch_font")
        private val ALLOW_SWIPE_FLIP = booleanPreferencesKey("allow_swipe_flip")
        private val EPUB_USE_BOOK_FONT = booleanPreferencesKey("epub_use_book_font")
        private val EPUB_DISABLE_CSS = booleanPreferencesKey("epub_disable_css")
        private val EPUB_SHOW_ANNOTATIONS = booleanPreferencesKey("epub_show_annotations")
        private val TRANSLATE_ENGINE = stringPreferencesKey("translate_engine")
        private val TRANSLATE_TARGET_LANG = stringPreferencesKey("translate_target_lang")
        private val SCREEN_ORIENTATION = stringPreferencesKey("screen_orientation")
        private val BRIGHTNESS = floatPreferencesKey("brightness")
        private val NAME_REPLACEMENTS = stringPreferencesKey("name_replacements")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { it[SERVER_URL] ?: "" }
    val apiKey: Flow<String>    = context.dataStore.data.map { it[API_KEY] ?: "" }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val fontSize: Flow<Int>     = context.dataStore.data.map { it[FONT_SIZE] ?: 17 }
    val fontFamily: Flow<String> = context.dataStore.data.map { it[FONT_FAMILY] ?: "serif" }
    val lineSpacing: Flow<Float> = context.dataStore.data.map { it[LINE_SPACING] ?: 1.8f }
    val customFontPath: Flow<String> = context.dataStore.data.map { it[CUSTOM_FONT_PATH] ?: "" }
    val themeId: Flow<String> = context.dataStore.data.map { it[THEME_ID] ?: "blue" }
    val screenOrientation: Flow<String> = context.dataStore.data.map { it[SCREEN_ORIENTATION] ?: "auto" }
    val brightness: Flow<Float> = context.dataStore.data.map { it[BRIGHTNESS] ?: -1f }
    val nameReplacements: Flow<String> = context.dataStore.data.map { it[NAME_REPLACEMENTS] ?: "" }

    // ── 可视选项 ─────────────────────────────────────────
    val textColor: Flow<String> = context.dataStore.data.map { it[TEXT_COLOR] ?: "" }
    val bgColor: Flow<String> = context.dataStore.data.map { it[BG_COLOR] ?: "" }
    val bgImagePath: Flow<String> = context.dataStore.data.map { it[BG_IMAGE_PATH] ?: "" }
    val bgImageAlpha: Flow<Float> = context.dataStore.data.map { it[BG_IMAGE_ALPHA] ?: 1.0f }
    val fontBold: Flow<Boolean> = context.dataStore.data.map { it[FONT_BOLD] ?: false }
    val fontItalic: Flow<Boolean> = context.dataStore.data.map { it[FONT_ITALIC] ?: false }
    val fontUnderline: Flow<Boolean> = context.dataStore.data.map { it[FONT_UNDERLINE] ?: false }
    val fontShadow: Flow<Boolean> = context.dataStore.data.map { it[FONT_SHADOW] ?: false }
    val textJustify: Flow<Boolean> = context.dataStore.data.map { it[TEXT_JUSTIFY] ?: true }
    val chineseLayout: Flow<Boolean> = context.dataStore.data.map { it[CHINESE_LAYOUT] ?: true }
    val verticalMode: Flow<Boolean> = context.dataStore.data.map { it[VERTICAL_MODE] ?: false }
    val simplifiedTrad: Flow<String> = context.dataStore.data.map { it[SIMPLIFIED_TRAD] ?: "none" }
    val paraSpacing: Flow<Float> = context.dataStore.data.map { it[PARA_SPACING] ?: 0.7f }
    val charSpacing: Flow<Float> = context.dataStore.data.map { it[CHAR_SPACING] ?: 0f }
    val fontScale: Flow<Float> = context.dataStore.data.map { it[FONT_SCALE] ?: 1.0f }
    val paddingH: Flow<Int> = context.dataStore.data.map { it[PADDING_H] ?: 20 }
    val paddingTop: Flow<Int> = context.dataStore.data.map { it[PADDING_TOP] ?: 16 }
    val paddingBottom: Flow<Int> = context.dataStore.data.map { it[PADDING_BOTTOM] ?: 16 }
    val pageAnimation: Flow<String> = context.dataStore.data.map { it[PAGE_ANIMATION] ?: "none" }

    // ── 控制选项 ─────────────────────────────────────────
    val tapLeftAction: Flow<String> = context.dataStore.data.map { it[TAP_LEFT_ACTION] ?: "prev_page" }
    val tapRightAction: Flow<String> = context.dataStore.data.map { it[TAP_RIGHT_ACTION] ?: "next_page" }
    val tapCenterAction: Flow<String> = context.dataStore.data.map { it[TAP_CENTER_ACTION] ?: "toggle_ui" }
    val longPressAction: Flow<String> = context.dataStore.data.map { it[LONG_PRESS_ACTION] ?: "select_text" }
    val volumeUpAction: Flow<String> = context.dataStore.data.map { it[VOLUME_UP_ACTION] ?: "prev_page" }
    val volumeDownAction: Flow<String> = context.dataStore.data.map { it[VOLUME_DOWN_ACTION] ?: "next_page" }
    val tapZone: Flow<String> = context.dataStore.data.map { it[TAP_ZONE] ?: "three_zone" }
    val nineZoneConfig: Flow<String> = context.dataStore.data.map { it[NINE_ZONE_CONFIG] ?: "" }
    val swipeLeftAction: Flow<String> = context.dataStore.data.map { it[SWIPE_LEFT_ACTION] ?: "none" }
    val swipeRightAction: Flow<String> = context.dataStore.data.map { it[SWIPE_RIGHT_ACTION] ?: "none" }
    val swipeUpAction: Flow<String> = context.dataStore.data.map { it[SWIPE_UP_ACTION] ?: "none" }
    val swipeDownAction: Flow<String> = context.dataStore.data.map { it[SWIPE_DOWN_ACTION] ?: "none" }

    // ── 其他选项 ─────────────────────────────────────────
    val showStatusBar: Flow<Boolean> = context.dataStore.data.map { it[SHOW_STATUS_BAR] ?: false }
    val showNavBar: Flow<Boolean> = context.dataStore.data.map { it[SHOW_NAV_BAR] ?: false }
    val keepScreenOn: Flow<Boolean> = context.dataStore.data.map { it[KEEP_SCREEN_ON] ?: true }
    val edgeSwipeBrightness: Flow<Boolean> = context.dataStore.data.map { it[EDGE_SWIPE_BRIGHTNESS] ?: true }
    val edgeSwipeFontSize: Flow<Boolean> = context.dataStore.data.map { it[EDGE_SWIPE_FONT_SIZE] ?: false }
    val keepLastLine: Flow<Boolean> = context.dataStore.data.map { it[KEEP_LAST_LINE] ?: false }
    val smartIndent: Flow<Boolean> = context.dataStore.data.map { it[SMART_INDENT] ?: true }
    val removeExtraBlank: Flow<Boolean> = context.dataStore.data.map { it[REMOVE_EXTRA_BLANK] ?: false }
    val blueLight: Flow<Boolean> = context.dataStore.data.map { it[BLUE_LIGHT] ?: false }
    val blueLightAmount: Flow<Float> = context.dataStore.data.map { it[BLUE_LIGHT_AMOUNT] ?: 0.3f }
    val readingReminder: Flow<Boolean> = context.dataStore.data.map { it[READING_REMINDER] ?: false }
    val readingReminderMins: Flow<Int> = context.dataStore.data.map { it[READING_REMINDER_MINS] ?: 30 }
    val ttsSplitMode: Flow<String> = context.dataStore.data.map { it[TTS_SPLIT_MODE] ?: "sentence" }
    val allowTiltFlip: Flow<Boolean> = context.dataStore.data.map { it[ALLOW_TILT_FLIP] ?: false }
    val allowPinchFont: Flow<Boolean> = context.dataStore.data.map { it[ALLOW_PINCH_FONT] ?: true }
    val allowSwipeFlip: Flow<Boolean> = context.dataStore.data.map { it[ALLOW_SWIPE_FLIP] ?: true }
    val epubUseBookFont: Flow<Boolean> = context.dataStore.data.map { it[EPUB_USE_BOOK_FONT] ?: true }
    val epubDisableCss: Flow<Boolean> = context.dataStore.data.map { it[EPUB_DISABLE_CSS] ?: false }
    val epubShowAnnotations: Flow<Boolean> = context.dataStore.data.map { it[EPUB_SHOW_ANNOTATIONS] ?: false }
    val translateEngine: Flow<String> = context.dataStore.data.map { it[TRANSLATE_ENGINE] ?: "google" }
    val translateTargetLang: Flow<String> = context.dataStore.data.map { it[TRANSLATE_TARGET_LANG] ?: "zh-CN" }

    suspend fun saveCredentials(url: String, key: String) {
        context.dataStore.edit {
            it[SERVER_URL] = url.trimEnd('/')
            it[API_KEY]    = key
        }
    }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun saveFontSize(size: Int) {
        context.dataStore.edit { it[FONT_SIZE] = size }
    }

    suspend fun saveFontFamily(family: String) {
        context.dataStore.edit { it[FONT_FAMILY] = family }
    }

    suspend fun saveLineSpacing(spacing: Float) {
        context.dataStore.edit { it[LINE_SPACING] = spacing }
    }

    suspend fun saveCustomFontPath(path: String) {
        context.dataStore.edit { it[CUSTOM_FONT_PATH] = path }
    }

    suspend fun saveThemeId(id: String) {
        context.dataStore.edit { it[THEME_ID] = id }
    }

    suspend fun saveScreenOrientation(orientation: String) {
        context.dataStore.edit { it[SCREEN_ORIENTATION] = orientation }
    }
    suspend fun saveBrightness(value: Float) {
        context.dataStore.edit { it[BRIGHTNESS] = value }
    }

    suspend fun saveNameReplacements(json: String) {
        context.dataStore.edit { it[NAME_REPLACEMENTS] = json }
    }

    // ── 可视选项 ─────────────────────────────────────────
    suspend fun saveTextColor(v: String) = context.dataStore.edit { it[TEXT_COLOR] = v }
    suspend fun saveBgColor(v: String) = context.dataStore.edit { it[BG_COLOR] = v }
    suspend fun saveBgImagePath(v: String) = context.dataStore.edit { it[BG_IMAGE_PATH] = v }
    suspend fun saveBgImageAlpha(v: Float) = context.dataStore.edit { it[BG_IMAGE_ALPHA] = v }
    suspend fun saveFontBold(v: Boolean) = context.dataStore.edit { it[FONT_BOLD] = v }
    suspend fun saveFontItalic(v: Boolean) = context.dataStore.edit { it[FONT_ITALIC] = v }
    suspend fun saveFontUnderline(v: Boolean) = context.dataStore.edit { it[FONT_UNDERLINE] = v }
    suspend fun saveFontShadow(v: Boolean) = context.dataStore.edit { it[FONT_SHADOW] = v }
    suspend fun saveTextJustify(v: Boolean) = context.dataStore.edit { it[TEXT_JUSTIFY] = v }
    suspend fun saveChineseLayout(v: Boolean) = context.dataStore.edit { it[CHINESE_LAYOUT] = v }
    suspend fun saveVerticalMode(v: Boolean) = context.dataStore.edit { it[VERTICAL_MODE] = v }
    suspend fun saveSimplifiedTrad(v: String) = context.dataStore.edit { it[SIMPLIFIED_TRAD] = v }
    suspend fun saveParaSpacing(v: Float) = context.dataStore.edit { it[PARA_SPACING] = v }
    suspend fun saveCharSpacing(v: Float) = context.dataStore.edit { it[CHAR_SPACING] = v }
    suspend fun saveFontScale(v: Float) = context.dataStore.edit { it[FONT_SCALE] = v }
    suspend fun savePaddingH(v: Int) = context.dataStore.edit { it[PADDING_H] = v }
    suspend fun savePaddingTop(v: Int) = context.dataStore.edit { it[PADDING_TOP] = v }
    suspend fun savePaddingBottom(v: Int) = context.dataStore.edit { it[PADDING_BOTTOM] = v }
    suspend fun savePageAnimation(v: String) = context.dataStore.edit { it[PAGE_ANIMATION] = v }

    // ── 控制选项 ─────────────────────────────────────────
    suspend fun saveTapLeftAction(v: String) = context.dataStore.edit { it[TAP_LEFT_ACTION] = v }
    suspend fun saveTapRightAction(v: String) = context.dataStore.edit { it[TAP_RIGHT_ACTION] = v }
    suspend fun saveTapCenterAction(v: String) = context.dataStore.edit { it[TAP_CENTER_ACTION] = v }
    suspend fun saveLongPressAction(v: String) = context.dataStore.edit { it[LONG_PRESS_ACTION] = v }
    suspend fun saveVolumeUpAction(v: String) = context.dataStore.edit { it[VOLUME_UP_ACTION] = v }
    suspend fun saveVolumeDownAction(v: String) = context.dataStore.edit { it[VOLUME_DOWN_ACTION] = v }
    suspend fun saveTapZone(v: String) = context.dataStore.edit { it[TAP_ZONE] = v }
    suspend fun saveNineZoneConfig(v: String) = context.dataStore.edit { it[NINE_ZONE_CONFIG] = v }
    suspend fun saveSwipeLeftAction(v: String) = context.dataStore.edit { it[SWIPE_LEFT_ACTION] = v }
    suspend fun saveSwipeRightAction(v: String) = context.dataStore.edit { it[SWIPE_RIGHT_ACTION] = v }
    suspend fun saveSwipeUpAction(v: String) = context.dataStore.edit { it[SWIPE_UP_ACTION] = v }
    suspend fun saveSwipeDownAction(v: String) = context.dataStore.edit { it[SWIPE_DOWN_ACTION] = v }

    // ── 其他选项 ─────────────────────────────────────────
    suspend fun saveShowStatusBar(v: Boolean) = context.dataStore.edit { it[SHOW_STATUS_BAR] = v }
    suspend fun saveShowNavBar(v: Boolean) = context.dataStore.edit { it[SHOW_NAV_BAR] = v }
    suspend fun saveKeepScreenOn(v: Boolean) = context.dataStore.edit { it[KEEP_SCREEN_ON] = v }
    suspend fun saveEdgeSwipeBrightness(v: Boolean) = context.dataStore.edit { it[EDGE_SWIPE_BRIGHTNESS] = v }
    suspend fun saveEdgeSwipeFontSize(v: Boolean) = context.dataStore.edit { it[EDGE_SWIPE_FONT_SIZE] = v }
    suspend fun saveKeepLastLine(v: Boolean) = context.dataStore.edit { it[KEEP_LAST_LINE] = v }
    suspend fun saveSmartIndent(v: Boolean) = context.dataStore.edit { it[SMART_INDENT] = v }
    suspend fun saveRemoveExtraBlank(v: Boolean) = context.dataStore.edit { it[REMOVE_EXTRA_BLANK] = v }
    suspend fun saveBlueLight(v: Boolean) = context.dataStore.edit { it[BLUE_LIGHT] = v }
    suspend fun saveBlueLightAmount(v: Float) = context.dataStore.edit { it[BLUE_LIGHT_AMOUNT] = v }
    suspend fun saveReadingReminder(v: Boolean) = context.dataStore.edit { it[READING_REMINDER] = v }
    suspend fun saveReadingReminderMins(v: Int) = context.dataStore.edit { it[READING_REMINDER_MINS] = v }
    suspend fun saveTtsSplitMode(v: String) = context.dataStore.edit { it[TTS_SPLIT_MODE] = v }
    suspend fun saveAllowTiltFlip(v: Boolean) = context.dataStore.edit { it[ALLOW_TILT_FLIP] = v }
    suspend fun saveAllowPinchFont(v: Boolean) = context.dataStore.edit { it[ALLOW_PINCH_FONT] = v }
    suspend fun saveAllowSwipeFlip(v: Boolean) = context.dataStore.edit { it[ALLOW_SWIPE_FLIP] = v }
    suspend fun saveEpubUseBookFont(v: Boolean) = context.dataStore.edit { it[EPUB_USE_BOOK_FONT] = v }
    suspend fun saveEpubDisableCss(v: Boolean) = context.dataStore.edit { it[EPUB_DISABLE_CSS] = v }
    suspend fun saveEpubShowAnnotations(v: Boolean) = context.dataStore.edit { it[EPUB_SHOW_ANNOTATIONS] = v }

    val hasSeenWelcome: Flow<Boolean> = context.dataStore.data.map {
        it[HAS_SEEN_WELCOME_KEY] ?: false
    }

    suspend fun setHasSeenWelcome(value: Boolean) {
        context.dataStore.edit { it[HAS_SEEN_WELCOME_KEY] = value }
    }

    fun observeBookmarks(bookId: String): Flow<List<Int>> = context.dataStore.data.map { prefs ->
        prefs[stringPreferencesKey("bookmarks_$bookId")]?.split(",")
            ?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    }

    suspend fun saveBookmarks(bookId: String, pages: List<Int>) {
        context.dataStore.edit {
            if (pages.isEmpty()) it.remove(stringPreferencesKey("bookmarks_$bookId"))
            else it[stringPreferencesKey("bookmarks_$bookId")] = pages.joinToString(",")
        }
    }

    val emptyFolders: Flow<Set<String>> = context.dataStore.data.map {
        it[EMPTY_FOLDERS] ?: emptySet()
    }

    suspend fun addEmptyFolder(name: String) {
        context.dataStore.edit { prefs ->
            val current: Set<String> = prefs[EMPTY_FOLDERS] ?: emptySet()
            prefs[EMPTY_FOLDERS] = current + name
        }
    }

    suspend fun removeEmptyFolder(name: String) {
        context.dataStore.edit { prefs ->
            val current: Set<String> = prefs[EMPTY_FOLDERS] ?: emptySet()
            prefs[EMPTY_FOLDERS] = current - name
        }
    }

    suspend fun batch(block: suspend MutablePreferences.() -> Unit) {
        context.dataStore.edit { it.block() }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    // ── 批量写入（单次 edit 事务）─────────────────────
    suspend fun saveVisualOptions(
        fontSize: Int, lineSpacing: Float, fontFamily: String,
    ) {
        context.dataStore.edit {
            it[FONT_SIZE] = fontSize; it[LINE_SPACING] = lineSpacing
            it[FONT_FAMILY] = fontFamily
        }
    }

    suspend fun saveControlOptions(
        tapLeftAction: String, tapRightAction: String, tapCenterAction: String,
        longPressAction: String, volumeUpAction: String, volumeDownAction: String,
        tapZone: String, nineZoneConfig: String, swipeLeftAction: String,
        swipeRightAction: String, swipeUpAction: String, swipeDownAction: String,
    ) {
        context.dataStore.edit {
            it[TAP_LEFT_ACTION] = tapLeftAction; it[TAP_RIGHT_ACTION] = tapRightAction
            it[TAP_CENTER_ACTION] = tapCenterAction; it[LONG_PRESS_ACTION] = longPressAction
            it[VOLUME_UP_ACTION] = volumeUpAction; it[VOLUME_DOWN_ACTION] = volumeDownAction
            it[TAP_ZONE] = tapZone; it[NINE_ZONE_CONFIG] = nineZoneConfig
            it[SWIPE_LEFT_ACTION] = swipeLeftAction; it[SWIPE_RIGHT_ACTION] = swipeRightAction
            it[SWIPE_UP_ACTION] = swipeUpAction; it[SWIPE_DOWN_ACTION] = swipeDownAction
        }
    }

    suspend fun saveOtherOptions(
        showStatusBar: Boolean, showNavBar: Boolean, keepScreenOn: Boolean, edgeSwipeBrightness: Boolean,
        edgeSwipeFontSize: Boolean, keepLastLine: Boolean, smartIndent: Boolean,
        removeExtraBlank: Boolean, blueLight: Boolean, blueLightAmount: Float,
        readingReminder: Boolean, readingReminderMins: Int, ttsSplitMode: String,
        allowTiltFlip: Boolean, allowPinchFont: Boolean, allowSwipeFlip: Boolean,
        epubUseBookFont: Boolean, epubDisableCss: Boolean, epubShowAnnotations: Boolean,
    ) {
        context.dataStore.edit {
            it[SHOW_STATUS_BAR] = showStatusBar; it[SHOW_NAV_BAR] = showNavBar; it[KEEP_SCREEN_ON] = keepScreenOn
            it[EDGE_SWIPE_BRIGHTNESS] = edgeSwipeBrightness; it[EDGE_SWIPE_FONT_SIZE] = edgeSwipeFontSize
            it[KEEP_LAST_LINE] = keepLastLine; it[SMART_INDENT] = smartIndent
            it[REMOVE_EXTRA_BLANK] = removeExtraBlank; it[BLUE_LIGHT] = blueLight
            it[BLUE_LIGHT_AMOUNT] = blueLightAmount; it[READING_REMINDER] = readingReminder
            it[READING_REMINDER_MINS] = readingReminderMins; it[TTS_SPLIT_MODE] = ttsSplitMode
            it[ALLOW_TILT_FLIP] = allowTiltFlip; it[ALLOW_PINCH_FONT] = allowPinchFont
            it[ALLOW_SWIPE_FLIP] = allowSwipeFlip; it[EPUB_USE_BOOK_FONT] = epubUseBookFont
            it[EPUB_DISABLE_CSS] = epubDisableCss; it[EPUB_SHOW_ANNOTATIONS] = epubShowAnnotations
        }
    }

    suspend fun saveTranslateEngine(v: String) = context.dataStore.edit { it[TRANSLATE_ENGINE] = v }
    suspend fun saveTranslateTargetLang(v: String) = context.dataStore.edit { it[TRANSLATE_TARGET_LANG] = v }
}
