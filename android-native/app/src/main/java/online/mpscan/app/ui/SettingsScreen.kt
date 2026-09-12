package online.mpscan.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
 var page by remember{mutableStateOf<String?>(null)}
 var notifications by remember{mutableStateOf(store.notifications)};var wifiOnly by remember{mutableStateOf(store.wifiOnly)};var animations by remember{mutableStateOf(store.animations)};var wideReader by remember{mutableStateOf(store.wideReader)}
 if(page!=null){SettingsDetail(page!!){page=null};return}
 LazyColumn(Modifier.fillMaxSize().background(MpBackground),contentPadding=PaddingValues(bottom=34.dp)){
  item{Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF302060),MpAccent,Color(0xFFB13B80)))).padding(horizontal=16.dp,vertical=22.dp)){TextButton(back,Modifier.align(Alignment.TopStart)){Text("← Voltar",color=Color.White)};Column(Modifier.padding(top=62.dp,bottom=10.dp)){Text("Configurações",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black);Text("Personalize sua experiência na MP SCAN",color=Color.White.copy(alpha=.78f))}}}
  item{SectionTitle("Conta e privacidade")};item{InfoRow("✓","Verificação externa","O status será carregado da sua conta verificada"){page="Verificação externa"}};item{InfoRow("◈","Conteúdo sensível","Disponível somente conforme as permissões da conta"){page="Conteúdo sensível"}};item{InfoRow("♙","Conta e segurança","Login, e-mail, senha e dispositivos"){page="Conta e segurança"}};item{InfoRow("♡","Perfil","Foto, capa, nome, arroba, bio e privacidade"){page="Perfil"}}
  item{SectionTitle("Aplicativo e leitura")};item{ToggleRow("♢","Notificações","Novos capítulos, respostas e avisos",notifications){notifications=it;store.notifications=it}};item{ToggleRow("⇣","Baixar somente por Wi-Fi","Evita o uso de dados móveis nos downloads",wifiOnly){wifiOnly=it;store.wifiOnly=it}};item{ToggleRow("▣","Leitor em largura total","Usa toda a largura disponível para as páginas",wideReader){wideReader=it;store.wideReader=it}};item{ToggleRow("✦","Animações","Movimentos suaves na interface",animations){animations=it;store.animations=it}};item{InfoRow("☾","Aparência","Tema claro, escuro e cores de destaque"){page="Aparência"}};item{InfoRow("⇣","Downloads","Capítulos salvos e armazenamento offline"){page="Downloads"}}
  item{SectionTitle("Informações e políticas")};item{InfoRow("i","Sobre a MP SCAN","Versão, equipe e informações do aplicativo"){page="Sobre a MP SCAN"}};item{InfoRow("□","Política de privacidade","Como seus dados são tratados"){page="Política de privacidade"}};item{InfoRow("⌾","Política de segurança","Proteção da conta e da comunidade"){page="Política de segurança"}};item{InfoRow("✓","Política de uso","Regras para usar a plataforma"){page="Política de uso"}}
 }
}
@Composable private fun SectionTitle(text:String){Text(text,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(start=18.dp,end=18.dp,top=28.dp,bottom=8.dp))}
@Composable private fun InfoRow(icon:String,title:String,subtitle:String,open:()->Unit){SettingCard(icon,title,subtitle,Modifier.clickable{open()}){Text("›",color=MpMuted,style=MaterialTheme.typography.headlineSmall)}}
@Composable private fun ToggleRow(icon:String,title:String,subtitle:String,checked:Boolean,change:(Boolean)->Unit){SettingCard(icon,title,subtitle){Switch(checked,change)}}
@Composable private fun SettingCard(icon:String,title:String,subtitle:String,modifier:Modifier=Modifier,trailing:@Composable ()->Unit){Surface(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=5.dp).then(modifier),color=MpSurface,shape=RoundedCornerShape(19.dp),border=BorderStroke(1.dp,MpLine)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Surface(color=MpSurface2,shape=RoundedCornerShape(14.dp)){Box(Modifier.size(46.dp),contentAlignment=Alignment.Center){Text(icon,color=MpAccent,fontWeight=FontWeight.Black)}};Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(title,fontWeight=FontWeight.Bold);Text(subtitle,color=MpMuted,style=MaterialTheme.typography.bodySmall)};trailing()}}}

@Composable private fun SettingsDetail(title:String,back:()->Unit){val body=when(title){"Verificação externa"->"A verificação será mostrada aqui quando a autenticação da conta estiver conectada. Ela não pode ser alterada apenas neste aparelho.";"Conteúdo sensível"->"Esta permissão depende da conta verificada e das regras da plataforma. O aplicativo não libera conteúdo restrito por um botão local.";"Conta e segurança"->"Gerencie login, e-mail, senha e sessões da conta. Os controles serão ativados junto com a autenticação Firebase.";"Perfil"->"Edição de foto, capa, nome, arroba, biografia e privacidade do perfil.";"Aparência"->"Seleção de tema claro ou escuro e cores de destaque da interface.";"Downloads"->"Os capítulos baixados ficam salvos no aparelho e podem ser lidos sem internet pela Biblioteca.";"Sobre a MP SCAN"->"MP SCAN para Android • versão 5.0.0 em desenvolvimento. Aplicativo nativo para acompanhar e ler obras.";"Política de privacidade"->"Esta página exibirá integralmente a política de privacidade publicada pela MP SCAN.";"Política de segurança"->"Esta página exibirá integralmente as orientações de segurança publicadas pela MP SCAN.";else->"Esta página exibirá integralmente as regras e condições de uso publicadas pela MP SCAN."};Column(Modifier.fillMaxSize().background(MpBackground).padding(16.dp)){TextButton(back){Text("← Configurações")};Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(top=24.dp,bottom=18.dp));Surface(color=MpSurface,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,MpLine)){Text(body,color=MpMuted,modifier=Modifier.padding(20.dp))}}}
