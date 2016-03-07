package com.aniketpednekar.netra;

import android.content.Context;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aniketpednekar on 3/6/16.
 */
public class RetrieveTags extends AsyncTask<byte[], Void, List<String>> {

    private Exception exception;
    private Context context;
    private TextView captionTextView;
    private TextToSpeech tts;
    private boolean isSpeaking;
    private boolean showText;

    final String CLARIFAI_ID = "9GCHk6DcJOqdGx_KT3WbLb2JVUHYUhUkfF7HfC2J";
    final String CLARIFAI_SECRET = "g6MxcRw_Ou0cb2c_PkQleV1UirufHZgL2o69o7Fy";

    public RetrieveTags(Context appcontext, TextView captionTv, TextToSpeech t1, boolean speaking, boolean st){
        context = appcontext;
        captionTextView = captionTv;
        tts = t1;
        isSpeaking  =speaking;
        showText = st;
    }

    protected List<String> doInBackground(byte[]... params) {
        try {
            HttpClient httpClient0 = new DefaultHttpClient();
            HttpPost httpPost0 = new HttpPost("https://api.imgur.com/3/image");
            httpPost0.setHeader("Authorization", "Client-ID 24ab1294f988a14");
            System.out.println("Length of the byte array sent" + params[0].length);
            httpPost0.setEntity(new ByteArrayEntity(params[0]));
            HttpResponse response0 = httpClient0.execute(httpPost0);
            if(response0.getStatusLine().getStatusCode()!=200)
                return null;
            HttpEntity ent0 = response0.getEntity();

            String imgur = EntityUtils.toString(ent0);
            Log.d("IMGUR", imgur);
            JSONObject reader0 = new JSONObject(imgur);
            JSONObject imgdata = reader0.getJSONObject("data");

            String link = imgdata.getString("link");
            Log.d("IMGUR LINK", link);

            //{'grant_type':'client_credentials','client_id':CLARIFAI_ID, 'client_secret':CLARIAI_SECRET}
            String clarifaiTokenUrl = "https://api.clarifai.com/v1/token/";
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(clarifaiTokenUrl);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("grant_type", "client_credentials"));
            nameValuePairs.add(new BasicNameValuePair("client_id", CLARIFAI_ID));
            nameValuePairs.add(new BasicNameValuePair("client_secret", CLARIFAI_SECRET));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs,"UTF-8");
            httpPost.setEntity(entity);
            Log.d("Clarifai Token", "Inside Code");
            //httpPost.setHeader("Authorization", "Client-ID 24ab1294f988a14");
            //System.out.println("Length of the byte array sent" + params[0].length);
            //httpPost.setEntity(new ByteArrayEntity(params[0]));
            HttpResponse response = httpClient.execute(httpPost);
            if(response.getStatusLine().getStatusCode()!=200){

                Log.d("Clarifai Token", "HTTP CALL FAILED" + String.valueOf(response.getStatusLine().getStatusCode()));
                Log.d("Entity", EntityUtils.toString(response.getEntity()));
                return null;
            }
            HttpEntity ent = response.getEntity();

            String clarifaiToken = EntityUtils.toString(ent);
            Log.d("Clarifai Token", clarifaiToken);
            JSONObject reader = new JSONObject(clarifaiToken);
            String token = reader.getString("access_token");
            Log.d("TOKEN", token);
            HttpClient httpClient2 = new DefaultHttpClient();
            HttpPost httpPost2 = new HttpPost("https://api.clarifai.com/v1/tag");
            httpPost2.setHeader("Authorization", "Bearer " + token);
            List<NameValuePair> nameValuePairs2 = new ArrayList<NameValuePair>(1);
            nameValuePairs2.add(new BasicNameValuePair("url", link));
            UrlEncodedFormEntity entity2 = new UrlEncodedFormEntity(nameValuePairs2,"UTF-8");
            httpPost2.setEntity(entity2);

            HttpResponse response2 = httpClient2.execute(httpPost2);
            if(response2.getStatusLine().getStatusCode()!=200){
                Log.d("STATUS", String.valueOf(response2.getStatusLine().getStatusCode()));
                String tags = EntityUtils.toString(response2.getEntity());
                Log.d("TAGS", tags);
                return null;
            }
            //data["results"][0]["result"]["tag"]["classes"]

            String tags = EntityUtils.toString(response2.getEntity());
            Log.d("TAGS", tags);
            JSONObject reader2 = new JSONObject(tags);
            JSONObject result2Json = (JSONObject) reader2.getJSONArray("results").get(0);
            //JSONObject result2Json = new JSONObject(result2);
            JSONArray tagarray = result2Json.getJSONObject("result").getJSONObject("tag").getJSONArray("classes");

            ArrayList<String> finaltags = new ArrayList<String>();
            for(int i = 0; i < tagarray.length(); i++){
                finaltags.add(tagarray.getString(i));
            }

            for(String i: finaltags)
                Log.d("CLARIFAI TAG LIST", i);

            return finaltags.subList(0,5);

        } catch (Exception e) {
            this.exception = e;
            Log.d("EXCEPTION", e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<String> feed){
        if(feed==null){
            String pleaseTry = "Please try again";
            captionTextView.setText(pleaseTry);
            if(showText)
                captionTextView.setVisibility(View.VISIBLE);
            tts.speak(pleaseTry, TextToSpeech.QUEUE_FLUSH, null);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            captionTextView.setVisibility(View.INVISIBLE);
            return;
        }
        for (String i: feed){
            //while(isSpeaking);
            isSpeaking = true;
            Log.d("feed_tag", i);
            captionTextView.setVisibility(View.VISIBLE);
            captionTextView.setText(i);

            tts.speak(i, TextToSpeech.QUEUE_FLUSH, null);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        /*try{
            JSONObject reader = new JSONObject(feed);
            String data  = reader.getString("caption");
            Toast.makeText(context, data, Toast.LENGTH_LONG)
                    .show();
        }
        catch(org.json.JSONException e){

        }*/
    }
}