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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import online.mpscan.app.ui.MpImage
import kotlinx.coroutines.launch
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*

@Composable fun AccountProfileScreen(openSettings:()->Unit){
 val ctx=LocalContext.current;val store=remember{AccountStore(ctx)};val repo=remember{AccountRepository()};val scope=rememberCoroutineScope()
 var session by remember{mutableStateOf(store.session())};var profile by remember{mutableStateOf(ProfileSnapshots.get(session?.uid))};var frameKind by remember{mutableStateOf("comment")};var frames by remember{mutableStateOf<List<CommentFrame>>(emptyList())};var extras by remember{mutableStateOf(ProfileExtras(emptyList(),emptyList(),emptyList(),emptyList(),emptyList()))}
 var connectionsLoading by remember{mutableStateOf(true)}
 var tab by remember{mutableStateOf("Visão geral")};var busy by remember{mutableStateOf(session!=null)};var error by remember{mutableStateOf("")};var login by remember{mutableStateOf(false)};var edit by remember{mutableStateOf(false)}
 fun load(){val ss=session?:return
  scope.launch{busy=true;runCatching{repo.profile(ss)}.onSuccess{profile=it;ProfileSnapshots.save(it);error=""}.onFailure{error=it.message?:"Não foi possível carregar o perfil."};busy=false}
  scope.launch{connectionsLoading=true;runCatching{repo.connections(ss)}.onSuccess{extras=extras.copy(followers=it.first,following=it.second);profile=profile?.copy(followers=it.first.size,following=it.second.size);profile?.let(ProfileSnapshots::save)}.onFailure{error="Não foi possível carregar os seguidores. Tente novamente."};connectionsLoading=false}
  scope.launch{runCatching{repo.extras(ss)}.onSuccess{extras=it;profile=profile?.copy(followers=it.followers.size,following=it.following.size,comments=it.activities.count{a->a.type=="comentario"})}}
 }
 LaunchedEffect(session?.uid){profile=ProfileSnapshots.get(session?.uid);if(session!=null)load()}
 LaunchedEffect(tab,session?.uid,frameKind){if(tab=="Molduras")session?.let{runCatching{repo.frames(it,frameKind)}.onSuccess{frames=it}.onFailure{error="Não foi possível carregar as molduras."}}}
 if(login){AuthScreen({login=false}){session=it;store.save(it);login=false};return}
 val p=profile;val admin=p?.role.equals("ADM",true)||p?.role.equals("Administrador",true)
 if(edit&&p!=null){
  ProfileEditScreen(p,busy,error,{edit=false}){updated->
   scope.launch{busy=true;runCatching{repo.saveProfile(session!!,updated)}.onSuccess{profile=updated;ProfileSnapshots.save(updated);edit=false;error=""}.onFailure{error=it.message?:"Não foi possível salvar o perfil."};busy=false}
  }
  return
 }
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=34.dp)){
  item{
   Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(MpAccent.copy(.1f),MpBackground))).padding(18.dp)){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("SEU ESPAÇO NA MP SCAN",color=MpAccent,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall);Text("Meu perfil",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(top=6.dp))};OutlinedButton(openSettings,shape=RoundedCornerShape(16.dp)){Text("Ajustes")}}
    if(!p?.cover.isNullOrBlank())MpImage(p!!.cover,null,Modifier.fillMaxWidth().padding(top=20.dp).height(130.dp).clip(RoundedCornerShape(24.dp)),contentScale=ContentScale.Crop)
    Row(Modifier.padding(top=22.dp),verticalAlignment=Alignment.CenterVertically){
     FramedAvatar(p?.photo.orEmpty(),p?.name?:"MP",p?.avatarFrameId.orEmpty(),90.dp)
     Column(Modifier.weight(1f).padding(start=18.dp)){Text(p?.name?:"Seu perfil",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall);if(!p?.username.isNullOrBlank())Text("@${p!!.username.removePrefix("@")}",color=MpMuted,modifier=Modifier.padding(top=4.dp));if(admin)Surface(Modifier.padding(top=8.dp),color=MpAccent.copy(.12f),shape=RoundedCornerShape(14.dp)){Text("✦ ADMINISTRADOR",Modifier.padding(horizontal=10.dp,vertical=5.dp),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold,color=MpAccent)}}
    }
   }
  }
  item{Column(Modifier.padding(18.dp)){
   if(session==null){Text("Entre para carregar seu perfil e suas molduras.",color=MpMuted);Button({login=true},Modifier.fillMaxWidth().padding(top=12.dp)){Text("Entrar na conta")}}
   else{
    if(!p?.bio.isNullOrBlank())Surface(Modifier.fillMaxWidth(),color=MpSurface,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MpLine)){Text(p!!.bio,Modifier.padding(16.dp))}
    Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){Button({edit=true},Modifier.weight(1f),enabled=p!=null,shape=RoundedCornerShape(16.dp)){Text("Editar perfil")};OutlinedButton({store.clear();session=null;profile=null},shape=RoundedCornerShape(16.dp)){Text("Sair da conta")}}
    Row(Modifier.fillMaxWidth().padding(top=18.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("${p?.followers?:0}","Seguidores",Modifier.weight(1f));Stat("${p?.following?:0}","Seguindo",Modifier.weight(1f));Stat("${p?.comments?:0}","Comentários",Modifier.weight(1f))}
   }
   if(busy)LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=12.dp));if(error.isNotBlank()){Text(error,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=10.dp));TextButton({load()}){Text("Tentar novamente")}}
  }}
  if(session!=null){
   item{AchievementCard()}
   if(admin)item{AdminCard()}
   item{LazyRow(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(listOf("Visão geral","Atividade","Seguidores","Seguindo","Favoritos","Coleções","Molduras")){name->FilterChip(tab==name,{tab=name},{Text(name)})}}}
   item{when(tab){
    "Molduras"->Column{Row(Modifier.padding(horizontal=18.dp)){FilterChip(frameKind=="comment",{frameKind="comment"},{Text("Comentários")});Spacer(Modifier.width(8.dp));FilterChip(frameKind=="avatar",{frameKind="avatar"},{Text("Foto do perfil")})};FramesPanel(frames,if(frameKind=="avatar")p?.avatarFrameId.orEmpty()else p?.frameId.orEmpty(),
     action={frame,claim->val current=profile?:return@FramesPanel;scope.launch{busy=true;runCatching{if(claim)repo.claimFrame(session!!,current,frame)else repo.selectFrame(session!!,current,frame.id,frameKind)}.onSuccess{profile=if(frameKind=="avatar")current.copy(avatarFrameId=frame.id)else current.copy(frameId=frame.id);profile?.let(ProfileSnapshots::save);frames=frames.map{it.copy(owned=it.owned||it.id==frame.id)};error=""}.onFailure{error=it.message?:"Não foi possível usar esta moldura."};busy=false}},
     clear={val current=profile?:return@FramesPanel;scope.launch{busy=true;runCatching{repo.clearFrame(session!!,current,frameKind)}.onSuccess{profile=if(frameKind=="avatar")current.copy(avatarFrameId="")else current.copy(frameId="");profile?.let(ProfileSnapshots::save);error=""}.onFailure{error=it.message?:"Não foi possível remover a moldura."};busy=false}}
    )}
    "Seguidores"->if(connectionsLoading)Column(Modifier.padding(18.dp)){LinearProgressIndicator(Modifier.fillMaxWidth());Text("Buscando seus seguidores…",color=MpMuted,modifier=Modifier.padding(top=12.dp))}else PeoplePanel("Seus seguidores",extras.followers)
    "Seguindo"->if(connectionsLoading)Column(Modifier.padding(18.dp)){LinearProgressIndicator(Modifier.fillMaxWidth());Text("Buscando os perfis que você segue…",color=MpMuted,modifier=Modifier.padding(top=12.dp))}else PeoplePanel("Pessoas que você segue",extras.following)
    "Atividade"->ActivitiesPanel(extras.activities)
    "Favoritos"->WorksPanel("Favoritos",extras.favorites)
    "Coleções"->CollectionsPanel(extras.collections)
    else->OverviewPanel(extras)
   }}
  }
 }
}
@Composable private fun AchievementCard(){Surface(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),color=MpSurface,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MpLine)){Column(Modifier.padding(20.dp)){Text("Sua identidade de leitor",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text("Organize suas coleções, acompanhe sua atividade e escolha uma moldura para seus comentários.",color=MpMuted,modifier=Modifier.padding(top=8.dp))}}}
@Composable private fun AdminCard(){val uri=LocalUriHandler.current;Surface(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),color=MpAccent.copy(.08f),shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MpAccent.copy(.25f))){Column(Modifier.padding(20.dp)){Text("Painel administrativo",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text("Gerencie a MP SCAN no painel do site.",color=MpMuted,modifier=Modifier.padding(top=6.dp));Button({uri.openUri("https://www.mpscan.online/#/adm/visao-geral")},Modifier.fillMaxWidth().padding(top=16.dp)){Text("Abrir painel ADM")}}}}
@Composable private fun FramesPanel(frames:List<CommentFrame>,selected:String,action:(CommentFrame,Boolean)->Unit,clear:()->Unit){
 val owned=frames.filter{it.owned};val available=frames.filterNot{it.owned}
 Column(Modifier.padding(horizontal=18.dp)){
  Text(if(frames.firstOrNull()?.kind=="avatar")"Sua foto, sua identidade"else"Moldura do comentário",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall)
  Text("Escolha uma arte para sua identidade na comunidade MP SCAN.",color=MpMuted,modifier=Modifier.padding(top=5.dp,bottom=12.dp))
  OutlinedButton(clear,Modifier.fillMaxWidth(),enabled=selected.isNotBlank()){Text(if(selected.isBlank())"Sem moldura" else "Remover moldura atual")}
  FrameGroup("Minhas molduras","As molduras que você pegou ficam guardadas aqui.",owned,selected,action,true)
  FrameGroup("Catálogo MP SCAN","Molduras publicadas pelo ADM e disponíveis para sua coleção.",available,selected,action,false)
 }
}
@Composable private fun FrameGroup(title:String,subtitle:String,frames:List<CommentFrame>,selected:String,action:(CommentFrame,Boolean)->Unit,owned:Boolean){
 Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=22.dp))
 Text(subtitle,color=MpMuted,modifier=Modifier.padding(top=4.dp,bottom=10.dp))
 if(frames.isEmpty()){
  Surface(Modifier.fillMaxWidth().padding(bottom=14.dp),color=MpSurface,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MpLine)){Text(if(owned)"Você ainda não pegou nenhuma moldura." else "Não há novas molduras disponíveis.",color=MpMuted,modifier=Modifier.padding(18.dp))}
 }else for(frame in frames){
  val chosen=selected==frame.id
  Surface(Modifier.fillMaxWidth().padding(bottom=14.dp),color=MpSurface,shape=RoundedCornerShape(22.dp),border=BorderStroke(if(chosen)3.dp else 1.dp,if(chosen)MpAccent else MpLine)){
   Column{
    if(frame.kind=="avatar")Box(Modifier.fillMaxWidth().padding(20.dp),contentAlignment=Alignment.Center){FramedAvatar("","MP",frame.id,120.dp,org.json.JSONObject().put("imagemUrl",frame.image).put("cor",frame.color).put("effect",frame.effect))}else {Surface(Modifier.fillMaxWidth().heightIn(min=150.dp).commentMotion(frame.effect,frame.speed),color=runCatching{Color(android.graphics.Color.parseColor(frame.background))}.getOrDefault(MpSurface),shape=RoundedCornerShape(frame.radius.coerceIn(8,40).dp)){Box{if(frame.image.isNotBlank())MpImage(frame.image,null,Modifier.matchParentSize(),contentScale=ContentScale.Fit);Column(Modifier.padding(frame.padding.coerceIn(10,34).dp)){Text("MP SCAN • seu comentário",color=Color.White,fontWeight=FontWeight.Bold);Text("Uma nova história, uma identidade só sua.",color=Color.White.copy(.8f),modifier=Modifier.padding(top=18.dp))}}}}
    Column(Modifier.padding(16.dp)){
     Text(frame.name,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)
     Text(if(chosen)"USANDO" else if(owned)"NA COLEÇÃO" else "DISPONÍVEL",color=if(chosen||owned)MpAccent2 else MpAccent,style=MaterialTheme.typography.labelSmall)
     Button({action(frame,!owned)},Modifier.fillMaxWidth().padding(top=10.dp),enabled=!chosen){
      Text(if(chosen)"✓ Moldura em uso" else if(owned)"Usar moldura" else "＋ Pegar e usar")
     }
    }
   }
  }
 }
}
@Composable private fun PeoplePanel(title:String,people:List<ProfilePerson>){
 val uri=LocalUriHandler.current
 Column(Modifier.padding(horizontal=18.dp)){
  Text(title,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)
  if(people.isEmpty()) Text("Nenhum perfil encontrado.",color=MpMuted,modifier=Modifier.padding(vertical=20.dp))
  else for(person in people){
   Surface(Modifier.fillMaxWidth().padding(top=9.dp).clickable{uri.openUri("https://www.mpscan.online/#/perfil/${person.uid}")},color=MpSurface,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,MpLine)){
    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
     Surface(Modifier.size(52.dp),shape=CircleShape,color=MpSurface2){if(person.photo.isNotBlank())MpImage(person.photo,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(person.name.take(1),fontWeight=FontWeight.Bold)}}
     Column(Modifier.weight(1f).padding(start=12.dp)){Text(person.name,fontWeight=FontWeight.Bold);if(person.username.isNotBlank())Text("@"+person.username.removePrefix("@"),color=MpMuted)}
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
     if(work.cover.isNotBlank())MpImage(work.cover,null,Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
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
   Box(Modifier.fillMaxWidth().height(330.dp).background(Brush.linearGradient(listOf(MpAccent.copy(.18f),MpSurface,MpBackground)))){
    if(co.isNotBlank())MpImage(co,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,MpBackground.copy(.92f)))))
    OutlinedButton(close,Modifier.padding(16.dp),shape=RoundedCornerShape(16.dp)){Text("← Perfil")}
    Row(Modifier.align(Alignment.BottomStart).padding(24.dp),verticalAlignment=Alignment.Bottom){
     Surface(Modifier.size(108.dp),shape=RoundedCornerShape(28.dp),color=MpSurface2,border=BorderStroke(3.dp,runCatching{Color(android.graphics.Color.parseColor(color))}.getOrDefault(MpAccent))){
      if(ph.isNotBlank())MpImage(ph,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(n.take(1).uppercase(),fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineLarge)}
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
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)){colors.forEach{hex->
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
