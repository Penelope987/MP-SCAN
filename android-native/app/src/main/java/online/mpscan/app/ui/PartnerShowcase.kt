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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
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
 val readableA=PartnerPresentation.accessibleAccent(a,bg);val readableB=PartnerPresentation.accessibleAccent(b,bg)
 val readableAccent=color(readableA);val accent2=color(readableB)
 val type=MaterialTheme.typography;val editorial=scan.optString("themePreset") in listOf("editorial","velvet","paper");val titles=if(editorial)type.copy(headlineMedium=type.headlineMedium.copy(fontFamily=FontFamily.Serif),titleLarge=type.titleLarge.copy(fontFamily=FontFamily.Serif))else type
 MaterialTheme(typography=titles,colorScheme=base.copy(background=color(bg),onBackground=ink,surface=lerp(color(bg),ink,.045f),surfaceVariant=lerp(color(bg),ink,.09f),onSurface=ink,onSurfaceVariant=ink.copy(.72f),primary=readableAccent,onPrimary=if(AppearanceColors.darkText(readableA))Color.Black else Color.White,secondary=accent2,onSecondary=if(AppearanceColors.darkText(readableB))Color.Black else Color.White,outline=ink.copy(.15f)),content=content)
}
@Composable internal fun PartnerShowcase(scan:JSONObject,donation:Boolean,works:List<Work>,open:()->Unit){
 val members=PartnerPresentation.members(scan,donation,works)
 val ownerUid=scan.optString(if(donation)"donorUid"else"ownerUid")
 var team by remember(scan.toString(),donation){mutableStateOf(PartnerPresentation.roster(scan,donation,ownerUid))}
 var person by remember{mutableStateOf<String?>(null)}
 person?.let{NativeProfileDialog(it){person=null}}
 LaunchedEffect(ownerUid,scan.toString()){
  team=team.map{member->try{ProfileIdentity.person(member.uid,UserDirectory.profile(member.uid))}catch(e:CancellationException){throw e}catch(e:Exception){member}}
 }
 Surface(Modifier.fillMaxWidth().clickable(onClick=open),color=MpSurface,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MpLine)){
  Column{
   Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.linearGradient(listOf(MpAccent.copy(.4f),MpSurface2)))){
    val banner=scan.optString("cover").ifBlank{scan.optString("banner").ifBlank{members.firstOrNull()?.cover.orEmpty()}}
    if(banner.isNotBlank())MpImage(banner,null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
    Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(.75f)))))
    Text(if(donation)"OBRAS DOADAS"else"HOSPEDAGEM",Modifier.align(Alignment.BottomStart).padding(16.dp),color=Color.White,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
   }
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){FramedAvatar(scan.optString("photo"),scan.optString("scanName"),size=44.dp);Column(Modifier.weight(1f).padding(start=12.dp)){Text(scan.optString("scanName"),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge,maxLines=2);Text(if(members.size==1)"1 obra"else"${members.size} obras",color=MpMuted,style=MaterialTheme.typography.bodySmall)};Text("↗",color=MpAccent)}
    if(team.isNotEmpty())Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
     Text(if(donation)"Equipe doadora"else"ADMs da hospedagem",color=MpMuted,style=MaterialTheme.typography.labelSmall)
     team.forEach{member->Box(Modifier.clickable{person=member.uid}){FramedAvatar(member.photo,member.name,size=32.dp)}}
    }
    if(members.isNotEmpty())Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){members.take(8).forEach{MpImage(it.cover,it.title,Modifier.width(64.dp).height(96.dp).clip(RoundedCornerShape(9.dp)),contentScale=ContentScale.Crop)}}
    FilledTonalButton(onClick=open,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.filledTonalButtonColors(containerColor=MpAccent.copy(.12f),contentColor=MpAccent)){
     Text(if(donation)"Explorar obras doadas →"else"Conhecer esta hospedagem →",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge)
    }
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
 var team by remember(id){mutableStateOf(PartnerPresentation.roster(scan,donation,id))};var profile by remember(id){mutableStateOf<String?>(null)}
 var error by remember(id){mutableStateOf("")};var loading by remember(id){mutableStateOf(initialWorks.isEmpty())};var recent by remember(id){mutableStateOf<List<RecentUpdate>>(emptyList())};var ratings by remember(id){mutableStateOf<Map<String,WorkRating>>(emptyMap())}
 val members=PartnerPresentation.members(current,donation,works)
 profile?.let{NativeProfileDialog(it){profile=null}}
 LaunchedEffect(id,donation,online){
  if(!online){error="Você está offline. Abra seus capítulos baixados na Biblioteca.";loading=false;return@LaunchedEffect}
  while(true){try{val refreshed=publicJson((if(donation)"donationScans"else"partnerScans")+"/$id");available=PartnerPresentation.visible(refreshed,donation);if(available)current=refreshed;works=CatalogRepository().works();error=""}catch(e:CancellationException){throw e}catch(e:Exception){error="Não foi possível atualizar a vitrine. Confira sua conexão."};loading=false;delay(20000)}
 }
 LaunchedEffect(id,current.toString(),online){
  if(!online)return@LaunchedEffect
  val roster=PartnerPresentation.roster(current,donation,id)
  val gate=Semaphore(6);team=coroutineScope{roster.map{person->async{gate.withPermit{try{val p=UserDirectory.profile(person.uid);if(p.length()>0)ProfileIdentity.person(person.uid,p)else person}catch(e:CancellationException){throw e}catch(e:Exception){person}}}}.awaitAll()}
 }
 LaunchedEffect(id,members.map{it.id to it.updatedAt},online){
  if(!online)return@LaunchedEffect
  while(true){val gate=Semaphore(6);ratings=coroutineScope{members.map{work->async{gate.withPermit{work.id to try{WorkSocialRepository().rating(work.id,null)}catch(e:CancellationException){throw e}catch(e:Exception){WorkRating()}}}}.awaitAll().toMap()};recent=CatalogRepository().recentUpdates(members,10);delay(60000)}
 }
 Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)){
  HostingTheme(current){Surface(Modifier.fillMaxWidth(.97f).widthIn(max=1080.dp).fillMaxHeight(.94f),color=MaterialTheme.colorScheme.background,shape=RoundedCornerShape(26.dp)){
   if(!available)Column(Modifier.padding(24.dp)){Text("Esta hospedagem não está disponível.");TextButton(close){Text("Voltar")}}
   else LazyVerticalGrid(GridCells.Adaptive(if(current.optString("layout")=="compact")115.dp else 140.dp),Modifier.padding(horizontal=if(current.optString("displayMode")=="app")12.dp else 18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(top=12.dp,bottom=24.dp)){
    item(span={GridItemSpan(maxLineSpan)}){Row(verticalAlignment=Alignment.CenterVertically){Text(if(donation)"OBRAS DOADAS"else"HOSPEDAGEM",Modifier.weight(1f),color=MpAccent,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold);TextButton(close){Text("Fechar")}}}
    item(span={GridItemSpan(maxLineSpan)}){PartnerHero(current,donation,members.size)}
    item(span={GridItemSpan(maxLineSpan)}){Row(verticalAlignment=Alignment.CenterVertically){FramedAvatar(current.optString("photo"),current.optString("scanName"),size=48.dp);Text(current.optString("scanName"),Modifier.padding(start=12.dp),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}
    if(team.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(donation)"Doador e equipe"else"ADMs da hospedagem",color=MpMuted,style=MaterialTheme.typography.labelLarge);LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){items(team,key={it.uid}){person->Box(Modifier.width(245.dp)){UserIdentityCard(person){profile=person.uid}}}}}}
    item(span={GridItemSpan(maxLineSpan)}){Column{
     val social=current.optString("socialUrl");if(social.startsWith("https://"))OutlinedButton({uri.openUri(social)},modifier=if(current.optString("socialStyle")=="bar")Modifier.fillMaxWidth()else Modifier,shape=RoundedCornerShape(if(current.optString("socialStyle")=="circle")50 else 16)){Text("Rede social da scan ↗")}
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
     if(current.optBoolean("showRecent",true)&&recent.isNotEmpty()){
      item(span={GridItemSpan(maxLineSpan)}){SectionTitle("Atualizações recentes")}
      if(current.optString("updatesStyle","grid")=="grid")items(recent,key={"recent_"+it.work.id}){update->Column{PartnerWorkTile(update.work,current){close();openWork(update.work)};Text(update.chapter.label,color=MpAccent,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=6.dp))}}
      else item(span={GridItemSpan(maxLineSpan)}){if(current.optString("updatesStyle")=="strips")Column(verticalArrangement=Arrangement.spacedBy(10.dp)){recent.forEach{update->Surface(color=MpSurface2,shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(10.dp)){SearchResultCard(update.work){close();openWork(update.work)};Text(update.chapter.label,color=MpAccent,style=MaterialTheme.typography.labelSmall)}}}}else LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){items(recent,key={it.work.id}){update->Column(Modifier.width(190.dp)){PartnerWorkTile(update.work,current){close();openWork(update.work)};Text(update.chapter.label,color=MpAccent,modifier=Modifier.padding(top=6.dp),style=MaterialTheme.typography.labelMedium)}}}}
     }
     val popular=visible.sortedWith(compareByDescending<Work>{ratings[it.id]?.average?:0.0}.thenByDescending{it.reads})
     if(current.optBoolean("showPopular",true)&&popular.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){PartnerShelf("Ranking das obras",popular.take(8),current,true,ratings){close();openWork(it)}}
    }
    item(span={GridItemSpan(maxLineSpan)}){SectionTitle(if(query.isNotBlank())"Resultados · ${visible.size}"else"Obras")}
    if(visible.isEmpty()&&!loading)item(span={GridItemSpan(maxLineSpan)}){Text(if(home=="search"&&query.isBlank())"Pesquise para explorar o catálogo desta scan."else"Nenhuma obra disponível nesta seleção.",color=MpMuted)}
    items(visible,key={it.id}){work->PartnerWorkTile(PartnerPresentation.enrich(work,PartnerDirectory.entries.value),current){close();openWork(PartnerPresentation.enrich(work,PartnerDirectory.entries.value))}}
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
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){SectionTitle(title);LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){itemsIndexed(works,key={_,w->w.id}){index,work->Column(Modifier.width(if(scan.optString("cardsStyle")=="shelf")125.dp else 150.dp)){Box{PartnerWorkTile(work,scan){open(PartnerPresentation.enrich(work,PartnerDirectory.entries.value))};if(rank)Surface(Modifier.padding(8.dp),color=MpAccent,shape=RoundedCornerShape(10.dp)){Text("${index+1}º",Modifier.padding(8.dp),color=MaterialTheme.colorScheme.onPrimary,fontWeight=FontWeight.Bold)}};if(rank)Text(ratings[work.id]?.takeIf{it.total>0}?.let{"%.1f / 5 · %d votos".format(it.average,it.total)}?:"Ainda sem avaliações",color=MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=6.dp))}}}}
}
@Composable private fun PartnerWorkTile(work:Work,scan:JSONObject,open:()->Unit){
 val style=scan.optString("cardsStyle","covers")
 if(style=="covers")WorkCoverTile(work,Modifier.fillMaxWidth(),open)
 else Surface(Modifier.fillMaxWidth().then(if(style=="floating")Modifier.shadow(7.dp,RoundedCornerShape(20.dp))else Modifier).clickable(onClick=open),color=if(style=="floating")MpSurface2 else Color.Transparent,shape=RoundedCornerShape(if(style=="shelf")10.dp else 20.dp)){
  Column(Modifier.padding(if(style=="floating")8.dp else 0.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){MpImage(work.cover,work.title,Modifier.fillMaxWidth().aspectRatio(.68f).clip(RoundedCornerShape(if(style=="shelf")8.dp else 16.dp)),contentScale=ContentScale.Crop);Text(work.title,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium);WorkOriginBadge(work)}
 }
}
@Composable fun WorkOriginBadge(work:Work){
 val entries by PartnerDirectory.entries.collectAsState();val origin=PartnerPresentation.origin(work,entries)?:return
 val label=when(origin.kind){"hosting"->"⌂ Hospedagem";"donation"->"♡ Obra doada";else->"◇ Scan parceira"}
 Surface(color=MpSurface2,shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,MpLine)){Row(Modifier.padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){if(origin.photo.isNotBlank()){FramedAvatar(origin.photo,origin.name,size=20.dp);Spacer(Modifier.width(6.dp))};Text("$label · ${origin.name}",color=MpAccent,style=MaterialTheme.typography.labelSmall,maxLines=2,overflow=TextOverflow.Ellipsis)}}
}
@Composable fun WorkOriginCard(work:Work,openWork:(Work)->Unit){
 val entries by PartnerDirectory.entries.collectAsState();val origin=PartnerPresentation.origin(work,entries)?:return
 var person by remember(work.id){mutableStateOf<ProfilePerson?>(null)}
 LaunchedEffect(origin.uid){person=null;if(origin.uid.isNotBlank())try{val value=UserDirectory.profile(origin.uid);if(value.length()>0)person=ProfileIdentity.person(origin.uid,value)}catch(e:CancellationException){throw e}catch(e:Exception){}}
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
   person?.let{UserIdentityCard(it){profile=true}}?:run{if(origin.uid.isNotBlank())TextButton({profile=true}){Text(if(origin.kind=="donation")"Ver perfil do doador"else"Ver perfil responsável")}}
  }
 }
}
