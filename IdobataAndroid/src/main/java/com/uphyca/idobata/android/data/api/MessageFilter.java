
package com.uphyca.idobata.android.data.api;

import com.uphyca.idobata.model.Message;
import com.uphyca.idobata.model.User;

public interface MessageFilter {

    boolean isSubscribed(User user, Message message);
}
