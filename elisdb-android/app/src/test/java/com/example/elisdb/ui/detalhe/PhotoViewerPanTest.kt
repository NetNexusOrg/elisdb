package com.example.elisdb.ui.detalhe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoViewerPanTest {

  @Test
  fun pan_ampliado_deslocaEmPixeisDeEcra() {
    val result = panOffset(Offset.Zero, Offset(30f, 20f), scale = 3f, IntSize(1000, 1000))
    assertEquals(Offset(90f, 60f), result)
  }

  @Test
  fun pan_acumulaSobreOffsetExistente() {
    val result = panOffset(Offset(10f, 5f), Offset(30f, 20f), scale = 2f, IntSize(1000, 1000))
    assertEquals(Offset(70f, 45f), result)
  }

  @Test
  fun pan_limitaAoExcessoDaEscala() {
    val result = panOffset(Offset.Zero, Offset(5000f, 0f), scale = 2f, IntSize(1000, 1000))
    assertEquals(Offset(500f, 0f), result)
  }

  @Test
  fun pan_a1xMantemOffsetAZero() {
    val result = panOffset(Offset.Zero, Offset(30f, 20f), scale = 1f, IntSize(1000, 1000))
    assertEquals(Offset.Zero, result)
  }
}
