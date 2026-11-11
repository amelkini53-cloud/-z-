package com.vortexrp.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

private const val GAME_PACKAGE = "com.vortexrp.crmp" // Replace with the chosen CRMP client's real package.
private const val PREFS = "vortex_launcher"
private const val PREF_SERVERS_URL = "servers_json_url"

private val Background = Color(0xFF090611)
private val Panel = Color(0xFF171027)
private val Purple = Color(0xFF9B5CFF)
private val Pink = Color(0xFFE05BFF)
private val Muted = Color(0xFFB9AFCB)

data class GameServer(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val version: String,
    val players: Int?
) {
    val address: String get() = "$host:$port"
}

class MainActivity : ComponentActivity() {
    private var servers by mutableStateOf<List<GameServer>>(emptyList())
    private var configStatus by mutableStateOf("Ссылка на servers.json не задана")
    private var configUrl by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        configUrl = prefs.getString(PREF_SERVERS_URL, "") ?: ""
        if (configUrl.isNotBlank()) loadServers(configUrl)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Purple, secondary = Pink, background = Background, surface = Panel
            )) {
                LauncherScreen(
                    servers = servers,
                    configStatus = configStatus,
                    configUrl = configUrl,
                    onPlay = { launchGame() },
                    onInstall = {
                        Toast.makeText(this, "Установщик подключим после выбора совместимого CRMP-клиента.", Toast.LENGTH_LONG).show()
                    },
                    onSaveConfigUrl = { newUrl ->
                        configUrl = newUrl.trim()
                        prefs.edit().putString(PREF_SERVERS_URL, configUrl).apply()
                        loadServers(configUrl)
                    }
                )
            }
        }
    }

    private fun loadServers(sharedUrl: String) {
        configStatus = "Загружаем servers.json…"
        thread {
            var connection: HttpURLConnection? = null
            try {
                val directUrl = normalizeDropboxUrl(sharedUrl)
                connection = (URL(directUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/json")
                }
                if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val array = JSONObject(body).optJSONArray("servers") ?: error("В JSON нет массива servers")
                val parsed = buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val host = item.optString("host").trim()
                        val port = item.optInt("port", -1)
                        if (host.isBlank() || port !in 1..65535) continue
                        add(GameServer(
                            id = item.optString("id", "server_${i + 1}"),
                            name = item.optString("name", "Сервер ${i + 1}"),
                            host = host,
                            port = port,
                            version = item.optString("version", "—"),
                            players = if (item.has("players") && !item.isNull("players")) item.optInt("players") else null
                        ))
                    }
                }
                if (parsed.isEmpty()) error("В servers.json нет корректных серверов")
                runOnUiThread {
                    servers = parsed
                    configStatus = "Загружено серверов: ${parsed.size}"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    servers = emptyList()
                    configStatus = "Ошибка загрузки: ${e.message ?: "проверь ссылку и JSON"}"
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun normalizeDropboxUrl(value: String): String {
        val trimmed = value.trim()
        require(trimmed.startsWith("https://")) { "Нужна HTTPS-ссылка общего доступа" }
        if (!trimmed.contains("dropbox.com")) return trimmed
        return when {
            trimmed.contains("dl=0") -> trimmed.replace("dl=0", "raw=1")
            trimmed.contains("raw=1") -> trimmed
            trimmed.contains("?") -> "$trimmed&raw=1"
            else -> "$trimmed?raw=1"
        }
    }

    private fun launchGame() {
        val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(GAME_PACKAGE)
        if (launchIntent == null) {
            Toast.makeText(this, "CRMP-клиент не найден. Укажи настоящий package name совместимого клиента.", Toast.LENGTH_LONG).show()
        } else {
            startActivity(launchIntent)
        }
    }
}

@Composable
private fun LauncherScreen(
    servers: List<GameServer>,
    configStatus: String,
    configUrl: String,
    onPlay: () -> Unit,
    onInstall: () -> Unit,
    onSaveConfigUrl: (String) -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }
    var editedUrl by remember(configUrl) { mutableStateOf(configUrl) }

    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF24103F), Background, Color(0xFF090611)))
    )) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).background(
                    Brush.linearGradient(listOf(Purple, Pink)), RoundedCornerShape(15.dp)
                ), contentAlignment = Alignment.Center) {
                    Text("V", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("VORTEX", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("ROLE PLAY", color = Muted, fontSize = 11.sp, letterSpacing = 4.sp)
                }
                IconButton(onClick = { editedUrl = configUrl; settingsOpen = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = Muted)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ТВОЙ ГОРОД. ТВОЯ ИСТОРИЯ.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Добро пожаловать в Vortex Role Play", color = Muted, fontSize = 14.sp)
            }

            ServerCard(servers, configStatus)

            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Icon(Icons.Default.SportsEsports, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("ИГРАТЬ", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }

            OutlinedButton(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Pink)
                Spacer(Modifier.width(9.dp))
                Text("УСТАНОВИТЬ / ОБНОВИТЬ ИГРУ")
            }

            Text("РАЗДЕЛЫ ЛАУНЧЕРА", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("Новости", "Обновления проекта", Modifier.weight(1f))
                FeatureCard("Профиль", "Аккаунт игрока", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("Файлы", "Проверка клиента", Modifier.weight(1f))
                FeatureCard("Поддержка", "Помощь игрокам", Modifier.weight(1f))
            }

            Text("Vortex Launcher • 0.2.0 • servers.json", color = Color(0xFF766A8A), fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp))
        }
    }

    if (settingsOpen) {
        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text("Подключение servers.json") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Вставь публичную HTTPS-ссылку Dropbox на JSON-файл. Лаунчер преобразует её для прямой загрузки.",
                        color = Muted, fontSize = 13.sp)
                    OutlinedTextField(
                        value = editedUrl,
                        onValueChange = { editedUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ссылка на servers.json") },
                        placeholder = { Text("https://www.dropbox.com/s/...") },
                        singleLine = false,
                        minLines = 2
                    )
                    Text("После сохранения список серверов загрузится автоматически.", color = Muted, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveConfigUrl(editedUrl)
                    settingsOpen = false
                }) { Text("СОХРАНИТЬ") }
            },
            dismissButton = { TextButton(onClick = { settingsOpen = false }) { Text("ОТМЕНА") } },
            containerColor = Panel,
            titleContentColor = Color.White,
            textContentColor = Muted
        )
    }
}

@Composable
private fun ServerCard(servers: List<GameServer>, status: String) {
    Column(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF56358A), RoundedCornerShape(20.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Cloud, contentDescription = null, tint = Purple)
            Spacer(Modifier.width(8.dp))
            Text("СПИСОК СЕРВЕРОВ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        if (servers.isEmpty()) {
            Text("Серверы ещё не загружены", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        } else {
            servers.forEachIndexed { index, server ->
                if (index > 0) HorizontalDivider(color = Color(0xFF35254D))
                Text(server.name, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(server.address, color = Pink, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ServerMetric("ИГРОКИ", server.players?.toString() ?: "—")
                    ServerMetric("ВЕРСИЯ", server.version)
                    ServerMetric("ПОРТ", server.port.toString())
                }
            }
        }
        HorizontalDivider(color = Color(0xFF35254D))
        Text(status, color = if (servers.isEmpty()) Color(0xFFFFC66D) else Color(0xFF8BE0A3),
            fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ServerMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Muted, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Color(0xFF130D20), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF34224F), RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(subtitle, color = Muted, fontSize = 11.sp)
    }
}
