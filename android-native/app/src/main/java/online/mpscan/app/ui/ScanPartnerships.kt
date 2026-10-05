package online.mpscan.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*

@Composable fun ScanPartnerships(openWork:(Work)->Unit,back:()->Unit){
 val context=LocalContext.current;val uri=LocalUriHandler.current;val scope=rememberCoroutineScope();val online=networkAvailable()
 var partners by remember{mutableStateOf<List<ScanPartnership>>(emptyList())};var selected by remember{mutableStateOf<ScanPartnership?>(null)}
 var administrator by remember{mutableStateOf(false)};var editing by remember{mutableStateOf<ScanPartnership?>(null)};var retry by remember{mutableIntStateOf(0)};var loading by remember{mutableStateOf(true)};var message by remember{mutableStateOf("")};var saving by remember{mutableStateOf(false)}
 LaunchedEffect(online,retry){loading=true;partners=ExternalCatalog.partners();loading=false;administrator=false
  if(online)try{AccountStore(context).session()?.let{old->val fresh=AccountRepository().refresh(old);AccountStore(context).save(fresh);val profile=AccountRepository().profile(fresh);administrator=profile.role.lowercase() in listOf("adm","admin","administrador")}}catch(e:CancellationException){throw e}catch(e:Exception){}
 }
 editing?.let{partner->PartnershipEditor(partner,saving,message,{if(!saving)editing=null}){updated->scope.launch{saving=true;try{ExternalCatalog.save(context,updated);message="Parceria salva. O endereço completo será usado como filtro.";editing=null;if(selected?.id==updated.id)selected=updated.takeIf{it.enabled};retry++}catch(e:CancellationException){throw e}catch(e:Exception){message=PublicErrors.message(e,"Não foi possível salvar a parceria.")}finally{saving=false}}}}
 selected?.let{partner->
  BackHandler{selected=null}
  PartnerCatalog(partner,administrator,{selected=null},{message="";editing=partner},openWork)
  return
 }
 BackHandler(onBack=back)
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(top=18.dp,bottom=32.dp)){
  item{TextButton(back){Text("← Menu")};Text("Parcerias scan",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium);Text("Outras scans, com suas próprias obras e atualizações.",color=MpMuted,modifier=Modifier.padding(top=8.dp))}
  item{Surface(color=MpSurface,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(18.dp)){Text("Sua próxima leitura também pode vir de uma parceira",fontWeight=FontWeight.Bold);Text("Cada página tem seu catálogo separado. Downloads completos ficam disponíveis na Biblioteca, mesmo sem internet.",color=MpMuted,modifier=Modifier.padding(top=8.dp))}}}
  if(administrator)item{Button({message="";editing=ScanPartnership(java.util.UUID.randomUUID().toString(),"","")},Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text("＋ Cadastrar parceria")}}
  if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
  if(message.isNotBlank())item{Text(message,color=MpAccent2)}
  items(partners,key={it.id}){partner->Surface(Modifier.fillMaxWidth().clickable{selected=partner},color=MpSurface,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MpLine)){Column{
   if(partner.cover.isNotBlank())MpImage(partner.cover,partner.name,Modifier.fillMaxWidth().height(140.dp),contentScale=ContentScale.Crop)
   Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){if(partner.photo.isNotBlank())MpImage(partner.photo,partner.name,Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)),contentScale=ContentScale.Crop)else Text("◈",color=MpAccent2,style=MaterialTheme.typography.headlineMedium);Column(Modifier.weight(1f).padding(start=14.dp)){Text(partner.name,fontWeight=FontWeight.Black);if(partner.handle.isNotBlank())Text("@"+partner.handle.removePrefix("@"),color=MpAccent2);Text(partner.description.ifBlank{"Catálogo da página cadastrada"},color=MpMuted,style=MaterialTheme.typography.bodySmall)};Text("›",color=MpAccent2)}
  }}}
  item{OutlinedButton({retry++},Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text("Atualizar parcerias")}}
 }
}
@Composable private fun PartnerCatalog(partner:ScanPartnership,admin:Boolean,back:()->Unit,edit:()->Unit,openWork:(Work)->Unit){
 val uri=LocalUriHandler.current;val online=networkAvailable()
 var works by remember(partner){mutableStateOf(ExternalCatalog.cachedCatalog(partner.id))};var retry by remember{mutableIntStateOf(0)};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf("")};var query by remember{mutableStateOf("")}
 LaunchedEffect(partner,online,retry){loading=true;error="";if(online){try{works=ExternalCatalog.catalog(partner)}catch(e:CancellationException){throw e}catch(e:Exception){error=PublicErrors.message(e,"Não foi possível carregar esta página parceira. Confira sua conexão.")}}else error="Você está offline. Capítulos já baixados continuam na Biblioteca.";loading=false}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),contentPadding=PaddingValues(top=18.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{TextButton(back){Text("← Parcerias scan")};if(partner.cover.isNotBlank())MpImage(partner.cover,partner.name,Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(24.dp)),contentScale=ContentScale.Crop);Text(partner.name,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(top=10.dp));Text(partner.description,color=MpMuted);Text("Somente as obras da página cadastrada",color=MpAccent2,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=8.dp))}
  if(admin)item{OutlinedButton(edit,Modifier.fillMaxWidth()){Text("Editar parceria")}}
  item{OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("Buscar nesta parceria")},singleLine=true,shape=RoundedCornerShape(18.dp))}
  if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
  if(error.isNotBlank())item{Surface(color=MpSurface,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(18.dp)){Text(error,color=MpMuted);if(works.isNotEmpty())Text("Mostrando o último catálogo carregado.",color=MpAccent2,modifier=Modifier.padding(top=8.dp));Row{TextButton({retry++}){Text("Tentar novamente")};TextButton({uri.openUri(partner.url)}){Text("Abrir página original")}}}}}
  if(!loading&&error.isBlank()&&works.isEmpty())item{Text("Esta página ainda não apresenta obras.",color=MpMuted)}
  val filtered=works.filter{query.isBlank()||it.title.contains(query,true)||it.author.contains(query,true)||it.synopsis.contains(query,true)}
  items(filtered.chunked(2)){row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{work->Surface(Modifier.weight(1f).clickable{openWork(work)},color=MpSurface,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,MpLine)){Column{MpImage(work.cover,work.title,Modifier.fillMaxWidth().aspectRatio(.72f),contentScale=ContentScale.Crop);Text(work.title,fontWeight=FontWeight.Bold,modifier=Modifier.padding(12.dp),maxLines=3)}}};if(row.size==1)Spacer(Modifier.weight(1f))}}
 }
}
@Composable private fun PartnershipEditor(initial:ScanPartnership,saving:Boolean,serverError:String,close:()->Unit,save:(ScanPartnership)->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 var name by remember(initial){mutableStateOf(initial.name)};var url by remember(initial){mutableStateOf(initial.url)};var cover by remember(initial){mutableStateOf(initial.cover)};var photo by remember(initial){mutableStateOf(initial.photo)};var handle by remember(initial){mutableStateOf(initial.handle)};var description by remember(initial){mutableStateOf(initial.description)};var enabled by remember(initial){mutableStateOf(initial.enabled)};var photoTarget by remember{mutableStateOf(false)};var localError by remember{mutableStateOf("")};var preparing by remember{mutableStateOf(false)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){value->if(value!=null)scope.launch{preparing=true;try{val encoded=withContext(Dispatchers.IO){encodePartnerImage(context,value)};if(photoTarget)photo=encoded else cover=encoded}catch(e:Exception){localError="Não foi possível preparar esta imagem. Escolha outra foto."};preparing=false}}
 AlertDialog(onDismissRequest=close,title={Text(if(initial.name.isBlank())"Nova parceria"else"Editar parceria")},text={LazyColumn(Modifier.heightIn(max=460.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Text("O endereço completo define quais obras entram. Uma categoria importa apenas aquela categoria.",color=MpMuted)}
  item{OutlinedTextField(name,{name=it.take(100)},label={Text("Nome da scan")},singleLine=true)}
  item{OutlinedTextField(url,{url=it.take(2048)},label={Text("Página do catálogo")},placeholder={Text("https://site.com/categoria/scan/")},singleLine=true)}
  item{if(cover.isNotBlank())MpImage(cover,"Capa da parceria",Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp)),contentScale=ContentScale.Crop);OutlinedButton({photoTarget=false;picker.launch("image/*")},enabled=!preparing){Text("Escolher capa")}}
  item{if(photo.isNotBlank())MpImage(photo,"Foto da scan",Modifier.size(60.dp).clip(RoundedCornerShape(20.dp)),contentScale=ContentScale.Crop);OutlinedButton({photoTarget=true;picker.launch("image/*")},enabled=!preparing){Text("Escolher foto")}}
  item{OutlinedTextField(handle,{handle=it.take(100)},label={Text("Arroba do responsável")},singleLine=true)}
  item{OutlinedTextField(description,{description=it.take(1000)},label={Text("Apresentação da scan")},minLines=3)}
  item{Row(verticalAlignment=Alignment.CenterVertically){Text("Parceria ativa",Modifier.weight(1f));Switch(enabled,{enabled=it})}}
  if(localError.isNotBlank())item{Text(localError,color=MaterialTheme.colorScheme.error)}
  if(serverError.isNotBlank())item{Text(serverError,color=MaterialTheme.colorScheme.error)}
 }},confirmButton={TextButton({try{ExternalSourceParser.url(url);require(name.isNotBlank());localError="";save(initial.copy(name=name.trim(),url=url.trim(),cover=cover,photo=photo,handle=handle.trim(),description=description.trim(),enabled=enabled))}catch(e:Exception){localError=PublicErrors.message(e,"Informe o nome e o endereço da parceria.")}},enabled=!preparing&&!saving){Text(if(saving)"Salvando…"else"Salvar parceria")}},dismissButton={TextButton(close){Text("Cancelar")}})
}
private fun encodePartnerImage(context:android.content.Context,uri:android.net.Uri):String {
 val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true};context.contentResolver.openInputStream(uri).use{android.graphics.BitmapFactory.decodeStream(it,null,bounds)}
 require(bounds.outWidth>0&&bounds.outHeight>0)
 var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1024)sample*=2
 val options=android.graphics.BitmapFactory.Options().apply{inSampleSize=sample};val bitmap=context.contentResolver.openInputStream(uri).use{android.graphics.BitmapFactory.decodeStream(it,null,options)}?:error("Imagem inválida")
 try{val output=java.io.ByteArrayOutputStream();bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,82,output);require(output.size()<700000);return "data:image/jpeg;base64,"+android.util.Base64.encodeToString(output.toByteArray(),android.util.Base64.NO_WRAP)}finally{bitmap.recycle()}
}
@Composable fun ExternalWorkScreen(initial:Work,back:()->Unit,read:(Work,Chapter)->Unit){
 val context=LocalContext.current;val uri=LocalUriHandler.current;val online=networkAvailable();val scope=rememberCoroutineScope();val offlineStore=remember{OfflineStore(context)}
 var work by remember(initial.id){mutableStateOf(initial)};var chapters by remember(initial.id){mutableStateOf(ExternalCatalog.cachedChapters(initial.id))};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf("")};var retry by remember{mutableIntStateOf(0)};var saved by remember{mutableStateOf(setOf<String>())};var pending by remember{mutableStateOf(setOf<String>())};var progress by remember{mutableIntStateOf(0)};var downloading by remember{mutableStateOf(false)}
 LaunchedEffect(initial.id,online,retry){loading=true;error="";val local=withContext(Dispatchers.IO){offlineStore.downloads().filter{it.workId==initial.id}.mapNotNull{it.chapter}}
  if(online){try{work=ExternalCatalog.details(work);chapters=(ExternalCatalog.chapters(work.id)+local).distinctBy{it.id}}catch(e:CancellationException){throw e}catch(e:Exception){error=PublicErrors.message(e,"Não foi possível carregar os capítulos da origem.");chapters=(chapters+local).distinctBy{it.id}}}else chapters=(chapters+local).distinctBy{it.id}
  chapters=chapters.sortedByDescending{it.number?:-1.0};saved=withContext(Dispatchers.IO){offlineStore.downloads().filter{it.workId==work.id}.map{it.chapterId}.toSet()};loading=false
 }
 LaunchedEffect(initial.id){WorkManager.getInstance(context).getWorkInfosByTagFlow("work-download-${initial.id}").collect{infos->
  val active=infos.filter{!it.state.isFinished};downloading=active.isNotEmpty();pending=active.mapNotNull{it.progress.getString(ChapterDownloadWorker.CHAPTER_ID)}.toSet();progress=active.maxOfOrNull{it.progress.getInt(ChapterDownloadWorker.PROGRESS,0)}?:0
  saved=withContext(Dispatchers.IO){offlineStore.downloads().filter{it.workId==work.id}.map{it.chapterId}.toSet()}
  infos.firstOrNull{it.state==androidx.work.WorkInfo.State.FAILED}?.outputData?.getString(ChapterDownloadWorker.ERROR)?.takeIf{it.isNotBlank()}?.let{error=it}
 }}
 fun download(chapter:Chapter?){scope.launch{try{if(chapter==null)ChapterDownloadWorker.enqueueAll(context,work)else ChapterDownloadWorker.enqueue(context,work,chapter)}catch(e:CancellationException){throw e}catch(e:Exception){error=PublicErrors.message(e,"Não foi possível iniciar o download.")}}}
 BackHandler(onBack=back)
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(top=18.dp,bottom=32.dp)){
  item{TextButton(back){Text("← Voltar")};Row(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically){MpImage(work.cover,work.title,Modifier.width(112.dp).aspectRatio(.7f).clip(RoundedCornerShape(20.dp)),contentScale=ContentScale.Crop);Column(Modifier.weight(1f)){Text(work.scan,color=MpAccent2,fontWeight=FontWeight.Bold);Text(work.title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text(work.author,color=MpMuted,modifier=Modifier.padding(top=8.dp));Text("${chapters.size} capítulos",color=MpMuted)}}}
  if(work.synopsis.isNotBlank())item{Text(work.synopsis,color=MpMuted)}
  item{Text("Atualizada pela scan na própria origem",color=MpAccent2,style=MaterialTheme.typography.labelMedium)}
  if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
  if(error.isNotBlank())item{Surface(color=MpSurface,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(16.dp)){Text(error,color=MpMuted);TextButton({retry++}){Text("Atualizar capítulos")}}}}
  item{Button({download(null)},Modifier.fillMaxWidth(),enabled=online&&!downloading&&chapters.isNotEmpty(),shape=RoundedCornerShape(16.dp)){Text(if(downloading)"Baixando… $progress%"else"↓ Baixar todos os capítulos")};if(downloading)LinearProgressIndicator(progress={progress/100f},Modifier.fillMaxWidth().padding(top=8.dp))}
  items(chapters,key={it.id}){chapter->Surface(color=MpSurface,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MpLine)){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f).clickable{read(work,chapter)}){Text(chapter.label,fontWeight=FontWeight.Bold);if(chapter.subtitle.isNotBlank())Text(chapter.subtitle,color=MpMuted,style=MaterialTheme.typography.bodySmall);if(chapter.id in saved)Text("Disponível offline",color=MpAccent2,style=MaterialTheme.typography.labelSmall)};TextButton({read(work,chapter)}){Text("Ler")};OutlinedButton({download(chapter)},enabled=online&&!downloading&&chapter.id !in saved){Text(if(chapter.id in saved)"✓"else"↓")}}}}
  if(!loading&&chapters.isEmpty())item{Text("Os capítulos aparecerão quando a origem fornecer uma lista compatível.",color=MpMuted)}
 }
}
