package com.example.atlasinstructionsskeleton;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    private WebView atlas3DView; // Changed type to WebView

    private TextView slideCounterTextView;

    private Button EVDButton;
    private ImageButton leftButton;
    private ImageButton rightButton;
    private LinearLayout progressBarLayout;
    private ProgressBar progressBar;
    private TextView progressPercentage;

    private LinearLayout registrationErrorLayout;
    private LinearLayout registrationErrorBox;
    private TextView registrationErrorValue;


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
        EdgeToEdge.enable(this); // If using EdgeToEdge library; otherwise, remove
        setContentView(R.layout.pick_procedure_main);


        EVDButton = findViewById(R.id.EVDButton);

        EVDButton.setOnClickListener(v -> {
            EVDSlides();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeWebView() {
        atlas3DView.setWebViewClient(new WebViewClient()); // Ensure links open within the WebView
        WebSettings webSettings = atlas3DView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript
        webSettings.setAllowFileAccess(true); // Allow access to local files
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
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

    private void EVDSlides() {
        setContentView(R.layout.activity_main);

        titleTextView = findViewById(R.id.titleTextView);
        instructionTextView = findViewById(R.id.instructionTextView);
        imageView = findViewById(R.id.imageView);
        atlas3DView = findViewById(R.id.atlas3dView); // Initialize WebView
        // Load the HTML file in WebView
        atlas3DView.loadUrl("file:///android_asset/model_viewer.html");
        slideCounterTextView = findViewById(R.id.slideCounterTextView);
        leftButton = findViewById(R.id.leftButton);
        rightButton = findViewById(R.id.rightButton);
        progressBarLayout = findViewById(R.id.progressBarlayout);
        progressBar = findViewById(R.id.progressBar);
        progressPercentage = findViewById(R.id.progressPercentage);
        exitButton = findViewById(R.id.ExitButton);
        registrationErrorLayout = findViewById(R.id.registrationErrorLayout);
        registrationErrorBox = findViewById(R.id.registrationErrorBox);
        registrationErrorValue = findViewById(R.id.registrationErrorValue);

        slides = new ArrayList<>();
        slides.add(new Slide("Table Set Up", "Confirm the surgical table looks as such", R.drawable.table_diagram));
        slides.add(new Slide("Burr Hole", "Create a Burr hole at Kocher's point using a surgical screw driver. Then attach ATLAS at the Burr hole.", R.drawable.kochers_point_diagram));
        slides.add(new AtlasSlide("Calibration", "Touch the Probe to the green points on the patient’s skin. Once touched, the point will turn blue"));
        slides.add(new AtlasSlide("Fine Tune", "Trace the surface of the patient’s skin for about 15 seconds, ensuring to capture different features of the head."));
        slides.add(new AtlasSlide("Guide EVD", "Attach the EVD to the effector at 12cm and follow visual guidance to place the drain."));
        slides.add(new Slide("Test Drain", "Remove the stylet and check drainage.\n\nIf draining, plug drain and proceed. Otherwise, return to step 6.\n", R.drawable.draining_diagram));
        slides.add(new Slide("Unmount", "Remove EVD from effector.\n\nUnbolt ATLAS and remove over drain.\n", R.drawable.removal_diagram));
        slides.add(new Slide("Closure", "Attach trocar to end of EVD and tunnel about 6 cm posterior to the burr hole. Staple drain to fixate and connect drainage bag.", R.drawable.cleaning_diagram));

        leftButton.setOnClickListener(v -> handleLeft());

        rightButton.setOnClickListener(v -> handleRight());

        exitButton.setOnClickListener(v -> setDialog(0));

        // Initialize WebView settings
        initializeWebView();

        updateSlide();
    }


    private void voidOpenMenu(){
        setContentView(R.layout.pick_procedure_main);

        EVDButton = findViewById(R.id.EVDButton);


        EVDButton.setOnClickListener(v -> {
            EVDSlides();
        });

    }

    private void setDialog(int x){
        Dialog dialog = new Dialog(this);
        if (x == 0){
            dialog.setContentView(R.layout.error1);

            Button button1 = dialog.findViewById(R.id.dialog_button_1);
            Button button2 = dialog.findViewById(R.id.dialog_button_2);

            button1.setOnClickListener(new View.OnClickListener() {         //needs to return to procedure selection screen
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    voidOpenMenu();

                    currentSlideIndex = 0;
                    updateSlide();
                    resetProgress();
                    dialog.dismiss();

                }
            });

            dialog.show();
        } else if (x == 1){
            dialog.setContentView(R.layout.error2);

            Button button1 = dialog.findViewById(R.id.dialog_close_button);
            Button button2 = dialog.findViewById(R.id.dialog_redo_button);

            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            button2.setOnClickListener(new View.OnClickListener() {         //needs to goto redo calibration
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        }
    }
    private void updateSlide() {
        Slide currentSlide = slides.get(currentSlideIndex);
        int currentStep = currentSlideIndex + 1;

        titleTextView.setText("Step " + currentStep + ": " + currentSlide.title);
        instructionTextView.setText(currentSlide.instruction);

        if (currentSlide instanceof AtlasSlide) {
            if (currentStep == 5) {
                imageView.setVisibility(View.GONE);
                atlas3DView.setVisibility(View.VISIBLE);
                progressBarLayout.setVisibility(View.GONE);
                registrationErrorLayout.setVisibility(View.VISIBLE);
                updateRegistrationError(0);
            } else {
                // Show WebView and hide ImageView
                imageView.setVisibility(View.GONE);
                atlas3DView.setVisibility(View.VISIBLE);
                progressBarLayout.setVisibility(View.VISIBLE);
                registrationErrorLayout.setVisibility(View.INVISIBLE);


            }
        } else {
            // Show ImageView and hide WebView
            imageView.setVisibility(View.VISIBLE);
            atlas3DView.setVisibility(View.GONE);
            progressBarLayout.setVisibility(View.INVISIBLE);
            registrationErrorLayout.setVisibility(View.INVISIBLE);

            if (currentSlide.imageResId != 0) {
                imageView.setImageResource(currentSlide.imageResId);
            } else {
                imageView.setImageDrawable(null);
            }
        }

        slideCounterTextView.setText("Step " + currentStep + "/" + slides.size());
    }

    private void updateProgress(int percentage) {
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

    private void updateRegistrationError(double errorValue) {
        registrationErrorValue.setText(String.format("%.2f mm", errorValue));
        if (errorValue > 2) {
            registrationErrorBox.setBackgroundResource(R.drawable.registration_frame_red);
            setDialog(1);
        } else if (errorValue < 1) {
            registrationErrorBox.setBackgroundResource(R.drawable.registration_frame_green);
        } else {
            registrationErrorBox.setBackgroundResource(R.drawable.registration_frame_yellow);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // No need to manage WebView lifecycle here
    }

    @Override
    protected void onPause() {
        super.onPause();
        // No need to manage WebView lifecycle here
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // No need to manage WebView lifecycle here
    }
}
