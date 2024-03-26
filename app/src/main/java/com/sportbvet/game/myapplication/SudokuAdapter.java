package com.sportbvet.game.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;

// SudokuAdapter.java
public class SudokuAdapter extends BaseAdapter {
    private final Context mContext;

    ArrayList<SudokuBoard> sudokuArray = new ArrayList<>();

    public SudokuAdapter(Context context, ArrayList<SudokuBoard> sudokuArray) {
        mContext = context;
        this.sudokuArray = sudokuArray;
    }

    @Override
    public int getCount() {
        return sudokuArray.size(); // Assuming a 9x9 Sudoku grid
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {
            textView = new TextView(mContext);
            textView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)); // Adjust size as needed
            textView.setTextSize(20.0f);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(8, 8, 8, 8);
        } else {
            textView = (TextView) convertView;
        }

        int row = position / 9;
        int col = position % 9;

        SudokuBoard sudokuBoard = sudokuArray.get(position);
        if (sudokuBoard.row == row && sudokuBoard.col == col) {
            textView.setText(sudokuBoard.value + "");
            if (sudokuBoard.isFilled) {
                textView.setBackground(mContext.getResources().getDrawable(R.drawable.cell_fiiled,null));
                textView.setTextColor(Color.WHITE);
            } else {
                textView.setBackground(mContext.getResources().getDrawable(R.drawable.cell,null));
                textView.setTextColor(Color.BLACK);
            }
        }


        return textView;
    }
}

