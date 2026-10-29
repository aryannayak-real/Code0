package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChangeType
import com.example.data.model.ProposedFileChange
import com.example.data.repository.ProposalRepository
import com.example.ui.theme.DiffAddedGreen
import com.example.ui.theme.DiffAddedGreenText
import com.example.ui.theme.DiffModifiedYellow
import com.example.ui.theme.DiffRemovedRed
import com.example.ui.theme.DiffRemovedRedText

@Composable
fun DiffViewer(
    change: ProposedFileChange,
    diffLines: List<ProposalRepository.DiffLine>,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScroll = rememberScrollState()

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        // File Header & Inclusion Checkbox
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = change.isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.testTag("diff_checkbox_${change.relativePath}")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = change.relativePath,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val badgeColor = when (change.changeType) {
                            ChangeType.CREATED -> DiffAddedGreen
                            ChangeType.MODIFIED -> DiffModifiedYellow
                            ChangeType.DELETED -> DiffRemovedRed
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeColor,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = change.changeType.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        val addedCount = diffLines.count { it.type == ProposalRepository.DiffLineType.ADDED }
                        val removedCount = diffLines.count { it.type == ProposalRepository.DiffLineType.REMOVED }
                        Text(
                            text = "+$addedCount -$removedCount",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Conflict Alert if detected!
        if (change.hasConflict) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Conflict",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = change.conflictReason ?: "Conflict detected: file changed since proposal was generated.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Line-by-line Diff List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .horizontalScroll(horizontalScroll)
        ) {
            items(diffLines) { line ->
                val (bg, textColor, symbol) = when (line.type) {
                    ProposalRepository.DiffLineType.ADDED -> Triple(
                        DiffAddedGreen.copy(alpha = 0.25f),
                        DiffAddedGreenText,
                        "+"
                    )
                    ProposalRepository.DiffLineType.REMOVED -> Triple(
                        DiffRemovedRed.copy(alpha = 0.25f),
                        DiffRemovedRedText,
                        "-"
                    )
                    ProposalRepository.DiffLineType.NORMAL -> Triple(
                        Color.Transparent,
                        MaterialTheme.colorScheme.onSurface,
                        " "
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Old line number
                    Text(
                        text = line.oldLineNum?.toString() ?: "",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.width(32.dp)
                    )
                    // New line number
                    Text(
                        text = line.newLineNum?.toString() ?: "",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.width(32.dp)
                    )
                    // Symbol +/-
                    Text(
                        text = symbol,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.width(16.dp)
                    )
                    // Content
                    Text(
                        text = line.content,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )
                }
            }
        }
    }
}
