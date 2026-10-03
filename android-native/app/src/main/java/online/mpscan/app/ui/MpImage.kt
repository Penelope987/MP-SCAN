package online.mpscan.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.request.allowHardware
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Firebase chapters and profile pictures may be inline base64 rather than URLs. */
@Composable fun MpImage(model:Any?,contentDescription:String?,modifier:Modifier=Modifier,contentScale:ContentScale=ContentScale.Fit,onSuccess:((AsyncImagePainter.State.Success)->Unit)?=null,onError:((AsyncImagePainter.State.Error)->Unit)?=null){
 val original=if(model is ImageRequest)model.data else model
 val inline=(original as? String)?.takeIf{it.startsWith("data:image/")}
 val bytes by produceState<ByteArray?>(null,inline){if(inline!=null)value=withContext(Dispatchers.IO){runCatching{android.util.Base64.decode(inline.substringAfter(','),android.util.Base64.DEFAULT)}.getOrNull()}}
 val resolved=remember(model,bytes){if(inline==null)model else if(bytes==null)null else if(model is ImageRequest)model.newBuilder().data(bytes).build()else bytes}
 val context=LocalContext.current;val blurred=LocalAdultDisplay.current=="blur"&&original is String&&original in LocalAdultCovers.current
 val displayModel=remember(resolved,blurred){if(!blurred||resolved==null)resolved else (if(resolved is ImageRequest)resolved.newBuilder()else ImageRequest.Builder(context).data(resolved)).allowHardware(false).transformations(CoverBlur()).build()}
 key(blurred){AsyncImage(displayModel,contentDescription,modifier,contentScale=contentScale,onSuccess=onSuccess,onError=onError)}
}
