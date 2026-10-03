package online.mpscan.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*

@Composable fun DiscoveryScreen(works:List<Work>,loading:Boolean,error:String,retry:()->Unit,finish:()->Unit,account:()->Unit){
 val context=LocalContext.current;val store=remember{SettingsStore(context)}
 var step by rememberSaveable{mutableIntStateOf(0)};var index by rememberSaveable{mutableIntStateOf(0)};var display by remember{mutableStateOf(store.adultDisplay)};var votes by remember{mutableStateOf(store.choices())};var round by remember{mutableIntStateOf(0)}
 // Keep a stable deck while voting. Only changing the display preference or catalog rebuilds it.
 val deck=remember(works,display,round){works.filter{DiscoveryPolicy.eligible(it,display)}.let{all->all.filter{store.choices()[it.id]==null}.ifEmpty{if(round>0)all else emptyList()}}}
 fun close(){store.discoveryDone=true;finish()}
 fun vote(choice:String){deck.getOrNull(index)?.let{store.choose(it.id,choice);votes=store.choices();index++;if(index>=deck.size)step=3}}
 BackHandler{if(step>0)step-- else close()}
 CompositionLocalProvider(LocalAdultDisplay provides display){
 Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MpAccent.copy(alpha=.15f),MpBackground,MpBackground))).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton({if(step>0)step-- else close()}){Text("← Voltar")};Spacer(Modifier.weight(1f));Text("${step+1} / 4",color=MpMuted);TextButton({step=3}){Text("Ignorar")}}
 LinearProgressIndicator(progress={(step+1)/4f},modifier=Modifier.fillMaxWidth())
 AnimatedContent(step,label="boas-vindas"){current->Column(verticalArrangement=Arrangement.spacedBy(16.dp)){
 when(current){
 0->{Surface(color=MpAccent,shape=RoundedCornerShape(24.dp)){Text("MP",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Black,color=androidx.compose.ui.graphics.Color.White,modifier=Modifier.padding(20.dp))};Kicker("SEU UNIVERSO DE HISTÓRIAS");Text("Bem-vindo à\nMP SCAN.",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Black);Text("Manhwas, mangás, manhuas e novels.\nVamos descobrir a sua próxima leitura?",color=MpMuted);Fan(works.filter{DiscoveryPolicy.visible(it.adult,display)});Button({step=1},Modifier.fillMaxWidth()){Text("Encontrar minha próxima história")};TextButton({step=3},Modifier.fillMaxWidth()){Text("Prefiro explorar por conta própria")}}
 1->{Kicker("SEU CONFORTO VEM PRIMEIRO");Text("Como você prefere ver as obras +18?",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Text("A exibição das capas é uma preferência visual. Ela não substitui a verificação etária nem as permissões de leitura.",color=MpMuted);AdultDisplayOptions(display){display=it;store.adultDisplay=it;index=0};Button({step=2},Modifier.fillMaxWidth()){Text("Escolher minhas histórias")}}
 2->{Kicker("SEU GOSTO, SUAS ESCOLHAS");Text("Qual delas você leria?",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Text("Gostou? Coração. Não combina? X.\nAinda não sabe? Pode pular.",color=MpMuted)
 val work=deck.getOrNull(index)
 when{loading&&deck.isEmpty()->CircularProgressIndicator();error.isNotBlank()&&deck.isEmpty()->{Text(error);Button(retry){Text("Tentar novamente")}};work==null->Text("Você chegou ao fim das opções. Suas escolhas estão guardadas.",color=MpMuted);else->{Surface(color=MpSurface,shape=RoundedCornerShape(26.dp)){Column{MpImage(work.cover,work.title,Modifier.fillMaxWidth().height(290.dp),contentScale=ContentScale.Crop);Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text(work.type+(if(work.adult)" · +18"else""),color=MpAccent2);Text(work.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(work.genres.joinToString(" · "),color=MpMuted);Text(work.synopsis.ifBlank{"Conheça esta história na MP SCAN."},maxLines=5)}}};Text("${votes.values.count{it=="like"}} histórias curtidas · ${index+1} de ${deck.size}",color=MpMuted);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({vote("no")},Modifier.weight(1f)){Text("× Não")};OutlinedButton({vote("skip")},Modifier.weight(1f)){Text("Pular")};Button({vote("like")},Modifier.weight(1f)){Text("♥ Gostei")}}}}
 Button({step=3},Modifier.fillMaxWidth()){Text("Confirmar minhas escolhas")}}
 3->{Kicker("TUDO PRONTO!");Text("Seu próximo capítulo começa aqui.",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);val count=votes.values.count{it=="like"};Text(if(count>0)"$count histórias curtidas. Suas escolhas ajudam a encontrar obras que combinam com você."else"Você pode explorar livremente e escolher suas preferências depois.",color=MpMuted);Fan(works.filter{votes[it.id]=="like"&&DiscoveryPolicy.visible(it.adult,display)});Text("Suas escolhas ficam salvas neste aparelho. Você pode refazer nos Ajustes.",color=MpMuted,style=MaterialTheme.typography.bodySmall);OutlinedButton({round++;index=0;step=2},Modifier.fillMaxWidth()){Text("Quero conhecer mais obras")};Button({store.discoveryDone=true;account()},Modifier.fillMaxWidth()){Text("Entrar ou criar minha conta")};TextButton({close()},Modifier.fillMaxWidth()){Text("Continuar para a MP SCAN")}}
 }
 }}
 }
 }
}
@Composable private fun Kicker(text:String){Text(text,color=MpAccent2,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)}
@Composable private fun Fan(works:List<Work>){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){works.take(3).forEach{w->MpImage(w.cover,w.title,Modifier.weight(1f).aspectRatio(.72f).clip(RoundedCornerShape(16.dp)),contentScale=ContentScale.Crop)}}}
@Composable fun AdultDisplayOptions(value:String,change:(String)->Unit){
 listOf(Triple("show","Mostrar as capas","Exibir as capas das obras +18 no catálogo."),Triple("blur","Desfocar as capas +18","Manter as capas desfocadas no aplicativo."),Triple("hide","Ocultar obras +18","Deixar essas obras fora do seu catálogo.")).forEach{(key,title,description)->Surface(onClick={change(key)},Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),color=if(value==key)MpAccent.copy(alpha=.15f)else MpSurface,border=androidx.compose.foundation.BorderStroke(1.dp,if(value==key)MpAccent else MpLine)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(value==key,{change(key)});Column(Modifier.padding(start=8.dp)){Text(title,fontWeight=FontWeight.Bold);Text(description,color=MpMuted,style=MaterialTheme.typography.bodySmall)}}}}
}
