package com.sportbvet.game.myapplication;

import static org.opencv.android.Utils.matToBitmap;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.RETR_TREE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.arcLength;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.getPerspectiveTransform;
import static org.opencv.imgproc.Imgproc.warpPerspective;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
import org.opencv.objdetect.Board;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SecondActivity extends AppCompatActivity {

    Button button;
    ImageView image;

    private double BOARD_SIZE_IN_PX = 9 * 111.0;
    int[][] sudokuBoardValues = new int[9][9];

    TextRecognizer textRecognizer;

    SudokuSolver sudokuSolver;

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

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Image picked successfully
            Uri imageUri = data.getData();
            try {
                Bitmap b = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                Size size = new Size(b.getWidth(),b.getHeight());

                // grayscaleImage
                Mat tmp = new Mat (size, CvType.CV_8UC1);
                Utils.bitmapToMat(b, tmp);
                Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
                matToBitmap(tmp, b);

                // image process for detect board
                GaussianBlur(tmp, tmp, new Size(11.0, 11.0), 0.0);
                adaptiveThreshold(tmp, tmp, 255.0, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY_INV, 13, 2.0);

                Mat hierarchy = new Mat();
                ArrayList<MatOfPoint> matpoints = new ArrayList<>();
                findContours(tmp,matpoints,hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
                MatOfPoint largestContour = getLargestContour(matpoints);
                List<Point> largestContourCornerList = getLargestContourCornerList(largestContour);
                Mat perspectiveMat = createPerspectiveMat(tmp, largestContourCornerList);

                Bitmap boardImage = Bitmap.createBitmap(
                        perspectiveMat.cols(),
                        perspectiveMat.rows(),
                        Bitmap.Config.ARGB_8888
                );

                matToBitmap(perspectiveMat, boardImage);


                // extract sudoku

                int sudokuBoardValuesSize = 9 * 9;

                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        sudokuBoardValues[i][j] = 0;
                    }
                }

                AtomicInteger processedCellCount = new AtomicInteger(0);
                List<List<Bitmap>> cellImageList = generateCellImageList(boardImage);

                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        int finalI = i;
                        int finalJ = j;
                        InputImage inputImage = InputImage.fromBitmap(cellImageList.get(finalI).get(finalJ),0);
                        textRecognizer.process(inputImage)
                                .addOnSuccessListener(new OnSuccessListener<Text>() {
                                    @Override
                                    public void onSuccess(Text result) {
                                        int value = parseTextToInt(result.getText());
                                        if (value != Integer.MIN_VALUE) {
                                            sudokuBoardValues[finalI][finalJ] = value;
                                        }

                                        if (finalI == 8 && finalJ == 8) {
                                            boolean result1 = sudokuSolver.solveSudoku(sudokuBoardValues);

                                            if (result1) {
                                                System.out.println("Solved");
                                                printBoard();
                                            } else {
                                                System.out.println("Not solved");
                                            }
                                        }
                                    }
                                });
                    }
                }






                image.setImageBitmap(boardImage);

            } catch (IOException e) {
                e.printStackTrace();
            }
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
                System.out.print(sudokuBoardValues[i][j] + " ");
            }
            System.out.println();
        }
    }

    private int parseTextToInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    public static List<List<Bitmap>> generateCellImageList(Bitmap boardImage) {
        int cellSize = boardImage.getWidth() / 9;

        List<List<Bitmap>> cellImageList = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            List<Bitmap> row = new ArrayList<>();
            for (int j = 0; j < 9; j++) {
                row.add(Bitmap.createBitmap(boardImage, cellSize * j, cellSize * i, cellSize, cellSize));
            }
            cellImageList.add(row);
        }

        return cellImageList;
    }

    @NonNull
    private static MatOfPoint getLargestContour(ArrayList<MatOfPoint> matPoints) {
        MatOfPoint largestContour = matPoints.stream().max(new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                return Double.compare(Imgproc.contourArea(o1),Imgproc.contourArea(o2));
            }
        }).get();
        return largestContour;
    }

    private static List<Point> getLargestContourCornerList(MatOfPoint largestContour) {
        List<Point> largestContourCornerList = new ArrayList<>();

        MatOfPoint2f src = new MatOfPoint2f();
        MatOfPoint2f approxPoly = new MatOfPoint2f();
        largestContour.convertTo(src, CvType.CV_32FC2);
        double arcLength = arcLength(src, true);
        approxPolyDP(src, approxPoly, 0.02 * arcLength, true);
        largestContourCornerList = approxPoly.toList();
        return largestContourCornerList;
    }

    private Mat createPerspectiveMat(Mat originalMat,List<Point> largestContourCornerList) {
        Mat perspectiveMat = new Mat();
        Mat perspectiveSrc =  new MatOfPoint2f(getTopLeft(largestContourCornerList), getTopRight(largestContourCornerList),
                getBottomLeft(largestContourCornerList), getBottomRight(largestContourCornerList));
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
        Collections.sort(sortedPoints, Comparator.comparing(Point::getX));
        return sortedPoints.subList(0, 2).stream().min(Comparator.comparing(Point::getY)).orElse(null);
    }

    public static Point getBottomLeft(List<Point> points) {
        List<Point> sortedPoints = new ArrayList<>(points);
        Collections.sort(sortedPoints, Comparator.comparing(Point::getX));
        return sortedPoints.subList(0, 2).stream().max(Comparator.comparing(Point::getY)).orElse(null);
    }

    public static Point getTopRight(List<Point> points) {
        List<Point> sortedPoints = new ArrayList<>(points);
        Collections.sort(sortedPoints, Comparator.comparing(Point::getX).reversed());
        return sortedPoints.subList(0, 2).stream().min(Comparator.comparing(Point::getY)).orElse(null);
    }

    public static Point getBottomRight(List<Point> points) {
        List<Point> sortedPoints = new ArrayList<>(points);
        Collections.sort(sortedPoints, Comparator.comparing(Point::getX).reversed());
        return sortedPoints.subList(0, 2).stream().max(Comparator.comparing(Point::getY)).orElse(null);
    }
}