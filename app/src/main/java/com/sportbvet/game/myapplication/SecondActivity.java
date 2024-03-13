package com.sportbvet.game.myapplication;

import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.RETR_TREE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.findContours;

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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
import java.util.Comparator;
import java.util.List;

public class SecondActivity extends AppCompatActivity {

    Button button;
    ImageView image;

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
                Utils.matToBitmap(tmp, b);

                // image process for detect board
                GaussianBlur(tmp, tmp, new Size(11.0, 11.0), 0.0);
                adaptiveThreshold(tmp, tmp, 255.0, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY_INV, 13, 2.0);

                Mat hierarchy = new Mat();
                ArrayList<MatOfPoint> matpoints = new ArrayList<>();
                findContours(tmp,matpoints,hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
                MatOfPoint largestContour = matpoints.stream().max(new Comparator<MatOfPoint>() {
                    @Override
                    public int compare(MatOfPoint o1, MatOfPoint o2) {
                        return Double.compare(Imgproc.contourArea(o1),Imgproc.contourArea(o2));
                    }
                }).get();

                List<Point> largestContourCornerList = new ArrayList<>();

                MatOfPoint2f src = new MatOfPoint2f();
                val approxPoly = MatOfPoint2f()
                largestContour.convertTo(src, CvType.CV_32FC2)
                val arcLength = arcLength(src, true)
                approxPolyDP(src, approxPoly, 0.02 * arcLength, true)

                image.setImageBitmap(b);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}