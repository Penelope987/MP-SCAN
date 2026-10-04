package online.mpscan.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.*
import kotlinx.coroutines.launch
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*

@Composable fun AuthScreen(close:()->Unit,mandatory:Boolean=false,signedIn:(AccountSession)->Unit){
 val context=LocalContext.current;val repo=remember{AccountRepository()};val scope=rememberCoroutineScope()
 var mode by remember{mutableStateOf("Entrar")};var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var name by remember{mutableStateOf("")};var username by remember{mutableStateOf("")};var visible by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var notice by remember{mutableStateOf("")}
 fun runAction(action:suspend ()->Unit){if(busy)return;scope.launch{busy=true;error="";notice="";try{action()}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){error=e.message?:"Não foi possível concluir. Tente novamente."}finally{busy=false}}}
 Column(Modifier.fillMaxSize().background(MpBackground).safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Spacer(Modifier.height(if(mandatory)36.dp else 0.dp));if(!mandatory)TextButton(close,enabled=!busy){Text("← Voltar")};Surface(color=MpAccent.copy(.12f),shape=RoundedCornerShape(20.dp)){Text("MP SCAN",color=MpAccent,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(18.dp))};Text(if(mode=="Cadastrar")"Sua história começa aqui"else if(mode=="Recuperar")"Recupere seu acesso"else "Sua próxima história espera por você",fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium);Text("Sua biblioteca, suas leituras e sua comunidade em um só lugar.",color=MpMuted)
  Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){FilterChip(mode=="Entrar",{mode="Entrar";error=""},{Text("Entrar")},enabled=!busy);FilterChip(mode=="Cadastrar",{mode="Cadastrar";error=""},{Text("Cadastrar")},enabled=!busy)}
  if(mode=="Cadastrar"){OutlinedTextField(name,{name=it.take(40)},Modifier.fillMaxWidth(),label={Text("Nome")},singleLine=true,enabled=!busy);OutlinedTextField(username,{username=it.filterNot(Char::isWhitespace).take(24)},Modifier.fillMaxWidth(),label={Text("Arroba")},singleLine=true,enabled=!busy)}
  OutlinedTextField(email,{email=it},Modifier.fillMaxWidth(),label={Text("E-mail")},singleLine=true,enabled=!busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email))
  if(mode!="Recuperar")OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text("Senha")},singleLine=true,enabled=!busy,visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),trailingIcon={TextButton({visible=!visible}){Text(if(visible)"Ocultar"else"Ver")}})
  Button({runAction{when(mode){"Cadastrar"->signedIn(repo.signUp(name,username,email,password));"Recuperar"->{repo.resetPassword(email);notice="Se este e-mail estiver cadastrado, você receberá as instruções para redefinir a senha."};else->signedIn(repo.signIn(email,password))}}},Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp),enabled=!busy&&email.isNotBlank()&&(mode=="Recuperar"||password.isNotBlank())){Text(if(busy)"Aguarde…"else if(mode=="Recuperar")"Enviar instruções"else mode,fontWeight=FontWeight.Bold)}
  if(mode!="Recuperar"){
   TextButton({mode="Recuperar"},enabled=!busy){Text("Esqueceu sua senha?")}
   HorizontalDivider(color=MpLine)
   OutlinedButton({runAction{
    val clientId=runCatching{repo.googleClientId()}.getOrElse{failure->val resource=context.resources.getIdentifier("default_web_client_id","string",context.packageName);if(resource==0)throw failure;context.getString(resource)}
    val option=GetSignInWithGoogleOption.Builder(clientId).build()
    val result=CredentialManager.create(context).getCredential(context,GetCredentialRequest.Builder().addCredentialOption(option).build())
    val credential=result.credential
    check(credential is CustomCredential&&credential.type==GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){"Não foi possível obter a conta Google."}
    signedIn(repo.signInGoogle(GoogleIdTokenCredential.createFrom(credential.data).idToken))
   }},Modifier.fillMaxWidth().height(52.dp),enabled=!busy,shape=RoundedCornerShape(16.dp)){Text("G  ·  Entrar com Google",fontWeight=FontWeight.Bold)}
  }
  Text("Entre para acessar suas leituras e participar da comunidade.",color=MpMuted,style=MaterialTheme.typography.bodySmall)
  val uri=androidx.compose.ui.platform.LocalUriHandler.current
  TextButton({uri.openUri("https://www.mpscan.online/#/termos")}){Text("Termos de uso e privacidade",style=MaterialTheme.typography.labelSmall)}
  if(busy)LinearProgressIndicator(Modifier.fillMaxWidth());if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error);if(notice.isNotBlank())Text(notice,color=MpAccent2)
 }
}
