package online.mpscan.app.ui.theme

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import online.mpscan.app.data.AppearanceColors
import online.mpscan.app.ui.MpImage
import java.io.File

private val LocalPhotoBackground=staticCompositionLocalOf{false}
val MpBackground: Color @Composable get() = if(LocalPhotoBackground.current)Color.Transparent else MaterialTheme.colorScheme.background
val MpSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val MpSurface2: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val MpLine: Color @Composable get() = MaterialTheme.colorScheme.outline
val MpText: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val MpMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val MpAccent: Color @Composable get() = MaterialTheme.colorScheme.primary
val MpAccent2: Color @Composable get() = MaterialTheme.colorScheme.secondary
private fun color(value:String)=Color(android.graphics.Color.parseColor(value))
private fun onColor(value:String)=if(AppearanceColors.darkText(value))Color(0xff18131f)else Color.White

@Composable fun MpScanTheme(content: @Composable () -> Unit) {
 val prefs=LocalContext.current.getSharedPreferences("mp_scan_settings",0)
 var revision by remember{mutableIntStateOf(0)}
 DisposableEffect(prefs){val listener=SharedPreferences.OnSharedPreferenceChangeListener{_,_->revision++};prefs.registerOnSharedPreferenceChangeListener(listener);onDispose{prefs.unregisterOnSharedPreferenceChangeListener(listener)}}
 val mode=remember(revision){prefs.getString("theme","system")}
 val dark=when(mode){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
 val base=if(dark)darkColorScheme(primary=Color(0xFFA98AFF),secondary=Color(0xFFFF94C2),background=Color(0xFF151517),surface=Color(0xFF202023),surfaceVariant=Color(0xFF2A2A2E),outline=Color(0xFF3A3A40),onBackground=Color(0xFFF5F4F7),onSurface=Color(0xFFF5F4F7),onSurfaceVariant=Color(0xFFB8B5C0))
 else lightColorScheme(primary=Color(0xFF6F43CA),secondary=Color(0xFFA52969),background=Color(0xFFF9F7FC),surface=Color.White,surfaceVariant=Color(0xFFEFEBF4),outline=Color(0xFFDBD5E3),onBackground=Color(0xFF241E2D),onSurface=Color(0xFF241E2D),onSurfaceVariant=Color(0xFF6C6378))
 val accent=remember(revision){prefs.getString("accent",null)?.takeIf(AppearanceColors::valid)}
 val panel=remember(revision){prefs.getString("panel_color",null)?.takeIf(AppearanceColors::valid)}
 var scheme=base
 if(accent!=null)scheme=scheme.copy(primary=color(accent),onPrimary=onColor(accent),secondary=color(accent),onSecondary=onColor(accent))
 if(panel!=null){val foreground=onColor(panel);scheme=scheme.copy(background=color(panel),onBackground=foreground,surface=color(panel),surfaceVariant=color(panel),surfaceContainer=color(panel),surfaceContainerHigh=color(panel),surfaceContainerHighest=color(panel),onSurface=foreground,onSurfaceVariant=foreground.copy(.74f),outline=foreground.copy(.18f))}
 val path=remember(revision){prefs.getString("app_wallpaper","").orEmpty()}
 val photo=path.isNotBlank()&&File(path).isFile
 val dim=remember(revision){prefs.getFloat("wallpaper_dim",.8f).coerceIn(.35f,.95f)}
 MaterialTheme(colorScheme=scheme){
  Box(Modifier.fillMaxSize().background(scheme.background)){
   if(photo){MpImage(File(path),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop);Box(Modifier.matchParentSize().background(scheme.background.copy(dim)))}
   CompositionLocalProvider(LocalPhotoBackground provides photo){content()}
  }
 }
}
