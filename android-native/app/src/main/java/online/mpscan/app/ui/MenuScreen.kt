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

@Composable fun MenuScreen(works:List<Work>,openWork:(Work)->Unit,settings:()->Unit){
 var hostingInfo by remember{mutableStateOf(false)}
 if(hostingInfo)AlertDialog(onDismissRequest={hostingInfo=false},title={Text("Sua scan na MP SCAN")},text={Text("A hospedagem reúne as obras da sua equipe em uma vitrine própria, com descrição, créditos e acesso às leituras. A administração avalia cada parceria. Para solicitar a inclusão da sua scan, fale com a equipe pelo canal de suporte oficial da MP SCAN e informe o nome, a descrição e as obras que deseja hospedar.")},confirmButton={TextButton({hostingInfo=false}){Text("Entendi")}})
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
   item{PartnerList("partnerScans","Hospedagem",works,openWork)}
   item{PartnerList("donationScans","Obras doadas",works,openWork)}
   item{MenuTile("＋","Hospedar com a gente","Conheça a parceria e como solicitar"){hostingInfo=true}}
  }
 }
}
@Composable private fun MenuTile(icon:String,title:String,subtitle:String,open:()->Unit){Surface(Modifier.fillMaxWidth().clickable(onClick=open),shape=RoundedCornerShape(22.dp),color=MpSurface,border=BorderStroke(1.dp,MpLine)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,color=MpAccent2,style=MaterialTheme.typography.headlineSmall);Column(Modifier.weight(1f).padding(horizontal=14.dp)){Text(title,fontWeight=FontWeight.Bold);Text(subtitle,color=MpMuted,style=MaterialTheme.typography.bodySmall)};Text("›",color=MpMuted)}}}
private suspend fun publicJson(path:String,token:String=""):JSONObject=withContext(Dispatchers.IO){val auth=if(token.isBlank())""else "?auth="+java.net.URLEncoder.encode(token,"UTF-8");AccountRepository().request("https://nnnsss-23f2f-default-rtdb.firebaseio.com/$path.json$auth")}
@Composable private fun PartnerList(path:String,title:String,works:List<Work>,openWork:(Work)->Unit){
 var entries by remember{mutableStateOf<List<Pair<String,JSONObject>>>(emptyList())};var error by remember{mutableStateOf("")};var loading by remember{mutableStateOf(true)}
 var selected by remember{mutableStateOf<Pair<String,JSONObject>?>(null)}
 selected?.let{PartnerDetails(it.first,it.second,path=="donationScans",works,{selected=null},openWork)}
 LaunchedEffect(path){runCatching{publicJson(path)}.onSuccess{root->entries=root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.takeIf{it.optString("status")=="approved"&&!it.optBoolean("hostingPaused")}?.let{id to it}}.toList()}.onFailure{error="Não foi possível carregar os parceiros. Confira sua conexão."};loading=false}
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);if(loading)LinearProgressIndicator(Modifier.fillMaxWidth());if(error.isNotBlank())Text(error,color=MpMuted);if(!loading&&error.isBlank()&&entries.isEmpty())Text("Nenhuma scan disponível nesta seção.",color=MpMuted);entries.forEach{(id,scan)->CreditTile(title,scan.optString("scanName","Scan"),scan.optString("photo",scan.optString("donorPhoto")),""){selected=id to scan}}}
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
  donations.forEach{(id,scan)->CreditTile("Obra doada",scan.optString("scanName","Scan doadora"),scan.optString("photo",scan.optString("donorPhoto")),""){selectedPartner=id to scan};if(scan.optString("donorUid").isNotBlank())CreditTile("Doada por",scan.optString("donorName","Leitor MP SCAN"),scan.optString("donorPhoto"),scan.optString("donorHandle")){uri.openUri("https://www.mpscan.online/#/perfil/"+scan.optString("donorUid"))}}
  requested.forEach{(id,person)->CreditTile("Obra pedida por",person.optString("nome","Leitor MP SCAN"),person.optString("foto"),person.optString("nomeUsuario")){uri.openUri("https://www.mpscan.online/#/perfil/$id")}}
 }
}
@Composable private fun CreditTile(label:String,name:String,photo:String,handle:String,open:()->Unit){Surface(Modifier.fillMaxWidth().clickable(onClick=open),shape=RoundedCornerShape(20.dp),color=MpSurface,border=BorderStroke(1.dp,MpAccent.copy(.3f))){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){if(photo.isNotBlank())MpImage(photo,name,Modifier.size(46.dp).clip(RoundedCornerShape(23.dp)),contentScale=ContentScale.Crop)else Text(name.take(1),color=MpAccent2,fontWeight=FontWeight.Black);Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(label,color=MpAccent2,style=MaterialTheme.typography.labelSmall);Text(name,fontWeight=FontWeight.Bold);if(handle.isNotBlank())Text("@"+handle.removePrefix("@"),color=MpMuted,style=MaterialTheme.typography.bodySmall)};Text("↗",color=MpMuted)}}}

@Composable private fun PartnerDetails(id:String,scan:JSONObject,donation:Boolean,initialWorks:List<Work>,close:()->Unit,openWork:(Work)->Unit){
 val uri=LocalUriHandler.current
 var works by remember(id){mutableStateOf(initialWorks)};var owner by remember(id){mutableStateOf<ProfilePerson?>(null)}
 var loading by remember(id){mutableStateOf(initialWorks.isEmpty())};var error by remember(id){mutableStateOf("")}
 val ownerUid=scan.optString(if(donation)"donorUid"else"ownerUid",id)
 LaunchedEffect(id){if(works.isEmpty())runCatching{CatalogRepository().works()}.onSuccess{works=it}.onFailure{error="Não foi possível carregar as obras. Confira sua conexão."};loading=false;runCatching{withContext(Dispatchers.IO){SiteAccess.json("perfisPublicos/$ownerUid")}}.onSuccess{owner=ProfileIdentity.person(ownerUid,it)}}
 val ids=scan.optJSONArray("donatedWorkIds")
 val members=works.filter{if(donation)ids!=null&&(0 until ids.length()).any{index->ids.optString(index)==it.id}else it.scanOwnerUid==id||it.scan.equals(scan.optString("scanName"),true)}
 androidx.compose.ui.window.Dialog(close,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
 Surface(Modifier.fillMaxWidth(.96f).fillMaxHeight(.9f),color=MpBackground,shape=RoundedCornerShape(28.dp)){
 LazyColumn(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=20.dp)){
  item{Row(verticalAlignment=Alignment.CenterVertically){Text(if(donation)"OBRAS DOADAS"else"SCAN PARCEIRA",Modifier.weight(1f),color=MpAccent,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall);TextButton(close){Text("Fechar")}}}
  item{if(scan.optString("banner",scan.optString("bannerUrl")).isNotBlank())MpImage(scan.optString("banner",scan.optString("bannerUrl")),null,Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)),contentScale=ContentScale.Crop)}
  item{Row(verticalAlignment=Alignment.CenterVertically){FramedAvatar(scan.optString("photo",scan.optString("donorPhoto")),scan.optString("scanName","MP"),size=76.dp);Column(Modifier.padding(start=12.dp)){Text(scan.optString("scanName","Scan parceira"),fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall);Text("${members.size} histórias nesta vitrine",color=MpMuted)}}}
  item{Text(scan.optString("description").ifBlank{if(donation)"Histórias compartilhadas com a comunidade MP SCAN."else"Conheça as histórias desta equipe."},style=MaterialTheme.typography.bodyLarge)}
  owner?.let{person->item{Surface(color=MpSurface,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,MpLine)){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){FramedAvatar(person.photo,person.name,size=48.dp);Column(Modifier.padding(start=12.dp)){Text(if(donation)"Compartilhado por"else"Administração da scan",color=MpAccent,style=MaterialTheme.typography.labelSmall);Text(person.name,fontWeight=FontWeight.Bold);if(person.username.isNotBlank())Text("@${person.username}",color=MpMuted)}}}}}
  item{val social=scan.optString("socialUrl");if(social.startsWith("https://"))OutlinedButton({uri.openUri(social)},shape=RoundedCornerShape(16.dp)){Text("Rede social da equipe ↗")}}
  item{Text("Histórias da scan",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);if(loading)LinearProgressIndicator(Modifier.fillMaxWidth());if(error.isNotBlank())Text(error,color=MpMuted);if(!loading&&error.isBlank()&&members.isEmpty())Text("Esta equipe está preparando sua vitrine.",color=MpMuted)}
  items(members.size){index->val work=members[index];Surface(Modifier.fillMaxWidth().clickable{close();openWork(work)},color=MpSurface,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,MpLine)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){MpImage(work.cover,work.title,Modifier.width(62.dp).height(90.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop);Column(Modifier.padding(start=14.dp)){Text(work.title,fontWeight=FontWeight.Bold);Text(work.genres.take(3).joinToString(" • "),color=MpMuted,style=MaterialTheme.typography.labelSmall)}}}}
 }
 }
 }
}
