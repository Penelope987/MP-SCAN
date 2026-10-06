package online.mpscan.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*
import org.json.JSONObject

@Composable internal fun HostingTheme(scan:JSONObject,content:@Composable ()->Unit){
 val defaults=PartnerPresentation.preset(scan)
 fun value(key:String,fallback:String)=scan.optString(key).takeIf(AppearanceColors::valid)?:fallback
 val bg=value("background",defaults[2]);val a=value("accent",defaults[0]);val b=value("accent2",defaults[1]);val light=AppearanceColors.darkText(bg)
 fun color(s:String)=Color(android.graphics.Color.parseColor(s))
 val base=if(light)lightColorScheme()else darkColorScheme()
 val ink=if(light)Color(0xff251c2d)else Color(0xfff6f1fa)
 val accent=color(a);val accent2=color(b)
 val readableAccent=if(AppearanceColors.darkText(a)==light)accent else lerp(accent,ink,.28f)
 MaterialTheme(colorScheme=base.copy(background=color(bg),onBackground=ink,surface=lerp(color(bg),ink,.045f),surfaceVariant=lerp(color(bg),ink,.09f),onSurface=ink,onSurfaceVariant=ink.copy(.72f),primary=readableAccent,onPrimary=if(AppearanceColors.darkText(a))Color.Black else Color.White,secondary=accent2,outline=ink.copy(.15f)),content=content)
}
@Composable internal fun PartnerShowcase(scan:JSONObject,donation:Boolean,works:List<Work>,open:()->Unit){
 val members=PartnerPresentation.members(scan,donation,works)
 Surface(Modifier.fillMaxWidth().clickable(onClick=open),color=MpSurface,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MpLine)){
  Column{
   Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.linearGradient(listOf(MpAccent.copy(.4f),MpSurface2)))){
    val banner=scan.optString("cover").ifBlank{scan.optString("banner").ifBlank{members.firstOrNull()?.cover.orEmpty()}}
    if(banner.isNotBlank())MpImage(banner,null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
    Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(.75f)))))
    Text(if(donation)"OBRAS DOADAS"else"HOSPEDAGEM",Modifier.align(Alignment.BottomStart).padding(16.dp),color=Color.White,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
   }
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){FramedAvatar(scan.optString("photo"),scan.optString("scanName"),size=44.dp);Column(Modifier.weight(1f).padding(start=12.dp)){Text(scan.optString("scanName"),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge,maxLines=2);Text("${members.size} obras",color=MpMuted,style=MaterialTheme.typography.bodySmall)};Text("↗",color=MpAccent)}
    if(members.isNotEmpty())Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){members.take(6).forEach{MpImage(it.cover,it.title,Modifier.weight(1f).aspectRatio(.7f).clip(RoundedCornerShape(9.dp)),contentScale=ContentScale.Crop)}}
    Text(if(donation)"Explorar obras doadas →"else"Conhecer esta hospedagem →",color=MpAccent,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge)
   }
  }
 }
}
@Composable internal fun PartnerHero(scan:JSONObject,donation:Boolean,count:Int){
 val style=scan.optString("heroStyle","cinematic")
 Box(Modifier.fillMaxWidth().heightIn(min=150.dp,max=210.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(MpAccent.copy(.32f),MpSurface2)))){
  if(scan.optString("cover").isNotBlank()){MpImage(scan.optString("cover"),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop);Box(Modifier.matchParentSize().background(Color.Black.copy(.67f)))}
  val ink=if(scan.optString("cover").isNotBlank())Color.White else MpText
  Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=if(style=="centered")Alignment.CenterHorizontally else Alignment.Start){
   Text(if(donation)"OBRAS DOADAS À MP SCAN"else"A VITRINE DESTA SCAN",color=ink.copy(.75f),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
   Text(scan.optString("scanName"),color=ink,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(top=8.dp),maxLines=2)
   Text(scan.optString("description").ifBlank{"Conheça as obras e a equipe desta scan."},color=ink.copy(.8f),maxLines=2,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=8.dp))
   Text("$count obras nesta vitrine",color=ink.copy(.7f),style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=10.dp))
  }
 }
}
@Composable internal fun PartnerDetails(id:String,scan:JSONObject,donation:Boolean,initialWorks:List<Work>,close:()->Unit,openWork:(Work)->Unit){
 val context=LocalContext.current;val online=networkAvailable();val uri=LocalUriHandler.current
 var current by remember(id,donation){mutableStateOf(scan)};var available by remember(id,donation){mutableStateOf(true)}
 var works by remember(id){mutableStateOf(initialWorks)};var query by remember(id){mutableStateOf("")};var searchOpen by remember(id){mutableStateOf(false)}
 var team by remember(id){mutableStateOf<List<ProfilePerson>>(emptyList())};var profile by remember(id){mutableStateOf<String?>(null)}
 var error by remember(id){mutableStateOf("")};var loading by remember(id){mutableStateOf(initialWorks.isEmpty())};var recent by remember(id){mutableStateOf<List<RecentUpdate>>(emptyList())};var ratings by remember(id){mutableStateOf<Map<String,WorkRating>>(emptyMap())}
 val members=PartnerPresentation.members(current,donation,works)
 profile?.let{NativeProfileDialog(it){profile=null}}
 LaunchedEffect(id,donation,online){
  if(!online){error="Você está offline. Abra seus capítulos baixados na Biblioteca.";loading=false;return@LaunchedEffect}
  while(true){try{val refreshed=publicJson((if(donation)"donationScans"else"partnerScans")+"/$id");available=PartnerPresentation.visible(refreshed,donation);if(available)current=refreshed;works=CatalogRepository().works();error=""}catch(e:CancellationException){throw e}catch(e:Exception){error="Não foi possível atualizar a vitrine. Confira sua conexão."};loading=false;delay(20000)}
 }
 LaunchedEffect(id,current.toString(),online){
  if(!online)return@LaunchedEffect
  val root=current.optJSONObject("publicAdmins")?:JSONObject()
  val roster=linkedMapOf<String,ProfilePerson>()
  val uid=current.optString(if(donation)"donorUid"else"ownerUid",if(donation)""else id)
  if(uid.isNotBlank())roster[uid]=ProfilePerson(uid,current.optString(if(donation)"donorName"else"scanName","Perfil"),current.optString("donorHandle"),current.optString("donorPhoto"))
  root.keys().forEach{key->root.optJSONObject(key)?.let{p->roster[key]=ProfilePerson(key,p.optString("name","Perfil"),p.optString("handle"),p.optString("photo"))}}
  current.optJSONArray("donationAdmins")?.let{list->(0 until list.length()).forEach{n->list.optJSONObject(n)?.let{p->val key=p.optString("uid");if(key.isNotBlank())roster[key]=ProfilePerson(key,p.optString("name","Perfil"),p.optString("handle"),p.optString("photo"))}}}
  val gate=Semaphore(6);team=coroutineScope{roster.values.map{person->async{gate.withPermit{try{val p=UserDirectory.profile(person.uid);if(p.length()>0)ProfileIdentity.person(person.uid,p)else person}catch(e:CancellationException){throw e}catch(e:Exception){person}}}}.awaitAll()}
 }
 LaunchedEffect(id,members.map{it.id to it.updatedAt},online){
  if(!online)return@LaunchedEffect
  while(true){val gate=Semaphore(6);ratings=coroutineScope{members.map{work->async{gate.withPermit{work.id to try{WorkSocialRepository().rating(work.id,null)}catch(e:CancellationException){throw e}catch(e:Exception){WorkRating()}}}}.awaitAll().toMap()};recent=CatalogRepository().recentUpdates(members,10);delay(60000)}
 }
 Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)){
  HostingTheme(current){Surface(Modifier.fillMaxWidth(.97f).widthIn(max=1080.dp).fillMaxHeight(.94f),color=MaterialTheme.colorScheme.background,shape=RoundedCornerShape(26.dp)){
   if(!available)Column(Modifier.padding(24.dp)){Text("Esta hospedagem não está disponível.");TextButton(close){Text("Voltar")}}
   else LazyVerticalGrid(GridCells.Adaptive(if(current.optString("layout")=="compact")115.dp else 140.dp),Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(top=12.dp,bottom=24.dp)){
    item(span={GridItemSpan(maxLineSpan)}){Row(verticalAlignment=Alignment.CenterVertically){Text(if(donation)"OBRAS DOADAS"else"HOSPEDAGEM",Modifier.weight(1f),color=MpAccent,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold);TextButton(close){Text("Fechar")}}}
    item(span={GridItemSpan(maxLineSpan)}){PartnerHero(current,donation,members.size)}
    item(span={GridItemSpan(maxLineSpan)}){Row(verticalAlignment=Alignment.CenterVertically){FramedAvatar(current.optString("photo"),current.optString("scanName"),size=48.dp);Text(current.optString("scanName"),Modifier.padding(start=12.dp),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}
    if(team.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(donation)"Doador e equipe"else"ADMs da hospedagem",color=MpMuted,style=MaterialTheme.typography.labelLarge);team.forEach{person->UserIdentityCard(person){profile=person.uid}}}}
    item(span={GridItemSpan(maxLineSpan)}){Column{
     val social=current.optString("socialUrl");if(social.startsWith("https://"))OutlinedButton({uri.openUri(social)},shape=RoundedCornerShape(if(current.optString("socialStyle")=="circle")50 else 16)){Text("Rede social da scan ↗")}
     val separate=current.optString("searchMode")=="separate"
     if(separate)TextButton({searchOpen=!searchOpen}){Text("⌕ Buscar obras desta scan")}
     if(!separate||searchOpen)OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(top=8.dp),label={Text("Encontre sua próxima leitura")},placeholder={Text("Buscar em ${current.optString("scanName")}")},singleLine=true,shape=RoundedCornerShape(if(current.optString("searchStyle")=="line")4 else 18))
     if(loading)LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=12.dp));if(error.isNotBlank())Text(error,color=MpMuted,modifier=Modifier.padding(top=8.dp))
    }}
    val home=current.optString("homeMode","all");val visible=if(query.isNotBlank())members.filter{Discovery.matches(it,query)}else if(home=="search")emptyList()else if(home=="recent")members.filter{w->recent.any{it.work.id==w.id}}else members
    if(query.isBlank()&&home!="search"){
     if(current.optBoolean("showFeatured",true)&&visible.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){PartnerFeature(visible.first(),current){close();openWork(PartnerPresentation.enrich(it,PartnerDirectory.entries.value))}}
     val history=ReadingStore(context).history().map{it.workId}.toSet();val reading=visible.filter{it.id in history}
     if(current.optBoolean("showContinue",true)&&reading.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){PartnerShelf("Continue lendo",reading,current,false,ratings){close();openWork(it)}}
     if(current.optBoolean("showRecent",true)&&recent.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){SectionTitle("Atualizações recentes");if(current.optString("updatesStyle")=="strips")recent.forEach{update->Surface(color=MpSurface2,shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(10.dp)){SearchResultCard(update.work){close();openWork(update.work)};Text(update.chapter.label,color=MpAccent,style=MaterialTheme.typography.labelSmall)}}}else LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){items(recent,key={it.work.id}){update->Column(Modifier.width(if(current.optString("updatesStyle")=="spotlight")190.dp else 150.dp)){WorkCoverTile(update.work){close();openWork(update.work)};Text(update.chapter.label,color=MpAccent,modifier=Modifier.padding(top=6.dp),style=MaterialTheme.typography.labelMedium)}}}}}
     val popular=visible.sortedWith(compareByDescending<Work>{ratings[it.id]?.average?:0.0}.thenByDescending{it.reads})
     if(current.optBoolean("showPopular",true)&&popular.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){PartnerShelf("Ranking das obras",popular.take(8),current,true,ratings){close();openWork(it)}}
    }
    item(span={GridItemSpan(maxLineSpan)}){SectionTitle(if(query.isNotBlank())"Resultados · ${visible.size}"else"Obras")}
    if(visible.isEmpty()&&!loading)item(span={GridItemSpan(maxLineSpan)}){Text(if(home=="search"&&query.isBlank())"Pesquise para explorar o catálogo desta scan."else"Nenhuma obra disponível nesta seleção.",color=MpMuted)}
    items(visible,key={it.id}){work->WorkCoverTile(PartnerPresentation.enrich(work,PartnerDirectory.entries.value),Modifier.fillMaxWidth()){close();openWork(PartnerPresentation.enrich(work,PartnerDirectory.entries.value))}}
   }
  }}
 }
}
@Composable private fun SectionTitle(title:String){Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=8.dp))}
@Composable private fun PartnerFeature(work:Work,scan:JSONObject,open:(Work)->Unit){
 Surface(Modifier.fillMaxWidth().clickable{open(work)},color=MpSurface2,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MpLine)){
  Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f).padding(end=12.dp)){Text("✦ DESTAQUE",color=MpAccent,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold);Text(work.title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge,maxLines=3,modifier=Modifier.padding(top=8.dp));Text(work.synopsis,color=MpMuted,maxLines=3,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp));Text("Ver detalhes ↗",color=MpAccent,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=16.dp))};MpImage(work.cover,work.title,Modifier.width(if(scan.optString("heroStyle")=="split")105.dp else 92.dp).aspectRatio(.68f).clip(RoundedCornerShape(16.dp)),contentScale=ContentScale.Crop)}
 }
}
@Composable private fun PartnerShelf(title:String,works:List<Work>,scan:JSONObject,rank:Boolean,ratings:Map<String,WorkRating>,open:(Work)->Unit){
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){SectionTitle(title);LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){itemsIndexed(works,key={_,w->w.id}){index,work->Column(Modifier.width(if(scan.optString("cardsStyle")=="shelf")125.dp else 150.dp)){Box{WorkCoverTile(work){open(PartnerPresentation.enrich(work,PartnerDirectory.entries.value))};if(rank)Surface(Modifier.padding(8.dp),color=MpAccent,shape=RoundedCornerShape(10.dp)){Text("${index+1}º",Modifier.padding(8.dp),color=MaterialTheme.colorScheme.onPrimary,fontWeight=FontWeight.Bold)}};if(rank)Text(ratings[work.id]?.takeIf{it.total>0}?.let{"%.1f / 5 · %d votos".format(it.average,it.total)}?:"Ainda sem avaliações",color=MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=6.dp))}}}}
}
@Composable fun WorkOriginBadge(work:Work){
 val entries by PartnerDirectory.entries.collectAsState();val origin=PartnerPresentation.origin(work,entries)?:return
 val label=when(origin.kind){"hosting"->"⌂ Hospedagem";"donation"->"♡ Obra doada";else->"◇ Scan parceira"}
 Surface(color=MpSurface2,shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,MpLine)){Row(Modifier.padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){if(origin.photo.isNotBlank()){FramedAvatar(origin.photo,origin.name,size=20.dp);Spacer(Modifier.width(6.dp))};Text("$label · ${origin.name}",color=MpAccent,style=MaterialTheme.typography.labelSmall,maxLines=2,overflow=TextOverflow.Ellipsis)}}
}
@Composable fun WorkOriginCard(work:Work,openWork:(Work)->Unit){
 val entries by PartnerDirectory.entries.collectAsState();val origin=PartnerPresentation.origin(work,entries)?:return
 var selected by remember(work.id){mutableStateOf<PartnerEntry?>(null)};var profile by remember(work.id){mutableStateOf(false)}
 val uri=LocalUriHandler.current
 selected?.let{PartnerDetails(it.id,it.scan,it.donation,emptyList(),{selected=null},openWork)}
 if(profile&&origin.uid.isNotBlank())NativeProfileDialog(origin.uid){profile=false}
 Surface(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),shape=RoundedCornerShape(22.dp),color=MpSurface2,border=BorderStroke(1.dp,MpLine)){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   WorkOriginBadge(work)
   Text(when(origin.kind){"hosting"->"Obra publicada pela hospedagem ${origin.name}.";"donation"->"Obra doada por ${origin.name}. Os créditos da equipe de origem são preservados.";else->"Catálogo de ${origin.name}. A obra e seus créditos pertencem à equipe de origem; a MP SCAN oferece o leitor e os recursos do aplicativo."},color=MpMuted,style=MaterialTheme.typography.bodySmall)
   if(origin.kind!="external")TextButton({selected=entries.firstOrNull{it.id==origin.id&&it.donation==(origin.kind=="donation")}}){Text("Conhecer a scan →")}
   else TextButton({uri.openUri(work.hosting.takeIf{it.startsWith("https://")}?:"https://"+work.hosting)}){Text("Visitar a origem ↗")}
   if(origin.uid.isNotBlank())TextButton({profile=true}){Text(if(origin.kind=="donation")"Ver perfil do doador"else"Ver perfil responsável")}
  }
 }
}
