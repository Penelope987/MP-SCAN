package online.mpscan.app.ui
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
 var session by remember{mutableStateOf(store.session())};var profile by remember{mutableStateOf<AccountProfile?>(null)};var frames by remember{mutableStateOf<List<CommentFrame>>(emptyList())};var extras by remember{mutableStateOf(ProfileExtras(emptyList(),emptyList(),emptyList(),emptyList(),emptyList()))}
 var tab by remember{mutableStateOf("Visão geral")};var busy by remember{mutableStateOf(session!=null)};var error by remember{mutableStateOf("")};var login by remember{mutableStateOf(false)};var edit by remember{mutableStateOf(false)}
 fun load(){val ss=session?:return;scope.launch{busy=true;runCatching{Triple(repo.profile(ss),repo.frames(ss),repo.extras(ss))}.onSuccess{profile=it.first;frames=it.second;extras=it.third;error=""}.onFailure{error=it.message?:"Não foi possível carregar o perfil."};busy=false}}
 LaunchedEffect(session?.uid){if(session!=null)load()}
 if(login)LoginDialog({login=false}){e,p->scope.launch{busy=true;runCatching{repo.signIn(e,p)}.onSuccess{session=it;store.save(it);login=false}.onFailure{error=it.message?:"Não foi possível entrar."};busy=false}}
 val p=profile;val admin=p?.role.equals("ADM",true)||p?.role.equals("Administrador",true)
 if(edit&&p!=null){
  ProfileEditScreen(p,busy,error,{edit=false}){updated->
   scope.launch{busy=true;runCatching{repo.saveProfile(session!!,updated)}.onSuccess{profile=updated;edit=false;error=""}.onFailure{error=it.message?:"Não foi possível salvar o perfil."};busy=false}
  }
  return
 }
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
    "Molduras"->FramesPanel(frames,p?.frameId.orEmpty()){frame->val current=profile?:return@FramesPanel;scope.launch{busy=true;val updated=current.copy(frameId=frame.id);runCatching{repo.selectFrame(session!!,frame.id)}.onSuccess{profile=updated;frames=frames.map{it.copy(owned=it.owned||it.id==frame.id)};error=""}.onFailure{error=it.message?:"Não foi possível usar esta moldura."};busy=false}}
    "Seguidores"->PeoplePanel("Seus seguidores",extras.followers)
    "Seguindo"->PeoplePanel("Pessoas que você segue",extras.following)
    "Atividade"->ActivitiesPanel(extras.activities)
    "Favoritos"->WorksPanel("Favoritos",extras.favorites)
    "Coleções"->CollectionsPanel(extras.collections)
    else->OverviewPanel(extras)
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
      Column(Modifier.padding(16.dp)){Text(frame.name,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text(if(frame.owned)"NA COLEÇÃO" else "Bloqueada",color=if(frame.owned)MpAccent2 else MpMuted,style=MaterialTheme.typography.labelSmall);Button({use(frame)},Modifier.fillMaxWidth().padding(top=10.dp),enabled=frame.owned&&!chosen){Text(if(chosen)"Moldura em uso" else if(frame.owned)"Usar moldura" else "Moldura não liberada")}}
     }
    }
   }
  }
 }
}
@Composable private fun PeoplePanel(title:String,people:List<ProfilePerson>){
 Column(Modifier.padding(horizontal=18.dp)){
  Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)
  if(people.isEmpty()) Text("Nenhum perfil encontrado.",color=MpMuted,modifier=Modifier.padding(vertical=20.dp))
  else for(person in people){
   Surface(Modifier.fillMaxWidth().padding(top=9.dp),color=MpSurface,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MpLine)){
    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
     Surface(Modifier.size(52.dp),shape=CircleShape,color=MpSurface2){if(person.photo.isNotBlank())AsyncImage(person.photo,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
     Column(Modifier.padding(start=12.dp)){Text(person.name,fontWeight=FontWeight.Bold);if(person.username.isNotBlank())Text("@"+person.username.removePrefix("@"),color=MpMuted)}
    }
   }
  }
 }
}
@Composable private fun WorksPanel(title:String,works:List<ProfileWork>){
 Column(Modifier.padding(horizontal=18.dp)){
  Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)
  if(works.isEmpty()) Text("Nenhuma obra encontrada.",color=MpMuted,modifier=Modifier.padding(vertical=20.dp))
  else for(work in works){
   Surface(Modifier.fillMaxWidth().padding(top=9.dp),color=MpSurface,shape=RoundedCornerShape(18.dp)){
    Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
     if(work.cover.isNotBlank())AsyncImage(work.cover,null,Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
     Text(work.title,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=12.dp))
    }
   }
  }
 }
}
@Composable private fun CollectionsPanel(collections:List<ProfileCollection>){
 Column(Modifier.padding(horizontal=18.dp)){
  Text("Coleções",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)
  if(collections.isEmpty())Text("Nenhuma coleção encontrada.",color=MpMuted,modifier=Modifier.padding(vertical=20.dp))
  else for(item in collections){Surface(Modifier.fillMaxWidth().padding(top=9.dp),color=MpSurface,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Text(item.name,fontWeight=FontWeight.Bold);Text("${item.works.size} obra(s)",color=MpMuted)}}}
 }
}
@Composable private fun ActivitiesPanel(items:List<ProfileActivity>){
 Column(Modifier.padding(horizontal=18.dp)){
  Text("Atividade recente",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)
  if(items.isEmpty())Text("Nenhuma atividade encontrada.",color=MpMuted,modifier=Modifier.padding(vertical=20.dp))
  else for(activity in items.take(40)){Surface(Modifier.fillMaxWidth().padding(top=9.dp),color=MpSurface,shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(14.dp)){Text(if(activity.type=="avaliacao")"★" else "●",color=MpAccent);Column(Modifier.padding(start=12.dp)){Text(activity.title,fontWeight=FontWeight.Bold);Text(activity.detail,color=MpMuted,maxLines=2)}}}}
 }
}
@Composable private fun OverviewPanel(x:ProfileExtras){
 Column{
  Panel("Resumo","Acompanhe sua leitura, participação, conexões e biblioteca em um só lugar.")
  Row(Modifier.fillMaxWidth().padding(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("${x.activities.size}","Atividades",Modifier.weight(1f));Stat("${x.favorites.size}","Favoritos",Modifier.weight(1f));Stat("${x.collections.size}","Coleções",Modifier.weight(1f))}
 }
}
@Composable private fun Panel(title:String,body:String){Surface(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,bottom=18.dp),color=MpSurface,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(20.dp)){Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text(body,color=MpMuted,modifier=Modifier.padding(top=8.dp))}}}
@Composable private fun Stat(v:String,l:String,m:Modifier){Surface(m,color=MpSurface,shape=RoundedCornerShape(17.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(vertical=13.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(v,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text(l,color=MpMuted,style=MaterialTheme.typography.labelSmall)}}}
@Composable private fun LoginDialog(close:()->Unit,go:(String,String)->Unit){var e by remember{mutableStateOf("")};var p by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("Entrar na MP SCAN")},text={Column{OutlinedTextField(e,{e=it},label={Text("E-mail")});OutlinedTextField(p,{p=it},label={Text("Senha")},visualTransformation=PasswordVisualTransformation())}},confirmButton={Button({go(e,p)},enabled=e.isNotBlank()&&p.isNotBlank()){Text("Entrar")}},dismissButton={TextButton(close){Text("Cancelar")}})}
private fun imageData(context:Context,uri:Uri,maxBytes:Int):String{
 val bytes=context.contentResolver.openInputStream(uri)?.use{it.readBytes()}?:error("Não foi possível abrir a imagem.")
 if(bytes.size>maxBytes)error("A imagem é maior que o limite permitido.")
 val mime=context.contentResolver.getType(uri)?:"image/jpeg"
 return "data:$mime;base64,"+Base64.encodeToString(bytes,Base64.NO_WRAP)
}
@Composable private fun ProfileEditScreen(x:AccountProfile,busy:Boolean,externalError:String,close:()->Unit,save:(AccountProfile)->Unit){
 val context=LocalContext.current
 var n by remember{x.let{mutableStateOf(it.name)}}
 var u by remember{x.let{mutableStateOf(it.username.removePrefix("@"))}}
 var b by remember{x.let{mutableStateOf(it.bio)}}
 var ph by remember{x.let{mutableStateOf(it.photo)}}
 var co by remember{x.let{mutableStateOf(it.cover)}}
 var color by remember{x.let{mutableStateOf(it.color.ifBlank{"#8d2bff"})}}
 var pub by remember{x.let{mutableStateOf(it.isPublic)}}
 var imageError by remember{mutableStateOf("")}
 val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
  if(uri!=null)runCatching{imageData(context,uri,4*1024*1024)}.onSuccess{ph=it;imageError=""}.onFailure{imageError=it.message.orEmpty()}
 }
 val coverPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
  if(uri!=null)runCatching{imageData(context,uri,6*1024*1024)}.onSuccess{co=it;imageError=""}.onFailure{imageError=it.message.orEmpty()}
 }
 val colors=listOf("#8d2bff","#ff3d8d","#4285f4","#ad204d","#28b67a","#f1a20b")
 LazyColumn(Modifier.fillMaxSize().background(MpBackground),contentPadding=PaddingValues(bottom=42.dp)){
  item{
   Box(Modifier.fillMaxWidth().height(330.dp).background(Brush.linearGradient(listOf(Color(0xff2b183d),MpAccent,Color(0xff111014))))){
    if(co.isNotBlank())AsyncImage(co,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000),Color(0xdd08080b)))))
    OutlinedButton(close,Modifier.padding(16.dp),shape=RoundedCornerShape(16.dp)){Text("← Perfil")}
    Row(Modifier.align(Alignment.BottomStart).padding(24.dp),verticalAlignment=Alignment.Bottom){
     Surface(Modifier.size(108.dp),shape=RoundedCornerShape(28.dp),color=MpSurface2,border=BorderStroke(3.dp,runCatching{Color(android.graphics.Color.parseColor(color))}.getOrDefault(MpAccent))){
      if(ph.isNotBlank())AsyncImage(ph,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(n.take(1).uppercase(),fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineLarge)}
     }
     Column(Modifier.padding(start=14.dp,bottom=8.dp)){Text(n.ifBlank{"Seu perfil"},fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall);Text("@"+u.removePrefix("@"),color=MpMuted)}
    }
   }
  }
  item{
   Surface(Modifier.fillMaxWidth().padding(18.dp),color=MpSurface,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MpLine)){
    Column(Modifier.padding(20.dp)){
     Text("Editar perfil",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium)
     Text("Personalize sua identidade no MP SCAN. Sua foto atualizada aparece automaticamente nos comentários.",color=MpMuted,modifier=Modifier.padding(top=8.dp,bottom=16.dp))
     Button({photoPicker.launch("image/*")},Modifier.fillMaxWidth()){Text("📷 Escolher foto de perfil")}
     if(ph.isNotBlank())TextButton({ph=""}){Text("Remover foto atual")}
     OutlinedButton({coverPicker.launch("image/*")},Modifier.fillMaxWidth().padding(top=8.dp)){Text("🖼 Escolher capa do perfil")}
     if(co.isNotBlank())TextButton({co=""}){Text("Remover capa atual")}
     Text("Foto: até 4 MB • Capa: até 6 MB",color=MpMuted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(bottom=18.dp))
     OutlinedTextField(n,{n=it.take(40)},Modifier.fillMaxWidth(),label={Text("Nome")},singleLine=true)
     OutlinedTextField(u,{u=it.filterNot(Char::isWhitespace).removePrefix("@").take(24)},Modifier.fillMaxWidth().padding(top=12.dp),label={Text("@username")},singleLine=true)
     OutlinedTextField(b,{b=it.take(280)},Modifier.fillMaxWidth().padding(top=12.dp),label={Text("Bio")},minLines=4,supportingText={Text("${b.length}/280")})
     Text("Cor de destaque do perfil",color=MpMuted,modifier=Modifier.padding(top=18.dp,bottom=10.dp))
     Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){colors.forEach{hex->
      val chosen=color.equals(hex,true);Box(Modifier.size(44.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex))).border(if(chosen)3.dp else 1.dp,if(chosen)Color.White else MpLine,CircleShape).clickable{color=hex})
     }}
     Surface(Modifier.fillMaxWidth().padding(top=20.dp),color=MpSurface2,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MpAccent.copy(.45f))){
      Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
       Text(if(pub)"🌐" else "🔒",style=MaterialTheme.typography.titleLarge)
       Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(if(pub)"Perfil público" else "Perfil privado",fontWeight=FontWeight.Bold);Text(if(pub)"Outras pessoas podem visualizar suas informações públicas." else "Somente seguidores aprovados verão o perfil completo.",color=MpMuted,style=MaterialTheme.typography.bodySmall)}
       Switch(pub,{pub=it})
      }
     }
     Text("Mesmo no modo privado, seu nome, foto e @ continuam aparecendo nos comentários.",color=MpMuted,modifier=Modifier.padding(top=14.dp))
     if(imageError.isNotBlank())Text(imageError,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=10.dp))
     if(externalError.isNotBlank())Text(externalError,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=10.dp))
     Row(Modifier.fillMaxWidth().padding(top=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
      Button({save(x.copy(name=n.trim(),username=u.trim(),bio=b.trim(),photo=ph,cover=co,color=color,isPublic=pub))},Modifier.weight(1f),enabled=n.isNotBlank()&&u.isNotBlank()&&!busy){Text(if(busy)"Salvando..." else "Salvar alterações")}
      OutlinedButton(close,enabled=!busy){Text("Cancelar")}
     }
     if(busy)LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=12.dp))
    }
   }
  }
 }
}
