package com.example.rpb;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectDetection extends AppCompatActivity {
    private final Context context;

    public ObjectDetection(Context context){
        this.context = context;
    }

    private Interpreter liteDetector = null;
    private List<String> labelsList = null;;
    private static final int NUM_DETECTIONS = 10;
    private final int imgX = 300;
    private final int imgY = 300;
    private ByteBuffer arrayOfBytes = null;
    private int[] pix = null;
    private String objLabel = null;
    private Float objConf = null;
    private RectF objPos = null;
    private Float numOfObjDetected = null;



    void detectObj(Bitmap bitmap){

        arrayOfBytes = ByteBuffer.allocateDirect(imgX * imgY * 3);
        arrayOfBytes.order(ByteOrder.nativeOrder());
        pix = new int[imgX * imgY];

        try {
            liteDetector = new Interpreter(loadModelFile(),null);
            labelsList = loadLabelsList();
        } catch (Exception ex) {
            ex.printStackTrace();
            finishAndRemoveTask();
            System.exit(0);
        }

        Bitmap resizedBitmap = getResizedBitmap(bitmap);
        convertBitmapToByteBuffer(resizedBitmap);

        float[][] outputClasses = new float[1][NUM_DETECTIONS];
        float[][] outputScores = new float[1][NUM_DETECTIONS];
        float[][][] outputLocations = new float[1][NUM_DETECTIONS][4];
        float[] numDetections = new float[1];

        Object[] inputArray = {arrayOfBytes};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);

        liteDetector.runForMultipleInputsOutputs(inputArray, outputMap);

        objConf = outputScores[0][0]*100;
        if (Math.round(objConf) > 89){
            objLabel = labelsList.get((int) outputClasses[0][0]);
            objPos = new RectF(outputLocations[0][0][1] * imgX, outputLocations[0][0][0] * imgY, outputLocations[0][0][3] * imgX, outputLocations[0][0][2] * imgY);
            numOfObjDetected = numDetections[0];
        } else {
            objLabel = "None";
        }

    }


    private void convertBitmapToByteBuffer(Bitmap resizedBitmap) {
        if (arrayOfBytes == null) {
            return;
        }
        arrayOfBytes.rewind();
        resizedBitmap.getPixels(pix, 0, resizedBitmap.getWidth(), 0,0,resizedBitmap.getWidth(), resizedBitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < imgX; i++){
            for (int j = 0; j < imgY; j++){
                final int val = pix[pixel++];
                arrayOfBytes.put((byte) ((val >> 16) & 0xFF));
                arrayOfBytes.put((byte) ((val >> 8) & 0xFF));
                arrayOfBytes.put((byte) (val & 0xFF));
            }
        }
    }

    private Bitmap getResizedBitmap(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) 300) / width;
        float scaleHeight = ((float) 300) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            matrix.postRotate(90);
        } else {
            matrix.postRotate(0);
        }

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        String modelFile = "detect.tflite";
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFile);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        fileDescriptor.close();
        inputStream.close();
        return mappedByteBuffer;
    }

    private List<String> loadLabelsList() throws IOException {
        List<String> lList = new ArrayList<String>();
        String labelsFile = "label.txt";
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(labelsFile)));
        String line;
        while ((line = reader.readLine()) != null) {
            lList.add(line);
        }
        reader.close();
        return lList;
    }

    public RectF getObjPosition() {return objPos;}
    public String getObjLabel() {return objLabel;}
    public Float getObjConf() {return objConf;}
    public Float getNumDetections() {return numOfObjDetected;}
}
