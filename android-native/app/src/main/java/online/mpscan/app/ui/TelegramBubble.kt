package online.mpscan.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import online.mpscan.app.ui.theme.*
import kotlin.math.roundToInt

@Composable fun TelegramBubble(){
 val context=LocalContext.current;val prefs=remember{context.getSharedPreferences("mp_telegram",0)};val uri=LocalUriHandler.current;val haptic=LocalHapticFeedback.current
 var x by remember{mutableFloatStateOf(prefs.getFloat("x",1f).coerceIn(0f,1f))};var y by remember{mutableFloatStateOf(prefs.getFloat("y",.78f).coerceIn(0f,1f))}
 var dragging by remember{mutableStateOf(false)};var showLabel by remember{mutableStateOf(false)};var gesture by remember{mutableIntStateOf(0)}
 LaunchedEffect(dragging,gesture){if(!dragging&&showLabel){delay(3000);showLabel=false}}
 BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(start=16.dp,end=16.dp,top=48.dp,bottom=88.dp)){
  val density=LocalDensity.current;val rangeX=with(density){(maxWidth-60.dp).coerceAtLeast(0.dp).toPx()};val rangeY=with(density){(maxHeight-60.dp).coerceAtLeast(0.dp).toPx()}
  Box(Modifier.offset{IntOffset((x*rangeX).roundToInt(),(y*rangeY).roundToInt())}.size(60.dp)){
   AnimatedVisibility(showLabel,Modifier.align(Alignment.TopCenter).offset(y=(-38).dp),enter=fadeIn(),exit=fadeOut()){
    Surface(color=MpSurface,shape=RoundedCornerShape(50),border=BorderStroke(1.dp,MpLine),shadowElevation=3.dp){Text("Canal",Modifier.padding(horizontal=14.dp,vertical=7.dp),color=MpText,style=MaterialTheme.typography.labelMedium)}
   }
   Surface(Modifier.fillMaxSize().semantics{contentDescription="Canal MP SCAN no Telegram. Arraste para mudar de lugar."}.pointerInput(rangeX,rangeY){
    fun finish(){dragging=false;gesture++;prefs.edit().putFloat("x",x).putFloat("y",y).apply()}
    detectDragGestures(onDragStart={dragging=true;showLabel=true;haptic.performHapticFeedback(HapticFeedbackType.LongPress)},onDragEnd={finish()},onDragCancel={finish()}){change,amount->change.consume();if(rangeX>0)x=(x+amount.x/rangeX).coerceIn(0f,1f);if(rangeY>0)y=(y+amount.y/rangeY).coerceIn(0f,1f)}
   }.clickable{uri.openUri("https://t.me/MPSCAN0")},shape=CircleShape,color=Color(0xff229ED9),shadowElevation=if(dragging)12.dp else 6.dp){
    Canvas(Modifier.padding(16.dp)){
     val scale=size.width/24f
     val plane=Path().apply{moveTo(2f*scale,10f*scale);lineTo(22f*scale,2f*scale);lineTo(18f*scale,22f*scale);lineTo(11f*scale,16f*scale);lineTo(8f*scale,20f*scale);lineTo(8f*scale,13f*scale);close()}
     drawPath(plane,Color.White)
     val fold=Path().apply{moveTo(8f*scale,13f*scale);lineTo(18f*scale,6f*scale);lineTo(11f*scale,16f*scale);lineTo(8f*scale,20f*scale);close()};drawPath(fold,Color(0xffB6E3F4))
    }
   }
  }
 }
}
