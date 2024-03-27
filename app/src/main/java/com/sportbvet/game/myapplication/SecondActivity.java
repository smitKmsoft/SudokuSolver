package com.sportbvet.game.myapplication;

import static org.opencv.android.Utils.matToBitmap;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.RETR_TREE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.arcLength;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.getPerspectiveTransform;
import static org.opencv.imgproc.Imgproc.warpPerspective;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecondActivity extends AppCompatActivity {

    Button button;
    ImageView image;

    TextView question, answer;
    TextRecognizer textRecognizer;

    SudokuSolver sudokuSolver;
    GridView gridView;
    Bitmap boardImage;
    ArrayList<SudokuBoard> sudokuBoards = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        sudokuSolver = new SudokuSolver();

        button = findViewById(R.id.button);
        image = findViewById(R.id.image);
        question = findViewById(R.id.question);
        answer = findViewById(R.id.answer);
        gridView = findViewById(R.id.gridView);

        button.setOnClickListener(v -> {
            question.setVisibility(View.GONE);
            image.setVisibility(View.GONE);
            answer.setVisibility(View.GONE);
            gridView.setVisibility(View.GONE);
            sudokuBoards.clear();
            sudokuBoards = new ArrayList<>();

            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, 1);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Image picked successfully
            assert data != null;
            Uri imageUri = data.getData();
            try {
                Bitmap b = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                Mat tmp = new Mat(b.getHeight(), b.getWidth(), CvType.CV_8UC1);
                Utils.bitmapToMat(b, tmp);

                Mat processingMat = new Mat(b.getHeight(), b.getWidth(), CvType.CV_8UC1);

                prepareProcessingMat(tmp, processingMat);
                MatOfPoint largestContour = getLargestContour(processingMat);
                List<Point> largestContourCornerList = getLargestContourCornerList(largestContour);
                Mat perspectiveMat = createPerspectiveMat(tmp, largestContourCornerList);
                boardImage = createBoardImage(perspectiveMat);
                b.recycle();

                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        SudokuBoard sudokuBoard = new SudokuBoard();
                        sudokuBoard.isFilled = false;
                        sudokuBoard.value = 0;
                        sudokuBoard.row = i;
                        sudokuBoard.col = j;
                        sudokuBoards.add(sudokuBoard);
                    }
                }

                InputImage inputImage = InputImage.fromBitmap(boardImage, 0);

                textRecognizer.process(inputImage)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text result) {
                                List<Text.TextBlock> blocks = new ArrayList<>(result.getTextBlocks());

                                for (Text.TextBlock block : blocks) {
                                    for (Text.Line line : block.getLines()) {
                                        for (Text.Element element : line.getElements()) {
                                            for (Text.Symbol symbol : element.getSymbols()) {
                                                Rect rect = symbol.getBoundingBox();
                                                String symbolText = filterNumericCharacters(symbol.getText());

                                                int cellSize = boardImage.getWidth() / 9;

                                                assert rect != null;
                                                int column = (rect.left - 1) / cellSize; // Subtract 1 to ensure zero-based indexing

                                                int row = (rect.top - 1) / cellSize; // Subtract 1 to ensure zero-based indexing

                                                int value = 0;
                                                value = parseTextToInt(symbolText);
                                                if (value != Integer.MIN_VALUE) {

                                                    for (int i = 0; i < sudokuBoards.size(); i++) {
                                                        if (sudokuBoards.get(i).row == row && sudokuBoards.get(i).col == column) {
                                                            sudokuBoards.get(i).isFilled = true;
                                                            sudokuBoards.get(i).value = value;
                                                            break;
                                                        }
                                                    }
                                                } else {
                                                    value = 0;
                                                }
                                            }
                                        }
                                    }
                                }

                                printBoard();

                                boolean result1 = sudokuSolver.getBoard(sudokuBoards);
                                if (result1) {
                                    printBoard();

                                } else {
                                    System.out.println("Not solved");
                                }

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(SecondActivity.this, "" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });


                Bitmap resizedBitmap = Bitmap.createScaledBitmap(boardImage, boardImage.getWidth()*2, boardImage.getHeight()*2, true);
                image.setImageBitmap(resizedBitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String filterNumericCharacters(@NonNull String text) {
        return text.replaceAll("[^0-9]", "");
    }

    @NonNull
    private static Bitmap createBoardImage(@NonNull Mat perspectiveMat) {
        Bitmap boardImage = Bitmap.createBitmap(
                perspectiveMat.cols(),
                perspectiveMat.rows(),
                Bitmap.Config.ARGB_8888
        );

        matToBitmap(perspectiveMat, boardImage);
        return boardImage;
    }

    private static void prepareProcessingMat(Mat tmp, Mat processingMat) {
        cvtColor(tmp, processingMat, COLOR_RGB2GRAY);
//        GaussianBlur(processingMat, processingMat, new Size(11.0, 11.0), 0.0);
        adaptiveThreshold(processingMat, processingMat, 255.0, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY_INV, 13, 2.0);
    }

    private void printBoard() {
        // Print the Sudoku board
//        for (int i = 0; i < 9; i++) {
//            if (i % 3 == 0 && i != 0) {
//                System.out.println("  ");
//            }
//            for (int j = 0; j < 9; j++) {
//                if (j % 3 == 0 && j != 0) {
//                    System.out.print("  ");
//                }
//                System.out.print(sudokuBoardValues[i][j] + " ");
//            }
//            System.out.println();
//        }

        for (int i = 0; i < sudokuBoards.size(); i++) {
            if (!sudokuBoards.get(i).isFilled) {
                sudokuBoards.get(i).value = sudokuSolver.board2[sudokuBoards.get(i).row][sudokuBoards.get(i).col];
            }
        }

        question.setVisibility(View.VISIBLE);
        image.setVisibility(View.VISIBLE);
        answer.setVisibility(View.VISIBLE);
        gridView.setVisibility(View.VISIBLE);

        SudokuAdapter adapter = new SudokuAdapter(this, sudokuBoards);
        gridView.setAdapter(adapter);
    }

    private int parseTextToInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    @NonNull
    private static MatOfPoint getLargestContour(Mat processingMat) {

        Mat hierarchy = new Mat();
        ArrayList<MatOfPoint> matpoints = new ArrayList<>();
        findContours(processingMat, matpoints, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);

        MatOfPoint largestContour = matpoints.stream().max(new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                return Double.compare(Imgproc.contourArea(o1), Imgproc.contourArea(o2));
            }
        }).get();
        return largestContour;
    }

    private static List<Point> getLargestContourCornerList(@NonNull MatOfPoint largestContour) {
        List<Point> largestContourCornerList = new ArrayList<>();

        MatOfPoint2f src = new MatOfPoint2f();
        MatOfPoint2f approxPoly = new MatOfPoint2f();
        largestContour.convertTo(src, CvType.CV_32FC2);
        double arcLength = arcLength(src, true);
        approxPolyDP(src, approxPoly, 0.02 * arcLength, true);
        largestContourCornerList = approxPoly.toList();
        return largestContourCornerList;
    }

    @NonNull
    private Mat createPerspectiveMat(Mat originalMat, List<Point> largestContourCornerList) {
        Mat perspectiveMat = new Mat();
        Mat perspectiveSrc = new MatOfPoint2f(getTopLeft(largestContourCornerList), getTopRight(largestContourCornerList),
                getBottomLeft(largestContourCornerList), getBottomRight(largestContourCornerList));
//        double BOARD_SIZE_IN_PX = 9 * 111.0;
        double BOARD_SIZE_IN_PX = 9 * 24.0;
        Mat perspectiveDst = new MatOfPoint2f(new Point(0.0, 0.0), new Point(BOARD_SIZE_IN_PX, 0.0), new Point(0.0, BOARD_SIZE_IN_PX), new Point(BOARD_SIZE_IN_PX, BOARD_SIZE_IN_PX));
        Mat perspectiveTransform = getPerspectiveTransform(perspectiveSrc, perspectiveDst);

        warpPerspective(
                originalMat,
                perspectiveMat,
                perspectiveTransform,
                new Size(BOARD_SIZE_IN_PX, BOARD_SIZE_IN_PX)
        );

        return perspectiveMat;
    }

    public static Point getTopLeft(List<Point> points) {
        List<Point> sortedPoints = new ArrayList<>(points);
        sortedPoints.sort(Comparator.comparing(Point::getX));
        return sortedPoints.subList(0, 2).stream().min(Comparator.comparing(Point::getY)).orElse(null);
    }

    public static Point getBottomLeft(List<Point> points) {
        List<Point> sortedPoints = new ArrayList<>(points);
        sortedPoints.sort(Comparator.comparing(Point::getX));
        return sortedPoints.subList(0, 2).stream().max(Comparator.comparing(Point::getY)).orElse(null);
    }

    public static Point getTopRight(List<Point> points) {
        List<Point> sortedPoints = new ArrayList<>(points);
        sortedPoints.sort(Comparator.comparing(Point::getX).reversed());
        return sortedPoints.subList(0, 2).stream().min(Comparator.comparing(Point::getY)).orElse(null);
    }

    public static Point getBottomRight(List<Point> points) {
        List<Point> sortedPoints = new ArrayList<>(points);
        sortedPoints.sort(Comparator.comparing(Point::getX).reversed());
        return sortedPoints.subList(0, 2).stream().max(Comparator.comparing(Point::getY)).orElse(null);
    }
}