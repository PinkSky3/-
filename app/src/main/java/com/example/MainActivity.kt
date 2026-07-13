package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DailyHotDashboard
import com.example.ui.screens.openExternalUrl
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GoldPriceViewModel
import com.example.ui.viewmodel.HotSearchViewModel
import com.example.ui.viewmodel.News60sViewModel
import com.example.ui.viewmodel.OilPriceViewModel
import com.example.ui.viewmodel.UpdateViewModel
import com.example.ui.viewmodel.WeatherAlertViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      var isDarkTheme by remember { mutableStateOf(false) }
      MyApplicationTheme(darkTheme = isDarkTheme) {
        val hotViewModel: HotSearchViewModel = viewModel()
        val oilViewModel: OilPriceViewModel = viewModel()
        val goldViewModel: GoldPriceViewModel = viewModel()
        val news60sViewModel: News60sViewModel = viewModel()
        val weatherAlertViewModel: WeatherAlertViewModel = viewModel()
        val updateViewModel: UpdateViewModel = viewModel()
        val availableUpdate by updateViewModel.availableUpdate.collectAsState()
        DailyHotDashboard(
          hotViewModel = hotViewModel,
          oilViewModel = oilViewModel,
          goldViewModel = goldViewModel,
          news60sViewModel = news60sViewModel,
          weatherAlertViewModel = weatherAlertViewModel,
          isDarkTheme = isDarkTheme,
          onToggleTheme = { isDarkTheme = !isDarkTheme },
          availableUpdate = availableUpdate,
          onDismissUpdate = updateViewModel::dismissUpdate,
          onDownloadUpdate = { url ->
            updateViewModel.dismissUpdate()
            openExternalUrl(this@MainActivity, url)
          },
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}
