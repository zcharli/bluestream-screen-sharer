package che.carleton.ottawa.bluestream.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import che.carleton.ottawa.bluestream.Constants;
import che.carleton.ottawa.activity.ActivityResultBus;
import che.carleton.ottawa.activity.ActivityResultEvent;
import che.carleton.ottawa.bluestream.Models.BluetoothService;
import che.carleton.ottawa.bluestream.R;
import che.carleton.ottawa.bluestream.Settings;

/**
 * Provides UI for the screen capture.
 */

//TODO: Refactor this class to act as a model rather than a fragment
public class ScreenCaptureFragment extends Fragment{

    private static final String TAG = "ScreenCaptureFragment";

    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";

    private static final int REQUEST_MEDIA_PROJECTION = 1;

    private int mScreenDensity;

    private int mResultCode;
    private Intent mResultData;

    //private Surface mSurface;
    private MediaProjection mMediaProjection;
    //private VirtualDisplay mVirtualDisplay;
    private MediaProjectionManager mMediaProjectionManager;
    private Button mButtonToggle;
    //private SurfaceView mSurfaceView;

    /* Recording Media Project variables */
    //private MediaProjectionCallback mMediaProjectionCallback; // define as a class below
    private Handler mImageHandler;

    //private MediaRecorder mMediaRecorder;
    private ImageReader mImageReader;
    private int DISPLAY_WIDTH = 480;
    private int DISPLAY_HEIGHT = 640;

    /* Bluetooth Communication variables */
    private BluetoothService mBluetoothService = null;

    private boolean ScreenCapturePlaying = false;

    public boolean isScreenCapturePlaying() { return ScreenCapturePlaying; }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
            mResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ActivityResultBus.getInstance().register(mActivityResultSubscriber);
    }

    @Override
    public void onStop() {
        super.onStop();
        // TODO: onStop may fire when we press home button. this may cause and issue
        ActivityResultBus.getInstance().unregister(mActivityResultSubscriber);
    }

    private Object mActivityResultSubscriber = new Object() {
        @Subscribe
        public void onActivityResultReceived(ActivityResultEvent event) {
            int requestCode = event.getRequestCode();
            int resultCode = event.getResultCode();
            Intent data = event.getData();
            onActivityResult(requestCode, resultCode, data);
        }
    };


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_screen_capture, container, false);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //mMediaProjectionCallback = new MediaProjectionCallback();
        Activity activity = getActivity();
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mMediaProjectionManager = (MediaProjectionManager)
                getActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mImageHandler = new Handler();
                Looper.loop();
            }
        }.start();
        //Log.d(TAG, "Initialized recorder");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mResultData != null) {
            outState.putInt(STATE_RESULT_CODE, mResultCode);
            outState.putParcelable(STATE_RESULT_DATA, mResultData);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                //Log.i(TAG, "User cancelled");
                Toast.makeText(getActivity(), R.string.user_cancelled, Toast.LENGTH_SHORT).show();
                return;
            }
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            //Log.i(TAG, "Starting screen capture");
            mResultCode = resultCode;
            mResultData = data;
            setUpMediaProjection();
            setUpVirtualDisplay();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopScreenCapture();
    }

    public void setBluetoothService(BluetoothService src) {
        if (src != null) {
            mBluetoothService = src;
        } else {
            //Log.i(TAG, "BSSocket passed to SCFrag was null");
        }
    }

    private void setUpMediaProjection() {
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
        //mMediaProjection.registerCallback(mMediaProjectionCallback, null);
    }

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    public void startScreenCapture() {
        ScreenCapturePlaying = true;

        if (mMediaProjection != null) {
            setUpVirtualDisplay();
        } else if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection();
            setUpVirtualDisplay();
        } else {
            // This initiates a prompt dialog for the user to confirm screen projection.
            getActivity().startActivityForResult(
                    mMediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
        }
    }

    public void stopScreenCapture() {
        ScreenCapturePlaying = false;

        mImageHandler.post(new Runnable() {
            @Override
            public void run() {
                //mVirtualDisplay.release();
                //mVirtualDisplay = null;
                tearDownMediaProjection();
                mImageReader = null;

            }
        });
    }

    private void setUpVirtualDisplay() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        DISPLAY_WIDTH = size.x;
        DISPLAY_HEIGHT = size.y;
        // Initialize image reader
        mImageReader = ImageReader.newInstance(DISPLAY_WIDTH, DISPLAY_HEIGHT, PixelFormat.RGBA_8888, 2);

        // Initialize the media projection and hook image reader to capture the surface
        mMediaProjection.createVirtualDisplay("ScreenCapture",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mImageReader.getSurface(), null /*Callbacks*/, mImageHandler /*Handler*/);

        //TODO: Investigate if delegating ImageListener to background thread with handler is better
        // Set the on image handler to capture a image and surface changes
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mImageHandler /* Handler*/);

    }

    /*
    * This class will catch all new images that ImageReader can catch from MediaProjection
    */

    //private int IMAGES_PRODUCED = 0;
    //private String STORE_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/screenshots/";
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            Bitmap bitmap = null;
            // Replace Byte array to use normal array cause no resizing will occur
            ByteArrayOutputStream jpegByteOutStream = new ByteArrayOutputStream();
            //FileOutputStream fos = null; // testing
            try {
                // Grab the image that the reader prepared us with
                image = mImageReader.acquireLatestImage();
                if (image != null) {

                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] data = new byte[buffer.capacity()];
                    buffer.get(data);
                    // Get the array of pixels of the 2d plane
                    // Fill the buffer with the image and rewind the buffer pointer to the begining
                    buffer = ByteBuffer.wrap(data);

                    // Create the bitmap
                    buffer.rewind();
                    //byte[] buffer = getDataFromImage(image);
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * DISPLAY_WIDTH;
                    bitmap = Bitmap.createBitmap(DISPLAY_WIDTH + rowPadding / pixelStride, DISPLAY_HEIGHT, Bitmap.Config.ARGB_8888);
                    // Fill the bitmap with the pixels in our buffer

                    bitmap.copyPixelsFromBuffer(buffer);
                    bitmap = Bitmap.createScaledBitmap(bitmap, 480, 640, false);

                    // Compress bitmap and send over to output stream in bluetooth socket
                    bitmap.compress(Bitmap.CompressFormat.JPEG, Settings.QUALITY_LEVEL, jpegByteOutStream);
                    // Send the byte output stream to bluetooth output stream
                    mBluetoothService.write(jpegByteOutStream.toByteArray());

                    //Log.v(TAG, "Sent a single image!");
                }
            } catch(Exception e) {
                e.printStackTrace();
                //TODO: Clean up stopping so we dont have to see this error
                //Log.d(TAG,"FUCK! Image error!");
            }finally {
                if (jpegByteOutStream!=null) {
                    try {
                        jpegByteOutStream.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                if (bitmap!=null) {
                    bitmap.recycle();
                }
                if (image!=null) {
                    image.close();
                }
            }
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            // This call back will help close the recorder when we press the stop recording btn
            if (mButtonToggle.getText().toString() == "Stop") {
                mButtonToggle.setText(R.string.start);
                // mMediaRecorder.stop();
                // mMediaRecorder.reset();
                //Log.v(TAG, "Recording Stopped");
                //initRecorder();
                //prepareRecorder();
                mMediaProjection.stop();
                stopScreenCapture();
                mImageReader = null;
            }

            //Log.i(TAG, "MediaProjection Stopped");
        }
    }

}
