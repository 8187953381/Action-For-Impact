package com.afi.actionforimpact;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.afi.actionforimpact.services.ConnectivityReceiver;
import com.afi.actionforimpact.services.OpenRapBridge;
import com.afi.lexsdk.Constants;
import com.afi.lexsdk.persistence.SharedPrefrence;

import static com.afi.actionforimpact.ColorUtil.changeWindowColor;
import static com.afi.lexsdk.Constants.openRapUrl;

public class OpenRapActivity extends AppCompatActivity implements ConnectivityReceiver.ConnectivityReceiverListener {

    private ConnectivityReceiver connectivityReceiver;

    private AutoCloseBottomSheetBehavior mSheetBehavior;

    private Snackbar mSnackbarOffline;
    private Snackbar mSnackbarOnline;
    private WebView mWebView;
    private final String openRapIndex = "/public/hot-1/2/for-app/index.html";

    boolean isConnectedToInternet;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_open_rap);

        Toolbar toolbar = findViewById(R.id.openrap_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_hamburger));

        mWebView = findViewById(R.id.openrap_webview);
        ImageView mButtonLogo = findViewById(R.id.lex_logo);


        changeWindowColor(this);
        loadWebView();



        mButtonLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnectedToInternet) {
                    goOnline();
                } else {
                    Toast.makeText(OpenRapActivity.this, "Please check your Internet Connectivity", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ConstraintLayout mSheetLayout = findViewById(R.id.openrap_instructions_layout);
        mSheetBehavior = (AutoCloseBottomSheetBehavior) AutoCloseBottomSheetBehavior.from(mSheetLayout);

        CoordinatorLayout openrapLayout = findViewById(R.id.openrap_layout);
        mSnackbarOffline = Snackbar.make(openrapLayout, "No Connection", Snackbar.LENGTH_INDEFINITE);
        mSnackbarOffline.setAction("GO OFFLINE", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goOffline();
            }
        });
        mSnackbarOffline.setActionTextColor(Color.WHITE);
        mSnackbarOnline = Snackbar.make(openrapLayout, "Connected to Internet", Snackbar.LENGTH_INDEFINITE);
        mSnackbarOnline.setAction("GO ONLINE", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goOnline();
            }
        });
        mSnackbarOnline.setActionTextColor(Color.WHITE);

    }

    private void loadWebView(){
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        final OpenRapBridge openRapBridge = new OpenRapBridge(this);
        mWebView.addJavascriptInterface(openRapBridge, "appRef");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_openrap, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_openrap_downloads: goOffline(); break;
            case R.id.action_openrap_instructions: openBottomSheet(); break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectivityReceiver = new ConnectivityReceiver(this);
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(connectivityReceiver, intentFilter);

        String userId = SharedPrefrence.getItem(OpenRapActivity.this, "userId", "");
        if (!userId.equals("")) {
            mWebView.loadUrl(openRapUrl + openRapIndex);
        } else {
            mWebView.loadUrl("file:///android_asset/openrap_not_authenticated.html");
        }

        mWebView.setWebViewClient(new WebViewClient() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if(errorResponse.getStatusCode() == 404 && request.getUrl().toString().equalsIgnoreCase(openRapUrl + openRapIndex) ) {
                    Log.d("OpenRapActivity", "error page");
                    mWebView.loadUrl("about://blank");
                    AlertDialog.Builder builder = new AlertDialog.Builder(OpenRapActivity.this);
                    builder.setMessage("Lex hotspot is not available right now. Please try again later.");
                    builder.setPositiveButton("GO TO DOWNLOADS", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            goOffline();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if(!(request.getUrl().toString().contains("infosys") || request.getUrl().toString().contains("captive.openrap") || request.getUrl().toString().contains("file:///"))){
                    mWebView.removeJavascriptInterface("appRef");
                }
                return super.shouldOverrideUrlLoading(view, request);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connectivityReceiver);
    }

    @Override
    public void connectedToInternet() {
        isConnectedToInternet = true;
        mSnackbarOnline.show();
    }

    private void goOnline() {
        Intent intent = new Intent(OpenRapActivity.this, HomeActivity.class);
        startActivity(intent);
    }


    @Override
    public void offline() {
        isConnectedToInternet = false;
        mSnackbarOffline.show();
    }




    private void goOffline() {
        String lastLoggedInString = SharedPrefrence.getItem(this, Constants.last_loggedin_date, "undefined");
        Log.d("HomeActivityError","val "+lastLoggedInString);
        long lastLoggedInLong = 0L;
        if (lastLoggedInString.equalsIgnoreCase("undefined")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("LEX");
            builder.setMessage("You have not logged in yet. Please login and check your network connectivity");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
        } else {
            lastLoggedInLong = Long.parseLong(lastLoggedInString);
            lastLoggedInLong = lastLoggedInLong + Constants.accessExpiry;
            if (lastLoggedInLong > System.currentTimeMillis()) {
                Intent downloadPage = new Intent(this, DownloadsWebView.class);
                downloadPage.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(downloadPage);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Access Downloads");
                builder.setMessage("You haven't logged-in in the past one week. Please login once again for accessing the offline content.");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
            }
        }

    }

    private void openBottomSheet() {
        if (mSheetBehavior.getState() == AutoCloseBottomSheetBehavior.STATE_COLLAPSED) {
            mSheetBehavior.setState(AutoCloseBottomSheetBehavior.STATE_EXPANDED);
        }
    }
}
