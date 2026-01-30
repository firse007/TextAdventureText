package com.example.textadventure

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("text_adventure_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var player by mutableStateOf(Player("Hero", 100, 100))
    var currentMonster by mutableStateOf<Monster?>(null)
    var gameLog by mutableStateOf(listOf("Welcome to the Text Adventure!"))
    var isGameOver by mutableStateOf(false)
    
    var showLevelUpScreen by mutableStateOf(false)
    var levelUpOptions by mutableStateOf<List<Item>>(emptyList())

    // Advanced Map System
    var nodes by mutableStateOf<List<GameNode>>(emptyList())
    var currentLayer by mutableStateOf(0)
    var showPathSelection by mutableStateOf(true)

    init {
        loadGame()
        if (nodes.isEmpty()) {
            generateMap()
        }
    }

    private fun generateMap() {
        val newNodes = mutableListOf<GameNode>()
        for (layer in 0 until 10) {
            val nodesInLayer = if (layer == 9) 1 else Random.nextInt(2, 4)
            for (i in 0 until nodesInLayer) {
                val type = when {
                    layer == 9 -> NodeType.MONSTER
                    layer % 3 == 0 && layer != 0 -> NodeType.REST
                    Random.nextInt(100) < 60 -> NodeType.MONSTER
                    else -> NodeType.TREASURE
                }
                newNodes.add(GameNode("${layer}_$i", type, layer))
            }
        }
        nodes = newNodes
        currentLayer = -1
        showPathSelection = true
    }

    private fun saveGame() {
        prefs.edit()
            .putString("player_save", gson.toJson(player))
            .putString("nodes_save", gson.toJson(nodes))
            .putInt("current_layer", currentLayer)
            .apply()
    }

    private fun loadGame() {
        val playerJson = prefs.getString("player_save", null)
        val nodesJson = prefs.getString("nodes_save", null)
        currentLayer = prefs.getInt("current_layer", -1)

        if (playerJson != null) player = gson.fromJson(playerJson, Player::class.java)
        if (nodesJson != null) {
            val type = object : TypeToken<List<GameNode>>() {}.type
            nodes = gson.fromJson(nodesJson, type)
            showPathSelection = currentMonster == null
        }
    }

    fun selectNode(node: GameNode) {
        if (node.layer != currentLayer + 1) return
        
        currentLayer = node.layer
        showPathSelection = false
        
        when (node.type) {
            NodeType.MONSTER -> spawnMonster()
            NodeType.REST -> {
                val healAmount = (player.totalMaxHp * 0.4f).toInt()
                player = player.copy(hp = (player.hp + healAmount).coerceAtMost(player.totalMaxHp))
                log("Rested. Healed $healAmount HP.")
                completeNode(node)
            }
            NodeType.TREASURE -> {
                log("Found a treasure!")
                prepareLevelUp()
                completeNode(node)
            }
        }
        saveGame()
    }

    private fun completeNode(node: GameNode) {
        nodes = nodes.map { if (it.id == node.id) it.copy(isCompleted = true) else it }
        if (currentLayer == 9) {
            log("Victory! Map cleared.")
            generateMap()
        } else {
            showPathSelection = true
        }
        saveGame()
    }

    fun attack() {
        val monster = currentMonster ?: return
        val playerElement = player.artifacts.firstOrNull { it.element != Element.NONE }?.element ?: Element.NONE
        val damageToMonster = ((player.attack * getElementBonus(playerElement, monster.element)) - monster.defense).toInt().coerceAtLeast(1)
        
        currentMonster = monster.copy(hp = monster.hp - damageToMonster)
        if (currentMonster!!.hp <= 0) {
            val defeatedMonster = currentMonster!!
            log("${defeatedMonster.name} defeated!")
            player = player.copy(exp = player.exp + defeatedMonster.expReward, gold = player.gold + defeatedMonster.goldReward)
            currentMonster = null
            if (player.canLevelUp()) prepareLevelUp()
            
            val node = nodes.find { it.layer == currentLayer && !it.isCompleted }
            if (node != null) completeNode(node)
            return
        }
        monsterAttack()
    }

    private fun monsterAttack() {
        val monster = currentMonster ?: return
        val damageToPlayer = (monster.attack - player.defense).coerceAtLeast(1)
        player = player.copy(hp = player.hp - damageToPlayer)
        if (player.hp <= 0) {
            isGameOver = true
            prefs.edit().clear().apply()
        }
    }

    private fun getElementBonus(attacker: Element, defender: Element): Float {
        return when {
            attacker == Element.FIRE && defender == Element.WIND -> 1.5f
            attacker == Element.WIND && defender == Element.EARTH -> 1.5f
            attacker == Element.EARTH && defender == Element.WATER -> 1.5f
            attacker == Element.WATER && defender == Element.FIRE -> 1.5f
            attacker != Element.NONE && defender != Element.NONE && attacker == defender -> 0.7f
            else -> 1.0f
        }
    }

    fun spawnMonster() {
        val monsterLevel = player.level + (currentLayer / 2)
        val monsterTypes = listOf(
            MonsterData("Slime", 20, 5, 1, 20, 10, Element.EARTH),
            MonsterData("Fire Bat", 15, 8, 0, 25, 12, Element.FIRE),
            MonsterData("Water Snake", 25, 6, 2, 30, 15, Element.WATER),
            MonsterData("Wind Wolf", 30, 10, 3, 50, 25, Element.WIND)
        )
        val type = monsterTypes.random()
        val hp = type.baseHp + (monsterLevel * 10)
        
        currentMonster = Monster(
            name = "${type.name} (Lv.$monsterLevel)",
            level = monsterLevel,
            hp = hp,
            maxHp = hp,
            attack = type.baseAtk + (monsterLevel * 2),
            defense = type.baseDef + (monsterLevel * 1),
            expReward = type.expBase + (monsterLevel * 5),
            goldReward = type.goldBase + (monsterLevel * 3),
            element = type.element
        )
    }

    private fun prepareLevelUp() {
        val options = mutableListOf<Item>()
        repeat(3) {
            val rarity = when(Random.nextInt(100)) { in 0..5 -> Rarity.LEGENDARY; in 6..20 -> Rarity.EPIC; in 21..50 -> Rarity.RARE; else -> Rarity.COMMON }
            val type = ItemType.entries.toTypedArray().random()
            val finalVal = ((if(type == ItemType.OFFENSE) player.level * 3 else if(type == ItemType.DEFENSE) player.level * 1 else player.level * 15) * rarity.multiplier).toInt() + Random.nextInt(1, 5)
            
            val artifactNames = mapOf(
                ItemType.OFFENSE to listOf("Power Gem", "Sharp Blade", "Cursed Skull"),
                ItemType.DEFENSE to listOf("Steel Plate", "Oak Shield", "Magic Barrier"),
                ItemType.UTILITY to listOf("Heart Stone", "Life Elixir", "Golden Apple")
            )
            
            options.add(Item(
                name = "${artifactNames[type]?.random()} (${rarity.name})",
                type = type,
                value = finalVal,
                rarity = rarity,
                element = Element.entries.toTypedArray().random(),
                description = "Grants +$finalVal to ${type.name}."
            ))
        }
        levelUpOptions = options
        showLevelUpScreen = true
    }

    fun selectLevelUpOption(item: Item) {
        val updatedArtifacts = player.artifacts.toMutableList()
        updatedArtifacts.add(item)
        
        var newPlayer = player.copy(artifacts = updatedArtifacts)
        if (newPlayer.canLevelUp()) {
            newPlayer.levelUp()
        }
        
        player = newPlayer
        showLevelUpScreen = false
        saveGame()
        if (player.canLevelUp()) prepareLevelUp()
    }

    fun restartGame() {
        player = Player("Hero", 100, 100)
        currentMonster = null
        isGameOver = false
        showLevelUpScreen = false
        prefs.edit().clear().apply()
        generateMap()
    }

    fun log(message: String) { gameLog = (gameLog + message).takeLast(10) }

    private data class MonsterData(
        val name: String, 
        val baseHp: Int, 
        val baseAtk: Int, 
        val baseDef: Int, 
        val expBase: Int, 
        val goldBase: Int, 
        val element: Element
    )
}
