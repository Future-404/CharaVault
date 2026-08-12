package com.charavault.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY sortOrder ASC, updatedAt DESC")
    fun getAllCardsFlow(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: String): CardEntity?

    @Query("SELECT * FROM cards WHERE fileHash = :fileHash LIMIT 1")
    suspend fun getCardByFileHash(fileHash: String): CardEntity?

    @Query("SELECT * FROM cards WHERE normalizedJsonHash = :normalizedJsonHash LIMIT 1")
    suspend fun getCardByNormalizedJsonHash(normalizedJsonHash: String): CardEntity?

    @Query("SELECT * FROM cards WHERE semanticHash = :semanticHash LIMIT 1")
    suspend fun getCardBySemanticHash(semanticHash: String): CardEntity?

    @Query("SELECT * FROM cards WHERE normalizedJsonHash = ''")
    suspend fun getCardsMissingNormalizedJsonHash(): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Update
    suspend fun updateCards(cards: List<CardEntity>)

    @Query("UPDATE cards SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()
}
