
package com.uphyca.idobata.android;

import com.uphyca.idobata.event.MessageCreatedEvent;
import com.uphyca.idobata.model.User;

public interface MessageFilter {

    boolean isSubscribed(User user, MessageCreatedEvent event);
}
