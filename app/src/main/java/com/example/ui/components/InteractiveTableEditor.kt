package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AppStrings
import com.example.ui.theme.LocalGlassTokens

data class AndroidTableColumn(
    val id: String,
    var name: String,
    var type: String = "text" // text, number, checkbox, status
)

data class AndroidTableRow(
    val values: MutableMap<String, String> = mutableMapOf()
)

@Composable
fun InteractiveTableEditorDialog(
    initialRows: Int = 3,
    initialCols: Int = 3,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSaveMarkdown: (String) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val columns = remember {
        mutableStateListOf<AndroidTableColumn>().apply {
            for (c in 1..initialCols) {
                add(AndroidTableColumn(id = "col_$c", name = "Column $c", type = "text"))
            }
        }
    }
    val rows = remember {
        mutableStateListOf<AndroidTableRow>().apply {
            for (r in 1..initialRows) {
                val row = AndroidTableRow()
                columns.forEach { col ->
                    row.values[col.id] = "Item $r"
                }
                add(row)
            }
        }
    }

    var tableTitle by remember { mutableStateOf("Project Table") }

    fun addRow() {
        val newRow = AndroidTableRow()
        columns.forEach { col ->
            newRow.values[col.id] = "New"
        }
        rows.add(newRow)
    }

    fun addCol() {
        val colId = "col_${System.currentTimeMillis() % 10000}"
        val newCol = AndroidTableColumn(id = colId, name = "Col ${columns.size + 1}")
        columns.add(newCol)
        rows.forEach { row ->
            row.values[colId] = "Val"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.TableChart, null, tint = tokens.accentStart, modifier = Modifier.size(22.dp))
                        OutlinedTextField(
                            value = tableTitle,
                            onValueChange = { tableTitle = it },
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = tokens.textPrimary),
                            singleLine = true,
                            modifier = Modifier.width(200.dp)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = tokens.textSecondary)
                    }
                }

                // Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = ::addRow,
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.accentStart)
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Row", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = ::addCol) {
                        Icon(Icons.Rounded.ViewColumn, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Column", fontSize = 12.sp)
                    }
                }

                // Table Grid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0B101B))
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier
                                .background(Brush.horizontalGradient(listOf(tokens.accentStart.copy(alpha = 0.3f), tokens.accentEnd.copy(alpha = 0.3f))))
                                .padding(vertical = 8.dp)
                        ) {
                            Box(modifier = Modifier.width(50.dp), contentAlignment = Alignment.Center) {
                                Text("#", color = tokens.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            columns.forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    OutlinedTextField(
                                        value = col.name,
                                        onValueChange = { col.name = it },
                                        textStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = tokens.textPrimary),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        // Data rows
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            itemsIndexed(rows) { rIdx, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier =Modifier.width(50.dp), contentAlignment = Alignment.Center) {
                                        Text("${rIdx + 1}", color = tokens.textSecondary, fontSize = 11.sp)
                                    }
                                    columns.forEach { col ->
                                        Box(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            val cellVal = row.values[col.id] ?: ""
                                            OutlinedTextField(
                                                value = cellVal,
                                                onValueChange = { row.values[col.id] = it },
                                                textStyle = MaterialTheme.typography.bodySmall.copy(color = tokens.textPrimary),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancelBtn)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // Build Markdown table representation
                            var md = "### $tableTitle\n\n"
                            md += "| " + columns.joinToString(" | ") { it.name } + " |\n"
                            md += "| " + columns.joinToString(" | ") { "---" } + " |\n"
                            rows.forEach { row ->
                                md += "| " + columns.joinToString(" | ") { col -> row.values[col.id] ?: "" } + " |\n"
                            }
                            onSaveMarkdown(md)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.accentStart)
                    ) {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save & Insert")
                    }
                }
            }
        }
    }
}
