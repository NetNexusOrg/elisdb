package com.example.elisdb.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.elisdb.AppContainer
import com.example.elisdb.ElisdbApp

@Composable
fun rememberAppContainer(): AppContainer {
  val context = LocalContext.current
  return remember {
    val appContext = context.applicationContext
    check(appContext is ElisdbApp) { "Application is not ElisdbApp" }
    appContext.container
  }
}
