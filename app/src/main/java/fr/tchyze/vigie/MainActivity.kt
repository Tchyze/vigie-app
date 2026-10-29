package fr.tchyze.vigie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.tchyze.vigie.ui.VigieRoot
import fr.tchyze.vigie.ui.theme.VigieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            VigieTheme {
                VigieRoot()
            }
        }
    }
}
