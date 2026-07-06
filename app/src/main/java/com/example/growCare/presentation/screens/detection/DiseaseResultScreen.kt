package com.example.growCare.presentation.screens.detection

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.growCare.domain.model.DiseaseAnalysis
import com.example.growCare.domain.model.toDisplayString
import com.example.growCare.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseResultScreen(
    analysis: DiseaseAnalysis,
    imageUrl: String?,
    onNavigateBack: () -> Unit,
    onScanAnother: () -> Unit = onNavigateBack,
    onNavigateToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Use Material3 theme colors for semantic meaning
    val severityColor = when (analysis.severity) {
        com.example.growCare.domain.model.DiseaseSeverity.SEVERE -> MaterialTheme.colorScheme.error
        com.example.growCare.domain.model.DiseaseSeverity.MODERATE -> MaterialTheme.colorScheme.tertiary
        com.example.growCare.domain.model.DiseaseSeverity.MILD -> MaterialTheme.colorScheme.primary
    }
    
    val severityBgColor = when (analysis.severity) {
        com.example.growCare.domain.model.DiseaseSeverity.SEVERE -> 
            MaterialTheme.colorScheme.errorContainer
        com.example.growCare.domain.model.DiseaseSeverity.MODERATE -> 
            MaterialTheme.colorScheme.tertiaryContainer
        com.example.growCare.domain.model.DiseaseSeverity.MILD -> 
            MaterialTheme.colorScheme.primaryContainer
    }
    
    val severityIcon = when (analysis.severity) {
        com.example.growCare.domain.model.DiseaseSeverity.SEVERE -> Icons.Default.Warning
        com.example.growCare.domain.model.DiseaseSeverity.MODERATE -> Icons.Default.Warning
        com.example.growCare.domain.model.DiseaseSeverity.MILD -> Icons.Default.CheckCircle
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Disease Analysis", 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (imageUrl != null) {
                Card(modifier = Modifier.fillMaxWidth().height(200.dp), shape = RoundedCornerShape(12.dp)) {
                    AsyncImage(model = imageUrl, contentDescription = "Plant Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }

            // Disease Header Card - minimalist design
            Card(
                modifier = Modifier.fillMaxWidth(), 
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ), 
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        analysis.diseaseName, 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Severity indicator with color-coded badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Severity:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = severityColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                analysis.severity.toDisplayString(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = severityColor
                            )
                        }
                    }
                    
                    // Confidence Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Detection Confidence", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${analysis.confidence}%", 
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        LinearProgressIndicator(
                            progress = { analysis.confidence / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }

            // Symptoms Section
            ResultSection(
                title = "Symptoms", 
                items = analysis.symptoms, 
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Treatment Section
            ResultSection(
                title = "Treatment", 
                items = analysis.treatment, 
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Prevention Section
            ResultSection(
                title = "Prevention", 
                items = analysis.prevention, 
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!analysis.additionalNotes.isNullOrEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Additional Notes", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            analysis.additionalNotes ?: "", 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Action Buttons
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToHome,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Home, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Home", fontWeight = FontWeight.SemiBold)
                }
                
                Button(
                    onClick = onScanAnother,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.CameraAlt, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Another", fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ResultSection(
    title: String, 
    items: List<String>, 
    backgroundColor: Color, 
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(12.dp), 
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "•", 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = textColor
                    )
                    Text(
                        item, 
                        style = MaterialTheme.typography.bodyMedium, 
                        modifier = Modifier.weight(1f), 
                        color = textColor
                    )
                }
            }
        }
    }
}

