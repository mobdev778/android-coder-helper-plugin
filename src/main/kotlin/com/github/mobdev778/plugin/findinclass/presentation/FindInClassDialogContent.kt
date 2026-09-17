package com.github.mobdev778.plugin.findinclass.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.mobdev778.MyBundle
import com.github.mobdev778.plugin.findinclass.domain.FieldDescriptor
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

@Composable
internal fun FindInClassDialogContent(
    className: String?,
    descriptors: List<FieldDescriptor>,
    onSearch: (String) -> Unit,
    onDescriptorClick: (FieldDescriptor) -> Unit,
    onCopyClick: (FieldDescriptor) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    onClose()
                    true
                } else {
                    false
                }
            },
    ) {
        Text(MyBundle.message("dialog.className", className ?: ""))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            var query by remember { mutableStateOf(TextFieldValue()) }
            TextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it.text)
                },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        if (descriptors.isEmpty()) {
            Text(MyBundle.message("dialog.noFields"))
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(descriptors) { descriptor ->
                    DescriptorRow(
                        descriptor = descriptor,
                        onClick = { onDescriptorClick(descriptor) },
                        onCopyClick = { onCopyClick(descriptor) },
                    )
                }
            }
        }
    }
}

