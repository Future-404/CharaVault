package com.charavault.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charavault.app.data.local.CardEntity
import com.charavault.app.data.repository.CardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class CardGroup(
    val title: String,
    val isFavoriteGroup: Boolean = false,
    val cards: List<CardEntity>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardRepository(application)
    private val json = Json { ignoreUnknownKeys = true }

    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(null)

    val allCards: StateFlow<List<CardEntity>> = repository.getAllCards()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Grouped Cards: Favorite group + Category tag groups
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

        // 1. Favorites Group (if any)
        val favorites = filtered.filter { it.isFavorite }
        if (favorites.isNotEmpty()) {
            resultGroups.add(CardGroup(title = "⭐ 常用收藏", isFavoriteGroup = true, cards = favorites))
        }

        // 2. Tag Groups
        val tagMap = mutableMapOf<String, MutableList<CardEntity>>()
        filtered.forEach { card ->
            val tags = try { json.decodeFromString<List<String>>(card.tagsJson) } catch (e: Exception) { listOf("未分类") }
            val effectiveTags = if (tags.isEmpty()) listOf("未分类") else tags
            effectiveTags.forEach { tag ->
                tagMap.getOrPut(tag) { mutableListOf() }.add(card)
            }
        }

        tagMap.forEach { (tag, list) ->
            resultGroups.add(CardGroup(title = "🏷️ $tag", isFavoriteGroup = false, cards = list))
        }

        resultGroups
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importCardUri(uri: Uri, selectedTags: List<String> = emptyList()) {
        viewModelScope.launch {
            val contentResolver = getApplication<Application>().contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@launch
            val fileName = uri.lastPathSegment
            repository.importCardStream(inputStream, fileName, selectedTags)
        }
    }

    fun updateCardTags(cardId: String, newTags: List<String>) {
        viewModelScope.launch {
            repository.updateCardTags(cardId, newTags)
        }
    }

    fun toggleFavorite(card: CardEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(card.id, !card.isFavorite)
        }
    }

    fun deleteCard(card: CardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }
}
