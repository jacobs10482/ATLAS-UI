package com.example.atlasinstructionsskeleton;

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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView titleTextView;
    private TextView instructionTextView;
    private ImageView imageView;
    private View interactiveView;
    private TextView slideCounterTextView;
    private ImageButton leftButton;
    private ImageButton rightButton;
    private LinearLayout progressBarLayout;
    private ProgressBar progressBar;
    private TextView progressPercentage;

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
        interactiveView = findViewById(R.id.interactiveView);
        slideCounterTextView = findViewById(R.id.slideCounterTextView);
        leftButton = findViewById(R.id.leftButton);
        rightButton = findViewById(R.id.rightButton);
        progressBarLayout = findViewById(R.id.progressBarlayout);
        progressBar = findViewById(R.id.progressBar);
        progressPercentage = findViewById(R.id.progressPercentage);

        slides = new ArrayList<>();
        slides.add(new Slide("Table Set Up", "Confirm the surgical table looks as such", R.drawable.table_diagram, false));
        slides.add(new Slide("Burr Hole", "Create a Burr hole at Kocher's point using a surgical screw driver. Then attach ATLAS at the Burr hole.", R.drawable.kochers_point_diagram, false));
        slides.add(new Slide("Calibration", "Touch the Probe to the green points on the patient’s skin. Once touched, the point will turn blue", R.drawable.example, true));
        slides.add(new Slide("Fine Tune", "Trace the surface of the patient’s skin for about 15 seconds, ensuring to capture different features of the head.", R.drawable.example, true));
        slides.add(new Slide("Guide EVD", "Attach the EVD to the effector at 12cm and follow visual guidance to place the drain.", R.drawable.example, true));
        slides.add(new Slide("Test Drain", "Remove the stylet and check drainage.\n\nIf draining, plug drain and proceed.  Otherwise, return to step 6.\n", R.drawable.insertion_diagram, false));
        slides.add(new Slide("Unmount", "Remove EVD from effector.\n\n\nUnbolt ATLAS and remove over drain.\n", R.drawable.draining_diagram, false));
        slides.add(new Slide("Closure", "Attach trocar to end of EVD and tunnel about 6 cm posterior to the burr hole. Staple drain to fixate and connect drainage bag.", R.drawable.cleaning_diagram, false));

        leftButton.setOnClickListener(v -> {
            handleLeft();
        });

        rightButton.setOnClickListener(v -> {
            handleRight();
        });

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

    private void updateSlide() {
        Slide currentSlide = slides.get(currentSlideIndex);
        int currentStep = currentSlideIndex + 1;

        titleTextView.setText("Step " + currentStep + ": " + currentSlide.title);
        instructionTextView.setText(currentSlide.instruction);

        if (currentSlide.showInteractive) {
            imageView.setVisibility(View.GONE);
            interactiveView.setVisibility(View.VISIBLE);
            progressBarLayout.setVisibility(View.VISIBLE);
            interactiveView.setOnClickListener(v -> handlePointTouch());
        } else {
            imageView.setVisibility(View.VISIBLE);
            interactiveView.setVisibility(View.GONE);
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
        int progress = (int)Math.floor(currentProgress+0.5);
        progressBar.setProgress(progress);
        progressPercentage.setText(progress + "%");
    }

    private void resetProgress() {
        currentProgress = 0;
        pointsTouched = 0;
        progressBar.setProgress(0);
        progressPercentage.setText(0 + "%");
    }

}
