package online.mpscan.app.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.mpscan.app.ui.theme.*
@Composable fun SupportCard(modifier:Modifier=Modifier){
 val uri=LocalUriHandler.current
 Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=MpSurface){
 Column(Modifier.background(Brush.linearGradient(listOf(MpAccent.copy(alpha=.18f),MpSurface,MpAccent2.copy(alpha=.12f)))).padding(22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 Text("♡  FEITO COM APOIO DA COMUNIDADE",color=MpAccent2,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
 Text("Ajude a história a continuar",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
 Text("Seu apoio ajuda a MP SCAN a trazer os próximos capítulos.",color=MpMuted,style=MaterialTheme.typography.bodyMedium)
 Button({uri.openUri("https://livepix.gg/mpscan")},shape=RoundedCornerShape(12.dp)){Text("Apoiar a scan  ↗")}
 }}}
