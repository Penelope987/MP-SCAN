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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import online.mpscan.nativeapp.data.FirebaseCatalogRepository
import online.mpscan.nativeapp.model.Chapter
import online.mpscan.nativeapp.model.Work

private val Bg = Color(0xFF09070D)
private val Card = Color(0xFF17101E)
private val Line = Color(0xFF382542)
private val Purple = Color(0xFF8750C1)
private val Pink = Color(0xFFD454A4)
private val Muted = Color(0xFFB7A5C1)

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
    val error: String? = null
)

class CatalogViewModel : ViewModel() {
    private val repository = FirebaseCatalogRepository()
    var state by mutableStateOf(CatalogState())
        private set

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        state = state.copy(loading = true, error = null)
        runCatching { repository.loadWorks() }
            .onSuccess { state = state.copy(loading = false, works = it) }
            .onFailure { state = state.copy(loading = false, error = "Não foi possível carregar o catálogo.") }
    }

    fun open(work: Work) = viewModelScope.launch {
        state = state.copy(selected = work, chapters = emptyList(), loading = true, error = null)
        runCatching { repository.loadChapters(work.id) }
            .onSuccess { state = state.copy(loading = false, chapters = it) }
            .onFailure { state = state.copy(loading = false, error = "Não foi possível carregar os capítulos.") }
    }

    fun closeWork() { state = state.copy(selected = null, chapters = emptyList(), error = null) }
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

@Composable
private fun MpScanApp(vm: CatalogViewModel = viewModel()) {
    var tab by remember { mutableStateOf(Tab.Home) }
    var query by remember { mutableStateOf("") }
    val state = vm.state
    BackHandler(state.selected != null) { vm.closeWork() }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            if (state.selected == null) NavigationBar(containerColor = Color(0xF5120C19)) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(item.icon) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.selected != null -> WorkScreen(state.selected, state.chapters, state.loading, vm::closeWork)
                tab == Tab.Home -> HomeScreen(state, vm::open, vm::refresh)
                tab == Tab.Search -> SearchScreen(state.works, query, { query = it }, vm::open)
                tab == Tab.Library -> PlaceholderScreen("Sua biblioteca", "Downloads e progresso offline entrarão na próxima etapa.", "▣")
                else -> PlaceholderScreen("Mais", "Conta, notificações e preferências serão conectadas aqui.", "⚙")
            }
            state.error?.let { message ->
                Snackbar(Modifier.align(Alignment.BottomCenter).padding(12.dp)) { Text(message) }
            }
        }
    }
}

@Composable
private fun AppHeader(subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(50.dp).clip(RoundedCornerShape(17.dp))
                .background(Brush.linearGradient(listOf(Purple, Pink))),
            contentAlignment = Alignment.Center
        ) { Text("MP", fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(12.dp))
        Column { Text("MP SCAN", fontWeight = FontWeight.Black); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun HomeScreen(state: CatalogState, open: (Work) -> Unit, refresh: () -> Unit) {
    when {
        state.loading && state.works.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.works.isEmpty() -> PlaceholderScreen("Catálogo vazio", "Nenhuma obra adequada foi encontrada agora.", "⌁", refresh)
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
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
        Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF4A285E), Color(0xFF211329))))
            .clickable { open(work) }
    ) {
        AsyncImage(model = work.banner.ifBlank { work.cover }, contentDescription = work.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF20D0910), Color(0x40100B12)))))
        Column(Modifier.align(Alignment.BottomStart).padding(24.dp).fillMaxWidth(.88f)) {
            Text("DESTAQUE MP SCAN", color = Color(0xFFE1BCF4), fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp)); Text(work.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            if (work.sensitive) AssistChip(onClick = {}, label = { Text("Conteúdo sensível") })
            Text(work.synopsis, color = Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp)); Button(onClick = { open(work) }) { Text("Ver obra") }
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
        AsyncImage(model = work.cover, contentDescription = work.title, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(18.dp)).background(Card), contentScale = ContentScale.Crop)
        Text(work.title, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (work.sensitive) "Sensível • ${work.type}" else work.type, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun SearchScreen(works: List<Work>, query: String, change: (String) -> Unit, open: (Work) -> Unit) {
    val filtered = remember(works, query) { works.filter { (it.title + " " + it.alternativeTitle + " " + it.author + " " + it.genres.joinToString()).contains(query, true) } }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        AppHeader("Encontre sua próxima leitura"); Spacer(Modifier.height(18.dp))
        OutlinedTextField(value = query, onValueChange = change, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Buscar por nome, autor ou gênero") })
        Text("${filtered.size} obras", color = Muted, modifier = Modifier.padding(vertical = 12.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { items(filtered, key = { it.id }) { WorkCard(it, open) } }
    }
}

@Composable
private fun WorkScreen(work: Work, chapters: List<Chapter>, loading: Boolean, close: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(230.dp)) {
                AsyncImage(model = work.banner.ifBlank { work.cover }, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x50000000), Bg))))
                FilledTonalButton(onClick = close, modifier = Modifier.padding(14.dp).align(Alignment.TopStart)) { Text("← Voltar") }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp)) {
                AsyncImage(model = work.cover, contentDescription = work.title, modifier = Modifier.width(112.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp)); Column { Text(work.type.uppercase(), color = Color(0xFFE1BCF4), style = MaterialTheme.typography.labelSmall); Text(work.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(work.alternativeTitle, color = Muted); Text(work.status, modifier = Modifier.padding(top = 8.dp)); if (work.sensitive) AssistChip(onClick = {}, label = { Text("Aviso: sensível") }) }
            }
        }
        item { Text(work.synopsis.ifBlank { "Sinopse ainda não informada." }, color = Muted, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge) }
        item { Text("Capítulos", modifier = Modifier.padding(16.dp, 8.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        items(chapters, key = { it.id }) { chapter ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clip(RoundedCornerShape(18.dp)).background(Card).clickable { }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFF2A1936)), contentAlignment = Alignment.Center) { Text(chapter.number?.toString()?.removeSuffix(".0") ?: "—", color = Color(0xFFE0BCF6), fontWeight = FontWeight.Black) }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(chapter.label, fontWeight = FontWeight.Bold); Text(chapter.title.ifBlank { "Toque para ler" }, color = Muted, style = MaterialTheme.typography.bodySmall) }
                Text("›", style = MaterialTheme.typography.headlineSmall)
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
