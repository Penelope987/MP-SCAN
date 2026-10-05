package online.mpscan.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import online.mpscan.app.data.*
import online.mpscan.app.ui.PartnerVitrine
import online.mpscan.app.ui.UserIdentityCard
import online.mpscan.app.ui.theme.MpScanTheme
import org.junit.*
import org.junit.Assert.*
import java.io.File

class PartnershipVisualTest {
 @get:Rule val compose=createComposeRule()
 @Test fun phoneAndTabletCardsRespectCustomThemeAndRemainClickable(){
  val context=ApplicationProvider.getApplicationContext<Context>();val prefs=context.getSharedPreferences("mp_scan_settings",0)
  val old=prefs.all;var tablet by mutableStateOf(false);var clicks=0
  prefs.edit().putString("theme","light").putString("accent","#245C48").apply()
  try{
   compose.setContent{val base=LocalDensity.current;CompositionLocalProvider(LocalDensity provides Density(if(tablet)base.density*.45f else base.density,1f)){MpScanTheme{
    LazyVerticalGrid(GridCells.Adaptive(280.dp),Modifier.fillMaxSize().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
     items(3){i->PartnerVitrine(ScanPartnership("scan$i","Equipe parceira ${i+1}","https://scan.example/",description="Um catálogo organizado para descobrir sua próxima história.",draft=i==0)){clicks++}}
    }
   }}}
   compose.onNodeWithText("Equipe parceira 1").performClick();assertEquals(1,clicks)
   compose.onNodeWithText("RASCUNHO · SÓ VOCÊ").assertIsDisplayed()
   save(context,"parcerias-celular-claro.png")
   compose.runOnIdle{tablet=true;prefs.edit().putString("theme","dark").putString("accent","#FFD578").apply()}
   compose.waitForIdle();compose.onNodeWithText("Equipe parceira 3").assertIsDisplayed();save(context,"parcerias-tablet-escuro.png")
  }finally{val editor=prefs.edit().clear();old.forEach{(k,v)->when(v){is String->editor.putString(k,v);is Boolean->editor.putBoolean(k,v);is Float->editor.putFloat(k,v);is Int->editor.putInt(k,v);is Long->editor.putLong(k,v)}};editor.apply()}
 }
 @Test fun userIdentityShowsNameHandleAndOpensTheSelectedPerson(){
  var clicked="";val person=ProfilePerson("user-id","Penélope","penelope","")
  compose.setContent{MpScanTheme{UserIdentityCard(person){clicked=person.uid}}}
  compose.onNodeWithText("Penélope").assertIsDisplayed();compose.onNodeWithText("@penelope").assertIsDisplayed().performClick();assertEquals("user-id",clicked)
 }
 private fun save(context:Context,name:String){
  val bitmap=compose.onRoot().captureToImage().asAndroidBitmap();val dir=File(context.getExternalFilesDir(null),"ui-checks").apply{mkdirs()}
  val file=File(dir,name)
  file.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
  InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("mkdir -p /sdcard/Download/mpscan-ui-checks && cp ${file.absolutePath} /sdcard/Download/mpscan-ui-checks/$name").use { descriptor -> java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
 }
}
