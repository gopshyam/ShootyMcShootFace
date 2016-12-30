package com.example.carbo.shootymcshootface;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.Face;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DisplayFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DisplayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DisplayFragment extends Fragment {

    private static final String TAG = "DisplayFragment";
    private static final String CAMERA_NOT_FOUND = "CAMERA_NOT_FOUND";
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private Thread mStartOpenCameraThread;
    private TextureView mBackgroundView;
    private SurfaceView mMiddleView, mForegroundView;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;
    private boolean testvar = true;
    private ImageReader mImageReader;

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            if (testvar) {
                Log.d(TAG, String.valueOf(partialResult.get(CaptureResult.STATISTICS_FACE_DETECT_MODE)));
                Face[] facesList = partialResult.get(CaptureResult.STATISTICS_FACES);
                if (facesList != null) {
                    Log.d(TAG, String.valueOf(facesList.length));
                    testvar = false;
                }
            }
        }
    };

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            Log.d(TAG, "Camera Opened");
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };
    private OnFragmentInteractionListener mListener;

    public DisplayFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DisplayFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DisplayFragment newInstance() {
        DisplayFragment fragment = new DisplayFragment();
        return fragment;
    }

    private void createCameraPreviewSession() {

        SurfaceTexture texture = mBackgroundView.getSurfaceTexture();
        texture.setDefaultBufferSize(mBackgroundView.getWidth(), mBackgroundView.getHeight());
        Surface targetSurface = new Surface(texture);
        mImageReader = ImageReader.newInstance(mBackgroundView.getWidth(), mBackgroundView.getHeight(), ImageFormat.JPEG, 2);
        Surface readerSurface = mImageReader.getSurface();

        try {
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(targetSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(targetSurface, readerSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if (null == mCameraDevice) {
                        return;
                    }

                    mCaptureSession = session;
                    mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF);
                    mPreviewRequest = mPreviewRequestBuilder.build();

                    try {
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Preview created");
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(getContext(), "Camera preview failed", Toast.LENGTH_LONG).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void findCompatibleCamera() {
        //Get the cameraManager and find out camera stuff.
        String[] cameraIdList = new String[0];
        try {
            cameraIdList = mCameraManager.getCameraIdList();
            for (String cameraId : cameraIdList) {
                int lensFacing = mCameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING);
                if (lensFacing == CameraMetadata.LENS_FACING_FRONT) {
                    int[] faceDetectModes = mCameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
                    for (int faceDetectMode : faceDetectModes) {
                        if (CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE == faceDetectMode) {
                            openCamera(cameraId);
                        }
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_display, container, false);
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
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        startBackgroundThread();

        mCameraManager = (CameraManager) getActivity().getApplicationContext().getSystemService(Context.CAMERA_SERVICE);

        mBackgroundView = (TextureView) getView().findViewById(R.id.background);
        mMiddleView = (SurfaceView) getView().findViewById(R.id.middle);
        mForegroundView = (SurfaceView) getView().findViewById(R.id.foreground);

        mStartOpenCameraThread = new Thread(new Runnable() {
            @Override
            public void run() {
                findCompatibleCamera();
            }
        });
        mStartOpenCameraThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopBackgroundThread();
        closeCamera();
    }

    private void openCamera(String cameraId) {

        if (mCameraManager == null) {
            Log.e(TAG, "Camera Manager is Null");
            return;
        }
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timeout waiting to acquire camera lock");
            }
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.e(TAG, "Permissions error");
                Toast.makeText(getActivity().getApplicationContext(), "Camera permission required", Toast.LENGTH_LONG).show();
                return;
            }
            mCameraManager.openCamera(cameraId, mStateCallback, mBackgroundHandler);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        } catch (CameraAccessException e) {
            Toast.makeText(getContext(), "Need camera permissions", Toast.LENGTH_LONG).show();
            throw new RuntimeException("Camera Access Exception");
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
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
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
