package online.mpscan.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import online.mpscan.app.ui.theme.*

object AppPolicies {
 val privacy="""
Política de privacidade do aplicativo MP SCAN
Atualizada em 4 de outubro de 2026.

1. Sua conta e seus dados
O aplicativo usa a conta da MP SCAN para autenticação, verificação de acesso e sincronização. São tratados e-mail, identificador da conta e os dados de perfil que você fornece, como nome, arroba, foto e biografia. Ao entrar com Google, o serviço fornece as informações necessárias para identificar sua conta; o aplicativo não recebe sua senha do Google.

2. Leituras e comunidade
Favoritos, coleções, preferências de avisos, avaliações, comentários e suas interações são usados para disponibilizar esses recursos. Nome, arroba, foto, molduras e comentários podem aparecer para outras pessoas. Coleções marcadas como públicas ficam disponíveis à comunidade. Mantenha dados pessoais sensíveis fora de comentários e biografias. Um perfil privado não torna invisíveis os comentários que você publicou.

3. Dados no aparelho
Sessão, preferências, progresso de leitura e capítulos baixados são guardados no aparelho. Downloads ocupam armazenamento e continuam disponíveis sem internet enquanto os arquivos estiverem íntegros e o acesso local estiver autorizado. A remoção do aplicativo pode apagar esses dados. Sair da conta não apaga automaticamente os downloads; use os controles de downloads antes de compartilhar o aparelho.

4. Serviços utilizados
A autenticação e a sincronização usam os serviços Google/Firebase. A edição regular pode usar Firebase Analytics para estatísticas de uso. Imagens, páginas, anúncios e links de parceiros podem ser fornecidos por serviços externos, sujeitos às respectivas políticas. Conteúdo de anúncios configurados pela administração pode aparecer no aplicativo. Não publique credenciais ou documentos nos espaços públicos.

5. Permissões e escolhas
Notificações são opcionais e podem ser recusadas ou desligadas nos ajustes do celular e do aplicativo. Recusar não impede a leitura. Preferências de downloads por Wi-Fi e privacidade do perfil podem ser ajustadas. O aplicativo não solicita acesso à agenda ou aos contatos para leitura.

6. Seus pedidos
Você pode solicitar informações sobre o tratamento dos seus dados, acesso, correção e exclusão, observadas as obrigações e hipóteses legais aplicáveis. Procure a administração pelo canal de suporte divulgado oficialmente pela MP SCAN e informe apenas os dados necessários para identificar sua conta. A equipe poderá confirmar sua identidade para evitar pedidos feitos por terceiros. A retenção necessária à segurança, resolução de conflitos e cumprimento de obrigações será avaliada conforme o caso; esta política não promete exclusão imediata de todos os registros.

7. Atualizações
Mudanças relevantes nesta política serão comunicadas nos canais oficiais. Esta política descreve o aplicativo Android; serviços externos e o site podem ter documentos próprios.
""".trimIndent()
 val security="""
Política de segurança do aplicativo MP SCAN
Atualizada em 4 de outubro de 2026.

1. Proteja sua conta
Use uma senha exclusiva, proteja seu e-mail e mantenha o celular atualizado. A MP SCAN não precisa da sua senha por comentário ou mensagem para prestar suporte. Confira se o aplicativo e os canais de atendimento são oficiais. Em aparelho compartilhado, saia da conta e remova downloads e informações que não deseja deixar acessíveis.

2. Controle de acesso
O aplicativo verifica a sessão e as restrições de acesso da conta. Banimentos da MP SCAN também se aplicam ao aplicativo. Offline, uma restrição nova pode não ser conhecida até a próxima conexão; restrições já conhecidas não são liberadas por um botão local. Não tente contornar banimentos, verificações de idade ou permissões de conteúdo.

3. Administração e moderação
Funções administrativas dependem das autorizações da plataforma. Administradores devem proteger suas contas, verificar pedidos sensíveis e conceder apenas o acesso necessário. Remoção, edição e moderação de conteúdo devem respeitar as regras da comunidade e a privacidade. Ser administrador não autoriza solicitar senhas dos usuários ou divulgar dados privados.

4. Comunidade segura
Não compartilhe ameaças, assédio, dados pessoais de terceiros, links enganosos ou arquivos maliciosos. Marque spoilers e respeite as orientações de moderação. Avise a administração pelo suporte oficial ao identificar abuso, acesso indevido ou conteúdo suspeito; evite divulgar publicamente informações que permitam explorar uma falha.

5. Limites e incidentes
Nenhum sistema oferece garantia absoluta contra falhas ou acesso indevido. Problemas de conexão e serviços externos podem afetar sincronização e notificações. Se houver suspeita de comprometimento, troque a senha pelo fluxo de recuperação e procure a administração. A equipe avaliará medidas de contenção e as comunicações exigidas pela legislação aplicável.
""".trimIndent()
 val usage="""
Política de uso do aplicativo MP SCAN
Atualizada em 4 de outubro de 2026.

1. Acesso
Uma conta é necessária para usar os recursos da comunidade e manter suas preferências. Forneça informações verdadeiras quando exigidas para acesso e verificação. Respeite classificações indicativas e restrições da plataforma; baixar um capítulo não concede autorização para conteúdo restrito.

2. Respeito à comunidade
Não pratique assédio, discriminação, ameaças, fraude, spam ou exposição de informações pessoais. Não se passe por outra pessoa ou pela administração. Publique comentários relacionados à leitura e marque spoilers. Você é responsável pelo conteúdo que publica.

3. Obras e downloads
Obras, imagens e marcas pertencem aos respectivos titulares. A leitura offline é destinada ao uso pessoal no aplicativo. Não redistribua arquivos, remova créditos ou use o conteúdo de forma que viole direitos. A disponibilidade pode mudar por decisão da equipe, dos parceiros ou dos titulares.

4. Coleções, molduras e parceiros
Coleções públicas podem ser vistas por outros usuários; coleções privadas destinam-se à sua conta. Molduras disponíveis seguem as liberações da administração. Informações e links de parceiros não representam garantia de serviços externos. Apoio financeiro é voluntário e não elimina as regras de acesso ou moderação.

5. Moderação e atendimento
A equipe pode moderar conteúdo e restringir contas conforme as regras e a gravidade da situação. Para contestar uma medida ou pedir ajuda, use o suporte oficial da MP SCAN. Não contorne uma restrição criando ou usando outra conta.

6. Disponibilidade
A leitura online, a sincronização e os avisos dependem da conexão, das permissões do aparelho e dos serviços utilizados. Downloads precisam terminar antes da leitura offline. A administração poderá atualizar funcionalidades e estas condições, comunicando alterações relevantes pelos canais oficiais.
""".trimIndent()
 fun text(title:String)=when(title){"Política de privacidade"->privacy;"Política de segurança"->security;else->usage}
}
@Composable fun AppPoliciesDialog(close:()->Unit){
 var selected by remember{mutableStateOf("Política de privacidade")}
 Dialog(close){Surface(shape=RoundedCornerShape(24.dp),color=MpSurface){Column(Modifier.fillMaxHeight(.85f).padding(20.dp)){
 Text("Políticas do aplicativo",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
 Row(Modifier.horizontalScroll(rememberScrollState())){listOf("Política de privacidade","Política de segurança","Política de uso").forEach{title->TextButton({selected=title}){Text(title.removePrefix("Política de "))}}}
 Text(AppPolicies.text(selected),Modifier.weight(1f).verticalScroll(rememberScrollState()),style=MaterialTheme.typography.bodyMedium)
 TextButton(close,Modifier.fillMaxWidth()){Text("Fechar")}
 }}}
}
