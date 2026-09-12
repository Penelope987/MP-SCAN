package online.mpscan.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MpBackground = Color(0xFF0B0B0D)
val MpSurface = Color(0xFF141419)
val MpSurface2 = Color(0xFF1B1B22)
val MpLine = Color(0xFF2A2A33)
val MpText = Color(0xFFF5F5F7)
val MpMuted = Color(0xFFA8A8B3)
val MpAccent = Color(0xFF7B4DFF)
val MpAccent2 = Color(0xFFFF5AA5)

@Composable fun MpScanTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(primary=MpAccent, secondary=MpAccent2, background=MpBackground, surface=MpSurface, surfaceVariant=MpSurface2, outline=MpLine, onBackground=MpText, onSurface=MpText),
    content = content
)
