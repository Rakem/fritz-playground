package logic

import dev.fritz2.validation.Validation
import dev.fritz2.validation.ValidationMessage
import dev.fritz2.validation.validation
import model.Cell
import model.CellState
import model.Field
import model.GameState

class GameEndMessage(override val path: String, val text: String, val type: String) : ValidationMessage {
    override val isError: Boolean = false
}

val neighbourhood =
    arrayOf(
        Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
        Pair(0, -1), /*exclude origin, */ Pair(0, 1),
        Pair(1, -1), Pair(1, 0), Pair(1, 1)
    )

class Engine {
    companion object {
        private val endingValidator: Validation<Field, GameState, GameEndMessage> = validation { inspector, _ ->
            if (inspector.data.any { it.value.state == CellState.EXPLODED }) {
                add(GameEndMessage(inspector.path, "You have exploded!", "alert-info"))
            } else if (GameState.isFull(inspector.data)) {
                add(GameEndMessage(inspector.path, "You have survived!", "alert-success"))
            }
        }

    }

    private fun getNeighbors(field: Field, cell: Cell): List<Cell> {
        return neighbourhood.mapNotNull {
            field[Pair(
                cell.coordinates.first + it.first,
                cell.coordinates.second + it.second
            )]
        }
    }

    private fun calculateAdjacency(field: Field, cell: Cell): Int {
        return getNeighbors(field, cell).sumOf { if (it.hasMine) 1 else 0 as Int }
    }


    private fun visitCell(field: Field, move: Cell?): Field {
        if (move == null || move.state == CellState.VISITED) return field


        val adjacents = calculateAdjacency(field, move)
        var newState: CellState = move.state
        if (move.state == CellState.UNVISITED)
            newState = if (move.hasMine) {
                CellState.EXPLODED
            } else {
                CellState.VISITED
            }
        var newField = field + mapOf(move.coordinates to move.copy(state = newState, adjacents = adjacents))
        if (newState == CellState.VISITED && adjacents == 0) {
            newField = getNeighbors(field, move).fold(newField) { f, neighbor -> visitCell(f, neighbor) }
        }

        return newField
    }

    fun next(state: GameState, move: Cell): GameState {
        if (!state.hasEnded()) {
            val newField = visitCell(state.field, move)
            val messages = endingValidator(newField, state)
            return state.copy(field = newField, messages = messages)
        } else {
            return state
        }
    }
}