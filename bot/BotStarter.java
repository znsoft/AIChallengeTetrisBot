// Copyright 2015 theaigames.com (developers@theaigames.com)

//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at

//        http://www.apache.org/licenses/LICENSE-2.0

//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//	
//    For the full copyright and license information, please view the LICENSE
//    file that was distributed with this source code.

package bot;

import field.CellType;
import field.Field;
import field.ShapeType;
import moves.MoveType;
import tetris.*;
import tetris.logic.BestMoveFinder;

import java.util.ArrayList;
import java.util.List;

/**
 * BotStarter class
 * <p>
 * This class is where the main logic should be. Implement getMoves() to
 * return something better than random moves.
 *
 * @author Jim van Eeden <jim@starapple.nl>
 */

public class BotStarter {

    private final BestMoveFinder bestMoveFinder = new BestMoveFinder();

    public static void main(String[] args) {
        BotParser parser = new BotParser(new BotStarter());
        parser.run();
    }

    public ArrayList<MoveType> getMoves(BotState state, long timeout) {
        GameState gameState = getGameState(state);
        List<Move> moves = bestMoveFinder.findBestMoves(gameState);

        System.err.println("Round = " + state.getRound());
        System.err.println(gameState.getFallingTetrimino());
        System.err.println(gameState.getBoard());
        System.err.println(moves);
        System.err.println("-----------------------");

        ArrayList<MoveType> res = new ArrayList<>();

        for (Move move : moves) {
            res.add(convertMove(move));
        }

        return res;
    }

    private MoveType convertMove(Move move) {
        switch (move) {
            case LEFT:
                return MoveType.LEFT;
            case RIGHT:
                return MoveType.RIGHT;
            case DROP:
                return MoveType.DROP;
            case ROTATE_CW:
                return MoveType.TURNRIGHT;
            case ROTATE_CCW:
                return MoveType.TURNLEFT;
            default:
                throw new RuntimeException();
        }
    }

    private GameState getGameState(BotState state) {
        Field field = state.getMyField();
        Board board = new Board(field.getWidth(), field.getHeight());
        for (int i = 0; i < field.getHeight(); i++) {
            for (int j = 0; j < field.getWidth(); j++) {
                CellType cellType = field.getCell(j, i).getState();
                board.set(i, j, cellType == CellType.SOLID || cellType == CellType.BLOCK);
                if (cellType == CellType.SOLID && board.getPenalty() == 0) {
                    board.setPenalty(board.getHeight() - i);
                }
            }
        }
        TetriminoWithPosition fallingTetrimino = new TetriminoWithPosition(
                state.getShapeLocation().y,
                state.getShapeLocation().x,
                convertTetrimino(state.getCurrentShape())
        );
        return new GameState(
                board,
                fallingTetrimino,
                convertTetrimino(state.getNextShape()),
                state.getMyBot().getCombo()
        );
    }

    private Tetrimino convertTetrimino(ShapeType shape) {
        switch (shape) {
            case I:
                return Tetrimino.I;
            case J:
                return Tetrimino.J;
            case L:
                return Tetrimino.L;
            case O:
                return Tetrimino.O;
            case S:
                return Tetrimino.S;
            case T:
                return Tetrimino.T;
            case Z:
                return Tetrimino.Z;
            default:
                throw new RuntimeException("None tetrimino?");
        }
    }
}
