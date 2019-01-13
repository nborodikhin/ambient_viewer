package com.pinnacleimagingsystems.ambientviewer;

public interface Algorithm {
    interface Meta {
        int parameterMin();
        int parameterMax();
        float defaultParameter(int lux);
    }

    Meta getMeta();

    void init(float parameter);
    void apply(int[] rgbaPixels, int width, int height);
}
