package com.uade.tmt.presentation.torneos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.uade.tmt.R

/** Carga con la forma del contenido (A2). Para TalkBack es un solo "Cargando". */
@Composable
internal fun SiluetaTorneos(modifier: Modifier = Modifier) {
    val cargando = stringResource(R.string.estado_cargando)
    Column(
        modifier = modifier
            .padding(16.dp)
            .clearAndSetSemantics { contentDescription = cargando },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(4) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0.4f, 0.85f, 0.6f, 0.7f).forEach { Barra(it) }
                }
            }
        }
    }
}

@Composable
private fun Barra(ancho: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(ancho)
            .height(16.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small),
    )
}
