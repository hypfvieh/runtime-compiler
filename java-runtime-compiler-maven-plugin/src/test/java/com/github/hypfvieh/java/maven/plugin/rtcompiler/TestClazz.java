package com.github.hypfvieh.java.maven.plugin.rtcompiler;

public final class TestClazz {

    @SuppressWarnings({"checkstyle:StaticVariableNameCheck", "checkstyle:VisibilityModifierCheck"})
    public static int MAIN_INVOCATION_COUNT = 0;

    private TestClazz() {
    }

    public static void main(String[] _args) {
        MAIN_INVOCATION_COUNT++;
    }

}
