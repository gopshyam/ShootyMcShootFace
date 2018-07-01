package com.example.carbo.shootymcshootface;

import android.graphics.PointF;
import android.util.Log;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.util.List;


class FaceFeatures {
    //Class that stores all the features of a target face, may also be used to check if a face is similar - Face Recognition.

    private static final String TAG = "FaceFeatures";
    private PointF bottomMouth, leftCheek, leftEar, leftEarTip, leftEye, leftMouth, noseBase, rightCheek, rightEar, rightEarTip, rightEye, rightMouth;
    private Double[] ratioList = new Double[4];
    private Double threshold = 0.50;


    public FaceFeatures(Face face) {
        List<Landmark> landmarkList = face.getLandmarks();
        Log.e(TAG, landmarkList.toString());
        for (Landmark landmark : landmarkList) {
            switch (landmark.getType()) {
                case Landmark.BOTTOM_MOUTH:
                    bottomMouth = landmark.getPosition();
                    Log.e(TAG, "Bottom Mouth Found");
                    break;
                case Landmark.LEFT_CHEEK:
                    leftCheek = landmark.getPosition();
                    Log.e(TAG, "Left Cheek Found");
                    break;
                case Landmark.LEFT_EAR:
                    leftEar = landmark.getPosition();
                    Log.e(TAG, "Left Ear Found");
                    break;
                case Landmark.LEFT_EAR_TIP:
                    leftEarTip = landmark.getPosition();
                    Log.e(TAG, "Left Ear Tip Found");
                    break;
                case Landmark.LEFT_EYE:
                    leftEye = landmark.getPosition();
                    Log.e(TAG, "Left Eye Found");
                    break;
                case Landmark.LEFT_MOUTH:
                    leftMouth = landmark.getPosition();
                    Log.e(TAG, "Left Mouth Found");
                    break;
                case Landmark.NOSE_BASE:
                    noseBase = landmark.getPosition();
                    Log.e(TAG, "Nose Base Found");
                    break;
                case Landmark.RIGHT_CHEEK:
                    rightCheek = landmark.getPosition();
                    Log.e(TAG, "Right cheek Found");
                    break;
                case Landmark.RIGHT_EAR:
                    rightEar = landmark.getPosition();
                    Log.e(TAG, "Right Ear Found");
                    break;
                case Landmark.RIGHT_EAR_TIP:
                    rightEarTip = landmark.getPosition();
                    Log.e(TAG, "Right Ear Tip Found");
                    break;
                case Landmark.RIGHT_EYE:
                    rightEye = landmark.getPosition();
                    Log.e(TAG, "Right Eye Found");
                    break;
                case Landmark.RIGHT_MOUTH:
                    rightMouth = landmark.getPosition();
                    Log.e(TAG, "Right Mout Found");
                    break;
            }
        }
        calculateRatios();
    }

    /*public void resetFace(Face face) {
        List<Landmark> landmarkList = face.getLandmarks();
        for (Landmark landmark: landmarkList) {
            switch(landmark.getType()) {
                case Landmark.BOTTOM_MOUTH:
                    bottomMouth = landmark.getPosition();
                    break;
                case Landmark.LEFT_CHEEK:
                    leftCheek = landmark.getPosition();
                    break;
                case Landmark.LEFT_EAR:
                    leftEar = landmark.getPosition();
                    break;
                case Landmark.LEFT_EAR_TIP:
                    leftEarTip = landmark.getPosition();
                    break;
                case Landmark.LEFT_EYE:
                    leftEye = landmark.getPosition();
                    break;
                case Landmark.LEFT_MOUTH:
                    leftMouth = landmark.getPosition();
                    break;
                case Landmark.NOSE_BASE:
                    noseBase = landmark.getPosition();
                    break;
                case Landmark.RIGHT_CHEEK:
                    rightCheek = landmark.getPosition();
                    break;
                case Landmark.RIGHT_EAR:
                    rightEar = landmark.getPosition();
                    break;
                case Landmark.RIGHT_EAR_TIP:
                    rightEarTip = landmark.getPosition();
                    break;
                case Landmark.RIGHT_EYE:
                    rightEye = landmark.getPosition();
                    break;
                case Landmark.RIGHT_MOUTH:
                    rightMouth = landmark.getPosition();
                    break;
            }
        }
        calculateRatios();
        Log.e(TAG, ratioList.toString());
    }*/

    private double distance(PointF p1, PointF p2) {
        if (p1 == null || p2 == null) {
            return 0;
        }
        return Math.hypot(p1.x - p2.x, p1.y - p2.y);
    }

    private void calculateRatios() {
        double ratioBase = distance(rightEye, leftEye);
        double ratio1 = distance(rightEye, noseBase) / ratioBase;
        double ratio2 = distance(leftEye, noseBase) / ratioBase;
        double ratio3 = distance(rightEye, bottomMouth) / ratioBase;
        double ratio4 = distance(leftEye, bottomMouth) / ratioBase;

        ratioList[0] = ratio1;
        ratioList[1] = ratio2;
        ratioList[2] = ratio3;
        ratioList[3] = ratio4;
    }

    private Double[] getRatioList() {
        return ratioList;
    }

    public boolean compareFace(FaceFeatures face) {
        //Check a given FaceFeatures instance with this face
        Double[] newRatios = face.getRatioList();
        double diffsum = 0;

        for (int i = 0; i < 4; i++) {
            diffsum += ratioList[i] - newRatios[i];
        }
        Log.e("DIFFSUM", String.valueOf(diffsum));

        if (diffsum < threshold) {
            return true;
        } else {
            return false;
        }
    }


}
