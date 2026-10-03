package online.mpscan.app.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import online.mpscan.app.ui.theme.*
import java.io.File
import java.security.MessageDigest
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

private class LockStore(val context:Context){
 val prefs=context.getSharedPreferences("mp_lock",Context.MODE_PRIVATE)
 var mode:String get()=prefs.getString("mode","").orEmpty();set(v){prefs.edit().putString("mode",v).apply()}
 var wallpaper:String get()=prefs.getString("wallpaper","").orEmpty();set(v){prefs.edit().putString("wallpaper",v).apply()}
 fun hash(value:String,salt:String)=MessageDigest.getInstance("SHA-256").digest((salt+value).toByteArray()).joinToString(""){"%02x".format(it)}
 fun save(value:String){val salt=java.util.UUID.randomUUID().toString();prefs.edit().putString("salt",salt).putString("hash",hash(value,salt)).apply()}
 fun matches(value:String)=hash(value,prefs.getString("salt","").orEmpty())==prefs.getString("hash","")
}
@Composable fun LockSettings(back:()->Unit){
 val context=LocalContext.current;val store=remember{LockStore(context)}
 var mode by remember{mutableStateOf(store.mode.ifBlank{"PIN"})};var secret by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};var message by remember{mutableStateOf("")};var visible by remember{mutableStateOf(false)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)runCatching{val file=File(context.filesDir,"lock-wallpaper");context.contentResolver.openInputStream(uri)!!.use{input->file.outputStream().use{input.copyTo(it)}};store.wallpaper=file.absolutePath}.onSuccess{message="Foto da tela de bloqueio salva."}.onFailure{message="Não foi possível salvar a foto."}}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  TextButton(back){Text("← Menu")};Text("Personalizar",style=MaterialTheme.typography.headlineMedium);Text("Tela de bloqueio",color=MpAccent2);Text("Escolha como proteger o aplicativo. Guarde sua senha: ela será solicitada ao abrir e ao voltar ao app.",color=MpMuted)
  listOf("PIN","Senha","Padrão","Biometria").forEach{item->FilterChip(mode==item,{mode=item;secret="";confirm=""},{Text(when(item){"PIN"->"Código numérico";"Senha"->"Letras e números";"Padrão"->"Ligar os pontos";else->"Biometria / bloqueio do aparelho"})})}
  if(mode=="Padrão"){Text("Escolha pelo menos 4 pontos diferentes, na ordem desejada.");PatternPad(secret){secret=it};TextButton({secret=""}){Text("Limpar padrão")}}
  else if(mode!="Biometria")OutlinedTextField(secret,{secret=if(mode=="PIN")it.filter(Char::isDigit).take(12)else it.take(64)},Modifier.fillMaxWidth(),label={Text("Nova senha")},visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=if(mode=="PIN")KeyboardType.NumberPassword else KeyboardType.Password),trailingIcon={TextButton({visible=!visible}){Text(if(visible)"Ocultar"else"Ver")}})
  if(mode!="Biometria"){
   if(mode=="Padrão"){Text("Repita o padrão");PatternPad(confirm){confirm=it};TextButton({confirm=""}){Text("Limpar confirmação")}}
   else OutlinedTextField(confirm,{confirm=if(mode=="PIN")it.filter(Char::isDigit).take(12)else it.take(64)},Modifier.fillMaxWidth(),label={Text("Confirmar senha")},visualTransformation=PasswordVisualTransformation())
  }
  Button({val valid=when(mode){"Biometria"->(context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure;"PIN"->secret.length>=4&&secret.all(Char::isDigit)&&secret==confirm;"Padrão"->secret.length>=4&&secret==confirm;else->secret.length>=6&&secret.any(Char::isLetter)&&secret.any(Char::isDigit)&&secret==confirm};if(valid){if(mode!="Biometria")store.save(secret);store.mode=mode;message="Bloqueio ativado."}else message=if(mode=="Biometria")"Configure primeiro o bloqueio e a biometria nos ajustes do celular."else"Confira a senha e a confirmação. PIN e padrão: mínimo de 4; senha: 6 caracteres com letras e números."},Modifier.fillMaxWidth()){Text("Ativar bloqueio")}
  OutlinedButton({picker.launch("image/*")},Modifier.fillMaxWidth()){Text("Escolher foto da tela")}
  TextButton({store.wallpaper="";message="Foto removida."}){Text("Remover foto")}
  TextButton({store.mode="";message="Bloqueio desativado."}){Text("Desativar bloqueio")}
  if(message.isNotBlank())Text(message,color=MpAccent2)
 }
}
@Composable private fun PatternPad(value:String,change:(String)->Unit){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){(0..2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){(1..3).forEach{column->val digit=(row*3+column).toString();OutlinedButton({if(digit !in value)change(value+digit)},Modifier.size(58.dp),shape=RoundedCornerShape(29.dp)){Text(if(digit in value)"●"else"○",color=if(digit in value)MpAccent2 else MpMuted)}}}}}}
@Composable fun AppLock(content:@Composable ()->Unit){
 val context=LocalContext.current;val store=remember{LockStore(context)};val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 var locked by remember{mutableStateOf(store.mode.isNotBlank())};var value by remember{mutableStateOf("")};var error by remember{mutableStateOf("")}
 val device=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->if(result.resultCode==Activity.RESULT_OK){locked=false;value=""}else error="Desbloqueio cancelado. Tente novamente."}
 DisposableEffect(owner){val observer=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP&&store.mode.isNotBlank()){locked=true;value=""}};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}}
 if(!locked){content();return}
 Box(Modifier.fillMaxSize().background(MpBackground)){
  if(store.wallpaper.isNotBlank())AsyncImage(File(store.wallpaper),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
  Surface(Modifier.align(Alignment.Center).padding(24.dp),color=MpSurface.copy(alpha=.96f),shape=RoundedCornerShape(26.dp)){
   Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
    Text("MP SCAN",style=MaterialTheme.typography.headlineMedium);Text("Desbloqueie para continuar",color=MpMuted)
    if(store.mode=="Biometria")Button({@Suppress("DEPRECATION") val intent=(context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).createConfirmDeviceCredentialIntent("MP SCAN","Desbloqueie para continuar");if(intent!=null)device.launch(intent)else error="Bloqueio do aparelho indisponível."}){Text("Desbloquear com o aparelho")}
    else{
     if(store.mode=="Padrão")PatternPad(value){value=it}else OutlinedTextField(value,{value=if(store.mode=="PIN")it.filter(Char::isDigit)else it},label={Text("Senha")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=if(store.mode=="PIN")KeyboardType.NumberPassword else KeyboardType.Password))
     Button({if(store.matches(value)){locked=false;value=""}else{error="Senha incorreta.";value=""}}){Text("Desbloquear")};if(store.mode=="Padrão")TextButton({value=""}){Text("Limpar")}
    };if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
   }
  }
 }
}
