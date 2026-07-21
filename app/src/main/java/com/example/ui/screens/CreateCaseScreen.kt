package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.data.CaseRepository
import com.example.game.model.Case as UserCase
import com.example.game.model.Character as UserCharacter
import com.example.ui.components.ParchmentCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCaseScreen(
    repository: CaseRepository,
    editingCase: UserCase? = null,
    onCaseSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var title by remember { mutableStateOf(editingCase?.title ?: "") }
    var location by remember { mutableStateOf(editingCase?.location ?: "") }
    var time by remember { mutableStateOf(editingCase?.time ?: "") }
    var victim by remember { mutableStateOf(editingCase?.victim ?: "") }
    var description by remember { mutableStateOf(editingCase?.description ?: "") }
    var explanation by remember { mutableStateOf(editingCase?.explanation ?: "") }

    var suspectCount by remember { mutableStateOf(editingCase?.characters?.size ?: 4) }

    val criminalCount = if (suspectCount == 4) 1 else 2
    val evidenceCount = if (suspectCount == 4) 3 else 4

    val evidences = remember {
        mutableStateListOf<String>().apply {
            val initialList = editingCase?.evidenceList ?: List(4) { "" }
            addAll(initialList)
        }
    }

    val characters = remember {
        mutableStateListOf<UserCharacter>().apply {
            if (editingCase != null && editingCase.characters.isNotEmpty()) {
                addAll(editingCase.characters)
            } else {
                repeat(6) {
                    add(
                        UserCharacter(
                            name = "", age = 30, occupation = "", background = "", traits = "غامض", hiddenMotive = "غير معروف",
                            fullName = "", personalitySummary = "شخصية غامضة", socialStatus = "متوسط الحال", relationshipToVictim = "مجهول",
                            relationshipToOtherSuspects = "", possibleMotive = "غير معروف", relevantHistory = "سجل خالي من السوابق", isMafia = false
                        )
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingCase == null) "صناعة قضية جديدة" else "تعديل القضية", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PapyrusBg)
            )
        },
        containerColor = PapyrusBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ParchmentCard(seed = 123L) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("بيانات اللغز الأساسية", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A1008))

                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان القضية") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("المكان") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("الوقت والتاريخ") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = victim, onValueChange = { victim = it }, label = { Text("الضحية") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("قصة ووصف الجريمة العثور عليها") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                }
            }

            ParchmentCard(seed = 456L) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("قواعد وهيكل القضية", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A1008))
                    Text("اختر عدد المشتبه بهم الإجمالي في هذه القضية:", fontSize = 14.sp, color = PapyrusText)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(4, 5, 6).forEach { count ->
                            FilterChip(
                                selected = suspectCount == count,
                                onClick = { suspectCount = count },
                                label = { Text("$count لاعبين") }
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0x22000000))
                    Text("حسب اختيارك؛ تتطلب اللعبة تلقائياً: $criminalCount مجرم و $evidenceCount دلائل.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RedAccent)
                }
            }

            ParchmentCard(seed = 789L) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("إدخال الدلائل ($evidenceCount دلائل مطلوبة)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A1008))
                    for (i in 0 until evidenceCount) {
                        while (evidences.size <= i) {
                            evidences.add("")
                        }
                        OutlinedTextField(
                            value = evidences[i],
                            onValueChange = { text -> evidences[i] = text },
                            label = { Text("دليل رقم ${i + 1}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            ParchmentCard(seed = 999L) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("بيانات المشتبه بهم ($suspectCount لاعبين)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A1008))
                    Text("تنبيه: يجب تفعيل خانة 'هل هو مجرم؟' لعدد $criminalCount شخصيات بالظبط.", fontSize = 12.sp, color = RedAccent)

                    for (i in 0 until suspectCount) {
                        while (characters.size <= i) {
                            characters.add(
                                UserCharacter(
                                    name = "", age = 30, occupation = "", background = "", traits = "غامض", hiddenMotive = "غير معروف",
                                    fullName = "", personalitySummary = "شخصية غامضة", socialStatus = "متوسط الحال", relationshipToVictim = "مجهول",
                                    relationshipToOtherSuspects = "", possibleMotive = "غير معروف", relevantHistory = "سجل خالي من السوابق", isMafia = false
                                )
                            )
                        }
                        val currentSuspect = characters[i]

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("المشتبه به رقم ${i + 1}", fontWeight = FontWeight.Bold, color = Color(0xFF4A1008))

                            OutlinedTextField(
                                value = currentSuspect.name,
                                onValueChange = { text -> characters[i] = currentSuspect.copy(name = text, fullName = text) },
                                label = { Text("الاسم") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = currentSuspect.occupation,
                                onValueChange = { text -> characters[i] = currentSuspect.copy(occupation = text) },
                                label = { Text("الوظيفة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = currentSuspect.background,
                                onValueChange = { text -> characters[i] = currentSuspect.copy(background = text) },
                                label = { Text("الخلفية والسر") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = currentSuspect.hiddenMotive,
                                onValueChange = { text -> characters[i] = currentSuspect.copy(hiddenMotive = text, possibleMotive = text) },
                                label = { Text("الدافع المحتمل") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = currentSuspect.isMafia,
                                    onCheckedChange = { isChecked -> characters[i] = currentSuspect.copy(isMafia = isChecked) }
                                )
                                Text("هل هذا اللاعب هو المجرم؟", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            HorizontalDivider(color = Color(0x11000000))
                        }
                    }
                }
            }

            ParchmentCard(seed = 111L) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الحل النهائي (Explanation)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A1008))
                    OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("شرح طريقة ارتكاب الجريمة والحل المفصل للتطبيق") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                }
            }

            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "الرجاء إدخال عنوان القضية على الأقل", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // تنقية وتجهيز المشتبه بهم المطلوبة
                    val sanitizedCharacters = characters.take(suspectCount).mapIndexed { index, suspect ->
                        val charName = suspect.name.ifBlank { "مشتبه به ${index + 1}" }
                        suspect.copy(
                            name = charName,
                            fullName = charName,
                            occupation = suspect.occupation.ifBlank { "غير محدد" },
                            background = suspect.background.ifBlank { "لا توجد معلومات" },
                            traits = suspect.traits.ifBlank { "غامض" },
                            hiddenMotive = suspect.hiddenMotive.ifBlank { "غير معروف" },
                            personalitySummary = suspect.personalitySummary.ifBlank { "شخصية غامضة" },
                            socialStatus = suspect.socialStatus.ifBlank { "متوسط الحال" },
                            relationshipToVictim = suspect.relationshipToVictim.ifBlank { "مجهول" },
                            possibleMotive = suspect.possibleMotive.ifBlank { "غير معروف" },
                            relevantHistory = suspect.relevantHistory.ifBlank { "سجل خالي من السوابق" }
                        )
                    }

                    // تنقية الدلائل
                    val sanitizedEvidences = evidences.take(evidenceCount).mapIndexed { index, ev ->
                        ev.ifBlank { "دليل رقم ${index + 1}" }
                    }

                    val finalCase = UserCase(
                        id = editingCase?.id ?: java.util.UUID.randomUUID().toString(),
                        title = title,
                        location = location.ifBlank { "غير محدد" },
                        time = time.ifBlank { "غير محدد" },
                        description = description.ifBlank { "لا توجد تفاصيل" },
                        victim = victim.ifBlank { "مجهول" },
                        victimProfile = editingCase?.victimProfile ?: "ملف ضحية مجهولة",
                        suspicionDistribution = "توزيع الشك متساوي",
                        hint = "ابحث في الدلائل جيدا",
                        explanation = explanation.ifBlank { "لم يتم إضافة شرح بعد" },
                        evidenceList = sanitizedEvidences,
                        characters = sanitizedCharacters
                    )

                    scope.launch {
                        try {
                            repository.saveCase(finalCase)
                            Toast.makeText(context, "تم حفظ القضية بنجاح 💾", Toast.LENGTH_SHORT).show()
                            onCaseSaved()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "حدث خطأ أثناء الحفظ", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ القضية والملف والعودة للمكتبة", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}