package fr.tchyze.vigie.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.tchyze.vigie.AppContainer
import fr.tchyze.vigie.VigieApp

/** Fabrique de ViewModel qui reçoit le conteneur de dépendances de l'application. */
inline fun <reified VM : ViewModel> containerFactory(crossinline create: CreationExtras.(AppContainer) -> VM) =
    viewModelFactory {
        initializer { create((this[APPLICATION_KEY] as VigieApp).container) }
    }
