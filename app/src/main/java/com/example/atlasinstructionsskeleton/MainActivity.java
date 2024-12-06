package com.example.atlasinstructionsskeleton;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log; // Added for logging
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Import ZeroMQ classes
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

// Import for JSON quoting
import org.json.JSONObject;
import org.zeromq.ZMQException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // Tag for logging

    private TextView titleTextView;
    private TextView instructionTextView;
    private ImageView imageView;
    private WebView atlas3DView;

    private TextView slideCounterTextView;

    private Button EVDButton;
    private Button NoConnection;
    private ImageButton leftButton;
    private ImageButton rightButton;
    private Button finishProcedureButton;
    private LinearLayout progressBarLayout;
    private ProgressBar progressBar;
    private TextView progressPercentage;

    private LinearLayout registrationErrorLayout;
    private LinearLayout registrationErrorBox;
    private TextView registrationErrorValue;

    private Button exitButton;

    private TextView unconnectedText;
    private TextView unconnected3dOverlay;

    private List<Slide> slides;
    private int currentSlideIndex = 0;
    private double currentProgress = 0;
    private int pointsTouched = 0;
    private int totalPoints = 3;
    private double percentage = 100.0 / totalPoints;
    private double regError = 0.0;

    // ZeroMQ context and sockets
    private ZContext zmqContext;
    private ZMQ.Socket zmqPushSocket;
    private ZMQ.Socket zmqSubSocket;
    private Thread zmqThread;
    private boolean isZmqRunning = false;

    // App state
    private boolean isUnconnectedMode = false;

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    public class WebAppInterface {
        private static final String TAG = "WebAppInterface";
        private MainActivity mainActivity;

        WebAppInterface(MainActivity activity) {
            this.mainActivity = activity;
        }

        @JavascriptInterface
        public void handleCallFromJS(String message) {
            Log.d(TAG, "handleCallFromJS() called from JavaScript with message: " + message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleRight();
                    Log.d(TAG, "handleRight() invoked from WebView");
                }
            });
        }

        @JavascriptInterface
        public void handleErrorFromJS(String message, String error) {
            Log.d(TAG, "handleCallFromJS() called from JavaScript with message: " + message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Error from JavaScript: " + error);
                    mainActivity.regError = Double.parseDouble(error) * 1000;
                }
            });
        }

        @JavascriptInterface
        public void handleResetRegistrationJS() {
            Log.d(TAG, "handleResetRegistrationJS() called from JavaScript");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.currentSlideIndex = 2;
                    mainActivity.updateSlide();
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // If using EdgeToEdge library; otherwise, remove
        setContentView(R.layout.pick_procedure_main);

        EVDButton = findViewById(R.id.EVDButton);

        EVDButton.setOnClickListener(v -> {
            EVDSlides();
            setUnconnectedMode(false);
        });

        NoConnection = findViewById(R.id.noConnection);
        NoConnection.setOnClickListener(v -> {
            setDialog(2);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        hideSystemUI();

    }



    private void initializeZeroMQ() {
        // Create ZeroMQ context
        zmqContext = new ZContext();

        // Server IP and ports
        String serverIp = "192.168.4.10"; // Replace with your server IP address
        int syncPort = 5557; // Port for synchronization (server's PULL socket)
        int pubPort = 5556;  // Port for receiving data (server's PUB socket)

        // 1. Create a PUSH socket to send a synchronization message to the server
        zmqPushSocket = zmqContext.createSocket(ZMQ.PUSH);
        String syncAddress = "tcp://" + serverIp + ":" + syncPort;
        zmqPushSocket.connect(syncAddress);

        // Send a synchronization message
        zmqPushSocket.send("sync");

        // Close the PUSH socket as it's no longer needed
        zmqPushSocket.close();

        // 2. Create a SUB socket to receive data from the server
        zmqSubSocket = zmqContext.createSocket(ZMQ.SUB);
        String pubAddress = "tcp://" + serverIp + ":" + pubPort;
        zmqSubSocket.connect(pubAddress);

        // Subscribe to all messages (you can specify a topic if needed)
        zmqSubSocket.subscribe("".getBytes());

        // Start a background thread to receive messages
        isZmqRunning = true;
        zmqThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "ZeroMQ receiving thread started.");
                while (isZmqRunning && !Thread.currentThread().isInterrupted()) {
                    try {
                        String receivedData = zmqSubSocket.recvStr();
                        if (receivedData != null) {
                            // Pass the data to JavaScript
                            runOnUiThread(() -> passDataToJavaScript(receivedData));
                        }
                    } catch (ZMQException e) {
                        if (zmqContext.isClosed() || !isZmqRunning) {
                            Log.d(TAG, "ZeroMQ context closed or running flag false, exiting receive loop.");
                        } else {
                            Log.e(TAG, "ZeroMQ exception: " + e.getMessage());
                        }
                        break; // Exit the loop to prevent further exceptions
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected exception in ZeroMQ thread: " + e.getMessage());
                        break;
                    }
                }
                Log.d(TAG, "ZeroMQ receiving thread terminated.");
            }
        });
        zmqThread.start();
    }

    private void passDataToJavaScript(String data) {
        // Escape data to prevent JavaScript injection
        String escapedData = JSONObject.quote(data);

        String jsCode = "handleDataFromAndroid(" + escapedData + ");";
        atlas3DView.evaluateJavascript(jsCode, null);
    }


    private void initializeWebView() {
        atlas3DView.setWebViewClient(new WebViewClient()); // Ensure links open within the WebView
        WebSettings webSettings = atlas3DView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript
        webSettings.setAllowFileAccess(true); // Allow access to local files
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // Add JavaScript interface
        atlas3DView.addJavascriptInterface(new WebAppInterface(this), "AndroidInterface");
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
        leftButton = findViewById(R.id.leftButton);
        rightButton = findViewById(R.id.leftButton);
        // Load the HTML file in WebView
        atlas3DView.loadUrl("file:///android_asset/model_viewer.html");
        slideCounterTextView = findViewById(R.id.slideCounterTextView);
        leftButton = findViewById(R.id.leftButton);
        rightButton = findViewById(R.id.rightButton);
        finishProcedureButton = findViewById(R.id.finishProcedureButton);
        progressBarLayout = findViewById(R.id.progressBarlayout);
        progressBar = findViewById(R.id.progressBar);
        progressPercentage = findViewById(R.id.progressPercentage);
        exitButton = findViewById(R.id.ExitButton);
        registrationErrorLayout = findViewById(R.id.registrationErrorLayout);
        registrationErrorBox = findViewById(R.id.registrationErrorBox);
        registrationErrorValue = findViewById(R.id.registrationErrorValue);

        slides = new ArrayList<>();
        slides.add(new Slide("Table Set Up", "Place all necessary equipment on the surgical table, and mount the tablet within view.", R.drawable.table_diagram));
        slides.add(new Slide("Burr Hole", "Create a burr hole at Kocher's point using the cranial access kit. Then attach ATLAS at the burr hole.", R.drawable.kochers_point_diagram));
        slides.add(new AtlasSlide("Calibration", "Touch the probe to the green points on the patient’s skin. Once touched, the point will turn blue."));
        slides.add(new AtlasSlide("Fine Tune", "Trace along the surface of the nose, cheekbones, and head to improve accuracy."));
        slides.add(new AtlasSlide("Guide EVD", "Attach the EVD to the effector at 12cm and follow visual guidance to place the drain in the ipsilateral ventricle."));
        slides.add(new Slide("Test Drain", "Remove the stylet and check drainage.\n\nContinue if draining, or return to step 5.\n", R.drawable.draining_diagram));
        slides.add(new Slide("Unmount", "Remove EVD from effector.\n\nUnbolt ATLAS and remove over drain.\n", R.drawable.removal_diagram));
        slides.add(new Slide("Closure", "Attach trocar to end of EVD and tunnel about 6 cm posterior to the burr hole. Staple drain to fixate and connect drainage bag.", R.drawable.cleaning_diagram));

        leftButton.setOnClickListener(v -> handleLeft());

        rightButton.setOnClickListener(v -> handleRight());

        exitButton.setOnClickListener(v -> setDialog(0));
        finishProcedureButton.setOnClickListener(v -> setDialog(0));

        // Initialize WebView settings
        initializeWebView();

        updateSlide();

        // Initialize ZeroMQ
        initializeZeroMQ();
    }

    private void voidOpenMenu() {
        setContentView(R.layout.pick_procedure_main);
        isUnconnectedMode = false;

        EVDButton = findViewById(R.id.EVDButton);
        NoConnection = findViewById(R.id.noConnection);

        EVDButton.setOnClickListener(v -> {
            EVDSlides();
        });

        NoConnection.setOnClickListener(v -> {
            setDialog(2);
        });
    }

    private void showVideo() {
        setContentView(R.layout.video_main);


        VideoView videoView = findViewById(R.id.videoView);


        MediaController mediaController = new MediaController(videoView.getContext());
        mediaController.setAnchorView(videoView); // Set where the controls should appear
        videoView.setMediaController(mediaController);

        videoView.setFocusable(true);
        videoView.requestFocus();


        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.atlas_demo);

        videoView.setVideoURI(videoUri);

        videoView.setZOrderOnTop(true);

        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start playing the video when clicked
                if (!videoView.isPlaying()) {
                    videoView.start();
                } else {
                    videoView.pause();  // Pause if already playing
                }
            }
        });



        // Add ExitButton functionality
        Button exitButton = findViewById(R.id.ExitButton);
        exitButton.setOnClickListener(v -> {
            voidOpenMenu();  // Navigate back to the menu screen
        });

        // Add a button to move to the next slide
        Button nextButton = findViewById(R.id.nextSlide);
        nextButton.setOnClickListener(v -> {
            EVDSlides(); // Transition to EVD slides
            setUnconnectedMode(true);
        });
    }


    private void setDialog(int x) {
        Dialog dialog = new Dialog(this);
        if (x == 0) {
            dialog.setContentView(R.layout.error1);

            Button button1 = dialog.findViewById(R.id.dialog_button_1);
            Button button2 = dialog.findViewById(R.id.dialog_button_2);

            button1.setOnClickListener(new View.OnClickListener() { // Return to procedure selection screen
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    voidOpenMenu();
                    // Safely close the ZeroMQ resources
                    isZmqRunning = false;
                    if (zmqThread != null) {
                        Log.d(TAG, "Interrupting ZeroMQ thread");
                        zmqThread.interrupt();
                    }
                    if (zmqPushSocket != null) {
                        Log.d(TAG, "Closing ZeroMQ PUSH socket");
                        zmqPushSocket.close();
                    }
                    if (zmqSubSocket != null) {
                        Log.d(TAG, "Closing ZeroMQ SUB socket");
                        zmqSubSocket.close();
                    }


                    // Return to procedure selection screen
                    currentSlideIndex = 0;
                    updateSlide();
                    resetProgress();
                    dialog.dismiss();
                }
            });

            dialog.show();
        } else if (x == 1) {
            dialog.setContentView(R.layout.error2);

            Button button1 = dialog.findViewById(R.id.dialog_close_button);
            Button button2 = dialog.findViewById(R.id.dialog_redo_button);

            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            button2.setOnClickListener(new View.OnClickListener() { // Go to redo calibration
                @Override
                public void onClick(View v) {
                    currentSlideIndex = 2;
                    updateSlide();
                    atlas3DView.evaluateJavascript("resetRegistration();", null);
                    dialog.dismiss();
                }
            });

            dialog.show();


            final int countdownTime = 10;
            final AtomicInteger remainingTime = new AtomicInteger(countdownTime);


            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {

                    button2.setText(String.format("Redo (%d seconds)", remainingTime.get()));


                    if (remainingTime.get() > 0) {
                        remainingTime.decrementAndGet();

                        new Handler(Looper.getMainLooper()).postDelayed(this, 1000);
                    } else {

                        if (dialog.isShowing()) {
                            currentSlideIndex = 2;
                            updateSlide();
                            atlas3DView.evaluateJavascript("resetRegistration();", null);
                            Log.d(TAG, "Resetting registration");
                            dialog.dismiss();
                        }
                    }
                }
            }, 1000);
        } else if (x == 2) {
            dialog.setContentView(R.layout.unconnected_popup);
            Button button1 = dialog.findViewById(R.id.dialog_button_1);
            Button button2 = dialog.findViewById(R.id.dialog_button_2);

            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            button2.setOnClickListener(new View.OnClickListener() { // Go to redo calibration
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    showVideo();
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
            // disable right arrow
            if (!isUnconnectedMode) {
                rightButton.setClickable(false);
                rightButton.setColorFilter(Color.rgb(240, 240, 240), PorterDuff.Mode.SRC_IN);   // gray out arrow
            } else {
                rightButton.setClickable(true);
                rightButton.clearColorFilter();
            }

            if (currentStep == 5) {
                imageView.setVisibility(View.GONE);
                atlas3DView.setVisibility(View.VISIBLE);
                updateRegistrationError(regError);

            } else {
                // Show WebView and hide ImageView
                imageView.setVisibility(View.GONE);
                atlas3DView.setVisibility(View.VISIBLE);
                //progressBarLayout.setVisibility(View.VISIBLE);
                //registrationErrorLayout.setVisibility(View.GONE);
                progressBar.setOnClickListener(v -> handlePointTouch());
            }
        } else {
            rightButton.setClickable(true);
            rightButton.clearColorFilter();

            // gray out arrows
            if (currentStep == 1) {
                leftButton.setVisibility(View.INVISIBLE);
                rightButton.setVisibility(View.VISIBLE);
                slideCounterTextView.setVisibility(View.VISIBLE);
                finishProcedureButton.setVisibility(View.GONE);
            } else if (currentStep == 8) {
                leftButton.setVisibility(View.VISIBLE);
                rightButton.setVisibility(View.GONE);
                slideCounterTextView.setVisibility(View.GONE);
                finishProcedureButton.setVisibility(View.VISIBLE);
            } else {
                leftButton.setVisibility(View.VISIBLE);
                rightButton.setVisibility(View.VISIBLE);
                slideCounterTextView.setVisibility(View.VISIBLE);
                finishProcedureButton.setVisibility(View.GONE);
                finishProcedureButton.setVisibility(View.GONE);
            }

            // Show ImageView and hide WebView
            imageView.setVisibility(View.VISIBLE);
            atlas3DView.setVisibility(View.GONE);
            progressBarLayout.setVisibility(View.GONE);
            registrationErrorLayout.setVisibility(View.GONE);

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
            updateProgress(percentage);
        } else if (pointsTouched == totalPoints) {
            updateProgress(33);
            new Handler(Looper.getMainLooper()).postDelayed(this::handleRight, 500);
        }
    }

    private void updateProgress(double percentage) {
        currentProgress += percentage;
        int progress = (int) Math.floor(currentProgress + 0.5);
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
        if (errorValue > 5) {
            registrationErrorBox.setBackgroundResource(R.drawable.registration_frame_red);
            if (!isUnconnectedMode) {
                setDialog(1);
            }
        } else if (errorValue < 1) {
            registrationErrorBox.setBackgroundResource(R.drawable.registration_frame_green);
            rightButton.setClickable(true);
            rightButton.clearColorFilter();
        } else {
            registrationErrorBox.setBackgroundResource(R.drawable.registration_frame_yellow);
            rightButton.setClickable(true);
            rightButton.clearColorFilter();
        }
    }

    private void setUnconnectedMode(boolean newUnconnectedModeState) {

        isUnconnectedMode = newUnconnectedModeState;
        updateUnconnectedMode();
    }

    private void updateUnconnectedMode() {

        unconnectedText = findViewById(R.id.unconnectedText);
        unconnected3dOverlay = findViewById(R.id.unconnected3dOverlay);

        if (isUnconnectedMode) {
            unconnectedText.setText("Unconnected Mode");
            unconnected3dOverlay.setVisibility(View.VISIBLE);
        } else {
            unconnectedText.setText("");
            unconnected3dOverlay.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up ZeroMQ resources
        isZmqRunning = false;
        if (zmqThread != null) {
            zmqThread.interrupt();
        }
        if (zmqSubSocket != null) {
            zmqSubSocket.close();
        }
        if (zmqContext != null) {
            zmqContext.close();
        }
    }
}
