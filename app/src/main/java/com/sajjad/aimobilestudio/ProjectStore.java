package com.sajjad.aimobilestudio;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ProjectStore {
    private final File root;
    public ProjectStore(Context context) {
        root = new File(context.getFilesDir(), "projects");
        if (!root.exists()) root.mkdirs();
    }
    public synchronized String createProject(String requested) throws Exception {
        String name = requested == null ? "" : requested.trim().replaceAll("[^A-Za-z0-9_-]", "");
        if (name.isEmpty()) name = "AndroidProject";
        File dir = new File(root, name);
        int n = 2;
        while (dir.exists()) dir = new File(root, name + n++);
        if (!dir.mkdirs()) throw new Exception("Cannot create project directory");
        write(new File(dir, "README.md"), "# " + dir.getName() + "\n\nCreated by AI Mobile Studio.\n");
        write(new File(dir, "settings.gradle"), "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\ndependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\nrootProject.name='" + dir.getName() + "'\ninclude(':app')\n");
        write(new File(dir, "build.gradle"), "plugins { id 'com.android.application' version '8.7.3' apply false }\n");
        new File(dir, "app/src/main/java").mkdirs();
        return dir.getName();
    }
    public synchronized List<String> listProjects() {
        List<String> names = new ArrayList<>();
        File[] files = root.listFiles(File::isDirectory);
        if (files != null) for (File f : files) names.add(f.getName());
        return names;
    }
    public String projectPath(String name) { return new File(root, name).getAbsolutePath(); }
    public synchronized void saveText(String project, String relative, String content) throws Exception {
        File base = new File(root, project).getCanonicalFile();
        File target = new File(base, relative).getCanonicalFile();
        if (!target.toPath().startsWith(base.toPath())) throw new SecurityException("Invalid path");
        File parent = target.getParentFile(); if (parent != null) parent.mkdirs();
        write(target, content);
    }
    private void write(File file, String value) throws Exception {
        try (FileOutputStream out = new FileOutputStream(file)) { out.write(value.getBytes(StandardCharsets.UTF_8)); }
    }
}
