package fr.tchyze.vigie.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler

/** Ouvre un lien officiel dans le navigateur ; ne fait rien si aucun navigateur n'est disponible. */
@Composable
fun rememberOpenUrl(): (String) -> Unit {
    val handler = LocalUriHandler.current
    return { url -> runCatching { handler.openUri(url) } }
}
