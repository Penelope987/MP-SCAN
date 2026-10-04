package online.mpscan.app




import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.mpscan.app.ui.MpImage
import online.mpscan.app.data.Discovery
import online.mpscan.app.data.RankedWork
import online.mpscan.app.ui.DiscoverySearch
import online.mpscan.app.ui.SearchResultCard
import online.mpscan.app.ui.SupportCard
import online.mpscan.app.ui.SearchFilters
import online.mpscan.app.ui.LibrarySections
import online.mpscan.app.ui.WorkActions
import online.mpscan.app.data.SiteAccess
import online.mpscan.app.ui.SiteAccountGate
import online.mpscan.app.ui.SiteAnnouncement
import online.mpscan.app.data.CatalogRepository
import online.mpscan.app.data.Work
import online.mpscan.app.data.RecentUpdate
import online.mpscan.app.data.Chapter
import online.mpscan.app.data.ChapterMetadata
import online.mpscan.app.data.OfflineStore
import online.mpscan.app.data.OfflineChapter
import online.mpscan.app.data.ReadingStore
import online.mpscan.app.data.ReadingProgress
import online.mpscan.app.data.FavoritesStore
import online.mpscan.app.data.CollectionsStore
import online.mpscan.app.data.AccountStore
import online.mpscan.app.data.AccountRepository
import online.mpscan.app.data.WorkRating
import online.mpscan.app.data.WorkReaction
import online.mpscan.app.data.WorkSocialRepository
import online.mpscan.app.data.NewBadgeStyle
import kotlinx.coroutines.flow.catch
import online.mpscan.app.data.ChapterDownloadWorker
import online.mpscan.app.data.NewChapterWorker
import online.mpscan.app.data.LibraryRepository
import online.mpscan.app.data.UserCollection
import online.mpscan.app.ui.theme.*
import online.mpscan.app.ui.SettingsScreen
import online.mpscan.app.ui.CommentsSection
import online.mpscan.app.ui.AccountProfileScreen
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale




class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);SiteAccess.init(applicationContext);NewChapterWorker.schedule(applicationContext);lifecycleScope.launch{repeatOnLifecycle(Lifecycle.State.STARTED){while(true){NewChapterWorker.runNow(applicationContext);delay(60000)}}};enableEdgeToEdge();setContent{MpScanTheme{online.mpscan.app.ui.AppLock{SiteAccountGate{HomeRoot()}}}}}}
private enum class Destination(val label:String,val icon:String){Home("Início","⌂"),Search("Busca","⌕"),Library("Biblioteca","▣"),Profile("Perfil","♙"),Menu("Menu","☰")}




@Composable private fun HomeRoot(){
 var selected by remember{mutableStateOf(Destination.Home)};var selectedWork by remember{mutableStateOf<Work?>(null)};var directReader by remember{mutableStateOf<Pair<Work,Chapter>?>(null)};var settingsOpen by remember{mutableStateOf(false)};var works by remember{mutableStateOf<List<Work>>(emptyList())};var newBadge by remember{mutableStateOf(NewBadgeStyle())};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf("")}
 var catalogOnline by remember{mutableStateOf(false)}
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 var attempt by remember{mutableIntStateOf(0)}
 var connected by remember{mutableStateOf(isConnected(context))}
 LaunchedEffect(Unit){AccountStore(context).session()?.let{runCatching{AccountRepository().profile(it)}.onSuccess{online.mpscan.app.data.ProfileSnapshots.save(it)}}}
 LaunchedEffect(attempt,connected){
  loading=true;error="";val repository=CatalogRepository()
  val saved=withContext(Dispatchers.IO){OfflineStore(context).offlineWorks()}
  if(!connected){works=saved;catalogOnline=false;loading=false}
  else {runCatching{repository.works()}.onSuccess{works=it;catalogOnline=true}.onFailure{if(it is kotlinx.coroutines.CancellationException)throw it;works=saved;catalogOnline=false;if(saved.isEmpty())error="Não foi possível carregar o catálogo. Confira a conexão e tente novamente."};loading=false;scope.launch{val store=OfflineStore(context);val ids=withContext(Dispatchers.IO){store.downloads().map{it.workId}.toSet()};works.filter{it.id in ids}.forEach{store.cacheWork(it)}};newBadge=runCatching{repository.newBadge()}.getOrDefault(NewBadgeStyle())}
 }

 DisposableEffect(Unit){
  val manager=context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
  val callback=object:android.net.ConnectivityManager.NetworkCallback(){
   private var current:android.net.Network?=null
   override fun onAvailable(network:android.net.Network){current=network}
   override fun onCapabilitiesChanged(network:android.net.Network,capabilities:android.net.NetworkCapabilities){
    if(network!=current)return
    val available=capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)&&capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    scope.launch{val wasConnected=connected;connected=available;if(available&&!wasConnected)attempt++}
   }
   override fun onLost(network:android.net.Network){if(network==current){current=null;scope.launch{connected=false}}}
  }

  manager.registerDefaultNetworkCallback(callback)
  onDispose{manager.unregisterNetworkCallback(callback)}
 }

 val lifecycleOwner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 DisposableEffect(lifecycleOwner){val observer=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_RESUME){scope.launch{val available=isConnected(context);connected=available;if(available&&!catalogOnline&&!loading)attempt++}}};lifecycleOwner.lifecycle.addObserver(observer);onDispose{lifecycleOwner.lifecycle.removeObserver(observer)}}
 LaunchedEffect(connected,catalogOnline,loading){if(connected&&!catalogOnline&&!loading){delay(6000);if(connected&&!catalogOnline)attempt++}}

 online.mpscan.app.ui.FirstNotificationPermission()
 SiteAnnouncement(works,connected&&selectedWork==null&&directReader==null&&!settingsOpen){selectedWork=it}
 BackHandler(settingsOpen||directReader!=null||selectedWork!=null){when{settingsOpen->settingsOpen=false;directReader!=null->directReader=null;selectedWork!=null->selectedWork=null}}
 Scaffold(containerColor=MpBackground,topBar={if(!connected&&!settingsOpen&&directReader==null&&selectedWork==null)Surface(color=MpSurface2){Text("Modo offline • Suas obras baixadas estão disponíveis.",Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),color=MpAccent2,style=MaterialTheme.typography.bodySmall)}},bottomBar={if(selectedWork==null&&directReader==null&&!settingsOpen)Surface(Modifier.padding(horizontal=12.dp,vertical=8.dp),shape=RoundedCornerShape(26.dp),color=MpSurface,border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){NavigationBar(containerColor=MpSurface,tonalElevation=0.dp){Destination.entries.forEach{x->NavigationBarItem(selected=selected==x,onClick={selected=x},icon={Icon(when(x){Destination.Home->Icons.Default.Home;Destination.Search->Icons.Default.Search;Destination.Library->Icons.Default.List;Destination.Profile->Icons.Default.Person;Destination.Menu->Icons.Default.Menu},contentDescription=x.label)},label={Text(x.label,style=MaterialTheme.typography.labelSmall)},colors=NavigationBarItemDefaults.colors(indicatorColor=MpAccent.copy(.15f)))}}}}){p->
  Box(Modifier.fillMaxSize().padding(p)){when{settingsOpen->SettingsScreen{settingsOpen=false};directReader!=null->Reader(directReader!!.first,directReader!!.second){directReader=null};selectedWork!=null->WorkDetails(selectedWork!!,newBadge,{selectedWork=null}){selectedWork=it};selected==Destination.Library->OfflineLibrary(works,{selectedWork=it}){w,c->directReader=w to c};selected==Destination.Profile->AccountProfileScreen{settingsOpen=true};selected==Destination.Menu->online.mpscan.app.ui.MenuScreen(works,{selectedWork=it}){settingsOpen=true};loading->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()};error.isNotBlank()->Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Text(error,color=MpMuted);Button({attempt++},Modifier.padding(top=16.dp)){Text("Tentar novamente")};Text("Para ler offline, vá à Biblioteca e toque em Downloads.",color=MpAccent2,modifier=Modifier.padding(top=16.dp))};works.isEmpty()->Message("As obras publicadas aparecerão aqui.");selected==Destination.Home->Home(works,newBadge,connected&&catalogOnline,Modifier,{selectedWork=it},{w,c->directReader=w to c},{selected=Destination.Search},{selected=Destination.Profile});selected==Destination.Search->Search(works){selectedWork=it}}}
 }
}
@Composable private fun OfflineLibrary(works:List<Work>,openWork:(Work)->Unit,open:(Work,Chapter)->Unit){
 val context=LocalContext.current
 val store=remember{OfflineStore(context.applicationContext)}
 val readingStore=remember{ReadingStore(context.applicationContext)}
 val favorites=remember{FavoritesStore(context.applicationContext)}
 var downloads by remember{mutableStateOf(store.downloads())}
 var tab by remember{mutableStateOf("favorites")};var libraryQuery by remember{mutableStateOf("")}
 val history=readingStore.history()
 Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
  Spacer(Modifier.height(24.dp))
  Text("SEU UNIVERSO",color=MpAccent,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall)
  Text("Biblioteca",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(top=6.dp))
  Text("Histórias favoritas, coleções e o seu próximo capítulo.",color=MpMuted,modifier=Modifier.padding(top=6.dp))
  Surface(Modifier.fillMaxWidth().padding(top=18.dp),shape=RoundedCornerShape(26.dp),color=MpAccent.copy(.07f),border=BorderStroke(1.dp,MpAccent.copy(.18f))){Row(Modifier.padding(20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf("${favorites.ids().size}" to "Favoritos","${history.distinctBy{it.workId}.size}" to "Obras lidas","${downloads.size}" to "Capítulos salvos").forEach{(count,label)->Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text(count,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall,color=MpAccent);Text(label,color=MpMuted,style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center,modifier=Modifier.padding(top=4.dp))}}}}
  LibrarySections(tab){tab=it;libraryQuery=""}
  if(tab=="favorites"){
   OutlinedTextField(libraryQuery,{libraryQuery=it},Modifier.fillMaxWidth().padding(bottom=16.dp),singleLine=true,shape=RoundedCornerShape(18.dp),leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Pesquisar na sua biblioteca")})
   val list=works.filter{favorites.contains(it.id)&&Discovery.matches(it,libraryQuery)}
   if(list.isEmpty())LibraryEmpty(if(libraryQuery.isBlank())"Sua coleção começa com uma história"else"Nenhum favorito encontrado",if(libraryQuery.isBlank())"Toque em Biblioteca na obra para guardá-la aqui."else"Tente outro nome, autor ou gênero.")
   else LazyVerticalGrid(GridCells.Adaptive(140.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){gridItems(list,key={it.id}){Card(it,openWork,Modifier.fillMaxWidth())}}
  }else if(tab=="collections") online.mpscan.app.ui.MyCollections(works,openWork)
  else if(tab=="community") online.mpscan.app.ui.CommunityCollections(works,openWork)
  else if(tab=="downloads"){
   val groups=downloads.groupBy{it.workId}.values.toList()
   if(groups.isEmpty())LibraryEmpty("Nenhuma obra baixada","Abra uma obra e use Baixar todos os capítulos.")
   else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=24.dp)){items(groups,key={it.first().workId}){chapters->val saved=chapters.first();val work=works.firstOrNull{it.id==saved.workId}?:saved.toWork();DownloadedWorkRow(work,chapters.size){openWork(work)}}}
  }else if(tab=="continue") ProgressList(history.filter{it.percent<100}.distinctBy{it.workId},open,"Nenhuma leitura em andamento.")
  else ProgressList(history,open,"Seu histórico ainda está vazio.")
 }
}
@Composable private fun LibraryEmpty(title:String,body:String){Surface(Modifier.fillMaxWidth(),color=MpSurface,shape=RoundedCornerShape(22.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("▣",color=MpAccent,style=MaterialTheme.typography.displaySmall);Text(title,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp));Text(body,color=MpMuted)}}}
@Composable private fun ProgressList(items:List<ReadingProgress>,open:(Work,Chapter)->Unit,empty:String){if(items.isEmpty()){LibraryEmpty(empty,"As leituras realizadas aparecerão aqui.");return};LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=24.dp)){items(items,key={it.workId+it.chapterId}){item->ProgressRow(item){open(item.toWork(),item.toChapter())}}}}
@Composable private fun ProgressRow(item:ReadingProgress,open:()->Unit){Surface(Modifier.fillMaxWidth().clickable{open()},color=MpSurface,shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){MpImage(item.workCover,item.workTitle,Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(MpSurface2),contentScale=ContentScale.Crop);Column(Modifier.weight(1f).padding(start=12.dp)){Text(item.workTitle,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${item.chapterLabel} • página ${item.page}/${item.totalPages}",color=MpMuted,style=MaterialTheme.typography.bodySmall);LinearProgressIndicator(progress={item.percent/100f},Modifier.fillMaxWidth().padding(top=9.dp));Text("${item.percent}% lido",color=MpAccent2,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=4.dp))}}}}
@Composable private fun DownloadedRow(item:OfflineChapter,open:()->Unit,delete:()->Unit){Surface(Modifier.fillMaxWidth(),color=MpSurface,shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){MpImage(item.workCover,item.workTitle,Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).background(MpSurface2),contentScale=ContentScale.Crop);Column(Modifier.weight(1f).padding(horizontal=12.dp).clickable{open()}){Text(item.workTitle,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${item.chapterLabel} • ${item.pageCount} páginas",color=MpMuted,style=MaterialTheme.typography.bodySmall);Text("Disponível offline",color=MpAccent2,style=MaterialTheme.typography.labelSmall)};TextButton(delete){Text("Excluir")}}}}
@Composable private fun DownloadedWorkRow(work:Work,chapterCount:Int,open:()->Unit){Surface(Modifier.fillMaxWidth().clickable{open()},color=MpSurface,shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){MpImage(work.cover,work.title,Modifier.width(72.dp).aspectRatio(3f/4f).clip(RoundedCornerShape(12.dp)).background(MpSurface2),contentScale=ContentScale.Crop);Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(work.title,fontWeight=FontWeight.Black,maxLines=2,overflow=TextOverflow.Ellipsis);Text("$chapterCount ${if(chapterCount==1)"capítulo baixado" else "capítulos baixados"}",color=MpAccent2,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=4.dp));Text("Toque para abrir a página da obra",color=MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=4.dp))};Text("›",color=MpMuted,style=MaterialTheme.typography.headlineSmall)}}}
private fun OfflineChapter.toWork()=(work?.copy(cover=workCover)?:Work(workId,workTitle,"",workCover,"","","","",emptyList(),0,0))
private fun OfflineChapter.toChapter()=chapter?:Chapter(chapterId,chapterLabel.substringAfter("Capítulo ").toDoubleOrNull(),"",true,0)
private fun ReadingProgress.toWork()=Work(workId,workTitle,"",workCover,"","","","",emptyList(),0,0)
private fun ReadingProgress.toChapter()=Chapter(chapterId,chapterLabel.substringAfter("Capítulo ").toDoubleOrNull(),"",true,0)
@Composable private fun Header(){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(MpAccent,MpAccent2))),contentAlignment=Alignment.Center){Text("MP",fontWeight=FontWeight.Black)};Spacer(Modifier.width(12.dp));Column{Text("MP SCAN",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium);Text("Sua próxima leitura começa aqui",color=MpMuted,style=MaterialTheme.typography.bodySmall)};Spacer(Modifier.weight(1f));Surface(shape=RoundedCornerShape(14.dp),color=MpSurface,border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){Box(Modifier.size(42.dp),contentAlignment=Alignment.Center){Text("♢")}}}}


@Composable private fun Home(works:List<Work>,badge:NewBadgeStyle,connected:Boolean,modifier:Modifier,open:(Work)->Unit,continueReading:(Work,Chapter)->Unit,search:()->Unit,profile:()->Unit){
 val context=LocalContext.current;val uri=androidx.compose.ui.platform.LocalUriHandler.current
 val state=rememberLazyListState()
 var headerVisible by remember{mutableStateOf(true)};var searchOpen by remember{mutableStateOf(false)};var avatar by remember{mutableStateOf("")}
 var ranking by remember{mutableStateOf<List<RankedWork>>(emptyList())};var rankingError by remember{mutableStateOf(false)}
 var updates by remember(works){mutableStateOf<List<RecentUpdate>>(emptyList())}
 var updateChapters by remember(works){mutableStateOf<Map<String,List<Chapter>>>(emptyMap())}
 var continuations by remember{mutableStateOf<List<ContinueEntry>>(emptyList())}
 LaunchedEffect(Unit){
  var previousIndex=0;var previousOffset=0
  snapshotFlow{state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset}.collect{(index,offset)->
   if(state.isScrollInProgress){if(index>previousIndex||index==previousIndex&&offset-previousOffset>3)headerVisible=false else if(index<previousIndex||index==previousIndex&&previousOffset-offset>3)headerVisible=true}
   previousIndex=index;previousOffset=offset
  }
 }
 LaunchedEffect(Unit){AccountStore(context).session()?.uid?.let{uid->avatar=online.mpscan.app.data.ProfileSnapshots.get(uid)?.photo.orEmpty();if(isConnected(context))avatar=runCatching{SiteAccess.json("usuarios/$uid").optString("foto")}.getOrDefault("")}}
 LaunchedEffect(works,connected){
  continuations=loadContinuations(context,works)
  if(!connected)return@LaunchedEffect
  while(true){
   runCatching{Discovery.ranking(works,SiteAccess.json("ratings"))}.onSuccess{ranking=it.take(10);rankingError=false}.onFailure{rankingError=true}
   updates=runCatching{CatalogRepository().recentUpdates(works,8)}.getOrDefault(updates)
   updateChapters=kotlinx.coroutines.coroutineScope{updates.map{u->async{u.work.id to runCatching{CatalogRepository().chapters(u.work.id).filter{it.available}.take(2)}.getOrDefault(listOf(u.chapter))}}.awaitAll().toMap()}
   delay(45000)
  }
 }
 if(searchOpen)DiscoverySearch(works,{searchOpen=false},open)
 Box(modifier.fillMaxSize()){
  LazyColumn(Modifier.fillMaxSize(),state=state,contentPadding=PaddingValues(top=108.dp,bottom=30.dp),verticalArrangement=Arrangement.spacedBy(26.dp)){
   item{HomeShowcase(works.take(5),open,continueReading)}
   if(connected)item{RankedRail(ranking,rankingError,open)}
   else item{Text("Suas histórias offline",Modifier.padding(horizontal=22.dp),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)}
   if(continuations.isNotEmpty())item{Column(Modifier.padding(horizontal=16.dp)){Text("Continue de onde parou",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("Sua próxima página está esperando",color=MpMuted,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=4.dp,bottom=14.dp));LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){items(continuations,key={it.work.id}){entry->ContinueCard(entry){continueReading(entry.work,entry.chapter)}}}}}
   if(connected)item{HomeUpdates(updates,updateChapters,badge,open,continueReading)}
   item{Box(Modifier.padding(horizontal=16.dp)){SupportCard()}}
  }
  AnimatedVisibility(headerVisible,Modifier.align(Alignment.TopCenter).padding(horizontal=12.dp,vertical=10.dp),enter=fadeIn(tween(220))+slideInVertically(tween(220)){-it},exit=fadeOut(tween(220))+slideOutVertically(tween(220)){-it}){
   Surface(Modifier.fillMaxWidth(),color=MpSurface.copy(.95f),shape=RoundedCornerShape(22.dp),shadowElevation=4.dp,border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){
    Row(Modifier.padding(horizontal=12.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.weight(1f)){Text("MP SCAN",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text("Sua próxima leitura",color=MpMuted,style=MaterialTheme.typography.labelSmall)}
     IconButton({uri.openUri("https://livepix.gg/mpscan")}){Icon(Icons.Default.FavoriteBorder,"Apoiar a MP SCAN",tint=MpAccent2)}
     IconButton({searchOpen=true}){Icon(Icons.Default.Search,"Pesquisar na Home")}
     IconButton(profile){if(avatar.isNotBlank())MpImage(avatar,"Seu perfil",Modifier.size(36.dp).clip(RoundedCornerShape(50)),contentScale=ContentScale.Crop)else Icon(Icons.Default.Person,"Seu perfil")}
    }
   }
  }
 }
}
private data class ContinueEntry(val work:Work,val chapter:Chapter,val progress:ReadingProgress?,val completed:Int,val total:Int)
private suspend fun loadContinuations(context:android.content.Context,works:List<Work>):List<ContinueEntry> = kotlinx.coroutines.coroutineScope {
 val history=ReadingStore(context).history();val store=OfflineStore(context)
 history.distinctBy{it.workId}.take(10).map{last->async{
  if(!isConnected(context)&&works.none{it.id==last.workId})return@async null
  val work=works.firstOrNull{it.id==last.workId}?:last.toWork()
  val chapters=runCatching{check(isConnected(context));CatalogRepository().chapters(work.id).filter{it.available}.sortedBy{it.number?:Double.MAX_VALUE}}.getOrDefault(store.downloads().filter{it.workId==work.id}.map{it.toChapter()}.sortedBy{it.number?:Double.MAX_VALUE})
  val records=history.filter{it.workId==work.id}.associateBy{it.chapterId};val completed=chapters.count{(records[it.id]?.percent?:0)>=100}
  val current=chapters.firstOrNull{it.id==last.chapterId&&(records[it.id]?.percent?:0)<100}?:chapters.firstOrNull{(records[it.id]?.percent?:0)<100}
  if(current==null)null else ContinueEntry(work,current,records[current.id],completed,chapters.size)
 }}.awaitAll().filterNotNull()
}
@Composable private fun ContinueCard(entry:ContinueEntry,open:()->Unit){
 val fraction=if(entry.total==0)0f else (entry.completed+(entry.progress?.percent?:0)/100f)/entry.total
 Surface(Modifier.width(300.dp).clickable(onClick=open),color=MpSurface,shape=RoundedCornerShape(24.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){
  Column(Modifier.padding(16.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){MpImage(entry.work.cover,entry.work.title,Modifier.width(72.dp).aspectRatio(3f/4.4f).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop);Column(Modifier.weight(1f).padding(start=14.dp)){Text(entry.work.title,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis);Text(entry.chapter.label,color=MpAccent,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=7.dp));Text(entry.progress?.let{"Página ${it.page.coerceAtMost(it.totalPages)} de ${it.totalPages}"}?:"Próximo capítulo para ler",color=MpMuted,style=MaterialTheme.typography.labelSmall)}}
   LinearProgressIndicator(progress={fraction.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(top=16.dp),color=MpAccent,trackColor=MpSurface2)
   Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("${entry.completed} de ${entry.total} capítulos lidos",color=MpMuted,style=MaterialTheme.typography.labelSmall);Text("${(fraction*100).toInt()}%",color=MpAccent,style=MaterialTheme.typography.labelSmall)}
   Button(open,Modifier.fillMaxWidth().padding(top=12.dp),shape=RoundedCornerShape(13.dp)){Text("Continuar leitura →")}
  }
 }
}
@Composable private fun RankedRail(ranking:List<RankedWork>,error:Boolean,open:(Work)->Unit){
 Column(Modifier.padding(horizontal=16.dp)){
  Text("Popular entre os leitores",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
  Text("As obras com mais avaliações da comunidade",color=MpMuted,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=4.dp,bottom=14.dp))
  if(ranking.isEmpty())Text(if(error)"Não foi possível carregar as avaliações agora." else "As obras avaliadas aparecerão aqui.",color=MpMuted)
  LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)){items(ranking,key={it.work.id}){entry->
   val position=ranking.indexOf(entry)+1
   val accent=when(position){1->Color(0xFFDBB45E);2->Color(0xFFADA1E2);3->Color(0xFFCD936B);4->MpAccent2;5->MpAccent;else->MpMuted}
   Surface(Modifier.width(230.dp).clickable{open(entry.work)},color=MpSurface,shape=RoundedCornerShape(26.dp),border=androidx.compose.foundation.BorderStroke(1.dp,accent.copy(if(position<=5).65f else .3f))){
    Column(Modifier.padding(12.dp)){
     Box(Modifier.fillMaxWidth().aspectRatio(3f/4.5f).clip(RoundedCornerShape(18.dp))){
      MpImage(entry.work.cover,entry.work.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
      Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xDD131017)))))
      Surface(Modifier.align(Alignment.TopStart).padding(10.dp),color=Color(0xCC18141E),shape=RoundedCornerShape(12.dp)){Text("♛ TOP $position",color=accent,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(10.dp))}
      Text(position.toString().padStart(2,'0'),color=accent,fontWeight=FontWeight.Black,fontSize=60.sp,modifier=Modifier.align(Alignment.BottomStart).padding(14.dp))
      Surface(Modifier.align(Alignment.BottomEnd).padding(12.dp),color=Color(0xBB18141E),shape=RoundedCornerShape(12.dp)){Text("★ "+"%.1f".format(Locale("pt","BR"),entry.average),color=Color(0xFFFFDB8B),fontWeight=FontWeight.Bold,modifier=Modifier.padding(10.dp))}
     }
     Text(if(position==1)"A FAVORITA DOS LEITORES" else "ENTRE AS MAIS VOTADAS",color=accent,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=14.dp))
     Text(entry.work.title,fontWeight=FontWeight.Bold,maxLines=2,minLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=8.dp))
     Text("${entry.votes} avaliações · Conhecer ↗",color=MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=10.dp,bottom=8.dp))
    }
   }
  }}
 }
}
@Composable private fun HomeShowcase(works:List<Work>,open:(Work)->Unit,read:(Work,Chapter)->Unit){
 if(works.isEmpty())return
 var index by remember(works){mutableIntStateOf(0)}
 val work=works[index.coerceIn(0,works.lastIndex)]
 val showcaseContext=LocalContext.current
 var chapters by remember(work.id){mutableStateOf(OfflineStore(showcaseContext).downloads().filter{it.workId==work.id}.map{it.toChapter()})}
 var rating by remember(work.id){mutableStateOf<WorkRating?>(null)}
 LaunchedEffect(work.id){if(!isConnected(showcaseContext))return@LaunchedEffect;chapters=runCatching{CatalogRepository().chapters(work.id).filter{it.available}}.getOrDefault(emptyList());rating=runCatching{WorkSocialRepository().rating(work.id,null)}.getOrNull()}
 Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(MpAccent.copy(.09f),MpBackground))).padding(horizontal=22.dp,vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally){
  Text("EM DESTAQUE",color=MpMuted,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.labelSmall)
  MpImage(work.cover,work.title,Modifier.padding(top=18.dp).widthIn(max=210.dp).fillMaxWidth(.55f).aspectRatio(3f/4.5f).clip(RoundedCornerShape(22.dp)),contentScale=ContentScale.Crop)
  val score=rating
  if(score!=null&&score.total>0)Text("★ "+"%.1f".format(Locale("pt","BR"),score.average),color=Color(0xFFCC942B),fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=16.dp))
  Text(work.title,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold,maxLines=3,overflow=TextOverflow.Ellipsis,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(top=12.dp))
  Text(work.synopsis,color=MpMuted,maxLines=4,overflow=TextOverflow.Ellipsis,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(top=12.dp))
  LazyRow(Modifier.padding(top=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(work.genres.take(4)){genre->Surface(color=MpSurface2,shape=RoundedCornerShape(50)){Text(genre,color=MpText,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(horizontal=13.dp,vertical=8.dp))}}}
  Row(Modifier.widthIn(max=440.dp).fillMaxWidth().padding(top=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   Button({val first=chapters.minByOrNull{it.number?:Double.MAX_VALUE};if(first!=null)read(work,first)else open(work)},Modifier.weight(1f),shape=RoundedCornerShape(16.dp),contentPadding=PaddingValues(vertical=14.dp)){Text("▶ Ler agora",fontWeight=FontWeight.Bold)}
   FilledTonalButton({open(work)},Modifier.weight(1f),shape=RoundedCornerShape(16.dp),contentPadding=PaddingValues(vertical=14.dp)){Text("Detalhes")}
  }
  if(chapters.isNotEmpty())Text("${chapters.size} capítulos",color=MpMuted,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=12.dp))
  Row(Modifier.padding(top=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
   IconButton({index=(index-1+works.size)%works.size}){Text("‹",fontSize=26.sp)}
   works.forEachIndexed{i,_->Box(Modifier.width(if(i==index)24.dp else 7.dp).height(7.dp).clip(RoundedCornerShape(50)).background(if(i==index)MpAccent else MpLine).clickable{index=i})}
   IconButton({index=(index+1)%works.size}){Text("›",fontSize=26.sp)}
  }
 }
}
@Composable private fun HomeUpdates(updates:List<RecentUpdate>,chapters:Map<String,List<Chapter>>,badge:NewBadgeStyle,open:(Work)->Unit,read:(Work,Chapter)->Unit){
 var grid by remember{mutableStateOf(true)}
 Column(Modifier.padding(horizontal=16.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){Text("Últimas atualizações",Modifier.weight(1f),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);TextButton({grid=!grid}){Text(if(grid)"☷" else "▦",fontSize=23.sp)}}
  Spacer(Modifier.height(14.dp))
  if(updates.isEmpty())Text("Os próximos capítulos aparecerão aqui.",color=MpMuted)
  else updates.chunked(if(grid)2 else 1).forEach{pair->
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){
    pair.forEach{update->Column(Modifier.weight(1f)){
     MpImage(update.work.cover,update.work.title,Modifier.fillMaxWidth().aspectRatio(if(grid)3f/4.5f else 1.4f).clip(RoundedCornerShape(20.dp)).clickable{open(update.work)},contentScale=ContentScale.Crop)
     Text(update.work.title,fontWeight=FontWeight.SemiBold,maxLines=2,minLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=10.dp,bottom=6.dp))
     (chapters[update.work.id]?:listOf(update.chapter)).forEach{chapter->Surface(Modifier.fillMaxWidth().padding(bottom=6.dp).clickable{read(update.work,chapter)},color=MpSurface2,shape=RoundedCornerShape(10.dp)){
      Column(Modifier.padding(horizontal=10.dp,vertical=9.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Text(chapter.label,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold);if(isNewChapter(chapter)&&badge.enabled)AdminNewBadge(badge)};Text(formatWorkDate(maxOf(chapter.updatedAt,chapter.createdAt,chapter.scheduledAt)),color=MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=2.dp))}
     }}
    }}
    if(grid&&pair.size==1)Spacer(Modifier.weight(1f))
   }
   Spacer(Modifier.height(20.dp))
  }
 }
}
@Composable private fun Hero(w:Work,open:(Work)->Unit){Box(Modifier.fillMaxWidth().height(370.dp).clip(RoundedCornerShape(34.dp)).background(MpSurface)){MpImage(w.banner.ifBlank{w.cover},w.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF207070A),Color(0xA607070A),Color.Transparent))));Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xF507070A)))));Column(Modifier.align(Alignment.BottomStart).padding(26.dp).fillMaxWidth(.92f)){Text("DESTAQUE MP SCAN",color=MpAccent2,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall);Spacer(Modifier.height(10.dp));Text(w.title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineLarge,maxLines=2,overflow=TextOverflow.Ellipsis);Text(w.synopsis,color=MpMuted,maxLines=3,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(vertical=12.dp));Button(onClick={open(w)},shape=RoundedCornerShape(14.dp)){Text("Ver detalhes",fontWeight=FontWeight.Black)}}}}
@Composable private fun Rail(title:String,works:List<Work>,ranked:Boolean,open:(Work)->Unit){Column{Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));LazyRow(horizontalArrangement=Arrangement.spacedBy(13.dp)){items(works,key={it.id}){w->Box{Card(w,open);if(ranked)Text((works.indexOf(w)+1).toString(),fontWeight=FontWeight.Black,style=MaterialTheme.typography.displaySmall,color=Color.White,modifier=Modifier.align(Alignment.BottomStart).background(Color(0xB30B0B0D)).padding(horizontal=8.dp))}}}}}
@Composable private fun RecentUpdates(updates:List<RecentUpdate>,open:(Work)->Unit){Column{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Atualizações recentes",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text("Capítulos que acabaram de chegar",color=MpMuted,style=MaterialTheme.typography.bodySmall)}};Spacer(Modifier.height(14.dp));if(updates.isEmpty()){Text("As próximas atualizações aparecerão aqui.",color=MpMuted)}else updates.chunked(2).forEach{pair->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){pair.forEach{update->val w=update.work;Surface(Modifier.weight(1f),color=MpSurface,shape=RoundedCornerShape(20.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){Column{Box(Modifier.fillMaxWidth().height(142.dp).clickable{open(w)}){MpImage(w.cover,w.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)};Column(Modifier.padding(12.dp)){Text(w.title,fontWeight=FontWeight.Black,maxLines=2,minLines=2,overflow=TextOverflow.Ellipsis);Row(Modifier.padding(top=6.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){Text(update.chapter.label,color=MpAccent2,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodySmall);if(System.currentTimeMillis()-normalizeMillis(update.updatedAt) in 0..14L*24*60*60*1000)Surface(color=MpAccent.copy(.18f),shape=RoundedCornerShape(6.dp)){Text("NOVO",color=MpAccent2,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(horizontal=5.dp,vertical=3.dp))}};Text(update.chapter.subtitle.ifBlank{" "},color=MpMuted,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis);Text(formatUpdateDate(update.updatedAt),color=MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=3.dp));Button(onClick={open(w)},modifier=Modifier.fillMaxWidth().padding(top=10.dp),shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(vertical=8.dp)){Text("LER AGORA",fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelMedium)}}}}};if(pair.size==1)Spacer(Modifier.weight(1f))};Spacer(Modifier.height(12.dp))}}}
private fun formatUpdateDate(value:Long):String{if(value<=0)return "Atualização recente";val millis=if(value<100000000000L)value*1000 else value;return SimpleDateFormat("dd/MM/yyyy",Locale("pt","BR")).format(Date(millis))}
@Composable private fun Card(w:Work,open:(Work)->Unit,modifier:Modifier=Modifier.width(148.dp)){Column(modifier.clickable{open(w)}){MpImage(w.cover,w.title,Modifier.fillMaxWidth().aspectRatio(3f/4f).clip(RoundedCornerShape(18.dp)).background(MpSurface2),contentScale=ContentScale.Crop);Text(w.title,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=8.dp));Text(w.type,color=MpMuted,style=MaterialTheme.typography.bodySmall)}}
@Composable private fun Search(works:List<Work>,open:(Work)->Unit){
 var query by remember{mutableStateOf("")};var status by remember{mutableStateOf("")};var genre by remember{mutableStateOf<Set<String>>(emptySet())};var filtersOpen by remember{mutableStateOf(false)}
 val genres=remember(works){works.flatMap{it.genres}.distinct().sorted()}
 val filtered=remember(works,query,status,genre){works.filter{w->Discovery.matches(w,query)&&(status.isBlank()||localizedStatus(w.status).equals(status,true))&&Discovery.genresMatch(w,genre)}}
 if(filtersOpen)SearchFilters(status,genre,genres,{filtersOpen=false}){newStatus,newGenre->status=newStatus;genre=newGenre}
 Column(Modifier.fillMaxSize().padding(horizontal=18.dp,vertical=24.dp)){
  Text("DESCUBRA NOVAS HISTÓRIAS",color=MpAccent,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall)
  Text("Buscar obras",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(top=6.dp))
  Text("Explore mundos, personagens e histórias que ficam.",color=MpMuted,modifier=Modifier.padding(top=8.dp,bottom=18.dp))
  OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(22.dp),leadingIcon={Icon(Icons.Default.Search,"Pesquisar")},trailingIcon={if(query.isNotBlank())IconButton({query=""}){Icon(Icons.Default.Close,"Limpar pesquisa")}},label={Text("Título, sinopse, autor ou gênero")})
  Row(Modifier.fillMaxWidth().padding(vertical=16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("${filtered.size} obras encontradas",fontWeight=FontWeight.Bold);Text((listOf(status)+genre).filter{it.isNotBlank()}.joinToString(" • ").ifBlank{"Todo o universo MP SCAN"},color=MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=4.dp))};OutlinedButton({filtersOpen=true},shape=RoundedCornerShape(16.dp)){Text(if(status.isBlank()&&genre.isEmpty())"Refinar busca"else"Filtros ativos")}}
  if(filtered.isEmpty())LibraryEmpty("Nenhuma história encontrada","Tente outro termo ou remova os filtros.")
  else LazyVerticalGrid(GridCells.Adaptive(145.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(22.dp),contentPadding=PaddingValues(bottom=28.dp)){gridItems(filtered,key={it.id}){Card(it,open,Modifier.fillMaxWidth())}}
 }
}
@Composable private fun WorkDetails(work:Work,newBadge:NewBadgeStyle,back:()->Unit,openWork:(Work)->Unit){
 val context=LocalContext.current;val appContext=context.applicationContext;val favorites=remember{FavoritesStore(appContext)};val libraryRepository=remember{LibraryRepository()};val readingStore=remember{ReadingStore(appContext)};val offlineStore=remember{OfflineStore(appContext)};val repository=remember{CatalogRepository()};val social=remember{WorkSocialRepository()};val accountStore=remember{AccountStore(appContext)};var session by remember{mutableStateOf(accountStore.session())};val accountRepository=remember{AccountRepository()};val scope=rememberCoroutineScope()
 var favorite by remember(work.id){mutableStateOf(favorites.contains(work.id))};var chapters by remember(work.id){mutableStateOf(offlineStore.downloads().filter{it.workId==work.id}.map{it.toChapter()}.sortedByDescending{it.number?:-1.0})};var loading by remember(work.id){mutableStateOf(true)};var reading by remember(work.id){mutableStateOf<Chapter?>(null)}
 var releaseTick by remember(work.id){mutableIntStateOf(0)}
 LaunchedEffect(chapters){while(true){val release=chapters.filter{!it.available&&it.scheduledAt>System.currentTimeMillis()}.minOfOrNull{it.scheduledAt}?:break;delay((release-System.currentTimeMillis()+250).coerceAtLeast(250));releaseTick++}}
 var tab by remember(work.id){mutableStateOf("Capítulos")};var rating by remember(work.id){mutableStateOf(WorkRating())};var reactions by remember(work.id){mutableStateOf<List<WorkReaction>>(emptyList())};var socialError by remember{mutableStateOf("")}
 var downloadingAll by remember(work.id){mutableStateOf(false)};var bulkProgress by remember(work.id){mutableIntStateOf(0)};var bulkMessage by remember(work.id){mutableStateOf("")};var bulkError by remember(work.id){mutableStateOf("")}
 var downloadedIds by remember(work.id){mutableStateOf(offlineStore.downloads().filter{it.workId==work.id}.map{it.chapterId}.toSet())}
 var subscribed by remember(work.id){mutableStateOf(false)};var collectionDialog by remember(work.id){mutableStateOf(false)};var actionMessage by remember{mutableStateOf("")}
 val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)NewChapterWorker.runNow(appContext);actionMessage=if(granted)"Notificações de capítulos ativadas." else "Permita notificações nos ajustes do celular para receber os avisos."}
 val chapterProgress=remember(work.id){mutableStateMapOf<String,Int>()}
 val chapterErrors=remember(work.id){mutableStateMapOf<String,Boolean>()}
 if(reading!=null){BackHandler{reading=null};Reader(work,reading!!){reading=null};return}
 LaunchedEffect(work.id){if(!isConnected(context)){loading=false;return@LaunchedEffect};session?.let{old->runCatching{accountRepository.refresh(old)}.onSuccess{fresh->session=fresh;accountStore.save(fresh)}};subscribed=session?.let{runCatching{libraryRepository.notificationEnabled(it,work.id)}.getOrDefault(false)}?:false;val savedDownloads=offlineStore.downloads().filter{it.workId==work.id};downloadedIds=savedDownloads.map{it.chapterId}.toSet();val saved=savedDownloads.map{it.toChapter()};runCatching{repository.chapters(work.id)}.onSuccess{remote->chapters=(remote+saved).distinctBy{it.id}.sortedByDescending{it.number?:-1.0}}.onFailure{chapters=saved};rating=runCatching{social.rating(work.id,session)}.getOrDefault(WorkRating());reactions=runCatching{social.reactions(work.id,session)}.getOrDefault(emptyList());loading=false}
 LaunchedEffect(work.id,chapters){
  WorkManager.getInstance(appContext).getWorkInfosByTagFlow("work-download-${work.id}").catch{bulkError="Não foi possível acompanhar o download. Abra a obra novamente.";downloadingAll=false}.collect{infos->
   val active=infos.filter{!it.state.isFinished}
   val bulk=active.firstOrNull{"work-download-all-${work.id}" in it.tags}
   downloadedIds=withContext(Dispatchers.IO){offlineStore.downloads().filter{it.workId==work.id}.map{it.chapterId}.toSet()}
   chapters.forEach{chapter->
    val info=active.firstOrNull{ChapterDownloadWorker.uniqueName(work.id,chapter.id) in it.tags}
    val bulkChapter=bulk?.progress?.getString(ChapterDownloadWorker.CHAPTER_ID)==chapter.id
    if(info!=null)chapterProgress[chapter.id]=info.progress.getInt(ChapterDownloadWorker.PROGRESS,0)
    else if(bulkChapter)chapterProgress[chapter.id]=bulk!!.progress.getInt(ChapterDownloadWorker.CHAPTER_PROGRESS,0)
    else chapterProgress.remove(chapter.id)
    chapterErrors[chapter.id]=chapter.id !in downloadedIds&&info==null&&bulk==null&&infos.any{ChapterDownloadWorker.uniqueName(work.id,chapter.id) in it.tags&&it.state==androidx.work.WorkInfo.State.FAILED}
   }
   downloadingAll=active.isNotEmpty()
   bulkProgress=bulk?.progress?.getInt(ChapterDownloadWorker.PROGRESS,0)?:if(chapters.isEmpty())0 else (chapters.count{it.id in downloadedIds}*100)/chapters.size
   if(active.isNotEmpty()){
    bulkError=""
    bulkMessage=if(active.all{it.state==androidx.work.WorkInfo.State.ENQUEUED})"Na fila. O download aguarda conexão ou uma nova tentativa." else "Baixando. Você pode usar outros aplicativos."
   }else{
    bulkMessage=""
    bulkError=if(chapters.isNotEmpty()&&chapters.all{it.id in downloadedIds})"" else infos.firstOrNull{"work-download-all-${work.id}" in it.tags&&it.state==androidx.work.WorkInfo.State.FAILED}?.outputData?.getString(ChapterDownloadWorker.ERROR).orEmpty()
   }
  }
 }

 val progress=remember(work.id,chapters){readingStore.history().filter{it.workId==work.id}.associateBy{it.chapterId}}
 val availableChapters=remember(chapters,releaseTick){chapters.filter{it.available}};val continueChapter=availableChapters.firstOrNull{it.id==progress.values.maxByOrNull{p->p.updatedAt}?.chapterId&&(progress[it.id]?.percent?:0)<100}?:availableChapters.sortedBy{it.number?:Double.MAX_VALUE}.firstOrNull{(progress[it.id]?.percent?:0)<100}?:availableChapters.lastOrNull()
 val allDownloaded=availableChapters.isNotEmpty()&&availableChapters.all{it.id in downloadedIds}
 fun downloadChapter(chapter:Chapter){
  if(!chapter.available||chapterProgress.containsKey(chapter.id))return
  chapterErrors.remove(chapter.id);chapterProgress[chapter.id]=0
  scope.launch{try{ChapterDownloadWorker.enqueue(appContext,work,chapter)}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){chapterProgress.remove(chapter.id);chapterErrors[chapter.id]=true;bulkError=e.message?:"Não foi possível iniciar o download."}}
 }

 if(collectionDialog)CollectionPickerDialog(libraryRepository,session,work.id,{collectionDialog=false}){message->actionMessage=message}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=30.dp)){
  item{WorkHero(work,availableChapters.size,rating.average,back)}
  item{online.mpscan.app.ui.WorkCredits(work.id,openWork)}
  item{Column(Modifier.padding(horizontal=12.dp,vertical=14.dp)){Button({continueChapter?.let{reading=it}},Modifier.fillMaxWidth().height(58.dp),enabled=continueChapter!=null,shape=RoundedCornerShape(17.dp)){Text(if(progress.isNotEmpty())"▶ Continuar lendo" else "▶ Começar a ler",fontWeight=FontWeight.Black)};WorkActions(favorite,subscribed,{favorite=favorites.toggle(work.id)},{val current=session;if(current==null)actionMessage="Entre na conta para ativar notificações." else scope.launch{runCatching{val fresh=accountRepository.refresh(current);session=fresh;accountStore.save(fresh);val enabled=!subscribed;libraryRepository.setNotification(fresh,work.id,enabled);enabled}.onSuccess{enabled->subscribed=enabled;if(enabled)NewChapterWorker.runNow(appContext);if(enabled&&android.os.Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)else actionMessage=if(enabled)"Você receberá avisos de novos capítulos." else "Avisos de novos capítulos desativados."}.onFailure{actionMessage=it.message?:"Não foi possível atualizar as notificações."}}},{if(session==null)actionMessage="Entre na conta para usar suas coleções." else collectionDialog=true});if(actionMessage.isNotBlank())Text(actionMessage,color=MpAccent2,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=9.dp))}}
  item{Box(Modifier.padding(horizontal=18.dp,vertical=8.dp)){SupportCard()}}
  if(reactions.isNotEmpty())item{WorkReactions(reactions){chosen->val current=session;if(current==null){socialError="Entre na conta para escolher uma reação."}else scope.launch{runCatching{if(current.refreshToken.isBlank())current else accountRepository.refresh(current)}.mapCatching{fresh->session=fresh;accountStore.save(fresh);social.react(work.id,chosen,fresh);fresh}.onSuccess{fresh->reactions=social.reactions(work.id,fresh);socialError="Reação registrada com sucesso."}.onFailure{socialError=it.message?:"Não foi possível salvar sua reação."}}}}
  item{RatingPanel(rating,session!=null){note->val current=session;if(current==null)socialError="Entre na conta para avaliar." else scope.launch{runCatching{if(current.refreshToken.isBlank())current else accountRepository.refresh(current)}.mapCatching{fresh->session=fresh;accountStore.save(fresh);social.rate(work.id,note,fresh);fresh}.onSuccess{fresh->rating=social.rating(work.id,fresh);socialError="Avaliação salva com sucesso."}.onFailure{socialError=it.message?:"Não foi possível salvar a avaliação."}}}}
  if(socialError.isNotBlank())item{Text(socialError,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(horizontal=16.dp,vertical=8.dp))}
  item{WorkTabs(tab,chapters.size){tab=it}}
  when(tab){
   "Sobre"->{item{SynopsisCard(work)};item{WorkInformation(work,chapters.firstOrNull()?.updatedAt?:work.updatedAt)}}
   "Comentários"->item{CommentsSection("obra",work.id,"",Modifier.padding(horizontal=12.dp,vertical=18.dp))}
   else->{
    item{DownloadAllCard(allDownloaded,downloadingAll,bulkProgress,bulkMessage,bulkError,!loading&&availableChapters.isNotEmpty()){if(availableChapters.isNotEmpty()){bulkError="";bulkMessage="Preparando download…";downloadingAll=true;scope.launch{try{ChapterDownloadWorker.enqueueAll(appContext,work)}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){downloadingAll=false;bulkMessage="";bulkError=e.message?:"Não foi possível iniciar o download."}}}}}
    if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal=16.dp))}
    items(chapters,key={it.id}){c->if(!c.available)ScheduledChapterRow(c)else ChapterRow(c,progress[c.id],c.id in downloadedIds,chapterProgress[c.id],chapterErrors[c.id]==true,newBadge,{reading=c}){downloadChapter(c)}}
   }
  }
 }
}
@Composable private fun CollectionPickerDialog(repository:LibraryRepository,session:online.mpscan.app.data.AccountSession?,workId:String,close:()->Unit,message:(String)->Unit){
 val scope=rememberCoroutineScope();var collections by remember{mutableStateOf<List<UserCollection>>(emptyList())};var selected by remember{mutableStateOf<Set<String>>(emptySet())};var loading by remember{mutableStateOf(true)};var saving by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var creating by remember{mutableStateOf(false)};var input by remember{mutableStateOf("")};var publicCollection by remember{mutableStateOf(false)}
 LaunchedEffect(session?.uid){if(session==null){error="Entre na conta para usar suas coleções.";loading=false}else runCatching{repository.collections(session)}.onSuccess{list->collections=list;selected=list.filter{workId in it.workIds}.map{it.id}.toSet()}.onFailure{error=it.message?:"Não foi possível carregar suas coleções."}.also{loading=false}}
 AlertDialog(onDismissRequest=close,title={Text("Adicionar à coleção")},text={Column{Text("Escolha suas coleções públicas ou privadas.",color=MpMuted);Spacer(Modifier.height(12.dp));when{loading->CircularProgressIndicator();error.isNotBlank()->Text(error,color=MaterialTheme.colorScheme.error);collections.isEmpty()&&!creating->Text("Você ainda não criou uma coleção.",color=MpMuted);else->LazyColumn(Modifier.heightIn(max=320.dp)){items(collections,key={it.id}){item->val checked=item.id in selected;Surface(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{selected=if(checked)selected-item.id else selected+item.id},color=if(checked)MpAccent.copy(.16f)else MpSurface2,shape=RoundedCornerShape(14.dp),border=androidx.compose.foundation.BorderStroke(1.dp,if(checked)MpAccent else MpLine)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Text(if(checked)"✓" else "▣",color=MpAccent);Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(item.name,fontWeight=FontWeight.Bold);Text(if(item.isPublic)"🌐 Pública" else "🔒 Privada",color=MpMuted,style=MaterialTheme.typography.labelSmall)};Text(if(checked)"Selecionada" else "Adicionar",color=MpMuted,style=MaterialTheme.typography.labelSmall)}}}}};if(creating){Spacer(Modifier.height(10.dp));OutlinedTextField(input,{input=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Nome da coleção")});Row(verticalAlignment=Alignment.CenterVertically){Checkbox(publicCollection,{publicCollection=it});Text(if(publicCollection)"Coleção pública" else "Coleção privada")};Button({val current=session;if(current!=null&&input.isNotBlank())scope.launch{saving=true;runCatching{repository.createCollection(current,input,publicCollection)}.onSuccess{created->collections=(collections+created).sortedBy{it.name.lowercase()};input="";creating=false;message("Coleção criada.")}.onFailure{error=it.message?:"Não foi possível criar a coleção."};saving=false}},enabled=!saving){Text("Criar")}}else TextButton({creating=true}){Text("＋ Nova coleção")}}},confirmButton={Button({val current=session;if(current!=null)scope.launch{saving=true;error="";runCatching{collections.forEach{item->val wanted=item.id in selected;val has=workId in item.workIds;if(wanted!=has)repository.setWork(current,item,workId,wanted)}}.onSuccess{message("Coleções atualizadas.");close()}.onFailure{error=it.message?:"Não foi possível atualizar as coleções."};saving=false}},enabled=!loading&&!saving&&session!=null){Text(if(saving)"Salvando…" else "Salvar alterações")}},dismissButton={TextButton(close){Text("Cancelar")}})
}
@Composable private fun WorkHero(work:Work,chapterCount:Int,average:Double,back:()->Unit){
 Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(MpAccent.copy(.1f),MpBackground))).padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")};Text("DETALHES DA OBRA",color=MpMuted,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall)}
  Surface(Modifier.padding(top=14.dp,bottom=22.dp).width(190.dp).aspectRatio(3f/4.4f),shape=RoundedCornerShape(24.dp),shadowElevation=12.dp,border=BorderStroke(1.dp,MpLine)){MpImage(work.cover,work.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
  Text(work.title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium,textAlign=TextAlign.Center)
  if(work.alternateTitle.isNotBlank())Text(work.alternateTitle,color=MpMuted,textAlign=TextAlign.Center,modifier=Modifier.padding(top=6.dp))
  Text(listOf(work.author,work.type).filter{it.isNotBlank()}.joinToString(" • "),color=MpMuted,modifier=Modifier.padding(top=10.dp))
  LazyRow(Modifier.padding(top=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){item{WorkPill(if(average>0)"★ ${"%.1f".format(Locale.US,average)}" else "Sem avaliações")};item{WorkPill(localizedStatus(work.status))};item{WorkPill("$chapterCount capítulos")}}
 }
}
@Composable private fun WorkPill(text:String){Surface(color=MpSurface.copy(.9f),shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){Text(text,Modifier.padding(horizontal=12.dp,vertical=7.dp),style=MaterialTheme.typography.bodySmall)}}
@Composable private fun WorkReactions(items:List<WorkReaction>,choose:(WorkReaction)->Unit){Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),color=MpSurface,shape=RoundedCornerShape(24.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xff57253d))){Column(Modifier.padding(18.dp)){Text("✨ Qual foi sua reação?",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text("Escolha uma reação criada pela equipe para esta obra.",color=MpMuted,modifier=Modifier.padding(top=4.dp,bottom=14.dp));LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){items(items,key={it.source+it.id}){reaction->Surface(Modifier.width(132.dp).clickable{choose(reaction)},color=if(reaction.selected)MpAccent.copy(.12f)else MpSurface2,shape=RoundedCornerShape(20.dp),border=androidx.compose.foundation.BorderStroke(if(reaction.selected)2.dp else 1.dp,if(reaction.selected)MpAccent2 else MpLine)){Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){if(reaction.image.isNotBlank())MpImage(reaction.image,reaction.label,Modifier.size(66.dp),contentScale=ContentScale.Fit);Text(reaction.label,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=8.dp));if(reaction.source=="global")Text("PARA TODOS",color=MpAccent2,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=8.dp));Text(if(reaction.selected)"Sua reação • ${reaction.votes} votos" else "${reaction.votes} votos",color=MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=7.dp))}}}}}}}
@Composable private fun RatingPanel(rating:WorkRating,logged:Boolean,rate:(Int)->Unit){Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),color=MpSurface,shape=RoundedCornerShape(24.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){Column(Modifier.padding(20.dp)){Text("AVALIAÇÃO DOS LEITORES",color=MpAccent2,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelMedium);Row(verticalAlignment=Alignment.Bottom){Text("${"%.1f".format(Locale("pt","BR"),rating.average)}",fontWeight=FontWeight.Black,style=MaterialTheme.typography.displayMedium);Text("★",color=Color(0xffffc13d),style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(bottom=8.dp))};Text("${rating.total} ${if(rating.total==1)"avaliação" else "avaliações"} no total",color=MpMuted);Spacer(Modifier.height(14.dp));for(note in 5 downTo 1){val count=rating.counts[note]?:0;val part=if(rating.total==0)0f else count.toFloat()/rating.total;Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){Text("$note★",Modifier.width(42.dp));LinearProgressIndicator(progress={part},Modifier.weight(1f),color=Color(0xffffc13d),trackColor=MpSurface2);Text("$count",Modifier.width(32.dp),textAlign=androidx.compose.ui.text.style.TextAlign.End,color=MpMuted)}};HorizontalDivider(Modifier.padding(vertical=16.dp),color=MpLine);Text(if(rating.mine>0)"Sua avaliação: ${rating.mine} de 5"else"O que você achou desta história?",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text(if(logged)"Toque em uma estrela para avaliar. Você pode mudar quando quiser." else "Entre na conta para participar da avaliação.",color=MpMuted,style=MaterialTheme.typography.bodySmall);Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)){for(note in 1..5){OutlinedButton({rate(note)},enabled=logged,contentPadding=PaddingValues(0.dp),modifier=Modifier.weight(1f).height(48.dp),border=androidx.compose.foundation.BorderStroke(1.dp,if(note<=rating.mine)Color(0xffffc13d)else MpLine)){Text("★",color=if(note<=rating.mine)Color(0xffffc13d)else MpMuted)}}}}}}
@Composable private fun WorkTabs(selected:String,count:Int,change:(String)->Unit){Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=12.dp).clip(RoundedCornerShape(20.dp)).background(MpSurface2).padding(5.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("Sobre","Capítulos","Comentários").forEach{name->Surface(Modifier.weight(1f).clickable{change(name)},color=if(selected==name)MpAccent.copy(.15f)else Color.Transparent,shape=RoundedCornerShape(15.dp)){Column(Modifier.padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(if(name=="Capítulos")"Capítulos $count" else name,fontWeight=FontWeight.Bold,color=if(selected==name)MaterialTheme.colorScheme.onSurface else MpMuted);if(selected==name)Box(Modifier.padding(top=7.dp).width(58.dp).height(3.dp).background(MpAccent2,RoundedCornerShape(2.dp)))}}}}}
@Composable private fun SynopsisCard(work:Work){Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),color=MpSurface,shape=RoundedCornerShape(24.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){Column(Modifier.padding(20.dp)){Text("SINOPSE",color=MpAccent2,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelMedium);Text(work.synopsis.ifBlank{"Sinopse ainda não informada."},modifier=Modifier.padding(top=14.dp),lineHeight=28.sp);if(work.genres.isNotEmpty())LazyRow(Modifier.padding(top=18.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(work.genres){genre->Surface(color=MpAccent.copy(.1f),shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xff5b2b42))){Text(genre,Modifier.padding(horizontal=14.dp,vertical=8.dp),fontWeight=FontWeight.Bold)}}}}}}
@Composable
private fun WorkInformation(work: Work, lastUpdate: Long) {
    val rows = listOf(
        "TIPO" to localizedType(work.type),
        "STATUS" to localizedStatus(work.status),
        "AGENDA" to work.schedule,
        "ÚLTIMA ATUALIZAÇÃO" to formatWorkDate(lastUpdate),
        "AUTOR" to work.author,
        "ARTISTA" to work.artist,
        "SCAN" to work.scan,
        "IDIOMA" to work.language
    ).filter { it.second.isNotBlank() }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        rows.forEach { (label, value) ->
            Surface(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                color = MpSurface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (label == "ÚLTIMA ATUALIZAÇÃO") Color(0xff62263f) else MpLine
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(label, color = MpMuted, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                    Text(
                        value.ifBlank { "—" },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}
@Composable private fun DownloadAllCard(allDownloaded:Boolean,downloading:Boolean,progress:Int,message:String,error:String,enabled:Boolean,onDownload:()->Unit){Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp),color=if(allDownloaded)Color(0xff2eb98a).copy(.1f)else MpSurface,shape=RoundedCornerShape(24.dp),border=androidx.compose.foundation.BorderStroke(1.dp,if(allDownloaded)Color(0xff2eb98a)else MpLine)){Column(Modifier.padding(17.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(if(allDownloaded)Color(0xff1c473a)else MpAccent.copy(.18f)),contentAlignment=Alignment.Center){Text(if(allDownloaded)"✓" else "⇣",color=if(allDownloaded)Color(0xff62e6b8)else MpAccent,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)};Column(Modifier.weight(1f).padding(start=12.dp)){Text(if(allDownloaded)"Todos os capítulos baixados" else "Leitura offline",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium);Text(if(allDownloaded)"A obra inteira já está disponível sem internet." else "Baixe tudo ou escolha cada capítulo abaixo.",color=MpMuted,style=MaterialTheme.typography.bodySmall)}};Text("Para ler sem internet, abra Biblioteca → Downloads.",color=MpAccent2,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=12.dp));Button(onClick=onDownload,enabled=enabled&&!downloading,modifier=Modifier.fillMaxWidth().padding(top=14.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=if(allDownloaded)Color(0xff246b55)else MpAccent)){Text(when{allDownloaded->"✓ VERIFICAR DOWNLOADS";downloading->"BAIXANDO… $progress%";else->"⇣ BAIXAR TODOS OS CAPÍTULOS"},fontWeight=FontWeight.Black)};if(downloading){LinearProgressIndicator(progress={progress/100f},modifier=Modifier.fillMaxWidth().padding(top=10.dp),color=MpAccent2,trackColor=MpSurface2);Text("$progress% concluído",color=MpAccent2,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=6.dp))};if(message.isNotBlank())Text(message,color=if(error.isBlank())MpAccent2 else MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp));if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=4.dp))}}}




@Composable private fun ChapterRow(chapter:Chapter,readingProgress:ReadingProgress?,saved:Boolean,downloadProgress:Int?,failed:Boolean,badge:NewBadgeStyle,open:()->Unit,download:()->Unit){
 val fresh=isNewChapter(chapter)
 val accent=when{failed->MaterialTheme.colorScheme.error;saved->MpAccent2;else->MpAccent}
 Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),color=MpSurface,shape=RoundedCornerShape(24.dp),border=androidx.compose.foundation.BorderStroke(1.dp,if(saved)MpAccent2.copy(.35f)else MpLine)){
  Column(Modifier.padding(16.dp)){
   Row(Modifier.fillMaxWidth().clickable(onClick=open),verticalAlignment=Alignment.CenterVertically){
    Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(accent.copy(.22f),accent.copy(.08f)))),contentAlignment=Alignment.Center){
     Text(chapter.number?.toString()?.removeSuffix(".0")?:"—",color=accent,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge,maxLines=1)
    }
    Column(Modifier.weight(1f).padding(start=12.dp)){
     Text(chapter.label,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
     if(chapter.subtitle.isNotBlank())Text(chapter.subtitle,color=MpMuted,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=3.dp))
     Text(formatWorkDate(maxOf(chapter.updatedAt,chapter.createdAt,chapter.scheduledAt)),color=MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=5.dp))
    }
    if(fresh&&badge.enabled)AdminNewBadge(badge)
   }
   Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
    Surface(color=accent.copy(.12f),shape=RoundedCornerShape(8.dp)){
     Text(when{saved->"✓ Offline";downloadProgress!=null->"Baixando · $downloadProgress%";failed->"Download falhou";readingProgress!=null&&readingProgress.percent>=100->"✓ Lido";readingProgress!=null->"Lendo · ${readingProgress.percent}%";else->"Pronto para ler"},color=accent,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(horizontal=9.dp,vertical=6.dp))
    }
   }
   Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Button(open,Modifier.weight(1f),shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(horizontal=8.dp,vertical=12.dp)){
     Text(if(readingProgress!=null&&readingProgress.percent in 1..99)"Continuar" else "Ler agora",fontWeight=FontWeight.Bold)
    }
    if(!saved)OutlinedButton(download,Modifier.weight(1f),enabled=downloadProgress==null,shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(horizontal=8.dp,vertical=12.dp)){
     Text(if(downloadProgress!=null)"$downloadProgress%" else if(failed)"Tentar baixar" else "↓ Baixar")
    }
   }
   if(downloadProgress!=null)LinearProgressIndicator(progress={downloadProgress.coerceIn(0,100)/100f},modifier=Modifier.fillMaxWidth().padding(top=12.dp),color=MpAccent2,trackColor=MpSurface2)
   else if(readingProgress!=null&&readingProgress.percent in 1..99)LinearProgressIndicator(progress={readingProgress.percent/100f},modifier=Modifier.fillMaxWidth().padding(top=12.dp),color=MpAccent,trackColor=MpSurface2)
  }
 }
}
@Composable private fun AdminNewBadge(style:NewBadgeStyle){
 fun badgeColor(value:String,fallback:Color):Color{return try{Color(android.graphics.Color.parseColor(value))}catch(_:Exception){fallback}}
 val bg1=badgeColor(style.bgColor,MpAccent)
 val bg2=badgeColor(style.bgColor2,MpAccent2)
 val foreground=badgeColor(style.textColor,Color.White)
 val brush=if(style.backgroundMode=="transparent")Brush.linearGradient(listOf(Color.Transparent,Color.Transparent)) else Brush.linearGradient(listOf(bg1,bg2))
 val modifier=Modifier.height((24*style.size/100f).dp).widthIn(min=(48*style.size/100f).dp,max=120.dp).clip(RoundedCornerShape(style.radius.coerceAtMost(40).dp)).background(brush)
 Box(modifier,contentAlignment=when(style.textPosition){"left"->Alignment.CenterStart;"right"->Alignment.CenterEnd;else->Alignment.Center}){
  if(style.imageUrl.isNotBlank()&&style.backgroundMode in listOf("both","image"))MpImage(style.imageUrl,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
  Text(style.text,Modifier.padding(horizontal=8.dp),color=foreground,fontWeight=if(style.fontWeight>=700)FontWeight.Black else FontWeight.SemiBold,fontSize=(10*style.fontSize/100f).sp,maxLines=1)
 }
}
private fun localizedType(value:String)=when(value.trim().lowercase(Locale.ROOT)){"manga","mangá"->"Mangá";"manhwa"->"Manhwa";"manhua"->"Manhua";"novel"->"Novel";"webtoon"->"Webtoon";"oneshot","one-shot"->"One-shot";"hq"->"HQ";else->value.ifBlank{"—"}}
private fun isNewChapter(chapter:Chapter)=ChapterMetadata.isNew(chapter)
private fun normalizeMillis(value:Long)=if(value in 1..99_999_999_999L)value*1000 else value
private fun formatWorkDate(value:Long)=if(value<=0)"—" else SimpleDateFormat("dd/MM/yyyy",Locale("pt","BR")).format(Date(normalizeMillis(value)))
private fun localizedStatus(value:String):String=when(value.trim().lowercase(Locale.ROOT).replace("_"," ").replace("-"," ")){
 "ongoing","in progress","andamento","em andamento"->"Em andamento"
 "completed","complete","completo","completa","finalizado","finalizada"->"Completa"
 "hiatus","paused","pause","em pausa","pausado","pausada"->"Em pausa"
 "future","upcoming","futuro","futura"->"Futura"
 "cancelled","canceled","cancelado","cancelada"->"Cancelada"
 else->value.ifBlank{"Status não informado"}
}
@Composable private fun Reader(work:Work,chapter:Chapter,back:()->Unit){
 val context=LocalContext.current;val store=remember{OfflineStore(context.applicationContext)};val readingStore=remember{ReadingStore(context.applicationContext)};val scope=rememberCoroutineScope();val listState=rememberLazyListState()
 var current by remember(work.id,chapter.id){mutableStateOf(chapter)}
 var chapters by remember(work.id){mutableStateOf<List<Chapter>>(emptyList())}
 var listOpen by remember{mutableStateOf(false)}
 var pages by remember(current.id){mutableStateOf<List<String>>(emptyList())};var remotePages by remember(current.id){mutableStateOf<List<String>>(emptyList())};var loading by remember(current.id){mutableStateOf(true)};var failed by remember(current.id){mutableStateOf(false)};var offline by remember(current.id){mutableStateOf(false)};var downloading by remember(current.id){mutableStateOf(false)};var progress by remember(current.id){mutableIntStateOf(0)};var downloadError by remember(current.id){mutableStateOf("")}
 LaunchedEffect(work.id){val saved=store.downloads().filter{it.workId==work.id}.map{it.toChapter()};chapters=runCatching{check(isConnected(context));(CatalogRepository().chapters(work.id).filter{it.available}+saved).distinctBy{it.id}.sortedBy{it.number?:Double.MAX_VALUE}}.getOrDefault(saved.sortedBy{it.number?:Double.MAX_VALUE});if(chapters.none{it.id==current.id})chapters=(chapters+current).distinctBy{it.id}.sortedBy{it.number?:Double.MAX_VALUE}}
 LaunchedEffect(current.id){if(!current.available){loading=false;return@LaunchedEffect};val saved=store.localPages(work.id,current.id);if(saved.isNotEmpty()){pages=saved;offline=true;loading=false}else{runCatching{CatalogRepository().pages(work.id,current.id)}.onSuccess{remotePages=it;pages=it}.onFailure{failed=true};loading=false};if(pages.isNotEmpty()){val last=readingStore.progress(work.id,current.id)?.page?.minus(1)?.coerceIn(0,pages.lastIndex)?:0;listState.scrollToItem(last)}}
 LaunchedEffect(work.id,current.id){
  WorkManager.getInstance(context.applicationContext).getWorkInfosByTagFlow("work-download-${work.id}").catch{downloadError="Não foi possível acompanhar o download. Abra o capítulo novamente.";downloading=false}.collect{infos->
   val saved=withContext(Dispatchers.IO){store.localPages(work.id,current.id)}
   if(saved.isNotEmpty()){pages=saved;offline=true;downloading=false;downloadError=""}
   else{
    val name=ChapterDownloadWorker.uniqueName(work.id,current.id)
    val active=infos.firstOrNull{!it.state.isFinished&&(name in it.tags||"work-download-all-${work.id}" in it.tags)}
    downloading=active!=null
    progress=if(active?.progress?.getString(ChapterDownloadWorker.CHAPTER_ID)==current.id)active.progress.getInt(ChapterDownloadWorker.CHAPTER_PROGRESS,0) else active?.progress?.getInt(ChapterDownloadWorker.PROGRESS,0)?:0
    if(active==null)downloadError=infos.firstOrNull{name in it.tags&&it.state==androidx.work.WorkInfo.State.FAILED}?.outputData?.getString(ChapterDownloadWorker.ERROR).orEmpty()
   }
  }
 }
 LaunchedEffect(listState,pages.size,current.id){if(pages.isNotEmpty())snapshotFlow{listState.firstVisibleItemIndex}.distinctUntilChanged().collect{index->readingStore.save(work,current,index+1,pages.size)}}
 val position=chapters.indexOfFirst{it.id==current.id};val previous=chapters.getOrNull(position-1);val next=chapters.getOrNull(position+1)
 if(listOpen)ChapterListDialog(chapters,current.id,store,work.id,{listOpen=false}){current=it;listOpen=false}

 val prefs=remember{context.getSharedPreferences("mp_reader",0)}
 var controls by remember(current.id){mutableStateOf(true)}
 var commentsOpen by remember{mutableStateOf(false)}
 var settingsOpen by remember{mutableStateOf(false)}
 var autoScroll by remember{mutableStateOf(false)}
 var speed by remember{mutableFloatStateOf(prefs.getFloat("speed",50f))}
 var width by remember{mutableFloatStateOf(prefs.getFloat("width",1f))}
 var zoom by remember{mutableFloatStateOf(prefs.getFloat("zoom",1f))}
 var brightness by remember{mutableFloatStateOf(prefs.getFloat("brightness",-1f))}
 var fullScreen by remember{mutableStateOf(prefs.getBoolean("fullscreen",false))}
 var readerBackground by remember{mutableStateOf(prefs.getString("background","theme")?:"theme")}
 val animationMillis=if(online.mpscan.app.data.SettingsStore(context).animations)240 else 0
 val scheme=MaterialTheme.colorScheme
 val readerColor=when(readerBackground){"white"->Color.White;"black"->Color.Black;"sepia"->Color(0xFFF3E6CE);else->MaterialTheme.colorScheme.background}
 val window=(context as? android.app.Activity)?.window
 DisposableEffect(brightness,fullScreen){
  val original=window?.attributes?.screenBrightness?:-1f
  window?.let{w->val attributes=w.attributes;attributes.screenBrightness=brightness;w.attributes=attributes
   val controller=androidx.core.view.WindowCompat.getInsetsController(w,w.decorView)
   controller.systemBarsBehavior=androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
   if(fullScreen)controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())else controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
  }
  onDispose{window?.let{w->val attributes=w.attributes;attributes.screenBrightness=original;w.attributes=attributes;androidx.core.view.WindowCompat.getInsetsController(w,w.decorView).show(androidx.core.view.WindowInsetsCompat.Type.systemBars())}}
 }
 LaunchedEffect(current.id,listState){
  var lastIndex=listState.firstVisibleItemIndex;var lastOffset=listState.firstVisibleItemScrollOffset
  snapshotFlow{listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset}.collect{(index,offset)->
   if(listState.isScrollInProgress&&!commentsOpen&&!settingsOpen){
    if(index>lastIndex||(index==lastIndex&&offset-lastOffset>3))controls=false
    else if(index<lastIndex||(index==lastIndex&&lastOffset-offset>3))controls=true
   }
   lastIndex=index;lastOffset=offset
  }
 }
 LaunchedEffect(autoScroll,speed,loading,current.id,commentsOpen,settingsOpen,listOpen){
  if(autoScroll&&!loading&&!commentsOpen&&!settingsOpen&&!listOpen){controls=false;while(true){delay(16);if(!listState.canScrollForward){autoScroll=false;controls=true;break};listState.scrollBy(speed*0.016f)}}
 }
 BackHandler(commentsOpen||settingsOpen){commentsOpen=false;settingsOpen=false}
 if(commentsOpen)Dialog(onDismissRequest={commentsOpen=false},properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.fillMaxWidth(.94f).fillMaxHeight(.85f).widthIn(max=720.dp),shape=RoundedCornerShape(26.dp),color=MpSurface){
   Column{Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Comentários",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text(current.label,color=MpMuted)};TextButton({commentsOpen=false}){Text("Fechar")}}
    LazyColumn(Modifier.weight(1f)){item{CommentsSection("capitulo",work.id,current.id,Modifier.fillMaxWidth().padding(16.dp))}}
   }
  }
 }
 if(settingsOpen)Dialog(onDismissRequest={settingsOpen=false},properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.fillMaxWidth(.94f).widthIn(max=560.dp).heightIn(max=640.dp),shape=RoundedCornerShape(26.dp),color=MpSurface){
   Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Text("Sua leitura, seu ritmo",Modifier.weight(1f),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);TextButton({settingsOpen=false}){Text("Fechar")}}
    Text("Ajuste para ficar confortável",color=MpMuted)
    Row(Modifier.fillMaxWidth().padding(top=18.dp),verticalAlignment=Alignment.CenterVertically){Text("Rolagem automática",Modifier.weight(1f));Switch(autoScroll,{autoScroll=it})}
    Text("Velocidade · ${speed.toInt()} px/s",color=MpMuted);Slider(speed,{speed=it;prefs.edit().putFloat("speed",it).apply()},valueRange=10f..180f)
    Text("Largura · ${(width*100).toInt()}%",fontWeight=FontWeight.SemiBold);Slider(width,{width=it;prefs.edit().putFloat("width",it).apply()},valueRange=.55f..1f)
    Text("Zoom · ${(zoom*100).toInt()}%",fontWeight=FontWeight.SemiBold);Slider(zoom,{zoom=it;prefs.edit().putFloat("zoom",it).apply()},valueRange=1f..2f)
    Text("Brilho",fontWeight=FontWeight.SemiBold);Slider(if(brightness<0).5f else brightness,{brightness=it;prefs.edit().putFloat("brightness",it).apply()},valueRange=.05f..1f)
    TextButton({brightness=-1f;prefs.edit().putFloat("brightness",-1f).apply()}){Text("Usar brilho do aparelho")}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Tela cheia",Modifier.weight(1f));Switch(fullScreen,{fullScreen=it;prefs.edit().putBoolean("fullscreen",it).apply()})}
    if(!offline&&pages.isNotEmpty())OutlinedButton({val chapterToSave=current;scope.launch{ChapterDownloadWorker.enqueue(context.applicationContext,work,chapterToSave)}},enabled=!downloading,modifier=Modifier.fillMaxWidth()){Text(if(downloading)"Baixando · $progress%" else "↓ Salvar capítulo offline")}
    if(downloadError.isNotBlank())Text(downloadError,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(vertical=8.dp))
    Text("Fundo da leitura",fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=10.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
     listOf("theme" to "Tema","white" to "Branco","black" to "Preto","sepia" to "Sépia").forEach{(id,label)->FilterChip(readerBackground==id,{readerBackground=id;prefs.edit().putString("background",id).apply()},{Text(label)})}
    }
    Text("Deslize para baixo para ocultar as barras. Suba ou toque duas vezes para mostrá-las.",color=MpMuted,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=14.dp))
   }
  }
 }
 Box(Modifier.fillMaxSize().background(readerColor)){
  when{
   !current.available->Message("Capítulo agendado. Disponível em "+formatScheduled(current.scheduledAt))
   loading->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
   failed->Message("Não foi possível abrir o capítulo. Confira sua conexão ou escolha um capítulo salvo offline.")
   pages.isEmpty()->Message("Nenhuma página encontrada.")
   else->LazyColumn(Modifier.fillMaxSize().pointerInput(current.id){detectTapGestures(onDoubleTap={controls=true})},state=listState,horizontalAlignment=Alignment.CenterHorizontally){
    items(pages){page->BoxWithConstraints(Modifier.fillMaxWidth(width).widthIn(max=1000.dp)){val pageWidth=maxWidth*zoom;Box(Modifier.horizontalScroll(rememberScrollState())){Box(Modifier.width(pageWidth)){ReaderPage(page)}}}}
   }
  }
  AnimatedVisibility(controls,Modifier.align(Alignment.TopCenter).padding(horizontal=12.dp,vertical=12.dp),enter=fadeIn(tween(animationMillis))+slideInVertically(tween(animationMillis)){ -it/2 },exit=fadeOut(tween(animationMillis))+slideOutVertically(tween(animationMillis)){ -it/2 }){
   Surface(Modifier.fillMaxWidth().widthIn(max=850.dp),color=scheme.surface.copy(alpha=.90f),shape=RoundedCornerShape(22.dp),border=androidx.compose.foundation.BorderStroke(1.dp,scheme.outline.copy(alpha=.35f)),shadowElevation=6.dp){
    Row(Modifier.padding(horizontal=6.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
     IconButton(back){Text("‹",fontSize=30.sp)}
     Column(Modifier.weight(1f)){Text(work.title,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${current.label} · ${(listState.firstVisibleItemIndex+1).coerceAtMost(pages.size)} / ${pages.size}"+(if(offline)" · Offline" else ""),color=MpMuted,style=MaterialTheme.typography.labelSmall)}
     IconButton({commentsOpen=true}){ReaderGlyph("comments",MpText)}
     IconButton({settingsOpen=true}){ReaderGlyph("settings",MpText)}
    }
   }
  }
  AnimatedVisibility(controls,Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp),enter=fadeIn(tween(animationMillis))+slideInVertically(tween(animationMillis)){it/2},exit=fadeOut(tween(animationMillis))+slideOutVertically(tween(animationMillis)){it/2}){
   Surface(Modifier.widthIn(max=480.dp).fillMaxWidth(),color=scheme.surface.copy(alpha=.90f),shape=RoundedCornerShape(22.dp),border=androidx.compose.foundation.BorderStroke(1.dp,scheme.outline.copy(alpha=.35f)),shadowElevation=6.dp){
    Row(Modifier.padding(8.dp),horizontalArrangement=Arrangement.SpaceEvenly){
     ReaderNavAction("Anterior","‹",previous!=null){previous?.let{current=it;controls=true;autoScroll=false}}
     ReaderNavAction("Capítulos","☰",true){listOpen=true;autoScroll=false}
     ReaderNavAction("Obra","⌂",true,back)
     ReaderNavAction("Próximo","›",next!=null){next?.let{current=it;controls=true;autoScroll=false}}
    }
   }
  }
 }
}
@Composable private fun ReaderNavAction(label:String,icon:String,enabled:Boolean,action:()->Unit){
 TextButton(action,enabled=enabled,contentPadding=PaddingValues(horizontal=6.dp,vertical=6.dp)){
  Column(horizontalAlignment=Alignment.CenterHorizontally){Text(icon,fontSize=24.sp);Text(label,style=MaterialTheme.typography.labelSmall)}
 }
}
@Composable private fun ReaderGlyph(kind:String,color:Color){
 androidx.compose.foundation.Canvas(Modifier.size(23.dp)){
  val stroke=androidx.compose.ui.graphics.drawscope.Stroke(width=2.dp.toPx())
  if(kind=="comments"){
   drawRoundRect(color,topLeft=androidx.compose.ui.geometry.Offset(size.width*.1f,size.height*.1f),size=androidx.compose.ui.geometry.Size(size.width*.8f,size.height*.6f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),style=stroke)
   drawLine(color,androidx.compose.ui.geometry.Offset(size.width*.2f,size.height*.7f),androidx.compose.ui.geometry.Offset(size.width*.2f,size.height*.95f),stroke.width)
   drawLine(color,androidx.compose.ui.geometry.Offset(size.width*.2f,size.height*.95f),androidx.compose.ui.geometry.Offset(size.width*.45f,size.height*.7f),stroke.width)
  }else{
   listOf(.2f,.5f,.8f).forEachIndexed{i,y->drawLine(color,androidx.compose.ui.geometry.Offset(0f,size.height*y),androidx.compose.ui.geometry.Offset(size.width,size.height*y),stroke.width);drawCircle(color,3.dp.toPx(),androidx.compose.ui.geometry.Offset(size.width*(if(i==1).3f else .7f),size.height*y))}
  }
 }
}
@Composable private fun ChapterNavigation(previous:Chapter?,next:Chapter?,openList:()->Unit,open:(Chapter)->Unit){
 Surface(Modifier.fillMaxWidth().padding(16.dp),color=MpSurface,shape=RoundedCornerShape(24.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){
  Column(Modifier.padding(18.dp)){
   Text(if(next!=null)"A história continua" else "Você chegou ao último capítulo",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
   Text(next?.label?:"Escolha outro capítulo para reler.",color=MpMuted,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=4.dp))
   if(next!=null)Button({open(next)},Modifier.fillMaxWidth().padding(top=14.dp),shape=RoundedCornerShape(14.dp)){Text("Próximo capítulo →",fontWeight=FontWeight.Bold)}
   Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedButton({previous?.let(open)},Modifier.weight(1f),enabled=previous!=null,shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(horizontal=8.dp,vertical=10.dp)){Text("← Anterior")}
    OutlinedButton(openList,Modifier.weight(1f),shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(horizontal=8.dp,vertical=10.dp)){Text("Ver capítulos")}
   }
  }
 }
}
@Composable private fun ChapterListDialog(chapters:List<Chapter>,selectedId:String,store:OfflineStore,workId:String,close:()->Unit,open:(Chapter)->Unit){
 AlertDialog(onDismissRequest=close,title={Text("Lista de capítulos")},text={if(chapters.isEmpty())Text("Nenhum capítulo disponível offline.",color=MpMuted)else LazyColumn(Modifier.heightIn(max=480.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(chapters,key={it.id}){item->val saved=store.localPages(workId,item.id).isNotEmpty();Surface(Modifier.fillMaxWidth().clickable{open(item)},color=if(item.id==selectedId)MpAccent.copy(.18f)else MpSurface2,shape=RoundedCornerShape(14.dp),border=androidx.compose.foundation.BorderStroke(1.dp,if(item.id==selectedId)MpAccent else MpLine)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.label,fontWeight=FontWeight.Bold);if(item.subtitle.isNotBlank())Text(item.subtitle,color=MpMuted,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)};if(saved)Text("OFFLINE",color=MpAccent2,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall)}}}}},confirmButton={TextButton(close){Text("Fechar")}})
}
@Composable private fun Message(text:String){Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Text(text,color=MpMuted)}}

@Composable private fun ReaderPage(source:String){
 var ratio by remember(source){mutableFloatStateOf(0.7f)}
 var failed by remember(source){mutableStateOf(false)}
 var attempt by remember(source){mutableIntStateOf(0)}
 val context=LocalContext.current
 Column(Modifier.fillMaxWidth()){
  key(attempt){MpImage(coil3.request.ImageRequest.Builder(context).data(if(source.startsWith("file:"))java.io.File(java.net.URI(source))else source).build(),"Página do capítulo",Modifier.fillMaxWidth().aspectRatio(ratio),contentScale=ContentScale.FillWidth,
   onSuccess={result->val image=result.result.image;if(image.width>0&&image.height>0)ratio=image.width.toFloat()/image.height;failed=false},onError={failed=true})}
  if(failed)OutlinedButton({attempt++;failed=false},Modifier.fillMaxWidth().padding(12.dp)){Text("Esta página não carregou. Toque para tentar novamente.")}
 }
}
private fun formatScheduled(value:Long):String=SimpleDateFormat("dd/MM/yyyy 'às' HH:mm",Locale("pt","BR")).apply{timeZone=java.util.TimeZone.getTimeZone("America/Sao_Paulo")}.format(Date(value))
@Composable private fun ScheduledChapterRow(chapter:Chapter){
 Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),color=MpSurface,shape=RoundedCornerShape(20.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){
  Column(Modifier.padding(18.dp)){Text("◷ ${chapter.label} · Agendado",fontWeight=FontWeight.Bold,color=MpAccent2);Text("Disponível em ${formatScheduled(chapter.scheduledAt)}",color=MpMuted,modifier=Modifier.padding(top=6.dp));Text("A leitura e o download serão liberados no horário marcado.",color=MpMuted,style=MaterialTheme.typography.bodySmall)}
 }
}

private fun isConnected(context:android.content.Context):Boolean { val manager=context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager;val capabilities=manager.getNetworkCapabilities(manager.activeNetwork)?:return false;return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)&&capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) }
