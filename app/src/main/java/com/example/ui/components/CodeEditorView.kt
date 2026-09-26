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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.editor.CodeHighlighter

@Composable
fun CodeEditorView(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fileExtension: String,
    onInsertSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val linesCount by remember(textFieldValue.text) {
        derivedStateOf {
            maxOf(1, textFieldValue.text.count { it == '\n' } + 1)
        }
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val syntaxTransformation = remember(fileExtension) {
        VisualTransformation { original ->
            val highlighted = CodeHighlighter.highlight(original.text, fileExtension)
            TransformedText(highlighted, OffsetMapping.Identity)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Main Editor with Line Numbers
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Line Numbers Gutter
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF090D16))
                    .verticalScroll(verticalScrollState)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..linesCount) {
                    Text(
                        text = "$i",
                        color = Color(0xFF475569),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Code Text Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState)
                    .verticalScroll(verticalScrollState)
                    .padding(start = 12.dp, end = 24.dp, top = 12.dp, bottom = 24.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFFE2E8F0)
                    ),
                    cursorBrush = SolidColor(Color(0xFF38BDF8)),
                    visualTransformation = syntaxTransformation,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("code_editor_input")
                )
            }
        }

        // Quick Symbol Keyboard Toolbar
        QuickSymbolToolbar(onInsertSymbol = onInsertSymbol)
    }
}

@Composable
fun QuickSymbolToolbar(
    onInsertSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickSymbols = listOf(
        "<", ">", "/", "=", "\"", "'", "{", "}", "(", ")",
        "[", "]", ";", ":", "!", "$", ".", "#", "+", "-",
        "*", ",", "&&", "||", "div", "script", "style", "console.log"
    )

    Surface(
        color = Color(0xFF1E293B),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(quickSymbols) { sym ->
                Surface(
                    onClick = {
                        val toInsert = when (sym) {
                            "div" -> "<div></div>"
                            "script" -> "<script></script>"
                            "style" -> "<style></style>"
                            "console.log" -> "console.log();"
                            else -> sym
                        }
                        onInsertSymbol(toInsert)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF334155),
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(34.dp)
                        .testTag("quick_sym_$sym")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = sym,
                            color = if (sym.length > 2) Color(0xFF38BDF8) else Color(0xFFF1F5F9),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
