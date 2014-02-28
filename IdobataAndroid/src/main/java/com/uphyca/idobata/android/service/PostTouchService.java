
package com.uphyca.idobata.android.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.uphyca.idobata.Idobata;
import com.uphyca.idobata.IdobataError;
import com.uphyca.idobata.android.InjectionUtils;

import javax.inject.Inject;

public class PostTouchService extends IntentService {

    public static void postTouch(Context context, Uri roomUri) {
        Intent intent = new Intent(context, PostTouchService.class).setData(roomUri);
        context.startService(intent);
    }

    @Inject
    Idobata mIdobata;

    public PostTouchService() {
        super("PostTouchService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        InjectionUtils.getObjectGraph(this)
                      .inject(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Uri roomUri = intent.getData();
        try {
            String[] tuple = roomUri.getFragment()
                                    .split("/");
            String organizationSlug = tuple[2];
            String roomName = tuple[4];
            long roomId = mIdobata.getRooms(organizationSlug, roomName)
                                  .get(0)
                                  .getId();
            mIdobata.postTouch(roomId);
        } catch (IdobataError idobataError) {
            idobataError.printStackTrace();
        }
    }
}
