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

class Engine {
    companion object {
        private val endingValidator: Validation<Field, GameState, GameEndMessage> = validation { inspector, gameState ->
            if (inspector.data.any { it.value.state == CellState.EXPLODED }) {
                add(GameEndMessage(inspector.path, "You have exploded!", "alert-info"))
            } else if (GameState.isFull(inspector.data)) {
                add(GameEndMessage(inspector.path, "You have survived!", "alert-success"))
            }
        }

    }

    fun calculateAdjacency(field: Field, cell: Cell): Int {
        var adjacency = 0
        if (field[Pair(cell.coordinates.first - 1, cell.coordinates.second)]?.hasMine == true) adjacency += 1
        if (field[Pair(cell.coordinates.first + 1, cell.coordinates.second)]?.hasMine == true) adjacency += 1
        if (field[Pair(cell.coordinates.first, cell.coordinates.second - 1)]?.hasMine == true) adjacency += 1
        if (field[Pair(cell.coordinates.first, cell.coordinates.second + 1)]?.hasMine == true) adjacency += 1
        return adjacency
    }

    fun visitCell(field: Field, move: Cell?): Field {
        println(move)
        if (move == null || move?.state == CellState.VISITED) return field
        var newField = field.toMap()
        newField[move.coordinates]?.let {
            val adjacents = calculateAdjacency(newField, move)
            val newState: CellState
            if (move.hasMine) {
                newState = CellState.EXPLODED
            } else {
                newState = CellState.VISITED
            }
            newField = newField + mapOf(move.coordinates to it.copy(state = newState, adjacents = adjacents))
            if (newState == CellState.VISITED && adjacents == 0) {
                newField = visitCell(newField, newField[Pair(move.coordinates.first - 1, move.coordinates.second)])
                newField = visitCell(newField, newField[Pair(move.coordinates.first + 1, move.coordinates.second)])
                newField = visitCell(newField, newField[Pair(move.coordinates.first, move.coordinates.second - 1)])
                newField = visitCell(newField, newField[Pair(move.coordinates.first, move.coordinates.second + 1)])
            }
        }
        return newField
    }

    fun next(state: GameState, move: Cell): GameState {
        if (!state.hasEnded() && state.field[move.coordinates]?.state == CellState.UNVISITED) {
            val newField = visitCell(state.field, move)
            val messages = endingValidator(newField, state)
            return state.copy(field = newField, messages = messages)
        } else {
            return state
        }
    }
}