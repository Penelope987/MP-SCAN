package online.mpscan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.mpscan.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { MpScanTheme { MpScanRoot() } } }
}

private enum class Destination(val label:String,val icon:String){Home("Início","⌂"),Search("Busca","⌕"),Library("Biblioteca","▣"),Profile("Perfil","♙")}

@Composable private fun MpScanRoot(){
    var selected by remember { mutableStateOf(Destination.Home) }
    Scaffold(containerColor=MpBackground,bottomBar={ NavigationBar(containerColor=MpSurface){ Destination.entries.forEach{ item->NavigationBarItem(selected=selected==item,onClick={selected=item},icon={Text(item.icon)},label={Text(item.label)}) } } }){ padding->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp,vertical=14.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){ Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(MpAccent,MpAccent2))),contentAlignment=Alignment.Center){Text("MP",fontWeight=FontWeight.Black)};Spacer(Modifier.width(12.dp));Column{Text("MP SCAN",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium);Text("Aplicativo nativo",color=MpMuted,style=MaterialTheme.typography.bodySmall)} }
            Spacer(Modifier.height(22.dp));Text(selected.label,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black);Spacer(Modifier.height(8.dp));Text("Base limpa criada a partir do tema oficial. Esta tela será substituída pela composição correspondente do XML.",color=MpMuted)
        }
    }
}
