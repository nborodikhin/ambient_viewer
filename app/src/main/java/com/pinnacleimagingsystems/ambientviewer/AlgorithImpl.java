package com.pinnacleimagingsystems.ambientviewer;

import android.graphics.Color;

public class AlgorithImpl implements Algorithm {
    private static final int DEGAMMA_SIZE = 256;
    private static final int GAMMA_SIZE = 1024;

    private static float GAMMA_TO_LINEAR[];
    private static int LINEAR_TO_GAMMA[];

    private int parameter = 0;

    private float linearize(int value) {
        return GAMMA_TO_LINEAR[value];
    }

    private int delinearize(float value) {
        value *= (GAMMA_SIZE + 1);
        value = Math.min(GAMMA_SIZE, value);
        value = Math.max(0, value);
        return LINEAR_TO_GAMMA[(int) value];
    }

    private float linearizeExact(float value) {
        if (value <= 0.04045f) {
            return value / 12.92f;
        } else {
            float a = 0.055f;
            return (float) Math.pow((value + a) / (1.0f + a), 2.4f);
        }
    }

    private float delinearizeExact(float value) {
        return (float) Math.pow(value, 1/2.2f);
    }

    @Override
    public void init(int parameter) {
        initCurves();
        this.parameter = parameter;
    }

    private void initCurves() {
        if (GAMMA_TO_LINEAR == null) {
            GAMMA_TO_LINEAR = new float[DEGAMMA_SIZE];

            for (int i = 0; i < DEGAMMA_SIZE; i++) {
                float gamma = ((float) i) / 255f;
                GAMMA_TO_LINEAR[i] = linearizeExact(gamma);
            }
        }

        if (LINEAR_TO_GAMMA == null) {
            LINEAR_TO_GAMMA = new int[GAMMA_SIZE + 1];

            for (int i = 0; i < GAMMA_SIZE; i++) {
                float linear = ((float) i) / (GAMMA_SIZE);
                float gamma = delinearizeExact(linear);
                int intGamma = (int) Math.floor(gamma * 255.0f);

                LINEAR_TO_GAMMA[i] = intGamma;
                System.out.println("" + i + " -> " + LINEAR_TO_GAMMA[i]);
            }
            LINEAR_TO_GAMMA[GAMMA_SIZE] = 255;
        }
    }

    @Override
    public void apply(int[] rgbaPixels, int width, int height) {
        int numPixels = width * height;

        for (int i = 0; i < numPixels; i++) {
            int pixel = rgbaPixels[i];
            int r = apply(Color.red(pixel));
            int g = apply(Color.green(pixel));
            int b = apply(Color.blue(pixel));

            rgbaPixels[i] = Color.rgb(r, g, b);
        }
    }

    private int apply(int component) {
        float linear = linearize(component);

        float k = 1.f + parameter * 0.5f;
        float x = 0.01f;
        float y = k * x;

        if (linear < x) {
            linear = linear * y / x;
        } else {
            linear = y + (linear - x) / (1.0f - x) * (1.0f - y);
        }

        return delinearize(linear);
    }
}
