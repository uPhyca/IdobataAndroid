
package com.uphyca.idobata.android;

import com.uphyca.idobata.event.MessageCreatedEvent;
import com.uphyca.idobata.model.User;

import java.util.List;

public class MentionFilter implements MessageFilter {

    @Override
    public boolean isSubscribed(User user, MessageCreatedEvent event) {
        List<Long> mentions = event.getMentions();
        if (mentions == null) {
            return false;
        }
        return mentions.contains(user.getId());
    }
}
