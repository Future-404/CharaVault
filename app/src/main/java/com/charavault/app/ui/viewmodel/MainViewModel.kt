package com.charavault.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charavault.app.data.local.CardEntity
import com.charavault.app.data.model.CharacterCardV3
import com.charavault.app.data.release.AvailableUpdate
import com.charavault.app.data.release.UpdateChecker
import com.charavault.app.data.repository.BatchImportResult
import com.charavault.app.data.repository.CardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.charavault.app.ui.theme.ThemeMode

data class CardGroup(
    val title: String,
    val cards: List<CardEntity>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardRepository(application)
    private val updateChecker = UpdateChecker(application)
    private val json = Json { ignoreUnknownKeys = true }

    private val prefs = application.getSharedPreferences("charavault_settings", Context.MODE_PRIVATE)

    private val _accentColorHex = MutableStateFlow(
        prefs.getString("accent_color_hex", "#8B5CF6") ?: "#8B5CF6"
    )
    val accentColorHex: StateFlow<String> = _accentColorHex.asStateFlow()

    fun updateAccentColor(hex: String) {
        _accentColorHex.value = hex
        prefs.edit().putString("accent_color_hex", hex).apply()
    }

    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun updateThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    private val _availableUpdate = MutableStateFlow<AvailableUpdate?>(null)
    val availableUpdate: StateFlow<AvailableUpdate?> = _availableUpdate.asStateFlow()

    private var hasCheckedForUpdates = false

    fun checkForUpdates() {
        if (hasCheckedForUpdates) return
        hasCheckedForUpdates = true

        viewModelScope.launch {
            runCatching { updateChecker.check() }
                .onSuccess { update ->
                    val ignoredVersionCode = prefs.getLong("ignored_update_version_code", -1L)
                    _availableUpdate.value = update?.takeIf { it.versionCode != ignoredVersionCode }
                }
        }
    }

    fun dismissUpdateNotice() {
        _availableUpdate.value = null
    }

    fun ignoreAvailableUpdate() {
        _availableUpdate.value?.let { update ->
            prefs.edit().putLong("ignored_update_version_code", update.versionCode).apply()
        }
        _availableUpdate.value = null
    }

    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(null)

    val allCards: StateFlow<List<CardEntity>> = repository.getAllCards()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Dynamically extract all existing tags from current database cards (excluding '未分类')
    val existingTags: StateFlow<List<String>> = allCards.map { cards ->
        cards.flatMap { card ->
            try { json.decodeFromString<List<String>>(card.tagsJson) } catch (e: Exception) { emptyList() }
        }.filter { it.isNotBlank() && it != "未分类" }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pure Category Tag Grouped Cards
    val groupedCards: StateFlow<List<CardGroup>> = combine(allCards, searchQuery, selectedTagFilter) { cards, query, tagFilter ->
        val filtered = cards.filter { card ->
            val matchesQuery = query.isBlank() || 
                card.name.contains(query, ignoreCase = true) ||
                card.creator.contains(query, ignoreCase = true) ||
                card.description.contains(query, ignoreCase = true)
            
            val tags = try { json.decodeFromString<List<String>>(card.tagsJson) } catch (e: Exception) { emptyList() }
            val matchesTag = tagFilter == null || tags.contains(tagFilter)

            matchesQuery && matchesTag
        }

        val resultGroups = mutableListOf<CardGroup>()

        // Clean Tag Groups Only
        val tagMap = mutableMapOf<String, MutableList<CardEntity>>()
        filtered.forEach { card ->
            val tags = try { json.decodeFromString<List<String>>(card.tagsJson) } catch (e: Exception) { listOf("未分类") }
            val effectiveTags = if (tags.isEmpty()) listOf("未分类") else tags
            effectiveTags.forEach { tag ->
                tagMap.getOrPut(tag) { mutableListOf() }.add(card)
            }
        }

        tagMap.forEach { (tag, list) ->
            list.sortBy { card ->
                try {
                    json.decodeFromString<Map<String, Int>>(card.categorySortOrdersJson)[tag] ?: Int.MAX_VALUE
                } catch (_: Exception) {
                    Int.MAX_VALUE
                }
            }
            resultGroups.add(CardGroup(title = tag, cards = list))
        }

        resultGroups
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun reorderCards(category: String, cardIdsInOrder: List<String>) {
        viewModelScope.launch {
            repository.updateCardsOrder(category, cardIdsInOrder)
        }
    }

    fun updateFullCardData(id: String, updatedV3: CharacterCardV3) {
        viewModelScope.launch {
            repository.updateFullCardData(id, updatedV3)
        }
    }

    fun importCardUrisBatch(uris: List<Uri>, selectedTags: List<String>, onResult: (BatchImportResult) -> Unit) {
        viewModelScope.launch {
            val contentResolver = getApplication<Application>().contentResolver
            val items = uris.mapNotNull { uri ->
                val stream = contentResolver.openInputStream(uri) ?: return@mapNotNull null
                val fileName = uri.lastPathSegment ?: "character.png"
                Pair(stream, fileName)
            }
            val result = repository.importCardStreamsBatch(items, selectedTags)
            onResult(result)
        }
    }

    fun updateCardTags(cardId: String, newTags: List<String>) {
        viewModelScope.launch {
            repository.updateCardTags(cardId, newTags)
        }
    }

    fun updateCardAvatar(cardId: String, imageUri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.updateCardAvatar(getApplication(), cardId, imageUri)
            onResult(success)
        }
    }

    fun deleteCard(card: CardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }
}
