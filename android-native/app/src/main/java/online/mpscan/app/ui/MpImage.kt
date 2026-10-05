package online.mpscan.app.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import online.mpscan.app.data.PageManifest
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Firebase chapters and profile pictures may be inline base64 rather than URLs. */
@Composable fun MpImage(model:Any?,contentDescription:String?,modifier:Modifier=Modifier,contentScale:ContentScale=ContentScale.Fit,onSuccess:((AsyncImagePainter.State.Success)->Unit)?=null,onError:((AsyncImagePainter.State.Error)->Unit)?=null){
 val original=if(model is ImageRequest)model.data else model
 val inline=(original as? String)?.takeIf{it.startsWith("data:image/",true)}
 var bytes by remember(inline){mutableStateOf<ByteArray?>(null)};var decoded by remember(inline){mutableStateOf(false)}
 LaunchedEffect(inline){if(inline!=null){bytes=withContext(Dispatchers.IO){runCatching{android.util.Base64.decode(inline.substringAfter(','),android.util.Base64.DEFAULT)}.getOrNull()};decoded=true}}
 if(inline!=null&&!decoded){Box(modifier);return}
 val resolved=remember(model,bytes){if(inline==null){val normalized=if(original is String)PageManifest.normalize(original)?:original else original;if(model is ImageRequest)model.newBuilder().data(normalized).build()else normalized}else if(model is ImageRequest)model.newBuilder().data(bytes).build()else bytes}
 AsyncImage(resolved,contentDescription,modifier,contentScale=contentScale,onSuccess=onSuccess,onError=onError)
}
