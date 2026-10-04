package online.mpscan.app.ui

import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable fun SiteAccountGate(content:@Composable ()->Unit){
 val context=LocalContext.current;val prefs=context.getSharedPreferences("mp_account",0);val store=remember{AccountStore(context)}
 var uid by remember{mutableStateOf(store.session()?.uid)}
 var ban by remember(uid){mutableStateOf(uid?.let{SiteAccess.cachedBan(context,it)}?:SiteBan())}
 var verified by remember(uid){mutableStateOf(uid==null)}
 var error by remember(uid){mutableStateOf("")};var retry by remember{mutableIntStateOf(0)}
 DisposableEffect(prefs){val listener=SharedPreferences.OnSharedPreferenceChangeListener{_,key->if(key==null||key=="uid")uid=store.session()?.uid};prefs.registerOnSharedPreferenceChangeListener(listener);onDispose{prefs.unregisterOnSharedPreferenceChangeListener(listener)}}
 LaunchedEffect(uid,retry){
  if(uid==null){verified=true;return@LaunchedEffect}
  while(true){
   try{ban=SiteAccess.checkBan(context);verified=true;error=""}
   catch(e:CancellationException){throw e}
   catch(e:Exception){
    val manager=context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    if(manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)!=true&&!ban.blocks())verified=true
    else {verified=false;error="Não foi possível verificar o acesso da conta. Confira a conexão e tente novamente."}
   }
   delay(15000)
  }
 }
 when{
  uid==null->AuthScreen({},mandatory=true){store.save(it);uid=it.uid}
  ban.blocks()->Column(Modifier.fillMaxSize().background(MpBackground).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
   Surface(Modifier.size(84.dp),color=MpAccent.copy(.1f),shape=RoundedCornerShape(28.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpAccent.copy(.3f))){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("!",color=MpAccent,fontWeight=FontWeight.Black,style=MaterialTheme.typography.displaySmall)}}
   Text("Vamos conversar sobre sua conta",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(top=24.dp))
   Text("Seu acesso está suspenso. Confira os detalhes abaixo ou fale com a equipe.",color=MpMuted,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(top=12.dp,bottom=24.dp))
   Surface(Modifier.fillMaxWidth(),color=MpSurface,shape=RoundedCornerShape(24.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MpLine)){Column(Modifier.padding(20.dp)){
    Text(if(ban.permanent)"SUSPENSÃO PERMANENTE"else"SUSPENSÃO TEMPORÁRIA",color=MpAccent,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall)
    Text(ban.reason.ifBlank{"Entre em contato com a equipe para entender o motivo."},modifier=Modifier.padding(top=12.dp))
    if(!ban.permanent)Text("Até "+SimpleDateFormat("dd/MM/yyyy 'às' HH:mm",Locale("pt","BR")).apply{timeZone=TimeZone.getTimeZone("America/Sao_Paulo")}.format(Date(ban.until)),color=MpMuted,modifier=Modifier.padding(top=14.dp))
   }}
   Button({context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.mpscan.online/#/suporte")))},Modifier.fillMaxWidth().padding(top=20.dp).height(52.dp),shape=RoundedCornerShape(16.dp)){Text("Conversar com a equipe")}
   OutlinedButton({retry++},Modifier.fillMaxWidth().padding(top=10.dp).height(50.dp),shape=RoundedCornerShape(16.dp)){Text("Verificar meu acesso novamente")}
   TextButton({store.clear();uid=null},Modifier.padding(top=10.dp)){Text("Sair da conta")}
  }
  !verified->Column(Modifier.fillMaxSize().padding(28.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
   if(error.isEmpty()){CircularProgressIndicator();Text("Verificando sua conta…",Modifier.padding(top=16.dp))}
   else{Text(error,color=MpMuted);Button({retry++},Modifier.padding(top=16.dp)){Text("Tentar novamente")};TextButton({store.clear();uid=null}){Text("Entrar em outra conta")}}
  }
  else->content()
 }
}
@Composable fun SiteAnnouncement(works:List<Work>,allowed:Boolean,openWork:(Work)->Unit){
 val context=LocalContext.current;val prefs=remember{context.getSharedPreferences("mp_announcements",0)}
 var announcement by remember{mutableStateOf<org.json.JSONObject?>(null)};var visible by remember{mutableStateOf(false)}
 LaunchedEffect(allowed,works){
  visible=false
  if(allowed)while(true){
   try{
    val x=SiteAccess.json("config/homeAnnouncement");val id=x.optString("id");val day=SimpleDateFormat("yyyy-MM-dd",Locale.US).apply{timeZone=TimeZone.getTimeZone("America/Sao_Paulo")}.format(Date())
    val workId=x.optString("workId")
    val validWork=workId.isBlank()||works.any{it.id==workId}
    val valid=x.optBoolean("enabled")&&id.isNotBlank()&&validWork
    if(!valid){visible=false;announcement=null}
    else if(prefs.getLong("until_$id",0)<=System.currentTimeMillis()&&prefs.getString("day_$id","")!=day){if(announcement?.optString("id")!=id){visible=false;delay(1800)};announcement=x;visible=true}
   }catch(e:CancellationException){throw e}catch(_:Exception){}
   delay(30000)
  }
 }
 val x=announcement?:return
 fun close(today:Boolean=false){val id=x.optString("id");val day=SimpleDateFormat("yyyy-MM-dd",Locale.US).apply{timeZone=TimeZone.getTimeZone("America/Sao_Paulo")}.format(Date());prefs.edit().putLong("until_$id",System.currentTimeMillis()+x.optInt("hours",3).coerceIn(1,24)*3600000L).putString("day_$id",if(today)day else "").apply();visible=false}
 if(visible&&allowed)Dialog(onDismissRequest={close()},properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.fillMaxWidth(.92f).widthIn(max=560.dp).heightIn(max=720.dp),shape=RoundedCornerShape(26.dp),color=MpSurface){
   Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Text("MP SCAN · PARA VOCÊ",Modifier.weight(1f),color=MpMuted,style=MaterialTheme.typography.labelSmall);IconButton({close()}){Text("×",style=MaterialTheme.typography.headlineSmall)}}
    val work=works.firstOrNull{it.id==x.optString("workId")};val image=work?.cover?:x.optString("image")
    Text(x.optString("title").ifBlank{work?.title?:"Novidades da MP SCAN"},fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
    if(image.isNotBlank())MpImage(image,"Anúncio MP SCAN",Modifier.fillMaxWidth().heightIn(max=430.dp).padding(top=14.dp),contentScale=ContentScale.Fit)
    if(x.optString("text").isNotBlank())Text(x.optString("text"),Modifier.padding(top=12.dp),color=MpMuted)
    val link=x.optString("link");val safe=link.startsWith("https://")||link.startsWith("http://")||(link.startsWith("/")&&!link.startsWith("//"))
    if(work!=null||safe)Button({close();if(work!=null)openWork(work)else{val destination=if(link.startsWith("/"))"https://www.mpscan.online/#$link" else link;context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(destination)))}},Modifier.fillMaxWidth().padding(top=16.dp)){Text(x.optString("button").ifBlank{"Conhecer agora"})}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton({close(true)}){Text("Não mostrar hoje")};TextButton({close()}){Text("Fechar")}}
   }
  }
 }
}
