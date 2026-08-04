package com.charavault.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charavault.app.data.local.CardEntity
import com.charavault.app.ui.viewmodel.CardGroup

@Composable
fun CategoryRow(
    group: CardGroup,
    onCardClick: (CardEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        // Group Header Title & Counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (group.isFavoriteGroup) MaterialTheme.colorScheme.primary else Color.White
            )
            Text(
                text = "${group.cards.size} 张",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        // Horizontal Carousel LazyRow
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            items(group.cards, key = { it.id }) { card ->
                CardItem(
                    card = card,
                    onClick = { onCardClick(card) }
                )
            }
        }
    }
}
