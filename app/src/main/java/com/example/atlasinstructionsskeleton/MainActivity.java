package com.example.atlasinstructionsskeleton;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
    private Button leftButton;
    private Button rightButton;

    private View progressBarView;

    private List<Slide> slides;
    private int currentSlideIndex = 0;

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
        progressBarView = findViewById(R.id.progressBarView);

        slides = new ArrayList<>();
        slides.add(new Slide("Preparation", "Prepare the patient...", R.drawable.example, false));
        slides.add(new Slide("Positioning", "Position the patient...", R.drawable.example2, false));
        slides.add(new Slide("Setup", "Set up the equipment...", R.drawable.example, false));
        slides.add(new Slide("Calibration", "Touch the Probe to the green points on the patient’s skin. Once touched, the point will turn blue.", R.drawable.example, false));
        slides.add(new Slide("3D View", "Interact with the 3D model.", R.drawable.example, true));

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
        }
    }

    private void handleRight() {
        if (currentSlideIndex < slides.size() - 1) {
            currentSlideIndex++;
            updateSlide();
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
            progressBarView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.VISIBLE);
            interactiveView.setVisibility(View.GONE);
            progressBarView.setVisibility(View.INVISIBLE);
            if (currentSlide.imageResId != 0) {
                imageView.setImageResource(currentSlide.imageResId);
            } else {
                imageView.setImageDrawable(null);
            }
        }

        slideCounterTextView.setText("Step " + currentStep + "/" + slides.size());
    }

}
