package com.afi.actionforimpact.services;

import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.google.gson.JsonObject;
import com.afi.actionforimpact.ChatBotActivity;
import com.afi.actionforimpact.ExternalPlayerActivity;
import com.afi.actionforimpact.HomeActivity;
import com.afi.actionforimpact.Util;

import com.google.gson.JsonParser;

import com.afi.lexsdk.UtilityJava;

import static com.afi.lexsdk.Constants.CHAT_BOT;
import static com.afi.lexsdk.Constants.EXTERNAL_OPEN;
import static com.afi.lexsdk.Constants.PATH_PARAM;
import static com.afi.lexsdk.Constants.SEARCH_PARAM;

/**
 * Created by akansha.goyal on 7/3/2018.
 */

public class ChatBotBridge {

    private ChatBotActivity mChatBotActivity;


    public ChatBotBridge(ChatBotActivity chatBotActivity) {
        mChatBotActivity = chatBotActivity;
    }


    @JavascriptInterface
    public void chatBotResponse(String response) {
        response = response.replaceAll("[^a-zA-Z0-9._-]", " ");
        if (ChatBotActivity.textToSpeech) {
            Log.d("chatBotResponse", response);
            String textToSynthesize = response.trim().replaceAll("\\<[^>]*>", "").replaceAll("&nbsp", "").replaceAll("[^a-zA-Z0-9. ]", "");
            Log.d("chatBotResponse", textToSynthesize.replaceAll("-", "  "));
            ChatBotActivity.mTextToSpeech.speak(textToSynthesize.replaceAll("-", "  "), TextToSpeech.QUEUE_FLUSH, null);
        }
    }






    @JavascriptInterface
    public void eventFromChatbot(String eventData){
        Log.d("eventFromChatbot","String0: "+eventData);
        JsonParser jsonParser = new JsonParser();
        JsonObject eventDataJson = jsonParser.parse(eventData).getAsJsonObject();
        Log.d("eventFromChatbot","String2: "+eventDataJson.get("eventName").getAsString());
        if(eventDataJson.get("eventName").getAsString().equalsIgnoreCase("APP_LOADED")){
            Log.d("eventFromChatbot","window.receiveToken(\"" + UtilityJava.tokenValue.trim().replaceAll("Bearer ", "") + "\")");
            final String setTokenScriptStr = "window.receiveToken(\"" + UtilityJava.tokenValue.trim().replaceAll("Bearer ", "") + "\")";
            (mChatBotActivity).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mChatBotActivity.chatBot.evaluateJavascript(setTokenScriptStr, null);
                    } catch (Exception e) {
                        Log.e("jsinterface", "error while sending token: " + e.getMessage());
                    }
                }
            });
        }
        if(eventDataJson.get("eventName").getAsString().equalsIgnoreCase("OPEN_EXTERNAL_URL")){
            Log.d("eventFromChatbot","data: "+eventDataJson.get("data").getAsJsonObject().get("value").getAsString());
            Intent activityIntent = new Intent(mChatBotActivity,ExternalPlayerActivity.class);
            activityIntent.putExtra(PATH_PARAM,eventDataJson.get("data").getAsJsonObject().get("value").getAsString());
            activityIntent.putExtra(EXTERNAL_OPEN,CHAT_BOT);
            mChatBotActivity.startActivity(activityIntent);
        }

    }

    @JavascriptInterface
    public void navigateFromChatBot(String action , String value){
        Log.d("TestingCHatBotResponse","Reached here: "+value);
        if(action.equalsIgnoreCase("viewer")) {
            Util.navigateToPage(mChatBotActivity,"/"+value);
            return;
        } else {
            Intent homeActivity = new Intent(mChatBotActivity, HomeActivity.class);
            homeActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            homeActivity.putExtra(SEARCH_PARAM,value);
            mChatBotActivity.startActivity(homeActivity);
//            HomeActivity.loginPage.evaluateJavascript("navigateTo('"+action+"',"+value+")",null);
//            HomeActivity.loginPage.evaluateJavascript("navigateTo('" + action.toString(),value.toString() + "')", null);
        }
        Log.d("TestingCHatBot","value " + value);
        Log.d("TestingCHatBot","action " + action);
    }

}
