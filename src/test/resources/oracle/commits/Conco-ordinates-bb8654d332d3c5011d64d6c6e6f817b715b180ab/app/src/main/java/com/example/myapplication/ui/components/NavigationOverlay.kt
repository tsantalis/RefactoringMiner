package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.models.NavigationState
import com.example.myapplication.ui.theme.ConcordiaMaroon

@Composable
fun NavigationOverlay(
    navState:        NavigationState,
    onRecenterClick: () -> Unit,
    onExit:          () -> Unit,
    destinationName: () -> String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top Instruction Box
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape           = RoundedCornerShape(12.dp),
            color           = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier          = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowRightAlt,
                    contentDescription = null,
                    tint               = ConcordiaMaroon
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text  = navState.currentInstruction,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
            }
        }

        // Bottom Button Row
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // RECENTER
            Button(
                onClick  = onRecenterClick,
                modifier = Modifier.height(54.dp).weight(1.6f),
                shape    = RoundedCornerShape(27.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon),
                elevation        = ButtonDefaults.buttonElevation(4.dp),
                contentPadding   = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text     = "RECENTER",
                    style    = MaterialTheme.typography.labelLarge.copy(
                        fontWeight   = FontWeight.Bold,
                        fontSize     = 12.sp,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }

            // EXIT
            Button(
                onClick        = onExit,
                modifier       = Modifier.height(54.dp).weight(0.7f),
                shape          = RoundedCornerShape(12.dp),
                colors         = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon),
                elevation      = ButtonDefaults.buttonElevation(4.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text  = "EXIT",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize   = 12.sp
                    )
                )
            }
        }
    }

    if (navState.hasArrived) {
        AlertDialog(
            onDismissRequest = { /* Force action */ },
            title            = { Text("Destination Reached", fontWeight = FontWeight.Bold) },
            text             = { Text("You have arrived at ${destinationName()}.") },
            confirmButton    = {
                Button(
                    onClick = onExit,
                    colors  = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon)
                ) { Text("END TRIP", color = Color.White) }
            },
            shape          = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}
