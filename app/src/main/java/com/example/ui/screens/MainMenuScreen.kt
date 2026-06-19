package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.RoomState
import com.example.game.network.LanManager
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.ParchmentCard
import com.example.ui.components.ParchmentHeaderBanner
import com.example.ui.theme.*
import com.example.ui.components.MysteryBackground

@Composable
fun MainMenuOrLobbyScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    var showPlayerSetup by remember { mutableStateOf(false) }
    var showLanJoinLobby by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    val discoveredHosts by LanManager.discoveredHosts.collectAsState()
    val localIp = remember { LanManager.getLocalIpAddress() }

    if (isSettingsOpen) {
        SettingsDialog(viewModel = viewModel) { isSettingsOpen = false }
    }

    if (state.mode == "LAN") {
        val isHost = state.hostId == viewModel.myPlayerId.value
        if (isHost) {
            HostLobbyScreen(viewModel, state)
        } else {
            ClientWaitingScreen(viewModel, state)
        }
    } else {
        if (showPlayerSetup) {
            LocalSetupScreen(viewModel, state) { showPlayerSetup = false }
        } else if (showLanJoinLobby) {
            LanJoinLobbyScreen(viewModel, state, discoveredHosts, localIp) { showLanJoinLobby = false }
        } else {
            MainMenuHomeScreen(
                viewModel = viewModel,
                onStartPassPlay = {
                    viewModel.setupPassAndPlayGame()
                    showPlayerSetup = true
                },
                onOpenLanJoin = {
                    LanManager.startDiscovery()
                    showLanJoinLobby = true
                },
                onOpenSettings = { isSettingsOpen = true }
            )
        }
    }
}

@Composable
fun HostLobbyScreen(viewModel: GameViewModel, state: RoomState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "اوضة المضيف")
        Spacer(modifier = Modifier.height(10.dp))
        ThrillerTitleComponent(fontSize = 38.sp)
        Spacer(modifier = Modifier.height(10.dp))
        ParchmentCard(modifier = Modifier.weight(1f), seed = 789L) {
            Text(
                text = "شارك هذا الكود مع أصدقائك للانضمام:",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0A05)),
                border = BorderStroke(2.dp, GoldShine),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = state.roomId,
                    color = GoldShine,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "اللاعبين المنضمون (${state.players.size}) : ",
                color = Color(0xFF4A1008),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.players, key = { it.id }) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x1F2C1E14), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).background(DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = player.avatarId.toString(), color = GoldShine, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = player.name + if (player.id == state.hostId) " (مضيف)" else "",
                            color = PapyrusText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (player.id != state.hostId) {
                            IconButton(onClick = { viewModel.removePlayerFromLobby(player.id) }) {
                                Icon(Icons.Default.Delete, "Remove Client", tint = RedAccent)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            val needed = 4 - state.players.size
            if (needed > 0) {
                Text(
                    text = "متبقي $needed لاعبين كحد أدنى للبدء.",
                    color = RedAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "الاوضة جاهزة لبدء القضية!",
                    color = InnocentAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.resetToMainMenu() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1008)),
                modifier = Modifier.weight(1f)
            ) {
                Text("إلغاء الاوضة", color = GoldShine)
            }
            Button(
                onClick = { viewModel.startInvestigationGame() },
                enabled = state.players.size in 4..6,
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier.weight(1.5f)
            ) {
                Icon(Icons.Default.PlayArrow, "Start game", tint = GoldShine)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ابدأ القضية!", color = GoldShine, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ClientWaitingScreen(viewModel: GameViewModel, state: RoomState) {
    val myName = viewModel.myPlayerName.collectAsState().value
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "في انتظار التحقيق")
        Spacer(modifier = Modifier.height(10.dp))
        ThrillerTitleComponent(fontSize = 38.sp)
        Spacer(modifier = Modifier.height(10.dp))
        ParchmentCard(modifier = Modifier.weight(1f), seed = 999L) {
            Text(
                text = "أنت منضم للاوضة رقم:",
                color = PapyrusTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.roomId,
                color = Color(0xFF4A1008),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedAccent, strokeWidth = 3.dp)
                Icon(Icons.Default.Fingerprint, "Investigating fingerprints", tint = DarkWoodButton, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "يرجى الانتظار بينما يجمع المضيف اللاعبين الآخرين لبدء توزيع الأدلة الجنائية السرية...",
                color = PapyrusText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = " اللاعبون الحاليون باللوبي (${state.players.size}) : ",
                color = Color(0xFF4A1008),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.players, key = { it.id }) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x06000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x142C1E14), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp)
                                .background(if (player.name == myName) RedAccent else DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = player.avatarId.toString(), color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = player.name + if (player.name == myName) " (أنت)" else "",
                            color = PapyrusText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = { viewModel.resetToMainMenu() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("مغادرة والرجوع للرئيسية", color = GoldShine)
        }
    }
}

@Composable
fun LocalSetupScreen(viewModel: GameViewModel, state: RoomState, onBack: () -> Unit) {
    val context = LocalContext.current
    var tempPlayerName by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "إعداد اللاعبين")
        Spacer(modifier = Modifier.height(10.dp))
        ThrillerTitleComponent(fontSize = 32.sp)
        Spacer(modifier = Modifier.height(10.dp))
        ParchmentCard(modifier = Modifier.weight(1f), seed = 123L) {
            Text(
                text = "عدد اللاعبين: ${state.players.size} ",
                color = Color(0xFF4A1008),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "4 - 6 لاعبين (1 مجرم في 4 لاعبين، 2 مجرم في 5+ لاعبين)",
                color = PapyrusTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tempPlayerName,
                    onValueChange = { tempPlayerName = it },
                    label = { Text("اسم اللاعب الجديد") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PapyrusText,
                        unfocusedTextColor = PapyrusText,
                        focusedBorderColor = DarkWoodButton,
                        unfocusedBorderColor = PapyrusTextSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1f).testTag("player_name_input"),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (tempPlayerName.isNotBlank()) {
                            if (state.players.size < 6) {
                                viewModel.addLocalLobbyPlayer(tempPlayerName)
                                tempPlayerName = ""
                            } else {
                                Toast.makeText(context, "اخرك 6 لاعيبة", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier.testTag("add_player_button")
                ) {
                    Icon(Icons.Default.Add, "Add player", tint = GoldShine)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.players, key = { it.id }) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x1F2C1E14), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).background(DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = player.avatarId.toString(), color = GoldShine, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = player.name, color = PapyrusText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.removePlayerFromLobby(player.id) }) {
                            Icon(Icons.Default.Delete, "Remove", tint = RedAccent)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldShine),
                modifier = Modifier.weight(1f)
            ) {
                Text("رجوع")
            }
            Button(
                onClick = {
                    if (state.players.size < 4) Toast.makeText(context, "اقل حاجة 4 لاعيبة", Toast.LENGTH_SHORT).show()
                    else viewModel.startInvestigationGame()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier.weight(1.5f).testTag("start_game_button"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, "Start", tint = GoldShine)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ابدأ اللعبة 🔍", color = GoldShine, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LanJoinLobbyScreen(
    viewModel: GameViewModel,
    state: RoomState,
    discoveredHosts: Map<String, String>,
    localIp: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }
    var playerNameInput by remember { mutableStateOf("حمادة") }
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "الانضمام للاوضة")
        Spacer(modifier = Modifier.height(10.dp))
        ThrillerTitleComponent(fontSize = 32.sp)
        Spacer(modifier = Modifier.height(10.dp))
        ParchmentCard(modifier = Modifier.weight(1f), seed = 456L) {
            Text(
                text = "جهازك متصل بالشبكة المحلية IP: $localIp",
                color = PapyrusTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = playerNameInput,
                onValueChange = { playerNameInput = it },
                label = { Text("اسمك في اللعبة") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PapyrusText,
                    unfocusedTextColor = PapyrusText,
                    focusedBorderColor = DarkWoodButton,
                    unfocusedBorderColor = PapyrusTextSecondary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("player_name_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "انضم عن طريق رمز الغرفة:",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { if (it.length <= 5) inputCode = it.filter { char -> char.isDigit() } },
                    label = { Text("اكتب الرمز (5 أرقام)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PapyrusText,
                        unfocusedTextColor = PapyrusText,
                        focusedBorderColor = DarkWoodButton,
                        unfocusedBorderColor = PapyrusTextSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (inputCode.length == 5) {
                            val success = viewModel.joinLanHostByCode(inputCode, playerNameInput)
                            if (!success) Toast.makeText(context, "يبدو أن الرمز غير نشط بالشبكة حالياً. تأكد من تشغيل الاوضة من المضيف.", Toast.LENGTH_LONG).show()
                        } else Toast.makeText(context, "الرمز يجب أن يتكون من 5 أرقام", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ربط", color = GoldShine)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "أو اختر اوضة من كشف الشبكة (UDP):",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (discoveredHosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DarkWoodButton, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("يبحث عن لغز نشط على الـ WiFi...", color = PapyrusTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                val hostsList = remember(discoveredHosts) { discoveredHosts.toList() }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hostsList, key = { it.first }) { (ip, hostDetails) ->
                        val parts = remember(hostDetails) { hostDetails.split("|") }
                        val hostName = parts.getOrNull(0) ?: "اوضة مجهولة"
                        val rCode = parts.getOrNull(1) ?: "----"
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                                .border(2.dp, GoldYell, RoundedCornerShape(10.dp))
                                .clickable { viewModel.joinLanHost(ip, playerNameInput) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Wifi, "Wifi game", tint = RedAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(hostName, color = PapyrusText, fontWeight = FontWeight.Bold)
                                Text("رمز الاوضة: $rCode | IP: $ip", color = PapyrusTextSecondary, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.ArrowForward, "Join details", tint = DarkWoodButton)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { LanManager.stopDiscovery(); onBack() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("الرجوع للقائمة الرئيسية", color = GoldShine)
        }
    }
}

@Composable
fun MainMenuHomeScreen(
    viewModel: GameViewModel,
    onStartPassPlay: () -> Unit,
    onOpenLanJoin: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ThrillerTitleComponent()
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)).clickable { onStartPassPlay() }.testTag("new_game_opt_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = "Pass device", tint = DarkWoodButton, modifier = Modifier.size(36.dp))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), horizontalAlignment = Alignment.End) {
                        Text("لعبة جديدة", color = Color(0xFF4A1008), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("جرِّب اللعب بالتمرير (جهاز واحد)", color = PapyrusTextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go play", tint = DarkWoodButton)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)).clickable { onOpenLanJoin() }.testTag("lan_multiplayer_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = "WiFi game", tint = DarkWoodButton, modifier = Modifier.size(36.dp))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), horizontalAlignment = Alignment.End) {
                        Text("دخول برمز الغرفة (WiFi)", color = Color(0xFF4A1008), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("العب على جهازك مع أصدقائك بالرمز", color = PapyrusTextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go LAN Connect", tint = DarkWoodButton)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)).clickable { viewModel.startLanHost("مضيف التحقيق") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF35120D)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, GoldYell)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AddBox, "Host Game", tint = GoldShine)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("إنشاء ومشاركة اوضة جديدة (Host)", color = GoldShine, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)).clickable { onOpenSettings() }.testTag("settings_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings Icon", tint = DarkWoodButton, modifier = Modifier.size(30.dp))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), horizontalAlignment = Alignment.End) {
                        Text("الإعدادات وقواعد اللعب", color = Color(0xFF4A1008), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go settings", tint = DarkWoodButton)
                }
            }
        }
        Text(
            text = " !! القاعدة الاولي والاخيرة ... شك في الجميع",
            color = PapyrusBgLight.copy(alpha = 0.5f),
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp)
        )
    }
}