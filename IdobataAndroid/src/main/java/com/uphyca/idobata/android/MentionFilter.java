
package com.uphyca.idobata.android;

import com.uphyca.idobata.event.MessageCreatedEvent;
import com.uphyca.idobata.model.User;

public class MentionFilter implements MessageFilter {

    @Override
    public boolean isSubscribed(User user, MessageCreatedEvent event) {
        return event.getMentions()
                    .contains(user.getId());
    }
}
