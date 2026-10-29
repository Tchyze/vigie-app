package fr.tchyze.vigie.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.tchyze.vigie.ui.branch.BranchScreen
import fr.tchyze.vigie.ui.feed.FeedScreen
import fr.tchyze.vigie.ui.follow.FollowScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object FeedRoute
@Serializable data object InventoryRoute
@Serializable data object AdvisoriesRoute
@Serializable data object SettingsRoute
@Serializable data class BranchRoute(val id: String)
@Serializable data object FollowRoute

private data class Tab(val label: String, val icon: ImageVector, val route: Any, val type: KClass<*>)

private val tabs = listOf(
    Tab("Fil", Icons.Outlined.DynamicFeed, FeedRoute, FeedRoute::class),
    Tab("Inventaire", Icons.Outlined.Dns, InventoryRoute, InventoryRoute::class),
    Tab("Avis", Icons.Outlined.Shield, AdvisoriesRoute, AdvisoriesRoute::class),
    Tab("Réglages", Icons.Outlined.Settings, SettingsRoute, SettingsRoute::class),
)

@Composable
fun VigieRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = destination?.hierarchy?.any { it.hasRoute(tab.type) } == true,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = FeedRoute, modifier = Modifier.padding(padding)) {
            composable<FeedRoute> {
                FeedScreen(
                    onOpenBranch = { nav.navigate(BranchRoute(it)) },
                    onOpenFollow = { nav.navigate(FollowRoute) },
                )
            }
            composable<FollowRoute> { FollowScreen(onBack = { nav.popBackStack() }) }
            composable<BranchRoute> { entry ->
                BranchScreen(branchId = entry.toRoute<BranchRoute>().id, onBack = { nav.popBackStack() })
            }
            composable<InventoryRoute> { Placeholder("Inventaire") }
            composable<AdvisoriesRoute> { Placeholder("Avis de sécurité") }
            composable<SettingsRoute> { Placeholder("Réglages") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Placeholder(title: String) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(title) })
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("À venir") }
    }
}
