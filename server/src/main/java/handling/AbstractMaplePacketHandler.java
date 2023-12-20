package handling;

import client.MapleClient;
import java.awt.*;
import networking.packet.MaplePacketHandler;
import server.maps.AnimatedMapleMapObject;
import server.movement.Elem;
import server.movement.MovePath;

public abstract class AbstractMaplePacketHandler implements MaplePacketHandler {

    @Override
    public boolean validateState(MapleClient c) {
        return c.isLoggedIn();
    }

    protected void updatePosition(MovePath path, AnimatedMapleMapObject target, int yoffset) {
        for (Elem elem : path.lElem) {
            if (elem.x != 0 && elem.y != 0) {
                target.setPosition(new Point(elem.x, elem.y));
            }
            target.setStance(elem.bMoveAction);
        }
    }
}