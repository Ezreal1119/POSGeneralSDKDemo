package com.urovo.uhome;

import com.urovo.uhome.IUmsCallback;

interface IUmsFunction {
    String getUmsVersion();
    void uploadConfig(String a, String b, String c, IUmsCallback cb);
    void syncConfigFeedback(String a, String b, IUmsCallback cb);
    String getConnectStatus();
    void getBindStatus(IUmsCallback cb);
    boolean getOnlineStatus();
}