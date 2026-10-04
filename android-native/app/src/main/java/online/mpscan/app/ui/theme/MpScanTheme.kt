package online.mpscan.app.ui.theme

import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val MpBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val MpSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val MpSurface2: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val MpLine: Color @Composable get() = MaterialTheme.colorScheme.outline
val MpText: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val MpMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val MpAccent: Color @Composable get() = MaterialTheme.colorScheme.primary
val MpAccent2: Color @Composable get() = MaterialTheme.colorScheme.secondary

@Composable fun MpScanTheme(content: @Composable () -> Unit) {
 val prefs=LocalContext.current.getSharedPreferences("mp_scan_settings",0)
 var mode by remember { mutableStateOf(prefs.getString("theme","system")) }
 DisposableEffect(prefs){val listener=SharedPreferences.OnSharedPreferenceChangeListener{p,k->if(k=="theme")mode=p.getString("theme","system")};prefs.registerOnSharedPreferenceChangeListener(listener);onDispose{prefs.unregisterOnSharedPreferenceChangeListener(listener)}}
 val dark=when(mode){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
 val scheme=if(dark)darkColorScheme(primary=Color(0xFFA98AFF),secondary=Color(0xFFFF94C2),background=Color(0xFF151517),surface=Color(0xFF202023),surfaceVariant=Color(0xFF2A2A2E),outline=Color(0xFF3A3A40),onBackground=Color(0xFFF5F4F7),onSurface=Color(0xFFF5F4F7),onSurfaceVariant=Color(0xFFB8B5C0))
 else lightColorScheme(primary=Color(0xFF6F43CA),secondary=Color(0xFFA52969),background=Color(0xFFF9F7FC),surface=Color.White,surfaceVariant=Color(0xFFEFEBF4),outline=Color(0xFFDBD5E3),onBackground=Color(0xFF241E2D),onSurface=Color(0xFF241E2D),onSurfaceVariant=Color(0xFF6C6378))
 MaterialTheme(colorScheme=scheme,content=content)
}
