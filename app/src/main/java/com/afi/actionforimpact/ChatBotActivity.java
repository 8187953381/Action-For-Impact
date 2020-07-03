package com.afi.actionforimpact;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.afi.actionforimpact.services.ChatBotBridge;
import com.afi.lexsdk.Constants;


import java.util.ArrayList;
import java.util.Locale;

import static com.afi.actionforimpact.ColorUtil.changeWindowColor;

//import static com.infosysit.lexsdk.ColorUtil.changeWindowColor;

public class ChatBotActivity extends AppCompatActivity implements RecognitionListener {

    public static WebView chatBot = null;
    private EditText textBox ;
    private ImageView imageBox;
    private ImageView closeBtn;
    public static TextToSpeech mTextToSpeech;
    public static Boolean textToSpeech = true;
    private FloatingActionButton volumeUpDown = null;
    private TextView listenngTextBox;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private boolean chatBotLoaded = false;
    private String LOG_TAG = "VoiceRecognitionActivity";
//    public  static Synthesizer m_syn;
    Animation animation,animation1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_chat_bot);
        chatBot = (WebView) findViewById(R.id.chatBotWebView);
        closeBtn = (ImageView) findViewById(R.id.closeButton);

        volumeUpDown = (FloatingActionButton) findViewById(R.id.fabVolBtn);
        listenngTextBox = (TextView) findViewById(R.id.hintListening);

//        progressBar.setVisibility(View.VISIBLE);

        textBox = (EditText) findViewById(R.id.textBlock);
        textBox.addTextChangedListener(mTextEditorWatcher);
        imageBox = (ImageView) findViewById(R.id.imageBlock);
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        changeWindowColor(this);
        loadWebView();

        imageBox.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    Log.d("chatBotMicClicked","clicked");
                    if(imageBox.getTag().equals("mic_button")){
                        imageBox.startAnimation(animation);
                        listenngTextBox.setVisibility(View.VISIBLE);
                        textBox.setHint("Hold to record, Release to send");
                        mTextToSpeech.stop();
                        speech.startListening(recognizerIntent);
                    }
                    else{
                        Log.d("chatBotMicClicked","Text "+textBox.getText());
                        chatBot.evaluateJavascript("speechRecognise('"+textBox.getText()+"')",null);
                        textBox.setText("");
                    }

                }


                if(event.getAction() == MotionEvent.ACTION_UP){
                    Log.d("chatBotMicClicked","removed");
                    if(speech != null && imageBox.getTag().equals("mic_button")){
                        Log.d("mTooltip","Reached here");
                        textBox.setHint("Type a message");
                        listenngTextBox.setVisibility(View.GONE);
                        imageBox.startAnimation(animation1);
                        speech.stopListening();
                    }

                }

                return false;
            }
        });
        animation= AnimationUtils.loadAnimation(getApplicationContext(),R.anim.zoom_out);
        animation1 = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.normal);





    }

    private void loadWebView(){
        final ChatBotBridge chatBotBridge = new ChatBotBridge(this);
        final ProgressBar loader = (ProgressBar) findViewById(R.id.progressBarChatBot);

        chatBot.getSettings().setJavaScriptEnabled(true);
        chatBot.getSettings().setDomStorageEnabled(true);
        chatBot.getSettings().setAllowFileAccessFromFileURLs(true);
        chatBot.addJavascriptInterface(chatBotBridge, "chatRef");
        chatBot.loadUrl(Constants.baseUrl+"/chat-bot?mic=0&keyboard=0&speech=1");
//        chatBot.loadUrl("file:///android_asset/chatbot-page/index.html");
        chatBot.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {


                return super.shouldOverrideUrlLoading(view, url);

            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                loader.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                new Handler().postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                closeBtn.setVisibility(View.VISIBLE);
                                textBox.setVisibility(View.VISIBLE);
                                imageBox.setVisibility(View.VISIBLE);
                                volumeUpDown.setVisibility(View.VISIBLE);
                                loader.setVisibility(View.GONE);
                                if(!chatBotLoaded){
                                    Log.d("appLoaded","Android Chat bot loaded");
                                    chatBot.evaluateJavascript("mobileAppLoaded()",null);
                                    chatBotLoaded = true;
                                }

                            }
                        }, 3000);



//                progressBar.setVisibility(View.GONE);
                // Added by nagasai_govula
//                String loadFinishedCheckScriptStr = "var __mobileApp__var__android__childNodeLength = document.childNodes && document.childNodes.length; if (__mobileApp__var__android__childNodeLength && __mobileApp__var__android__childNodeLength>0) { window.chatRef.appLoaded(); console.log('Called the app loaded function')} else {console.log('Child nodes empty')}";
//                Log.d("jsinterface", "onPageFinished: ");
//                chatBot.evaluateJavascript(loadFinishedCheckScriptStr, null);

            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Log.d("onReceivedError","Error "+error +"request "+request);
                super.onReceivedError(view, request, error);
            }
        });
        chatBot.setWebChromeClient(new WebChromeClient());
        // inline
        chatBot.addJavascriptInterface(chatBotBridge, "chatRef");
        mTextToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    mTextToSpeech.setLanguage(Locale.UK);
                    mTextToSpeech.setPitch(0.7f);
                    mTextToSpeech.setSpeechRate(1.1f);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        chatBot.resumeTimers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
        }

    }

    @Override
    protected void onDestroy() {
        if(speech != null){
            speech.destroy();
        }
        if(mTextToSpeech != null){
            mTextToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            Log.d("TextWatcher","before "+s.toString());
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Resources res = getResources(); /** from an Activity */
            if(s.length() > 0){
                imageBox.setTag("send_button");
                Log.d(LOG_TAG,"TAGSet: "+imageBox.getTag());
                imageBox.setImageDrawable(res.getDrawable(R.drawable.send_button));
//                imageBox.setImageDrawable(R.drawable.send_button);
            }
            else{
                imageBox.setTag("mic_button");
                imageBox.setImageDrawable(res.getDrawable(R.drawable.mic_button));
            }
            //This sets a textview to the current length
            Log.d("TextWatcher","on "+s.toString());
        }

        public void afterTextChanged(Editable s) {
            Log.d("TextWatcher","after "+s.toString());
        }
    };

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
    }

    @Override
    public void onError(int errorCode) {
        Log.d(LOG_TAG, "TAG: " +imageBox.getTag() );
        String errorMessage = getErrorText(errorCode);
        if(!errorMessage.isEmpty()){
            Log.d(LOG_TAG, "FAILED " + errorMessage);
            Toast.makeText(this,errorMessage,Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        chatBot.evaluateJavascript("speechRecognise('"+matches.get(0).trim()+"')",null);
        Log.d(LOG_TAG,"Result needed "+matches.get(0));
//        returnedText.setText(matches.get(0));
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
//        progressBar.setProgress((int) rmsdB);
    }

    public static String getErrorText(int errorCode) {
        String message = "";
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Provide microphone permission";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "Recognition Service is busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "Some error occured";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
        }
        return message;
    }




    public void closeChatBot(View view) {
        finish();
    }

    public void fabVolumeButton(View view) {
        Resources res = getResources();
        if(volumeUpDown.getTag().equals("volumeOff")){
            volumeUpDown.setImageDrawable(res.getDrawable(R.drawable.volume_on));
            volumeUpDown.setTag("volumeOn");
            textToSpeech = true;
        }
        else{
            volumeUpDown.setImageDrawable(res.getDrawable(R.drawable.volume_off));
            volumeUpDown.setTag("volumeOff");
            if(mTextToSpeech != null){
                mTextToSpeech.stop();
            }
            textToSpeech = false;
        }

    }
}
