
package com.uphyca.idobata.android.data;

import com.uphyca.idobata.android.data.api.MessageFilter;
import com.uphyca.idobata.android.data.api.NotificationsMentions;
import com.uphyca.idobata.android.data.prefs.BooleanPreference;
import com.uphyca.idobata.model.Message;
import com.uphyca.idobata.model.User;

import java.util.List;

import javax.inject.Inject;

public class MentionsFilter implements MessageFilter {

    private final BooleanPreference mNotificationsMentionsPreference;

    @Inject
    public MentionsFilter(@NotificationsMentions BooleanPreference notificationMentionsPreference) {
        mNotificationsMentionsPreference = notificationMentionsPreference;
    }

    @Override
    public boolean isSubscribed(User user, Message message) {
        if (!mNotificationsMentionsPreference.get()) {
            return false;
        }
        List<Long> mentions = message.getMentions();
        if (mentions == null) {
            return false;
        }
        return mentions.contains(user.getId());
    }
}
