package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.data.CaseRepository
import com.example.game.model.Case as UserCase
import com.example.ui.components.ParchmentCard
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesLibraryScreen(
    repository: CaseRepository,
    completedCaseTitles: Set<String> = emptySet(),
    onPlayCase: (UserCase) -> Unit,
    onCreateNewCase: () -> Unit,
    onEditCase: (UserCase) -> Unit
) {
    var defaultCasesList by remember { mutableStateOf(listOf<UserCase>()) }
    var customCasesList by remember { mutableStateOf(listOf<UserCase>()) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var caseToExport by remember { mutableStateOf<UserCase?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Launcher لاستيراد القضايا
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val success = repository.importCases(context, it)
                if (success) {
                    customCasesList = repository.loadAllCustomCases()
                    Toast.makeText(context, "تم استيراد القضية بنجاح! 🎉", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "فشل استيراد الملف، تأكد من صحة التنسيق!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher لحفظ القضية كملف JSON على الجهاز
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { targetUri ->
            val caseData = caseToExport
            if (caseData != null) {
                scope.launch {
                    val success = withContext(Dispatchers.IO) {
                        try {
                            val jsonString = caseData.toJsonObject().toString(4)
                            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                            }
                            true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            false
                        }
                    }

                    if (success) {
                        Toast.makeText(context, "تم حفظ القضية بنجاح 📄", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "فشل حفظ الملف!", Toast.LENGTH_SHORT).show()
                    }
                    caseToExport = null
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        defaultCasesList = repository.getDefaultCases()
        customCasesList = repository.loadAllCustomCases()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مكتبة قضايا التحقيق", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PapyrusBg)
            )
        },
        containerColor = PapyrusBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = GoldShine)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استيراد قضايا", color = GoldShine, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onCreateNewCase,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = GoldShine)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("قضية جديدة", color = GoldShine, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF35120D),
                contentColor = GoldShine
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "قضايا اللعبة (${defaultCasesList.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selectedTabIndex == 0) GoldShine else Color(0xAAFFF8DC)
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "القضايا المضافة (${customCasesList.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selectedTabIndex == 1) GoldShine else Color(0xAAFFF8DC)
                        )
                    }
                )
            }

            val currentList = if (selectedTabIndex == 0) defaultCasesList else customCasesList

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTabIndex == 0) "لا توجد قضايا أساسية" else "لا توجد قضايا مضافة بعد",
                        color = PapyrusTextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(currentList, key = { _, item -> item.id.ifBlank { item.title } }) { index, userCase ->
                        val isPlayed = completedCaseTitles.contains(userCase.title)

                        ParchmentCard(
                            seed = userCase.title.hashCode().toLong(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .clickable { onPlayCase(userCase) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // أزرار التحكم (مسح للقضايا المضافة + تصدير/حفظ)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedTabIndex == 1) {
                                        IconButton(onClick = {
                                            scope.launch {
                                                repository.deleteCase(userCase.id)
                                                customCasesList = repository.loadAllCustomCases()
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "مسح", tint = RedAccent)
                                        }
                                    }

                                    // زر الحفظ كـ JSON
                                    IconButton(onClick = {
                                        caseToExport = userCase
                                        val safeFileName = "case_${userCase.title.replace(" ", "_")}.json"
                                        exportJsonLauncher.launch(safeFileName)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "حفظ كـ JSON",
                                            tint = Color(0xFF4A1008)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // علامة توضح هل لعبت القضية أم لا
                                        Surface(
                                            color = if (isPlayed) Color(0xFF2E7D32) else Color(0xFF8D6E63),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlayed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = if (isPlayed) "تم لعبها" else "جديدة",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // ترقيم القضية
                                        Text(
                                            text = "قضية #${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RedAccent
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = userCase.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A1008))
                                    Text(text = "المكان: ${userCase.location} | الوقت: ${userCase.time}", fontSize = 13.sp, color = PapyrusTextSecondary)
                                    Text(text = "المشتبه بهم: ${userCase.characters.size} لاعبين", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RedAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}