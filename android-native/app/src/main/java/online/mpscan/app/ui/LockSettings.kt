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
import kotlinx.coroutines.*
import androidx.activity.compose.BackHandler
import online.mpscan.app.ui.MpImage
import androidx.compose.ui.layout.ContentScale

private class LockStore(val context:Context){
 val prefs=context.getSharedPreferences("mp_lock",Context.MODE_PRIVATE)
 var mode:String get()=prefs.getString("mode","").orEmpty();set(v){prefs.edit().putString("mode",v).apply()}
 var wallpaper:String get()=prefs.getString("wallpaper","").orEmpty();set(v){prefs.edit().putString("wallpaper",v).apply()}
 fun hash(value:String,salt:String):String{val spec=javax.crypto.spec.PBEKeySpec(value.toCharArray(),salt.toByteArray(),120000,256);return try{javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded.joinToString(""){"%02x".format(it)}}finally{spec.clearPassword()}}

 fun save(value:String,method:String){val salt=java.util.UUID.randomUUID().toString();check(prefs.edit().putString("salt",salt).putString("hash",hash(value,salt)).putString("mode",method).commit()){ "Não foi possível salvar a senha" }}
 fun matches(value:String)=hash(value,prefs.getString("salt","").orEmpty())==prefs.getString("hash","")
}
@Composable fun LockSettings(back:()->Unit){
 val context=LocalContext.current;val store=remember{LockStore(context)};val scope=rememberCoroutineScope();var busy by remember{mutableStateOf(false)};var active by remember{mutableStateOf(store.mode)};var wallpaper by remember{mutableStateOf(store.wallpaper)}
 var mode by remember{mutableStateOf(store.mode.ifBlank{"PIN"})};var secret by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};var message by remember{mutableStateOf("")};var visible by remember{mutableStateOf(false)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)scope.launch{busy=true;try{wallpaper=withContext(Dispatchers.IO){val file=File(context.filesDir,"lock-wallpaper-${System.currentTimeMillis()}");try{context.contentResolver.openInputStream(uri)?.use{input->file.outputStream().use{input.copyTo(it)}}?:error("Foto indisponível");val options=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true};android.graphics.BitmapFactory.decodeFile(file.absolutePath,options);check(options.outWidth>0&&options.outHeight>0);val old=store.wallpaper;store.wallpaper=file.absolutePath;if(old.isNotBlank())File(old).delete();file.absolutePath}catch(e:Exception){file.delete();throw e}};message="✓ Foto salva. Confira a prévia abaixo."}catch(e:Exception){if(e is CancellationException)throw e;message="Não foi possível salvar a foto. Escolha outra imagem."}finally{busy=false}}}

 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  TextButton(back){Text("← Menu")};Text("Personalizar",style=MaterialTheme.typography.headlineMedium);Text("Tela de bloqueio",color=MpAccent2);Text("Escolha como proteger o aplicativo. Guarde sua senha: ela será solicitada ao abrir e ao voltar ao app.",color=MpMuted)
  Surface(color=MpAccent.copy(alpha=.12f),shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(16.dp)){Text(if(active.isBlank())"Bloqueio desativado" else "✓ Bloqueio ativo · $active",fontWeight=androidx.compose.ui.text.font.FontWeight.Bold);Text("As alterações só são aplicadas ao tocar em Salvar.",color=MpMuted)}}
  if(message.isNotBlank())Text(message,color=MpAccent2)
  if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
  listOf("PIN","Senha","Padrão","Biometria").forEach{item->FilterChip(mode==item,{mode=item;secret="";confirm=""},{Text(when(item){"PIN"->"Código numérico";"Senha"->"Letras e números";"Padrão"->"Ligar os pontos";else->"Biometria / bloqueio do aparelho"})})}
  if(mode=="Padrão"){Text("Escolha pelo menos 4 pontos diferentes, na ordem desejada.");PatternPad(secret){secret=it};TextButton({secret=""}){Text("Limpar padrão")}}
  else if(mode!="Biometria")OutlinedTextField(secret,{secret=if(mode=="PIN")it.filter(Char::isDigit).take(12)else it.take(64)},Modifier.fillMaxWidth(),label={Text("Nova senha")},visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=if(mode=="PIN")KeyboardType.NumberPassword else KeyboardType.Password),trailingIcon={TextButton({visible=!visible}){Text(if(visible)"Ocultar"else"Ver")}})
  if(mode!="Biometria"){
   if(mode=="Padrão"){Text("Repita o padrão");PatternPad(confirm){confirm=it};TextButton({confirm=""}){Text("Limpar confirmação")}}
   else OutlinedTextField(confirm,{confirm=if(mode=="PIN")it.filter(Char::isDigit).take(12)else it.take(64)},Modifier.fillMaxWidth(),label={Text("Confirmar senha")},visualTransformation=PasswordVisualTransformation())
  }
  Button(onClick={val valid=when(mode){"Biometria"->(context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure;"PIN"->secret.length>=4&&secret.all(Char::isDigit)&&secret==confirm;"Padrão"->secret.length>=4&&secret==confirm;else->secret.length>=6&&secret.any(Char::isLetter)&&secret.any(Char::isDigit)&&secret==confirm};if(valid){val selected=mode;val password=secret;scope.launch{busy=true;try{withContext(Dispatchers.Default){if(selected!="Biometria")store.save(password,selected)else store.mode=selected};active=selected;secret="";confirm="";message="✓ Senha aceita. Bloqueio $selected ativado com sucesso."}catch(e:Exception){if(e is CancellationException)throw e;message="Não foi possível salvar. Tente novamente."}finally{busy=false}}}else message=if(mode=="Biometria")"Configure primeiro o bloqueio nos ajustes do celular."else"As duas senhas devem ser iguais. PIN/padrão: mínimo de 4. Senha: 6 caracteres com letras e números."},modifier=Modifier.fillMaxWidth(),enabled=!busy){Text(if(busy)"Salvando…"else"Salvar e ativar bloqueio")}
  if(wallpaper.isNotBlank()){Text("Prévia da tela de bloqueio");MpImage(File(wallpaper),"Foto da tela de bloqueio",Modifier.fillMaxWidth().height(220.dp),contentScale=ContentScale.Crop)}

  OutlinedButton({picker.launch("image/*")},Modifier.fillMaxWidth()){Text("Escolher foto da tela")}
  TextButton({store.wallpaper="";wallpaper="";message="Foto removida."}){Text("Remover foto")}
  TextButton({store.mode="";active="";message="Bloqueio desativado."}){Text("Desativar bloqueio")}
  if(message.isNotBlank())Text(message,color=MpAccent2)
 }
}
@Composable private fun PatternPad(value:String,change:(String)->Unit){
 val latestValue by rememberUpdatedState(value)
 val latestChange by rememberUpdatedState(change)
 var gesture=""
 Canvas(Modifier.size(240.dp).pointerInput(Unit){
  fun hit(position:Offset){val cell=size.width/3f;val column=(position.x/cell).toInt().coerceIn(0,2);val row=(position.y/cell).toInt().coerceIn(0,2);val center=Offset((column+.5f)*cell,(row+.5f)*cell);if((position-center).getDistance()<cell*.4f){val digit=(row*3+column+1).toString();if(digit !in gesture){gesture+=digit;latestChange(gesture)}}}
  detectDragGestures(onDragStart={gesture=latestValue;hit(it)},onDrag={event,_->event.consume();hit(event.position)})
 }){
  val cell=size.width/3f
  fun center(digit:Char):Offset{val n=digit.digitToInt()-1;return Offset((n%3+.5f)*cell,(n/3+.5f)*cell)}
  value.zipWithNext().forEach{(a,b)->drawLine(MpAccent2,center(a),center(b),strokeWidth=6.dp.toPx())}
  ('1'..'9').forEach{digit->drawCircle(if(digit in value)MpAccent2 else MpMuted,radius=11.dp.toPx(),center=center(digit));drawCircle(MpLine,radius=22.dp.toPx(),center=center(digit),style=androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))}
 }
}

@Composable fun AppLock(content:@Composable ()->Unit){
 val context=LocalContext.current;val store=remember{LockStore(context)};val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 val scope=rememberCoroutineScope();var verifying by remember{mutableStateOf(false)}
 var locked by remember{mutableStateOf(store.mode.isNotBlank())};var value by remember{mutableStateOf("")};var error by remember{mutableStateOf("")}
 val device=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->if(result.resultCode==Activity.RESULT_OK){locked=false;value=""}else error="Desbloqueio cancelado. Tente novamente."}
 DisposableEffect(owner){val observer=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP&&store.mode.isNotBlank()){locked=true;value=""}};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}}
 Box(Modifier.fillMaxSize()){
 content()
 if(locked){BackHandler{}
 Box(Modifier.fillMaxSize().background(MpBackground).clickable(enabled=true,onClick={})) {
  if(store.wallpaper.isNotBlank())MpImage(File(store.wallpaper),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
  Surface(Modifier.align(Alignment.Center).padding(24.dp),color=MpSurface.copy(alpha=.96f),shape=RoundedCornerShape(26.dp)){
   Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
    Text("MP SCAN",style=MaterialTheme.typography.headlineMedium);Text("Desbloqueie para continuar",color=MpMuted)
    if(store.mode=="Biometria")Button({@Suppress("DEPRECATION") val intent=(context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).createConfirmDeviceCredentialIntent("MP SCAN","Desbloqueie para continuar");val fallback={if(intent!=null)device.launch(intent)else error="Bloqueio do aparelho indisponível."}
      if(android.os.Build.VERSION.SDK_INT>=28){
       val executor=androidx.core.content.ContextCompat.getMainExecutor(context)
       val builder=android.hardware.biometrics.BiometricPrompt.Builder(context).setTitle("MP SCAN").setSubtitle("Desbloqueie para continuar")
       if(android.os.Build.VERSION.SDK_INT>=30)builder.setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
       else builder.setNegativeButton("Usar senha do aparelho",executor){_,_->fallback()}
       builder.build().authenticate(android.os.CancellationSignal(),executor,object:android.hardware.biometrics.BiometricPrompt.AuthenticationCallback(){
        override fun onAuthenticationSucceeded(result:android.hardware.biometrics.BiometricPrompt.AuthenticationResult){locked=false;value="";error=""}
        override fun onAuthenticationError(code:Int,text:CharSequence){error="Não foi possível desbloquear: $text"}
       })
      }else fallback()}){Text("Desbloquear com o aparelho")}
    else{
     if(store.mode=="Padrão")PatternPad(value){value=it}else OutlinedTextField(value,{value=if(store.mode=="PIN")it.filter(Char::isDigit)else it},label={Text("Senha")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=if(store.mode=="PIN")KeyboardType.NumberPassword else KeyboardType.Password))
     Button(onClick={val entered=value;scope.launch{verifying=true;try{if(withContext(Dispatchers.Default){store.matches(entered)}){locked=false;value="";error=""}else{error="Senha incorreta. Tente novamente.";value=""}}catch(e:Exception){if(e is CancellationException)throw e;error="Não foi possível verificar. Tente novamente."}finally{verifying=false}}},enabled=!verifying){Text(if(verifying)"Verificando…"else"Desbloquear")};if(store.mode=="Padrão")TextButton({value=""}){Text("Limpar")}
    };if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
   }
  }
 }
}
}
}
