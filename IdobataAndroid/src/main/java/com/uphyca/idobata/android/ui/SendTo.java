
package com.uphyca.idobata.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.uphyca.idobata.Idobata;
import com.uphyca.idobata.IdobataError;
import com.uphyca.idobata.android.InjectionUtils;
import com.uphyca.idobata.android.R;
import com.uphyca.idobata.android.data.api.Main;
import com.uphyca.idobata.android.data.api.Networking;
import com.uphyca.idobata.android.service.PostImageService;
import com.uphyca.idobata.android.service.PostTextService;
import com.uphyca.idobata.model.Organization;
import com.uphyca.idobata.model.Records;
import com.uphyca.idobata.model.Room;
import com.uphyca.idobata.model.Seed;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import static com.uphyca.idobata.android.data.IdobataUtils.findOrganizationById;

public class SendTo extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_to);
    }

    public static class Rooms extends ListFragment {

        @Inject
        Idobata mIdobata;

        @Inject
        @Networking
        Executor mExecutor;

        @Inject
        @Main
        Executor mDispatcher;

        private Seed mSeed;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            InjectionUtils.getObjectGraph(getActivity())
                          .inject(this);
            setListShown(false);
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        mSeed = mIdobata.getSeed();
                    } catch (IdobataError idobataError) {
                        idobataError.printStackTrace();
                        return;
                    }
                    mDispatcher.execute(new Runnable() {
                        @Override
                        public void run() {
                            showRooms();
                        }
                    });
                }
            });
        }

        private void showRooms() {
            final Records records = mSeed.getRecords();
            List<String> rooms = new ArrayList<String>();
            for (Room room : records.getRooms()) {
                Organization org = findOrganizationById(room.getOrganizationId(), records.getOrganizations());
                rooms.add(buildItem(room, org).toString());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, rooms) {
                @Override
                public long getItemId(int position) {
                    return records.getRooms()
                                  .get(position)
                                  .getId();
                }
            };

            setListAdapter(adapter);
            setListShown(true);
        }

        @Override
        public void onListItemClick(ListView l, View v, final int position, final long id) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Room room = mSeed.getRecords()
                                         .getRooms()
                                         .get(position);
                        Organization organization = findOrganizationById(room.getOrganizationId(), mSeed.getRecords()
                                                                                                        .getOrganizations());
                        Uri roomUri = Uri.parse(String.format("https://idobata.io/#/organization/%s/room/%s", organization.getSlug(), room.getName()));
                        Intent activityIntent = getActivity().getIntent();
                        String type = activityIntent.getType();

                        if (type.startsWith("text/")) {
                            handleText(roomUri, activityIntent.getStringExtra(Intent.EXTRA_TEXT));
                        } else if (type.startsWith("image/")) {
                            handleImage(roomUri, activityIntent.<Uri> getParcelableExtra(Intent.EXTRA_STREAM));
                        }

                        final Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.setAction(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);

                        intent.setData(roomUri);
                        startActivity(intent);
                        getActivity().finish();
                    } catch (IdobataError idobataError) {
                        idobataError.printStackTrace();
                    }
                }

            });
        }

        private void handleImage(Uri roomUri, Uri dataUri) {
            PostImageService.postImage(getActivity(), roomUri, dataUri);
        }

        private void handleText(Uri roomUri, String source) throws IdobataError {
            PostTextService.postText(getActivity(), roomUri, source);
        }

        private CharSequence buildItem(Room room, Organization org) {
            return new StringBuilder().append(org.getSlug())
                                      .append(' ')
                                      .append('/')
                                      .append(' ')
                                      .append(room.getName());
        }
    }
}
