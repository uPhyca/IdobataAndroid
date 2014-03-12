
package com.uphyca.idobata.android.data;

import com.uphyca.idobata.model.Organization;
import com.uphyca.idobata.model.Room;

import java.util.List;

public abstract class IdobataUtils {

    private IdobataUtils() {
        throw new UnsupportedOperationException();
    }

    public static Organization findOrganizationById(long id, List<Organization> organizations) {
        for (Organization org : organizations) {
            if (org.getId() == id) {
                return org;
            }
        }
        return null;
    }

    public static Room findRoomById(long id, List<Room> rooms) {
        for (Room room : rooms) {
            if (room.getId() == id) {
                return room;
            }
        }
        return null;
    }
}
