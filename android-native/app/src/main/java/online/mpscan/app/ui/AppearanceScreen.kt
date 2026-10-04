package online.mpscan.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable fun AppearanceScreen(back:()->Unit){
 val context=LocalContext.current;val prefs=remember{context.getSharedPreferences("mp_scan_settings",0)};val scope=rememberCoroutineScope()
 var mode by remember{mutableStateOf(prefs.getString("theme","system"))};var photo by remember{mutableStateOf(prefs.getString("app_wallpaper","").orEmpty())};var dim by remember{mutableFloatStateOf(prefs.getFloat("wallpaper_dim",.8f))};var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)scope.launch{busy=true;runCatching{LocalPhotos.wallpaper(context,uri)}.onSuccess{path->photo=path;prefs.edit().putString("app_wallpaper",path).apply();message="Seu novo fundo está pronto."}.onFailure{message="Não foi possível abrir esta foto. Escolha outra imagem."};busy=false}}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
  item{TextButton(back){Text("← Configurações")};Text("Do seu jeito",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Text("Um espaço para as histórias e para você.",color=MpMuted,modifier=Modifier.padding(top=6.dp))}
  item{Surface(shape=RoundedCornerShape(26.dp),color=MpSurface,border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(20.dp)){Text("Tema",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("system" to "Automático","light" to "Claro","dark" to "Escuro").forEach{(id,label)->ElegantPill(label,mode==id){mode=id;prefs.edit().putString("theme",id).apply()}}}}}}
  item{Surface(shape=RoundedCornerShape(26.dp),color=MpSurface,border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text("Um fundo só seu",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("Escolha uma foto do aparelho. As capas das histórias continuam com sua aparência original.",color=MpMuted)
   Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp)).background(MpSurface2),contentAlignment=Alignment.Center){if(photo.isNotBlank()){MpImage(File(photo),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop);Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.background.copy(dim)))};Surface(Modifier.padding(18.dp),color=MpSurface,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text("Sua próxima história",fontWeight=FontWeight.Bold);Text("Uma prévia das suas cores",color=MpMuted);Text("MP SCAN",color=MpAccent,fontWeight=FontWeight.Bold)}}}
   Button({picker.launch("image/*")},Modifier.fillMaxWidth(),enabled=!busy,shape=RoundedCornerShape(16.dp)){Text(if(busy)"Preparando foto…"else"Escolher foto de fundo")}
   if(photo.isNotBlank()){Text("Contraste do fundo",fontWeight=FontWeight.Bold);Slider(dim,{dim=it;prefs.edit().putFloat("wallpaper_dim",it).apply()},valueRange=0.35f..0.95f);Text("Mais contraste deixa os textos mais fáceis de ler.",color=MpMuted,style=MaterialTheme.typography.bodySmall);TextButton({photo="";prefs.edit().remove("app_wallpaper").apply()}){Text("Remover foto")}}
  }}}
  item{AppearanceColorPicker("Cor de destaque","Botões, opções selecionadas e detalhes da interface.","accent",listOf("#A98AFF","#B83C80","#57B9A5","#659DDA","#C88945"))}
  item{AppearanceColorPicker("Cor dos cartões","Configurações, coleções e comentários sem uma moldura própria.","panel_color",listOf("#202023","#282036","#34232F","#203330","#EDF3F8","#FFFFFF"))}
  item{OutlinedButton({prefs.edit().remove("theme").remove("accent").remove("panel_color").remove("app_wallpaper").remove("wallpaper_dim").apply();mode="system";photo="";dim=.8f;message="A aparência original foi restaurada."},Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text("Restaurar aparência original")};if(message.isNotBlank())Text(message,color=MpMuted,modifier=Modifier.padding(top=12.dp))}
 }
}
@Composable private fun AppearanceColorPicker(title:String,subtitle:String,key:String,presets:List<String>){
 val prefs=LocalContext.current.getSharedPreferences("mp_scan_settings",0);var selected by remember{mutableStateOf(prefs.getString(key,"").orEmpty())};var edit by remember{mutableStateOf(false)};var hex by remember{mutableStateOf(selected.ifBlank{"#A98AFF"})}
 DisposableEffect(prefs,key){val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener{p,k->if(k==key||k==null)selected=p.getString(key,"").orEmpty()};prefs.registerOnSharedPreferenceChangeListener(listener);onDispose{prefs.unregisterOnSharedPreferenceChangeListener(listener)}}
 if(edit)AlertDialog(onDismissRequest={edit=false},title={Text("Sua cor personalizada")},text={OutlinedTextField(hex,{hex=it.take(7)},label={Text("Código da cor • #A98AFF")},isError=!AppearanceColors.valid(hex),singleLine=true)},confirmButton={Button({prefs.edit().putString(key,hex.uppercase()).apply();edit=false},enabled=AppearanceColors.valid(hex)){Text("Aplicar")}},dismissButton={TextButton({edit=false}){Text("Cancelar")}})
 Surface(color=MpSurface,shape=RoundedCornerShape(26.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(20.dp)){
  Text(title,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text(subtitle,color=MpMuted,modifier=Modifier.padding(top=6.dp,bottom=16.dp))
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(12.dp)){presets.forEach{value->Box(Modifier.size(44.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(value))).border(if(selected.equals(value,true))3.dp else 1.dp,if(selected.equals(value,true))MpAccent else MpLine,CircleShape).clickable{prefs.edit().putString(key,value).apply()},contentAlignment=Alignment.Center){if(selected.equals(value,true))Text("✓",color=if(AppearanceColors.darkText(value))Color.Black else Color.White,fontWeight=FontWeight.Bold)}}}
  Row(Modifier.padding(top=12.dp)){TextButton({hex=selected.ifBlank{"#A98AFF"};edit=true}){Text("Outra cor")};TextButton({prefs.edit().remove(key).apply()}){Text("Padrão")}}
 }}
}
