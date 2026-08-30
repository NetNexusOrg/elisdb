package com.example.elisdb.ui.form

import android.content.Context
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun CameraCapture(
  createOutputFile: () -> File,
  onPhotoCaptured: (File) -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val previewView = remember { PreviewView(context) }
  var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val imageCapture =
    remember {
      ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setJpegQuality(80)
        .build()
    }

  LaunchedEffect(lifecycleOwner) {
    val provider = context.getCameraProvider()
    cameraProvider = provider
    val preview =
      Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
    imageCapture.targetRotation =
      previewView.display?.rotation ?: Surface.ROTATION_0
    runCatching {
      provider.unbindAll()
      provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
    }.onFailure { errorMessage = "Não foi possível abrir a câmara." }
  }

  DisposableEffect(Unit) {
    onDispose { cameraProvider?.unbindAll() }
  }

  Box(modifier.fillMaxSize()) {
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    Button(
      onClick = {
        val outputFile = createOutputFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        imageCapture.takePicture(
          outputOptions,
          ContextCompat.getMainExecutor(context),
          object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
              onPhotoCaptured(outputFile)
            }

            override fun onError(exception: ImageCaptureException) {
              scope.launch { errorMessage = "Não foi possível capturar a foto." }
            }
          },
        )
      },
      modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
    ) {
      Text("Tirar foto")
    }

    Button(
      onClick = onCancel,
      modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
    ) {
      Text("Cancelar")
    }

    errorMessage?.let { Text(it, modifier = Modifier.align(Alignment.Center).padding(16.dp)) }
  }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
  suspendCancellableCoroutine { continuation ->
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener(
      {
        if (continuation.isActive) {
          runCatching { continuation.resume(future.get()) }
            .onFailure { continuation.resumeWithException(it) }
        }
      },
      ContextCompat.getMainExecutor(this),
    )
  }
