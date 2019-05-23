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
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.common.helpers.CameraPermissionHelper;
import com.example.common.helpers.DisplayRotationHelper;
import com.example.common.helpers.FullScreenHelper;
import com.example.common.helpers.SnackbarHelper;
import com.example.common.helpers.TapHelper;
import com.example.common.rendering.BackgroundRenderer;
import com.example.common.rendering.ObjectRenderer;
import com.example.common.rendering.PlaneRenderer;
import com.example.common.rendering.PointCloudRenderer;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
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
    private TextView mNoObject;
    private boolean isHide = false;
    private boolean isHideForever = false;
    private boolean displayPlane = true;
    private boolean isYellowKeyTaken = false;
    private boolean isBlueKeyTaken = false;
    private boolean isGreenKeyTaken = false;
    private boolean isYellowChestOpen = false;
    private boolean isBlueChestOpen = false;
    private boolean isGreenChestOpen = false;

    //Objects color
    private static final float[] crowbarColor = {75.0f, 0.0f, 0.0f, 255.0f};
    private static final float[] yellowKeyColor = {252.0f, 220.0f, 18.0f, 255.0f};
    private static final float[] blueKeyColor = {0.0f, 0.0f, 255.0f, 255.0f};
    private static final float[] greenKeyColor = {0.0f, 150.0f, 0.0f, 255.0f};
    private static final float[] sideTableColor = {120.0f, 84.0f, 71.0f, 255.0f};
    private static final float[] yellowTreasureTrunkColor = {253.0f, 220.0f, 18.0f, 255.0f};
    private static final float[] blueTreasureTrunkColor = {1.0f, 0.0f, 255.0f, 255.0f};
    private static final float[] greenTreasureTrunkColor = {1.0f, 150.0f, 0.0f, 255.0f};
    private static final float[] cornerTableColor = {122.0f, 84.0f, 71.0f, 255.0f};
    private static final float[] woodenColor = {123.0f, 84.0f, 71.0f, 255.0f};

    //Objects pose
    private static Pose crowbarPose;
    private static Pose yellowKeyPose;
    private static Pose blueKeyPose;
    private static Pose greenKeyPose;
    private static Pose sideTablePose;
    private static Pose yellowTreasureTrunkPose;
    private static Pose blueTreasureTrunkPose;
    private static Pose greenTreasureTrunkPose;
    private static Pose cornerTablePose;
    private static Pose woodenPose;

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

    private static final String SEARCHING_PLANE_MESSAGE = "Veuillez scanner la pièce puis cliquez sur \"Terminer\"";

    public void onMenuClick(View view) {
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

        placeObjects();

        displayPlane = false;
    }

    private void placeObjects() {
        placeYellowKey(); //ok
        placeBlueKey(); //ok
        placeGreenKey(); //ok
        //placeCrowbar(); //ok
        //placeSideTable(); //ameliorer le placement random
        placeYellowTreasureTrunk(); //ok
        placeBlueTreasureTrunk(); //ok
        placeGreenTreasureTrunk(); //ok
        //placeCornerTable(); //pas de texture
        //placeWooden(); //Pas très plat
    }

    private void placeYellowKey() {
        Plane[] planes = session.getAllTrackables(Plane.class).toArray(new Plane[0]);

        int rng;
        Plane plane;
        float minX = 0.5f;
        float minZ = 0.5f;
        do{
            rng = rand.nextInt(planes.length);
            plane = planes[rng];
        } while ( !(plane.getType().equals(Plane.Type.HORIZONTAL_UPWARD_FACING))
                || !(plane.getTrackingState().equals(TrackingState.TRACKING))
                || (plane.getExtentX() < minX)
                || (plane.getExtentZ() < minZ)
                || !plane.isPoseInPolygon(plane.getCenterPose()));

        Pose pose;
        float[] translation;
        float[] rotation;
        float randomX;
        float randomZ;
        boolean isAlone;
        do{
            randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );
            randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

            if(randomX > 0) randomX -= 0.1;
            if(randomX < 0) randomX += 0.1;
            if(randomZ > 0) randomZ -= 0.1;
            if(randomZ < 0) randomZ += 0.1;

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

            isAlone = true;
            for(Anchor anchor : plane.getAnchors()){
                if(getDistance(anchor.getPose(), pose) < 0.3){
                    isAlone = false;
                    break;
                }
            }
        } while (!plane.isPoseInPolygon(pose) || !isAlone);

        yellowKeyPose = pose;
        Anchor anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, yellowKeyColor));
    }

    private void placeBlueKey() {
        Plane[] planes = session.getAllTrackables(Plane.class).toArray(new Plane[0]);

        int rng;
        Plane plane;
        float minX = 0.5f;
        float minZ = 0.5f;
        do{
            rng = rand.nextInt(planes.length);
            plane = planes[rng];
        } while ( !(plane.getType().equals(Plane.Type.HORIZONTAL_UPWARD_FACING))
                || !(plane.getTrackingState().equals(TrackingState.TRACKING))
                || (plane.getExtentX() < minX)
                || (plane.getExtentZ() < minZ)
                || !plane.isPoseInPolygon(plane.getCenterPose()));

        Pose pose;
        float[] translation;
        float[] rotation;
        float randomX;
        float randomZ;
        boolean isAlone;
        do{
            randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );
            randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

            if(randomX > 0) randomX -= 0.1;
            if(randomX < 0) randomX += 0.1;
            if(randomZ > 0) randomZ -= 0.1;
            if(randomZ < 0) randomZ += 0.1;

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

            isAlone = true;
            for(Anchor anchor : plane.getAnchors()){
                if(getDistance(anchor.getPose(), pose) < 0.3){
                    isAlone = false;
                    break;
                }
            }
        } while (!plane.isPoseInPolygon(pose) || !isAlone);

        blueKeyPose = pose;
        Anchor anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, blueKeyColor));
    }

    private void placeGreenKey() {
        Plane[] planes = session.getAllTrackables(Plane.class).toArray(new Plane[0]);

        int rng;
        Plane plane;
        float minX = 0.5f;
        float minZ = 0.5f;
        do{
            rng = rand.nextInt(planes.length);
            plane = planes[rng];
        } while ( !(plane.getType().equals(Plane.Type.HORIZONTAL_UPWARD_FACING))
                || !(plane.getTrackingState().equals(TrackingState.TRACKING))
                || (plane.getExtentX() < minX)
                || (plane.getExtentZ() < minZ)
                || !plane.isPoseInPolygon(plane.getCenterPose()));

        Pose pose;
        float[] translation;
        float[] rotation;
        float randomX;
        float randomZ;
        boolean isAlone;
        do{
            randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );
            randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

            if(randomX > 0) randomX -= 0.1;
            if(randomX < 0) randomX += 0.1;
            if(randomZ > 0) randomZ -= 0.1;
            if(randomZ < 0) randomZ += 0.1;

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

            isAlone = true;
            for(Anchor anchor : plane.getAnchors()){
                if(getDistance(anchor.getPose(), pose) < 0.3){
                    isAlone = false;
                    break;
                }
            }
        } while (!plane.isPoseInPolygon(pose) || !isAlone);

        greenKeyPose = pose;
        Anchor anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, greenKeyColor));
    }

    private void placeCrowbar() {
        Plane[] planes = session.getAllTrackables(Plane.class).toArray(new Plane[0]);

        int rng;
        Plane plane;
        float minX = 2;
        float minZ = 2;
        do{
            rng = rand.nextInt(planes.length);
            plane = planes[rng];
        } while ( !(plane.getType().equals(Plane.Type.HORIZONTAL_UPWARD_FACING))
                || !(plane.getTrackingState().equals(TrackingState.TRACKING))
                || (plane.getExtentX() < minX)
                || (plane.getExtentZ() < minZ)
                || !plane.isPoseInPolygon(plane.getCenterPose()));

        Pose pose;
        float[] translation;
        float[] rotation;
        float randomX;
        float randomZ;
        boolean isAlone;
        do{
            randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );
            randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

            pose = plane.getCenterPose();

            if(randomX > 0) randomX -= 0.2;
            if(randomX < 0) randomX += 0.2;
            if(randomZ > 0) randomZ -= 0.2;
            if(randomZ < 0) randomZ += 0.2;

            translation = pose.getTranslation();
            translation[0] += randomX;
            translation[2] += randomZ;

            rotation = pose.getRotationQuaternion();
            rotation[0] = 90;
            rotation[1] = 0;
            rotation[2] = 0;
            rotation[3] = 90;

            pose = new Pose(translation, rotation);

            isAlone = true;
            for(Anchor anchor : plane.getAnchors()){
                if(getDistance(anchor.getPose(), pose) < 0.3){
                    isAlone = false;
                    break;
                }
            }
        } while (!plane.isPoseInPolygon(pose) || !isAlone);

        Anchor anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, crowbarColor));
    }

    private void placeSideTable() {
        Plane[] planes = session.getAllTrackables(Plane.class).toArray(new Plane[0]);

        int rng;
        Plane plane;
        float minX = 3;
        float minZ = 3;
        do{
            rng = rand.nextInt(planes.length);
            plane = planes[rng];
        } while ( !(plane.getType().equals(Plane.Type.HORIZONTAL_UPWARD_FACING))
                || !(plane.getTrackingState().equals(TrackingState.TRACKING))
                || (plane.getExtentX() < minX)
                || (plane.getExtentZ() < minZ)
                || !plane.isPoseInPolygon(plane.getCenterPose()));

        Pose pose;
        float[] translation;
        float[] rotation;
        float randomX;
        float randomZ;
        do{
            randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );
            randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

            pose = plane.getCenterPose();

            translation = pose.getTranslation();
            translation[0] += randomX;
            translation[2] += randomZ;

            rotation = pose.getRotationQuaternion();

            pose = new Pose(translation, rotation);
        } while (!plane.isPoseInPolygon(pose));

        Anchor anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, sideTableColor));
    }

    private void placeYellowTreasureTrunk() {
        Plane[] planes = session.getAllTrackables(Plane.class).toArray(new Plane[0]);

        int rng;
        Plane plane;
        float minX = 2;
        float minZ = 2;
        do{
            rng = rand.nextInt(planes.length);
            plane = planes[rng];
        } while ( !(plane.getType().equals(Plane.Type.HORIZONTAL_UPWARD_FACING))
                || !(plane.getTrackingState().equals(TrackingState.TRACKING))
                || (plane.getExtentX() < minX)
                || (plane.getExtentZ() < minZ)
                || !plane.isPoseInPolygon(plane.getCenterPose()));

        Pose pose;
        float[] translation;
        float[] rotation;
        float randomX;
        float randomZ;
        boolean isAlone;
        do{
            randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );
            randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

            if(randomX > 0) randomX -= 0.25;
            if(randomX < 0) randomX += 0.25;
            if(randomZ > 0) randomZ -= 0.25;
            if(randomZ < 0) randomZ += 0.25;

            pose = plane.getCenterPose();

            translation = pose.getTranslation();
            translation[0] += randomX;
            translation[2] += randomZ;

            rotation = pose.getRotationQuaternion();

            pose = new Pose(translation, rotation);

            isAlone = true;
            for(Anchor anchor : plane.getAnchors()){
                if(getDistance(anchor.getPose(), pose) < 0.3){
                    isAlone = false;
                    break;
                }
            }
        } while (!plane.isPoseInPolygon(pose) || !isAlone);

        yellowTreasureTrunkPose = pose;
        Anchor anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, yellowTreasureTrunkColor));
    }

    private void placeBlueTreasureTrunk() {
        Plane[] planes = session.getAllTrackables(Plane.class).toArray(new Plane[0]);

        int rng;
        Plane plane;
        float minX = 2;
        float minZ = 2;
        do{
            rng = rand.nextInt(planes.length);
            plane = planes[rng];
        } while ( !(plane.getType().equals(Plane.Type.HORIZONTAL_UPWARD_FACING))
                || !(plane.getTrackingState().equals(TrackingState.TRACKING))
                || (plane.getExtentX() < minX)
                || (plane.getExtentZ() < minZ)
                || !plane.isPoseInPolygon(plane.getCenterPose()));

        Pose pose;
        float[] translation;
        float[] rotation;
        float randomX;
        float randomZ;
        boolean isAlone;
        do{
            randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );
            randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

            if(randomX > 0) randomX -= 0.25;
            if(randomX < 0) randomX += 0.25;
            if(randomZ > 0) randomZ -= 0.25;
            if(randomZ < 0) randomZ += 0.25;

            pose = plane.getCenterPose();

            translation = pose.getTranslation();
            translation[0] += randomX;
            translation[2] += randomZ;

            rotation = pose.getRotationQuaternion();

            pose = new Pose(translation, rotation);

            isAlone = true;
            for(Anchor anchor : plane.getAnchors()){
                if(getDistance(anchor.getPose(), pose) < 0.3){
                    isAlone = false;
                    break;
                }
            }
        } while (!plane.isPoseInPolygon(pose) || !isAlone);

        blueTreasureTrunkPose = pose;
        Anchor anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, blueTreasureTrunkColor));
    }

    private void placeGreenTreasureTrunk() {
        Plane[] planes = session.getAllTrackables(Plane.class).toArray(new Plane[0]);

        int rng;
        Plane plane;
        float minX = 2;
        float minZ = 2;
        do{
            rng = rand.nextInt(planes.length);
            plane = planes[rng];
        } while ( !(plane.getType().equals(Plane.Type.HORIZONTAL_UPWARD_FACING))
                || !(plane.getTrackingState().equals(TrackingState.TRACKING))
                || (plane.getExtentX() < minX)
                || (plane.getExtentZ() < minZ)
                || !plane.isPoseInPolygon(plane.getCenterPose()));

        Pose pose;
        float[] translation;
        float[] rotation;
        float randomX;
        float randomZ;
        boolean isAlone;
        do{
            randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );
            randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

            if(randomX > 0) randomX -= 0.25;
            if(randomX < 0) randomX += 0.25;
            if(randomZ > 0) randomZ -= 0.25;
            if(randomZ < 0) randomZ += 0.25;

            pose = plane.getCenterPose();

            translation = pose.getTranslation();
            translation[0] += randomX;
            translation[2] += randomZ;

            rotation = pose.getRotationQuaternion();

            pose = new Pose(translation, rotation);

            isAlone = true;
            for(Anchor anchor : plane.getAnchors()){
                if(getDistance(anchor.getPose(), pose) < 0.3){
                    isAlone = false;
                    break;
                }
            }
        } while (!plane.isPoseInPolygon(pose) || !isAlone);

        greenTreasureTrunkPose = pose;
        Anchor anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, greenTreasureTrunkColor));
    }

    private void placeCornerTable() {
        int size = session.getAllTrackables(Plane.class).size();
        Object[] planes = session.getAllTrackables(Plane.class).toArray();

        int rng;
        Plane plane;
        do{
            rng = rand.nextInt(size);
            plane = (Plane) planes[rng];
        } while (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING);


        float randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );

        float randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

        Pose pose = plane.getCenterPose();

        float[] translation = pose.getTranslation();
        translation[0] += randomX;
        translation[1] += 0.9f;
        translation[2] += randomZ;

        float[] rotation = pose.getRotationQuaternion();

        pose = new Pose(translation, rotation);

        Anchor anchor = plane.createAnchor(pose);
        anchors.add(new ColoredAnchor(anchor, cornerTableColor));
    }

    private void placeWooden() {
        Plane[] planes = session.getAllTrackables(Plane.class).toArray(new Plane[0]);

        int rng;
        Plane plane;
        float minX = 1;
        float minZ = 1;
        do{
            rng = rand.nextInt(planes.length);
            plane = planes[rng];
        } while ( !(plane.getType().equals(Plane.Type.HORIZONTAL_UPWARD_FACING))
                || !(plane.getTrackingState().equals(TrackingState.TRACKING))
                || (plane.getExtentX() < minX)
                || (plane.getExtentZ() < minZ)
                || !plane.isPoseInPolygon(plane.getCenterPose()));

        Pose pose;
        float[] translation;
        float[] rotation;
        float randomX;
        float randomZ;
        boolean isAlone;
        do{
            randomX = (plane.getExtentX() * rand.nextFloat()) - ( plane.getExtentX() / 2 );
            randomZ = (plane.getExtentZ() * rand.nextFloat()) - ( plane.getExtentZ() / 2 );

            pose = plane.getCenterPose();

            if(randomX > 0) randomX -= 0.3;
            if(randomX < 0) randomX += 0.3;
            if(randomZ > 0) randomZ -= 0.3;
            if(randomZ < 0) randomZ += 0.3;

            translation = pose.getTranslation();
            translation[0] += randomX;
            translation[2] += randomZ;

            rotation = pose.getRotationQuaternion();

            pose = new Pose(translation, rotation);

            isAlone = true;
            for(Anchor anchor : plane.getAnchors()){
                if(getDistance(anchor.getPose(), pose) < 0.3){
                    isAlone = false;
                    break;
                }
            }

        } while (!plane.isPoseInPolygon(pose) || !isAlone);

        Anchor anchor = plane.createAnchor(pose);
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

        mNoObject = findViewById(R.id.noObjects);
        mNoObject.setVisibility(View.VISIBLE);

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
        MenuItem yellowKey = mInventory.getMenu().findItem(R.id.inventory_yellow_key);
        MenuItem blueKey = mInventory.getMenu().findItem(R.id.inventory_blue_key);
        MenuItem greenKey = mInventory.getMenu().findItem(R.id.inventory_green_key);

        yellowKey.setVisible(false);
        blueKey.setVisible(false);
        greenKey.setVisible(false);
        yellowKey.setChecked(false);
        blueKey.setChecked(false);
        greenKey.setChecked(false);
        mInventory.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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

            virtualKey.createOnGlThread(/*context=*/ this, "models/key.obj", "models/white.png");
            virtualKey.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualCrowbar.createOnGlThread(/*context=*/ this, "models/crowbar2.obj", "models/white.png");
            virtualCrowbar.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualSideTable.createOnGlThread(/*context=*/ this, "models/sidetable.obj", "models/white.png");
            virtualSideTable.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualTreasureTrunk.createOnGlThread(/*context=*/ this, "models/treasuretrunk.obj", "models/white.png");
            virtualTreasureTrunk.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualCornerTable.createOnGlThread(/*context=*/ this, "models/vintagecornertable.obj", "models/white.png");
            virtualCornerTable.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

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
            handleTap(frame, camera);

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
                    if (hasLargePlane(2, 2) && !isHideForever)
                        mTerminate.setVisibility(View.VISIBLE);
                    else mTerminate.setVisibility(View.GONE);

                    if(mInventory.getMenu().hasVisibleItems()) mNoObject.setVisibility(View.GONE);
                    else mNoObject.setVisibility(View.VISIBLE);
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

                if(Arrays.equals(coloredAnchor.color, crowbarColor)){
                    virtualCrowbar.updateModelMatrix(anchorMatrix, 0.002f);
                    virtualCrowbar.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                }
                else if(Arrays.equals(coloredAnchor.color, yellowKeyColor)){
                    if(!isYellowKeyTaken) {
                        virtualKey.updateModelMatrix(anchorMatrix, 0.001f);
                        virtualKey.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                    }
                }
                else if(Arrays.equals(coloredAnchor.color, blueKeyColor)){
                    if(!isBlueKeyTaken) {
                        virtualKey.updateModelMatrix(anchorMatrix, 0.001f);
                        virtualKey.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                    }
                }
                else if(Arrays.equals(coloredAnchor.color, greenKeyColor)){
                    if(!isGreenKeyTaken) {
                        virtualKey.updateModelMatrix(anchorMatrix, 0.001f);
                        virtualKey.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                    }
                }
                else if(Arrays.equals(coloredAnchor.color, sideTableColor)){
                    virtualSideTable.updateModelMatrix(anchorMatrix, 2.0f);
                    virtualSideTable.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                }
                else if(Arrays.equals(coloredAnchor.color, yellowTreasureTrunkColor)){
                    virtualTreasureTrunk.updateModelMatrix(anchorMatrix, 0.03f);
                    virtualTreasureTrunk.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                }
                else if(Arrays.equals(coloredAnchor.color, blueTreasureTrunkColor)){
                    virtualTreasureTrunk.updateModelMatrix(anchorMatrix, 0.03f);
                    virtualTreasureTrunk.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                }
                else if(Arrays.equals(coloredAnchor.color, greenTreasureTrunkColor)){
                    virtualTreasureTrunk.updateModelMatrix(anchorMatrix, 0.03f);
                    virtualTreasureTrunk.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                }
                else if(Arrays.equals(coloredAnchor.color, cornerTableColor)){
                    virtualCornerTable.updateModelMatrix(anchorMatrix, 0.5f);
                    virtualCornerTable.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                }
                else if(Arrays.equals(coloredAnchor.color, woodenColor)){
                    virtualWooden.updateModelMatrix(anchorMatrix, 1.8f);
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

                if(getDistance(hit.getHitPose(), yellowKeyPose) <= 0.1){
                    Log.e(TAG, "Clef clické : " + getDistance(hit.getHitPose(), yellowKeyPose));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayToast("Clef jaune récupérée !");
                            mInventory.getMenu().findItem(R.id.inventory_yellow_key).setVisible(true).setChecked(true);
                        }
                    });

                    isYellowKeyTaken = true;
                    break;
                }
                else if (getDistance(hit.getHitPose(), blueKeyPose) <= 0.1) {
                    Log.e(TAG, "Clef clické : " + getDistance(hit.getHitPose(), blueKeyPose));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayToast("Clef bleue récupérée !");
                            mInventory.getMenu().findItem(R.id.inventory_blue_key).setVisible(true).setChecked(true);
                        }
                    });

                    isBlueKeyTaken = true;
                    break;
                }
                else if (getDistance(hit.getHitPose(), greenKeyPose) <= 0.1) {
                    Log.e(TAG, "Clef clické : " + getDistance(hit.getHitPose(), greenKeyPose));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayToast("Clef verte récupérée !");
                            mInventory.getMenu().findItem(R.id.inventory_green_key).setVisible(true).setChecked(true);
                        }
                    });

                    isGreenKeyTaken = true;
                    break;
                }
                else if(getDistance(hit.getHitPose(), yellowTreasureTrunkPose) <= 0.25){
                    Log.e(TAG, "Coffre clické : " + getDistance(hit.getHitPose(), yellowTreasureTrunkPose));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mInventory.getMenu().findItem(R.id.inventory_yellow_key).isChecked() && mInventory.getMenu().findItem(R.id.inventory_yellow_key).isVisible()){
                                displayToast("Félicitations !\nLe coffre jaune est maintenant ouvert !");
                                mInventory.getMenu().findItem(R.id.inventory_yellow_key).setVisible(false).setChecked(false);
                                for(int i = 0; i < mInventory.getMenu().size(); ++i){
                                    if(mInventory.getMenu().getItem(i).isVisible()){
                                        mInventory.getMenu().getItem(i).setChecked(true);
                                        break;
                                    }
                                }
                                isYellowChestOpen = true;
                                ckeckWin();
                            }
                            else if(isYellowChestOpen) displayToast("Ce coffre est déjà ouvert !");
                            else displayToast("Ce coffre nécessite la clef jaune !");
                        }
                    });

                    break;
                }
                else if(getDistance(hit.getHitPose(), blueTreasureTrunkPose) <= 0.25){
                    Log.e(TAG, "Coffre clické : " + getDistance(hit.getHitPose(), blueTreasureTrunkPose));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mInventory.getMenu().findItem(R.id.inventory_blue_key).isChecked() && mInventory.getMenu().findItem(R.id.inventory_blue_key).isVisible()){
                                displayToast("Félicitations !\nLe coffre bleue est maintenant ouvert !");
                                mInventory.getMenu().findItem(R.id.inventory_blue_key).setVisible(false).setChecked(false);
                                for(int i = 0; i < mInventory.getMenu().size(); ++i){
                                    if(mInventory.getMenu().getItem(i).isVisible()){
                                        mInventory.getMenu().getItem(i).setChecked(true);
                                        break;
                                    }
                                }
                                isBlueChestOpen = true;
                                ckeckWin();
                            }
                            else if(isBlueChestOpen) displayToast("Ce coffre est déjà ouvert !");
                            else displayToast("Ce coffre nécessite la clef bleue !");
                        }
                    });

                    break;
                }
                else if(getDistance(hit.getHitPose(), greenTreasureTrunkPose) <= 0.25){
                    Log.e(TAG, "Coffre clické : " + getDistance(hit.getHitPose(), greenTreasureTrunkPose));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mInventory.getMenu().findItem(R.id.inventory_green_key).isChecked() && mInventory.getMenu().findItem(R.id.inventory_green_key).isVisible()){
                                displayToast("Félicitations !\nLe coffre vert est maintenant ouvert !");
                                mInventory.getMenu().findItem(R.id.inventory_green_key).setVisible(false).setChecked(false);
                                for(int i = 0; i < mInventory.getMenu().size(); ++i){
                                    if(mInventory.getMenu().getItem(i).isVisible()){
                                        mInventory.getMenu().getItem(i).setChecked(true);
                                        break;
                                    }
                                }
                                isGreenChestOpen = true;
                                ckeckWin();
                            }
                            else if(isGreenChestOpen) displayToast("Ce coffre est déjà ouvert !");
                            else displayToast("Ce coffre nécessite la clef verte !");
                        }
                    });

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

    /** Checks if we detected at least one large plane. */
    private boolean hasLargePlane(float minX, float minZ) {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getExtentX() > minX && plane.getExtentZ() > minZ) return true;
        }
        return false;
    }

    private double getDistance(Pose pose0, Pose pose1){
        float dx = pose0.tx() - pose1.tx();
        float dy = pose0.ty() - pose1.ty();
        float dz = pose0.tz() - pose1.tz();
        return Math.sqrt(dx * dx + dz * dz + dy * dy);
    }

    private void ckeckWin() {
        if(isYellowChestOpen && isBlueChestOpen && isGreenChestOpen)
            displayToast("Félicitations !\nTous les coffres sont ouverts !");
    }

    private void displayToast(String s){
        Toast toast = Toast.makeText(this, s, Toast.LENGTH_LONG);
        TextView v = toast.getView().findViewById(android.R.id.message);
        if( v != null) v.setGravity(Gravity.CENTER);
        toast.setGravity(Gravity.CENTER, 0 , 0);
        toast.show();
    }
}
