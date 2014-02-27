
package com.uphyca.idobata.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.uphyca.idobata.android.service.IdobataService;

public class StartupListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, IdobataService.class));
    }
}
