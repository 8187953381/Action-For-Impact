package com.afi.lexsdk.persistence.dao;



import androidx.room.Dao;
import androidx.room.Insert;

import com.afi.lexsdk.persistence.entity.RequestedStatusEntity;

/**
 * Created by Sujeet_Jaiswal on 4/13/2018.
 */

@Dao
public interface RequestedStatusDao {

    @Insert
    void insertAll(RequestedStatusEntity... requestedStatusEntities);
}
