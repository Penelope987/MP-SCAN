package online.mpscan.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*

@Composable fun CommunityCollections(works:List<Work>,openWork:(Work)->Unit){
 val context=LocalContext.current
 var entries by remember{mutableStateOf<List<CommunityCollection>>(emptyList())};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf("")};var selected by remember{mutableStateOf<CommunityCollection?>(null)};var attempt by remember{mutableIntStateOf(0)}
 LaunchedEffect(attempt){loading=true;error="";val session=AccountStore(context).session();if(session==null)error="Entre na sua conta para explorar a comunidade."else runCatching{val fresh=AccountRepository().refresh(session);AccountStore(context).save(fresh);LibraryRepository().publicCollections(fresh)}.onSuccess{entries=it}.onFailure{if(it is kotlinx.coroutines.CancellationException)throw it;error="Não foi possível carregar as coleções. Confira sua conexão."};loading=false}
 val current=selected
 BackHandler(current!=null){selected=null}
 if(current!=null){LazyVerticalGrid(columns=GridCells.Adaptive(145.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)){
  item(span={GridItemSpan(maxLineSpan)}){TextButton({selected=null}){Text("← Coleções da comunidade")}}
  item(span={GridItemSpan(maxLineSpan)}){CollectionHero(current.collection,current.owner,works)}
  val members=works.filter{it.id in current.collection.workIds};if(members.isEmpty())item(span={GridItemSpan(maxLineSpan)}){Text("Nenhuma obra desta coleção está disponível no catálogo atual.",color=MpMuted)}
  items(members,key={it.id}){work->WorkCoverTile(work,Modifier.fillMaxWidth()){openWork(work)}}
 };return}
 val groups=entries.groupBy{it.owner.uid}.values.toList()
 LazyColumn(verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(bottom=28.dp)){
  item{Text("Estantes da comunidade",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("Descubra histórias escolhidas por outros leitores.",color=MpMuted,modifier=Modifier.padding(top=6.dp))}
  if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
  if(error.isNotBlank())item{Text(error,color=MpMuted);TextButton({attempt++}){Text("Tentar novamente")}}
  if(!loading&&error.isBlank()&&groups.isEmpty())item{Text("As primeiras coleções públicas aparecerão aqui.",color=MpMuted)}
  items(groups,key={it.first().owner.uid}){group->val owner=group.first().owner
   Surface(Modifier.fillMaxWidth(),color=MpSurface,shape=RoundedCornerShape(28.dp),border=BorderStroke(1.dp,MpLine)){
    Column(Modifier.padding(18.dp)){
     Row(verticalAlignment=Alignment.CenterVertically){FramedAvatar(owner.photo,owner.name,size=56.dp);Column(Modifier.padding(start=10.dp)){Text(owner.name,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text(listOf(owner.username.takeIf{it.isNotBlank()}?.let{"@$it"},"${group.size} ${if(group.size==1)"coleção pública"else"coleções públicas"}").filterNotNull().joinToString(" • "),color=MpMuted,style=MaterialTheme.typography.bodySmall)}}
     HorizontalDivider(Modifier.padding(vertical=14.dp),color=MpLine)
     LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){items(group,key={it.collection.id}){entry->CollectionTile(entry.collection,works,Modifier.width(235.dp)){selected=entry}}}
    }
   }
  }
 }
}
