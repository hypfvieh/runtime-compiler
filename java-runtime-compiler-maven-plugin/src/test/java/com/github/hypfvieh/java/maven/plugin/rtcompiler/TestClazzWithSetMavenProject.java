package com.github.hypfvieh.java.maven.plugin.rtcompiler;

import org.apache.maven.project.MavenProject;

@SuppressWarnings({"checkstyle:StaticVariableNameCheck", "checkstyle:VisibilityModifierCheck"})
public final class TestClazzWithSetMavenProject {

    public static int           MAIN_INVOCATION_COUNT = 0;
    public static String        PROJECT_ARTIFACT_ID   = null;

    private static MavenProject project;

    private TestClazzWithSetMavenProject() {
    }

    public static void main(String[] _args) {
        MAIN_INVOCATION_COUNT++;

        PROJECT_ARTIFACT_ID = project.getArtifact().getArtifactId();
    }

    public static void setMavenProject(MavenProject _prj) {
        project = _prj;
    }

}
