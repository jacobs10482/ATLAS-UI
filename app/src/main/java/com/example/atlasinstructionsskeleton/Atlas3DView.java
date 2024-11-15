package com.example.atlasinstructionsskeleton; // Replace with your actual package name

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Light;
import com.google.ar.sceneform.rendering.ModelRenderable;

public class Atlas3DView extends FrameLayout {

    private SceneView sceneView;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private Node modelNode;
    private float rotationX = 0f;
    private float rotationY = 0f;

    // Listener interface for point touches
    public interface OnPointTouchListener {
        void onPointTouched();
    }

    private OnPointTouchListener pointTouchListener;

    public void setOnPointTouchListener(OnPointTouchListener listener) {
        this.pointTouchListener = listener;
    }

    public Atlas3DView(Context context) {
        super(context);
        init(context, null);
    }

    public Atlas3DView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public Atlas3DView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // Initialize SceneView
        sceneView = new SceneView(context);
        sceneView.setBackgroundColor(android.graphics.Color.GREEN);
        // Add sceneView to this layout
        this.addView(sceneView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Initialize gesture detectors
        gestureDetector = new GestureDetector(context, new GestureListener());
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

        // Set the touch listener on the SceneView
        sceneView.setOnTouchListener((v, event) -> {
            boolean result1 = gestureDetector.onTouchEvent(event);
            boolean result2 = scaleGestureDetector.onTouchEvent(event);
            return result1 || result2;
        });

        // Load the glTF model from assets
        loadModel(Uri.parse("file:///android_asset/phantom.glb")); // Ensure the file is correctly placed
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (modelNode != null) {
                rotationX += distanceY / 10f;
                rotationY += distanceX / 10f;

                // Limit rotation to prevent flipping
                rotationX = Math.max(-90, Math.min(rotationX, 90));

                modelNode.setLocalRotation(Quaternion.eulerAngles(
                        new Vector3(rotationX, rotationY, 0f)
                ));
            }
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (modelNode != null) {
                float scale = modelNode.getLocalScale().x * detector.getScaleFactor();
                scale = Math.max(0.01f, Math.min(scale, 10.0f));
                modelNode.setLocalScale(new Vector3(scale, scale, scale));
            }
            return true;
        }
    }

    private void loadModel(Uri modelUri) {
        ModelRenderable.builder()
                .setSource(getContext(), modelUri)
                .build()
                .thenAccept(this::addModelToScene)
                .exceptionally(throwable -> {
                    Toast.makeText(getContext(), "Unable to load model: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    throwable.printStackTrace();
                    return null;
                });
    }

    private void addModelToScene(ModelRenderable renderable) {
        modelNode = new Node();
        modelNode.setRenderable(renderable);

        // Position the model 1 meter in front of the camera
        modelNode.setLocalPosition(new Vector3(0.0f, 0.0f, -0.4f));

        // Add the node to the scene
        sceneView.getScene().addChild(modelNode);

        // Set the initial position of the camera
        Camera camera = sceneView.getScene().getCamera();
        camera.setLocalPosition(new Vector3(0.0f, 0.0f, 0.0f)); // Camera at origin

        // Compute the direction vector to the model
        Vector3 cameraPosition = camera.getLocalPosition();
        Vector3 modelPosition = modelNode.getLocalPosition();
        Vector3 direction = Vector3.subtract(modelPosition, cameraPosition).normalized();

        // Compute the rotation quaternion
        Quaternion rotation = Quaternion.lookRotation(direction, Vector3.up());

        // Set the camera's rotation
        camera.setLocalRotation(rotation);

        // Add light to the scene
        addLightToScene();
    }

    private void addLightToScene() {
        Light light = Light.builder(Light.Type.DIRECTIONAL)
                .setColor(new Color(android.graphics.Color.WHITE))
                .setIntensity(1.0f)
                .build();

        Node lightNode = new Node();
        lightNode.setLight(light);
        // Adjust the direction of the light if necessary
        lightNode.setLocalRotation(Quaternion.eulerAngles(new Vector3(-90f, 0f, 0f)));

        sceneView.getScene().addChild(lightNode);
    }

    // Lifecycle management
    public void onResume() throws CameraNotAvailableException {
        sceneView.resume();
    }

    public void onPause() {
        sceneView.pause();
    }

    public void onDestroy() {
        sceneView.destroy();
    }
}
