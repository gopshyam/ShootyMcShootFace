package com.example.carbo.shootymcshootface;

import com.google.android.gms.vision.face.Face;

/**
 * Created by carbo on 1/13/2017.
 */

public class ShootFace {
    private Face face;
    private int faceId;

    public Face getFace() {
        return face;
    }

    public void setFace(Face face) {
        this.face = face;
    }

    public int getFaceId() {
        return faceId;
    }

    public void setFaceId(int faceId) {
        this.faceId = faceId;
    }
}
