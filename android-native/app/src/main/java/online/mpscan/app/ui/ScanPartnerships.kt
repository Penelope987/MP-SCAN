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

import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable fun ScanPartnerships(openWork:(Work)->Unit,back:()->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope();val online=networkAvailable()
 var partners by remember{mutableStateOf<List<ScanPartnership>>(emptyList())};var selected by remember{mutableStateOf<ScanPartnership?>(null)}
 var administrator by remember{mutableStateOf(false)};var editing by remember{mutableStateOf<ScanPartnership?>(null)};var deleting by remember{mutableStateOf<ScanPartnership?>(null)}
 var retry by remember{mutableIntStateOf(0)};var loading by remember{mutableStateOf(true)};var message by remember{mutableStateOf("")};var saving by remember{mutableStateOf(false)};var filter by remember{mutableStateOf("Todas")}
 LaunchedEffect(online,retry){loading=true;administrator=false;message=""
  try{if(online)AccountStore(context).session()?.let{old->val fresh=AccountRepository().refresh(old);AccountStore(context).save(fresh);val profile=AccountRepository().profile(fresh);administrator=profile.role.lowercase() in listOf("adm","admin","administrador")}
   partners=ExternalCatalog.partners(context).filter{!it.draft||administrator}
  }catch(e:CancellationException){throw e}catch(e:Exception){message="Não foi possível atualizar as parcerias."}finally{loading=false}
 }
 editing?.let{partner->PartnershipEditor(partner,saving,message,{if(!saving)editing=null},{deleting=partner}){updated->scope.launch{saving=true;try{ExternalCatalog.save(context,updated);editing=null;if(selected?.id==updated.id)selected=updated.copy(ownerUid=AccountStore(context).session()?.uid.orEmpty());retry++}catch(e:CancellationException){throw e}catch(e:Exception){message=PublicErrors.message(e,"Não foi possível salvar a parceria.")}finally{saving=false}}}}
 deleting?.let{partner->AlertDialog(onDismissRequest={if(!saving)deleting=null},title={Text("Excluir parceria?")},text={Text("${partner.name} será removida da lista. Esta ação não pode ser desfeita. Os capítulos já baixados no aparelho não serão apagados.")},confirmButton={Button({scope.launch{saving=true;try{ExternalCatalog.delete(context,partner);deleting=null;editing=null;if(selected?.id==partner.id)selected=null;retry++}catch(e:CancellationException){throw e}catch(e:Exception){message=PublicErrors.message(e,"Não foi possível excluir.");deleting=null}finally{saving=false}}},enabled=!saving){Text(if(saving)"Excluindo…"else"Excluir")}},dismissButton={TextButton({deleting=null},enabled=!saving){Text("Cancelar")}})}
 selected?.let{partner->BackHandler{selected=null};PartnerCatalog(partner,administrator,{selected=null},{message="";editing=partner},openWork);return}
 BackHandler(onBack=back)
 LazyVerticalGrid(GridCells.Adaptive(280.dp),Modifier.fillMaxSize().padding(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(top=18.dp,bottom=32.dp)){
  item(span={GridItemSpan(maxLineSpan)}){Column{TextButton(back){Text("← Menu")};Text("SCANS QUE CONECTAM HISTÓRIAS",color=MpAccent,style=MaterialTheme.typography.labelSmall);Text("Parcerias scan",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineLarge);Text("Uma vitrine para cada equipe. Novas histórias para levar com você.",color=MpMuted,modifier=Modifier.padding(top=8.dp))}}
  if(administrator)item(span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Button({message="";editing=ScanPartnership(java.util.UUID.randomUUID().toString(),"","",draft=true)},Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Text("＋ Nova parceria")};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Todas","Públicas","Rascunhos").forEach{label->FilterChip(filter==label,{filter=label},{Text(label)})}}}}
  if(loading)item(span={GridItemSpan(maxLineSpan)}){LinearProgressIndicator(Modifier.fillMaxWidth())}
  if(message.isNotBlank())item(span={GridItemSpan(maxLineSpan)}){Text(message,color=MaterialTheme.colorScheme.error)}
  val shown=partners.filter{when(filter){"Públicas"->!it.draft;"Rascunhos"->it.draft;else->true}}
  if(!loading&&shown.isEmpty())item(span={GridItemSpan(maxLineSpan)}){Surface(color=MpSurface,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(24.dp)){Text(if(filter=="Rascunhos")"Sua próxima parceria começa aqui"else"Novas equipes em breve",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text(if(administrator)"Prepare a vitrine em rascunho e publique quando estiver pronta."else"As parcerias publicadas pela equipe aparecerão aqui.",color=MpMuted,modifier=Modifier.padding(top=8.dp));if(administrator)TextButton({editing=ExternalCatalog.example.copy(draft=true)}){Text("Preparar exemplo Kenji")}}}}
  items(shown,key={it.id}){partner->PartnerVitrine(partner){selected=partner}}
  item(span={GridItemSpan(maxLineSpan)}){OutlinedButton({retry++},Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Text("Atualizar parcerias")}}
 }
}
@Composable internal fun PartnerVitrine(partner:ScanPartnership,open:()->Unit){
 Surface(Modifier.fillMaxWidth().clickable(onClick=open),color=MpSurface,shape=RoundedCornerShape(28.dp),border=BorderStroke(1.dp,MpLine)){
  Column{Box(Modifier.fillMaxWidth().height(180.dp).background(Brush.linearGradient(listOf(MpAccent.copy(.30f),MpSurface2)))){
   if(partner.cover.isNotBlank())MpImage(partner.cover,partner.name,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
   Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(.72f)))))
   Text(if(partner.draft)"RASCUNHO · SÓ VOCÊ"else"SCAN PARCEIRA",Modifier.align(Alignment.BottomStart).padding(18.dp),color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall)
  }
  Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){FramedAvatar(partner.photo,partner.name,size=56.dp);Column(Modifier.weight(1f).padding(start=12.dp)){Text(partner.name,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text(if(partner.draft)"Vitrine em preparação"else"Conheça as obras desta equipe",color=MpMuted,style=MaterialTheme.typography.bodySmall)}}
   if(partner.description.isNotBlank())Text(partner.description,color=MpMuted,maxLines=3)
   Text("Explorar catálogo →",color=MpAccent,fontWeight=FontWeight.Bold)
  }}
 }
}
@Composable private fun PartnerCatalog(partner:ScanPartnership,admin:Boolean,back:()->Unit,edit:()->Unit,openWork:(Work)->Unit){
 val uri=LocalUriHandler.current;val online=networkAvailable()
 var works by remember(partner){mutableStateOf(ExternalCatalog.cachedCatalog(partner.id))};var retry by remember{mutableIntStateOf(0)};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf("")};var query by remember{mutableStateOf("")};var owner by remember(partner){mutableStateOf<ProfilePerson?>(null)};var profile by remember{mutableStateOf(false)}
 LaunchedEffect(partner,online,retry){loading=true;error="";if(online){try{works=ExternalCatalog.catalog(partner)}catch(e:CancellationException){throw e}catch(e:Exception){error=PublicErrors.message(e,"Não foi possível carregar esta página parceira.")};if(partner.responsibleUid.isNotBlank())try{owner=ProfileIdentity.person(partner.responsibleUid,UserDirectory.profile(partner.responsibleUid))}catch(e:CancellationException){throw e}catch(e:Exception){}}else error="Você está offline. Capítulos baixados continuam na Biblioteca.";loading=false}
 if(profile&&partner.responsibleUid.isNotBlank())NativeProfileDialog(partner.responsibleUid){profile=false}
 LazyVerticalGrid(GridCells.Adaptive(145.dp),Modifier.fillMaxSize().padding(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(top=18.dp,bottom=32.dp)){
  item(span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){TextButton(back){Text("← Parcerias scan")};PartnerVitrine(partner){};owner?.let{person->Text("Responsável pela scan",color=MpMuted,style=MaterialTheme.typography.labelMedium);UserIdentityCard(person){profile=true}};if(admin)OutlinedButton(edit,Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Icon(Icons.Default.Edit,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("Gerenciar vitrine")};OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Buscar obras desta scan")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,shape=RoundedCornerShape(20.dp));Text("${works.size} obras nesta vitrine",color=MpMuted);if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())}}
  if(error.isNotBlank())item(span={GridItemSpan(maxLineSpan)}){Surface(color=MpSurface,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(18.dp)){Text(error,color=MpMuted);if(works.isNotEmpty())Text("Mostrando o último catálogo carregado.",color=MpAccent,modifier=Modifier.padding(top=8.dp));TextButton({retry++}){Text("Tentar novamente")};TextButton({uri.openUri(partner.url)}){Text("Abrir página original")}}}}
  val filtered=works.filter{query.isBlank()||Discovery.matches(it,query)}
  if(!loading&&error.isBlank()&&filtered.isEmpty())item(span={GridItemSpan(maxLineSpan)}){Text(if(query.isBlank())"Esta página ainda não apresenta obras."else"Nenhuma obra corresponde à busca.",color=MpMuted)}
  items(filtered,key={it.id}){work->WorkCoverTile(work,Modifier.fillMaxWidth()){openWork(work)}}
 }
}
@Composable private fun PartnershipEditor(initial:ScanPartnership,saving:Boolean,serverError:String,close:()->Unit,delete:()->Unit,save:(ScanPartnership)->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 var name by remember(initial){mutableStateOf(initial.name)};var url by remember(initial){mutableStateOf(initial.url)};var cover by remember(initial){mutableStateOf(initial.cover)};var photo by remember(initial){mutableStateOf(initial.photo)};var handle by remember(initial){mutableStateOf(initial.handle)};var responsibleUid by remember(initial){mutableStateOf(initial.responsibleUid)};var responsible by remember(initial){mutableStateOf<ProfilePerson?>(null)};var description by remember(initial){mutableStateOf(initial.description)};var draft by remember(initial){mutableStateOf(initial.draft)};var photoTarget by remember{mutableStateOf(false)};var localError by remember{mutableStateOf("")};var preparing by remember{mutableStateOf(false)}
 LaunchedEffect(initial.responsibleUid){if(initial.responsibleUid.isNotBlank())try{responsible=ProfileIdentity.person(initial.responsibleUid,UserDirectory.profile(initial.responsibleUid))}catch(e:CancellationException){throw e}catch(e:Exception){}}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){value->if(value!=null)scope.launch{preparing=true;try{val encoded=withContext(Dispatchers.IO){encodePartnerImage(context,value)};if(photoTarget)photo=encoded else cover=encoded}catch(e:CancellationException){throw e}catch(e:Exception){localError="Não foi possível preparar esta imagem. Escolha outra foto."}finally{preparing=false}}}
 Dialog(onDismissRequest={if(!saving)close()},properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.fillMaxWidth(.96f).widthIn(max=720.dp).fillMaxHeight(.92f),shape=RoundedCornerShape(28.dp),color=MpSurface){Column(Modifier.padding(20.dp)){
   Text(if(initial.name.isBlank())"Nova parceria"else"Gerenciar parceria",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall)
   LazyColumn(Modifier.weight(1f).padding(vertical=16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    item{Text("Prepare a identidade da scan e escolha quando compartilhar com os leitores.",color=MpMuted)}
    item{OutlinedTextField(name,{name=it.take(100)},Modifier.fillMaxWidth(),label={Text("Nome da scan")},singleLine=true,shape=RoundedCornerShape(16.dp))}
    item{OutlinedTextField(url,{url=it.take(2048)},Modifier.fillMaxWidth(),label={Text("Página do catálogo")},supportingText={Text("Somente as obras desta página serão importadas.")},singleLine=true,shape=RoundedCornerShape(16.dp))}
    item{if(cover.isNotBlank())MpImage(cover,"Capa da parceria",Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(20.dp)),contentScale=ContentScale.Crop);OutlinedButton({photoTarget=false;picker.launch("image/*")},enabled=!preparing&&!saving){Text("Escolher capa da vitrine")}}
    item{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){FramedAvatar(photo,name,size=60.dp);OutlinedButton({photoTarget=true;picker.launch("image/*")},enabled=!preparing&&!saving){Text("Escolher foto da scan")}}}
    item {
     OutlinedTextField(handle,{handle=it.take(100);responsibleUid="";responsible=null},Modifier.fillMaxWidth(),label={Text("Buscar responsável por nome ou @")},singleLine=true,shape=RoundedCornerShape(16.dp))
     if(responsible==null)UserSearchResults(handle){person->responsible=person;responsibleUid=person.uid;handle=person.username}
     responsible?.let{person->
      Column(Modifier.padding(top=10.dp)){
       UserIdentityCard(person){}
       TextButton({responsible=null;responsibleUid="";handle=""}){Text("Trocar responsável")}
      }
     }
    }
    item{OutlinedTextField(description,{description=it.take(1000)},Modifier.fillMaxWidth(),label={Text("Apresentação da scan")},minLines=3,shape=RoundedCornerShape(16.dp))}
    item{Text("Visibilidade",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){FilterChip(draft,{draft=true},{Text("Rascunho")});FilterChip(!draft,{draft=false},{Text("Público")})};Text(if(draft)"Só você verá este rascunho. Publique quando a vitrine estiver pronta."else"Todos os leitores poderão encontrar esta parceria.",color=MpMuted,style=MaterialTheme.typography.bodySmall)}
    if(localError.isNotBlank())item{Text(localError,color=MaterialTheme.colorScheme.error)}
    if(serverError.isNotBlank())item{Text(serverError,color=MaterialTheme.colorScheme.error)}
    if(initial.name.isNotBlank())item{OutlinedButton(delete,enabled=!saving,colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.error)){Icon(Icons.Default.Delete,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("Excluir parceria")}}
   }
   Button({try{ExternalSourceParser.url(url);require(name.isNotBlank());localError="";save(initial.copy(name=name.trim(),url=url.trim(),cover=cover,photo=photo,handle=handle.trim(),description=description.trim(),draft=draft,responsibleUid=responsibleUid))}catch(e:Exception){localError=PublicErrors.message(e,"Informe o nome e o endereço da parceria.")}},Modifier.fillMaxWidth(),enabled=!preparing&&!saving,shape=RoundedCornerShape(18.dp)){Text(if(saving)"Salvando…"else if(draft)"Salvar rascunho"else"Publicar parceria")}
   TextButton(close,Modifier.fillMaxWidth(),enabled=!saving){Text("Cancelar")}
  }}
 }
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
 var work by remember(initial.id){mutableStateOf(initial)};var chapters by remember(initial.id){mutableStateOf(ExternalCatalog.cachedChapters(initial.id))};var loading by remember(initial.id){mutableStateOf(true)};var error by remember(initial.id){mutableStateOf("")};var retry by remember{mutableIntStateOf(0)};var saved by remember(initial.id){mutableStateOf(setOf<String>())};var pending by remember{mutableStateOf(setOf<String>())};var progress by remember{mutableIntStateOf(0)};var downloading by remember{mutableStateOf(false)}
 LaunchedEffect(initial.id,online,retry){loading=true;error="";val local=withContext(Dispatchers.IO){offlineStore.downloads().filter{it.workId==initial.id}.mapNotNull{it.chapter}}
  if(online){try{work=ExternalCatalog.details(work);chapters=(ExternalCatalog.chapters(work.id)+local).distinctBy{it.id}}catch(e:CancellationException){throw e}catch(e:Exception){error=PublicErrors.message(e,"Não foi possível carregar os capítulos da origem.");chapters=(chapters+local).distinctBy{it.id}}}else chapters=(chapters+local).distinctBy{it.id}
  chapters=chapters.sortedByDescending{it.number?:-1.0};saved=withContext(Dispatchers.IO){offlineStore.downloads().filter{it.workId==work.id}.map{it.chapterId}.toSet()};loading=false
 }
 LaunchedEffect(initial.id){WorkManager.getInstance(context).getWorkInfosByTagFlow("work-download-${initial.id}").collect{infos->
  val active=infos.filter{!it.state.isFinished};downloading=active.isNotEmpty();if(downloading)error="";pending=chapters.filter{chapter->active.any{ChapterDownloadWorker.uniqueName(initial.id,chapter.id) in it.tags||it.progress.getString(ChapterDownloadWorker.CHAPTER_ID)==chapter.id}}.map{it.id}.toSet();progress=active.maxOfOrNull{it.progress.getInt(ChapterDownloadWorker.PROGRESS,0)}?:0
  saved=withContext(Dispatchers.IO){offlineStore.downloads().filter{it.workId==work.id}.map{it.chapterId}.toSet()}
  if(!downloading)infos.filter{it.state==androidx.work.WorkInfo.State.FAILED&&chapters.any{chapter->chapter.id !in saved&&(ChapterDownloadWorker.uniqueName(initial.id,chapter.id) in it.tags||"work-download-all-${initial.id}" in it.tags)}}.maxByOrNull{it.id.toString()}?.outputData?.getString(ChapterDownloadWorker.ERROR)?.takeIf{it.isNotBlank()}?.let{error=it}
 }}
 fun download(chapter:Chapter?){scope.launch{try{if(chapter==null)ChapterDownloadWorker.enqueueAll(context,work)else ChapterDownloadWorker.enqueue(context,work,chapter)}catch(e:CancellationException){throw e}catch(e:Exception){error=PublicErrors.message(e,"Não foi possível iniciar o download.")}}}
 BackHandler(onBack=back)
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(top=18.dp,bottom=32.dp)){
  item{TextButton(back){Text("← Voltar")};Row(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically){MpImage(work.cover,work.title,Modifier.width(112.dp).aspectRatio(.7f).clip(RoundedCornerShape(20.dp)),contentScale=ContentScale.Crop);Column(Modifier.weight(1f)){Text(work.scan,color=MpAccent2,fontWeight=FontWeight.Bold);Text(work.title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text(work.author,color=MpMuted,modifier=Modifier.padding(top=8.dp));Text("${chapters.size} capítulos",color=MpMuted)}}}
  if(work.synopsis.isNotBlank())item{Text(work.synopsis,color=MpMuted)}
  item{Text("Atualizada pela scan na própria origem",color=MpAccent2,style=MaterialTheme.typography.labelMedium)}
  if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
  if(error.isNotBlank())item{Surface(color=MpSurface,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(16.dp)){Text(error,color=MpMuted);TextButton({retry++}){Text("Atualizar capítulos")}}}}
  item{Button({download(null)},Modifier.fillMaxWidth(),enabled=online&&!downloading&&chapters.isNotEmpty(),shape=RoundedCornerShape(16.dp)){Icon(ScanDownloadIcon,null,Modifier.size(20.dp));Spacer(Modifier.width(10.dp));Text(if(downloading)"Baixando… $progress%"else"Baixar capítulos para ler offline")};if(downloading)LinearProgressIndicator(progress={progress/100f},Modifier.fillMaxWidth().padding(top=8.dp))}
  items(chapters,key={it.id}){chapter->val complete=chapter.id in saved;val active=chapter.id in pending
   Surface(color=MpSurface,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,if(complete)MpAccent.copy(.3f)else MpLine)){
    Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
     Column(Modifier.fillMaxWidth().clickable{read(work,chapter)}){Text(chapter.label,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);if(chapter.subtitle.isNotBlank())Text(chapter.subtitle,color=MpMuted,style=MaterialTheme.typography.bodySmall);Text(if(complete)"Disponível offline"else if(active)"Preparando seu capítulo…"else"Leia online ou guarde para depois",color=if(complete)MpAccent else MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=5.dp))}
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
      OutlinedButton({read(work,chapter)},Modifier.weight(1f),shape=RoundedCornerShape(14.dp)){Text("Ler capítulo")}
      FilledTonalButton({download(chapter)},Modifier.weight(1f),enabled=online&&!active&&!complete,shape=RoundedCornerShape(14.dp)){
       if(active)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(if(complete)Icons.Default.CheckCircle else ScanDownloadIcon,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text(if(complete)"Salvo"else if(active)"Baixando"else"Baixar")
      }
     }
    }
   }
  }
  if(!loading&&chapters.isEmpty())item{Text("Os capítulos aparecerão quando a origem fornecer uma lista compatível.",color=MpMuted)}
 }
}

private val ScanDownloadIcon=ImageVector.Builder(name="Download",defaultWidth=24.dp,defaultHeight=24.dp,viewportWidth=24f,viewportHeight=24f).apply{
 path(fill=SolidColor(Color.Black)){moveTo(11f,3f);lineTo(13f,3f);lineTo(13f,12f);lineTo(16f,9f);lineTo(17.4f,10.4f);lineTo(12f,15.8f);lineTo(6.6f,10.4f);lineTo(8f,9f);lineTo(11f,12f);close();moveTo(4f,16f);lineTo(6f,16f);lineTo(6f,19f);lineTo(18f,19f);lineTo(18f,16f);lineTo(20f,16f);lineTo(20f,21f);lineTo(4f,21f);close()}
}.build()
