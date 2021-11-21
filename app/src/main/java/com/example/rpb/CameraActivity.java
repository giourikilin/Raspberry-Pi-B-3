package com.example.rpb;


import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;


public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {


    private static final String TAG ="MSG";
    private Camera mCamera;
    private AutoFitTextureView textureView;
    private final Handler mainHandler = new Handler();
    private ObjectDetection liteDetector;
    int[] rgbValues = new int[640*480];
    private BackgroundRunnable runnable;
    private LinearLayout linearLayout;
    private ImageView imageView;
    private ArrayList<String> imageList = new ArrayList<>();
    private boolean state = false;
    private int[] leftCoordinates = new int[6];
    private int Pos = 0;
    private boolean p = true;
    private Integer counter = 0;



    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.camera_activity);
        linearLayout = findViewById(R.id.lr);
        textureView = new AutoFitTextureView(this);
        textureView.setSurfaceTextureListener(this);
        linearLayout.addView(textureView);

        imageView = findViewById(R.id.iv);
        imageView.setVisibility(View.INVISIBLE);


    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        activateCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    private static int getCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                return i;
        }
        return -1;
    }

    private void activateCamera() {
        mCamera = Camera.open(getCamera());
        liteDetector = new ObjectDetection(this);
        try {
            mCamera.setPreviewTexture(textureView.getSurfaceTexture());
            Camera.Parameters params =  mCamera.getParameters();
            params.setPreviewSize(640,480);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCamera.setDisplayOrientation(ORIENTATIONS.get(rotation));
            int orientation = getResources().getConfiguration().orientation;
            int width = 640;
            int height = 480;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(width, height);
            } else {
                textureView.setAspectRatio(height, width);
            }
            mCamera.setParameters(params);
        } catch (IOException ioe) {
            mCamera.release();
        }
        mCamera.setPreviewCallbackWithBuffer(this);
        mCamera.addCallbackBuffer(new byte[461361]);
        mCamera.startPreview();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (textureView.isAvailable()) {
            activateCamera();
        } else {
            textureView.setSurfaceTextureListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(null);
            try {
                new Thread(runnable);
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        runnable = new BackgroundRunnable(data);
        new Thread(runnable).start();
    }

    private Bitmap getBitmap(byte[] data)  {
        Bitmap nb = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
        cnvrtYUVToARGB8888(data,640,480,rgbValues);
        nb.setPixels(rgbValues,0,640,0,0,640,480);
        return nb;
    }

    public static void cnvrtYUVToARGB8888(byte[] input, int width, int height, int[] output) {
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width;
            int u = 0;
            int v = 0;

            for (int i = 0; i < width; i++, yp++) {
                int y = 0xff & input[yp];
                if ((i & 1) == 0) {
                    v = 0xff & input[uvp++];
                    u = 0xff & input[uvp++];
                }

                output[yp] = YUVtoRGB(y, u, v);
            }
        }
    }

    private static int YUVtoRGB(int y, int u, int v) {
        y = Math.max((y - 16), 0);
        u -= 128;
        v -= 128;

        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v - 400 * u);
        int b = (y1192 + 2066 * u);

        r = r > 262143 ? 262143 : (Math.max(r, 0));
        g = g > 262143 ? 262143 : (Math.max(g, 0));
        b = b > 262143 ? 262143 : (Math.max(b, 0));

        return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    }


    class BackgroundRunnable implements Runnable {
        byte[] data;

        BackgroundRunnable(byte[] data ){
            this.data = data;
        }

        @Override
        public void run() {
            Bitmap bitm = getBitmap(data);
            liteDetector.detectObj(bitm);
            int s = Math.round(liteDetector.getObjConf());
            String lbl = liteDetector.getObjLabel();
            Log.e("gesture",lbl + " " + s);

            if(lbl.equals("peace")){
                if(p){
                   try {
                        Thread.sleep(1000);
                        CameraActivity.this.runOnUiThread(new Runnable() {
                           public void run() {
                               Toast.makeText(getApplicationContext(), "Taking Picture...", Toast.LENGTH_SHORT).show(); }
                        });
                        savePic(bitm,data);
                        CameraActivity.this.runOnUiThread(new Runnable() {
                           public void run() {
                               Toast.makeText(getApplicationContext(), "Image Saved", Toast.LENGTH_SHORT).show(); }
                        });
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if(lbl.equals("palm")){
                accessImg();
                counter = 0;
            }


            if(lbl.equals("fist")) {
                if (Pos < 6) {
                    leftCoordinates[Pos] = (int) liteDetector.getObjPosition().left;
                    Pos++;
                } else if (Pos == 6) {
                   if (leftCoordinates[0] - leftCoordinates[5] > 0 ) {
                       counter--;
                       if(counter >= 0){
                           displayImages(imageList.get(counter));
                       } else {
                           counter=0;
                       }
                       Pos = 0;
                       Arrays.fill(leftCoordinates, 0);
                       try {
                           Thread.sleep(2000);
                       } catch (InterruptedException e) {
                           e.printStackTrace();
                       }
                   } else if (leftCoordinates[0] - leftCoordinates[5] < -1 ) {
                            counter++;
                           if(counter < imageList.size()){
                               displayImages(imageList.get(counter));
                           }
                           Pos = 0;
                           Arrays.fill(leftCoordinates, 0);
                           try {
                               Thread.sleep(2000);
                           } catch (InterruptedException e) {
                               e.printStackTrace();
                           }

                    }
                }
            }

            mainHandler.post(new Runnable() {
                @Override
                public void run() {}
            });
            mCamera.addCallbackBuffer(data);
        }
    }

    private void savePic(Bitmap bitmap, byte[] data){
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        File imageTaken = new File(createDirectoryPath(),formatter.format(new Date()) + ".jpg");

        try {
            FileOutputStream fos = new FileOutputStream(imageTaken);
            Matrix matrix = new Matrix();
            if (rotation == Configuration.ORIENTATION_PORTRAIT) {
                matrix.postRotate(0);
                Bitmap b1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                b1.compress(Bitmap.CompressFormat.PNG, 99, fos);
            } else {
                matrix.postRotate(90);
                Bitmap b1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                b1.compress(Bitmap.CompressFormat.PNG, 99, fos);
            }
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (IOException e ) {
            e.printStackTrace();
        }
    }

    private String createDirectoryPath() {
        String absPath;
        File sdCardPath = Environment.getExternalStorageDirectory();
        File myDir = new File(sdCardPath.getAbsolutePath() + "/3DDD");
        absPath = myDir.toString();
        if(!myDir.exists() && !myDir.mkdir()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Oops something went wrong.", Toast.LENGTH_SHORT).show();
                    finishAndRemoveTask();
                    System.exit(0);
                }
            });
        }

        return absPath;
    }

    private ArrayList<String> loadImages(){
        ArrayList<String> imgL = new ArrayList<>();
        File imgFolderPath = new File("/storage/emulated/0/3DDD");
        if (imgFolderPath.exists()){
            File[] imageFiles = imgFolderPath.listFiles();
            if (imageFiles != null && imageFiles.length != 0){
                for (File element : imageFiles){
                    imgL.add(element.toString());
                }
            }
        }
        return imgL;
    }

    private void openImages(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setVisibility(View.VISIBLE);
                linearLayout.setVisibility(View.INVISIBLE);
            }
        });
        imageList = loadImages();
        if (imageList.size() == 0){
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "No Pictures", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            displayImages(imageList.get(0));
        }
    }

    private void displayImages(String imgString){
        Bitmap imgBitmap = BitmapFactory.decodeFile(imgString);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(imgBitmap);
            }
        });
    }

    private void accessImg(){
        state =! state;
        try {
            if (state){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setVisibility(View.INVISIBLE);
                        linearLayout.setVisibility(View.VISIBLE);
                        p = true;
                    }
                });
            } else {
                openImages();
                p=false;
            }
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}






