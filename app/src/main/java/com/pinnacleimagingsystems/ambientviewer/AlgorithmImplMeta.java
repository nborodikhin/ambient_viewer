package com.pinnacleimagingsystems.ambientviewer;

class AlgorithmImplMeta implements Algorithm.Meta {
    @Override
    public int parameterMin() {
        return 0;
    }

    @Override
    public int parameterMax() {
        return 10;
    }

    @Override
    public int defaultParameter(int lux) {
        return 5;
    }
}
