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
import online.mpscan.app.ui.PartnerShowcase
import online.mpscan.app.ui.PartnerHero
import online.mpscan.app.ui.HostingTheme
import online.mpscan.app.ui.WorkOriginBadge
import org.json.JSONObject
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
 @Test fun hostingFollowsTheSitePaletteAndShowsOrigin(){
  val context=ApplicationProvider.getApplicationContext<Context>();var paper by mutableStateOf(false)
  compose.setContent{HostingTheme(JSONObject().put("themePreset",if(paper)"paper"else"garden").put("scanName","Hospedagem Teste").put("description","As obras e a equipe, na mesma vitrine.").put("heroStyle","centered")){Column{PartnerHero(JSONObject().put("scanName","Hospedagem Teste"),false,4);WorkOriginBadge(Work("test","Obra","","","","","","",emptyList(),0,0,originKind="hosting",originName="Equipe parceira"))}}}
  compose.onNodeWithText("Hospedagem Teste").assertIsDisplayed();compose.onNodeWithText("⌂ Hospedagem · Equipe parceira").assertIsDisplayed();save(context,"hospedagem-jardim.png")
  compose.runOnIdle{paper=true};compose.waitForIdle();compose.onNodeWithText("Hospedagem Teste").assertIsDisplayed();save(context,"hospedagem-paginas.png")
 }
 @Test fun singleHostedWorkUsesCompactCoverAndTheSameVitrineAsDonations(){
  val context=ApplicationProvider.getApplicationContext<Context>();var donation by mutableStateOf(false);var clicks=0
  val hosted=Work("hosted","Uma história","","","","","","",emptyList(),0,0,scanOwnerUid="owner",partnerOnly=true)
  val scan=JSONObject().put("scanName","Lunaris scan").put("ownerUid","owner").put("donatedWorkIds",org.json.JSONArray().put("hosted"))
  compose.setContent{MpScanTheme{Column(Modifier.fillMaxWidth().padding(18.dp)){PartnerShowcase(scan,donation,listOf(hosted.copy(partnerOnly=!donation))){clicks++}}}}
  compose.onNodeWithText("1 obra").assertIsDisplayed()
  val cover=compose.onNodeWithContentDescription("Uma história",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
  val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
  assertTrue("A single cover must not stretch across the card",cover.width<root.width/2)
  compose.onNodeWithText("Conhecer esta hospedagem →").performClick();compose.runOnIdle{assertEquals(1,clicks)};save(context,"hospedagem-vitrine-celular.png")
  compose.runOnIdle{donation=true};compose.waitForIdle()
  compose.onNodeWithText("Explorar obras doadas →").assertIsDisplayed();save(context,"doacao-vitrine-celular.png")
 }
 private fun save(context:Context,name:String){
  val bitmap=compose.onRoot().captureToImage().asAndroidBitmap();val dir=File(context.getExternalFilesDir(null),"ui-checks").apply{mkdirs()}
  val file=File(dir,name)
  file.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
  listOf("mkdir -p /sdcard/Download/mpscan-ui-checks", "cp ${file.absolutePath} /sdcard/Download/mpscan-ui-checks/$name").forEach { command ->
   android.os.ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).use { it.readBytes() }
  }
 }
}
