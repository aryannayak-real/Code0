package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxType

@Composable
fun CodeEditorView(
    content: String,
    onContentChange: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    fileExtension: String,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(content) {
        mutableStateOf(TextFieldValue(content))
    }

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val lines = content.lines()
    val lineCount = maxOf(1, lines.size)

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Quick Actions & Search bar toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onUndo, modifier = Modifier.testTag("editor_undo_btn")) {
                Icon(Icons.Default.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRedo, modifier = Modifier.testTag("editor_redo_btn")) {
                Icon(Icons.Default.Redo, contentDescription = "Redo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { isSearchOpen = !isSearchOpen }, modifier = Modifier.testTag("editor_search_btn")) {
                Icon(Icons.Default.Search, contentDescription = "Search & Replace", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${lines.size} lines | $fileExtension",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Search & Replace Bar
        if (isSearchOpen) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Find...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = { replaceQuery = it },
                            placeholder = { Text("Replace...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        )
                        IconButton(onClick = { isSearchOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    }
                    Row {
                        TextButton(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    val newContent = content.replace(searchQuery, replaceQuery)
                                    onContentChange(newContent)
                                }
                            }
                        ) {
                            Text("Replace All", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Editor Body: Line numbers + Code input
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(verticalScrollState)
        ) {
            // Line Numbers Gutter (Single Composable for O(1) Compose layout overhead)
            val lineNumbersText = remember(lineCount) {
                (1..lineCount).joinToString("\n")
            }
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = lineNumbersText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline,
                    lineHeight = 20.sp
                )
            }

            // Editable Code Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        onContentChange(it.text)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("code_editor_text_field"),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = { text ->
                        androidx.compose.ui.text.input.TransformedText(
                            highlightSyntax(text.text, fileExtension),
                            androidx.compose.ui.text.input.OffsetMapping.Identity
                        )
                    }
                )
            }
        }

        // Soft Symbol & Productivity Quick Bar for Mobile Keyboard
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            val symbolScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(symbolScrollState)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TAB / Indent 4 spaces
                Surface(
                    onClick = {
                        val currentText = textFieldValue.text
                        val cursor = textFieldValue.selection.start
                        val indent = "    "
                        val newText = if (cursor in 0..currentText.length) {
                            currentText.substring(0, cursor) + indent + currentText.substring(cursor)
                        } else currentText + indent
                        val newCursor = cursor + indent.length
                        textFieldValue = TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(newCursor))
                        onContentChange(newText)
                    },
                    modifier = Modifier.padding(horizontal = 2.dp).height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(text = "TAB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // UNINDENT
                Surface(
                    onClick = {
                        val currentText = textFieldValue.text
                        val cursor = textFieldValue.selection.start
                        if (cursor > 0) {
                            val lineStart = currentText.lastIndexOf('\n', cursor - 1) + 1
                            val linePrefix = currentText.substring(lineStart, cursor)
                            val spacesToRemove = minOf(4, linePrefix.takeWhile { it == ' ' }.length)
                            if (spacesToRemove > 0) {
                                val newText = currentText.substring(0, lineStart) + currentText.substring(lineStart + spacesToRemove)
                                val newCursor = maxOf(lineStart, cursor - spacesToRemove)
                                textFieldValue = TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(newCursor))
                                onContentChange(newText)
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 2.dp).height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(text = "UNTAB", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Smart Pair Map
                val pairs = mapOf(
                    "{" to "{}",
                    "(" to "()",
                    "[" to "[]",
                    "\"" to "\"\"",
                    "'" to "''",
                    "<" to "<>"
                )

                val symbols = listOf("{", "(", "[", "\"", "'", "<", "}", ")", "]", ";", "=", "/", "$", ":", "_", ".", "->", "=>")

                for (sym in symbols) {
                    val pairInsertion = pairs[sym]
                    Surface(
                        onClick = {
                            val currentText = textFieldValue.text
                            val cursor = textFieldValue.selection.start
                            val insertString = pairInsertion ?: sym
                            val newText = if (cursor in 0..currentText.length) {
                                currentText.substring(0, cursor) + insertString + currentText.substring(cursor)
                            } else {
                                currentText + insertString
                            }
                            // If auto-paired, position cursor between the pair!
                            val newCursor = if (pairInsertion != null) cursor + 1 else cursor + sym.length
                            textFieldValue = TextFieldValue(
                                text = newText,
                                selection = androidx.compose.ui.text.TextRange(newCursor)
                            )
                            onContentChange(newText)
                        },
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .height(34.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                            Text(
                                text = sym,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Comment line toggle
                Surface(
                    onClick = {
                        val currentText = textFieldValue.text
                        val cursor = textFieldValue.selection.start
                        val lineStart = if (cursor > 0) currentText.lastIndexOf('\n', cursor - 1) + 1 else 0
                        val lineEnd = currentText.indexOf('\n', cursor).let { if (it == -1) currentText.length else it }
                        val currentLine = currentText.substring(lineStart, lineEnd)

                        val newLine = if (currentLine.trimStart().startsWith("//")) {
                            currentLine.replaceFirst("// ", "").let { if (it == currentLine) currentLine.replaceFirst("//", "") else it }
                        } else {
                            "// $currentLine"
                        }
                        val newText = currentText.substring(0, lineStart) + newLine + currentText.substring(lineEnd)
                        val newCursor = (cursor + (newLine.length - currentLine.length)).coerceIn(0, newText.length)
                        textFieldValue = TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(newCursor))
                        onContentChange(newText)
                    },
                    modifier = Modifier.padding(horizontal = 2.dp).height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(text = "//", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * Pre-compiled regex cache and bounds for fast, memory-safe syntax highlighting.
 * Eliminates repeated regex instantiations and regex backtracking freezes on main looper.
 */
private object SyntaxRegexCache {
    val stringRegex = Regex("\"(\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])*'")
    val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
    val commentRegex = Regex("//.*|#.*")

    private val keywordPatterns = mapOf(
        "kt" to Regex("\\b(fun|val|var|class|interface|package|import|return|if|else|when|for|while|override|public|private|data|sealed|object|companion|suspend)\\b"),
        "java" to Regex("\\b(public|private|protected|class|interface|void|int|boolean|String|return|if|else|for|while|new|import|package|static|final)\\b"),
        "py" to Regex("\\b(def|class|import|from|return|if|elif|else|for|while|in|with|as|try|except|lambda|True|False|None|pass|async|await)\\b"),
        "js" to Regex("\\b(function|const|let|var|return|if|else|for|while|class|export|import|from|async|await|true|false|null|undefined|new)\\b"),
        "ts" to Regex("\\b(function|const|let|var|return|if|else|for|while|class|export|import|from|async|await|true|false|null|type|interface)\\b"),
        "html" to Regex("<(/?[a-zA-Z0-9]+)((\\s+[a-zA-Z0-9_:-]+(=(\"[^\"]*\"|'[^']*'))?)*)\\s*/?>"),
        "xml" to Regex("<(/?[a-zA-Z0-9_:-]+)((\\s+[a-zA-Z0-9_:-]+(=(\"[^\"]*\"|'[^']*'))?)*)\\s*/?>"),
        "default" to Regex("\\b(fun|def|class|return|if|else|val|var|const|let)\\b")
    )

    fun getKeywordRegex(extension: String): Regex {
        return keywordPatterns[extension.lowercase()] ?: keywordPatterns["default"]!!
    }
}

/**
 * Memory-safe token-based syntax highlighting with length safety cap
 */
fun highlightSyntax(code: String, extension: String): androidx.compose.ui.text.AnnotatedString {
    // Safety check: if file is huge (> 40,000 characters), avoid blocking the main UI thread with full-text regex
    if (code.length > 40_000) {
        return buildAnnotatedString { append(code) }
    }

    return try {
        buildAnnotatedString {
            append(code)

            val keywordRegex = SyntaxRegexCache.getKeywordRegex(extension)
            keywordRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = SyntaxKeyword), match.range.first, match.range.last + 1)
            }

            // String literals
            SyntaxRegexCache.stringRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = SyntaxString), match.range.first, match.range.last + 1)
            }

            // Numbers
            SyntaxRegexCache.numberRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = SyntaxNumber), match.range.first, match.range.last + 1)
            }

            // Comments
            SyntaxRegexCache.commentRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = SyntaxComment), match.range.first, match.range.last + 1)
            }
        }
    } catch (_: Exception) {
        buildAnnotatedString { append(code) }
    }
}
