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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val GAME_PACKAGE = "com.vortexrp.crmp" // TODO: set the real CRMP client package

private val Background = Color(0xFF090611)
private val Panel = Color(0xFF171027)
private val Purple = Color(0xFF9B5CFF)
private val Pink = Color(0xFFE05BFF)
private val Muted = Color(0xFFB9AFCB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Purple,
                secondary = Pink,
                background = Background,
                surface = Panel
            )) {
                LauncherScreen(
                    onPlay = { launchGame() },
                    onInstall = {
                        Toast.makeText(
                            this,
                            "Установщик подключим после выбора совместимого CRMP-клиента.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onSettings = {
                        Toast.makeText(this, "Настройки добавим следующим этапом.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun launchGame() {
        val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(GAME_PACKAGE)
        if (launchIntent == null) {
            Toast.makeText(
                this,
                "CRMP-клиент не найден. Нужно указать его настоящий package name.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            startActivity(launchIntent)
        }
    }
}

@Composable
private fun LauncherScreen(
    onPlay: () -> Unit,
    onInstall: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF24103F), Background, Color(0xFF090611))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Brush.linearGradient(listOf(Purple, Pink)), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("V", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("VORTEX", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("ROLE PLAY", color = Muted, fontSize = 11.sp, letterSpacing = 4.sp)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = Muted)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ТВОЙ ГОРОД. ТВОЯ ИСТОРИЯ.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Добро пожаловать в Vortex Role Play", color = Muted, fontSize = 14.sp)
            }

            ServerCard()

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

            Text(
                "Vortex Launcher • 0.1.0 • прототип",
                color = Color(0xFF766A8A),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun ServerCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF56358A), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Cloud, contentDescription = null, tint = Purple)
            Spacer(Modifier.width(8.dp))
            Text("СОСТОЯНИЕ СЕРВЕРА", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text("ОЖИДАЕТ НАСТРОЙКИ", color = Color(0xFFFFC66D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text("Vortex Role Play", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("Адрес сервера добавим после настройки CRMP-сервера", color = Muted, fontSize = 12.sp)
        HorizontalDivider(color = Color(0xFF35254D))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ServerMetric("ИГРОКИ", "—")
            ServerMetric("ПИНГ", "—")
            ServerMetric("ВЕРСИЯ", "0.1.0")
        }
    }
}

@Composable
private fun ServerMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Muted, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF130D20), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF34224F), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(subtitle, color = Muted, fontSize = 11.sp)
    }
}
