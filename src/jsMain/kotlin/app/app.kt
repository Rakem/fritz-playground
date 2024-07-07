package app

import dev.fritz2.core.RootStore
import dev.fritz2.core.lensOf
import dev.fritz2.core.render
import dev.fritz2.core.storeOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import logic.Engine
import model.Cell
import model.CellState
import model.Framework
import model.GameState
import kotlin.random.Random

class GameStore(private val engine: Engine, initialState: GameState) : RootStore<GameState>(initialState, job = Job()) {

    val field = data.map { it.field }
    val check = handle<Cell> { state, move ->
        engine.next(state, move)
    }
    val minesLeft = data.map { it.minesLeft() }
}

fun main() {


    val numMines = 10
    val gridWidth = 10
    val gridHeight = 10

    val gameStore = GameStore(Engine(), GameState(numMines, gridWidth, gridHeight))

    render {
        // card
        div("mx-auto sm:px-6 lg:px-8 py-12") {
            div("shadow-md flex flex-col") {

                // card-header
                div("px-6 py-4 text-white bg-sky-700 border-b border-gray-200 font-bold uppercase") {
                    +"Minesweeper"
                }
                div("px-6 py-4") {
                    gameStore.minesLeft.render { minesLeft ->
                        span { +"Mines left $minesLeft" }
                    }

                }
                gameStore.data.render {
                    if (it.hasEnded()) {
                        val message = it.messages.first()
                        div("alert") {
                            +message.text
                            className(message.type)
                            attr("role", "alert")
                        }
                    }
                }
                button {
                    +"Reset"
                    clicks handledBy { gameStore.update(GameState(numMines, gridWidth, gridHeight)) }
                }
                div("flex flex-col  bg-gray-100 p-8") {
                    gameStore.field.render { values ->
                        for (x in 0..<gridHeight) {
                            div("flex flex-row") {
                                for (y in 0..<gridWidth) {
                                    var cell = values[Pair(x, y)] ?: continue
                                    var color = "bg-white"
                                    if (cell.state == CellState.VISITED) {
                                        color = "bg-gray-50"
                                    }
                                    button("w-10 h-10 shadow-sm $color") {
                                        when (cell.state) {
                                            CellState.EXPLODED -> +"x"
                                            CellState.UNVISITED -> +""
                                            CellState.VISITED -> cell.adjacents?.let { if (it > 0) +it.toString() }
                                            CellState.SAFE -> +"_"
                                        }
                                        clicks.map { cell.copy() } handledBy gameStore.check
                                    }
                                }
                            }
                        }
                        values.forEach { cell ->

                        }
                    }
                }
            }
        }
    }
}

typealias MineStore = Map<Pair<Int, Int>, Boolean>