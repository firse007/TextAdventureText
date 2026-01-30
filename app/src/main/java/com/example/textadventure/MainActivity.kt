package com.example.textadventure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.textadventure.ui.theme.TextAdventureTheme

/**
 * MainActivity: คลาสหลักที่เป็นจุดเริ่มต้นของแอปพลิเคชัน
 * ทำหน้าที่ตั้งค่าหน้าจอเบื้องต้นและเรียกใช้ GameScreen เพื่อแสดงผล
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // เปิดโหมดแสดงผลเต็มหน้าจอ
        setContent {
            TextAdventureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    GameScreen()
                }
            }
        }
    }
}

/**
 * GameScreen: ฟังก์ชัน Composable หลักที่ควบคุมการแสดงผลของเกมทั้งหมด
 * จัดการทั้งส่วนของสถานะตัวละคร, การต่อสู้กับมอนสเตอร์, การเลือกด่าน และหน้าจอเลเวลอัป
 */
@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val player = viewModel.player
    val monster = viewModel.currentMonster

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding()
        ) {
            // ส่วนแสดงข้อมูลผู้เล่น (Player Header) เช่น ชื่อ, เลเวล, และแถบค่าประสบการณ์ (EXP)
            Text(text = player.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            
            // แถบแสดงค่าประสบการณ์ (EXP Bar)
            LinearProgressIndicator(
                progress = { player.exp.toFloat() / player.nextLevelExp.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 4.dp),
                color = Color.Cyan,
                trackColor = Color.DarkGray
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Lv. ${player.level}", color = Color.White)
                Text(text = "HP: ${player.hp}/${player.totalMaxHp}", color = Color.Red, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ส่วนแสดงไอเทมที่สวมใส่ (Artifact Collection)
            Text("Artifacts: ${player.artifacts.size}", color = Color.Gray, fontSize = 12.sp)
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(player.artifacts) { artifact ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2C2C2C), CircleShape)
                            .border(1.dp, artifact.rarity.color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(artifact.name.take(1).uppercase(), color = artifact.rarity.color, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ส่วนเลือกเส้นทาง (Path Selection UI) จะแสดงเมื่อจบการต่อสู้ในแต่ละชั้น
            if (viewModel.showPathSelection) {
                val nextLayerNodes = viewModel.nodes.filter { it.layer == viewModel.currentLayer + 1 }
                
                Text("Layer ${viewModel.currentLayer + 2}: Choose your path", color = Color.Yellow, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    nextLayerNodes.forEach { node ->
                        NodeItem(
                            node = node,
                            isSelectable = true,
                            isPassed = false,
                            onClick = { viewModel.selectNode(node) }
                        )
                    }
                }
            }

            // ส่วนแสดงผลมอนสเตอร์ (Monster Area) แสดงเมื่อมีการต่อสู้เกิดขึ้น
            if (monster != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    border = BorderStroke(1.dp, monster.element.color)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(monster.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Element: ${monster.element}", color = monster.element.color, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("HP: ${monster.hp}/${monster.maxHp}", color = Color.Red)
                            LinearProgressIndicator(
                                progress = { (monster.hp.toFloat() / monster.maxHp.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.width(200.dp).padding(top = 4.dp),
                                color = Color.Red
                            )
                        }
                    }
                }
            } else if (!viewModel.showPathSelection) {
                // ข้อความแสดงระหว่างรอเดินทางไปชั้นถัดไป
                Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    Text("Proceeding to next layer...", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ส่วนบันทึกเหตุการณ์ (Game Log) แสดงสถานะการโจมตีและความเสียหายที่เกิดขึ้น
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(8.dp)
            ) {
                items(viewModel.gameLog.reversed()) { msg ->
                    Text(text = msg, color = if (msg.contains("Effective")) Color.Yellow else Color.White, fontSize = 14.sp)
                }
            }

            // ปุ่มกดสำหรับการต่อสู้ (Battle Buttons)
            if (monster != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Button(
                        onClick = { viewModel.attack() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB22222))
                    ) {
                        Text("ATTACK")
                    }
                }
            }
        }

        // หน้าต่างเลือกไอเทมเมื่อเลเวลอัป (Level Up Screen Overlay)
        if (viewModel.showLevelUpScreen) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CHOOSE AN ARTIFACT", color = Color.Yellow, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(24.dp))
                    viewModel.levelUpOptions.forEach { item ->
                        ItemCard(item) { viewModel.selectLevelUpOption(item) }
                    }
                }
            }
        }

        // หน้าต่างจบเกม (Game Over Screen)
        if (viewModel.isGameOver) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RUN ENDED", color = Color.Red, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Text("Layer Reached: ${viewModel.currentLayer + 1}/10", color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.restartGame() }) { Text("START NEW RUN") }
                }
            }
        }
    }
}

/**
 * NodeItem: ส่วนที่ใช้แสดงผลสัญลักษณ์ของแต่ละด่านบนแผนที่เลือกเส้นทาง
 * @param node ข้อมูลด่านที่ต้องการแสดง
 * @param isSelectable สามารถคลิกเลือกได้หรือไม่
 * @param isPassed ด่านนี้ถูกผ่านไปแล้วหรือไม่
 */
@Composable
fun NodeItem(node: GameNode, isSelectable: Boolean, isPassed: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .alpha(if (isSelectable || isPassed) 1f else 0.3f)
            .clickable(enabled = isSelectable) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    if (isPassed) Color.DarkGray else node.type.color.copy(alpha = 0.2f),
                    CircleShape
                )
                .border(
                    width = 2.dp,
                    color = node.type.color,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = node.type.label.take(1),
                color = node.type.color,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        Text(
            text = node.type.label,
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * ItemCard: การ์ดแสดงรายละเอียดของไอเทม (Artifact) ในหน้าเลือกตอนเลเวลอัป
 * @param item ข้อมูลไอเทมที่ต้องการแสดง
 * @param onClick เหตุการณ์เมื่อคลิกเลือกไอเทมชิ้นนี้
 */
@Composable
fun ItemCard(item: Item, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252525)),
        border = BorderStroke(2.dp, item.rarity.color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.name, color = item.rarity.color, fontWeight = FontWeight.Bold)
                Text(item.type.name, color = Color.Gray, fontSize = 10.sp)
            }
            Text(item.description, color = Color.LightGray, fontSize = 12.sp)
            Text("Element: ${item.element}", color = item.element.color, fontSize = 12.sp)
        }
    }
}
