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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*

@Composable fun DiscoverySearch(works:List<Work>,close:()->Unit,open:(Work)->Unit){
 var query by remember{mutableStateOf("")}
 val matches=remember(works,query){works.filter{Discovery.matches(it,query)}}
 Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.fillMaxWidth(.95f).widthIn(max=760.dp).fillMaxHeight(.86f),shape=RoundedCornerShape(26.dp),color=MpSurface){
   Column(Modifier.padding(20.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Encontre sua próxima leitura",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("Título, sinopse, autor ou gênero",color=MpMuted,style=MaterialTheme.typography.bodySmall)};IconButton(close){Text("×",style=MaterialTheme.typography.headlineSmall)}}
    OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(vertical=16.dp),singleLine=true,placeholder={Text("O que você quer ler?")},shape=RoundedCornerShape(16.dp),leadingIcon={Text("⌕")},trailingIcon={if(query.isNotBlank())IconButton({query=""}){Text("×")}})
    Text(if(query.isBlank())"Para descobrir" else "${matches.size} obras encontradas",color=MpMuted,style=MaterialTheme.typography.labelMedium)
    LazyColumn(Modifier.weight(1f).padding(top=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
     items(matches.take(if(query.isBlank())8 else 100),key={it.id}){work->SearchResultCard(work){close();open(work)}}
     if(matches.isEmpty())item{Text("Nenhuma obra encontrada. Tente outra palavra.",Modifier.padding(20.dp),color=MpMuted)}
    }
   }
  }
 }
}
@Composable fun SearchResultCard(work:Work,open:()->Unit){
 Surface(Modifier.fillMaxWidth().clickable(onClick=open),color=MpSurface2,shape=RoundedCornerShape(18.dp)){
  Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
   MpImage(work.cover,work.title,Modifier.width(64.dp).aspectRatio(3f/4.4f).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
   Column(Modifier.weight(1f).padding(start=14.dp)){Text(work.title,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis);if(work.author.isNotBlank())Text(work.author,color=MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=4.dp));Text(work.synopsis,color=MpMuted,maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=6.dp))}
   Text("›",color=MpMuted,modifier=Modifier.padding(start=8.dp))
  }
 }
}
@Composable fun SupportCard(){
 val uri=androidx.compose.ui.platform.LocalUriHandler.current
 Surface(Modifier.fillMaxWidth(),color=MpSurface,shape=RoundedCornerShape(28.dp),border=BorderStroke(1.dp,MpAccent.copy(.18f))){
  Column(Modifier.padding(24.dp)){Text("♡ JUNTOS PELA MP SCAN",color=MpAccent,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall);Text("Histórias que continuam com você",modifier=Modifier.padding(top=10.dp),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("Seu apoio ajuda a MP SCAN a continuar trazendo novas histórias.",color=MpMuted,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp));Button({uri.openUri("https://livepix.gg/mpscan")},Modifier.fillMaxWidth().padding(top=14.dp),shape=RoundedCornerShape(14.dp)){Text("♡ Apoiar a MP SCAN")}}
 }
}
