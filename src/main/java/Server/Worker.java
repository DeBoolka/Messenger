package Server;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

public class Worker implements Runnable {
    private List queue = new LinkedList();

    public void processData(NioServer server, SocketChannel channel, byte[] data, int count){
        byte[] dataCopy = new byte[count];
        System.arraycopy(data, 0, dataCopy, 0, count);

        synchronized (queue) {
            queue.add(new ServerDataEvent(server, channel, dataCopy));
            queue.notify();
        }
    }


    @Override
    public void run() {
        ServerDataEvent dataEvent;

        while (true) {
            synchronized (queue) {
                while (queue.isEmpty()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                    }
                }

                dataEvent = (ServerDataEvent)queue.remove(0);
            }

            work(dataEvent);
        }
    }

    private void work(ServerDataEvent dataEvent){
        dataEvent.server.send(dataEvent.channel, dataEvent.data);
    }


}
