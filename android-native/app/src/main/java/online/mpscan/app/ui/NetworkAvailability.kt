package online.mpscan.app.ui

import android.content.Context
import android.net.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable fun networkAvailable():Boolean{
 val manager=LocalContext.current.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
 fun available():Boolean{val c=manager.getNetworkCapabilities(manager.activeNetwork)?:return false;return c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}
 var connected by remember{mutableStateOf(available())};val scope=rememberCoroutineScope()
 DisposableEffect(manager){val callback=object:ConnectivityManager.NetworkCallback(){
  private var current:Network?=null
  override fun onAvailable(network:Network){current=network}
  override fun onCapabilitiesChanged(network:Network,c:NetworkCapabilities){if(current==network){val valid=c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);scope.launch{connected=valid}}}
  override fun onLost(network:Network){if(current==network){current=null;scope.launch{connected=false}}}
 };manager.registerDefaultNetworkCallback(callback);onDispose{manager.unregisterNetworkCallback(callback)}}
 return connected
}
