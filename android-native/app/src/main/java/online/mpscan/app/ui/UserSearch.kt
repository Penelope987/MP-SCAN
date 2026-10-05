package online.mpscan.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.*
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*
import org.json.JSONObject

@Composable fun UserSearchResults(query:String,onSelect:((ProfilePerson)->Unit)?=null){
 val context=LocalContext.current;val online=networkAvailable();val uid=AccountStore(context).session()?.uid.orEmpty()
 var people by remember{mutableStateOf(emptyList<ProfilePerson>())};var loading by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var selected by remember{mutableStateOf<String?>(null)}
 LaunchedEffect(query,online,uid){people=emptyList();error="";loading=false;if(query.trim().removePrefix("@").length<2)return@LaunchedEffect
  loading=true;delay(250)
  try{if(!online)error="Conecte à internet para pesquisar perfis."else people=UserDirectory.people(uid).filter{UserDirectory.matches(it,query)}.sortedBy{it.username.lowercase()}.take(12)}
  catch(e:CancellationException){throw e}catch(e:Exception){error="Não foi possível pesquisar perfis agora."}finally{loading=false}
 }
 selected?.let{NativeProfileDialog(it){selected=null}}
 if(query.trim().removePrefix("@").length>=2)Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text("Usuários",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
  if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(error.isNotBlank())Text(error,color=MpMuted,style=MaterialTheme.typography.bodySmall)
  if(!loading&&error.isBlank()&&people.isEmpty())Text("Nenhum perfil encontrado.",color=MpMuted,style=MaterialTheme.typography.bodySmall)
  people.forEach{person->UserIdentityCard(person){if(onSelect!=null)onSelect(person)else selected=person.uid}}
 }
}
@Composable fun UserIdentityCard(person:ProfilePerson,click:()->Unit){
 Surface(Modifier.fillMaxWidth().clickable(onClick=click),color=MpSurface2,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MpLine)){
  Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){FramedAvatar(person.photo,person.name,size=46.dp);Column(Modifier.weight(1f).padding(start=12.dp)){Text(person.name,fontWeight=FontWeight.Bold,maxLines=2);if(person.username.isNotBlank())Text("@"+person.username.removePrefix("@"),color=MpAccent,style=MaterialTheme.typography.bodySmall)};Text("›",color=MpMuted)}
 }
}
@Composable fun NativeProfileDialog(uid:String,close:()->Unit){
 var profile by remember(uid){mutableStateOf<JSONObject?>(null)};var error by remember(uid){mutableStateOf("")};var attempt by remember(uid){mutableIntStateOf(0)}
 LaunchedEffect(uid,attempt){profile=null;error="";try{profile=UserDirectory.profile(uid)}catch(e:CancellationException){throw e}catch(e:Exception){error="Não foi possível abrir o perfil. Confira sua conexão."}}
 Dialog(close){Surface(Modifier.fillMaxWidth().widthIn(max=560.dp),color=MpSurface,shape=RoundedCornerShape(28.dp)){
  Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
   Row{Text("Perfil",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);TextButton(close){Text("Fechar")}}
   if(error.isNotBlank()){Text(error,color=MpMuted);TextButton({attempt++}){Text("Tentar novamente")}}
   else if(profile==null)CircularProgressIndicator()
   else{val p=profile!!;if(p.length()==0)Text("Este perfil não está disponível.",color=MpMuted)else{
    val person=ProfileIdentity.person(uid,p);FramedAvatar(person.photo,person.name,p.optString("molduraPerfilId"),84.dp);Text(person.name,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall);Text("@"+person.username.removePrefix("@"),color=MpAccent)
    if(p.optBoolean("publico",false)){if(p.optString("bio").isNotBlank())Text(p.optString("bio"),color=MpMuted)}else Text("Perfil privado. As informações pessoais ficam restritas conforme as permissões desta conta.",color=MpMuted)
   }}
  }
 }}
}
