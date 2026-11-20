package com.offlineide;

import android.content.Context;
import java.io.*;
import java.util.*;

/** Runs only the provisioned Gradle executable against a selected project; never invokes a shell. */
public final class BuildController {
  private final Context context;
  public BuildController(Context c){context=c.getApplicationContext();}
  public String status(){
    File root=new File(context.getFilesDir(),"toolchain");
    File gradle=new File(root,"gradle/bin/gradle");
    File sdk=new File(root,"android-sdk");
    return "Gradle: "+(gradle.canExecute()?"READY":"MISSING")+
      "\nAndroid SDK: "+(sdk.isDirectory()?"FOUND":"MISSING")+
      "\nExpected toolchain root: "+root.getAbsolutePath()+
      "\nBuild target: selected project / app:assembleDebug (offline)";
  }
  public String build(File project,boolean approved)throws Exception{
    if(!approved)throw new SecurityException("Build requires explicit user approval.");
    File root=new File(context.getFilesDir(),"toolchain");
    File gradle=new File(root,"gradle/bin/gradle");
    File sdk=new File(root,"android-sdk");
    if(!gradle.canExecute()||!sdk.isDirectory())throw new FileNotFoundException("Offline toolchain is not installed. Provision Gradle and Android SDK first.\n"+status());
    File canonicalProject=project.getCanonicalFile(),workspace=new File(context.getFilesDir(),"workspace").getCanonicalFile();
    if(!canonicalProject.getPath().startsWith(workspace.getPath()+File.separator)||!new File(canonicalProject,"settings.gradle").isFile())
      throw new SecurityException("Select a valid project inside AIMobileStudio workspace.");
    ProcessBuilder pb=new ProcessBuilder(gradle.getAbsolutePath(),"--offline","--no-daemon","assembleDebug");
    pb.directory(canonicalProject);pb.redirectErrorStream(true);
    Map<String,String> env=pb.environment();env.put("ANDROID_HOME",sdk.getAbsolutePath());env.put("ANDROID_SDK_ROOT",sdk.getAbsolutePath());
    Process process=pb.start();ByteArrayOutputStream log=new ByteArrayOutputStream();
    try(InputStream in=process.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))>0){log.write(b,0,n);if(log.size()>2*1024*1024)break;}}
    int code=process.waitFor();String output=log.toString("UTF-8");
    if(code!=0)throw new IOException("Gradle build failed (exit "+code+").\n"+output);
    return "BUILD SUCCESS\n"+output+"\nAPK: "+new File(canonicalProject,"app/build/outputs/apk/debug/app-debug.apk").getAbsolutePath();
  }
}
