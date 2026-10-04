package online.mpscan.app.ui

import android.graphics.Color as AndroidColor
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
 }},update={view->view.text=text;view.setTextColor(AndroidColor.TRANSPARENT);view.setShadowLayer(9f,0f,0f,color.toArgb())},modifier=Modifier.fillMaxWidth().clickable(onClickLabel="Revelar spoiler",onClick=reveal))
}
