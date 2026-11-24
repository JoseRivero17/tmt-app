package com.uade.tmt.presentation.torneos

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.uade.tmt.R
import com.uade.tmt.presentation.theme.TMTTheme

/** Encabezado azul de Inicio (A8): logo, título y cantidad de torneos. */
@Composable
internal fun EncabezadoInicio(cantidadTorneos: Int?, modifier: Modifier = Modifier) {
    IconosClarosEnBarraDeEstado()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TMTTheme.colores.marca)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Logo(modifier = Modifier.padding(bottom = 8.dp))
        Text(
            text = stringResource(R.string.torneos_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = TMTTheme.colores.sobreMarca,
            modifier = Modifier.semantics { heading() },
        )
        if (cantidadTorneos != null) {
            Text(
                text = pluralStringResource(R.plurals.torneos_en_calendario, cantidadTorneos, cantidadTorneos),
                style = MaterialTheme.typography.bodyMedium,
                color = TMTTheme.colores.sobreMarcaVariante,
            )
        }
    }
}

@Composable
private fun Logo(modifier: Modifier = Modifier) {
    val colores = TMTTheme.colores
    val (marca, go) = stringResource(R.string.torneos_marca_1) to stringResource(R.string.torneos_marca_2)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.logo_pelotita), contentDescription = null, modifier = Modifier.size(24.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = colores.sobreMarca)) { append("$marca ") }
                withStyle(SpanStyle(color = colores.pelotita)) { append(go) }
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Sobre el azul de marca los íconos del sistema van en claro; al salir de la pantalla se restauran. */
@Composable
private fun IconosClarosEnBarraDeEstado() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controlador = window?.let { WindowCompat.getInsetsController(it, view) }
        val anterior = controlador?.isAppearanceLightStatusBars
        controlador?.isAppearanceLightStatusBars = false
        onDispose { if (anterior != null) controlador.isAppearanceLightStatusBars = anterior }
    }
}
