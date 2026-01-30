package com.example.textadventure

import androidx.compose.ui.graphics.Color

enum class ItemType { OFFENSE, DEFENSE, UTILITY }

enum class NodeType(val label: String, val color: Color) {
    MONSTER("Monster", Color.Red),
    REST("Rest", Color.Green),
    TREASURE("Treasure", Color.Yellow)
}

data class GameNode(
    val id: String, // Unique ID for each node
    val type: NodeType,
    val layer: Int, // Which level/floor this node is on
    var isCompleted: Boolean = false
)

enum class Element(val color: Color) {
    NONE(Color.Gray),
    FIRE(Color(0xFFFF4500)),
    WATER(Color(0xFF1E90FF)),
    EARTH(Color(0xFF8B4513)),
    WIND(Color(0xFF32CD32))
}

enum class Rarity(val color: Color, val multiplier: Float) {
    COMMON(Color.White, 1.0f),
    RARE(Color(0xFF0070DD), 1.5f),
    EPIC(Color(0xFFA335EE), 2.5f),
    LEGENDARY(Color(0xFFFF8000), 4.0f)
}

data class Item(
    val name: String,
    val type: ItemType,
    val value: Int,
    val rarity: Rarity = Rarity.COMMON,
    val element: Element = Element.NONE,
    val description: String
)

data class Player(
    val name: String,
    var hp: Int,
    var maxHp: Int,
    var level: Int = 1,
    var exp: Int = 0,
    var gold: Int = 0,
    val artifacts: MutableList<Item> = mutableListOf()
) {
    val attack: Int get() {
        val artifactAtk = artifacts.filter { it.type == ItemType.OFFENSE }.sumOf { it.value }
        return 5 + (level * 2) + artifactAtk
    }
    
    val defense: Int get() {
        val artifactDef = artifacts.filter { it.type == ItemType.DEFENSE }.sumOf { it.value }
        return (level) + artifactDef
    }

    val bonusMaxHp: Int get() = artifacts.filter { it.type == ItemType.UTILITY }.sumOf { it.value }
    val totalMaxHp: Int get() = maxHp + bonusMaxHp

    val nextLevelExp: Int get() = level * 100

    fun canLevelUp(): Boolean = exp >= nextLevelExp

    fun levelUp() {
        level++
        exp -= (level - 1) * 100
        maxHp += 20
        hp = totalMaxHp
    }
}

data class Monster(
    val name: String,
    val level: Int,
    var hp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val expReward: Int,
    val goldReward: Int,
    val element: Element = Element.NONE
)
