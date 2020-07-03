package com.afi.lexsdk.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.afi.lexsdk.Constants;
import com.afi.lexsdk.UtilityJava;
import com.afi.lexsdk.persistence.AppDatabase;
import com.afi.lexsdk.persistence.entity.ContentEntity;
import com.afi.lexsdk.persistence.entity.DownloadStatusEntity;

import java.io.File;

import static com.afi.lexsdk.Constants.DATA_DIR_PATH;
import static com.afi.lexsdk.Constants.EXTRA_DOWNLOAD_RES_ID;
//import static com.infosysit.lexsdk.Constants.OPENRAP_DIR_PATH;
import static com.afi.lexsdk.Constants.STORAGE_BASE_PATH;
import static com.afi.lexsdk.Constants.TMP_DIR_PATH;
import static com.afi.lexsdk.Constants.encryptDecryptContentId;
import static com.afi.lexsdk.Constants.openRapUrl;
import static com.afi.lexsdk.Constants.typeOfEncrptDecrypt;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class DownloadOpenRapService extends IntentService {
    Context mContext;
    String mContentId;
    StringBuilder children = new StringBuilder();
    String parentId = null;


    public DownloadOpenRapService() {
        super("DownloadOpenRapService");
        mContext = this;
    }



    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            mContentId = intent.getStringExtra(EXTRA_DOWNLOAD_RES_ID).replaceAll("\"", "");
            try {
                EncryptionDecryption.decryptDataOpenRap(
                        Constants.OPENRAP_DECRYPTION_KEY,
                        TMP_DIR_PATH,
                        mContentId + ".lex",
                        TMP_DIR_PATH,
                        mContentId + ".zip",this);
                UtilityJava.unzipFile(mContentId + ".zip",this);
                File dir = new File(this.getExternalFilesDir("") + "/" + TMP_DIR_PATH + mContentId);
                traverse(dir);
                JsonObject response = UtilityJava.getJsonFromFile(dir.getAbsolutePath()  + "/" + mContentId + "/" + mContentId + ".json");
                JsonObject contentMeta = response.get("result").getAsJsonObject().get("content").getAsJsonObject();
                if (contentMeta != null) {
                    Log.d("openRapTesting", "ContentMeta not null: "+contentMeta);
                    makeDatabaseEntries(contentMeta);
                }
            } catch (Exception e) {
                Log.e("DownloadOpenRapService", e.getMessage());
            }

        }
    }

    public void traverse (File dir) {
        Log.d("traverse", "traversing folder " + dir.getAbsolutePath());
        if (dir.exists()) {
            for (File file: dir.listFiles()) {
                Log.d("traverse", "traversing files " + file.getAbsolutePath());
                boolean isWebModule = false;
                boolean isQuiz = false;
                for (File courseFile: file.listFiles()) {
                    Log.d("traverse", "traversing subfiles " + courseFile.getAbsolutePath());
                    if (courseFile.isFile()) {
                        String fileName = courseFile.getName();
                        if (fileName.equals("manifest.json")) {
                            isWebModule = true;

                        }
                        if(fileName.equalsIgnoreCase("quiz.json")){
                            isQuiz = true;
                        }
                        if (fileName.equals("quiz.json")) {
                            isQuiz = true;
                        }
                        String extension = fileName.substring(fileName.lastIndexOf("."));
                        if (!fileName.endsWith("json")) {
                            String outputFile = file.getName();
                            if ((extension.equalsIgnoreCase(".png") || extension.equalsIgnoreCase(".jpg") || extension.equalsIgnoreCase(".jpeg"))) {
                                outputFile += ".image.png";
                            } else {
                                outputFile += extension;
                            }
                            UtilityJava.moveFile(file.getAbsolutePath() + "/", fileName, this.getExternalFilesDir("") + "/" + DATA_DIR_PATH, outputFile);
                        }
                    }
                }

                if (isWebModule || isQuiz) {
                    UtilityJava.zipFileAtPath(file.getAbsolutePath(), this.getExternalFilesDir("") + "/" + DATA_DIR_PATH + file.getName() + ".zip");
                }
            }
        }
    }

    // TODO: reuse code from download content service
    public void makeDatabaseEntries(JsonObject contentJSON) {
        String identifier = contentJSON.get("identifier").getAsString();
        boolean initiatedByUser = mContentId.equals(identifier);
        String downloadUrl = openRapUrl + "/public/" + mContentId + ".zip";

        String mimeType = contentJSON.get("mimeType").getAsString();

        JsonArray modules = contentJSON.getAsJsonArray("children");
        if (modules != null && modules.size() > 0) {
            long nowInMillis = System.currentTimeMillis();

            StringBuilder childs = getAllChildren(contentJSON);
            contentJSON.addProperty("appIcon", STORAGE_BASE_PATH + DATA_DIR_PATH +identifier+".image.png");
            ContentEntity content = new ContentEntity(
                    identifier,
                        -1,
                    contentJSON.get("contentType").getAsString(),
                    String.valueOf(nowInMillis/1000),
                    String.valueOf(nowInMillis/1000),
                    String.valueOf(nowInMillis + Constants.expiry),
                    contentJSON.toString(),
                    childs.deleteCharAt(childs.length()-1).toString(),
                    "",
                    ""
                );
//                Log.d("downloadContent",identifier+": "+childs.toString());
                DownloadStatusEntity downloadStatus = new DownloadStatusEntity(
                        identifier,
                        initiatedByUser,
                        downloadUrl,
                        "DOWNLOADED",
                        100,
                        -1,
                        1
//                        ,Constants.UserEmail
                );


//                mSQLiteHelperJava.addContent(content);
            try {
                AppDatabase.getDb(mContext).downloadStatusDao().insertAll(downloadStatus);
                StringBuilder childrenString = new StringBuilder();
                AppDatabase.getDb(mContext).contentDao().insertAll(content);
            } catch (Exception e) {
                Log.d("Exception","Message: "+e.getMessage());
            }
            for (JsonElement module:modules) {
                    makeDatabaseEntries((JsonObject) module);
                }
        }
        else{
                Log.d("DownloadContent",identifier +"To download");
                String extension = null;
                switch (mimeType) {
                    case "application/pdf":  extension = "pdf";
                        break;
                    case "video/mp4":  extension = "mp4";
                        break;
                    case "audio/mpeg":  extension = "mp3";
                        break;
                    case "image/png":  extension = "png";
                        break;
                    case "application/quiz":  extension = "zip";
                        break;
                    case "application/web-module":  extension = "zip";
                        break;
                    default : extension = "lex";
                        break;
                }

            contentJSON.addProperty("appIcon",STORAGE_BASE_PATH + DATA_DIR_PATH+identifier+".image.png");
            StringBuilder childs = getAllChildren(contentJSON);
            ContentEntity content = new ContentEntity(
                        identifier,
                        -1,
                        contentJSON.get("contentType").getAsString(),
                        String.valueOf(System.currentTimeMillis()/1000),
                        String.valueOf(System.currentTimeMillis()/1000),
                        String.valueOf(System.currentTimeMillis()+ Constants.expiry),
                        contentJSON.toString(),
                        childs.deleteCharAt(childs.length()-1).toString(),
                        parentId,
                        extension
                );
            DownloadStatusEntity downloadStatus = new DownloadStatusEntity(
                        identifier,
                        initiatedByUser,
                        downloadUrl,
                        "DOWNLOADED",
                        100,
                        -1,
                        1
//                    ,Constants.UserEmail
                );

//                    Log.d("downloadContent", "its a resource" + identifier);

//                    Log.d("DownloadContentService", "Inserting " + downloadStatus.getContentId());
            try {
                AppDatabase.getDb(mContext).contentDao().insertAll(content);
                AppDatabase.getDb(mContext).downloadStatusDao().insertAll(downloadStatus);
            } catch (Exception e) {
                Log.d("Exception","Message: "+e.getMessage());
            }

                // encrypt data
                Intent encryptDecyptService = new Intent(this, EncrpytDecryptService.class);
                encryptDecyptService.putExtra(typeOfEncrptDecrypt,"EncryptData");
                encryptDecyptService.putExtra(encryptDecryptContentId, identifier);
                this.startService(encryptDecyptService);

        }

    }

    public StringBuilder getAllChildren(JsonObject contentJSON){

        JsonArray modules = contentJSON.getAsJsonArray("children");
        if(modules != null && modules.size() > 0){
            for (JsonElement children : modules){
                getAllChildren((JsonObject) children);
            }
        }
        else{
            children.append(contentJSON.get("identifier").getAsString()+",");
        }
        return children;
    }


}
