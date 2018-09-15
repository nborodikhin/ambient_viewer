package com.pinnacleimagingsystems.ambientviewer;

public interface Algorithm {
    void init(int parameter);
    void apply(int[] rgbaPixels, int width, int height);
}
