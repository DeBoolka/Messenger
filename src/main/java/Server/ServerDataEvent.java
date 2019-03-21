package Server;

import java.nio.channels.SocketChannel;

public class ServerDataEvent {
    public NioServer server;
    public SocketChannel channel;
    public byte[] data;

    public ServerDataEvent(NioServer server, SocketChannel channel, byte[] data) {
        this.server = server;
        this.channel = channel;
        this.data = data;
    }
}
