package online.mpscan.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*

/** One lazy item per image region, so even a 30,000 px page never becomes one huge bitmap. */
@Composable fun ReaderImages(sources:List<String>,state:LazyListState,width:Float,zoom:Float,online:Boolean,modifier:Modifier=Modifier,repair:suspend(Int)->String?){
 val context=LocalContext.current.applicationContext;val scope=rememberCoroutineScope()
 val assets=remember(sources){mutableStateMapOf<Int,PageAsset>()};val errors=remember(sources){mutableStateMapOf<Int,String>()};val jobs=remember(sources){mutableMapOf<Int,Job>()};val decoderRecovery=remember(sources){mutableSetOf<Int>()}
 val latestRepair by rememberUpdatedState(repair);val connected by rememberUpdatedState(online)
 fun load(index:Int,force:Boolean=false){
  if(index !in sources.indices||jobs[index]?.isActive==true||(!force&&(assets.containsKey(index)||errors.containsKey(index))))return
  if(force){assets.remove(index);errors.remove(index)}
  jobs[index]=scope.launch{
   try{
    val asset=try{PageFiles.fetch(context,sources[index])}catch(e:CancellationException){throw e}catch(e:Exception){
     if(sources[index].startsWith("file:")&&connected){val restored=latestRepair(index)?:throw e;PageFiles.fetch(context,restored)}else throw e
    }
    assets[index]=asset;errors.remove(index)
   }catch(e:CancellationException){throw e}catch(e:Exception){errors[index]=if(connected)"Não foi possível carregar a página ${index+1}. Tente novamente."else"A página ${index+1} ainda não está salva neste aparelho. Conecte à internet para restaurá-la."}
  }
 }
 DisposableEffect(sources){onDispose{jobs.values.forEach{it.cancel()}}}
 // Start the opening page independently of the first lazy-layout observation.
 LaunchedEffect(sources){if(sources.isNotEmpty())load(0)}
 LaunchedEffect(sources,state){snapshotFlow{state.layoutInfo.visibleItemsInfo.mapNotNull{PageTiles.sourceIndex(it.key)}}.distinctUntilChanged().collect{visible->
  val first=visible.minOrNull()?:0;val last=visible.maxOrNull()?:first
  (first..minOf(sources.lastIndex,last+2)).forEach{load(it)}
 }}
 LaunchedEffect(online){if(online)errors.keys.toList().forEach{load(it,true)}}
 LazyColumn(modifier,state=state,horizontalAlignment=Alignment.CenterHorizontally){
  sources.forEachIndexed{index,_->
   val asset=assets[index]
   if(asset==null)item(key="page:$index:0"){
    Column(Modifier.fillMaxWidth(width).heightIn(min=180.dp).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
     val error=errors[index]
     if(error==null){CircularProgressIndicator();Text("Preparando página ${index+1}…",color=MpMuted,modifier=Modifier.padding(top=12.dp))}
     else{Text(error,color=MpMuted);OutlinedButton({decoderRecovery.remove(index);load(index,true)},Modifier.padding(top=12.dp)){Text(if(sources[index].startsWith("file:"))"Restaurar página"else"Carregar novamente")}}
    }
   }else if(asset.text!=null){item(key="page:$index:0"){androidx.compose.foundation.text.selection.SelectionContainer{Text(asset.text,Modifier.fillMaxWidth(width).widthIn(max=780.dp).padding(horizontal=22.dp,vertical=12.dp).testTag("reader-text-$index"),fontSize=(18f*zoom.coerceIn(.8f,1.6f)).sp,lineHeight=(30f*zoom.coerceIn(.8f,1.6f)).sp,color=MaterialTheme.colorScheme.onBackground)}}
   }else{
    PageTiles.plan(asset.width,asset.height).forEachIndexed{tileIndex,tile->item(key="page:$index:$tileIndex"){
     ReaderTile(asset,tile,width,zoom){
      if(decoderRecovery.add(index)){
       if(!sources[index].startsWith("file:"))asset.file.delete()
       load(index,true)
      }else{assets.remove(index);errors[index]="Não foi possível abrir a página ${index+1}. Conecte à internet para restaurá-la."}
     }
    }}
   }
  }
 }
}
@Composable private fun ReaderTile(asset:PageAsset,tile:PageTiles.Tile,width:Float,zoom:Float,failed:()->Unit){
 val density=LocalDensity.current
 BoxWithConstraints(Modifier.fillMaxWidth(width).widthIn(max=1000.dp)){
  val pageWidth=maxWidth*zoom;val pixels=with(density){pageWidth.roundToPx().coerceIn(1,2400)}
  var bitmap by remember(asset,tile,pixels){mutableStateOf<android.graphics.Bitmap?>(null)}
  val latestFailed by rememberUpdatedState(failed)
  LaunchedEffect(asset,tile,pixels){try{bitmap=PageFiles.bitmap(asset,tile,pixels)}catch(e:CancellationException){throw e}catch(e:Exception){latestFailed()}}
  Box(Modifier.horizontalScroll(rememberScrollState())){
   Box(Modifier.width(pageWidth).aspectRatio(asset.width.toFloat()/tile.height)){
    bitmap?.let{Image(it.asImageBitmap(),"Página do capítulo",Modifier.fillMaxSize().testTag("reader-tile-${tile.top}"),contentScale=ContentScale.FillBounds)}
    if(bitmap==null)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(Modifier.size(24.dp))}
   }
  }
 }
}
