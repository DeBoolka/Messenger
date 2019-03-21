package Server;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ChangeRequest {
    public static final int CHANGEOPS = 1;

    public SocketChannel channel;
    public int type;
    public int ops;

    public ChangeRequest(SocketChannel channel, int type, int ops) {
        this.channel = channel;
        this.type = type;
        this.ops = ops;
    }
}
