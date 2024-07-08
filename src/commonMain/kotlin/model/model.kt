package model

import dev.fritz2.core.Lenses
import logic.GameEndMessage
import kotlin.random.Random

// Put your model data classes in here to use it on js and jvm side

@Lenses
data class Framework(val name: String) {
    companion object
}

enum class CellState {
    EXPLODED, UNVISITED, VISITED, SAFE
}

data class Cell(
    val coordinates: Pair<Int, Int>,
    val hasMine: Boolean = false,
    var state: CellState = CellState.UNVISITED,
    var adjacents: Int? = null
) {

}

typealias Field = Map<Pair<Int, Int>, Cell>

fun generateMines(numMines: Int, gridWidth: Int, gridHeight: Int): Map<Pair<Int, Int>, Boolean> {
    val newMines = HashMap<Pair<Int, Int>, Boolean>()
    while (newMines.size < numMines) {
        val coordinates = Pair(Random.nextInt(0, gridWidth), Random.nextInt(0, gridHeight))
        newMines[coordinates] = true
    }
    return newMines
}

@Lenses
data class Config(
    val numMines: Int,
    val gridWidth: Int,
    val gridHeight: Int
) {
    companion object
}


data class GameState(
    val config: Config,
    val field: Field = buildMap {
        val mines = generateMines(config.numMines, config.gridWidth, config.gridHeight)
        (0..<config.gridWidth).map { x ->
            (0..<config.gridHeight).map { y ->
                val coords = Pair(x, y)
                put(
                    coords,
                    Cell(coords, hasMine = mines[coords] == true)
                )
            }
        }
    },
    val messages: List<GameEndMessage> = emptyList()
) {
    companion object {
        fun isFull(field: Field) = field.all { it.value.state != CellState.UNVISITED || it.value.hasMine }
    }

    fun hasEnded() = messages.isNotEmpty()
    fun minesLeft(): Int = field.values.sumOf { if (it.hasMine && it.state == CellState.UNVISITED) 1 as Int else 0 }
}