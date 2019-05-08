package com.example.ter_ra_android;

/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.animation.Animator;
import android.content.Intent;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.example.common.helpers.CameraPermissionHelper;
import com.example.common.helpers.DisplayRotationHelper;
import com.example.common.helpers.FullScreenHelper;
import com.example.common.helpers.SnackbarHelper;
import com.example.common.helpers.TapHelper;
import com.example.common.rendering.BackgroundRenderer;
import com.example.common.rendering.ObjectRenderer;
import com.example.common.rendering.ObjectRenderer.BlendMode;
import com.example.common.rendering.PlaneRenderer;
import com.example.common.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static Random rand;

    private BottomNavigationView mInventory;
    private ToggleButton mToggleButton;
    private Button mTerminate;
    private boolean isHide = false;
    private boolean isHideForever = false;
    private boolean displayPlane = true;

    //Objects color
    private static final float[] crowbarColor = {162.0f, 31.0f, 31.0f, 255.0f}; //??
    private static final float[] keyColor = {252.0f, 220.0f, 18.0f, 255.0f};
    private static final float[] sideTableColor = {120.0f, 84.0f, 71.0f, 255.0f};
    private static final float[] treasureTrunkColor = {121.0f, 84.0f, 71.0f, 255.0f};
    private static final float[] cornerTableColor = {122.0f, 84.0f, 71.0f, 255.0f}; //??
    private static final float[] woodenColor = {123.0f, 84.0f, 71.0f, 255.0f};

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    private boolean installRequested;

    private Session session;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    private TapHelper tapHelper;

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer virtualKey = new ObjectRenderer();
    private final ObjectRenderer virtualCrowbar = new ObjectRenderer();
    private final ObjectRenderer virtualSideTable = new ObjectRenderer();
    private final ObjectRenderer virtualTreasureTrunk = new ObjectRenderer();
    private final ObjectRenderer virtualCornerTable = new ObjectRenderer();
    private final ObjectRenderer virtualWooden = new ObjectRenderer();
    private PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] anchorMatrix = new float[16];
    private static final float[] DEFAULT_COLOR = new float[] {0f, 0f, 0f, 0f};

    private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";

    public void onMenuClick(View view) {
        Log.e(TAG, "Menu");
        Toast.makeText(MainActivity.this, "Menu", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Menu.class);
        startActivity(intent);
    }

    public void hideOrShowInventory(View view) {
        if(!isHide){
            mInventory.animate().y(mInventory.getY() + mInventory.getHeight()).setDuration(1000);
            view.animate().y(view.getY() + mInventory.getHeight()).setDuration(1000).setListener(new Animator.AnimatorListener() {

                            @Override
                            public void onAnimationStart(Animator animation) {
                                view.setClickable(false);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                view.setClickable(true);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) { }

                            @Override
                            public void onAnimationRepeat(Animator animation) { }
                        });
            isHide = true;
        } else{
            mInventory.animate().y(mInventory.getY() - mInventory.getHeight()).setDuration(1000);
            view.animate().y(view.getY() - mInventory.getHeight()).setDuration(1000).setListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    view.setClickable(false);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setClickable(true);
                }

                @Override
                public void onAnimationCancel(Animator animation) { }

                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
            isHide = false;
        }
    }

    public void onTerminateClick(View view) {
        view.setVisibility(View.GONE);
        isHideForever = true;
        mInventory.setVisibility(View.VISIBLE);
        mToggleButton.setVisibility(View.VISIBLE);

        placeKey();
        //placeCrowbar();
        placeSideTable();
        placeTreasureTrunk();
        //placeCornerTable();
        placeWooden();
        //placeObjects();

        displayPlane = false;
    }

    private void placeObjects() {
        float[] objColor = new float[] {139.0f, 195.0f, 74.0f, 255.0f}; //vert
        Anchor anchor;
        float randomX;
        float randomZ;
        Pose pose;
        float[] translation;
        float[] rotation;

        for (Plane plane : session.getAllTrackables(Plane.class)) {

            randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );

            Log.e(TAG,"plane.getExtentX() : " + plane.getExtentX() );
            Log.e(TAG,"randomX : " + randomX );

            randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

            Log.e(TAG,"plane.getExtentZ() : " + plane.getExtentZ() );
            Log.e(TAG,"randomZ : " + randomZ );

            pose = plane.getCenterPose();
            Log.e(TAG,"centerPose : " + pose );

            translation = pose.getTranslation();
            rotation = pose.getRotationQuaternion();

            Log.e(TAG,"translation[0] : " + translation[0] );
            translation[0] += randomX;
            Log.e(TAG,"translation[0] : " + translation[0] );

            Log.e(TAG,"translation[2] : " + translation[2] );
            translation[2] += randomZ;
            Log.e(TAG,"translation[2] : " + translation[2] );

            pose = new Pose(translation, rotation);

            anchor = plane.createAnchor(pose);
            anchors.add(new ColoredAnchor(anchor, objColor));
        }
    }

    private void placeKey() {
        Anchor anchor;
        float randomX;
        float randomZ;
        Pose pose;
        float[] translation;
        float[] rotation;

        int size = session.getAllTrackables(Plane.class).size();
        Object[] planes = session.getAllTrackables(Plane.class).toArray();

        int rng;
        Plane plane;
        do{
            rng = rand.nextInt(size);
            plane = (Plane) planes[rng];
        } while (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING);

        randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );

        randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

        pose = plane.getCenterPose();

        translation = pose.getTranslation();
        translation[0] += randomX;
        translation[2] += randomZ;

        rotation = pose.getRotationQuaternion();
        rotation[0] = 90;
        rotation[1] = 0;
        rotation[2] = 0;
        rotation[3] = 90;

        pose = new Pose(translation, rotation);

        anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, keyColor));
    }

    private void placeCrowbar() {
        Anchor anchor;
        float randomX;
        float randomZ;
        Pose pose;
        float[] translation;
        float[] rotation;

        int size = session.getAllTrackables(Plane.class).size();
        Object[] planes = session.getAllTrackables(Plane.class).toArray();

        int rng;
        Plane plane;
        do{
            rng = rand.nextInt(size);
            plane = (Plane) planes[rng];
        } while (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING);

        randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );

        randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

        pose = plane.getCenterPose();

        translation = pose.getTranslation();
        translation[0] += randomX;
        translation[2] += randomZ;

        rotation = pose.getRotationQuaternion();
        rotation[0] = 90;
        rotation[1] = 0;
        rotation[2] = 0;
        rotation[3] = 90;

        pose = new Pose(translation, rotation);

        anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, crowbarColor));
    }

    private void placeSideTable() {
        Anchor anchor;
        float randomX;
        float randomZ;
        Pose pose;
        float[] translation;
        float[] rotation;

        int size = session.getAllTrackables(Plane.class).size();
        Object[] planes = session.getAllTrackables(Plane.class).toArray();

        int rng;
        Plane plane;
        do{
            rng = rand.nextInt(size);
            plane = (Plane) planes[rng];
        } while (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING);

        randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );

        randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

        pose = plane.getCenterPose();

        translation = pose.getTranslation();
        translation[0] += randomX;
        translation[2] += randomZ;

        rotation = pose.getRotationQuaternion();

        pose = new Pose(translation, rotation);

        anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, sideTableColor));
    }

    private void placeTreasureTrunk() {
        Anchor anchor;
        float randomX;
        float randomZ;
        Pose pose;
        float[] translation;
        float[] rotation;

        int size = session.getAllTrackables(Plane.class).size();
        Object[] planes = session.getAllTrackables(Plane.class).toArray();

        int rng;
        Plane plane;
        do{
            rng = rand.nextInt(size);
            plane = (Plane) planes[rng];
        } while (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING);

        randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );

        randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

        pose = plane.getCenterPose();

        translation = pose.getTranslation();
        translation[0] += randomX;
        translation[2] += randomZ;

        rotation = pose.getRotationQuaternion();

        pose = new Pose(translation, rotation);

        anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, treasureTrunkColor));
    }

    private void placeCornerTable() {
        Anchor anchor;
        float randomX;
        float randomZ;
        Pose pose;
        float[] translation;
        float[] rotation;

        int size = session.getAllTrackables(Plane.class).size();
        Object[] planes = session.getAllTrackables(Plane.class).toArray();

        int rng;
        Plane plane;
        do{
            rng = rand.nextInt(size);
            plane = (Plane) planes[rng];
        } while (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING);


        randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );

        randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

        pose = plane.getCenterPose();

        translation = pose.getTranslation();
        translation[0] += randomX;
        translation[2] += randomZ;

        rotation = pose.getRotationQuaternion();

        pose = new Pose(translation, rotation);

        anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, cornerTableColor));
    }

    private void placeWooden() {
        Anchor anchor;
        float randomX;
        float randomZ;
        Pose pose;
        float[] translation;
        float[] rotation;

        int size = session.getAllTrackables(Plane.class).size();
        Object[] planes = session.getAllTrackables(Plane.class).toArray();

        int rng;
        Plane plane;
        do{
            rng = rand.nextInt(size);
            plane = (Plane) planes[rng];
        } while (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING);


        randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );

        randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

        pose = plane.getCenterPose();

        translation = pose.getTranslation();
        translation[0] += randomX;
        translation[2] += randomZ;

        rotation = pose.getRotationQuaternion();

        pose = new Pose(translation, rotation);

        anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, woodenColor));
    }

    // Anchors created from taps used for object placing with a given color.
    private static class ColoredAnchor {
        public final Anchor anchor;
        public final float[] color;

        public ColoredAnchor(Anchor a, float[] color4f) {
            this.anchor = a;
            this.color = color4f;
        }
    }

    private final ArrayList<ColoredAnchor> anchors = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rand = new Random();

        setContentView(R.layout.activity_main);

        initInventory();

        mToggleButton = findViewById(R.id.toggleButton);
        mToggleButton.setVisibility(View.GONE);

        mTerminate = findViewById(R.id.terminate);
        mTerminate.setVisibility(View.GONE);

        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        // Set up tap listener.
        tapHelper = new TapHelper(/*context=*/ this);
        surfaceView.setOnTouchListener(tapHelper);

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surfaceView.setWillNotDraw(false);

        installRequested = false;

    }

    private void initInventory(){
        mInventory = findViewById(R.id.inventory);
        mInventory.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // handle desired action here
                // One possibility of action is to replace the contents above the nav bar
                // return true if you want the item to be displayed as the selected item

                Log.e(TAG, item.getTitle().toString());
                Toast.makeText(MainActivity.this, item.getTitle().toString(), Toast.LENGTH_SHORT).show();
                //if(item.isVisible()) item.setVisible(false);
                //else item.setVisible(true);

                return true;
            }
        });
        mInventory.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                // Create the session.
                session = new Session(/* context= */ this);

            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            messageSnackbarHelper.showError(this, "Camera not available. Please restart the app.");
            session = null;
            return;
        }

        surfaceView.onResume();

        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(/*context=*/ this);
            planeRenderer.createOnGlThread(/*context=*/ this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(/*context=*/ this);

            virtualKey.createOnGlThread(/*context=*/ this, "models/key.obj", "models/key.png");
            virtualKey.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

//            virtualCrowbar.createOnGlThread(/*context=*/ this, "models/crowbar.obj", "models/white.png");
//            virtualCrowbar.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualSideTable.createOnGlThread(/*context=*/ this, "models/sidetable.obj", "models/white.png");
            virtualSideTable.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualTreasureTrunk.createOnGlThread(/*context=*/ this, "models/treasuretrunk.obj", "models/white.png");
            virtualTreasureTrunk.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

//            virtualCornerTable.createOnGlThread(/*context=*/ this, "models/vintagecornertable.obj", "models/white.png");
//            virtualCornerTable.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualWooden.createOnGlThread(/*context=*/ this, "models/wooden.obj", "models/white.png");
            virtualWooden.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            // Handle one tap per frame.
            //handleTap(frame, camera);

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame);

            // If not tracking, don't draw 3D objects, show tracking failure reason instead.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                messageSnackbarHelper.showMessage(
                        this, TrackingStateHelper.getTrackingFailureReasonString(camera));
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            // Visualize tracked points.
            // Use try-with-resources to automatically release the point cloud.
            if(displayPlane){
                try (PointCloud pointCloud = frame.acquirePointCloud()) {
                    pointCloudRenderer.update(pointCloud);
                    pointCloudRenderer.draw(viewmtx, projmtx);
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (hasTrackingPlane() && !isHideForever)
                        mTerminate.setVisibility(View.VISIBLE);
                    else
                        mTerminate.setVisibility(View.GONE);
                }
            });

            // No tracking error at this point. If we detected any plane, then hide the
            // message UI, otherwise show searchingPlane message.
            if (hasTrackingPlane()) {
                messageSnackbarHelper.hide(this);
            } else {
                messageSnackbarHelper.showMessage(this, SEARCHING_PLANE_MESSAGE);
            }

            // Visualize planes.
            if(displayPlane) {
                planeRenderer.drawPlanes(
                        session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);
            }

            // Visualize anchors created by touch.
            for (ColoredAnchor coloredAnchor : anchors) {
                if (coloredAnchor.anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                coloredAnchor.anchor.getPose().toMatrix(anchorMatrix, 0);

                // Update and draw the model.

//                if(Arrays.equals(coloredAnchor.color, crowbarColor)){
//                    virtualCrowbar.updateModelMatrix(anchorMatrix, 0.1f);
//                    virtualCrowbar.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
//                }

                if(Arrays.equals(coloredAnchor.color, keyColor)){
                    virtualKey.updateModelMatrix(anchorMatrix, 0.001f);
                    virtualKey.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                }

                if(Arrays.equals(coloredAnchor.color, sideTableColor)){
                    virtualSideTable.updateModelMatrix(anchorMatrix, 2.0f);
                    virtualSideTable.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                }

                if(Arrays.equals(coloredAnchor.color, treasureTrunkColor)){
                    virtualTreasureTrunk.updateModelMatrix(anchorMatrix, 0.05f);
                    virtualTreasureTrunk.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                }

//                if(Arrays.equals(coloredAnchor.color, cornerTableColor)){
//                    virtualCornerTable.updateModelMatrix(anchorMatrix, 0.5f);
//                    virtualCornerTable.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
//                }

                if(Arrays.equals(coloredAnchor.color, woodenColor)){
                    virtualWooden.updateModelMatrix(anchorMatrix, 2.0f);
                    virtualWooden.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                }
            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleTap(Frame frame, Camera camera) {
        MotionEvent tap = tapHelper.poll();
        if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
            for (HitResult hit : frame.hitTest(tap)) {
                // Check if any plane was hit, and if it was hit inside the plane polygon
                Trackable trackable = hit.getTrackable();
                // Creates an anchor if a plane or an oriented point was hit.
                if ((trackable instanceof Plane
                        && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                        && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                        || (trackable instanceof Point
                        && ((Point) trackable).getOrientationMode()
                        == OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                    // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
                    // Cap the number of objects created. This avoids overloading both the
                    // rendering system and ARCore.
                    if (anchors.size() >= 20) {
                        anchors.get(0).anchor.detach();
                        anchors.remove(0);
                    }

                    // Assign a color to the object for rendering based on the trackable type
                    // this anchor attached to. For AR_TRACKABLE_POINT, it's blue color, and
                    // for AR_TRACKABLE_PLANE, it's green color.
                    float[] objColor;
                    if (trackable instanceof Point) {
                        objColor = new float[] {66.0f, 133.0f, 244.0f, 255.0f};
                    } else if (trackable instanceof Plane) {
                        objColor = new float[] {139.0f, 195.0f, 74.0f, 255.0f};
                    } else {
                        objColor = DEFAULT_COLOR;
                    }

                    // Adding an Anchor tells ARCore that it should track this position in
                    // space. This anchor is created on the Plane to place the 3D model
                    // in the correct position relative both to the world and to the plane.
                    anchors.add(new ColoredAnchor(hit.createAnchor(), objColor));

                    break;
                }
            }
        }
    }

    /** Checks if we detected at least one plane. */
    private boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }
}
