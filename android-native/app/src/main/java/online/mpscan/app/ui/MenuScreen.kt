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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable fun MenuScreen(works:List<Work>,openWork:(Work)->Unit,settings:()->Unit){
 var page by remember{mutableStateOf("Menu")}
 val uri=LocalUriHandler.current
 if(page=="Parcerias scan"){ScanPartnerships(openWork){page="Menu"};return}
 if(page=="Personalizar"){LockSettings{page="Menu"};return}
 LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)){
  item{if(page!="Menu")TextButton({page="Menu"}){Text("← Menu")};Text(page,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium);Text("Tudo para a sua experiência na MP SCAN",color=MpMuted,modifier=Modifier.padding(top=8.dp,bottom=12.dp))}
  if(page=="Menu"){
   item{MenuTile("◇","Parcerias scan","Catálogos externos e leitura offline"){page="Parcerias scan"}}
   item{MenuTile("♡","Parceiros","Hospedagem, obras doadas e como hospedar"){page="Parceiros"}}
   item{MenuTile("◈","Personalizar","Sua tela de bloqueio, senha e foto"){page="Personalizar"}}
   item{MenuTile("⚙","Ajustes","Conta, notificações e leitura",settings)}
   item{SupportCard()}
  }else{
   item{PartnerList("partnerScans","Hospedagens",works,openWork)}
   item{PartnerList("donationScans","Obras doadas",works,openWork)}
   item{MenuTile("＋","Hospedar com a gente","Conheça a parceria e como solicitar"){uri.openUri("https://www.mpscan.online/#/parceiros/hospedar")}}
  }
 }
}
@Composable private fun MenuTile(icon:String,title:String,subtitle:String,open:()->Unit){Surface(Modifier.fillMaxWidth().clickable(onClick=open),shape=RoundedCornerShape(22.dp),color=MpSurface,border=BorderStroke(1.dp,MpLine)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,color=MpAccent2,style=MaterialTheme.typography.headlineSmall);Column(Modifier.weight(1f).padding(horizontal=14.dp)){Text(title,fontWeight=FontWeight.Bold);Text(subtitle,color=MpMuted,style=MaterialTheme.typography.bodySmall)};Text("›",color=MpMuted)}}}
internal suspend fun publicJson(path:String,token:String=""):JSONObject=withContext(Dispatchers.IO){val auth=if(token.isBlank())""else "?auth="+java.net.URLEncoder.encode(token,"UTF-8");AccountRepository().request(if(token.isBlank())SiteAccess.authenticated("https://nnnsss-23f2f-default-rtdb.firebaseio.com/$path.json")else "https://nnnsss-23f2f-default-rtdb.firebaseio.com/$path.json$auth")}
@Composable private fun PartnerList(path:String,title:String,works:List<Work>,openWork:(Work)->Unit){
 var entries by remember{mutableStateOf<List<Pair<String,JSONObject>>>(emptyList())};var error by remember{mutableStateOf("")};var loading by remember{mutableStateOf(true)};var query by remember{mutableStateOf("")};var retry by remember{mutableIntStateOf(0)}
 val online=networkAvailable()
 val directory by PartnerDirectory.entries.collectAsState()
 var selected by remember{mutableStateOf<Pair<String,JSONObject>?>(null)}
 selected?.let{PartnerDetails(it.first,it.second,path=="donationScans",works,{selected=null},openWork)}
 LaunchedEffect(path,directory){if(entries.isEmpty())entries=directory.filter{it.donation==(path=="donationScans")}.map{it.id to it.scan}}
 LaunchedEffect(path,retry,online){
  if(!online){loading=false;error=if(entries.isEmpty())"Você está offline. Seus capítulos baixados estão na Biblioteca."else"Mostrando as informações salvas. Atualizaremos quando a conexão voltar.";return@LaunchedEffect}
  loading=entries.isEmpty()
  while(true){
   try{val root=publicJson(path);entries=root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.takeIf{PartnerPresentation.visible(it,path=="donationScans")}?.let{id to it}}.toList();error=""}
   catch(e:CancellationException){throw e}catch(e:Exception){error="Não foi possível atualizar os parceiros. Tente novamente."}
   loading=false;delay(20000)
  }
 }
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);if(loading)LinearProgressIndicator(Modifier.fillMaxWidth());if(error.isNotBlank())Text(error,color=MpMuted);if(!loading&&error.isBlank()&&entries.isEmpty())Text("Nenhuma scan disponível nesta seção.",color=MpMuted);OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("Buscar scan ou história")},singleLine=true,shape=RoundedCornerShape(18.dp));if(error.isNotBlank())TextButton({retry++}){Text("Tentar novamente")};entries.filter{(_,scan)->query.isBlank()||scan.optString("scanName").contains(query,true)||scan.optString("description").contains(query,true)||PartnerPresentation.members(scan,path=="donationScans",works).any{it.title.contains(query,true)}}.forEach{(id,scan)->PartnerShowcase(scan,path=="donationScans",works){selected=id to scan}}}

}
@Composable fun WorkCredits(workId:String,openWork:(Work)->Unit){
 val context=LocalContext.current;val uri=LocalUriHandler.current
 var selectedPartner by remember{mutableStateOf<Pair<String,JSONObject>?>(null)}
 selectedPartner?.let{PartnerDetails(it.first,it.second,true,emptyList(),{selectedPartner=null},openWork)}
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
  requested.forEach{(id,person)->CreditTile("Obra pedida por",person.optString("nome","Leitor MP SCAN"),person.optString("foto"),person.optString("nomeUsuario")){uri.openUri("https://www.mpscan.online/#/perfil/$id")}}
 }
}
@Composable private fun CreditTile(label:String,name:String,photo:String,handle:String,open:()->Unit){Surface(Modifier.fillMaxWidth().clickable(onClick=open),shape=RoundedCornerShape(20.dp),color=MpSurface,border=BorderStroke(1.dp,MpAccent.copy(.3f))){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){if(photo.isNotBlank())MpImage(photo,name,Modifier.size(46.dp).clip(RoundedCornerShape(23.dp)),contentScale=ContentScale.Crop)else Text(name.take(1),color=MpAccent2,fontWeight=FontWeight.Black);Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(label,color=MpAccent2,style=MaterialTheme.typography.labelSmall);Text(name,fontWeight=FontWeight.Bold);if(handle.isNotBlank())Text("@"+handle.removePrefix("@"),color=MpMuted,style=MaterialTheme.typography.bodySmall)};Text("↗",color=MpMuted)}}}
