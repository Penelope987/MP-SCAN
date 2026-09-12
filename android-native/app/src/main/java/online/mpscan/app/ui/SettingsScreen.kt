package online.mpscan.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.mpscan.app.data.SettingsStore
import online.mpscan.app.ui.theme.*

@Composable fun SettingsScreen(back:()->Unit){
 val context=LocalContext.current;val store=remember{SettingsStore(context.applicationContext)}
 var notifications by remember{mutableStateOf(store.notifications)};var wifiOnly by remember{mutableStateOf(store.wifiOnly)};var animations by remember{mutableStateOf(store.animations)};var wideReader by remember{mutableStateOf(store.wideReader)}
 LazyColumn(Modifier.fillMaxSize().background(MpBackground),contentPadding=PaddingValues(bottom=34.dp)){
  item{Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF302060),MpAccent,Color(0xFFB13B80)))).padding(horizontal=16.dp,vertical=22.dp)){TextButton(back,Modifier.align(Alignment.TopStart)){Text("← Voltar",color=Color.White)};Column(Modifier.padding(top=62.dp,bottom=10.dp)){Text("Configurações",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black);Text("Personalize sua experiência na MP SCAN",color=Color.White.copy(alpha=.78f))}}}
  item{SectionTitle("Conta e privacidade")};item{InfoRow("✓","Verificação externa","O status será carregado da sua conta verificada")};item{InfoRow("◈","Conteúdo sensível","Disponível somente conforme as permissões da conta")};item{InfoRow("♙","Conta e segurança","Login, e-mail, senha e dispositivos")};item{InfoRow("♡","Perfil","Foto, capa, nome, arroba, bio e privacidade")}
  item{SectionTitle("Aplicativo e leitura")};item{ToggleRow("♢","Notificações","Novos capítulos, respostas e avisos",notifications){notifications=it;store.notifications=it}};item{ToggleRow("⇣","Baixar somente por Wi-Fi","Evita o uso de dados móveis nos downloads",wifiOnly){wifiOnly=it;store.wifiOnly=it}};item{ToggleRow("▣","Leitor em largura total","Usa toda a largura disponível para as páginas",wideReader){wideReader=it;store.wideReader=it}};item{ToggleRow("✦","Animações","Movimentos suaves na interface",animations){animations=it;store.animations=it}};item{InfoRow("☾","Aparência","Tema claro, escuro e cores de destaque")};item{InfoRow("⇣","Downloads","Capítulos salvos e armazenamento offline")}
  item{SectionTitle("Informações e políticas")};item{InfoRow("i","Sobre a MP SCAN","Versão, equipe e informações do aplicativo")};item{InfoRow("□","Política de privacidade","Como seus dados são tratados")};item{InfoRow("⌾","Política de segurança","Proteção da conta e da comunidade")};item{InfoRow("✓","Política de uso","Regras para usar a plataforma")}
 }
}
@Composable private fun SectionTitle(text:String){Text(text,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(start=18.dp,end=18.dp,top=28.dp,bottom=8.dp))}
@Composable private fun InfoRow(icon:String,title:String,subtitle:String){SettingCard(icon,title,subtitle){Text("›",color=MpMuted,style=MaterialTheme.typography.headlineSmall)}}
@Composable private fun ToggleRow(icon:String,title:String,subtitle:String,checked:Boolean,change:(Boolean)->Unit){SettingCard(icon,title,subtitle){Switch(checked,change)}}
@Composable private fun SettingCard(icon:String,title:String,subtitle:String,trailing:@Composable()->Unit){Surface(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=5.dp),color=MpSurface,shape=RoundedCornerShape(19.dp),border=BorderStroke(1.dp,MpLine)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Surface(color=MpSurface2,shape=RoundedCornerShape(14.dp)){Box(Modifier.size(46.dp),contentAlignment=Alignment.Center){Text(icon,color=MpAccent,fontWeight=FontWeight.Black)}};Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(title,fontWeight=FontWeight.Bold);Text(subtitle,color=MpMuted,style=MaterialTheme.typography.bodySmall)};trailing()}}}
