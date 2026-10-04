package online.mpscan.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*
import org.json.JSONObject

private fun partnerWorks(scan:JSONObject,donation:Boolean,works:List<Work>):List<Work>{
 val ids=scan.optJSONArray("donatedWorkIds");val owner=scan.optString("ownerUid")
 return works.filter{work->if(donation)ids!=null&&(0 until ids.length()).any{ids.optString(it)==work.id}else owner.isNotBlank()&&work.scanOwnerUid==owner}
}
@Composable internal fun PartnerShowcase(scan:JSONObject,donation:Boolean,works:List<Work>,open:()->Unit){
 val members=partnerWorks(scan,donation,works)
 Surface(Modifier.fillMaxWidth().clickable(onClick=open),color=MpSurface,shape=RoundedCornerShape(28.dp),border=BorderStroke(1.dp,MpLine)){
  Column{
   Box(Modifier.fillMaxWidth().height(175.dp).background(MpAccent.copy(.12f))){
    val banner=scan.optString("banner",scan.optString("bannerUrl")).ifBlank{members.firstOrNull()?.cover.orEmpty()}
    if(banner.isNotBlank())MpImage(banner,null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
    Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.2f),Color.Black.copy(.86f)))))
    Column(Modifier.align(Alignment.BottomStart).padding(18.dp)){
     Text(if(donation)"HISTÓRIAS COMPARTILHADAS"else"SCAN PARCEIRA",color=Color.White.copy(.8f),style=MaterialTheme.typography.labelSmall)
     Row(Modifier.padding(top=10.dp),verticalAlignment=Alignment.CenterVertically){FramedAvatar(scan.optString("photo",scan.optString("donorPhoto")),scan.optString("scanName","MP"),size=52.dp);Column(Modifier.padding(start=12.dp)){Text(scan.optString("scanName","Scan parceira"),color=Color.White,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text("${members.size} obras nesta vitrine",color=Color.White.copy(.8f),style=MaterialTheme.typography.bodySmall)}}
    }
   }
   Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    Text(scan.optString("description").ifBlank{if(donation)"Um presente para quem ama descobrir novas histórias."else"Conheça as leituras e as pessoas desta equipe."},color=MpMuted,maxLines=3)
    if(members.isNotEmpty())Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){members.take(5).forEach{MpImage(it.cover,it.title,Modifier.weight(1f).aspectRatio(.68f).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop)}}
    Row(verticalAlignment=Alignment.CenterVertically){Text(if(members.size>5)"+${members.size-5} histórias para descobrir"else"Uma parceria que aproxima leitores",Modifier.weight(1f),color=MpMuted,style=MaterialTheme.typography.labelSmall);Text("Explorar →",color=MpAccent,fontWeight=FontWeight.Bold)}
   }
  }
 }
}
@Composable internal fun PartnerDetails(id:String,scan:JSONObject,donation:Boolean,initialWorks:List<Work>,close:()->Unit,openWork:(Work)->Unit){
 val uri=LocalUriHandler.current
 var works by remember(id){mutableStateOf(initialWorks)};var owner by remember(id){mutableStateOf<ProfilePerson?>(null)}
 var loading by remember(id){mutableStateOf(initialWorks.isEmpty())};var error by remember(id){mutableStateOf("")};var retry by remember{mutableIntStateOf(0)}
 val uid=scan.optString(if(donation)"donorUid"else"ownerUid",id)
 LaunchedEffect(id,retry){error="";if(works.isEmpty()){loading=true;runCatching{CatalogRepository().works()}.onSuccess{works=it}.onFailure{error="Não foi possível carregar as obras. Confira sua conexão."}};loading=false;runCatching{publicJson("perfisPublicos/$uid")}.onSuccess{owner=ProfileIdentity.person(uid,it)}}
 val members=partnerWorks(scan,donation,works)
 Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.fillMaxWidth(.96f).fillMaxHeight(.92f),color=MaterialTheme.colorScheme.background,shape=RoundedCornerShape(28.dp)){
   LazyVerticalGrid(GridCells.Adaptive(145.dp),Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(bottom=24.dp)){
    item(span={GridItemSpan(maxLineSpan)}){Row(verticalAlignment=Alignment.CenterVertically){Text("VITRINE DA COMUNIDADE",Modifier.weight(1f),color=MpAccent,style=MaterialTheme.typography.labelSmall);TextButton(close){Text("Fechar")}}}
    item(span={GridItemSpan(maxLineSpan)}){PartnerShowcase(scan,donation,works){}}
    owner?.let{person->item(span={GridItemSpan(maxLineSpan)}){Surface(color=MpSurface,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,MpLine)){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){FramedAvatar(person.photo,person.name,size=46.dp);Column(Modifier.padding(start=12.dp)){Text(if(donation)"Doada por"else"Responsável pela scan",color=MpAccent,style=MaterialTheme.typography.labelSmall);Text(person.name,fontWeight=FontWeight.Bold);if(person.username.isNotBlank())Text("@${person.username}",color=MpMuted)}}}}}
    val team=scan.optJSONArray("donationAdmins")
    if(team!=null&&team.length()>0){
     item(span={GridItemSpan(maxLineSpan)}){Text("Equipe da scan",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
     items(team.length()){index->val person=team.optJSONObject(index)?:JSONObject();Surface(color=MpSurface,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){FramedAvatar(person.optString("photo"),person.optString("name","MP"),size=48.dp);Text(person.optString("name",person.optString("handle","Perfil")),fontWeight=FontWeight.Bold);if(person.optString("handle").isNotBlank())Text("@"+person.optString("handle").removePrefix("@"),color=MpMuted,style=MaterialTheme.typography.bodySmall)}}}
    }
    item(span={GridItemSpan(maxLineSpan)}){Column{Text("Histórias para descobrir",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);val social=scan.optString("socialUrl");if(social.startsWith("https://"))TextButton({uri.openUri(social)}){Text("Rede social da equipe ↗")};if(loading)LinearProgressIndicator(Modifier.fillMaxWidth());if(error.isNotBlank()){Text(error,color=MpMuted);TextButton({retry++}){Text("Tentar novamente")}};if(!loading&&error.isBlank()&&members.isEmpty())Text("Esta equipe está preparando sua vitrine.",color=MpMuted)}}
    items(members,key={it.id}){work->Column(verticalArrangement=Arrangement.spacedBy(8.dp)){WorkCoverTile(work,Modifier.fillMaxWidth()){close();openWork(work)};if(donation)Surface(color=MpSurface,shape=RoundedCornerShape(14.dp)){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){FramedAvatar(owner?.photo?:scan.optString("donorPhoto"),owner?.name?:scan.optString("donorName","MP"),size=30.dp);Column(Modifier.padding(start=8.dp)){Text("Doada por",color=MpMuted,style=MaterialTheme.typography.labelSmall);Text(owner?.name?:scan.optString("donorName","Scan parceira"),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall,maxLines=2)}}}}}
   }
  }
 }
}
