package com.neoland.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoland.app.core.IslandBus
import com.neoland.app.core.tr
import com.neoland.app.data.IslandPlacement
import com.neoland.app.data.IslandSettings
import com.neoland.app.data.SettingsRepo
import com.neoland.app.island.rememberHaptic
import com.neoland.app.ui.Group
import com.neoland.app.ui.GroupStepSlider
import com.neoland.app.ui.GroupToggle
import com.neoland.app.ui.Page
import com.neoland.app.ui.Pos
import com.neoland.app.ui.Tile
import com.neoland.app.ui.Ui

private enum class PlacementProfile { HOME, LOCK }

/** Настройки геометрии хранятся отдельно для Home Screen и Lock Screen. */
@Composable
fun SizeScreen(onBack: () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    var profile by remember { mutableStateOf(PlacementProfile.HOME) }
    val original = remember { SettingsRepo.current }
    val haptic = rememberHaptic()
    val placement = if (profile == PlacementProfile.LOCK) {
        s.lockPlacement ?: IslandPlacement(s.offsetX, s.offsetY, s.width, s.height)
    } else {
        IslandPlacement(s.offsetX, s.offsetY, s.width, s.height)
    }

    DisposableEffect(Unit) {
        IslandBus.calibrating.value = true
        IslandBus.calibrationLockProfile.value = false
        onDispose {
            IslandBus.calibrating.value = false
            IslandBus.calibrationLockProfile.value = false
        }
    }

    fun setPlacement(block: IslandPlacement.() -> IslandPlacement) {
        SettingsRepo.update {
            val current = if (profile == PlacementProfile.LOCK) {
                lockPlacement ?: IslandPlacement(offsetX, offsetY, width, height)
            } else IslandPlacement(offsetX, offsetY, width, height)
            val next = current.block()
            if (profile == PlacementProfile.LOCK) copy(lockPlacement = next)
            else copy(offsetX = next.offsetX, offsetY = next.offsetY, width = next.width, height = next.height)
        }
    }

    val cancel = {
        SettingsRepo.update {
            copy(
                offsetX = original.offsetX, offsetY = original.offsetY, width = original.width,
                height = original.height, lockPlacement = original.lockPlacement,
            )
        }
        onBack()
    }
    BackHandler(onBack = cancel)

    Box(Modifier.fillMaxSize()) {
        Page(
            tr("Положение и размер", "Position and size", "Posisi dan ukuran"),
            onBack = cancel,
            action = {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Ui.Card).clickable {
                        setPlacement { IslandPlacement() }
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Restore, tr("Сбросить", "Reset", "Atur ulang profil ini"), tint = Ui.Text)
                }
            },
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Ui.Card).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ProfileTab(
                    modifier = Modifier.weight(1f),
                    selected = profile == PlacementProfile.HOME,
                    icon = { Icon(Icons.Rounded.Home, null, modifier = Modifier.size(18.dp)) },
                    title = tr("Главный экран", "Home screen", "Layar utama"),
                ) {
                    profile = PlacementProfile.HOME
                    IslandBus.calibrationLockProfile.value = false
                }
                ProfileTab(
                    modifier = Modifier.weight(1f),
                    selected = profile == PlacementProfile.LOCK,
                    icon = { Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(18.dp)) },
                    title = tr("Экран блокировки", "Lock screen", "Layar kunci"),
                ) {
                    profile = PlacementProfile.LOCK
                    IslandBus.calibrationLockProfile.value = true
                }
            }
            Spacer(Modifier.height(14.dp))
            Group {
                GroupStepSlider(
                    Pos.First, Icons.Rounded.SwapHoriz, tr("По горизонтали", "Horizontal", "Posisi horizontal"), placement.offsetX, -200..200,
                    action = tr("Центр", "Center", "Tengah") to { setPlacement { copy(offsetX = 0) } }, color = Tile.Blue,
                ) { v -> setPlacement { copy(offsetX = v) } }
                GroupStepSlider(
                    Pos.Last, Icons.Rounded.SwapVert, tr("Отступ сверху", "Top offset", "Jarak dari atas"), placement.offsetY, -10..120,
                    color = Tile.Indigo,
                ) { v -> setPlacement { copy(offsetY = v) } }
            }
            Spacer(Modifier.height(16.dp))
            Group {
                GroupStepSlider(Pos.First, Icons.Rounded.Straighten, tr("Ширина", "Width", "Lebar"), placement.width, 70..170, color = Tile.Purple) { v ->
                    setPlacement { copy(width = v) }
                }
                GroupStepSlider(Pos.Last, Icons.Rounded.Height, tr("Высота", "Height", "Tinggi"), placement.height, 22..46, color = Tile.Pink) { v ->
                    setPlacement { copy(height = v) }
                }
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp)
                .shadow(12.dp, CircleShape).clip(CircleShape).background(Ui.Primary)
                .clickable { haptic(); onBack() }.padding(horizontal = 30.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Check, null, tint = Ui.OnPrimary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("Сохранить", "Save"), color = Ui.OnPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProfileTab(
    modifier: Modifier,
    selected: Boolean,
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(if (selected) Ui.Primary.copy(alpha = 0.22f) else Ui.Card)
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(title, color = Ui.Text, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}
