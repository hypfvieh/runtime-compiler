package com.github.hypfvieh.java.rtcompiler.samples;

public final class SampleSourceExceptionAtRunTime {

    private SampleSourceExceptionAtRunTime() {

    }

    public static void main(String[] _args) {
        throw new RuntimeException();
    }

}
