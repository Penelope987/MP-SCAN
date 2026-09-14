package online.mpscan.app.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import coil3.compose.AsyncImage
import kotlinx.coroutines.*
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*
import org.json.JSONObject
import java.net.*
private data class CM(val id:String,val uid:String,val name:String,val username:String,val photo:String,val text:String,val commentImage:String,val spoiler:Boolean,val date:Long,val admin:Boolean,val rank:String,val reactions:JSONObject,val frame:CF)
private data class CF(val name:String="",val color:String="#8d2bff",val background:String="#17171d",val image:String="",val width:Int=2)
private class API{val b="https://nnnsss-23f2f-default-rtdb.firebaseio.com";fun e(v:String)=URLEncoder.encode(v,"UTF-8");fun path(t:String,w:String,c:String)=if(t=="obra")"comentariosV1/obra/${e(w)}" else "comentariosV1/capitulo/${e(w)}/${e(c)}";fun q(url:String,m:String="GET",body:String?=null):JSONObject{val x=URL(url).openConnection() as HttpURLConnection;x.requestMethod=m;x.connectTimeout=15000;x.readTimeout=25000;x.setRequestProperty("Content-Type","application/json");if(body!=null){x.doOutput=true;x.outputStream.use{it.write(body.toByteArray())}};val ok=x.responseCode in 200..299;val s=(if(ok)x.inputStream else x.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();x.disconnect();if(!ok)error(if("TOKEN_EXPIRED" in s)"Sua sessão expirou. Entre novamente." else "Não foi possível conectar.");return if(s.isBlank()||s=="null")JSONObject()else JSONObject(s)}
 suspend fun list(t:String,w:String,c:String)=withContext(Dispatchers.IO){val x=q("$b/${path(t,w,c)}.json");val ids=runCatching{q("$b/identidadesComentarios.json")}.getOrDefault(JSONObject());val profiles=runCatching{q("$b/perfisPublicos.json")}.getOrDefault(JSONObject());val frames=runCatching{q("$b/config/commentFrames.json")}.getOrDefault(JSONObject());x.keys().asSequence().mapNotNull{id->x.optJSONObject(id)?.let{n->val uid=n.optString("uid");val i=ids.optJSONObject(uid)?:profiles.optJSONObject(uid)?:JSONObject();val fid=i.optString("molduraComentarioId");val fr=frames.optJSONObject(fid)?:JSONObject();CM(id,uid,i.optString("nome","Leitor MP SCAN"),i.optString("nomeUsuario"),i.optString("foto"),n.optString("texto"),n.optString("imagemUrl"),n.optBoolean("spoiler"),n.optLong("data"),i.optBoolean("admin",i.optBoolean("isAdmin",false)),i.optString("ranking",i.optString("rank")),n.optJSONObject("reacoes")?:JSONObject(),CF(fr.optString("nome"),fr.optString("borderColor",fr.optString("bordaCor","#8d2bff")),fr.optString("bgColor",fr.optString("fundoCor","#17171d")),fr.optString("imageUrl",fr.optString("imagem")),fr.optInt("borderWidth",2).coerceIn(1,8)))}}.toList().reversed()}
 suspend fun send(t:String,w:String,c:String,text:String,sp:Boolean,s:AccountSession)=withContext(Dispatchers.IO){q("$b/${path(t,w,c)}.json?auth=${e(s.token)}","POST",JSONObject().put("uid",s.uid).put("data",System.currentTimeMillis()).put("texto",text).put("imagemUrl","").put("spoiler",sp).toString())}}
private fun color(v:String,f:Color)=runCatching{Color(android.graphics.Color.parseColor(v))}.getOrDefault(f)
@Composable fun CommentsSection(type:String,workId:String,chapterId:String="",modifier:Modifier=Modifier){val ctx=LocalContext.current;val api=remember{API()};val store=remember{AccountStore(ctx)};val accounts=remember{AccountRepository()};val scope=rememberCoroutineScope();var session by remember{mutableStateOf(store.session())};var list by remember{mutableStateOf<List<CM>>(emptyList())};var text by remember{mutableStateOf("")};var sp by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var err by remember{mutableStateOf("")};var login by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){if(session==null){val old=ctx.getSharedPreferences("comments_auth",0);val u=old.getString("u","").orEmpty();val t=old.getString("t","").orEmpty();if(u.isNotBlank()&&t.isNotBlank()){AccountSession(u,"",t).also{store.save(it);session=it}}}}
 suspend fun load(){busy=true;runCatching{api.list(type,workId,chapterId)}.onSuccess{list=it;err=""}.onFailure{err="Sem internet para carregar os comentários."};busy=false};LaunchedEffect(type,workId,chapterId){load()};if(login)LD({login=false}){e,p->scope.launch{runCatching{accounts.signIn(e,p)}.onSuccess{session=it;store.save(it);login=false}.onFailure{err=it.message?:"Não foi possível entrar."}}}
 Column(modifier){Text(if(type=="obra")"Comentários da obra" else "Comentários do capítulo",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall);Text("Perfil, avatar e moldura sincronizados com o site",color=MpMuted);Spacer(Modifier.height(10.dp));Surface(color=MpSurface,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(14.dp)){if(session==null){Text("Entre com sua conta para comentar.",color=MpMuted);Button({login=true},Modifier.fillMaxWidth().padding(top=8.dp)){Text("Entrar para comentar")}}else{OutlinedTextField(text,{text=it.take(3000)},Modifier.fillMaxWidth(),label={Text("Escreva um comentário…")},minLines=3);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){FilterChip(sp,{sp=!sp},{Text(if(sp)"⚠ Spoiler marcado" else "Marcar spoiler")});Spacer(Modifier.weight(1f));Button({scope.launch{busy=true;runCatching{api.send(type,workId,chapterId,text.trim(),sp,session!!)}.onSuccess{text="";sp=false;load()}.onFailure{err="Não foi possível publicar."};busy=false}},enabled=text.isNotBlank()&&!busy){Text("Enviar")}}}}};if(busy)LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=8.dp));if(err.isNotBlank())Text(err,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=8.dp));if(!busy&&list.isEmpty()&&err.isBlank())Text("Ainda não há comentários.",color=MpMuted,modifier=Modifier.padding(14.dp));list.forEach{Card(it)}}}
@Composable private fun Card(x:CM){
 var show by remember(x.id){mutableStateOf(!x.spoiler)}
 val bc=color(x.frame.color,MpAccent);val bg=color(x.frame.background,MpSurface)
 val date=remember(x.date){if(x.date>0)SimpleDateFormat("dd/MM/yyyy 'às' HH:mm",Locale("pt","BR")).format(Date(x.date)) else ""}
 Surface(Modifier.fillMaxWidth().padding(top=12.dp),color=bg,shape=RoundedCornerShape(20.dp),border=BorderStroke(x.frame.width.dp,bc)){
  Box(Modifier.defaultMinSize(minHeight=190.dp)){
   if(x.frame.image.isNotBlank())AsyncImage(x.frame.image,null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
   Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=if(x.frame.image.isNotBlank())0.38f else 0f)))
   Column(Modifier.padding(16.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){
     Surface(Modifier.size(52.dp),shape=CircleShape,color=MpSurface2,border=BorderStroke(2.dp,bc)){
      if(x.photo.isNotBlank())AsyncImage(x.photo,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
      else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(x.name.take(1).uppercase(),fontWeight=FontWeight.Black)}
     }
     Column(Modifier.padding(start=10.dp).weight(1f)){
      Row(verticalAlignment=Alignment.CenterVertically){
       Text(x.name,fontWeight=FontWeight.Black,color=Color.White)
       if(x.admin)Surface(Modifier.padding(start=6.dp),color=MpAccent.copy(alpha=.85f),shape=RoundedCornerShape(12.dp)){Text("◆ ADM",Modifier.padding(horizontal=7.dp,vertical=3.dp),style=MaterialTheme.typography.labelSmall,color=Color.White)}
       if(x.rank.isNotBlank())Surface(Modifier.padding(start=6.dp),color=Color(0xffffdc78),shape=RoundedCornerShape(12.dp)){Text("♛ ${x.rank}",Modifier.padding(horizontal=7.dp,vertical=3.dp),style=MaterialTheme.typography.labelSmall,color=Color(0xff332400))}
      }
      if(x.username.isNotBlank())Text("@${x.username.removePrefix("@")}",color=Color.White.copy(alpha=.75f),style=MaterialTheme.typography.labelMedium)
      if(date.isNotBlank())Text("◷ $date",color=Color.White.copy(alpha=.65f),style=MaterialTheme.typography.labelSmall)
     }
    }
    Spacer(Modifier.height(18.dp))
    if(x.spoiler&&!show)Text("⚠ Spoiler — toque para revelar",color=Color.White,modifier=Modifier.clickable{show=true})
    else {
     Text(x.text.ifBlank{"Comentário sem texto."},color=Color.White,style=MaterialTheme.typography.bodyLarge)
     if(x.commentImage.isNotBlank())AsyncImage(x.commentImage,null,Modifier.fillMaxWidth().heightIn(max=280.dp).padding(top=10.dp),contentScale=ContentScale.Fit)
    }
    HorizontalDivider(Modifier.padding(top=18.dp),color=Color.White.copy(alpha=.18f))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){
     listOf("👍","❤️","😂","😮","😢","🔥").forEach{emoji->
      val count=x.reactions.optJSONObject(emoji)?.length()?:x.reactions.optInt(emoji,0)
      Surface(color=Color.Black.copy(alpha=.35f),shape=CircleShape,border=BorderStroke(1.dp,Color.White.copy(alpha=.25f))){Text(if(count>0)"$emoji $count" else emoji,Modifier.padding(horizontal=10.dp,vertical=7.dp))}
     }
     TextButton({}){Text("↩ Responder",color=Color(0xff75b8ff))}
     if(x.admin)TextButton({}){Text("📌 Fixar",color=Color(0xffffb64d))}
    }
    if(x.uid.isNotBlank())Row{TextButton({}){Text("✎ Editar",color=Color(0xff75b8ff))};TextButton({}){Text("Excluir",color=Color(0xff75b8ff))}}
   }
  }
 }
}
@Composable private fun LD(close:()->Unit,go:(String,String)->Unit){var e by remember{mutableStateOf("")};var p by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("Entrar na MP SCAN")},text={Column{OutlinedTextField(e,{e=it},label={Text("E-mail")});OutlinedTextField(p,{p=it},label={Text("Senha")},visualTransformation=PasswordVisualTransformation())}},confirmButton={Button({go(e,p)},enabled=e.isNotBlank()&&p.isNotBlank()){Text("Entrar")}},dismissButton={TextButton(close){Text("Cancelar")}})}