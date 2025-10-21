package com.example.jasmineplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jasmineplayer.ui.theme.JasminePlayerTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private lateinit var playerViewModel: PlayerViewModel
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JasminePlayerTheme {
                playerViewModel = viewModel()
                val songViewModel: SongViewModel = viewModel()
                val folderViewModel: FolderViewModel = viewModel()
                val favoritesViewModel: FavoritesViewModel = viewModel()
                val settingsViewModel: SettingsViewModel = viewModel()

                navController = rememberNavController()

                val currentSong by playerViewModel.currentSong.collectAsState()
                val currentLyrics by playerViewModel.currentLyrics.collectAsState()

                val songs by songViewModel.songs.collectAsState()
                val songsLoaded by songViewModel.songsLoaded.collectAsState()
                val favoriteSongs by favoritesViewModel.favoriteSongs.collectAsState()

                val context = LocalContext.current
                var hasPermissions by remember {
                    mutableStateOf(permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
                }

                val requestPermissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    hasPermissions = permissions.values.all { it }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val currentScreen = Screen.fromRoute(currentDestination?.route)

                val songSortType by songViewModel.sortType.collectAsState()
                val songSortAscending by songViewModel.sortAscending.collectAsState()
                val folderSortType by folderViewModel.sortType.collectAsState()
                val folderSortAscending by folderViewModel.sortAscending.collectAsState()
                val favSortType by favoritesViewModel.sortType.collectAsState()
                val favSortAscending by favoritesViewModel.sortAscending.collectAsState()

                var showQueueBottomSheet by remember { mutableStateOf(false) }
                val queueSheetState = rememberModalBottomSheetState()
                var showLyricsBottomSheet by remember { mutableStateOf(false) }
                val lyricsSheetState = rememberModalBottomSheetState()
                val scope = rememberCoroutineScope()
                val haptic = LocalHapticFeedback.current

                var isSearchActive by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }

                LaunchedEffect(searchQuery, currentScreen, navBackStackEntry) {
                    val folderPath = navBackStackEntry?.arguments?.getString("folderPath")?.let {
                        URLDecoder.decode(it, java.nio.charset.StandardCharsets.UTF_8.toString())
                    }

                    when (currentScreen) {
                        Screen.SongList -> songViewModel.loadSongs(folderPath = folderPath, searchQuery = searchQuery)
                        Screen.Favorites -> favoritesViewModel.loadFavorites(searchQuery = searchQuery)
                        Screen.Folders -> folderViewModel.loadFolders(searchQuery = searchQuery)
                        else -> {}
                    }
                }

                Scaffold(
                    topBar = {
                        if (currentScreen != Screen.Player) {
                            TopAppBar(
                                title = {
                                    if (isSearchActive) {
                                        TextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = { Text("Search...") }
                                        )
                                    } else {
                                        Text(currentScreen.title)
                                    }
                                },
                                navigationIcon = {
                                     if (isSearchActive) {
                                        IconButton(onClick = { 
                                            isSearchActive = false
                                            searchQuery = ""
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    } else if (navController.previousBackStackEntry != null) {
                                        IconButton(onClick = { navController.navigateUp() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    }
                                },
                                actions = {
                                    if (currentScreen != Screen.Settings) {
                                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                                        }
                                    }

                                    if (!isSearchActive) {
                                        IconButton(onClick = { isSearchActive = true }) {
                                            Icon(Icons.Default.Search, contentDescription = "Search")
                                        }
                                    }
                                    when (currentScreen) {
                                        Screen.SongList -> {
                                            var menuExpanded by remember { mutableStateOf(false) }

                                            IconButton(onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                songViewModel.toggleSortOrder()
                                            }) {
                                                Icon(
                                                    if (songSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = "Sort Direction"
                                                )
                                            }

                                            Box {
                                                IconButton(onClick = { menuExpanded = true }) {
                                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort By")
                                                }
                                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                                    DropdownMenuItem(
                                                        text = { Text("By Name") },
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            songViewModel.changeSortType(SortType.NAME); menuExpanded = false
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("By Date Added") },
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            songViewModel.changeSortType(SortType.DATE_ADDED); menuExpanded = false
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("By Date Modified") },
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            songViewModel.changeSortType(SortType.DATE_MODIFIED); menuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Screen.Favorites -> {
                                            var menuExpanded by remember { mutableStateOf(false) }

                                            IconButton(onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                favoritesViewModel.toggleSortOrder()
                                            }) {
                                                Icon(
                                                    if (favSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = "Sort Direction"
                                                )
                                            }

                                            Box {
                                                IconButton(onClick = { menuExpanded = true }) {
                                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort By")
                                                }
                                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                                    DropdownMenuItem(
                                                        text = { Text("By Name") },
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            favoritesViewModel.changeSortType(SortType.NAME); menuExpanded = false
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("By Date Added") },
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            favoritesViewModel.changeSortType(SortType.DATE_ADDED); menuExpanded = false
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("By Date Modified") },
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            favoritesViewModel.changeSortType(SortType.DATE_MODIFIED); menuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Screen.Folders -> {
                                            var menuExpanded by remember { mutableStateOf(false) }

                                            IconButton(onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                folderViewModel.toggleSortOrder()
                                            }) {
                                                Icon(
                                                    if (folderSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = "Sort Direction"
                                                )
                                            }

                                            Box {
                                                IconButton(onClick = { menuExpanded = true }) {
                                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort By")
                                                }
                                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                                    DropdownMenuItem(
                                                        text = { Text("By Name") },
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            folderViewModel.changeSortType(FolderSortType.NAME); menuExpanded = false
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("By Song Count") },
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            folderViewModel.changeSortType(FolderSortType.SONG_COUNT); menuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (currentScreen != Screen.Player) {
                            BottomNavigationBar(
                                currentScreen = currentScreen,
                                onScreenSelected = { screen ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (hasPermissions) {
                                NavHost(navController, startDestination = Screen.SongList.route) {
                                    composable(Screen.Folders.route) {
                                        FolderScreen(folderViewModel = folderViewModel, onFolderClick = { folder ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            navController.navigate(Screen.SongList.createRoute(folder.path))
                                        })
                                    }
                                    composable(
                                        route = "song_list?folderPath={folderPath}",
                                        arguments = listOf(navArgument("folderPath") { type = NavType.StringType; nullable = true })
                                    ) {
                                        SongListScreen(
                                            songViewModel = songViewModel,
                                            playerViewModel = playerViewModel,
                                            onSongClick = { song ->
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                playerViewModel.play(songs, songs.indexOf(song))
                                                startService(Intent(this@MainActivity, PlayerService::class.java))
                                                navController.navigate(Screen.Player.route)
                                            }
                                        )
                                    }
                                    composable(Screen.Favorites.route) {
                                        FavoritesScreen(
                                            favoritesViewModel = favoritesViewModel,
                                            playerViewModel = playerViewModel,
                                            onSongClick = { song ->
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                playerViewModel.play(favoriteSongs, favoriteSongs.indexOf(song))
                                                startService(Intent(this@MainActivity, PlayerService::class.java))
                                                navController.navigate(Screen.Player.route)
                                            }
                                        )
                                    }
                                    composable(Screen.Player.route) {
                                        PlayerScreen(
                                            playerViewModel = playerViewModel,
                                            onDismiss = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                navController.popBackStack()
                                            },
                                            onShowQueue = { showQueueBottomSheet = true },
                                            onShowLyrics = { showLyricsBottomSheet = true }
                                        )
                                    }
                                    composable(Screen.Settings.route) {
                                        SettingsScreen()
                                    }
                                }

                                LaunchedEffect(Unit) {
                                    snapshotFlow { intent }
                                        .filter { it?.action == Intent.ACTION_VIEW && it.data != null }
                                        .collect { intent ->
                                            snapshotFlow { songsLoaded }
                                                .filter { it }
                                                .collect {
                                                    handleIntent(intent)
                                                }
                                        }
                                }

                            } else {
                                // Request permissions if not granted
                                LaunchedEffect(Unit) {
                                    requestPermissionsLauncher.launch(permissionsToRequest)
                                }

                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Для доступа к песням и отображения уведомлений требуются разрешения.")
                                    Button(onClick = { requestPermissionsLauncher.launch(permissionsToRequest) }) {
                                        Text("Запросить разрешения")
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = currentScreen != Screen.Player && currentSong != null,
                                enter = fadeIn(animationSpec = tween(300)),
                                exit = fadeOut(animationSpec = tween(300)),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                LargeFloatingActionButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        navController.navigate(Screen.Player.route)
                                    },
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = "Now Playing")
                                }
                            }

                            if (showQueueBottomSheet) {
                                ModalBottomSheet(
                                    onDismissRequest = { showQueueBottomSheet = false },
                                    sheetState = queueSheetState
                                ) {
                                    QueueScreen(playerViewModel = playerViewModel, onBack = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch { queueSheetState.hide() }.invokeOnCompletion {
                                            if (!queueSheetState.isVisible) {
                                                showQueueBottomSheet = false
                                            }
                                        }
                                    })
                                }
                            }

                            if (showLyricsBottomSheet) {
                                ModalBottomSheet(
                                    onDismissRequest = { showLyricsBottomSheet = false },
                                    sheetState = lyricsSheetState
                                ) {
                                    LyricsScreen(
                                        lyrics = currentLyrics,
                                        songTitle = playerViewModel.currentSongTitle.collectAsState().value,
                                        onBack = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            scope.launch { lyricsSheetState.hide() }.invokeOnCompletion {
                                                if (!lyricsSheetState.isVisible) {
                                                    showLyricsBottomSheet = false
                                                }
											}
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val songUri = intent.data
            if (songUri != null) {
                if (::playerViewModel.isInitialized) {
                    playerViewModel.playSongFromUri(songUri)
                    startService(Intent(this@MainActivity, PlayerService::class.java))
                    if (::navController.isInitialized) {
                        navController.navigate(Screen.Player.route)
                    }
                    intent.data = null
                }
            }
        }
    }
}
