package com.offlineide;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** ChatGPT plan-usage client. Never sends project files unless the user asks the agent to inspect them. */
public final class ChatGPTAgent {
  private final ChatGPTAuth auth;
  public ChatGPTAgent(Context c){auth=new ChatGPTAuth(c);}
  private String request(String url,String method,String body,String token)throws Exception{
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod(method);
    c.setConnectTimeout(15000);c.setReadTimeout(120000);c.setRequestProperty("Authorization","Bearer "+token);
    if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}}
    int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);
    String s=out.toString("UTF-8");c.disconnect();if(code<200||code>=300)throw new IOException("OpenAI HTTP "+code+": "+s);return s;
  }
  public JSONArray models()throws Exception{
    JSONObject cred=auth.load();if(cred==null||!cred.optBoolean("plan_usage_enabled"))throw new SecurityException("ChatGPT plan usage permission is not enabled.");
    JSONObject data=new JSONObject(request("https://api.openai.com/v1/models","GET",null,cred.getString("access_token")));
    JSONArray source=data.optJSONArray("models");if(source==null)source=data.optJSONArray("data");
    JSONArray result=new JSONArray();if(source!=null)for(int i=0;i<source.length();i++){JSONObject m=source.optJSONObject(i);if(m==null)continue;
      if(!m.has("visibility")||"list".equals(m.optString("visibility")))result.put(new JSONObject().put("id",m.optString("slug",m.optString("id"))).put("name",m.optString("display_name",m.optString("id"))));}
    return result;
  }
  public String generate(String instruction,String projectSummary)throws Exception{
    JSONObject cred=auth.load();if(cred==null||!cred.optBoolean("plan_usage_enabled"))throw new SecurityException("Sign in and enable ChatGPT plan usage first.");
    JSONArray available=models();if(available.length()==0)throw new IOException("No models available for this account.");
    String model=available.getJSONObject(0).getString("id");
    String prompt="You are the coding agent inside AIMobileStudio. Return ONLY valid JSON with schema {\"summary\":string,\"files\":[{\"path\":string,\"content\":string}]}. "+
      "Create complete production-minded Android Java/XML source files. Never include absolute paths, ../, shell commands, secrets or API keys. "+
      "Do not overwrite files not requested. User request: "+instruction+"\nExisting project files summary:\n"+projectSummary;
    JSONObject body=new JSONObject().put("model",model).put("store",false).put("stream",false)
      .put("input",new JSONArray().put(new JSONObject().put("role","user").put("content",prompt)));
    JSONObject response=new JSONObject(request("https://api.openai.com/v1/responses","POST",body.toString(),cred.getString("access_token")));
    StringBuilder text=new StringBuilder();JSONArray output=response.optJSONArray("output");
    if(output!=null)for(int i=0;i<output.length();i++){JSONObject item=output.optJSONObject(i);JSONArray content=item==null?null:item.optJSONArray("content");
      if(content!=null)for(int j=0;j<content.length();j++){JSONObject part=content.optJSONObject(j);if(part!=null&&"output_text".equals(part.optString("type")))text.append(part.optString("text"));}}
    if(text.length()==0)text.append(response.optString("output_text",""));
    if(text.length()==0)throw new IOException("No text returned from Responses API.");
    return text.toString().replaceAll("^\\s*\\x60{3}(?:json)?\\s*","").replaceAll("\\s*\\x60{3}\\s*$","");
  }
}
