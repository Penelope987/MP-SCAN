package online.mpscan.app.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import online.mpscan.app.ui.theme.*

@Composable fun ElegantPill(label:String,selected:Boolean,click:()->Unit){
 Surface(Modifier.clickable(onClick=click),shape=RoundedCornerShape(14.dp),color=if(selected)MpAccent.copy(.12f)else MpSurface,border=BorderStroke(1.dp,if(selected)MpAccent.copy(.5f)else MpLine)){
  Row(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){if(selected)Icon(Icons.Default.Check,null,Modifier.size(16.dp),tint=MpAccent);Text(label,color=if(selected)MpAccent else MaterialTheme.colorScheme.onSurface,fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium,style=MaterialTheme.typography.labelLarge)}
 }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun SearchFilters(status:String,genre:String,genres:List<String>,close:()->Unit,apply:(String,String)->Unit){
 var selectedStatus by remember{mutableStateOf(status)};var selectedGenre by remember{mutableStateOf(genre)};var query by remember{mutableStateOf("")}
 Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.fillMaxWidth(.94f).widthIn(max=600.dp).fillMaxHeight(.8f),color=MpSurface,shape=RoundedCornerShape(28.dp)){
   Column(Modifier.padding(22.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Uma leitura com a sua cara",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("Refine sua descoberta",color=MpMuted,modifier=Modifier.padding(top=5.dp))};IconButton(close){Icon(Icons.Default.Close,"Fechar filtros")}}
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top=20.dp)){
     Text("STATUS DA OBRA",color=MpAccent,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall)
     FlowRow(Modifier.padding(top=10.dp,bottom=22.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){listOf("","Em andamento","Completa","Em pausa","Futura","Cancelada").forEach{name->ElegantPill(name.ifBlank{"Qualquer status"},selectedStatus==name){selectedStatus=name}}}
     Text("GÊNEROS",color=MpAccent,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall)
     OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(vertical=12.dp),shape=RoundedCornerShape(16.dp),singleLine=true,placeholder={Text("Encontrar um gênero")},leadingIcon={Icon(Icons.Default.Search,null)})
     FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){ElegantPill("Todos os gêneros",selectedGenre.isBlank()){selectedGenre=""};genres.filter{it.contains(query,true)}.forEach{name->ElegantPill(name,selectedGenre==name){selectedGenre=if(selectedGenre==name)""else name}}}
    }
    HorizontalDivider(Modifier.padding(vertical=16.dp),color=MpLine)
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){OutlinedButton({selectedStatus="";selectedGenre=""},shape=RoundedCornerShape(16.dp)){Text("Limpar")};Button({apply(selectedStatus,selectedGenre);close()},Modifier.weight(1f),shape=RoundedCornerShape(16.dp)){Text("Mostrar obras")}}
   }
  }
 }
}
@Composable fun LibrarySections(selected:String,change:(String)->Unit){
 LazyRow(Modifier.padding(vertical=18.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(listOf("favorites" to "Favoritos","continue" to "Em leitura","collections" to "Coleções","downloads" to "Downloads","history" to "Histórico")){(id,name)->ElegantPill(name,selected==id){change(id)}}}
}
@Composable fun WorkActions(favorite:Boolean,subscribed:Boolean,favoriteClick:()->Unit,notificationClick:()->Unit,collectionClick:()->Unit){
 Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)){
  listOf(Triple(if(favorite)"Na biblioteca"else"Biblioteca",Icons.Default.Favorite,favoriteClick),Triple(if(subscribed)"Avisos ativos"else"Notificações",Icons.Default.Notifications,notificationClick),Triple("Coleção",Icons.Default.List,collectionClick)).forEachIndexed{index,item->
   val selected=index==0&&favorite||index==1&&subscribed
   Surface(Modifier.weight(1f).clickable(onClick=item.third),color=if(selected)MpAccent.copy(.1f)else MpSurface,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,if(selected)MpAccent.copy(.4f)else MpLine)){
    Column(Modifier.padding(horizontal=5.dp,vertical=16.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(item.second,item.first,Modifier.size(24.dp),tint=if(selected)MpAccent else MpMuted);Text(item.first,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,modifier=Modifier.padding(top=8.dp))}
   }
  }
 }
}
