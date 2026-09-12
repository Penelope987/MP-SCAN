package online.mpscan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import online.mpscan.app.data.CatalogRepository
import online.mpscan.app.data.Work
import online.mpscan.app.ui.theme.*

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);enableEdgeToEdge();setContent{MpScanTheme{HomeRoot()}}}}

@Composable private fun HomeRoot(){
 var works by remember{mutableStateOf<List<Work>>(emptyList())};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf("")}
 LaunchedEffect(Unit){runCatching{CatalogRepository().works()}.onSuccess{works=it}.onFailure{error="Não foi possível carregar o catálogo."};loading=false}
 Scaffold(containerColor=MpBackground,bottomBar={NavigationBar(containerColor=MpSurface){listOf("⌂" to "Início","⌕" to "Busca","▣" to "Biblioteca","♙" to "Perfil").forEachIndexed{i,x->NavigationBarItem(selected=i==0,onClick={},icon={Text(x.first)},label={Text(x.second)})}}}){p->
  when{loading->Box(Modifier.fillMaxSize().padding(p),contentAlignment=Alignment.Center){CircularProgressIndicator()};error.isNotBlank()->Message(error);works.isEmpty()->Message("As obras publicadas aparecerão aqui.");else->Home(works,Modifier.padding(p))}
 }
}
@Composable private fun Header(){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(MpAccent,MpAccent2))),contentAlignment=Alignment.Center){Text("MP",fontWeight=FontWeight.Black)};Spacer(Modifier.width(12.dp));Column{Text("MP SCAN",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium);Text("Sua próxima leitura começa aqui",color=MpMuted,style=MaterialTheme.typography.bodySmall)};Spacer(Modifier.weight(1f));Surface(shape=RoundedCornerShape(14.dp),color=MpSurface,border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){Box(Modifier.size(42.dp),contentAlignment=Alignment.Center){Text("♢")}}}}
@Composable private fun Home(works:List<Work>,modifier:Modifier){LazyColumn(modifier.fillMaxSize().padding(horizontal=16.dp),contentPadding=PaddingValues(top=16.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(28.dp)){item{Header()};item{Hero(works.first())};item{Rail("Em alta",works.sortedByDescending{it.reads}.take(10),true)};item{Rail("Atualizações recentes",works.take(10),false)}}}
@Composable private fun Hero(w:Work){Box(Modifier.fillMaxWidth().height(490.dp).clip(RoundedCornerShape(34.dp)).background(MpSurface)){AsyncImage(w.banner.ifBlank{w.cover},w.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF207070A),Color(0xA607070A),Color.Transparent))));Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xF507070A)))));Column(Modifier.align(Alignment.BottomStart).padding(26.dp).fillMaxWidth(.92f)){Text("DESTAQUE MP SCAN",color=MpAccent2,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall);Spacer(Modifier.height(10.dp));Text(w.title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineLarge,maxLines=2,overflow=TextOverflow.Ellipsis);Text(w.synopsis,color=MpMuted,maxLines=3,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(vertical=12.dp));Button(onClick={},shape=RoundedCornerShape(14.dp)){Text("Ver detalhes",fontWeight=FontWeight.Black)}}}}
@Composable private fun Rail(title:String,works:List<Work>,ranked:Boolean){Column{Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));LazyRow(horizontalArrangement=Arrangement.spacedBy(13.dp)){items(works,key={it.id}){w->Box{Card(w);if(ranked)Text((works.indexOf(w)+1).toString(),fontWeight=FontWeight.Black,style=MaterialTheme.typography.displaySmall,color=Color.White,modifier=Modifier.align(Alignment.BottomStart).background(Color(0xB30B0B0D)).padding(horizontal=8.dp))}}}}}
@Composable private fun Card(w:Work){Column(Modifier.width(148.dp)){AsyncImage(w.cover,w.title,Modifier.fillMaxWidth().aspectRatio(3f/4f).clip(RoundedCornerShape(18.dp)).background(MpSurface2),contentScale=ContentScale.Crop);Text(w.title,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=8.dp));Text(w.type,color=MpMuted,style=MaterialTheme.typography.bodySmall)}}
@Composable private fun Message(text:String){Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Text(text,color=MpMuted)}}
