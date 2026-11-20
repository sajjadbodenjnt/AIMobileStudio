package com.offlineide;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Open-source Sign in with ChatGPT OAuth client. Credentials are encrypted with Android Keystore. */
public final class ChatGPTAuth {
  private static final String AUTH="https://auth.openai.com/api/accounts/authorize";
  private static final String TOKEN="https://auth.openai.com/api/accounts/oauth/token";
  private static final String JWKS="https://auth.openai.com/.well-known/jwks.json";
  private static final String ISSUER="https://auth.openai.com";
  private static final String RESOURCE="https://api.openai.com/v1";
  private static final String PREF="chatgpt_auth";
  private final Context context;
  public ChatGPTAuth(Context c){context=c.getApplicationContext();}

  private static String b64(byte[] b){return Base64.encodeToString(b,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);}
  private static byte[] unb64(String s){return Base64.decode(s,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);}
  private static String random(int n){byte[] b=new byte[n];new SecureRandom().nextBytes(b);return b64(b);}
  private static String enc(String s){try{return java.net.URLEncoder.encode(s,"UTF-8");}catch(Exception e){throw new RuntimeException(e);}}
  private static String http(String url,String method,String body,String bearer)throws Exception{
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
    c.setRequestMethod(method);c.setConnectTimeout(15000);c.setReadTimeout(30000);
    if(bearer!=null)c.setRequestProperty("Authorization","Bearer "+bearer);
    if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}}
    int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);
    String result=out.toString("UTF-8");c.disconnect();if(code<200||code>=300)throw new IOException("HTTP "+code+": "+result);return result;
  }
  private SecretKey key()throws Exception{
    KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
    if(!ks.containsAlias("aimobilestudio.chatgpt")){KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");kg.init(new KeyGenParameterSpec.Builder("aimobilestudio.chatgpt",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());kg.generateKey();}
    return ((KeyStore.SecretKeyEntry)ks.getEntry("aimobilestudio.chatgpt",null)).getSecretKey();
  }
  private void save(JSONObject data)throws Exception{
    Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
    String value=b64(c.getIV())+"."+b64(c.doFinal(data.toString().getBytes(StandardCharsets.UTF_8)));
    context.getSharedPreferences(PREF,0).edit().putString("credential",value).apply();
  }
  public JSONObject load()throws Exception{
    String value=context.getSharedPreferences(PREF,0).getString("credential",null);if(value==null)return null;
    String[] p=value.split("\\.",2);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,unb64(p[0])));
    JSONObject record=new JSONObject(new String(c.doFinal(unb64(p[1])),StandardCharsets.UTF_8));
    if(record.optLong("expires_at",0)<System.currentTimeMillis()+60000L)record=refresh(record);
    return record;
  }
  private JSONObject refresh(JSONObject old)throws Exception{
    String rt=old.optString("refresh_token","");
    if(rt.isEmpty())throw new IOException("ChatGPT session expired. Sign in again.");
    String form="grant_type=refresh_token&client_id="+enc(old.getString("client_id"))+"&refresh_token="+enc(rt)+"&resource="+enc(RESOURCE);
    JSONObject fresh=new JSONObject(http(TOKEN,"POST",form,null));
    old.put("access_token",fresh.getString("access_token"));
    if(!fresh.optString("refresh_token","").isEmpty())old.put("refresh_token",fresh.getString("refresh_token"));
    old.put("expires_at",System.currentTimeMillis()+fresh.optLong("expires_in",3600)*1000L);
    if(fresh.has("scope")){String scopes=fresh.optString("scope","");old.put("scope",scopes);old.put("plan_usage_enabled",Arrays.asList(scopes.split(" ")).contains("chatgpt.tokens.use.direct"));}
    save(old);return old;
  }
  public void disconnect(){context.getSharedPreferences(PREF,0).edit().clear().apply();}
  public boolean isConnected(){try{return load()!=null;}catch(Exception e){return false;}}

  public JSONObject signIn()throws Exception{
    SharedPreferences p=context.getSharedPreferences(PREF,0);
    String host=p.getString("host_id",null);if(host==null){host="urn:uuid:"+UUID.randomUUID();p.edit().putString("host_id",host).apply();}
    JSONObject previous=load();
    String registeredClient=previous==null?null:previous.optString("client_id",null);
    String state=random(32),nonce=random(32),verifier=random(48);
    String challenge=b64(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
    ServerSocket server=new ServerSocket();server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0));
    server.setSoTimeout(180000);int port=server.getLocalPort();
    String redirect="http://127.0.0.1:"+port+"/auth/callback";
    String query="client_id="+enc(registeredClient==null?"dynamic_agent_client":registeredClient)+
      (registeredClient==null?"&agent_name_hint="+enc("AIMobileStudio"):"&id_token_hint="+enc(previous.optString("id_token","")))+
      "&ext_agent_host_id="+enc(host)+"&response_type=code&redirect_uri="+enc(redirect)+
      "&scope="+enc("openid profile email offline_access resource.invoke chatgpt.tokens.use.direct")+
      "&resource="+enc(RESOURCE)+"&state="+enc(state)+"&nonce="+enc(nonce)+
      "&code_challenge_method=S256&code_challenge="+enc(challenge);
    try{
      android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_VIEW,Uri.parse(AUTH+"?"+query));
      intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(intent);
      Socket socket=server.accept();socket.setSoTimeout(15000);
      BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));
      String first=reader.readLine();if(first==null||!first.startsWith("GET /auth/callback?"))throw new IOException("Invalid OAuth callback path");
      String target=first.split(" ")[1];String qs=target.substring(target.indexOf('?')+1);Map<String,String> params=new HashMap<>();
      for(String pair:qs.split("&")){String[] a=pair.split("=",2);params.put(URLDecoder.decode(a[0],"UTF-8"),a.length>1?URLDecoder.decode(a[1],"UTF-8"):"");}
      String page="HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\n\r\n<html><body>ChatGPT authorization received. Return to AIMobileStudio.</body></html>";
      socket.getOutputStream().write(page.getBytes(StandardCharsets.UTF_8));socket.close();
      if(!state.equals(params.get("state")))throw new SecurityException("OAuth state mismatch");
      if(params.containsKey("error"))throw new IOException("Authorization denied: "+params.get("error"));
      String code=params.get("code"),client=params.get("client_id");
      if(client==null)client=registeredClient;
      if(code==null||client==null||!client.startsWith("oaiapp_"))throw new IOException("Registration incomplete: issued client_id missing");
      String form="grant_type=authorization_code&client_id="+enc(client)+"&code="+enc(code)+
        "&code_verifier="+enc(verifier)+"&redirect_uri="+enc(redirect)+"&resource="+enc(RESOURCE);
      JSONObject tokens=new JSONObject(http(TOKEN,"POST",form,null));
      String idToken=tokens.optString("id_token");JSONObject claims=verifyIdToken(idToken,client,nonce);
      String scopes=tokens.optString("scope","");boolean allowed=Arrays.asList(scopes.split(" ")).contains("chatgpt.tokens.use.direct");
      if(tokens.optString("access_token").isEmpty())throw new SecurityException("No access token returned");
      JSONObject record=new JSONObject().put("client_id",client).put("host_id",host)
        .put("id_token",idToken).put("access_token",tokens.getString("access_token"))
        .put("refresh_token",tokens.optString("refresh_token","")).put("scope",scopes)
        .put("email",claims.optString("email","")).put("subject",claims.getString("sub"))
        .put("expires_at",System.currentTimeMillis()+tokens.optLong("expires_in",3600)*1000L)
        .put("plan_usage_enabled",allowed);
      save(record);return record;
    }finally{try{server.close();}catch(Exception ignored){}}
  }
  private JSONObject verifyIdToken(String jwt,String client,String nonce)throws Exception{
    String[] parts=jwt.split("\\.");if(parts.length!=3)throw new SecurityException("Malformed ID token");
    JSONObject header=new JSONObject(new String(unb64(parts[0]),StandardCharsets.UTF_8));
    if(!"RS256".equals(header.optString("alg")))throw new SecurityException("Unexpected JWT algorithm");
    JSONObject keys=new JSONObject(http(JWKS,"GET",null,null));JSONArray list=keys.getJSONArray("keys");JSONObject jwk=null;
    for(int i=0;i<list.length();i++)if(list.getJSONObject(i).optString("kid").equals(header.optString("kid")))jwk=list.getJSONObject(i);
    if(jwk==null)throw new SecurityException("Signing key not found");
    BigInteger n=new BigInteger(1,unb64(jwk.getString("n"))),e=new BigInteger(1,unb64(jwk.getString("e")));
    PublicKey pub=KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n,e));
    Signature sig=Signature.getInstance("SHA256withRSA");sig.initVerify(pub);sig.update((parts[0]+"."+parts[1]).getBytes(StandardCharsets.US_ASCII));
    if(!sig.verify(unb64(parts[2])))throw new SecurityException("ID token signature invalid");
    JSONObject claims=new JSONObject(new String(unb64(parts[1]),StandardCharsets.UTF_8));
    if(!ISSUER.equals(claims.optString("iss"))||!client.equals(claims.optString("aud"))||
      !nonce.equals(claims.optString("nonce"))||claims.optLong("exp",0)<System.currentTimeMillis()/1000L||
      claims.optString("sub").isEmpty())throw new SecurityException("ID token claims invalid");
    return claims;
  }
}
