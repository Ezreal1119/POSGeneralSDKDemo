package com.urovo.uhome;

interface IUmsCallback {
    void onSuccess(String code, String data, String msg);
    void onFailure(String code, String data, String msg);
}