package com.example.elisdb.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

data class Localizacao(
  val latitude: Double,
  val longitude: Double,
)

class LocationService(context: Context) {
  private val appContext = context.applicationContext
  private val client = LocationServices.getFusedLocationProviderClient(context)

  suspend fun getCurrentLocation(): Localizacao? {
    val granted =
      ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED
    if (!granted) return null
    val fresh = withTimeoutOrNull(LOCATION_TIMEOUT_MS) { awaitCurrentLocation() }
    return fresh ?: lastKnownLocation()
  }

  @SuppressLint("MissingPermission")
  private suspend fun awaitCurrentLocation(): Localizacao? =
    suspendCancellableCoroutine { continuation ->
      client
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnCompleteListener { task ->
          if (!continuation.isActive) return@addOnCompleteListener
          val location = task.result
          continuation.resume(
            if (task.isSuccessful && location != null) {
              Localizacao(location.latitude, location.longitude)
            } else {
              null
            }
          )
        }
    }

  @SuppressLint("MissingPermission")
  private suspend fun lastKnownLocation(): Localizacao? =
    suspendCancellableCoroutine { continuation ->
      client.lastLocation.addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        val location = task.result
        continuation.resume(
          if (task.isSuccessful && location != null) {
            Localizacao(location.latitude, location.longitude)
          } else {
            null
          }
        )
      }
    }

  private companion object {
    const val LOCATION_TIMEOUT_MS = 10_000L
  }
}
