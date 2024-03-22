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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.audiofx.DynamicsProcessing;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
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
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.Board;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SecondActivity extends AppCompatActivity {

    Button button;
    ImageView image;

    private double BOARD_SIZE_IN_PX = 9 * 111.0;
    int[][] sudokuBoardValues = new int[9][9];

    TextRecognizer textRecognizer;

    SudokuSolver sudokuSolver;

    private static final int[][] NUMBER_COLORS = {
            {0x00, 0x28, 0x27, 0x43, 0x00, 0x36}, // 0
            {0x2f, 0x4f, 0x57, 0x4f, 0x2f, 0x4f}, // 1
            {0x00, 0x37, 0x55, 0x55, 0x55, 0x55}, // 2
            {0x00, 0x37, 0x55, 0x55, 0x55, 0x00}, // 3
            {0x07, 0x55, 0x55, 0x55, 0x37, 0x00}, // 4
            {0x00, 0x55, 0x55, 0x55, 0x37, 0x00}, // 5
            {0x00, 0x55, 0x55, 0x55, 0x55, 0x00}, // 6
            {0x00, 0x37, 0x55, 0x4f, 0x2f, 0x4f}, // 7
            {0x00, 0x55, 0x55, 0x55, 0x55, 0x55}, // 8
            {0x00, 0x55, 0x55, 0x55, 0x37, 0x00}  // 9
    };

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

                Mat tmp = new Mat(b.getHeight(), b.getWidth(), CvType.CV_8UC1);
                Utils.bitmapToMat(b, tmp);

                Mat processingMat = new Mat(b.getHeight(), b.getWidth(), CvType.CV_8UC1);

                prepareProcessingMat(tmp, processingMat);
                MatOfPoint largestContour = getLargestContour(processingMat);
                List<Point> largestContourCornerList = getLargestContourCornerList(largestContour);
                Mat perspectiveMat = createPerspectiveMat(tmp, largestContourCornerList);
                Bitmap boardImage = createBoardImage(perspectiveMat);
                b.recycle();


                // extract sudoku
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        sudokuBoardValues[i][j] = 0;
                    }
                }

                List<List<Bitmap>> cellImageList = generateCellImageList(boardImage);

                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        int finalI = i;
                        int finalJ = j;
                        Bitmap bitmap = cellImageList.get(i).get(j);

                        Matrix matrix = new Matrix();

                        matrix.postRotate(180);

                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);

                        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

                        System.out.println(bitmap.getWidth() + "*" + bitmap.getHeight());

//                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                        byte[] byteArray = stream.toByteArray();


                        InputImage inputImage = InputImage.fromBitmap(rotatedBitmap,0);
                        textRecognizer.process(inputImage)
                                .addOnSuccessListener(new OnSuccessListener<Text>() {
                                    @Override
                                    public void onSuccess(Text result) {
                                        int value = 0;
                                        if (!result.getTextBlocks().isEmpty()) {
                                            value = parseTextToInt(result.getTextBlocks().get(0).getText());
                                        } else {
                                            value = parseTextToInt(result.getText());
                                        }

                                        if (value != Integer.MIN_VALUE) {
                                            sudokuBoardValues[finalI][finalJ] = value;
                                        }else {
                                            value = 0;
                                        }

                                        System.out.print(finalI + "*" + finalJ + " :");
                                        System.out.println( " " + value + " ");

                                        if (finalI == 8 && finalJ == 8) {
                                            boolean result1 = sudokuSolver.solveSudoku(sudokuBoardValues);
                                            if (result1) {
                                                System.out.println("Solved");
                                                printBoard();

                                                /*Canvas canvas = new Canvas(boardImage);
                                                float cellSize = boardImage.getWidth() / 9;
                                                Paint paint = new Paint();
                                                paint.setColor(Color.GREEN);
                                                paint.setTextSize(20f);

                                                for (int k = 0; k < 9; k++) {
                                                    for (int l = 0; l < 9; l++) {
                                                        if (sudokuBoardValues[k][l] != 0) {
                                                            continue;
                                                        }

                                                        canvas.drawText(String.valueOf(sudokuBoardValues[k][l]),l* cellSize + 40f,k * cellSize + 85f,paint);
                                                    }
                                                }

                                                image.setImageBitmap(boardImage);*/

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

    @NonNull
    private static Bitmap createBoardImage(Mat perspectiveMat) {
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
        GaussianBlur(processingMat, processingMat, new Size(11.0, 11.0), 0.0);
        adaptiveThreshold(processingMat, processingMat, 255.0, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY_INV, 13, 2.0);
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

                Bitmap cellImage = Bitmap.createBitmap(boardImage, cellSize * j, cellSize * i, cellSize, cellSize);
                row.add(cellImage);
            }
            cellImageList.add(row);
        }

        return cellImageList;
    }

    @NonNull
    private static MatOfPoint getLargestContour(Mat processingMat) {

        Mat hierarchy = new Mat();
        ArrayList<MatOfPoint> matpoints = new ArrayList<>();
        findContours(processingMat,matpoints,hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);

        MatOfPoint largestContour = matpoints.stream().max(new Comparator<MatOfPoint>() {
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