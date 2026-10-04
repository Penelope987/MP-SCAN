package online.mpscan.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import online.mpscan.app.data.*
import online.mpscan.app.ui.theme.*

@Composable fun CollectionTile(collection:UserCollection,works:List<Work>,modifier:Modifier=Modifier,open:()->Unit){
 val members=works.filter{it.id in collection.workIds}
 Surface(modifier.clickable(onClick=open),color=MpSurface2,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MpLine)){
  Column{
   Box(Modifier.fillMaxWidth().height(135.dp).background(Brush.linearGradient(listOf(MpAccent.copy(.18f),MpSurface2))),contentAlignment=Alignment.Center){
    if(collection.cover.isNotBlank())MpImage(collection.cover,null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
    else if(members.isNotEmpty())Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(2.dp)){members.take(3).forEach{work->MpImage(work.cover,null,Modifier.weight(1f).fillMaxHeight(),contentScale=ContentScale.Crop)}}
    else Text("▦",color=MpMuted,style=MaterialTheme.typography.displaySmall)
   }
   Column(Modifier.padding(16.dp)){
    Text(collection.name,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
    Text(collection.description.ifBlank{"Sem descrição."},color=MpMuted,maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=6.dp,bottom=16.dp))
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){PrivacyBadge(collection.isPublic);Spacer(Modifier.weight(1f));Text("${collection.workIds.size} obras",color=MpMuted,style=MaterialTheme.typography.labelSmall)}
   }
  }
 }
}
@Composable fun PrivacyBadge(public:Boolean){Surface(color=if(public)Color(0xff42bb9e).copy(.1f)else MpAccent.copy(.1f),shape=RoundedCornerShape(50),border=BorderStroke(1.dp,if(public)Color(0xff42bb9e).copy(.25f)else MpAccent.copy(.25f))){Text(if(public)"🌐 Pública"else"🔒 Privada",Modifier.padding(horizontal=10.dp,vertical=5.dp),color=if(public)Color(0xff38a48b)else MpAccent,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)}}
@Composable fun CollectionHero(collection:UserCollection,owner:ProfilePerson?,works:List<Work>){
 val cover=collection.cover.ifBlank{works.firstOrNull{it.id in collection.workIds}?.cover.orEmpty()}
 Box(Modifier.fillMaxWidth().heightIn(min=200.dp).clip(RoundedCornerShape(28.dp)).background(MpAccent.copy(.12f))){
  if(cover.isNotBlank()){MpImage(cover,null,Modifier.matchParentSize(),contentScale=ContentScale.Crop);Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.12f),Color.Black.copy(.85f)))))}
  Column(Modifier.padding(22.dp)){PrivacyBadge(collection.isPublic);Spacer(Modifier.height(40.dp));Text(collection.name,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.headlineMedium,color=if(cover.isNotBlank())Color.White else MpText);if(collection.description.isNotBlank())Text(collection.description,color=if(cover.isNotBlank())Color.White.copy(.8f)else MpMuted,modifier=Modifier.padding(top=8.dp));Row(Modifier.padding(top=14.dp),verticalAlignment=Alignment.CenterVertically){if(owner!=null){FramedAvatar(owner.photo,owner.name,size=32.dp);Text(owner.name,color=if(cover.isNotBlank())Color.White else MpText,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=8.dp))};Text("${collection.workIds.size} obras",color=if(cover.isNotBlank())Color.White.copy(.8f)else MpMuted)}}
 }
}
@Composable fun WorkCoverTile(work:Work,modifier:Modifier=Modifier,open:()->Unit){
 Surface(modifier.clickable(onClick=open),color=MpSurface,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,MpLine)){
  Column{Box(Modifier.fillMaxWidth().aspectRatio(.68f)){
   MpImage(work.cover,work.title,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
   Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Transparent,Color.Black.copy(.85f)))))
   Column(Modifier.align(Alignment.BottomStart).padding(14.dp)){Text(work.title,color=Color.White,fontWeight=FontWeight.Bold,maxLines=3,overflow=TextOverflow.Ellipsis);Text("Ver obra →",color=Color.White.copy(.75f),style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=5.dp))}
  }}
 }
}
