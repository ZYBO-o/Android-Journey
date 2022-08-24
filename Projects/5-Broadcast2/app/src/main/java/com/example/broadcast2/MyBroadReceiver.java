package com.example.broadcast2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class MyBroadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MyBroadReceiver","Receive MyBroadReceiver!");
        Toast.makeText(context,"Receive MyBroadReceiver!", Toast.LENGTH_SHORT).show();
    }
}