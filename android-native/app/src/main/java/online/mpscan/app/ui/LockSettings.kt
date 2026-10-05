package online.mpscan.app.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import online.mpscan.app.data.LocalPhotos
import online.mpscan.app.ui.MpImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

private class LockStore(val context:Context){
 val prefs=context.getSharedPreferences("mp_lock",Context.MODE_PRIVATE)
 var mode:String get()=prefs.getString("mode","").orEmpty();set(v){prefs.edit().putString("mode",v).apply()}
 var wallpaper:String get()=prefs.getString("wallpaper","").orEmpty();set(v){prefs.edit().putString("wallpaper",v).apply()}
 fun hash(value:String,salt:String):String{val spec=javax.crypto.spec.PBEKeySpec(value.toCharArray(),salt.toByteArray(),120000,256);return try{javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded.joinToString(""){"%02x".format(it)}}finally{spec.clearPassword()}}

 fun save(value:String){val salt=java.util.UUID.randomUUID().toString();prefs.edit().putString("salt",salt).putString("hash",hash(value,salt)).apply()}
 fun matches(value:String)=hash(value,prefs.getString("salt","").orEmpty())==prefs.getString("hash","")
}
@Composable fun LockSettings(back:()->Unit){
 val context=LocalContext.current;val store=remember{LockStore(context)};val scope=rememberCoroutineScope()
 var active by remember{mutableStateOf(store.mode)};var editing by remember{mutableStateOf(store.mode.isBlank())};var mode by remember{mutableStateOf(store.mode.ifBlank{"PIN"})}
 var secret by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};var message by remember{mutableStateOf("")};var visible by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var photo by remember{mutableStateOf(store.wallpaper)};var disable by remember{mutableStateOf(false)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)scope.launch{busy=true;runCatching{LocalPhotos.wallpaper(context,uri)}.onSuccess{photo=it;store.wallpaper=it;message="Foto cadastrada."}.onFailure{message="Não foi possível cadastrar esta foto. Escolha outra imagem."};busy=false}}
 if(disable)AlertDialog(onDismissRequest={disable=false},title={Text("Desativar o bloqueio?")},text={Text("O aplicativo deixará de pedir a sua senha ao abrir.")},confirmButton={TextButton({store.mode="";active="";editing=true;secret="";confirm="";disable=false;message="Bloqueio desativado."}){Text("Desativar")}},dismissButton={TextButton({disable=false}){Text("Cancelar")}})
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
  TextButton(back){Text("← Menu")}
  Column{Text("Seu espaço, protegido",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Text("Personalize sua tela de bloqueio com cuidado e conforto.",color=MpMuted,modifier=Modifier.padding(top=8.dp))}
  Surface(color=MpSurface,shape=RoundedCornerShape(28.dp),border=BorderStroke(1.dp,MpLine)){
   Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Text(if(active.isBlank())"Proteja suas leituras"else if(active=="Biometria")"Bloqueio do aparelho ativado"else"Senha cadastrada",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
    Text(if(active.isBlank())"Escolha uma forma de desbloquear o aplicativo."else if(active=="Biometria")"Você já usa o bloqueio do aparelho. Deseja trocar?"else"Você já tem uma senha. Deseja trocar?",color=MpMuted)
    if(active.isNotBlank()&&!editing)Button({editing=true;secret="";confirm="";message=""},shape=RoundedCornerShape(16.dp)){Text(if(active=="Biometria")"Trocar bloqueio"else"Trocar senha")}
    if(editing){
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("PIN","Senha","Padrão","Biometria").forEach{item->FilterChip(mode==item,{mode=item;secret="";confirm=""},label={Text(item)})}}
     Text(when(mode){"PIN"->"Um código com pelo menos 4 números.";"Senha"->"Use pelo menos 6 caracteres, com letras e números.";"Padrão"->"Ligue pelo menos 4 pontos diferentes e repita a mesma sequência.";else->"Use a biometria ou a senha já configurada no celular."},color=MpMuted,style=MaterialTheme.typography.bodySmall)
     if(mode=="Padrão"){
      Text("Novo padrão",fontWeight=FontWeight.Bold);Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){PatternPad(secret){secret=it}};TextButton({secret=""}){Text("Limpar padrão")}
      Text("Repita o padrão",fontWeight=FontWeight.Bold);Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){PatternPad(confirm){confirm=it}};TextButton({confirm=""}){Text("Limpar confirmação")}
     }else if(mode!="Biometria"){
      OutlinedTextField(secret,{secret=if(mode=="PIN")it.filter(Char::isDigit).take(12)else it.take(64)},Modifier.fillMaxWidth(),singleLine=true,enabled=!busy,label={Text(if(mode=="PIN")"Novo código"else"Nova senha")},shape=RoundedCornerShape(18.dp),visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=if(mode=="PIN")KeyboardType.NumberPassword else KeyboardType.Password),trailingIcon={TextButton({visible=!visible}){Text(if(visible)"Ocultar"else"Ver")}})
      OutlinedTextField(confirm,{confirm=if(mode=="PIN")it.filter(Char::isDigit).take(12)else it.take(64)},Modifier.fillMaxWidth(),singleLine=true,enabled=!busy,label={Text("Confirme para cadastrar")},shape=RoundedCornerShape(18.dp),visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=if(mode=="PIN")KeyboardType.NumberPassword else KeyboardType.Password))
     }
     Button({val valid=when(mode){"Biometria"->(context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure;"PIN"->secret.length>=4&&secret.all(Char::isDigit)&&secret==confirm;"Padrão"->secret.length>=4&&secret==confirm;else->secret.length>=6&&secret.any(Char::isLetter)&&secret.any(Char::isDigit)&&secret==confirm};if(valid){scope.launch{busy=true;runCatching{if(mode!="Biometria")withContext(Dispatchers.Default){store.save(secret)};store.mode=mode}.onSuccess{active=mode;editing=false;secret="";confirm="";message=if(mode=="Biometria")"Bloqueio do aparelho ativado."else"Senha cadastrada."}.onFailure{message="Não foi possível cadastrar o bloqueio. Tente novamente."};busy=false}}else message=if(mode=="Biometria")"Configure primeiro o bloqueio nos ajustes do celular."else"Confira os campos: a senha e a confirmação precisam ser iguais e ter o tamanho indicado."},Modifier.fillMaxWidth(),enabled=!busy,shape=RoundedCornerShape(16.dp)){Text(if(busy)"Cadastrando…"else if(active.isBlank())"Cadastrar bloqueio"else"Salvar novo bloqueio")}
     if(active.isNotBlank())TextButton({editing=false;secret="";confirm=""},enabled=!busy){Text("Manter minha senha atual")}
    }
    if(active.isNotBlank())TextButton({disable=true},enabled=!busy){Text("Desativar bloqueio")}
   }
  }
  Surface(color=MpSurface,shape=RoundedCornerShape(28.dp),border=BorderStroke(1.dp,MpLine)){
   Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    Text("Sua tela de boas-vindas",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text(if(photo.isBlank())"Escolha uma foto para aparecer na tela de bloqueio."else"Foto cadastrada",color=MpMuted)
    Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(22.dp)).background(MpSurface2),contentAlignment=Alignment.Center){
     if(photo.isNotBlank())MpImage(File(photo),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
     Box(Modifier.matchParentSize().background(Color.Black.copy(.25f)))
     Surface(Modifier.padding(20.dp),color=MpSurface.copy(.96f),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("MP SCAN",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("Seu próximo capítulo espera por você.",color=MpMuted,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp));Text("● ● ● ●",color=MpAccent,modifier=Modifier.padding(top=14.dp))}}
    }
    OutlinedButton({picker.launch("image/*")},Modifier.fillMaxWidth(),enabled=!busy,shape=RoundedCornerShape(16.dp)){Text(if(photo.isBlank())"Escolher foto"else"Trocar foto")}
    if(photo.isNotBlank())TextButton({store.wallpaper="";photo="";message="Foto removida."},enabled=!busy){Text("Remover foto")}
   }
  }
  if(message.isNotBlank())Surface(color=MpSurface2,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MpLine)){Text(message,Modifier.fillMaxWidth().padding(16.dp),color=MpText)}
  Text("Este bloqueio protege o acesso neste aparelho. Guarde sua senha em um lugar seguro.",color=MpMuted,style=MaterialTheme.typography.bodySmall)
 }
}
@Composable private fun PatternPad(value:String,change:(String)->Unit){
 val latestValue by rememberUpdatedState(value)
 val latestChange by rememberUpdatedState(change)
 val accent=MpAccent2;val muted=MpMuted;val line=MpLine
 Canvas(Modifier.size(240.dp).pointerInput(Unit){
  fun hit(position:Offset){val cell=size.width/3f;val column=(position.x/cell).toInt().coerceIn(0,2);val row=(position.y/cell).toInt().coerceIn(0,2);val center=Offset((column+.5f)*cell,(row+.5f)*cell);if((position-center).getDistance()<cell*.4f){val digit=(row*3+column+1).toString();if(digit !in latestValue)latestChange(latestValue+digit)}}
  detectDragGestures(onDragStart={hit(it)},onDrag={event,_->event.consume();hit(event.position)})
 }){
  val cell=size.width/3f
  fun center(digit:Char):Offset{val n=digit.digitToInt()-1;return Offset((n%3+.5f)*cell,(n/3+.5f)*cell)}
  value.zipWithNext().forEach{(a,b)->drawLine(accent,center(a),center(b),strokeWidth=6.dp.toPx())}
  ('1'..'9').forEach{digit->drawCircle(if(digit in value)accent else muted,radius=11.dp.toPx(),center=center(digit));drawCircle(line,radius=22.dp.toPx(),center=center(digit),style=androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))}
 }
}

@Composable fun AppLock(content:@Composable ()->Unit){
 val context=LocalContext.current;val store=remember{LockStore(context)};val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 var locked by remember{mutableStateOf(store.mode.isNotBlank())};var value by remember{mutableStateOf("")};var error by remember{mutableStateOf("")};var checking by remember{mutableStateOf(false)};val scope=rememberCoroutineScope()
 val device=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->if(result.resultCode==Activity.RESULT_OK){locked=false;value=""}else error="Desbloqueio cancelado. Tente novamente."}
 DisposableEffect(owner){val observer=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP&&store.mode.isNotBlank()){locked=true;value=""}};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}}
 Box(Modifier.fillMaxSize()){content()}
 if(!locked)return
 androidx.compose.ui.window.Dialog(onDismissRequest={},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=false,dismissOnClickOutside=false,decorFitsSystemWindows=false)){
 Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)){
  if(store.wallpaper.isNotBlank())MpImage(File(store.wallpaper),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
  Box(Modifier.matchParentSize().background(Color.Black.copy(.25f)))
  Surface(Modifier.align(Alignment.Center).safeDrawingPadding().imePadding().padding(24.dp).widthIn(max=460.dp),color=MpSurface.copy(alpha=.97f),shape=RoundedCornerShape(30.dp),border=BorderStroke(1.dp,MpLine)){
   Column(Modifier.verticalScroll(rememberScrollState()).padding(26.dp),verticalArrangement=Arrangement.spacedBy(16.dp),horizontalAlignment=Alignment.CenterHorizontally){
    Text("BEM-VINDA À SUA ESTANTE",color=MpAccent,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold);Text("MP SCAN",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Text("Seu próximo capítulo espera por você.",color=MpMuted)
    if(store.mode=="Biometria")Button({@Suppress("DEPRECATION") val intent=(context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).createConfirmDeviceCredentialIntent("MP SCAN","Desbloqueie para continuar");val fallback={if(intent!=null)device.launch(intent)else error="Bloqueio do aparelho indisponível."}
      if(android.os.Build.VERSION.SDK_INT>=28){
       val executor=androidx.core.content.ContextCompat.getMainExecutor(context)
       val builder=android.hardware.biometrics.BiometricPrompt.Builder(context).setTitle("MP SCAN").setSubtitle("Desbloqueie para continuar")
       if(android.os.Build.VERSION.SDK_INT>=30)builder.setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
       else builder.setNegativeButton("Usar senha do aparelho",executor){_,_->fallback()}
       builder.build().authenticate(android.os.CancellationSignal(),executor,object:android.hardware.biometrics.BiometricPrompt.AuthenticationCallback(){
        override fun onAuthenticationSucceeded(result:android.hardware.biometrics.BiometricPrompt.AuthenticationResult){locked=false;value="";error=""}
        override fun onAuthenticationError(code:Int,text:CharSequence){if(code!=android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED&&code!=13)fallback()}
       })
      }else fallback()}){Text("Desbloquear com o aparelho")}
    else{
     if(store.mode=="Padrão")PatternPad(value){value=it}else OutlinedTextField(value,{value=if(store.mode=="PIN")it.filter(Char::isDigit)else it},Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(18.dp),label={Text(if(store.mode=="PIN")"Seu código"else"Sua senha")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=if(store.mode=="PIN")KeyboardType.NumberPassword else KeyboardType.Password))
     Button({scope.launch{checking=true;error="";val candidate=value;val matches=runCatching{withContext(Dispatchers.Default){store.matches(candidate)}}.getOrDefault(false);if(matches&&owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)){locked=false;value=""}else{error="Senha incorreta. Tente novamente.";value=""};checking=false}},Modifier.fillMaxWidth(),enabled=!checking,shape=RoundedCornerShape(16.dp)){Text(if(checking)"Verificando…"else"Entrar na minha estante")};if(store.mode=="Padrão")TextButton({value=""}){Text("Limpar")}
    };if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
   }
  }
 }
}
}
