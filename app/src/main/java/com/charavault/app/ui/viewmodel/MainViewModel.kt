package com.charavault.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charavault.app.data.local.CardEntity
import com.charavault.app.data.repository.BatchImportResult
import com.charavault.app.data.repository.CardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class CardGroup(
    val title: String,
    val cards: List<CardEntity>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardRepository(application)
    private val json = Json { ignoreUnknownKeys = true }

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

    // Pure Category Tag Grouped Cards (Clean title without 🏷️ emoji)
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
            resultGroups.add(CardGroup(title = tag, cards = list))
        }

        resultGroups
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun reorderCards(cardIdsInOrder: List<String>) {
        viewModelScope.launch {
            repository.updateCardsOrder(cardIdsInOrder)
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

    fun deleteCard(card: CardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }
}
