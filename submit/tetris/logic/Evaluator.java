package tetris.logic;

import tetris.Board;

public class Evaluator {
    private final ParameterWeights parameterWeight;

    public Evaluator(ParameterWeights parameterWeight) {
        this.parameterWeight = parameterWeight;
    }

    public EvaluationState getEvaluation(Board board, int score, int combo, double prevStateEval, int skipCnt) {
        int badCnt = 0;
        int semiBadCnt = 0;
        int w = board.getWidth();

        int topBadRow = -1;
        int topBadCol = -1;

        for (int col = 0; col < w; col++) {
            boolean found = false;
            for (int row = 0; row < board.getHeight(); row++) {
                if (board.get(row, col)) {
                    found = true;
                } else {
                    if (found) {
                        if (isSemiBad(board, row, col)) {
                            semiBadCnt++;
                        } else {
                            badCnt++;
                            if (topBadRow == -1 || row < topBadRow) {
                                topBadRow = row;
                                topBadCol = col;
                            }
                        }
                    }
                }
            }
        }

        int cellsAboveTopBad = 0;
        if (topBadRow != -1) {
            for (int row = topBadRow - 1; row >= 0; row--) {
                if (board.get(row, topBadCol)) {
                    cellsAboveTopBad++;
                } else {
                    break;
                }
            }
        }

        int flatRate = 0;
        for (int i = 0; i < w - 1; i++) {
            int diff = Math.abs(board.getTopRowInColumn(i) - board.getTopRowInColumn(i + 1));
            flatRate += diff;
        }
        int holeCnt = 0;
        for (int i = 0; i < w; i++) {
            int left = i == 0 ? 999 : board.getColumnHeight(i - 1);
            int mid = board.getColumnHeight(i);
            int right = i == board.getWidth() - 1 ? 999 : board.getColumnHeight(i + 1);
            if (mid < Math.min(left, right) - 2) {
                holeCnt++;
            }
        }
        int maxColumnHeight = 0;
        for (int i = 0; i < w; i++) {
            maxColumnHeight = Math.max(maxColumnHeight, board.getColumnHeight(i));
        }
        boolean tSpinPattern = checkTSpinPattern(board);
        return new EvaluationState(
                badCnt,
                flatRate,
                holeCnt,
                maxColumnHeight,
                score,
                combo,
                cellsAboveTopBad,
                semiBadCnt,
                prevStateEval,
                skipCnt,
                tSpinPattern,
                false,
                parameterWeight
        );
    }

    private boolean checkTSpinPattern(Board board) {
        for (int leftCol = 0; leftCol + 3 - 1 < board.getWidth(); leftCol++) {
            int midTop = board.getTopRowInColumn(leftCol + 1);
            if (checkTSpinPatternLeft(board, leftCol, midTop)) {
                return true;
            }
            if (checkTSpinPatternRight(board, leftCol, midTop)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkTSpinPatternLeft(Board board, int leftCol, int midTop) {
        int leftTop = board.getTopRowInColumn(leftCol);
        if (leftTop < 3) {
            return false;
        }
        return leftTop < midTop &&
                !board.get(leftTop - 1, leftCol + 2) &&
                board.get(leftTop - 2, leftCol + 2) &&
                board.blocksInRowCnt(leftTop - 1) == board.getWidth() - 3 &&
                board.blocksInRowCnt(leftTop) == board.getWidth() - 1;
    }

    private boolean checkTSpinPatternRight(Board board, int leftCol, int midTop) {
        int rightTop = board.getTopRowInColumn(leftCol + 2);
        if (rightTop < 3) {
            return false;
        }
        return rightTop < midTop &&
                !board.get(rightTop - 1, leftCol) &&
                board.get(rightTop - 2, leftCol) &&
                board.blocksInRowCnt(rightTop - 1) == board.getWidth() - 3 &&
                board.blocksInRowCnt(rightTop) == board.getWidth() - 1;
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