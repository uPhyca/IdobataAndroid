
package com.uphyca.idobata.android.data;

import com.uphyca.idobata.android.data.api.MessageFilter;
import com.uphyca.idobata.model.Message;
import com.uphyca.idobata.model.User;

import java.util.List;

public class MentionFilter implements MessageFilter {

    @Override
    public boolean isSubscribed(User user, Message message) {
        List<Long> mentions = message.getMentions();
        if (mentions == null) {
            return false;
        }
        return mentions.contains(user.getId());
    }
}
