package tetris.logic;

import tetris.Board;
import tetris.Cell;

import static java.lang.Math.*;

public class Evaluator {
    private final ParameterWeights parameterWeight;

    public Evaluator(ParameterWeights parameterWeight) {
        this.parameterWeight = parameterWeight;
    }

    public EvaluationState getEvaluation(
            Board board,
            int score,
            int combo,
            double prevStateEval,
            int skipCnt,
            int linesCleared,
            boolean lost,
            int lastRound
    ) {
        Cell tSpinCell = checkTSpinPattern(board);
        int badCnt = 0;
        int semiBadCnt = 0;
        int w = board.getWidth();

        for (int col = 0; col < w; col++) {
            boolean found = false;
            for (int row = 0; row < board.getHeight(); row++) {
                if (board.get(row, col)) {
                    found = true;
                } else {
                    if (found) {
                        if (tSpinCell != null && tSpinCell.row == row && (tSpinCell.col == col - 1 || tSpinCell.col == col + 1)) {
                            continue;
                        }
                        if (isSemiBad(board, row, col)) {
                            semiBadCnt++;
                        } else {
                            badCnt++;
                        }
                    }
                }
            }
        }

        int flatRate = 0;
        for (int i = 0; i < w - 1; i++) {
            int diff = abs(board.getTopRowInColumn(i) - board.getTopRowInColumn(i + 1));
            flatRate += diff;
        }
        int holeCnt = 0;
        for (int i = 0; i < w; i++) {
            int left = i == 0 ? 999 : board.getColumnHeight(i - 1);
            int mid = board.getColumnHeight(i);
            int right = i == board.getWidth() - 1 ? 999 : board.getColumnHeight(i + 1);
            int holeHeight = min(left, right) - mid;
            if (holeHeight > 2) {
                holeCnt += (holeHeight + 1) / 4;
            }
        }
        int maxColumnHeight = 0;
        for (int i = 0; i < w; i++) {
            maxColumnHeight = max(maxColumnHeight, board.getColumnHeight(i));
        }
        boolean semiTSpinPattern = checkSemiTSpinPattern(board);
        boolean tSpinPattern = tSpinCell != null;

        int aboveBadFactor = calcAboveBadFactor(board);
        boolean lostLocal = lost || board.getMaxColumnHeight() == board.getHeight();
        int monotonicRate = calcMonotonicRate(board);
        int iPatternFactor = calcIPatternFactor(board);
        return new EvaluationState(
                badCnt,
                flatRate,
                holeCnt,
                maxColumnHeight,
                score,
                combo,
                aboveBadFactor,
                semiBadCnt,
                prevStateEval,
                skipCnt,
                linesCleared,
                monotonicRate,
                tSpinPattern,
                semiTSpinPattern,
                lostLocal,
                lastRound,
                iPatternFactor,
                parameterWeight
        );
    }

    private int calcIPatternFactor(Board board) {
        int minH = Integer.MAX_VALUE;
        for (int col = 0; col < board.getWidth(); col++) {
            int h = board.getColumnHeight(col);
            if (h < minH) {
                minH = h;
            }
        }
        int topRowInMinCol = board.getHeight() - minH;
        int row = topRowInMinCol - 1;
        int r = 0;
        while (row > 0 && board.getBlocksInRowCnt(row) == board.getWidth() - 1) {
            r++;
            row--;
        }
        return r;
    }

    private int calcMonotonicRate(Board board) {
        int r = 0;
        for (int col = 0; col <= board.getWidth() / 2 - 2; col++) {
            int h1 = board.getColumnHeight(col);
            int h2 = board.getColumnHeight(col + 1);
            if (h1 < h2) {
                r += h2 - h1;
            }
        }
        for (int col = board.getWidth() / 2; col <= board.getWidth() - 2; col++) {
            int h1 = board.getColumnHeight(col);
            int h2 = board.getColumnHeight(col + 1);
            if (h2 < h1) {
                r += h1 - h2;
            }
        }
        return r;
    }

    private int calcAboveBadFactor(Board board) {
        int[][] aboveBadFactor = new int[board.getHeight()][board.getWidth()];
        int r = 0;
        for (int row = 1; row < board.getHeight(); row++) {
            int rowEmptyMax = 0;
            for (int col = 0; col < board.getWidth(); col++) {
                if (!board.get(row, col)) { // empty
                    aboveBadFactor[row][col] = aboveBadFactor[row - 1][col];
                    r += aboveBadFactor[row][col];
                    rowEmptyMax = max(rowEmptyMax, aboveBadFactor[row][col]);
                }
            }
            for (int col = 0; col < board.getWidth(); col++) {
                if (board.get(row, col)) {
                    aboveBadFactor[row][col] = max(rowEmptyMax, aboveBadFactor[row - 1][col]) + 1;
                }
            }
        }
        return r;
    }

    private boolean checkSemiTSpinPattern(Board board) {
        for (int leftCol = 0; leftCol + 3 - 1 < board.getWidth(); leftCol++) {
            int leftTop = board.getTopRowInColumn(leftCol);
            if (leftTop < 3) {
                return false;
            }
            int midTop = board.getTopRowInColumn(leftCol + 1);
            int rightTop = board.getTopRowInColumn(leftCol + 2);
            if (leftTop == rightTop && leftTop < midTop) {
                if (existsBadInRow(board, leftTop) || existsBadInRow(board, leftTop - 1)) {
                    continue;
                }

                if (leftCol > 0) {
                    if (board.getTopRowInColumn(leftCol - 1) == leftTop - 1) {
                        return true;
                    }
                }
                if (leftCol + 3 - 1 < board.getWidth() - 1) {
                    if (board.getTopRowInColumn(leftCol + 3) == leftTop - 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean existsBadInRow(Board board, int row) {
        for (int col = 0; col < board.getWidth(); col++) {
            if (bad(board, row, col)) {
                return true;
            }
        }
        return false;
    }

    private boolean bad(Board board, int row, int col) {
        if (board.get(row, col)) {
            return false;
        }
        boolean leftWall;
        boolean rightWall;
        if (col == 0) {
            leftWall = true;
        } else {
            leftWall = board.getTopRowInColumn(col - 1) < row - 1;
        }
        if (col == board.getWidth() - 1) {
            rightWall = true;
        } else {
            rightWall = board.getTopRowInColumn(col + 1) < row - 1;
        }
        if (leftWall && rightWall) {
            return true;
        }
        for (int r = row - 1; r >= 0; r--) {
            if (board.get(r, col)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return center of TSpin pattern
     */
    private Cell checkTSpinPattern(Board board) {
        for (int leftCol = 0; leftCol + 3 - 1 < board.getWidth(); leftCol++) {
            int midTop = board.getTopRowInColumn(leftCol + 1);
            int leftTop = board.getTopRowInColumn(leftCol);
            if (checkTSpinPatternLeft(board, leftCol, midTop, leftTop)) {
                return new Cell(leftTop - 1, leftCol + 1);
            }
            int rightTop = board.getTopRowInColumn(leftCol + 2);
            if (checkTSpinPatternRight(board, leftCol, midTop, rightTop)) {
                return new Cell(rightTop - 1, leftCol + 1);
            }
        }
        return null;
    }

    private boolean checkTSpinPatternLeft(Board board, int leftCol, int midTop, int leftTop) {
        if (leftTop < 3) {
            return false;
        }
        return leftTop < midTop &&
                !board.get(leftTop - 1, leftCol + 2) &&
                board.get(leftTop - 2, leftCol + 2) &&
                board.getBlocksInRowCnt(leftTop - 1) == board.getWidth() - 3 &&
                board.getBlocksInRowCnt(leftTop) == board.getWidth() - 1;
    }

    private boolean checkTSpinPatternRight(Board board, int leftCol, int midTop, int rightTop) {
        if (rightTop < 3) {
            return false;
        }
        return rightTop < midTop &&
                !board.get(rightTop - 1, leftCol) &&
                board.get(rightTop - 2, leftCol) &&
                board.getBlocksInRowCnt(rightTop - 1) == board.getWidth() - 3 &&
                board.getBlocksInRowCnt(rightTop) == board.getWidth() - 1;
    }

    private boolean isSemiBad(Board board, int row, int col) {
        if (col >= 3 && board.getTopRowInColumn(col - 1) > row && board.getTopRowInColumn(col - 2) > row) {
            return true;
        }
        if (col <= board.getWidth() - 4 && board.getTopRowInColumn(col + 1) > row && board.getTopRowInColumn(col + 2) > row) {
            return true;
        }
        return false;
    }
}