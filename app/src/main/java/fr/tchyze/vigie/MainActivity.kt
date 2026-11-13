package fr.tchyze.vigie

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import fr.tchyze.vigie.ui.VigieRoot
import fr.tchyze.vigie.ui.theme.VigieTheme

class MainActivity : ComponentActivity() {
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        askNotificationsOnce()
        setContent {
            VigieTheme {
                VigieRoot()
            }
        }
    }

    /** Une seule demande au premier lancement ; ensuite, l'autorisation se donne depuis les réglages. */
    private fun askNotificationsOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val prefs = getSharedPreferences("lancement", MODE_PRIVATE)
        if (prefs.getBoolean(ASKED, false)) return
        prefs.edit().putBoolean(ASKED, true).apply()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        const val ASKED = "notifications_demandees"
    }
}
