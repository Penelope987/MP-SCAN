package online.mpscan.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*

@Composable fun CommunityCollections(works:List<Work>,openWork:(Work)->Unit){
 val context=LocalContext.current
 var entries by remember{mutableStateOf<List<CommunityCollection>>(emptyList())}
 var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf("")};var selected by remember{mutableStateOf<CommunityCollection?>(null)};var attempt by remember{mutableIntStateOf(0)}
 LaunchedEffect(attempt){loading=true;error="";val session=AccountStore(context).session();if(session==null){error="Entre na sua conta para explorar a comunidade."}else runCatching{LibraryRepository().publicCollections(session)}.onSuccess{entries=it}.onFailure{error="Não foi possível carregar as coleções. Confira a conexão e tente novamente."};loading=false}
 val current=selected
 Column{
  Text(if(current==null)"Estantes da comunidade"else current.collection.name,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
  Text(if(current==null)"Descubra histórias escolhidas por outros leitores."else "Por ${current.owner.name}",color=MpMuted,modifier=Modifier.padding(top=5.dp,bottom=14.dp))
  if(current!=null){TextButton({selected=null}){Text("← Todas as coleções")};if(current.collection.description.isNotBlank())Text(current.collection.description,color=MpMuted,modifier=Modifier.padding(bottom=14.dp))}
  if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(error.isNotBlank()){Text(error,color=MpMuted);TextButton({attempt++}){Text("Tentar novamente")}}
  if(!loading&&error.isBlank()&&entries.isEmpty())Text("As primeiras coleções públicas aparecerão aqui.",color=MpMuted)
  LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=24.dp)){
   if(current==null)items(entries,key={it.owner.uid+"/"+it.collection.id}){entry->
    Surface(Modifier.fillMaxWidth().clickable{selected=entry},color=MpSurface,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MpLine)){
     Column(Modifier.padding(18.dp)){
      Row(verticalAlignment=Alignment.CenterVertically){FramedAvatar(entry.owner.photo,entry.owner.name,size=44.dp);Column(Modifier.padding(start=10.dp)){Text(entry.owner.name,fontWeight=FontWeight.Bold);if(entry.owner.username.isNotBlank())Text("@${entry.owner.username}",color=MpMuted,style=MaterialTheme.typography.labelSmall)}}
      Text(entry.collection.name,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=14.dp))
      Text("${entry.collection.workIds.size} histórias • coleção pública",color=MpAccent,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=5.dp))
      Row(Modifier.padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){works.filter{it.id in entry.collection.workIds}.take(4).forEach{work->MpImage(work.cover,work.title,Modifier.weight(1f).aspectRatio(.68f).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)}}
     }
    }
   }else{val members=works.filter{it.id in current.collection.workIds};if(members.isEmpty())item{Text("Nenhuma obra desta coleção está disponível no catálogo atual.",color=MpMuted)};items(members,key={it.id}){work->Surface(Modifier.fillMaxWidth().clickable{openWork(work)},color=MpSurface,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,MpLine)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){MpImage(work.cover,work.title,Modifier.width(64.dp).height(94.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop);Column(Modifier.padding(start=14.dp)){Text(work.title,fontWeight=FontWeight.Bold);Text(work.author,color=MpMuted,style=MaterialTheme.typography.bodySmall)}}}}}
  }
 }
}
