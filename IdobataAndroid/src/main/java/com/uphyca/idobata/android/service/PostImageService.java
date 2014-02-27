
package com.uphyca.idobata.android.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.uphyca.idobata.Idobata;
import com.uphyca.idobata.IdobataError;
import com.uphyca.idobata.android.InjectionUtils;

import java.io.FileNotFoundException;

import javax.inject.Inject;

public class PostImageService extends IntentService {

    private static final String EXTRA_ROOM_URI = "room_uri";

    public static void postImage(Context context, Uri roomUri, Uri dataUri) {
        Intent intent = new Intent(context, PostImageService.class).setData(dataUri)
                                                                   .putExtra(EXTRA_ROOM_URI, roomUri);
        context.startService(intent);
    }

    @Inject
    Idobata mIdobata;

    public PostImageService() {
        super("PostImageService");
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
        Uri dataUri = intent.getData();
        String mimeType = null;
        try {
            String[] tuple = roomUri.getFragment()
                                    .split("/");
            String organizationSlug = tuple[2];
            String roomName = tuple[4];
            long roomId = mIdobata.getRooms(organizationSlug, roomName)
                                  .get(0)
                                  .getId();
            Cursor meta = getContentResolver().query(dataUri, new String[] {
                "mime_type"
            }, null, null, null);
            try {
                if (meta.moveToNext()) {
                    mimeType = meta.getString(0);
                }
            } finally {
                meta.close();
            }
            String fileName = "image." + mimeType.split("/")[1];
            mIdobata.postMessage(roomId, fileName, mimeType, getContentResolver().openInputStream(dataUri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IdobataError idobataError) {
            idobataError.printStackTrace();
        }
    }
}
