package com.github.mobdev778.plugin.findinclass.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.mobdev778.MyBundle
import com.github.mobdev778.plugin.findinclass.domain.FieldDescriptor
import com.github.mobdev778.plugin.findinclass.domain.FieldType
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
internal fun DescriptorRow(
    descriptor: FieldDescriptor,
    onClick: () -> Unit,
    onCopyClick: () -> Unit,
) {
    val fullName = descriptor.getFullName()

    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Icon(
                key = AllIconsKeys.Actions.Copy,
                contentDescription = MyBundle.message("dialog.copy"),
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = {
                        onCopyClick()
                    })
            )
            Text(fullName, Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp))
            Text(
                text = ": ",
                color = Color.DarkGray,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            when (descriptor.fieldType) {
                FieldType.List -> {
                    Text(
                        text = "List<${descriptor.type}>",
                        color = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }

                FieldType.Array -> {
                    Text(
                        text = "${descriptor.type}[]",
                        color = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }

                FieldType.Set -> {
                    Text(
                        text = "Set<${descriptor.type}>",
                        color = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }

                FieldType.Collection -> {
                    Text(
                        text = "Collection<${descriptor.type}>",
                        color = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }

                FieldType.Default -> {
                    Text(
                        text = descriptor.type,
                        color = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
