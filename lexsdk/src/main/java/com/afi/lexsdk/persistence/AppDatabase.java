package com.afi.lexsdk.persistence;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import android.util.Log;



import com.afi.lexsdk.persistence.dao.ContentDao;
import com.afi.lexsdk.persistence.dao.DownloadStatusDao;
import com.afi.lexsdk.persistence.entity.ContentEntity;
import com.afi.lexsdk.persistence.entity.DownloadStatusEntity;
import com.afi.lexsdk.persistence.entity.RequestedStatusEntity;

/**
 * Created by Sujeet_Jaiswal on 4/13/2018.
 */

@Database(entities = {ContentEntity.class, DownloadStatusEntity.class, RequestedStatusEntity.class}, version = 1 , exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase sAppDatabase;

    public abstract ContentDao contentDao();
    public abstract DownloadStatusDao downloadStatusDao();
//    public abstract RequestedStatusDao requestedStatusDao();

    public static AppDatabase getDb(Context context) {
        try{
            if (sAppDatabase == null) {
                sAppDatabase = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "lex-db")
                        .allowMainThreadQueries()
                        .build();
            }
        }
        catch (Exception ex){
            Log.e("RoomDatabase", ex.getMessage());
        }


        return sAppDatabase;
    }
}
