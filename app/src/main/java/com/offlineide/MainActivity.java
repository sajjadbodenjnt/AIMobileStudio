package com.offlineide;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
  private LinearLayout body;
  private TextView status;
  private EditText editor;
  private File workspace;
  private ChatGPTAuth chatAuth;
  private static final int PICK_IMAGES = 4107;
  private final ArrayList<Uri> selectedImages = new ArrayList<>();
  private ImageQueueStore imageQueue;
  private TextView imageStatus;
  private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density);}

  @Override public void onCreate(Bundle state){super.onCreate(state);chatAuth=new ChatGPTAuth(this);imageQueue=new ImageQueueStore(this);selectedImages.addAll(imageQueue.all());workspace=new File(getFilesDir(),"workspace");if(!workspace.exists())workspace.mkdirs();home();}
  private static void writeText(File file,String value)throws Exception{
    File parent=file.getParentFile();if(parent!=null&&!parent.exists())parent.mkdirs();
    try(FileOutputStream out=new FileOutputStream(file,false)){out.write(value.getBytes(StandardCharsets.UTF_8));out.flush();}
  }
  private static String readText(File file)throws Exception{
    try(FileInputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");
    }
  }
  private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);return t;}
  private Button action(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setOnClickListener(v->r.run());return b;}
  private void shell(String title){
    LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(12),dp(10),dp(12),dp(8));page.setBackgroundColor(Color.rgb(10,18,15));
    TextView heading=label(title,20);heading.setTypeface(null,Typeface.BOLD);heading.setPadding(0,dp(10),0,dp(14));page.addView(heading);
    ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(1);scroll.addView(body);
    page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));status=label("READY • LOCAL WORKSPACE",12);status.setTextColor(0xff6fe0a0);status.setPadding(0,dp(10),0,0);page.addView(status);setContentView(page);
  }
  @Override protected void onActivityResult(int requestCode,int resultCode,android.content.Intent data){
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode!=PICK_IMAGES||resultCode!=RESULT_OK||data==null)return;
    if(data.getClipData()!=null){android.content.ClipData clip=data.getClipData();for(int i=0;i<clip.getItemCount();i++){Uri u=clip.getItemAt(i).getUri();if(u!=null&&!selectedImages.contains(u))selectedImages.add(u);}}
    else if(data.getData()!=null&&!selectedImages.contains(data.getData()))selectedImages.add(data.getData());
    updateImageStatus();
  }
  private void chooseImages(){
    android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);intent.setType("image/*");
    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION|android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);\n    intent.putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(intent,PICK_IMAGES);
  }
  private void updateImageStatus(){if(imageStatus!=null)imageStatus.setText("Reference image queue: "+selectedImages.size()+" • Saved on this device");}\n  private void clearImageQueue(){imageQueue.clear();selectedImages.clear();updateImageStatus();}
  private void home(){
    shell("AIMOBILESTUDIO");
    body.addView(label("Offline-first Android app development studio",16));
    body.addView(label("Workspace: "+workspace.getAbsolutePath(),11));
    body.addView(action("New Android App Project",this::newProject));
    body.addView(action("Open Workspace",this::files));
    body.addView(action("✨ Create a complete app with ChatGPT",this::createWithChatGPT));
    body.addView(action("ChatGPT Coding Agent",this::chatGPT));
    body.addView(action("Local AI Assistant",this::ai));
    body.addView(action("APK Build Controller",this::buildInfo));
    body.addView(label("\nChatGPT-connected mode requires internet and separate authorization. Local AI and a provisioned offline build toolchain are separate modes.",13));
  }
  private void newProject(){
    EditText input=new EditText(this);input.setHint("App name (letters/numbers)");
    new android.app.AlertDialog.Builder(this).setTitle("Create Android app project").setView(input).setNegativeButton("Cancel",null)
      .setPositiveButton("Create",(d,w)->{String name=input.getText().toString().trim().replaceAll("[^A-Za-z0-9_]","");if(name.isEmpty())name="MyApp";
        try{File entry=ProjectGenerator.create(workspace,name);status.setText("PROJECT CREATED: "+name);edit(entry);}catch(Exception e){showError("Project error",e);}}).show();
  }
  private void edit(File file){
    shell("CODE EDITOR • "+file.getName());editor=new EditText(this);editor.setGravity(Gravity.TOP|Gravity.LEFT);editor.setTextSize(13);editor.setTypeface(Typeface.MONOSPACE);
    editor.setTextColor(0xffe3f3e9);editor.setBackgroundColor(0xff14211b);
    try{editor.setText(readText(file));}catch(Exception e){status.setText("OPEN ERROR: "+e.getMessage());}
    body.addView(editor,new LinearLayout.LayoutParams(-1,0,1));
    body.addView(action("Save",()->{try{writeText(file,editor.getText().toString());status.setText("SAVED: "+file.getName());}catch(Exception e){showError("Save error",e);}}));
    body.addView(action("Back",this::files));
  }
  private File[] projects(){File[] all=workspace.listFiles();if(all==null)return new File[0];ArrayList<File> p=new ArrayList<>();for(File f:all)if(f.isDirectory())p.add(f);return p.toArray(new File[0]);}
  private void files(){
    shell("PROJECT WORKSPACE");File[] all=workspace.listFiles();
    if(all==null||all.length==0)body.addView(label("No projects yet. Create a project first.",14));
    else for(File f:all){if(f.isDirectory()){body.addView(label("📁 "+f.getName(),15));File[] children=f.listFiles();if(children!=null)for(File c:children)if(c.isFile())body.addView(action("    "+c.getName(),()->edit(c)));}
      else body.addView(action(f.getName(),()->edit(f)));}
    body.addView(action("Home",this::home));
  }
  private void ai(){
    shell("LOCAL AI ASSISTANT");body.addView(label("Local endpoint: 127.0.0.1:11434 (Ollama)",13));
    EditText prompt=new EditText(this);prompt.setHint("Describe the code you want...");prompt.setTextColor(Color.WHITE);prompt.setMinLines(4);body.addView(prompt);
    body.addView(action("Ask local model",()->{String q=prompt.getText().toString().trim();if(q.isEmpty())return;status.setText("Connecting to local model...");
      new Thread(()->{String answer=LocalAI.generate("qwen2.5-coder:3b",q);runOnUiThread(()->new android.app.AlertDialog.Builder(this).setTitle("Local AI result").setMessage(answer).setPositiveButton("Close",null).show());}).start();}));
    body.addView(action("Home",this::home));
  }
  private void createWithChatGPT(){
    shell("CREATE APP WITH CHATGPT");
    body.addView(label("Describe the app you want. ChatGPT will generate a complete Android Studio project, then you review every file before it is saved.",14));
    EditText appName=new EditText(this);appName.setSingleLine(true);appName.setTextColor(Color.WHITE);appName.setHint("App name (example: MyShop)");body.addView(appName);
    selectedImages.clear();
    EditText prompt=new EditText(this);prompt.setTextColor(Color.WHITE);prompt.setMinLines(5);prompt.setHint("Describe every feature, screen and design you want. You can write in Bangla or English.");body.addView(prompt);
    body.addView(action("＋ Add design images / screenshots",this::chooseImages));imageStatus=label("",12);body.addView(imageStatus);updateImageStatus();body.addView(action("Clear saved image queue",this::clearImageQueue));
    body.addView(action("Generate complete Android app",()->{
      String rawName=appName.getText().toString().trim();
      String name=rawName.replaceAll("[^A-Za-z0-9_]","");
      String instruction=prompt.getText().toString().trim();
      final ArrayList<Uri> images=new ArrayList<>(selectedImages);
      if(name.isEmpty()||instruction.isEmpty()){status.setText("Enter an app name and describe your app.");return;}
      try{
        JSONObject profile=chatAuth.load();
        if(profile==null||!profile.optBoolean("plan_usage_enabled")){status.setText("Connect ChatGPT and approve plan usage first.");chatGPT();return;}
        File project=new File(workspace,name);
        if(project.exists()){status.setText("A project with this name already exists. Choose another name.");return;}
        if(!project.mkdirs())throw new IOException("Could not create project workspace.");
        status.setText("ChatGPT is designing and generating your complete app…");
        String fullRequest="Create a complete, buildable native Android application named "+name+". User requirements: "+instruction+
          "\\nGenerate every required project file, including settings.gradle, root build.gradle, gradle.properties, app/build.gradle, app/src/main/AndroidManifest.xml, Java Activity source, and all required XML resources. "+
          "Use Java and Android framework APIs only; avoid AndroidX and third-party libraries unless essential. Use compileSdk 35, minSdk 26, targetSdk 35, Java 17. "+
          "Implement the requested screens and interactions, polished responsive Material-like styling using framework widgets/drawables, validation, empty/loading/error states, and local persistence where appropriate. "+
          "Do not leave TODOs, pseudocode, placeholders, or omitted files. Ensure package names and manifest activity names match. Return only files needed for a complete Gradle Android project.";
        new Thread(()->{try{
          String raw=new ChatGPTAgent(this).generate(fullRequest,"",images);
          JSONObject plan=new JSONObject(raw);
          runOnUiThread(()->reviewPlan(project,plan));
        }catch(Exception e){runOnUiThread(()->{deleteTree(project);showError("App generation failed",e);});}}).start();
      }catch(Exception e){showError("Cannot start app generation",e);}
    }));
    body.addView(action("Back",this::home));
  }
  private void deleteTree(File file){
    if(file==null||!file.exists())return;
    File[] children=file.listFiles();if(children!=null)for(File child:children)deleteTree(child);
    try{file.delete();}catch(Exception ignored){}
  }
  private void chatGPT(){
    shell("CHATGPT CODING AGENT");
    try{
      JSONObject profile=chatAuth.load();
      if(profile==null){body.addView(label("Connect your ChatGPT account using the official system-browser authorization flow.",14));
        body.addView(action("Continue with ChatGPT",this::signInChatGPT));
      }else{
        body.addView(label("Connected account: "+profile.optString("email","ChatGPT account"),14));
        body.addView(label(profile.optBoolean("plan_usage_enabled")?"ChatGPT plan usage: ENABLED":"Identity connected; plan usage permission NOT granted.",13));
        body.addView(action("Reconnect / authorize again",this::signInChatGPT));
        body.addView(action("Disconnect ChatGPT",()->new android.app.AlertDialog.Builder(this).setMessage("Remove encrypted ChatGPT credentials from this app?").setNegativeButton("Cancel",null).setPositiveButton("Disconnect",(d,w)->{chatAuth.disconnect();chatGPT();}).show()));
        if(profile.optBoolean("plan_usage_enabled"))agentPrompt();
      }
    }catch(Exception e){body.addView(label("Connection state error: "+e.getMessage(),12));body.addView(action("Continue with ChatGPT",this::signInChatGPT));}
    body.addView(action("Home",this::home));
  }
  private void signInChatGPT(){
    status.setText("Opening official ChatGPT authorization…");
    new Thread(()->{try{JSONObject result=chatAuth.signIn();runOnUiThread(()->{new android.app.AlertDialog.Builder(this).setTitle("ChatGPT connected")
      .setMessage("Account: "+result.optString("email","Connected")+"\nPlan usage: "+(result.optBoolean("plan_usage_enabled")?"Enabled":"Not granted. Reconnect and approve ChatGPT plan usage to use the coding agent."))
      .setPositiveButton("Continue",(d,w)->chatGPT()).show();});}
      catch(Exception e){runOnUiThread(()->showError("ChatGPT sign-in failed",e));}}).start();
  }
  private void agentPrompt(){
    body.addView(label("\nDescribe the Android app or code change. The agent proposes files first; nothing is written until you approve.",13));
    selectedImages.clear();
    EditText request=new EditText(this);request.setTextColor(Color.WHITE);request.setMinLines(4);request.setHint("Example: Build a polished expense tracker with dashboard, transactions and settings…");body.addView(request);
    body.addView(action("＋ Add reference images",this::chooseImages));imageStatus=label("",12);body.addView(imageStatus);updateImageStatus();body.addView(action("Clear saved image queue",this::clearImageQueue));
    body.addView(action("Generate project files",()->{
      String q=request.getText().toString().trim();if(q.isEmpty()&&selectedImages.isEmpty())return;final ArrayList<Uri> images=new ArrayList<>(selectedImages);File[] ps=projects();
      if(ps.length==0){status.setText("Create a project before asking the agent to edit files.");return;}
      chooseProject(ps,project->{status.setText("ChatGPT is planning and generating code…");
        new Thread(()->{try{String summary=projectSummary(project);String raw=new ChatGPTAgent(this).generate(q,summary,images);JSONObject plan=new JSONObject(raw);JSONArray files=plan.getJSONArray("files");
          runOnUiThread(()->reviewPlan(project,plan));}catch(Exception e){runOnUiThread(()->showError("Agent request failed",e));}}).start();
      });
    }));
  }
  private interface ProjectChoice{void selected(File project);}
  private void chooseProject(File[] ps,ProjectChoice cb){
    String[] names=new String[ps.length];for(int i=0;i<ps.length;i++)names[i]=ps[i].getName();
    new android.app.AlertDialog.Builder(this).setTitle("Choose project").setItems(names,(d,w)->cb.selected(ps[w])).show();
  }
  private String projectSummary(File root)throws Exception{
    StringBuilder s=new StringBuilder();ArrayDeque<File> q=new ArrayDeque<>();q.add(root);int count=0;
    while(!q.isEmpty()&&count<35){File f=q.removeFirst();File[] list=f.listFiles();if(list==null)continue;
      for(File x:list){if(x.isDirectory()){if(!x.getName().equals("build")&&!x.getName().equals(".gradle"))q.add(x);}
        else if(!sensitiveFile(x)&&x.length()<24000&&count<35){String rel=x.getCanonicalPath().substring(root.getCanonicalPath().length()+1);s.append("\n--- ").append(rel).append(" ---\n").append(readText(x));count++;}}}
    return s.toString();
  }
  private boolean sensitiveFile(File file){
    String n=file.getName().toLowerCase(Locale.ROOT);
    return n.equals(".env")||n.startsWith(".env.")||n.equals("local.properties")||
      n.endsWith(".jks")||n.endsWith(".keystore")||n.contains("secret")||
      n.contains("credential")||n.equals("keystore.properties");
  }
  private File safeTarget(File root,String relative)throws Exception{
    if(relative==null||relative.trim().isEmpty()||relative.startsWith("/")||relative.contains("\\")||relative.matches("^[A-Za-z]:.*"))throw new SecurityException("Invalid file path");
    for(String part:relative.split("/"))if(part.equals("..")||part.equals(".")||part.isEmpty())throw new SecurityException("Path traversal rejected");
    File target=new File(root,relative).getCanonicalFile();String base=root.getCanonicalPath()+File.separator;
    if(!target.getPath().startsWith(base))throw new SecurityException("File is outside project workspace");
    if(target.exists()&&target.isDirectory())throw new SecurityException("Target is a directory");
    return target;
  }
  private void reviewPlan(File project,JSONObject plan){
    try{
      JSONArray files=plan.getJSONArray("files");StringBuilder preview=new StringBuilder(plan.optString("summary","Code proposal"));
      for(int i=0;i<files.length();i++){JSONObject f=files.getJSONObject(i);String path=f.getString("path");String content=f.getString("content");
        safeTarget(project,path);if(content.length()>200000)throw new SecurityException("Generated file too large: "+path);
        preview.append("\n\n• ").append(path).append(" (").append(content.length()).append(" chars)");}
      new android.app.AlertDialog.Builder(this).setTitle("Review AI code changes").setMessage(preview.toString())
        .setNegativeButton("Discard",null).setPositiveButton("Apply files",(d,w)->applyPlan(project,files)).show();
    }catch(Exception e){showError("Unsafe or invalid AI proposal",e);}
  }
  private void applyPlan(File project,JSONArray files){
    try{
      for(int i=0;i<files.length();i++){JSONObject f=files.getJSONObject(i);File target=safeTarget(project,f.getString("path"));
        if(target.exists()){File backup=new File(target.getParentFile(),target.getName()+".aims-backup");writeText(backup,readText(target));}
        writeText(target,f.getString("content"));}
      new android.app.AlertDialog.Builder(this).setTitle("Changes applied").setMessage(files.length()+" project files written. Existing files were backed up with .aims-backup suffix.")
        .setPositiveButton("OK",(d,w)->chatGPT()).show();
    }catch(Exception e){showError("Apply failed",e);}
  }
  private void buildInfo(){
    shell("APK BUILD CONTROLLER");BuildController controller=new BuildController(this);body.addView(label(controller.status(),13));
    body.addView(label("\nBuild is deliberately disabled until a compatible offline Gradle + Android SDK toolchain is provisioned inside app-private storage. It never executes arbitrary shell commands.",12));
    File[] ps=projects();if(ps.length>0)body.addView(action("Build selected project (requires approval)",()->chooseProject(ps,project->
      new android.app.AlertDialog.Builder(this).setTitle("Approve APK build").setMessage("Run Gradle --offline assembleDebug for "+project.getName()+"?").setNegativeButton("Cancel",null)
        .setPositiveButton("Build",(d,w)->{status.setText("Building…");new Thread(()->{try{String log=controller.build(project,true);runOnUiThread(()->new android.app.AlertDialog.Builder(this).setTitle("Build result").setMessage(log).setPositiveButton("OK",null).show());}
          catch(Exception e){runOnUiThread(()->showError("Build failed",e));}}).start();}).show())));
    body.addView(action("Home",this::home));
  }
  private void showError(String title,Exception e){new android.app.AlertDialog.Builder(this).setTitle(title).setMessage(e.getClass().getSimpleName()+": "+e.getMessage()).setPositiveButton("OK",null).show();}
}
