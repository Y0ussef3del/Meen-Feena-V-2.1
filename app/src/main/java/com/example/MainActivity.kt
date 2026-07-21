package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.example.game.audio.MysteryAudioPlayer
import com.example.game.data.CaseRepository
import com.example.game.viewmodel.GameViewModel
import com.example.ui.screens.GameNavigation
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // تفعيل EdgeToEdge الحديث من أندرويد
        enableEdgeToEdge()

        MobileAds.initialize(this) {}
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // تطبيق وضع ملء الشاشة (Immersive Mode) بالأسلوب الحديث والمتوافق مع SDK 35
        hideSystemUI()

        CaseRepository.init(this)

        try {
            val inputStream = resources.openRawResource(R.raw.cases)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            CaseRepository.loadCasesFromJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            MyApplicationTheme {
                val viewModel: GameViewModel = viewModel()
                val navController = rememberNavController()

                // نمرر التبطين لتأمين حواف الشاشة بالكامل
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameNavigation(
                        viewModel = viewModel,
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MysteryAudioPlayer.shutdown()
    }
}