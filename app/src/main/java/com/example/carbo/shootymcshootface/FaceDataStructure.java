package com.example.carbo.shootymcshootface;

import com.google.android.gms.vision.face.Face;

import java.util.HashMap;

/**
 * Created by carbo on 1/13/2017.
 */

public class FaceDataStructure {
    private HashMap<Integer, Face> faceHashMap;

    public FaceDataStructure() {
        faceHashMap = new HashMap<Integer, Face>();
    }

    public void newItem(Face face, int faceId) {
        faceHashMap.put(faceId, face);
    }

    public void update(Face face, int faceId) {
        faceHashMap.put(faceId, face);
    }

    public void remove(int faceId) {
        faceHashMap.remove(faceId);
    }
}
