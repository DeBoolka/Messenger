package Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class NioClient implements Runnable{

    static Logger log = LoggerFactory.getLogger(BlackDragon.NioServer.class);

    private SocketChannel channel;
    private Selector selector;
    private int port;
    private String host;

    private BlockingQueue<String> pushMessages = new ArrayBlockingQueue<>(2);
    private ByteBuffer byteBuffer = ByteBuffer.allocate(8192);

    public void init() throws IOException{
        selector = Selector.open();

        channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_CONNECT);

        channel.connect(new InetSocketAddress(host, port));
    }

    public NioClient(int port, String host) throws IOException {
        this.port = port;
        this.host = host;

        init();

        new Thread(this).start();
        ConsoleHandler();
    }

    public static void main(String... args){
        try {
            new NioClient(19000, "192.168.1.37");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();

                Iterator selectorsKey = selector.selectedKeys().iterator();
                while (selectorsKey.hasNext()){
                    SelectionKey key = (SelectionKey)selectorsKey.next();
                    selectorsKey.remove();

                    if(!key.isValid()){
                        continue;
                    }

                    if(key.isConnectable()){
                        SocketChannel channel = (SocketChannel)key.channel();

                        channel.finishConnect();
                        key.interestOps(SelectionKey.OP_READ);

                        log.info("[Connect]");

                    } else if(key.isReadable()){
                        log.info("[read]");
                        read(key);
                    } else if(key.isWritable()){
                        write(key);

                        key.interestOps(SelectionKey.OP_READ);
                        log.info("[write]");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    private byte[] read(SelectionKey key) {
        byteBuffer.clear();
        byte[] data = null;

        try {
            int num = channel.read(byteBuffer);

            if (num < 0) {
                closeChannel(key);
            }

            byteBuffer.flip();

            data = new byte[num];
            System.arraycopy(byteBufferToByteArray(byteBuffer), 0, data, 0, num);

            log.info("From server: {}", new String(data));

        } catch (IOException e) {
            closeChannel(key);
        }

        return data;
    }

    private void write(SelectionKey key) {
        while (!pushMessages.isEmpty()) {
            String message = pushMessages.poll();

            if (message != null) {
                try {
                    channel.write(ByteBuffer.wrap(message.getBytes()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void ConsoleHandler(){
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String line = scanner.nextLine();

                if (line.equals("q") || line.toLowerCase().equals("\\exit")) {
                    log.info("Exit!");
                    System.exit(0);
                }

                try {
                    pushMessages.put(line);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                SelectionKey key = channel.keyFor(selector);
                key.interestOps(SelectionKey.OP_WRITE);
                selector.wakeup();
            }
        }).start();
    }

    public static void closeChannel(SelectionKey key){
        SocketChannel channel = (SocketChannel)key.channel();

        try {
            key.cancel();
            channel.close();

            log.info("[Server Crashed]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] byteBufferToByteArray(ByteBuffer byteBuffer) {
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data, 0, data.length);

        return data;
    }
}
