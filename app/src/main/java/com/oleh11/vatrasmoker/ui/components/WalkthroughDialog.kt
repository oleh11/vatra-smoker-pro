package com.oleh11.vatrasmoker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oleh11.vatrasmoker.ui.theme.Charcoal600
import com.oleh11.vatrasmoker.ui.theme.Charcoal800
import com.oleh11.vatrasmoker.ui.theme.FlameOrange
import com.oleh11.vatrasmoker.ui.theme.TextGray
import com.oleh11.vatrasmoker.ui.theme.TextWhite

private data class TourPage(val title: String, val body: String)

private val tourPages = listOf(
    TourPage(
        "Welcome to Vatra Smoker Pro",
        "Control and watch your Camp Chef-based pellet grill over Bluetooth. " +
            "No grill nearby? Turn on the Grill Simulator on the Connect tab to try everything."
    ),
    TourPage(
        "Connect your grill",
        "Turn on the grill controller, open the Connect tab and tap Scan Now, then pick your grill. " +
            "Close the official Cabela's app first: the grill talks to one phone at a time."
    ),
    TourPage(
        "Set temperature and smoke",
        "Choose a setpoint and a smoke level on the Grill tab, then press Confirm. " +
            "Nothing is sent to the grill until you confirm."
    ),
    TourPage(
        "Meat probes are optional",
        "Use up to four probes, or switch them off in Settings > Meat Probes to see only grill temperature and smoke level. " +
            "You can reopen this guide any time from Settings."
    )
)

/** One-time, skippable introduction. Shown on first launch and from Settings > How to use. */
@Composable
fun WalkthroughDialog(onFinished: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val page = tourPages[index]
    val isLast = index == tourPages.lastIndex

    AlertDialog(
        onDismissRequest = onFinished,
        containerColor = Charcoal800,
        title = {
            Text(page.title, color = TextWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(page.body, style = MaterialTheme.typography.bodyMedium, color = TextGray)
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tourPages.indices.forEach { i ->
                        Spacer(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (i == index) FlameOrange else Charcoal600, CircleShape)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (isLast) onFinished() else index++ }) {
                Text(if (isLast) "Get started" else "Next", color = FlameOrange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onFinished) {
                Text("Skip", color = TextGray)
            }
        }
    )
}
