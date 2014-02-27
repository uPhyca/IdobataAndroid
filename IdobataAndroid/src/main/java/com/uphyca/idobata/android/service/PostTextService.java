
package com.uphyca.idobata.android.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.uphyca.idobata.Idobata;
import com.uphyca.idobata.IdobataError;
import com.uphyca.idobata.android.InjectionUtils;

import javax.inject.Inject;

public class PostTextService extends IntentService {

    private static final String EXTRA_ROOM_URI = "room_uri";
    private static final String EXTRA_SOURCE = "source";

    public static void postText(Context context, Uri roomUri, String source) {
        Intent intent = new Intent(context, PostTextService.class).putExtra(EXTRA_SOURCE, source)
                                                                  .putExtra(EXTRA_ROOM_URI, roomUri);
        context.startService(intent);
    }

    @Inject
    Idobata mIdobata;

    public PostTextService() {
        super("PostTextService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        InjectionUtils.getObjectGraph(this)
                      .inject(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Uri roomUri = intent.getParcelableExtra(EXTRA_ROOM_URI);
        String source = intent.getStringExtra(EXTRA_SOURCE);
        try {
            String[] tuple = roomUri.getFragment()
                                    .split("/");
            String organizationSlug = tuple[2];
            String roomName = tuple[4];
            long roomId = mIdobata.getRooms(organizationSlug, roomName)
                                  .get(0)
                                  .getId();
            mIdobata.postMessage(roomId, source);
        } catch (IdobataError idobataError) {
            idobataError.printStackTrace();
        }
    }
}
