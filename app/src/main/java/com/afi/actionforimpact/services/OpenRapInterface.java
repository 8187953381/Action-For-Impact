package com.afi.actionforimpact.services;

import com.afi.lexsdk.Constants;

import retrofit2.Call;
import retrofit2.http.GET;

public interface OpenRapInterface {

//    @GET("/public/" + Constants.OPENRAP_RANDOM_BITS)
    @GET("/public/" + Constants.OPENRAP_RANDOM_BITS)
    Call<Void> checkIfOpenRap();

    @GET("/")
    Call<Void> checkIfInternet();
}
