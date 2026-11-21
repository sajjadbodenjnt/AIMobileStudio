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
        root=new File(context.getFilesDir(),"projects");
        if(!root.exists()&&!root.mkdirs())throw new IllegalStateException("Cannot create project storage");
    }
    public synchronized String createProject(String requested)throws Exception{
        String name=requested==null?"":requested.trim().replaceAll("[^A-Za-z0-9_-]","");
        if(name.isEmpty())name="AndroidProject";
        File dir=new File(root,name);int n=2;while(dir.exists())dir=new File(root,name+n++);
        if(!dir.mkdirs())throw new Exception("Cannot create project directory");
        String appId="com.generated."+dir.getName().toLowerCase().replaceAll("[^a-z0-9_]","");
        write(new File(dir,"README.md"),"# "+dir.getName()+"\n\nAndroid project created by AI Mobile Studio.\n");
        write(new File(dir,"settings.gradle"),"pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\n"+
            "dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\n"+
            "rootProject.name = '"+dir.getName()+"'\ninclude ':app'\n");
        write(new File(dir,"build.gradle"),"plugins { id 'com.android.application' version '8.7.3' apply false }\n");
        write(new File(dir,"gradle.properties"),"org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\nandroid.useAndroidX=true\n");
        File app=new File(dir,"app");if(!app.mkdirs())throw new Exception("Cannot create app directory");
        write(new File(app,"build.gradle"),"plugins { id 'com.android.application' }\n\nandroid {\n    namespace '"+appId+"'\n    compileSdk 35\n    defaultConfig { applicationId '"+appId+"' minSdk 24 targetSdk 35 versionCode 1 versionName '1.0' }\n}\n");
        File main=new File(app,"src/main");File javaDir=new File(main,"java/"+appId.replace('.','/'));
        if(!javaDir.mkdirs())throw new Exception("Cannot create source directory");
        write(new File(main,"AndroidManifest.xml"),"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"><application android:theme=\"@android:style/Theme.Material.Light.NoActionBar\" android:label=\""+dir.getName()+"\"><activity android:name=\".MainActivity\" android:exported=\"true\"><intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter></activity></application></manifest>\n");
        write(new File(javaDir,"MainActivity.java"),"package "+appId+";\n\nimport android.app.Activity;\nimport android.os.Bundle;\nimport android.widget.TextView;\n\npublic class MainActivity extends Activity { @Override public void onCreate(Bundle state) { super.onCreate(state); TextView view = new TextView(this); view.setText(\""+dir.getName()+"\\nGenerated Android project\"); view.setTextSize(24); view.setPadding(32,64,32,32); setContentView(view); } }\n");
        return dir.getName();
    }
    public synchronized List<String> listProjects(){
        List<String> names=new ArrayList<>();File[] dirs=root.listFiles(File::isDirectory);
        if(dirs!=null)for(File f:dirs)names.add(f.getName());return names;
    }
    public String projectPath(String name){return new File(root,name).getAbsolutePath();}
    public synchronized String[] listFiles(String project){
        File base=new File(root,project);List<String> result=new ArrayList<>();collect(base,base,result);return result.toArray(new String[0]);
    }
    private void collect(File base,File dir,List<String> out){
        File[] children=dir.listFiles();if(children==null)return;
        for(File f:children){if(f.isDirectory())collect(base,f,out);else out.add(base.toURI().relativize(f.toURI()).getPath());}
    }
    public synchronized void saveText(String project,String relative,String content)throws Exception{
        if(relative==null||relative.trim().isEmpty()||relative.startsWith("/")||relative.startsWith("\\")||
            relative.matches("^[A-Za-z]:.*"))throw new SecurityException("Invalid relative path");
        for(String part:relative.replace('\\','/').split("/"))if(part.equals("..")||part.equals("."))throw new SecurityException("Path traversal rejected");
        File base=new File(root,project).getCanonicalFile();File target=new File(base,relative).getCanonicalFile();
        if(!target.toPath().startsWith(base.toPath()))throw new SecurityException("Invalid path");
        File parent=target.getParentFile();if(parent!=null&&!parent.exists()&&!parent.mkdirs())throw new Exception("Cannot create folder: "+parent);
        write(target,content);
    }
    private void write(File file,String value)throws Exception{
        try(FileOutputStream out=new FileOutputStream(file)){out.write(value.getBytes(StandardCharsets.UTF_8));}
    }
}
