package online.mpscan.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import online.mpscan.app.data.NewChapterWorker
import online.mpscan.app.data.SettingsStore

@Composable fun FirstNotificationPermission(){
 val context=LocalContext.current
 val prefs=remember{context.getSharedPreferences("mp_notification_prompt",0)}
 var offer by remember{mutableStateOf(!prefs.getBoolean("offered",false)&&Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->SettingsStore(context).notifications=granted;if(granted)NewChapterWorker.runNow(context)}
 if(offer)AlertDialog(onDismissRequest={offer=false;prefs.edit().putBoolean("offered",true).apply()},title={Text("Suas histórias, no momento certo")},text={Text("Quer receber avisos dos capítulos que você acompanha? Você pode escolher agora e mudar depois nos ajustes.")},confirmButton={Button({offer=false;prefs.edit().putBoolean("offered",true).apply();permission.launch(Manifest.permission.POST_NOTIFICATIONS)}){Text("Permitir avisos")}},dismissButton={TextButton({offer=false;prefs.edit().putBoolean("offered",true).apply();SettingsStore(context).notifications=false}){Text("Agora não")}})
}
