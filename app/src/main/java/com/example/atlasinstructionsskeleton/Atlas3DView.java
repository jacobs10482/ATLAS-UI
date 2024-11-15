package com.example.atlasinstructionsskeleton;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Light;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.Node;

public class Atlas3DView extends FrameLayout {

    private ArSceneView arSceneView;
    private Node modelNode;

    public Atlas3DView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public Atlas3DView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Atlas3DView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Initialize ArSceneView
        arSceneView = new ArSceneView(context);
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        arSceneView.setLayoutParams(params);
        addView(arSceneView);

        // Load the .sfb model
        ModelRenderable.builder()
                .setSource(context, Uri.parse("file:///android_asset/Box.glb"))
                .build()
                .thenAccept(this::addModelToScene)
                .exceptionally(throwable -> {
                    Toast.makeText(context, "Unable to load model: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    throwable.printStackTrace();
                    return null;
                });

        // Add light to the scene
        addLightToScene();
    }

    private void addModelToScene(ModelRenderable renderable) {
        modelNode = new Node();
        modelNode.setRenderable(renderable);

        // Position the model 1 meter in front of the camera
        modelNode.setLocalPosition(new Vector3(0.0f, 0.0f, -1.0f));

        // Add the node to the scene
        arSceneView.getScene().addChild(modelNode);

        // Adjust the camera if needed (Sceneform handles basic camera positioning)
    }

    private void addLightToScene() {
        // Create directional light
        Light light = Light.builder(Light.Type.DIRECTIONAL)
                .setColor(new com.google.ar.sceneform.rendering.Color(android.graphics.Color.WHITE))
                .setIntensity(1.0f)
                .build();

        // Create a node for the light
        Node lightNode = new Node();
        lightNode.setLight(light);
        // Adjust the direction of the light if necessary
        lightNode.setLocalRotation(Quaternion.eulerAngles(new Vector3(-90f, 0f, 0f)));

        // Add the light node to the scene
        arSceneView.getScene().addChild(lightNode);
    }

    // Lifecycle methods to manage ArSceneView
    public void onResume() {
        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void onPause() {
        arSceneView.pause();
    }

    public void onDestroy() {
        arSceneView.destroy();
    }
}
