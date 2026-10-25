package com.example.avenor

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.avenor.data.AvenorStore
import com.example.avenor.data.Destination
import com.example.avenor.data.Flashcard
import com.example.avenor.domain.FocusEngine
import com.example.avenor.notify.Reminders
import com.example.avenor.ui.screens.FocusScreen
import com.example.avenor.ui.screens.FocusSessionScreen
import com.example.avenor.ui.screens.OnboardingScreen
import com.example.avenor.ui.screens.PlanScreen
import com.example.avenor.ui.screens.ReviewScreen
import com.example.avenor.ui.screens.ReviewSessionScreen
import com.example.avenor.ui.screens.TodayScreen
import com.example.avenor.ui.screens.YouScreen
import com.example.avenor.ui.theme.Av
import com.example.avenor.ui.theme.AvenorTheme

class MainActivity : ComponentActivity() {
    private lateinit var store: AvenorStore
    private lateinit var focus: FocusEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Av.Background.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Av.Background.toArgb()),
        )
        Reminders.ensureChannel(this)
        store = AvenorStore(applicationContext)
        focus = FocusEngine(applicationContext)
        store.resyncReminders()   // alarms don't survive a reboot; re-create them on launch
        setContent { AvenorTheme { AvenorRoot(store, focus) } }
    }

    override fun onStop() {
        super.onStop()
        if (::focus.isInitialized) focus.onAppBackgrounded(store.profile.nudgeWhenLeaving && store.profile.remindersEnabled)
    }

    override fun onStart() {
        super.onStart()
        if (::focus.isInitialized) focus.onAppForegrounded()
    }
}

/** Hick's Law: four destinations, each with one job. Jakob's Law: a standard Material bottom navigation bar. */
@Composable
fun AvenorRoot(store: AvenorStore, focus: FocusEngine) {
    val snackbar = remember { SnackbarHostState() }
    var showProfile by remember { mutableStateOf(false) }
    var reviewQueue by remember { mutableStateOf<List<Flashcard>?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        store.updateProfile { it.copy(remindersEnabled = granted) }
    }
    val requestReminders: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else store.updateProfile { it.copy(remindersEnabled = true) }
    }

    when {
        !store.profile.onboarded -> OnboardingScreen(store, requestReminders)
        focus.isActive -> FocusSessionScreen(store, focus)
        reviewQueue != null -> {
            BackHandler { reviewQueue = null }
            ReviewSessionScreen(store, reviewQueue ?: emptyList()) { reviewQueue = null }
        }
        showProfile -> {
            BackHandler { showProfile = false }
            YouScreen(store, requestReminders) { showProfile = false }
        }
        else -> Scaffold(
            containerColor = Av.Background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = Av.Surface) {
                    Destination.entries.forEach { d ->
                        NavigationBarItem(
                            selected = store.tab == d,
                            onClick = { store.tab = d },
                            icon = {
                                val due = store.dueCards.size
                                if (d == Destination.REVIEW && due > 0) {
                                    BadgedBox(badge = { Badge { Text("$due") } }) { Icon(d.icon(), contentDescription = null) }
                                } else {
                                    Icon(d.icon(), contentDescription = null)
                                }
                            },
                            label = { Text(d.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black, indicatorColor = Av.Accent,
                                selectedTextColor = Av.Accent, unselectedIconColor = Av.Muted, unselectedTextColor = Av.Muted,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                when (store.tab) {
                    Destination.TODAY -> TodayScreen(store, focus) { showProfile = true }
                    Destination.PLAN -> PlanScreen(store, focus, snackbar)
                    Destination.FOCUS -> FocusScreen(store, focus)
                    Destination.REVIEW -> ReviewScreen(store) { reviewQueue = store.dueCards.take(20) }
                }
            }
        }
    }
}

private fun Destination.icon(): ImageVector = when (this) {
    Destination.TODAY -> Icons.Filled.WbSunny
    Destination.PLAN -> Icons.Filled.Checklist
    Destination.FOCUS -> Icons.Filled.Timer
    Destination.REVIEW -> Icons.Filled.Style
}
