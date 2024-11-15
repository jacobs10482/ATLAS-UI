package com.example.atlasinstructionsskeleton;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasinstructionsskeleton.Atlas3DView;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView titleTextView;
    private TextView instructionTextView;
    private ImageView imageView;
    private Atlas3DView atlas3DView; // Updated type
    private TextView slideCounterTextView;
    private ImageButton leftButton;
    private ImageButton rightButton;
    private LinearLayout progressBarLayout;
    private ProgressBar progressBar;
    private TextView progressPercentage;

    private Button exitButton;
    private List<Slide> slides;
    private int currentSlideIndex = 0;
    private double currentProgress = 0;
    private int pointsTouched = 0;
    private int totalPoints = 3;
    private double percentage = 100.0 / totalPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        titleTextView = findViewById(R.id.titleTextView);
        instructionTextView = findViewById(R.id.instructionTextView);
        imageView = findViewById(R.id.imageView);
        atlas3DView = findViewById(R.id.atlas3dView); // Initialize Atlas3DView
        slideCounterTextView = findViewById(R.id.slideCounterTextView);
        leftButton = findViewById(R.id.leftButton);
        rightButton = findViewById(R.id.rightButton);
        progressBarLayout = findViewById(R.id.progressBarlayout);
        progressBar = findViewById(R.id.progressBar);
        progressPercentage = findViewById(R.id.progressPercentage);
        exitButton = findViewById(R.id.ExitButton);

        slides = new ArrayList<>();
        slides.add(new Slide("Table Set Up", "Confirm the surgical table looks as such", R.drawable.table_diagram));
        slides.add(new Slide("Burr Hole", "Create a Burr hole at Kocher's point using a surgical screw driver. Then attach ATLAS at the Burr hole.", R.drawable.kochers_point_diagram));
        slides.add(new AtlasSlide("Calibration", "Touch the Probe to the green points on the patient’s skin. Once touched, the point will turn blue"));
        slides.add(new AtlasSlide("Fine Tune", "Trace the surface of the patient’s skin for about 15 seconds, ensuring to capture different features of the head."));
        slides.add(new AtlasSlide("Guide EVD", "Attach the EVD to the effector at 12cm and follow visual guidance to place the drain."));
        slides.add(new Slide("Test Drain", "Remove the stylet and check drainage.\n\nIf draining, plug drain and proceed. Otherwise, return to step 6.\n", R.drawable.draining_diagram));
        slides.add(new Slide("Unmount", "Remove EVD from effector.\n\nUnbolt ATLAS and remove over drain.\n", R.drawable.removal_diagram));
        slides.add(new Slide("Closure", "Attach trocar to end of EVD and tunnel about 6 cm posterior to the burr hole. Staple drain to fixate and connect drainage bag.", R.drawable.cleaning_diagram));

        leftButton.setOnClickListener(v -> {
            handleLeft();
        });

        rightButton.setOnClickListener(v -> {
            handleRight();
        });

        exitButton.setOnClickListener(v -> {
            setDialog(0);
        });

        // Set listener for Atlas3DView point touches
        atlas3DView.setOnPointTouchListener(() -> handlePointTouch());

        updateSlide();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void handleLeft() {
        if (currentSlideIndex > 0) {
            currentSlideIndex--;
            updateSlide();
            resetProgress();
        }
    }

    private void handleRight() {
        if (currentSlideIndex < slides.size() - 1) {
            currentSlideIndex++;
            updateSlide();
            resetProgress();
        }
    }

    private void setDialog(int x){
        Dialog dialog = new Dialog(this);
        if (x == 0){
            dialog.setContentView(R.layout.error1);

            Button button1 = dialog.findViewById(R.id.dialog_button_1);
            Button button2 = dialog.findViewById(R.id.dialog_button_2);

            button1.setOnClickListener(v -> setDialog(1));
            button2.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        } else if (x == 1){
            dialog.setContentView(R.layout.error2);

            Button button1 = dialog.findViewById(R.id.dialog_close_button);
            Button button2 = dialog.findViewById(R.id.dialog_redo_button);

            button1.setOnClickListener(v -> dialog.dismiss());
            button2.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        }
    }

    private void updateSlide() {
        Slide currentSlide = slides.get(currentSlideIndex);
        int currentStep = currentSlideIndex + 1;

        titleTextView.setText("Step " + currentStep + ": " + currentSlide.title);
        instructionTextView.setText(currentSlide.instruction);

        if (currentSlide instanceof AtlasSlide) {
            // Show Atlas3DView and hide ImageView
            imageView.setVisibility(View.GONE);
            atlas3DView.setVisibility(View.VISIBLE);
            progressBarLayout.setVisibility(View.VISIBLE);
        } else {
            // Show ImageView and hide Atlas3DView
            imageView.setVisibility(View.VISIBLE);
            atlas3DView.setVisibility(View.GONE);
            progressBarLayout.setVisibility(View.INVISIBLE);

            if (currentSlide.imageResId != 0) {
                imageView.setImageResource(currentSlide.imageResId);
            } else {
                imageView.setImageDrawable(null);
            }
        }

        slideCounterTextView.setText("Step " + currentStep + "/" + slides.size());
    }

    private void handlePointTouch() {
        pointsTouched++;
        if (pointsTouched < totalPoints) {
            updateProgress();
        } else if (pointsTouched == totalPoints) {
            updateProgress();
            new Handler(Looper.getMainLooper()).postDelayed(this::handleRight, 500);
        }
    }

    private void updateProgress() {
        currentProgress += percentage;
        int progress = (int)Math.floor(currentProgress + 0.5);
        progressBar.setProgress(progress);
        progressPercentage.setText(progress + "%");
    }

    private void resetProgress() {
        currentProgress = 0;
        pointsTouched = 0;
        progressBar.setProgress(0);
        progressPercentage.setText("0%");
    }

    // Lifecycle management
    @Override
    protected void onResume() {
        super.onResume();
        if (atlas3DView.getVisibility() == View.VISIBLE) {
            try {
                atlas3DView.onResume();
            } catch (CameraNotAvailableException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (atlas3DView.getVisibility() == View.VISIBLE) {
            atlas3DView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        atlas3DView.onDestroy();
    }
}
