package com.example.myapplication;
public class SudokuSolver {

    private static final int EMPTY = 0;
    private static final int SIZE = 9;

    public boolean solveSudoku(int[][] board) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] == EMPTY) {
                    for (int value = 1; value <= SIZE; value++) {
                        if (isValidMove(board, row, col, value)) {
                            board[row][col] = value;
                            if (solveSudoku(board)) {
                                return true;
                            } else {
                                board[row][col] = EMPTY;
                            }
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidMove(int[][] board, int row, int col, int value) {
        // Check row and column
        for (int i = 0; i < SIZE; i++) {
            if (board[row][i] == value || board[i][col] == value) {
                return false;
            }
        }

        // Check 3x3 subgrid
        int boxRow = row / 3 * 3;
        int boxCol = col / 3 * 3;
        for (int i = boxRow; i < boxRow + 3; i++) {
            for (int j = boxCol; j < boxCol + 3; j++) {
                if (board[i][j] == value) {
                    return false;
                }
            }
        }

        return true;
    }
}
