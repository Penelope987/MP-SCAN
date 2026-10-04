package online.mpscan.app.ui

import android.graphics.Color as AndroidColor
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme

/** Software shadow blurs the original glyphs on every supported Android version. */
@Composable fun SpoilerText(text:String,hidden:Boolean,color:Color,reveal:()->Unit){
 if(!hidden){Text(text,color=color,style=MaterialTheme.typography.bodyLarge);return}
 AndroidView(factory={context->TextView(context).apply{
  setLayerType(View.LAYER_TYPE_SOFTWARE,null)
  importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
  setTextIsSelectable(false);setPadding(8,12,8,12);textSize=16f
 }},update={view->view.text=text;view.setTextColor(AndroidColor.TRANSPARENT);view.setShadowLayer(9f*view.resources.displayMetrics.density,0f,0f,color.toArgb())},modifier=Modifier.fillMaxWidth().clickable(onClickLabel="Revelar spoiler",onClick=reveal))
}

@Composable fun SpoilerImage(source:String,hidden:Boolean,reveal:()->Unit){
 if(!hidden){MpImage(source,null,Modifier.fillMaxWidth().heightIn(max=280.dp),contentScale=androidx.compose.ui.layout.ContentScale.Fit);return}
 var bitmap by remember(source){mutableStateOf<android.graphics.Bitmap?>(null)}
 LaunchedEffect(source){bitmap=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){runCatching{
  val connection=java.net.URL(source).openConnection().apply{connectTimeout=15000;readTimeout=20000}
  try{connection.getInputStream().use{input->
   val original=android.graphics.BitmapFactory.decodeStream(input)?:return@use null
   val small=android.graphics.Bitmap.createScaledBitmap(original,12,18,true)
   val blurred=android.graphics.Bitmap.createScaledBitmap(small,360,540,true)
   if(original!==small)original.recycle();if(small!==blurred)small.recycle();blurred
  }}finally{(connection as? java.net.HttpURLConnection)?.disconnect()}
 }.getOrNull()}}
 val image=bitmap
 androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().heightIn(min=120.dp,max=280.dp).clickable(onClickLabel="Revelar spoiler",onClick=reveal)){
  if(image!=null)androidx.compose.foundation.Image(image.asImageBitmap(),null,Modifier.fillMaxWidth(),contentScale=androidx.compose.ui.layout.ContentScale.Fit)
  else androidx.compose.material3.Surface(Modifier.fillMaxWidth().height(120.dp),color=MaterialTheme.colorScheme.surfaceVariant){}
 }
}
