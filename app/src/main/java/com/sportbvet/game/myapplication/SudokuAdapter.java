package com.sportbvet.game.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

// SudokuAdapter.java
public class SudokuAdapter extends BaseAdapter {
    private Context mContext;
    private int[][] mSudokuArray;

    int cellSize;

    public SudokuAdapter(Context context, int[][] sudokuArray, int cellSize) {
        mContext = context;
        mSudokuArray = sudokuArray;
        this.cellSize = cellSize;
    }

    @Override
    public int getCount() {
        return 81; // Assuming a 9x9 Sudoku grid
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {
            textView = new TextView(mContext);
            textView.setLayoutParams(new GridView.LayoutParams(cellSize, cellSize)); // Adjust size as needed
            textView.setBackground(mContext.getResources().getDrawable(R.drawable.border));
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(20.0f);
            textView.setGravity(Gravity.CENTER);
        } else {
            textView = (TextView) convertView;
        }

        int row = position / 9;
        int col = position % 9;

        int sudokuValue = mSudokuArray[row][col];
        textView.setText(sudokuValue + "");

        return textView;
    }
}

