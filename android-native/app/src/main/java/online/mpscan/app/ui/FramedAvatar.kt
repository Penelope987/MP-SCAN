package online.mpscan.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AvatarArtCache { val definitions=java.util.concurrent.ConcurrentHashMap<String,JSONObject>() }
@Composable fun FramedAvatar(photo:String,name:String,frameId:String="",size:Dp=60.dp,definition:JSONObject?=null){
 var frame by remember(frameId,definition){mutableStateOf(definition?:AvatarArtCache.definitions[frameId]?:BuiltinAvatarFrames.definitions().optJSONObject(frameId)?:BuiltinAvatarFrames.rankingArt(frameId))}
 LaunchedEffect(frameId){if(frameId.isNotBlank()&&frame==null)runCatching{withContext(Dispatchers.IO){SiteAccess.json("config/avatarFrames/$frameId")}}.onSuccess{frame=it;AvatarArtCache.definitions[frameId]=it}}
 val art=frame?.optString("imagemUrl",frame?.optString("imageUrl").orEmpty()).orEmpty()
 val effect=frame?.optString("effect","none").orEmpty()
 val enabled=SettingsStore(LocalContext.current).animations&&effect.isNotBlank()&&effect!="none"
 val transition=rememberInfiniteTransition(label="Moldura da foto")
 val phase by transition.animateFloat(0f,360f,infiniteRepeatable(tween(6500,easing=LinearEasing)),label="Órbita")
 val glow=runCatching{Color(android.graphics.Color.parseColor(frame?.optString("cor","#b99cff")))}.getOrDefault(MpAccent)
 Box(Modifier.size(size),contentAlignment=Alignment.Center){
  Surface(Modifier.fillMaxSize(.79f),shape=CircleShape,color=MpSurface2,border=BorderStroke(1.dp,MpLine)){
   if(photo.isNotBlank())MpImage(photo,name,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
   else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(name.take(1).uppercase(),fontWeight=FontWeight.Black)}
  }
  if(art.isNotBlank())MpImage(art,null,Modifier.matchParentSize(),contentScale=ContentScale.Fit)
  else if(frameId.isNotBlank()&&frame!=null){
   Canvas(Modifier.matchParentSize()){
    val radius=this.size.minDimension*.43f
    drawCircle(glow.copy(.1f),radius=radius+5.dp.toPx(),style=Stroke(9.dp.toPx()))
    drawCircle(Brush.sweepGradient(listOf(glow,Color.White,glow.copy(.25f),glow)),radius=radius,style=Stroke(2.2.dp.toPx()))
    val rotation=if(enabled)phase else 0f
    for(i in 0..3){val angle=Math.toRadians((rotation+i*90).toDouble());val point=Offset(center.x+kotlin.math.cos(angle).toFloat()*radius,center.y+kotlin.math.sin(angle).toFloat()*radius);drawCircle(glow,radius=2.dp.toPx(),center=point)}
   }
   val symbol=when(effect){"dog"->"🐕";"paws"->"🐾";"sakura","garden"->"❀";"moon"->"☾";"butterfly"->"🦋";"hearts"->"♥";"crown"->"♛";"snow"->"❄";"pearls"->"●";"comet"->"☄";else->"✧"}
   if((frame?.optInt("rank")?:0)>0)Text("${frame?.optInt("rank")}º",color=glow,fontSize=(size.value*.15f).sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.BottomCenter))
   Text(symbol,color=glow,fontSize=(size.value*.22f).sp,modifier=Modifier.align(Alignment.TopCenter))
  }
 }
}
@Composable fun Modifier.commentMotion(effect:String,speed:Int):Modifier{
 val enabled=SettingsStore(LocalContext.current).animations&&effect!="none"
 if(!enabled)return this
 val transition=rememberInfiniteTransition(label="Arte do comentário")
 val phase by transition.animateFloat(0f,1f,infiniteRepeatable(tween(speed.coerceIn(1,12)*1000),RepeatMode.Reverse),label="Movimento")
 return graphicsLayer {
  when(effect){
   "float"->translationY=-5f*phase
   "wave"->translationX=5f*(phase-.5f)
   "swing"->rotationZ=(phase-.5f)*2f
   "pulse","breathe","zoom","heartbeat"->{scaleX=1f+.012f*phase;scaleY=scaleX}
   else->alpha=.78f+.22f*phase
  }
 }
}
