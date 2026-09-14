package online.mpscan.app.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*

@Composable fun AccountProfileScreen(openSettings:()->Unit){
 val ctx=LocalContext.current;val store=remember{AccountStore(ctx)};val repo=remember{AccountRepository()};val scope=rememberCoroutineScope()
 var session by remember{mutableStateOf(store.session())};var profile by remember{mutableStateOf<AccountProfile?>(null)};var frames by remember{mutableStateOf<List<CommentFrame>>(emptyList())}
 var tab by remember{mutableStateOf("Visão geral")};var busy by remember{mutableStateOf(session!=null)};var error by remember{mutableStateOf("")};var login by remember{mutableStateOf(false)};var edit by remember{mutableStateOf(false)}
 fun load(){val ss=session?:return;scope.launch{busy=true;runCatching{repo.profile(ss) to repo.frames(ss)}.onSuccess{profile=it.first;frames=it.second;error=""}.onFailure{error=it.message?:"Não foi possível carregar o perfil."};busy=false}}
 LaunchedEffect(session?.uid){if(session!=null)load()}
 if(login)LoginDialog({login=false}){e,p->scope.launch{busy=true;runCatching{repo.signIn(e,p)}.onSuccess{session=it;store.save(it);login=false}.onFailure{error=it.message?:"Não foi possível entrar."};busy=false}}
 if(edit&&profile!=null)EditDialog(profile!!,{edit=false}){p->scope.launch{busy=true;runCatching{repo.saveProfile(session!!,p)}.onSuccess{profile=p;edit=false}.onFailure{error=it.message?:"Não foi possível salvar."};busy=false}}
 val p=profile;val admin=p?.role.equals("ADM",true)||p?.role.equals("Administrador",true)
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=34.dp)){
  item{
   Box(Modifier.fillMaxWidth().height(330.dp).background(Brush.linearGradient(listOf(Color(0xff2b183d),MpAccent,Color(0xff111014))))){
    if(!p?.cover.isNullOrBlank())AsyncImage(p!!.cover,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xdd08080b),MpBackground))))
    Text("MP SCAN",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(18.dp))
    OutlinedButton(openSettings,Modifier.align(Alignment.TopEnd).padding(14.dp),shape=RoundedCornerShape(14.dp)){Text("⚙ Ajustes")}
    Row(Modifier.align(Alignment.BottomStart).padding(18.dp),verticalAlignment=Alignment.Bottom){
     Surface(Modifier.size(108.dp),shape=RoundedCornerShape(28.dp),color=MpSurface2,border=BorderStroke(3.dp,Color(0xffffd258))){
      if(!p?.photo.isNullOrBlank())AsyncImage(p!!.photo,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(p?.name?.take(1)?.uppercase()?:"MP",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineLarge)}
     }
     Column(Modifier.padding(start=14.dp,bottom=4.dp)){Text(p?.name?:"Seu perfil",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium,color=Color(0xffffd258));if(!p?.username.isNullOrBlank())Text("@${p!!.username.removePrefix("@")}",color=Color.White.copy(.8f));if(admin)Surface(Modifier.padding(top=8.dp),color=Color(0xff6b20bd).copy(.8f),shape=RoundedCornerShape(14.dp)){Text("✦ ADM",Modifier.padding(horizontal=12.dp,vertical=5.dp),fontWeight=FontWeight.Bold,color=Color(0xffc692ff))}}
    }
   }
  }
  item{Column(Modifier.padding(18.dp)){
   if(session==null){Text("Entre para carregar seu perfil e suas molduras.",color=MpMuted);Button({login=true},Modifier.fillMaxWidth().padding(top=12.dp)){Text("Entrar na conta")}}
   else{
    if(!p?.bio.isNullOrBlank())Surface(Modifier.fillMaxWidth(),color=MpSurface,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MpLine)){Text(p!!.bio,Modifier.padding(16.dp))}
    Row(Modifier.padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({edit=true}){Text("✎ Editar perfil")};OutlinedButton(openSettings){Text("⚙ Ajustes")};OutlinedButton({store.clear();session=null;profile=null}){Text("Sair")}}
    Row(Modifier.fillMaxWidth().padding(top=18.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("${p?.followers?:0}","Seguidores",Modifier.weight(1f));Stat("${p?.following?:0}","Seguindo",Modifier.weight(1f));Stat("${p?.comments?:0}","Comentários",Modifier.weight(1f))}
   }
   if(busy)LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=12.dp));if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=10.dp))
  }}
  if(session!=null){
   item{AchievementCard()}
   if(admin)item{AdminCard()}
   item{LazyRow(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(listOf("Visão geral","Atividade","Seguidores","Seguindo","Favoritos","Coleções","Molduras")){name->FilterChip(tab==name,{tab=name},{Text(name)})}}}
   item{when(tab){
    "Molduras"->FramesPanel(frames,p?.frameId.orEmpty()){frame->val current=profile?:return@FramesPanel;scope.launch{busy=true;val updated=current.copy(frameId=frame.id);runCatching{repo.saveProfile(session!!,updated)}.onSuccess{profile=updated;error=""}.onFailure{error="Não foi possível usar esta moldura."};busy=false}}
    "Seguidores"->Panel("Seus seguidores","${p?.followers?:0} pessoa(s) acompanham seu perfil.")
    "Seguindo"->Panel("Pessoas que você segue","Você segue ${p?.following?:0} perfil(is).")
    "Atividade"->Panel("Atividade recente","Comentários, avaliações e leituras aparecerão aqui.")
    "Favoritos"->Panel("Favoritos","Suas obras favoritas ficam organizadas nesta aba.")
    "Coleções"->Panel("Coleções","As coleções criadas na Biblioteca aparecem aqui.")
    else->Panel("Resumo","Acompanhe sua leitura, participação, conexões e biblioteca em um só lugar.")
   }}
  }
 }
}
@Composable private fun AchievementCard(){Surface(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),color=Color(0xff18171d),shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,Color(0xff715a2a))){Column(Modifier.padding(20.dp)){Text("✦ Galeria de conquistas",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text("Medalhas de jornada e presentes especiais entregues pela equipe MP SCAN.",color=MpMuted,modifier=Modifier.padding(top=6.dp));LinearProgressIndicator(progress={.43f},Modifier.fillMaxWidth().padding(top=18.dp),color=Color(0xffffc13d));Text("A primeira conquista de jornada chegará após um mês de cadastro.",color=MpMuted,modifier=Modifier.padding(top=16.dp))}}}
@Composable private fun AdminCard(){Surface(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),color=Color(0xff21131b),shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,Color(0xff7f294f))){Column(Modifier.padding(20.dp)){Text("🛠 Painel administrativo",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text("Gerencie obras, capítulos, usuários, comentários, banners e notificações.",color=MpMuted,modifier=Modifier.padding(top=5.dp));Button({},Modifier.fillMaxWidth().padding(top=16.dp)){Text("Abrir painel ADM")}}}}
@Composable private fun FramesPanel(frames:List<CommentFrame>,selected:String,use:(CommentFrame)->Unit){
 Column(Modifier.padding(horizontal=18.dp)){
  Text("🖼 Moldura do comentário",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall)
  Text("Escolha uma moldura criada pelo ADM. Ela também aparecerá nos seus comentários.",color=MpMuted,modifier=Modifier.padding(top=5.dp,bottom=12.dp))
  if(frames.isEmpty()){
   Surface(Modifier.fillMaxWidth(),color=MpSurface,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(20.dp)){Text("Minhas molduras",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text("Você ainda não possui uma moldura liberada pelo ADM.",color=MpMuted,modifier=Modifier.padding(top=8.dp))}}
  }else{
   for(frame in frames){
    val chosen=selected==frame.id
    Surface(Modifier.fillMaxWidth().padding(bottom=14.dp),color=MpSurface,shape=RoundedCornerShape(22.dp),border=BorderStroke(if(chosen)3.dp else 1.dp,if(chosen)MpAccent else MpLine)){
     Column{
      if(frame.image.isNotBlank())AsyncImage(frame.image,frame.name,Modifier.fillMaxWidth().height(220.dp),contentScale=ContentScale.Crop)
      Column(Modifier.padding(16.dp)){Text(frame.name,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Button({use(frame)},Modifier.fillMaxWidth().padding(top=10.dp),enabled=!chosen){Text(if(chosen)"Moldura em uso" else "Usar moldura")}}
     }
    }
   }
  }
 }
}
@Composable private fun Panel(title:String,body:String){Surface(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,bottom=18.dp),color=MpSurface,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(20.dp)){Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text(body,color=MpMuted,modifier=Modifier.padding(top=8.dp))}}}
@Composable private fun Stat(v:String,l:String,m:Modifier){Surface(m,color=MpSurface,shape=RoundedCornerShape(17.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(vertical=13.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(v,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text(l,color=MpMuted,style=MaterialTheme.typography.labelSmall)}}}
@Composable private fun LoginDialog(close:()->Unit,go:(String,String)->Unit){var e by remember{mutableStateOf("")};var p by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("Entrar na MP SCAN")},text={Column{OutlinedTextField(e,{e=it},label={Text("E-mail")});OutlinedTextField(p,{p=it},label={Text("Senha")},visualTransformation=PasswordVisualTransformation())}},confirmButton={Button({go(e,p)},enabled=e.isNotBlank()&&p.isNotBlank()){Text("Entrar")}},dismissButton={TextButton(close){Text("Cancelar")}})}
@Composable private fun EditDialog(x:AccountProfile,close:()->Unit,save:(AccountProfile)->Unit){var n by remember{mutableStateOf(x.name)};var u by remember{mutableStateOf(x.username)};var b by remember{mutableStateOf(x.bio)};var ph by remember{mutableStateOf(x.photo)};var co by remember{mutableStateOf(x.cover)};var pub by remember{mutableStateOf(x.isPublic)};AlertDialog(onDismissRequest=close,title={Text("Editar perfil")},text={Column(Modifier.verticalScroll(rememberScrollState())){OutlinedTextField(n,{n=it.take(40)},label={Text("Nome")});OutlinedTextField(u,{u=it.filterNot(Char::isWhitespace).take(24)},label={Text("@ de usuário")});OutlinedTextField(b,{b=it.take(180)},label={Text("Bio")},minLines=3);OutlinedTextField(ph,{ph=it},label={Text("URL da foto")});OutlinedTextField(co,{co=it},label={Text("URL da capa")});Row(verticalAlignment=Alignment.CenterVertically){Switch(pub,{pub=it});Text(if(pub)"Perfil público" else "Perfil privado",modifier=Modifier.padding(start=8.dp))}}},confirmButton={Button({save(x.copy(name=n.trim(),username=u.removePrefix("@"),bio=b.trim(),photo=ph.trim(),cover=co.trim(),isPublic=pub))},enabled=n.isNotBlank()){Text("Salvar")}},dismissButton={TextButton(close){Text("Cancelar")}})}
