
package com.uphyca.idobata.android.data.api;

import com.uphyca.idobata.event.MessageCreatedEvent;
import com.uphyca.idobata.model.User;

public interface MessageFilter {

    boolean isSubscribed(User user, MessageCreatedEvent event);
}
