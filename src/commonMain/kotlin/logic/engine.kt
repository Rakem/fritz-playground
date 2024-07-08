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
        for (x in -1..1)
            for (y in -1..1) {
                if (field[Pair(
                        cell.coordinates.first + x,
                        cell.coordinates.second + y
                    )]?.hasMine == true
                ) adjacency += 1
            }

        return adjacency
    }

    fun visitCell(field: Field, move: Cell?): Field {
        if (move == null || move.state == CellState.VISITED) return field
        var newField = field.toMap()
        newField[move.coordinates]?.let {
            val adjacents = calculateAdjacency(newField, move)
            var newState: CellState = move.state
            if (move.state == CellState.UNVISITED)
                newState = if (move.hasMine) {
                    CellState.EXPLODED
                } else {
                    CellState.VISITED
                }
            newField = newField + mapOf(move.coordinates to it.copy(state = newState, adjacents = adjacents))
            if (newState == CellState.VISITED && adjacents == 0) {
                for (x in -1..1)
                    for (y in -1..1) {
                        newField = visitCell(
                            newField,
                            field[Pair(
                                move.coordinates.first + x,
                                move.coordinates.second + y
                            )]
                        )
                    }

            }
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