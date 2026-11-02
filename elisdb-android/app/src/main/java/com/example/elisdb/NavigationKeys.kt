package com.example.elisdb

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object ClientList : NavKey

@Serializable data class ClientForm(val clienteId: String? = null) : NavKey

@Serializable data object ClientMap : NavKey

@Serializable data class ClientDetail(val clienteId: String) : NavKey

@Serializable data class ClientLog(val clienteId: String) : NavKey

@Serializable data class PhotoViewer(val clienteId: String, val indiceFoto: Int) : NavKey

@Serializable data class MapFullscreen(val clienteId: String) : NavKey

@Serializable data object Config : NavKey

@Serializable data object About : NavKey