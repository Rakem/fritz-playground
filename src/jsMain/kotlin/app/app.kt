package app

import dev.fritz2.core.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import logic.Engine
import model.*

class GameStore(private val engine: Engine, initialState: GameState) : RootStore<GameState>(initialState, job = Job()) {

    val field = data.map { it.field }
    val check = handle<Cell> { state, move ->
        engine.next(state, move)
    }
    val config = data.map { it.config }
}

fun main() {


    val configStore = storeOf(Config(10, 10, 10), job = Job())
    val numMines = configStore.map(
        lensOf(
            "numMines",
            { it.numMines.toString() },
            { config, value -> config.copy(numMines = value.toInt()) })
    )
    val gridWidth = configStore.map(
        lensOf(
            "numMines",
            { it.gridWidth.toString() },
            { config, value -> config.copy(gridWidth = value.toInt()) })
    )
    val gridHeight = configStore.map(
        lensOf(
            "numMines",
            { it.gridHeight.toString() },
            { config, value -> config.copy(gridHeight = value.toInt()) })
    )

    val gameStore = GameStore(
        Engine(),
        GameState(configStore.current)
    )
    render {
        // card
        div("mx-auto sm:px-6 lg:px-8 py-12") {
            div("shadow-md flex flex-col") {

                // card-header
                div("px-6 py-4 text-white bg-sky-700 border-b border-gray-200 font-bold uppercase") {
                    +"Minesweeper"
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
                label { +"Mines" }
                input {
                    type("number")
                    value(numMines.data)
                    changes.values() handledBy numMines.update
                }
                label { +"Width" }
                input {
                    type("number")
                    value(gridWidth.data)
                    changes.values() handledBy gridWidth.update
                }
                label { +"Height" }
                input {
                    type("number")
                    value(gridHeight.data)
                    changes.values() handledBy gridHeight.update
                }
                button {
                    +"Reset"
                    clicks handledBy {
                        gameStore.update(
                            GameState(
                                configStore.current
                            )
                        )
                    }
                }
                div("flex flex-col  bg-gray-100 p-8") {

                    gameStore.field.render { values ->
                        val gridWidth = gameStore.current.config.gridWidth
                        val gridHeight = gameStore.current.config.gridHeight
                        for (x in 0..<gridWidth) {
                            div("flex flex-row") {
                                for (y in 0..<gridHeight) {
                                    val cell = values[Pair(x, y)] ?: continue
                                    var color = "bg-white"
                                    if (cell.state == CellState.VISITED) {
                                        color = "bg-gray-200"
                                    }
                                    button("w-10 h-10 shadow-sm $color") {
                                        when (cell.state) {
                                            CellState.EXPLODED -> +"x"
                                            CellState.UNVISITED -> +""
                                            CellState.VISITED -> cell.adjacents?.let { if (it > 0) +it.toString() }
                                            CellState.SAFE -> +"?"
                                        }
                                        clicks.map { cell.copy() } handledBy gameStore.check
                                        contextmenus { preventDefault();stopPropagation() }.map {
                                            cell.copy(state = if (cell.state == CellState.UNVISITED) CellState.SAFE else CellState.UNVISITED)
                                        } handledBy gameStore.check
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
