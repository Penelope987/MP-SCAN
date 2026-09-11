package online.mpscan.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import online.mpscan.nativeapp.data.FirebaseCatalogRepository
import online.mpscan.nativeapp.data.FirebaseAuthRepository
import online.mpscan.nativeapp.data.AccountProfile
import online.mpscan.nativeapp.data.OfflineLibrary
import online.mpscan.nativeapp.data.OfflineChapter
import online.mpscan.nativeapp.data.CommunityRepository
import online.mpscan.nativeapp.data.WorkComment
import online.mpscan.nativeapp.model.Chapter
import online.mpscan.nativeapp.model.Work

// Tokens copiados do :root do tema MP SCAN. Toda tela nativa usa esta mesma base.
private val Bg = Color(0xFF0B0B0D)
private val Card = Color(0xFF141419)
private val Card2 = Color(0xFF1B1B22)
private val Line = Color(0xFF2A2A33)
private val Purple = Color(0xFF7B4DFF)
private val Pink = Color(0xFFFF5AA5)
private val Muted = Color(0xFFA8A8B3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MpScanTheme { MpScanApp() } }
    }
}

data class CatalogState(
    val loading: Boolean = true,
    val works: List<Work> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val selected: Work? = null,
    val error: String? = null,
    val reader: ReaderState? = null,
    val downloads: List<OfflineChapter> = emptyList(),
    val downloadProgress: Map<String, Int> = emptyMap(),
    val offline: Boolean = false,
    val libraryIds: Set<String> = emptySet()
    ,val comments: List<WorkComment> = emptyList()
    ,val commentsLoading: Boolean = false
)

data class ReaderState(val work: Work, val chapter: Chapter, val pages: List<String>, val downloaded: Boolean)

class CatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirebaseCatalogRepository()
    private val offline = OfflineLibrary(application)
    private val auth = FirebaseAuthRepository()
    private val community = CommunityRepository()
    private val preferences = application.getSharedPreferences("mp_scan_library", android.content.Context.MODE_PRIVATE)
    var state by mutableStateOf(CatalogState(libraryIds = preferences.getStringSet("works", emptySet())?.toSet() ?: emptySet()))
        private set

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        state = state.copy(loading = true, error = null)
        val sensitivePermission = runCatching {
            auth.current(getApplication())?.let { auth.loadProfile(it) }?.let { it.minorSensitiveApproved || it.sensitiveAllowed } == true
        }.getOrDefault(false)
        runCatching { repository.loadWorks(sensitivePermission) }
            .onSuccess { offline.saveCatalog(it); state = state.copy(loading = false, works = it, offline = false) }
            .onFailure { val cached = offline.loadCatalog(); state = state.copy(loading = false, works = cached, offline = true, error = if (cached.isEmpty()) "Conecte-se uma vez para sincronizar o catálogo." else null) }
    }

    fun open(work: Work) = viewModelScope.launch {
        state = state.copy(selected = work, chapters = emptyList(), comments = emptyList(), loading = true, error = null)
        runCatching { repository.loadChapters(work.id) }
            .onSuccess { offline.saveChapters(work.id, it); state = state.copy(loading = false, chapters = it, offline = false) }
            .onFailure { val cached = offline.loadChapters(work.id); state = state.copy(loading = false, chapters = cached, offline = true, error = if (cached.isEmpty()) "Estes capítulos ainda não foram sincronizados." else null) }
        loadComments(work.id)
    }

    fun loadComments(workId: String) = viewModelScope.launch {
        state = state.copy(commentsLoading = true)
        runCatching { community.comments(workId, auth.current(getApplication())?.uid) }
            .onSuccess { state = state.copy(comments = it, commentsLoading = false) }
            .onFailure { state = state.copy(commentsLoading = false) }
    }

    fun reactToComment(workId: String, comment: WorkComment, emoji: String) = viewModelScope.launch {
        val session = auth.current(getApplication())
        if (session == null) { state = state.copy(error = "Entre na sua conta para reagir."); return@launch }
        runCatching { community.react(workId, comment.id, session, emoji, comment.myReaction == emoji) }
            .onSuccess { loadComments(workId) }
            .onFailure { state = state.copy(error = it.message ?: "Não foi possível registrar a reação.") }
    }

    fun postComment(workId: String, text: String, spoiler: Boolean) = viewModelScope.launch {
        val session = auth.current(getApplication())
        if (session == null) { state = state.copy(error = "Entre na sua conta para comentar."); return@launch }
        if (text.isBlank()) { state = state.copy(error = "Escreva um comentário antes de enviar."); return@launch }
        state = state.copy(commentsLoading = true, error = null)
        runCatching { community.post(workId, session, text, spoiler) }
            .onSuccess { loadComments(workId) }
            .onFailure { state = state.copy(commentsLoading = false, error = it.message ?: "Não foi possível publicar o comentário.") }
    }

    fun closeWork() { state = state.copy(selected = null, chapters = emptyList(), error = null) }

    fun openChapter(work: Work, chapter: Chapter) = viewModelScope.launch {
        state = state.copy(loading = true, error = null)
        val local = offline.localPages(work.id, chapter.id)
        if (local.isNotEmpty()) { state = state.copy(loading = false, reader = ReaderState(work, chapter, local, true)); return@launch }
        runCatching { repository.loadPages(work.id, chapter.id) }
            .onSuccess { state = state.copy(loading = false, reader = ReaderState(work, chapter, it, false)) }
            .onFailure { state = state.copy(loading = false, error = "Este capítulo não foi baixado e a internet está indisponível.") }
    }

    fun closeReader() { state = state.copy(reader = null, error = null) }
    fun loadDownloads() = viewModelScope.launch { state = state.copy(downloads = offline.downloads()) }
    fun download(work: Work, chapter: Chapter) = viewModelScope.launch {
        val key = "${work.id}__${chapter.id}"
        state = state.copy(downloadProgress = state.downloadProgress + (key to 0), error = null)
        runCatching {
            val pages = repository.loadPages(work.id, chapter.id)
            offline.downloadChapter(work, chapter, pages) { value -> state = state.copy(downloadProgress = state.downloadProgress + (key to value)) }
        }.onSuccess { state = state.copy(downloadProgress = state.downloadProgress - key); loadDownloads() }
         .onFailure { state = state.copy(downloadProgress = state.downloadProgress - key, error = "Não foi possível baixar o capítulo.") }
    }
    fun downloadAll(work: Work, chapters: List<Chapter>) = viewModelScope.launch {
        if (chapters.isEmpty()) { state = state.copy(error = "Esta obra ainda não possui capítulos para baixar."); return@launch }
        if (work.id !in state.libraryIds) toggleLibrary(work)
        val allKey = "${work.id}__all"
        state = state.copy(downloadProgress = state.downloadProgress + (allKey to 0), error = null)
        var completed = 0
        var failed = 0
        for (chapter in chapters) {
            runCatching {
                val pages = repository.loadPages(work.id, chapter.id)
                offline.downloadChapter(work, chapter, pages) { pageProgress ->
                    val total = ((completed * 100) + pageProgress) / chapters.size
                    state = state.copy(downloadProgress = state.downloadProgress + (allKey to total))
                }
            }.onFailure { failed++ }
            completed++
            state = state.copy(downloadProgress = state.downloadProgress + (allKey to ((completed * 100) / chapters.size)))
        }
        state = state.copy(downloadProgress = state.downloadProgress - allKey, error = if (failed > 0) "$failed capítulo(s) não puderam ser baixados." else null)
        loadDownloads()
    }
    fun removeDownload(item: OfflineChapter) = viewModelScope.launch { offline.remove(item.work.id, item.chapter.id); loadDownloads() }
    fun toggleLibrary(work: Work) {
        val next = state.libraryIds.toMutableSet().apply { if (!add(work.id)) remove(work.id) }.toSet()
        preferences.edit().putStringSet("works", next).apply()
        state = state.copy(libraryIds = next)
    }
}

@Composable
private fun MpScanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple,
            secondary = Pink,
            background = Bg,
            surface = Card,
            outline = Line,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

private enum class Tab(val label: String, val icon: String) {
    Home("Início", "⌂"), Search("Busca", "⌕"), Library("Biblioteca", "▣"), More("Mais", "•••")
}

private enum class MorePage { Menu, Settings, Profile }

@Composable
private fun MpScanApp(vm: CatalogViewModel = viewModel()) {
    var tab by remember { mutableStateOf(Tab.Home) }
    var query by remember { mutableStateOf("") }
    var morePage by rememberSaveable { mutableStateOf(MorePage.Menu) }
    val state = vm.state
    BackHandler(state.reader != null || state.selected != null || (tab == Tab.More && morePage != MorePage.Menu)) {
        if (state.reader != null) vm.closeReader() else if (state.selected != null) vm.closeWork() else morePage = MorePage.Menu
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            if (state.selected == null && state.reader == null) NavigationBar(containerColor = Card) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Box(Modifier.size(38.dp).clip(RoundedCornerShape(14.dp)).background(if (tab == item) Purple else Color.Transparent), contentAlignment = Alignment.Center) { Text(item.icon, color = if (tab == item) Color.White else Muted) } },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.reader != null -> ReaderScreen(state.reader, vm::closeReader, { vm.download(state.reader.work, state.reader.chapter) }, state.downloadProgress["${state.reader.work.id}__${state.reader.chapter.id}"])
                state.selected != null -> WorkScreen(state.selected, state.chapters, state.loading, state.downloadProgress, state.selected.id in state.libraryIds, state.comments, state.commentsLoading, vm::closeWork, { vm.openChapter(state.selected, it) }, { vm.download(state.selected, it) }, { vm.downloadAll(state.selected, state.chapters) }, { vm.toggleLibrary(state.selected) }, { text, spoiler -> vm.postComment(state.selected.id, text, spoiler) }, { comment, emoji -> vm.reactToComment(state.selected.id, comment, emoji) })
                tab == Tab.Home -> HomeScreen(state, vm::open, vm::refresh)
                tab == Tab.Search -> SearchScreen(state.works, query, { query = it }, vm::open)
                tab == Tab.Library -> LibraryScreen(state.works.filter { it.id in state.libraryIds }, state.downloads, vm::loadDownloads, vm::open, { vm.openChapter(it.work, it.chapter) }, vm::removeDownload)
                else -> when (morePage) {
                    MorePage.Menu -> MoreScreen(
                        openSettings = { morePage = MorePage.Settings },
                        openProfile = { morePage = MorePage.Profile }
                    )
                    MorePage.Settings -> SettingsScreen { morePage = MorePage.Menu }
                    MorePage.Profile -> ProfileScreen({ morePage = MorePage.Menu }, vm::refresh)
                }
            }
            state.error?.let { message ->
                Snackbar(Modifier.align(Alignment.BottomCenter).padding(12.dp)) { Text(message) }
            }
        }
    }
}

@Composable
private fun MoreScreen(openSettings: () -> Unit, openProfile: () -> Unit) {
    val context = LocalContext.current
    val session = remember { FirebaseAuthRepository().current(context) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { AppHeader("Sua conta e preferências") }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = openProfile),
                color = Card,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Purple, Pink))), contentAlignment = Alignment.Center) { Text("👤", style = MaterialTheme.typography.headlineSmall) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (session == null) "Perfil" else "Conta conectada", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                        Text(if (session == null) "Entre para comentar e sincronizar sua conta" else session.email, color = Muted)
                    }
                    Text("›", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
        item { MenuCard("⚙", "Configurações", "Conta, conteúdo, downloads e documentos", openSettings) }
        item { MenuCard("🔔", "Notificações", "Capítulos novos, respostas e avisos", {}) }
        item { MenuCard("?", "Ajuda", "Dúvidas e suporte do MP SCAN", {}) }
    }
}

@Composable
private fun MenuCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), color = Card, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2A1936)), contentAlignment = Alignment.Center) { Text(icon) }
            Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }; Text("›", color = Muted)
        }
    }
}

@Composable
private fun SettingsScreen(back: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuthRepository() }
    val session = remember { auth.current(context) }
    var profile by remember { mutableStateOf<AccountProfile?>(null) }
    var profileLoading by remember { mutableStateOf(session != null) }
    var profileError by remember { mutableStateOf("") }
    LaunchedEffect(session?.idToken) {
        if (session != null) runCatching { auth.loadProfile(session) }
            .onSuccess { profile = it }
            .onFailure { profileError = it.message.orEmpty() }
        profileLoading = false
    }
    val prefs = remember { context.getSharedPreferences("mp_scan_settings", android.content.Context.MODE_PRIVATE) }
    var sensitive by rememberSaveable { mutableStateOf(prefs.getBoolean("sensitive_non_adult", false)) }
    var wifiOnly by rememberSaveable { mutableStateOf(prefs.getBoolean("wifi_only", true)) }
    var notifications by rememberSaveable { mutableStateOf(prefs.getBoolean("notifications", true)) }
    var dialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    fun setBool(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { FilledTonalButton(onClick = back, shape = RoundedCornerShape(14.dp)) { Text("←") }; Spacer(Modifier.width(12.dp)); Column { Text("Configurações", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text("Tudo organizado em um só lugar", color = Muted) } } }
        item {
            Surface(Modifier.fillMaxWidth(), color = Card, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(19.dp)).background(Brush.linearGradient(listOf(Purple, Pink))), contentAlignment = Alignment.Center) { Text("👤") }
                    Column(Modifier.weight(1f).padding(horizontal = 13.dp)) { Text(profile?.name?.takeIf { it.isNotBlank() } ?: if (session == null) "Visitante" else "Conta MP SCAN", fontWeight = FontWeight.Black); Text(session?.email ?: "Entre para sincronizar suas preferências", color = Muted, style = MaterialTheme.typography.bodySmall) }
                    Text(if (session == null) "Entrar" else "✓", color = Purple, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { SettingsTitle("Conta e proteção") }
        item { SettingsLink("👤", "Conta e segurança", if (session == null) "Você está como visitante" else "Conectada: ${session.email}") { dialog = "Conta e segurança" to if (session == null) "Entre pelo Perfil para sincronizar suas informações." else "Sua conta está conectada. Em breve esta página também terá confirmação do e-mail e recuperação de senha." } }
        item {
            val ageLabel = when {
                session == null -> "Entre para consultar sua verificação"
                profileLoading -> "Consultando sua conta…"
                profileError.isNotBlank() -> profileError
                profile?.ageVerified == true -> "Maioridade confirmada na conta"
                profile?.ageStatus.equals("minor", true) -> "Conta protegida para menor de 18 anos"
                else -> "Data de nascimento ainda não confirmada"
            }
            SettingsLink("🎂", "Verificação etária", ageLabel) {
                dialog = "Verificação etária" to when {
                    session == null -> "Entre na conta para consultar o status salvo no Firebase."
                    profile?.ageVerified == true -> "Sua maioridade já está confirmada no Firebase. O aplicativo reconheceu essa informação corretamente."
                    profile?.ageStatus.equals("minor", true) -> "A conta está identificada como menor de 18 anos e permanece protegida contra conteúdo adulto."
                    else -> "A conta ainda não possui uma verificação etária confirmada no Firebase."
                }
            }
        }
        item {
            val allowed = profile?.sensitiveAllowed == true || profile?.minorSensitiveApproved == true
            SettingsToggle("◈", "Conteúdo sensível", when {
                session == null -> "Entre na conta para consultar a permissão"
                profileLoading -> "Consultando sua conta…"
                allowed -> "Permitido pela sua conta"
                else -> "Não permitido pela sua conta"
            }, allowed && sensitive) {
                if (allowed) { sensitive = it; setBool("sensitive_non_adult", it) }
                else dialog = "Conteúdo sensível" to "Esta opção depende da permissão salva na sua conta e não pode ser liberada somente pelo aparelho."
            }
        }
        item { SettingsTitle("Aplicativo") }
        item { SettingsToggle("⇩", "Downloads somente no Wi-Fi", "Evita gastar dados móveis sem querer", wifiOnly) { wifiOnly = it; setBool("wifi_only", it) } }
        item { SettingsLink("▣", "Downloads", "Armazenamento, capítulos e limpeza") { dialog = "Downloads" to "Esta área mostrará espaço usado, capítulos salvos e a opção de remover arquivos individualmente." } }
        item { SettingsToggle("🔔", "Notificações", "Capítulos novos, comentários e respostas", notifications) { notifications = it; setBool("notifications", it) } }
        item { SettingsTitle("Documentos e informações") }
        item { SettingsLink("≡", "Política de uso", "Regras de utilização do MP SCAN") { dialog = "Política de uso" to "O texto publicado pela administração em config/termos/termos será exibido aqui." } }
        item { SettingsLink("▤", "Política de privacidade", "Como seus dados são tratados") { dialog = "Política de privacidade" to "O texto publicado pela administração em config/termos/privacidade será exibido aqui." } }
        item { SettingsLink("🛡", "Política de segurança", "Proteção da conta e denúncias") { dialog = "Política de segurança" to "O texto publicado pela administração em config/termos/seguranca será exibido aqui." } }
        item { SettingsLink("MP", "Sobre o MP SCAN", "Versão 3.0 nativa • informações do aplicativo") { dialog = "Sobre o MP SCAN" to "Aplicativo nativo do MP SCAN, criado para leitura, biblioteca e downloads com sincronização pelo Firebase." } }
    }
    dialog?.let { (title, text) -> AlertDialog(onDismissRequest = { dialog = null }, confirmButton = { TextButton(onClick = { dialog = null }) { Text("Entendi") } }, title = { Text(title) }, text = { Text(text) }) }
}

@Composable private fun SettingsTitle(text: String) { Text(text.uppercase(), color = Pink, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 9.dp, start = 4.dp)) }

@Composable
private fun SettingsLink(icon: String, title: String, subtitle: String, onClick: () -> Unit) = MenuCard(icon, title, subtitle, onClick)

@Composable
private fun SettingsToggle(icon: String, title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) {
    Surface(color = Card, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Card2), contentAlignment = Alignment.Center) { Text(icon) }
            Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
            Switch(checked = checked, onCheckedChange = change)
        }
    }
}

@Composable
private fun ProfileScreen(back: () -> Unit, signedIn: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuthRepository() }
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf(auth.current(context)) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<AccountProfile?>(null) }
    LaunchedEffect(session?.idToken) {
        profile = session?.let { runCatching { auth.loadProfile(it) }.getOrNull() }
    }
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { FilledTonalButton(onClick = back) { Text("←") }; Spacer(Modifier.width(12.dp)); Text("Perfil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) } }
        item {
            Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Color(0xFF37224E), Card2)))) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xC90B0B0D)))))
                Box(Modifier.align(Alignment.BottomStart).padding(18.dp).size(94.dp).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Purple, Pink))), contentAlignment = Alignment.Center) { Text("👤", style = MaterialTheme.typography.headlineLarge) }
            }
        }
        if (session != null) {
            item { Text(profile?.name?.takeIf { it.isNotBlank() } ?: "Conta conectada", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(profile?.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: session?.email.orEmpty(), color = Muted) }
            item {
                Surface(Modifier.fillMaxWidth(), color = Card, shape = RoundedCornerShape(21.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        ProfileStat("0", "Seguidores"); ProfileStat("0", "Seguindo"); ProfileStat("0", "Obras lidas")
                    }
                }
            }
            item { MenuCard("✎", "Editar perfil", "Nome, arroba, foto, capa e privacidade") { message = "A edição completa do perfil está sendo conectada nesta reconstrução." } }
            item { Button(onClick = { auth.signOut(context); session = null; message = "Você saiu da conta com segurança." }, modifier = Modifier.fillMaxWidth()) { Text("Sair da conta") } }
        } else {
            item { Text("Entre na sua conta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text("O perfil será usado nos comentários, reações e sincronização da biblioteca.", color = Muted) }
            item { OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), singleLine = true, enabled = !busy, label = { Text("E-mail") }) }
            item { OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), singleLine = true, enabled = !busy, label = { Text("Senha") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()) }
            item { Button(onClick = { scope.launch { busy = true; message = "Entrando…"; runCatching { auth.signIn(email, password) }.onSuccess { auth.save(context, it); session = it; password = ""; message = "Conta conectada."; signedIn() }.onFailure { message = it.message.orEmpty() }; busy = false } }, modifier = Modifier.fillMaxWidth(), enabled = !busy && email.isNotBlank() && password.length >= 6) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Entrar") } }
            item { OutlinedButton(onClick = { scope.launch { busy = true; message = "Criando conta…"; runCatching { auth.createAccount(email, password) }.onSuccess { auth.save(context, it); session = it; password = ""; message = "Conta criada e conectada."; signedIn() }.onFailure { message = it.message.orEmpty() }; busy = false } }, modifier = Modifier.fillMaxWidth(), enabled = !busy && email.isNotBlank() && password.length >= 6) { Text("Criar conta") } }
        }
        if (message.isNotBlank()) item { Text(message, color = Color(0xFFE1BCF4)) }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = Purple, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge); Text(label, color = Muted, style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun AppHeader(subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Purple, Pink))),
            contentAlignment = Alignment.Center
        ) { Text("MP", fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(12.dp))
        Column { Text("MP SCAN", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun HomeScreen(state: CatalogState, open: (Work) -> Unit, refresh: () -> Unit) {
    when {
        state.loading && state.works.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.works.isEmpty() -> PlaceholderScreen("Catálogo vazio", "Nenhuma obra adequada foi encontrada agora.", "⌁", refresh)
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item { AppHeader("Leitura nativa e sincronizada") }
            item { FeaturedWork(state.works.first(), open) }
            item { WorkRail("Em alta", state.works.sortedByDescending { it.reads }.take(10), open) }
            item { WorkRail("Atualizações recentes", state.works.take(10), open) }
        }
    }
}

@Composable
private fun FeaturedWork(work: Work, open: (Work) -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(490.dp).clip(RoundedCornerShape(34.dp))
            .background(Card)
            .clickable { open(work) }
    ) {
        AsyncImage(model = work.banner.ifBlank { work.cover }, contentDescription = work.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF207070A), Color(0xB807070A), Color(0x2E07070A)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF207070A)))))
        Column(Modifier.align(Alignment.BottomStart).padding(26.dp).fillMaxWidth(.90f)) {
            Text("DESTAQUE MP SCAN", color = Pink, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(10.dp)); Text(work.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            if (work.sensitive) AssistChip(onClick = {}, label = { Text("Conteúdo sensível") })
            Text(work.synopsis, color = Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp)); Button(onClick = { open(work) }, shape = RoundedCornerShape(14.dp)) { Text("Ver detalhes", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun WorkRail(title: String, works: List<Work>, open: (Work) -> Unit) {
    Column { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(works, key = { it.id }) { WorkCard(it, open) } } }
}

@Composable
private fun WorkCard(work: Work, open: (Work) -> Unit) {
    Column(Modifier.width(145.dp).clickable { open(work) }) {
        AsyncImage(model = work.cover, contentDescription = work.title, modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(18.dp)).background(Card2), contentScale = ContentScale.Crop)
        Text(work.title, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (work.sensitive) "Sensível • ${work.type}" else work.type, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun SearchScreen(works: List<Work>, query: String, change: (String) -> Unit, open: (Work) -> Unit) {
    val filtered = remember(works, query) { works.filter { (it.title + " " + it.alternativeTitle + " " + it.author + " " + it.genres.joinToString()).contains(query, true) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
        AppHeader("Encontre sua próxima leitura"); Spacer(Modifier.height(18.dp))
        OutlinedTextField(value = query, onValueChange = change, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp), leadingIcon = { Text("⌕", color = Muted) }, label = { Text("Buscar por nome, autor ou gênero") })
        Text("${filtered.size} obras", color = Muted, modifier = Modifier.padding(vertical = 12.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { items(filtered, key = { it.id }) { WorkCard(it, open) } }
    }
}

@Composable
private fun WorkScreen(work: Work, chapters: List<Chapter>, loading: Boolean, progress: Map<String, Int>, inLibrary: Boolean, comments: List<WorkComment>, commentsLoading: Boolean, close: () -> Unit, openChapter: (Chapter) -> Unit, download: (Chapter) -> Unit, downloadAll: () -> Unit, toggleLibrary: () -> Unit, postComment: (String, Boolean) -> Unit, react: (WorkComment, String) -> Unit) {
    var commentText by rememberSaveable(work.id) { mutableStateOf("") }
    var spoiler by rememberSaveable(work.id) { mutableStateOf(false) }
    var workTab by rememberSaveable(work.id) { mutableStateOf("chapters") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(330.dp).padding(horizontal = 12.dp).clip(RoundedCornerShape(24.dp))) {
                AsyncImage(model = work.banner.ifBlank { work.cover }, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x50000000), Bg))))
                FilledTonalButton(onClick = close, modifier = Modifier.padding(14.dp).align(Alignment.TopStart), shape = RoundedCornerShape(14.dp)) { Text("←") }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp)) {
                AsyncImage(model = work.cover, contentDescription = work.title, modifier = Modifier.width(120.dp).aspectRatio(3f / 4f).clip(RoundedCornerShape(18.dp)).background(Card2), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp)); Column { Text(work.type.uppercase(), color = Color(0xFFE1BCF4), style = MaterialTheme.typography.labelSmall); Text(work.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(work.alternativeTitle, color = Muted); Text(work.status, modifier = Modifier.padding(top = 8.dp)); if (work.sensitive) AssistChip(onClick = {}, label = { Text("Aviso: sensível") }) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { chapters.lastOrNull()?.let(openChapter) }, enabled = chapters.isNotEmpty(), modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Ler agora", fontWeight = FontWeight.Black) }
                FilledTonalButton(onClick = toggleLibrary, shape = RoundedCornerShape(14.dp)) { Text(if (inLibrary) "✓" else "♡") }
            }
        }
        item {
            val all = progress["${work.id}__all"]
            OutlinedButton(onClick = downloadAll, enabled = all == null && chapters.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(all?.let { "Baixando todos… $it%" } ?: "⇩ Baixar todos os capítulos")
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                listOf("about" to "Sobre", "chapters" to "Capítulos", "comments" to "Comentários").forEachIndexed { index, (key, label) ->
                    SegmentedButton(selected = workTab == key, onClick = { workTab = key }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(label) }
                }
            }
        }
        if (workTab == "about") item {
            Surface(Modifier.fillMaxWidth().padding(16.dp), color = Card, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(16.dp)) { Text("Sinopse", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium); Text(work.synopsis.ifBlank { "Sinopse ainda não informada." }, color = Muted, modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyLarge); if (work.genres.isNotEmpty()) Text(work.genres.joinToString("  •  "), color = Pink, modifier = Modifier.padding(top = 14.dp)) }
            }
        }
        if (workTab == "chapters") item { Text("Capítulos", modifier = Modifier.padding(16.dp, 8.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        if (workTab == "chapters") items(chapters, key = { it.id }) { chapter ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clip(RoundedCornerShape(18.dp)).background(Card).clickable { openChapter(chapter) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFF2A1936)), contentAlignment = Alignment.Center) { Text(chapter.number?.toString()?.removeSuffix(".0") ?: "—", color = Color(0xFFE0BCF6), fontWeight = FontWeight.Black) }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(chapter.label, fontWeight = FontWeight.Bold); Text(chapter.title.ifBlank { "Toque para ler" }, color = Muted, style = MaterialTheme.typography.bodySmall); progress["${work.id}__${chapter.id}"]?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) } }
                IconButton(onClick = { download(chapter) }) { Text("⇩", style = MaterialTheme.typography.titleLarge) }
            }
        }
        if (workTab == "comments") item { Text("Comentários", modifier = Modifier.padding(16.dp, 22.dp, 16.dp, 8.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (workTab == "comments") item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)).background(Card).padding(14.dp)) {
                OutlinedTextField(commentText, { commentText = it }, Modifier.fillMaxWidth(), label = { Text("Escreva um comentário") }, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(spoiler, { spoiler = it }); Text("Marcar como spoiler", Modifier.weight(1f))
                    Button(onClick = { postComment(commentText, spoiler); commentText = ""; spoiler = false }, enabled = commentText.isNotBlank() && !commentsLoading) { Text("Enviar") }
                }
            }
        }
        if (workTab == "comments" && commentsLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp)) }
        if (workTab == "comments" && !commentsLoading && comments.isEmpty()) item { Text("Ainda não há comentários nesta obra.", color = Muted, modifier = Modifier.padding(16.dp)) }
        if (workTab == "comments") items(comments, key = { it.id }) { comment ->
            var revealed by rememberSaveable(comment.id) { mutableStateOf(!comment.spoiler) }
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), color = Card, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(14.dp)) {
                    Text(comment.author + comment.username.takeIf { it.isNotBlank() }?.let { "  @$it" }.orEmpty(), fontWeight = FontWeight.Bold)
                    if (!revealed) Button(onClick = { revealed = true }, modifier = Modifier.padding(top = 8.dp)) { Text("⚠ SPOILER — tocar para revelar") }
                    else Text(comment.text, color = Color.White, modifier = Modifier.padding(top = 7.dp))
                    LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("👍", "❤️", "😂", "😮", "😢", "🔥")) { emoji ->
                            FilterChip(selected = comment.myReaction == emoji, onClick = { react(comment, emoji) }, label = { Text(emoji + comment.reactions[emoji]?.let { " $it" }.orEmpty()) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderScreen(reader: ReaderState, close: () -> Unit, download: () -> Unit, progress: Int?) {
    val context = LocalContext.current
    val positionPrefs = remember { context.getSharedPreferences("mp_scan_reader_progress", android.content.Context.MODE_PRIVATE) }
    val positionKey = "${reader.work.id}__${reader.chapter.id}"
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = positionPrefs.getInt("${positionKey}_index", 0), initialFirstVisibleItemScrollOffset = positionPrefs.getInt("${positionKey}_offset", 0))
    var controlsVisible by rememberSaveable(positionKey) { mutableStateOf(true) }
    var settingsOpen by rememberSaveable(positionKey) { mutableStateOf(false) }
    var pageWidth by rememberSaveable(positionKey) { mutableFloatStateOf(positionPrefs.getFloat("${positionKey}_width", 100f)) }
    var pageGap by rememberSaveable(positionKey) { mutableFloatStateOf(positionPrefs.getFloat("${positionKey}_gap", 0f)) }
    val readingProgress by remember { derivedStateOf { if (reader.pages.isEmpty()) 0f else ((listState.firstVisibleItemIndex + 1f) / reader.pages.size).coerceIn(0f, 1f) } }
    LaunchedEffect(positionKey) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            positionPrefs.edit().putInt("${positionKey}_index", index).putInt("${positionKey}_offset", offset).apply()
        }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF050507))) {
        if (controlsVisible) Row(Modifier.fillMaxWidth().background(Color(0xE60B0B0D)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = close) { Text("←") }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(reader.work.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold); Text(reader.chapter.label, color = Muted, style = MaterialTheme.typography.bodySmall) }
            if (!reader.downloaded) Button(onClick = download, enabled = progress == null) { Text(progress?.let { "$it%" } ?: "Baixar") }
            else AssistChip(onClick = {}, label = { Text("Offline ✓") })
            IconButton(onClick = { settingsOpen = true }) { Text("⚙") }
        }
        if (controlsVisible) LinearProgressIndicator(progress = { readingProgress }, modifier = Modifier.fillMaxWidth(), color = Purple, trackColor = Card2)
        if (reader.pages.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhuma página encontrada.") }
        else LazyColumn(Modifier.fillMaxSize().clickable { controlsVisible = !controlsVisible }, state = listState, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(pageGap.dp)) {
            items(reader.pages) { page -> AsyncImage(model = page, contentDescription = null, modifier = Modifier.fillParentMaxWidth(pageWidth / 100f), contentScale = ContentScale.FillWidth) }
            item { Text("Fim de ${reader.chapter.label}", modifier = Modifier.padding(28.dp), color = Muted) }
        }
    }
    if (settingsOpen) AlertDialog(
        onDismissRequest = { settingsOpen = false },
        title = { Text("Configurações de leitura") },
        text = { Column { Text("Largura das imagens: ${pageWidth.toInt()}%"); Slider(pageWidth, { pageWidth = it }, valueRange = 50f..100f, steps = 9); Spacer(Modifier.height(12.dp)); Text("Espaço entre páginas: ${pageGap.toInt()} px"); Slider(pageGap, { pageGap = it }, valueRange = 0f..40f, steps = 19) } },
        confirmButton = { Button(onClick = { positionPrefs.edit().putFloat("${positionKey}_width", pageWidth).putFloat("${positionKey}_gap", pageGap).apply(); settingsOpen = false }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { settingsOpen = false }) { Text("Cancelar") } }
    )
}

@Composable
private fun LibraryScreen(works: List<Work>, items: List<OfflineChapter>, load: () -> Unit, openWork: (Work) -> Unit, open: (OfflineChapter) -> Unit, remove: (OfflineChapter) -> Unit) {
    var tab by rememberSaveable { mutableStateOf("works") }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 34.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AppHeader("Sua leitura disponível sem internet") }
        item {
            Surface(Modifier.fillMaxWidth(), color = Card, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(works.size.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Purple); Text("Obras salvas", color = Muted, style = MaterialTheme.typography.bodySmall) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(items.size.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Pink); Text("Capítulos offline", color = Muted, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("works" to "Biblioteca", "downloads" to "Downloads").forEachIndexed { index, (key, label) ->
                    SegmentedButton(selected = tab == key, onClick = { tab = key }, shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(label) }
                }
            }
        }
        if (tab == "works") item { Text("Minha biblioteca", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
        if (tab == "works" && works.isNotEmpty()) item { LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(works, key = { it.id }) { WorkCard(it, openWork) } } }
        if (tab == "works" && works.isEmpty()) item { PlaceholderScreen("Biblioteca vazia", "Adicione uma obra para acompanhar e baixar capítulos.", "♡") }
        if (tab == "downloads") item { Text("Downloads offline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp)) }
        if (tab == "downloads" && items.isEmpty()) item { PlaceholderScreen("Nada baixado ainda", "Abra uma obra e toque em ⇩ para salvar um capítulo.", "⇩") }
        if (tab == "downloads") items(items, key = { "${it.work.id}__${it.chapter.id}" }) { item ->
            Surface(Modifier.fillMaxWidth().clickable { open(item) }, color = Card, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(item.work.cover, item.work.title, Modifier.width(64.dp).aspectRatio(3f/4f).clip(RoundedCornerShape(14.dp)).background(Card2), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(item.work.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${item.chapter.label} • ${item.pageCount} páginas", color = Muted, style = MaterialTheme.typography.bodySmall); Text("Disponível offline", color = Color(0xFF72D7A5), style = MaterialTheme.typography.labelSmall) }
                    TextButton(onClick = { remove(item) }) { Text("Excluir") }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, text: String, icon: String, action: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, style = MaterialTheme.typography.displayMedium); Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(text, color = Muted, modifier = Modifier.padding(top = 7.dp)); if (action != null) Button(onClick = action, modifier = Modifier.padding(top = 16.dp)) { Text("Tentar novamente") } }
    }
}
