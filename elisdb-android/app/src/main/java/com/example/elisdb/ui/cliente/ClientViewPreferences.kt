package com.example.elisdb.ui.cliente

import android.content.Context
import android.content.SharedPreferences

/** Persisted list/map view choices. Search terms stay transient. */
interface ClientViewPreferences {
  fun clientListFilter(): ClientStatusFilter

  fun setClientListFilter(value: ClientStatusFilter)

  fun clientSortOrder(): ClientSortOrder

  fun setClientSortOrder(value: ClientSortOrder)

  fun clientMapFilter(): ClientMapFilter

  fun setClientMapFilter(value: ClientMapFilter)
}

/** Defaults used by tests and by call sites that do not persist preferences. */
object NoOpClientViewPreferences : ClientViewPreferences {
  override fun clientListFilter(): ClientStatusFilter = ClientStatusFilter.ALL

  override fun setClientListFilter(value: ClientStatusFilter) = Unit

  override fun clientSortOrder(): ClientSortOrder = ClientSortOrder.NAME

  override fun setClientSortOrder(value: ClientSortOrder) = Unit

  override fun clientMapFilter(): ClientMapFilter = ClientMapFilter.ACTIVE

  override fun setClientMapFilter(value: ClientMapFilter) = Unit
}

/** SharedPreferences-backed implementation for non-sensitive UI preferences. */
class PrefsClientViewPreferences(context: Context) : ClientViewPreferences {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun clientListFilter(): ClientStatusFilter =
    prefs.getString(KEY_LIST_FILTER, null).toListFilter()

  override fun setClientListFilter(value: ClientStatusFilter) {
    prefs.edit().putString(KEY_LIST_FILTER, value.name).apply()
  }

  override fun clientSortOrder(): ClientSortOrder =
    prefs.getString(KEY_SORT_ORDER, null).toSortOrder()

  override fun setClientSortOrder(value: ClientSortOrder) {
    prefs.edit().putString(KEY_SORT_ORDER, value.name).apply()
  }

  override fun clientMapFilter(): ClientMapFilter =
    prefs.getString(KEY_MAP_FILTER, null).toMapFilter()

  override fun setClientMapFilter(value: ClientMapFilter) {
    prefs.edit().putString(KEY_MAP_FILTER, value.name).apply()
  }

  private fun String?.toListFilter(): ClientStatusFilter =
    ClientStatusFilter.entries.firstOrNull { it.name == this } ?: ClientStatusFilter.ALL

  private fun String?.toSortOrder(): ClientSortOrder =
    ClientSortOrder.entries.firstOrNull { it.name == this } ?: ClientSortOrder.NAME

  private fun String?.toMapFilter(): ClientMapFilter =
    ClientMapFilter.entries.firstOrNull { it.name == this } ?: ClientMapFilter.ACTIVE

  private companion object {
    const val PREFS_NAME = "client_view_preferences"
    const val KEY_LIST_FILTER = "client_list_filter"
    const val KEY_SORT_ORDER = "client_sort_order"
    const val KEY_MAP_FILTER = "client_map_filter"
  }
}
