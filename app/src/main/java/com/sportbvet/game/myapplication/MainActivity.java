package com.sportbvet.game.myapplication;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    SudokuSolver sudokuSolver;
    int[][] sudokuBoard = {
            {0,0,6,1,0,2,5,0,0},
            {0,3,9,0,0,0,1,4,0},
            {0,0,0,0,4,0,0,0,0},
            {9,0,2,0,3,0,4,0,1},
            {0,8,0,0,0,0,0,7,0},
            {1,0,3,0,6,0,8,0,9},
            {0,0,0,0,1,0,0,0,0},
            {0,5,4,0,0,0,9,1,0},
            {0,0,7,5,0,3,2,0,0}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sudokuSolver = new SudokuSolver();

        printBoard();

        boolean result = sudokuSolver.solveSudoku(sudokuBoard);

        if (result) {
            System.out.println("Solved");
            printBoard();
        } else {
            System.out.println("Not solved");
        }
    }

    private void printBoard() {
        // Print the Sudoku board
        for (int i = 0; i < 9; i++) {
            if (i % 3 == 0 && i != 0) {
                System.out.println("  ");
            }
            for (int j = 0; j < 9; j++) {
                if (j % 3 == 0 && j != 0) {
                    System.out.print("  ");
                }
                System.out.print(sudokuBoard[i][j] + " ");
            }
            System.out.println();
        }
    }
}