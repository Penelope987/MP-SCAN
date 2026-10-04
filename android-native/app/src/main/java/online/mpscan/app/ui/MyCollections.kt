package online.mpscan.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable fun MyCollections(works:List<Work>,openWork:(Work)->Unit){
 val context=LocalContext.current;val account=remember{AccountStore(context)};val repo=remember{LibraryRepository()};val scope=rememberCoroutineScope()
 var collections by remember{mutableStateOf<List<UserCollection>>(emptyList())};var selected by remember{mutableStateOf<UserCollection?>(null)};var loading by remember{mutableStateOf(true)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var editor by remember{mutableStateOf(false)};var editing by remember{mutableStateOf<UserCollection?>(null)};var confirmDelete by remember{mutableStateOf(false)};var attempt by remember{mutableIntStateOf(0)}
 suspend fun session():AccountSession {val old=account.session()?:error("Entre na sua conta para continuar.");return AccountRepository().refresh(old).also(account::save)}
 LaunchedEffect(attempt){loading=true;error="";runCatching{repo.collections(session())}.onSuccess{collections=it;selected=selected?.let{old->it.firstOrNull{item->item.id==old.id}}}.onFailure{if(it is kotlinx.coroutines.CancellationException)throw it;error="Não foi possível carregar suas coleções. Confira a conexão."};loading=false}
 BackHandler(selected!=null&&!editor){selected=null}
 if(editor)CollectionEditor(editing,busy,error,{if(!busy){editor=false;error=""}}){name,description,public,cover->scope.launch{busy=true;error="";runCatching{repo.saveCollection(session(),editing?.id,name,description,public,cover)}.onSuccess{saved->collections=(collections.filterNot{it.id==saved.id}+saved).sortedBy{it.name.lowercase()};selected=saved;editor=false}.onFailure{error="Não foi possível salvar a coleção. Confira sua conexão e tente novamente."};busy=false}}
 val current=selected
 if(confirmDelete&&current!=null)AlertDialog(onDismissRequest={if(!busy)confirmDelete=false},title={Text("Excluir ${current.name}?")},text={Text("A coleção será removida da sua conta e da comunidade. As obras e os downloads continuam disponíveis.")},confirmButton={Button({scope.launch{busy=true;runCatching{repo.deleteCollection(session(),current.id)}.onSuccess{collections=collections.filterNot{it.id==current.id};selected=null;confirmDelete=false}.onFailure{error="Não foi possível excluir a coleção."};busy=false}},enabled=!busy){Text(if(busy)"Excluindo…"else"Excluir")}},dismissButton={TextButton({confirmDelete=false},enabled=!busy){Text("Cancelar")}})
 LazyVerticalGrid(columns=GridCells.Adaptive(155.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)){
  item(span={GridItemSpan(maxLineSpan)}){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(if(current==null)"Minhas coleções"else"Minha estante",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text(if(current==null)"Organize histórias do seu jeito."else"Cada história tem seu lugar.",color=MpMuted,style=MaterialTheme.typography.bodySmall)};if(current==null)Button({editing=null;error="";editor=true},shape=RoundedCornerShape(16.dp)){Text("＋ Nova") }else TextButton({selected=null}){Text("← Coleções")}}}
  if(loading)item(span={GridItemSpan(maxLineSpan)}){LinearProgressIndicator(Modifier.fillMaxWidth())}
  if(error.isNotBlank()&&!editor)item(span={GridItemSpan(maxLineSpan)}){Column{Text(error,color=MaterialTheme.colorScheme.error);TextButton({attempt++}){Text("Tentar novamente")}}}
  if(current==null){
   if(!loading&&collections.isEmpty())item(span={GridItemSpan(maxLineSpan)}){Surface(color=MpSurface,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(24.dp)){Text("Uma estante só sua",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("Crie uma coleção com nome, descrição e uma capa. Escolha se ela será pública ou privada.",color=MpMuted,modifier=Modifier.padding(top=8.dp));Button({editing=null;editor=true},Modifier.padding(top=16.dp),shape=RoundedCornerShape(16.dp)){Text("Criar minha primeira coleção")}}}}
   items(collections,key={it.id}){collection->CollectionTile(collection,works,Modifier.fillMaxWidth()){selected=collection}}
  }else{
   item(span={GridItemSpan(maxLineSpan)}){val own=ProfileSnapshots.get(account.session()?.uid);CollectionHero(current,own?.let{ProfilePerson(it.uid,it.name,it.username,it.photo)},works)}
   item(span={GridItemSpan(maxLineSpan)}){Column{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton({editing=current;error="";editor=true},enabled=!busy,shape=RoundedCornerShape(16.dp)){Text("Editar coleção")};TextButton({confirmDelete=true},enabled=!busy){Text("Excluir")}};Text("Para organizar as obras, use o botão Coleção na página da história.",color=MpMuted,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=6.dp))}}
   val members=works.filter{it.id in current.workIds}
   if(members.isEmpty())item(span={GridItemSpan(maxLineSpan)}){Text("As histórias que você adicionar aparecerão aqui.",color=MpMuted)}
   items(members,key={it.id}){work->WorkCoverTile(work,Modifier.fillMaxWidth()){openWork(work)}}
  }
 }
}
@Composable fun CollectionEditor(initial:UserCollection?,busy:Boolean,error:String,close:()->Unit,save:(String,String,Boolean,String)->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 var name by remember(initial?.id){mutableStateOf(initial?.name.orEmpty())};var description by remember(initial?.id){mutableStateOf(initial?.description.orEmpty())};var public by remember(initial?.id){mutableStateOf(initial?.isPublic?:false)};var cover by remember(initial?.id){mutableStateOf(initial?.cover.orEmpty())};var importing by remember{mutableStateOf(false)};var photoError by remember{mutableStateOf("")}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)scope.launch{importing=true;photoError="";runCatching{LocalPhotos.collectionCover(context,uri)}.onSuccess{cover=it}.onFailure{photoError="Não foi possível abrir esta imagem."};importing=false}}
 Dialog(onDismissRequest={if(!busy&&!importing)close()},properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.fillMaxWidth(.95f).widthIn(max=620.dp).fillMaxHeight(.9f).imePadding(),color=MpSurface,shape=RoundedCornerShape(28.dp)){
   Column(Modifier.padding(22.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(if(initial==null)"Nova coleção"else"Editar coleção",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.headlineSmall);Text("Monte uma estante do seu jeito.",color=MpMuted,modifier=Modifier.padding(top=5.dp))};TextButton(close,enabled=!busy&&!importing){Text("✕")}}
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
     OutlinedTextField(name,{name=it.take(80)},Modifier.fillMaxWidth(),label={Text("Nome da coleção")},singleLine=true,enabled=!busy,shape=RoundedCornerShape(18.dp))
     OutlinedTextField(description,{description=it.take(1200)},Modifier.fillMaxWidth(),label={Text("Descrição • opcional")},placeholder={Text("Conte um pouquinho sobre esta coleção…")},minLines=3,enabled=!busy,shape=RoundedCornerShape(18.dp))
     Text("Capa • opcional",fontWeight=FontWeight.Bold)
     if(cover.isNotBlank()){CollectionTile(UserCollection("preview",name.ifBlank{"Sua coleção"},description,public,cover,emptySet()),emptyList(),Modifier.fillMaxWidth()){};TextButton({cover=""},enabled=!busy&&!importing){Text("Remover capa")}}
     OutlinedButton({picker.launch("image/*")},Modifier.fillMaxWidth(),enabled=!busy&&!importing,shape=RoundedCornerShape(16.dp)){Text(if(importing)"Preparando imagem…"else if(cover.isBlank())"Escolher capa"else"Trocar capa")}
     Text("Quem pode ver?",fontWeight=FontWeight.Bold)
     Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
      listOf(false to "🔒 Privada",true to "🌐 Pública").forEach{(value,label)->Surface(Modifier.weight(1f).clickable(enabled=!busy){public=value},color=if(public==value)MpAccent.copy(.12f)else MpSurface2,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,if(public==value)MpAccent else MpLine)){Column(Modifier.padding(14.dp)){Text(label,fontWeight=FontWeight.Bold);Text(if(value)"Outros leitores podem descobrir."else"Só você consegue ver.",color=MpMuted,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=5.dp))}}}
     }
     if(photoError.isNotBlank())Text(photoError,color=MaterialTheme.colorScheme.error);if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
    }
    HorizontalDivider(Modifier.padding(vertical=16.dp),color=MpLine)
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){OutlinedButton(close,enabled=!busy&&!importing,shape=RoundedCornerShape(16.dp)){Text("Cancelar")};Button({save(name.trim(),description.trim(),public,cover)},Modifier.weight(1f),enabled=name.isNotBlank()&&!busy&&!importing,shape=RoundedCornerShape(16.dp)){Text(if(busy)"Salvando…"else"Salvar coleção")}}
   }
  }
 }
}
