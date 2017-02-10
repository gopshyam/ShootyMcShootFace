package com.example.carbo.shootymcshootface;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;
import java.util.Hashtable;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GoogleVisionFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GoogleVisionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GoogleVisionFragment extends Fragment {

    private static final String TAG = "GoogleVisionFragment";
    private static final float END_SCALE = 0.6f;
    private static final int ANIMATION_DURATION = 280;
    private static final int PREVIEW_WIDTH = 640, PREVIEW_HEIGHT = 480;
    private static final float RECOIL_ROTATION = 20.0f, RECOIL_TRANSLATION = 100.0f;
    private OnFragmentInteractionListener mListener;
    private FaceDetector mFaceDetector;
    private Hashtable<Integer, Face> mFaceHashTable;
    private FaceDataStructure mFaceDataStructure;
    private CameraSource mCameraSource;
    private boolean mSurfaceAvailable = false;
    private SurfaceView mBackgroundView;
    private SurfaceView mForegroundView;
    private SurfaceView mTintView;
    private ImageView mGunImage;
    private ImageView mBulletImage;
    private View.OnTouchListener mTouchListener;
    private ObjectAnimator mBulletFirePropAnimation;
    private AnimatorSet mTintAnimatorSet, mFireAnimatorSet, mHudTextAnimatorSet;
    private ImageView mCrosshair;
    private TextView mHudTextView;
    private int mWidth, mHeight;

    public GoogleVisionFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment GoogleVisionFragment.
     */
    public static GoogleVisionFragment newInstance() {
        GoogleVisionFragment fragment = new GoogleVisionFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFaceHashTable = new Hashtable<>();
        mTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.e("Touch event", String.valueOf(event.getX()) + " " + String.valueOf(event.getY()));
                if (mFireAnimatorSet != null && !mFireAnimatorSet.isStarted()) { //TODO: do this inside the animation listener?
                    //mBulletFirePropAnimation.start();
                    mFireAnimatorSet.start();
                }
                return false;
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_google_vision, container, false);
    }

    private void InitializeViewObjects(View view) {
        mBackgroundView = (SurfaceView) view.findViewById(R.id.background);
        mBackgroundView.getHolder().addCallback(new BackGroundSurfaceCallback());

        mForegroundView = (SurfaceView) view.findViewById(R.id.foreground);
        mForegroundView.setOnTouchListener(mTouchListener);

        mGunImage = (ImageView) view.findViewById(R.id.gun_image);
        mGunImage.setX(view.getWidth() - mGunImage.getWidth());
        mGunImage.setY(view.getHeight() - mGunImage.getHeight());

        mTintView = (SurfaceView) view.findViewById(R.id.tint_view);

        mBulletImage = (ImageView) view.findViewById(R.id.bullet_image);
        mBulletImage.setX(2300);
        mBulletImage.setY(1300);

        mCrosshair = (ImageView) view.findViewById(R.id.crosshair);

        mHudTextView = (TextView) view.findViewById(R.id.hud_text);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InitializeViewObjects(view);
    }

    private void setupAnimations() {

        //Bullet animation
        PropertyValuesHolder pvhsx = PropertyValuesHolder.ofFloat(View.SCALE_X, END_SCALE);
        PropertyValuesHolder pvhsy = PropertyValuesHolder.ofFloat(View.SCALE_Y, END_SCALE);
        PropertyValuesHolder pvhtx = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, mWidth / 2 - (mBulletImage.getWidth() / END_SCALE));
        PropertyValuesHolder pvhty = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, mHeight / 2 - (mBulletImage.getHeight() / END_SCALE));
        mBulletFirePropAnimation = ObjectAnimator.ofPropertyValuesHolder(mBulletImage, pvhsx, pvhsy, pvhtx, pvhty);
        mBulletFirePropAnimation.setDuration(ANIMATION_DURATION);
        mBulletFirePropAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                ObjectAnimator bulletFirePropAnimation = (ObjectAnimator) animation;
                ImageView bulletImage = (ImageView) bulletFirePropAnimation.getTarget();
                bulletImage.setVisibility(ImageView.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator bulletFirePropAnimation = (ObjectAnimator) animation;
                ImageView bulletImage = (ImageView) bulletFirePropAnimation.getTarget();
                bulletImage.setVisibility(ImageView.GONE);
                checkShotResult();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                //This should not happen though, the only point of the fire in progress thing is to make sure only one shot
                //can be fired at a time.
                //LIFECYCLE EVENTS could trigger this though, so leaving it.
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        //Animation for firing a gun
        ObjectAnimator gunRotateClockwise = ObjectAnimator.ofPropertyValuesHolder(mGunImage,
                PropertyValuesHolder.ofFloat(View.ROTATION, RECOIL_ROTATION));
        gunRotateClockwise.setDuration(50);
        ObjectAnimator gunRotateAnti = ObjectAnimator.ofPropertyValuesHolder(mGunImage,
                PropertyValuesHolder.ofFloat(View.ROTATION, 0.0f));
        ObjectAnimator gunRecoilTranslate = ObjectAnimator.ofPropertyValuesHolder(mGunImage,
                PropertyValuesHolder.ofFloat(View.X, mGunImage.getX() + RECOIL_TRANSLATION));
        gunRecoilTranslate.setDuration(50);
        ObjectAnimator gunRecoilTranslateReset = ObjectAnimator.ofPropertyValuesHolder(mGunImage,
                PropertyValuesHolder.ofFloat(View.X, mGunImage.getX()));

        mFireAnimatorSet = new AnimatorSet();
        mFireAnimatorSet.play(gunRotateClockwise).with(gunRecoilTranslate).with(mBulletFirePropAnimation);
        mFireAnimatorSet.play(gunRotateAnti).after(gunRotateClockwise);
        mFireAnimatorSet.play(gunRecoilTranslateReset).after(gunRecoilTranslate);

        //Tint animation for when a shot is successful.
        PropertyValuesHolder tintFadeIn = PropertyValuesHolder.ofFloat(View.ALPHA, 0.5f);
        PropertyValuesHolder tintFadeOut = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f);

        ObjectAnimator tintFadeInAnimation = ObjectAnimator.ofPropertyValuesHolder(mTintView, tintFadeIn);
        tintFadeInAnimation.setDuration(50);
        ObjectAnimator tintFadeOutAnimation = ObjectAnimator.ofPropertyValuesHolder(mTintView, tintFadeOut);
        tintFadeOutAnimation.setDuration(150);

        mTintAnimatorSet = new AnimatorSet();
        mTintAnimatorSet.play(tintFadeInAnimation).before(tintFadeOutAnimation);

        //Utility Animation for showing text on the HUD.
        PropertyValuesHolder textFadeIn = PropertyValuesHolder.ofFloat(View.ALPHA, 0.85f);
        PropertyValuesHolder textFadeOut = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f);
        PropertyValuesHolder textMoveUpAnimationY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, mHudTextView.getY());
        PropertyValuesHolder textIncreaseSize = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f);

        ObjectAnimator textMoveUpAnimation = ObjectAnimator.ofPropertyValuesHolder(mHudTextView, textMoveUpAnimationY, textFadeIn, textIncreaseSize);
        ObjectAnimator textFadeOutAnimation = ObjectAnimator.ofPropertyValuesHolder(mHudTextView, textFadeOut);
        textMoveUpAnimation.setDuration(ANIMATION_DURATION * 3);
        textFadeOutAnimation.setDuration(ANIMATION_DURATION * 2);

        mHudTextAnimatorSet = new AnimatorSet();
        mHudTextAnimatorSet.play(textFadeOutAnimation).after(textMoveUpAnimation.getDuration() * 5).after(textMoveUpAnimation);
        mHudTextAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mHudTextView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mHudTextView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mHudTextView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void showHudText(String message) {
        if (mHudTextAnimatorSet.isStarted()) {
            mHudTextAnimatorSet.cancel();
        }
        mHudTextView.setText(message);
        mHudTextView.setY(mHeight / 4.0f);
        mHudTextView.setScaleY(0.5f);
        mHudTextView.setAlpha(0.0f);
        mHudTextAnimatorSet.start();

    }

    private void checkShotResult() {
        Log.e("RES", "func called");
        for (Hashtable.Entry<Integer, Face> entry : mFaceHashTable.entrySet()) {
            Face face = entry.getValue();
            PointF facePosition = transformPreviewToGlobal(face.getPosition());
            float faceWidth = face.getWidth() * mWidth / PREVIEW_WIDTH;
            float faceHeight = face.getHeight() * mHeight / PREVIEW_HEIGHT;
            Rect faceRect = new Rect((int) facePosition.x, (int) facePosition.y, (int) (facePosition.x + faceWidth), (int) (facePosition.y + faceHeight));

            Log.e("Shot fired", facePosition.toString() + " " + faceRect.toString() + " " + String.valueOf(mWidth));

            if (faceRect.contains(mWidth / 2, mHeight / 2)) {
                //The shot was successful
                mTintAnimatorSet.start();
                showHudText("Good Shot!");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSurfaceAvailable) {
            createCameraSource();
            startCameraPreview();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mCameraSource.stop();
        mCameraSource.release();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFaceDetector.release();
        mListener = null;
    }

    private void startCameraPreview() {
        if (mSurfaceAvailable) {
            try {
                if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    throw new IOException("Permission not granted, should have been requested by the activity");
                }
                mCameraSource.start(mBackgroundView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createCameraSource() {
        mFaceDetector = new FaceDetector.Builder(getActivity().getApplicationContext())
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        mFaceDetector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());

        if (!mFaceDetector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(getActivity().getApplicationContext(), mFaceDetector)
                .setRequestedPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .setAutoFocusEnabled(true)
                .build();

    }

    private void removeFace(int mFaceId) {
        mFaceHashTable.remove(mFaceId);
    }

    private void newFaceDetected(Face face, int mFaceId) {
        mFaceHashTable.put(mFaceId, face);
    }

    private void updateFace(Face face, int mFaceId) {
        mFaceHashTable.put(mFaceId, face);
        Log.e("Face Detected", String.valueOf(face.getPosition().toString()));
    }

    public PointF transformPreviewToGlobal(float x, float y) {
        float heightScale = mHeight / PREVIEW_HEIGHT;
        float widthScale = mWidth / PREVIEW_WIDTH;

        Log.w("Conversions", String.valueOf(x) + " " + String.valueOf(y));

        return new PointF(x * widthScale, y * heightScale);
    }

    public PointF transformPreviewToGlobal(PointF point) {
        return transformPreviewToGlobal(point.x, point.y);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker();
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        private int mFaceId;

        @Override
        public void onNewItem(int i, Face face) {
            super.onNewItem(i, face);
            mFaceId = i;
            newFaceDetected(face, mFaceId);
        }

        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            super.onUpdate(detections, face);
            updateFace(face, mFaceId);
        }

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            super.onMissing(detections);
        }

        @Override
        public void onDone() {
            super.onDone();
            removeFace(mFaceId); //only remove the face when we're sure it's gone for good.
        }
    }

    private class BackGroundSurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceAvailable = true;
            mWidth = getView().getWidth();
            mHeight = getView().getHeight();
            mGunImage.setX(mWidth - mGunImage.getWidth());
            mGunImage.setY(mHeight - (mGunImage.getHeight() * 2 / 3));
            mCrosshair.setX(mWidth / 2 - (mCrosshair.getWidth() / 2));
            mCrosshair.setY(mHeight / 2 - (mCrosshair.getHeight() / 2));
            setupAnimations();
            createCameraSource();
            startCameraPreview();
            showHudText("Shoot someone in the face :)");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mSurfaceAvailable = false;
        }
    }
}
