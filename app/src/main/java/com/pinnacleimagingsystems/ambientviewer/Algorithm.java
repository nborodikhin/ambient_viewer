package com.pinnacleimagingsystems.ambientviewer;

public interface Algorithm {
    interface Meta {
        int parameterMin();
        int parameterMax();
        int defaultParameter(int lux);
    }

    Meta getMeta();

    void init(int parameter);
    void apply(int[] rgbaPixels, int width, int height);
}
