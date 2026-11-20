package edu.module4

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
internal fun notificationAction(action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<() -> Unit>(1) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        val continuation = pending[0]
        pending[0] = null
        continuation?.invoke()
    }
    return {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (pending[0] == null) {
                pending[0] = action
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            action()
        }
    }
}
