package com.uade.tmt.presentation.login

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uade.tmt.R
import com.uade.tmt.presentation.theme.TMTTheme

/** RF02: acceso de jugadores. Al ingresar con éxito llama a [onLoginExitoso] (vuelve atrás). */
@Composable
fun LoginScreen(
    onVolver: () -> Unit,
    onLoginExitoso: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(estado.exito) { if (estado.exito) onLoginExitoso() }

    LoginContent(
        estado = estado,
        onCodigo = viewModel::cambiarCodigo,
        onClave = viewModel::cambiarClave,
        onIngresar = viewModel::ingresar,
        onVolver = onVolver,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginContent(
    estado: LoginUiState,
    onCodigo: (String) -> Unit,
    onClave: (String) -> Unit,
    onIngresar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enlaces = LocalUriHandler.current
    val urlRegistro = stringResource(R.string.url_registro)
    val urlRecuperar = stringResource(R.string.url_recuperar_clave)

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.login_titulo)) },
            navigationIcon = {
                IconButton(onClick = onVolver) {
                    Icon(painterResource(R.drawable.ic_volver), stringResource(R.string.login_volver))
                }
            },
            windowInsets = WindowInsets(0),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.login_explicacion),
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = estado.codigo,
                onValueChange = onCodigo,
                label = { Text(stringResource(R.string.login_codigo)) },
                singleLine = true,
                enabled = !estado.cargando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            // Siempre oculta: no hay botón para mostrarla ni opción "Recordar" (RNF06).
            OutlinedTextField(
                value = estado.clave,
                onValueChange = onClave,
                label = { Text(stringResource(R.string.login_clave)) },
                singleLine = true,
                enabled = !estado.cargando,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onIngresar() }),
                modifier = Modifier.fillMaxWidth(),
            )

            estado.error?.let { error ->
                AvisoError(
                    texto = stringResource(error.mensaje),
                    onReintentar = onIngresar.takeIf { estado.ofreceReintentar },
                )
            }

            Button(
                onClick = onIngresar,
                enabled = estado.puedeIngresar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                if (estado.cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(stringResource(R.string.login_ingresando))
                } else {
                    Text(stringResource(R.string.login_ingresar))
                }
            }

            if (estado.modoDemo) {
                Text(
                    text = stringResource(R.string.login_modo_demo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = { enlaces.openUri(urlRegistro) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.login_registrarme)) }
                TextButton(
                    onClick = { enlaces.openUri(urlRecuperar) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.login_olvide_clave)) }
            }
        }
    }
}

/** Error en línea con "Reintentar" opcional. También lo usa la reserva. */
@Composable
internal fun AvisoError(texto: String, onReintentar: (() -> Unit)?, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (onReintentar != null) {
                TextButton(onClick = onReintentar, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        text = stringResource(R.string.estado_reintentar),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@get:StringRes
private val ErrorLogin.mensaje: Int
    get() = when (this) {
        ErrorLogin.DATOS_INCOMPLETOS -> R.string.login_error_datos_incompletos
        ErrorLogin.CREDENCIALES_INVALIDAS -> R.string.login_error_credenciales
        ErrorLogin.SIN_CONEXION -> R.string.login_error_sin_conexion
        ErrorLogin.ERROR_SITIO -> R.string.login_error_sitio
    }

@PreviewLightDark
@Composable
private fun LoginPreview() {
    TMTTheme {
        Surface {
            LoginContent(
                estado = LoginUiState(codigo = "99999", error = ErrorLogin.ERROR_SITIO, modoDemo = true),
                onCodigo = {},
                onClave = {},
                onIngresar = {},
                onVolver = {},
            )
        }
    }
}
