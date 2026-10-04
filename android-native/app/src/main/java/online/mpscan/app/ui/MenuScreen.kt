package online.mpscan.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.mpscan.app.ui.MpImage
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable fun MenuScreen(settings:()->Unit){
 var page by remember{mutableStateOf("Menu")}
 val uri=LocalUriHandler.current
 if(page=="Personalizar"){LockSettings{page="Menu"};return}
 LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)){
  item{if(page!="Menu")TextButton({page="Menu"}){Text("← Menu")};Text(page,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium);Text("Tudo para a sua experiência na MP SCAN",color=MpMuted,modifier=Modifier.padding(top=8.dp,bottom=12.dp))}
  if(page=="Menu"){
   item{MenuTile("♡","Parceiros","Hospedagem, obras doadas e como hospedar"){page="Parceiros"}}
   item{MenuTile("◈","Personalizar","Sua tela de bloqueio, senha e foto"){page="Personalizar"}}
   item{MenuTile("⚙","Ajustes","Conta, notificações e leitura",settings)}
   item{SupportCard()}
  }else{
   item{PartnerList("partnerScans","Hospedagem")}
   item{PartnerList("donationScans","Obras doadas")}
   item{MenuTile("＋","Hospedar com a gente","Confira as informações e envie seu pedido"){uri.openUri("https://www.mpscan.online/#/parceiros/hospedar")}}
  }
 }
}
@Composable private fun MenuTile(icon:String,title:String,subtitle:String,open:()->Unit){Surface(Modifier.fillMaxWidth().clickable(onClick=open),shape=RoundedCornerShape(22.dp),color=MpSurface,border=BorderStroke(1.dp,MpLine)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,color=MpAccent2,style=MaterialTheme.typography.headlineSmall);Column(Modifier.weight(1f).padding(horizontal=14.dp)){Text(title,fontWeight=FontWeight.Bold);Text(subtitle,color=MpMuted,style=MaterialTheme.typography.bodySmall)};Text("›",color=MpMuted)}}}
private suspend fun publicJson(path:String,token:String=""):JSONObject=withContext(Dispatchers.IO){val auth=if(token.isBlank())""else "?auth="+java.net.URLEncoder.encode(token,"UTF-8");AccountRepository().request("https://nnnsss-23f2f-default-rtdb.firebaseio.com/$path.json$auth")}
@Composable private fun PartnerList(path:String,title:String){
 var entries by remember{mutableStateOf<List<Pair<String,JSONObject>>>(emptyList())};var error by remember{mutableStateOf("")};var loading by remember{mutableStateOf(true)}
 val uri=LocalUriHandler.current
 LaunchedEffect(path){runCatching{publicJson(path)}.onSuccess{root->entries=root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.takeIf{it.optString("status")=="approved"&&!it.optBoolean("hostingPaused")}?.let{id to it}}.toList()}.onFailure{error="Não foi possível carregar. Confira os parceiros no site."};loading=false}
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);if(loading)LinearProgressIndicator(Modifier.fillMaxWidth());if(error.isNotBlank())Text(error,color=MpMuted);if(!loading&&error.isBlank()&&entries.isEmpty())Text("Nenhuma scan disponível nesta seção.",color=MpMuted);entries.forEach{(id,scan)->CreditTile(title,scan.optString("scanName","Scan"),scan.optString("photo",scan.optString("donorPhoto")),""){uri.openUri("https://www.mpscan.online/#/parceiros/"+(if(path=="donationScans")"doacoes/"else "scan/")+id)}}}
}
@Composable fun WorkCredits(workId:String){
 val context=LocalContext.current;val uri=LocalUriHandler.current
 var donations by remember(workId){mutableStateOf<List<Pair<String,JSONObject>>>(emptyList())};var requested by remember(workId){mutableStateOf<List<Pair<String,JSONObject>>>(emptyList())}
 LaunchedEffect(workId){
  runCatching{publicJson("donationScans")}.onSuccess{root->donations=root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.takeIf{scan->scan.optString("status")=="approved"&&(scan.optJSONArray("donatedWorkIds")?.let{ids->(0 until ids.length()).any{ids.optString(it)==workId}}==true)}?.let{id to it}}.toList()}
  runCatching{publicJson("workRequestCredits/$workId").takeIf{it.length()>0}?:publicJson("obras/$workId/requestedBy")}.onSuccess{root->
   val token=AccountStore(context).session()?.token.orEmpty()
   requested=root.keys().asSequence().map{id->id to (root.optJSONObject(id)?:JSONObject())}.toList().map{(id,credit)->
    val profile=if(token.isNotBlank())runCatching{publicJson("perfisPublicos/$id",token)}.getOrDefault(JSONObject())else JSONObject()
    listOf("nome","nomeUsuario","foto").forEach{key->if(profile.optString(key).isNotBlank())credit.put(key,profile.optString(key))};id to credit
   }
  }
 }
 Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  donations.forEach{(id,scan)->CreditTile("Obra doada",scan.optString("scanName","Scan doadora"),scan.optString("photo",scan.optString("donorPhoto")),""){uri.openUri("https://www.mpscan.online/#/parceiros/doacoes/$id")};if(scan.optString("donorUid").isNotBlank())CreditTile("Doada por",scan.optString("donorName","Leitor MP SCAN"),scan.optString("donorPhoto"),scan.optString("donorHandle")){uri.openUri("https://www.mpscan.online/#/perfil/"+scan.optString("donorUid"))}}
  requested.forEach{(id,person)->CreditTile("Obra pedida por",person.optString("nome","Leitor MP SCAN"),person.optString("foto"),person.optString("nomeUsuario")){uri.openUri("https://www.mpscan.online/#/perfil/$id")}}
 }
}
@Composable private fun CreditTile(label:String,name:String,photo:String,handle:String,open:()->Unit){Surface(Modifier.fillMaxWidth().clickable(onClick=open),shape=RoundedCornerShape(20.dp),color=MpSurface,border=BorderStroke(1.dp,MpAccent.copy(.3f))){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){if(photo.isNotBlank())MpImage(photo,name,Modifier.size(46.dp).clip(RoundedCornerShape(23.dp)),contentScale=ContentScale.Crop)else Text(name.take(1),color=MpAccent2,fontWeight=FontWeight.Black);Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(label,color=MpAccent2,style=MaterialTheme.typography.labelSmall);Text(name,fontWeight=FontWeight.Bold);if(handle.isNotBlank())Text("@"+handle.removePrefix("@"),color=MpMuted,style=MaterialTheme.typography.bodySmall)};Text("↗",color=MpMuted)}}}
