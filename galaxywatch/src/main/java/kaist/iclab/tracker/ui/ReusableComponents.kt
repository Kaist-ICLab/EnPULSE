package kaist.iclab.tracker.ui


/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import kaist.iclab.tracker.ui.theme.WearAppTheme


@Composable
fun BasicIconButton(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    imageVector: ImageVector = Icons.Rounded.Phone,
    contentDescription: String = "triggers phone action",
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Button
        Button(
            modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
            onClick = onClick,
        ) {
            Icon(
                imageVector,
                contentDescription,
                modifier = iconModifier,
            )
        }
    }
}


@Composable
fun BasicCard(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier,
        appImage = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Message,
                contentDescription = "triggers open message action",
                modifier = iconModifier,
            )
        },
        appName = { Text("Messages") },
        time = { Text("12m") },
        title = { Text("Kim Green") },
        onClick = { /* ... */ },
    ) {
        Text("On my way!")
    }
}

@Composable
fun BasicChip(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    Chip(
        modifier = modifier,
        onClick = { /* ... */ },
        label = {
            Text(
                text = "5 minute Meditation",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.SelfImprovement,
                contentDescription = "triggers meditation action",
                modifier = iconModifier,
            )
        },
    )
}

@Composable
fun BasicToggleChip(
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    onEnable: () -> Unit = {},
    onDisable: () -> Unit = {},
    text: String = "TEXT",
) {
    ToggleChip(
        modifier = modifier,
        checked = checked,
        toggleControl = {
            Switch(
                checked = checked,
                enabled = enabled,
                modifier = Modifier.semantics {
                    this.contentDescription = if (checked) "On" else "Off"
                },
            )
        },
        onCheckedChange = {
            if (it) onEnable() else onDisable()
        },
        label = {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}


// Button Preview
@WearPreviewSmallRound
@Composable
fun ButtonExamplePreview() {
    WearAppTheme {
        BasicIconButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            iconModifier = Modifier
                .size(24.dp)
                .wrapContentSize(align = Alignment.Center),
        )
    }
}

// Card Preview
@WearPreviewSmallRound
@Composable
fun BasicCardPreview() {
    WearAppTheme {
        BasicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            iconModifier = Modifier
                .size(24.dp)
                .wrapContentSize(align = Alignment.Center),
        )
    }
}

// Chip Preview
@WearPreviewSmallRound
@Composable
fun BasicChipPreview() {
    WearAppTheme {
        BasicChip(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            iconModifier = Modifier
                .size(24.dp)
                .wrapContentSize(align = Alignment.Center),
        )
    }
}

// Toggle Chip Preview
@WearPreviewSmallRound
@Composable
fun BasicToggleChipPreview() {
    WearAppTheme {
        BasicToggleChip(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
    }
}
