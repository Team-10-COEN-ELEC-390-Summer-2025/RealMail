package com.team10.realmail.api;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YoloDetector {
    private static  int NUM_FEATURES = 6;    // 6 values per detection
    private static final int NUM_CELLS = 8400;
    private static int NUM_CLASSES = 2;
    private static final float THRESHOLD = 0.85f;
    private static final float NMS_THRESHOLD = 0.8f;

    public static final String[] labels = {"Letter", "Package"};

    private final Interpreter interpreter;
    private final int inputW, inputH;

    private int numLetters, numPackages;

    public YoloDetector(Context ctx) throws IOException {
        interpreter = new Interpreter(loadModelFile(ctx));
        int[] inputShape = interpreter.getInputTensor(0).shape();
        inputH = inputShape[1];
        inputW = inputShape[2];

        NUM_CLASSES = labels.length;
    }

    private MappedByteBuffer loadModelFile(Context ctx) throws IOException {
        AssetFileDescriptor afd = ctx.getAssets().openFd("modelv1.0_32.tflite");
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel fc = fis.getChannel();
        return fc.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getDeclaredLength());
    }

    public Map<String, Integer> detect(Bitmap bmp) {
        Bitmap resized = Bitmap.createScaledBitmap(bmp, inputW, inputH, true);
        ByteBuffer buf = ByteBuffer.allocateDirect(inputW * inputH * 3 * 4).order(ByteOrder.nativeOrder());
        for (int y = 0; y < inputH; y++)
            for (int x = 0; x < inputW; x++) {
                int p = resized.getPixel(x, y);
                buf.putFloat(((p >> 16) & 0xFF) / 255f);
                buf.putFloat(((p >> 8) & 0xFF) / 255f);
                buf.putFloat((p & 0xFF) / 255f);
            }
        buf.rewind();

        // Run inference: result shape = [1][6][8400]
        float[][][] output = new float[1][NUM_FEATURES][NUM_CELLS];
        interpreter.run(buf, output);
        return processOutput(output[0]); // shape [6][8400]
    }

    private Map<String, Integer> processOutput(float[][] out) {
        List<Detection> dets = new ArrayList<>();
        List<Float> Hscores = new ArrayList<>() ;

        float[] xc = out[0], yc = out[1], w = out[2], h = out[3], obj = out[4], cls = out[5]; // Obj = Letter conf, cls = Package conf

        for (int i = 0; i < NUM_CELLS; i++) {
            float score = 0;
            int clsIdx = -1;
            if (obj[i] < cls[i]){
                score = cls[i];
                clsIdx = 1;
            } else {
                score = obj[i];
                clsIdx =0;

            }
            if (score < THRESHOLD) continue;




            Hscores.add(score);
            if (score < THRESHOLD) continue;

            float x0 = xc[i] - w[i] / 2;
            float y0 = yc[i] - h[i] / 2;
            dets.add(new Detection(clsIdx, score, x0, y0, w[i], h[i]));




        }

        return applyNMSAndCount(dets);
    }

    private Map<String, Integer> applyNMSAndCount(List<Detection> boxes) {
        boxes.sort((a, b) -> Float.compare(b.conf, a.conf));
        List<Detection> keep = new ArrayList<>();
        for (Detection A : boxes) {
            boolean dup = false;
            for (Detection B : keep) {
                if (iou(A, B) > NMS_THRESHOLD) { dup = true; break; }
            }
            if (!dup) keep.add(A);
        }

        Map<String, Integer> counts = new HashMap<>();
        for (Detection d : keep) {
            String label = labels[d.cls];
            counts.put(labels[d.cls], counts.getOrDefault(labels[d.cls], 0) + 1);
        }
        return counts;
    }

    private float iou(Detection a, Detection b) {
        float x1 = Math.max(a.x, b.x), y1 = Math.max(a.y, b.y);
        float x2 = Math.min(a.x + a.w, b.x + b.w), y2 = Math.min(a.y + a.h, b.y + b.h);
        float iw = Math.max(0, x2 - x1), ih = Math.max(0, y2 - y1);
        float inter = iw * ih;
        return inter / (a.w * a.h + b.w * b.h - inter);
    }

    public void close() { interpreter.close(); }

    private static class Detection {
        int cls; float conf, x, y, w, h;
        Detection(int cls, float conf, float x, float y, float w, float h) {
            this.cls = cls; this.conf = conf; this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }
}

