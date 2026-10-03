package online.mpscan.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import online.mpscan.app.data.SettingsStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

private var light by mutableStateOf(false)
private var rose by mutableStateOf(false)
val MpBackground get()=if(light)Color(0xFFF5F4FA)else Color(0xFF120E1D)
val MpSurface get()=if(light)Color.White else Color(0xFF1B1529)
val MpSurface2 get()=if(light)Color(0xFFEAE7F2)else Color(0xFF251D35)
val MpLine get()=if(light)Color(0xFFD8D3E4)else Color(0xFF3A2D4D)
val MpText get()=if(light)Color(0xFF211D30)else Color(0xFFF5F5F7)
val MpMuted get()=if(light)Color(0xFF676176)else Color(0xFFA8A8B3)
val MpAccent get()=if(rose)Color(0xFFBA356D)else Color(0xFF7B4DFF)
val MpAccent2 get()=if(light)MpAccent else Color(0xFFFF5AA5)
@Composable fun MpScanTheme(content:@Composable ()->Unit){
 val context=LocalContext.current;val store=remember{SettingsStore(context)}
 var revision by remember{mutableIntStateOf(0)}
 DisposableEffect(store){val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener{_,_->revision++};store.prefs.registerOnSharedPreferenceChangeListener(listener);onDispose{store.prefs.unregisterOnSharedPreferenceChangeListener(listener)}}
 val currentLight=remember(revision){store.lightTheme};val currentRose=remember(revision){store.roseAccent}
 SideEffect{light=currentLight;rose=currentRose}
 val colors=if(light)lightColorScheme(primary=MpAccent,onPrimary=Color.White,secondary=MpAccent2,onSecondary=Color(0xFF25102E),background=MpBackground,surface=MpSurface,surfaceVariant=MpSurface2,outline=MpLine,onBackground=MpText,onSurface=MpText)else darkColorScheme(primary=MpAccent,onPrimary=Color.White,secondary=MpAccent2,onSecondary=Color(0xFF25102E),background=MpBackground,surface=MpSurface,surfaceVariant=MpSurface2,outline=MpLine,onBackground=MpText,onSurface=MpText)
 val base=Typography()
 val typography=Typography(displaySmall=base.displaySmall.copy(fontFamily=androidx.compose.ui.text.font.FontFamily.SansSerif,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold),headlineLarge=base.headlineLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,letterSpacing=(-.6).sp),headlineMedium=base.headlineMedium.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold),headlineSmall=base.headlineSmall.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold),titleLarge=base.titleLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold),bodyLarge=base.bodyLarge,bodyMedium=base.bodyMedium,labelLarge=base.labelLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold))
 MaterialTheme(colorScheme=colors,typography=typography,content=content)
}
