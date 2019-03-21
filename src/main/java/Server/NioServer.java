package Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

public class NioServer implements Runnable {

    static Logger log = LoggerFactory.getLogger(BlackDragon.NioServer.class);

    private int port;
    private String host;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private Worker worker;

    public List changeRequests = new ArrayList();
    public Map pendingData = new HashMap();
    public List<SocketChannel> channels = new ArrayList<>();

    NioServer(String host, int port) throws IOException{
        this.host = host;
        this.port = port;

        selector = initSelector();

        worker = new Worker();
        Thread thWorker = new Thread(worker);
        thWorker.setDaemon(true);
        thWorker.start();

        ConsoleHandler();

    }

    private Selector initSelector() throws IOException{
        Selector socketSelector = SelectorProvider.provider().openSelector();

        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        InetSocketAddress isa = new InetSocketAddress(host, port);
        serverChannel.socket().bind(isa);
        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector;
    }

    public static void main(String[] args){

        Scanner scanner = new Scanner(System.in);

        String host = scanner.nextLine();
        int port = Integer.parseInt(scanner.nextLine());


        try {
            new Thread(new NioServer(/*host*/"localhost", /*port*/19000)).start();
            System.out.println("Starting...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                changeRequestsHandler();

                selector.select();

                Iterator selectKeys = selector.selectedKeys().iterator();
                while (selectKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectKeys.next();
                    selectKeys.remove();

                    if(!key.isValid()){
                        continue;
                    }

                    if(key.isAcceptable()){
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    } else {
                        System.out.println("Error");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void changeRequestsHandler(){
        synchronized (changeRequests){
            Iterator changes = changeRequests.iterator();
            while (changes.hasNext()){
                ChangeRequest req = (ChangeRequest)changes.next();

                switch (req.type){
                    case ChangeRequest.CHANGEOPS:
                        SelectionKey cKey = req.channel.keyFor(selector);
                        cKey.interestOps(req.ops);
                        break;
                }
            }

            this.changeRequests.clear();
        }
    }

    private void accept(SelectionKey key) throws Exception{
        ServerSocketChannel masterChannel = (ServerSocketChannel)key.channel();

        SocketChannel channel = masterChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);

        channels.add(channel);

        log.info("[Accept user]");
    }

    private void read(SelectionKey key){
        SocketChannel channel = (SocketChannel)key.channel();

        int strLen;
        try {
            readBuffer.clear();
            strLen = channel.read(readBuffer);

        } catch (IOException e) {
            closeChannel(key);
            return;
        }

        if(strLen == -1){
            closeChannel(key);
            return;
        }

        log.info("[read] - {}", new String(this.readBuffer.array()));

        worker.processData(this, channel, readBuffer.array(), strLen);
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel)key.channel();

        List writeList;
        synchronized (pendingData) {
            writeList = (List) pendingData.get(channel);


            while (!writeList.isEmpty()) {
                ByteBuffer buff = (ByteBuffer) writeList.get(0);

                channel.write(buff);

                log.info("[write] - " + new String(buff.array()));

                if (buff.remaining() > 0) {
                    // ... or the socket's buffer fills up
                    break;
                }

                writeList.remove(0);
            }

            if(writeList.isEmpty()){
                key.interestOps(SelectionKey.OP_READ);
            }
        }

    }

    public void send(SocketChannel channel, byte[] data) {
        synchronized (changeRequests) {
            changeRequests.add(new ChangeRequest(channel, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

            synchronized (pendingData) {
                List queue = (List)pendingData.get(channel);
                if(queue == null){
                    queue = new ArrayList();
                    pendingData.put(channel, queue);
                }

                queue.add(ByteBuffer.wrap(data));
            }
        }

        selector.wakeup();
    }

    private void ConsoleHandler(){
        new Thread(()->{
            Scanner scanner = new Scanner(System.in);

            while (true) {
                String scanLine = scanner.nextLine();

                if (scanLine.equals("q") || scanLine.toLowerCase().equals("\\exit")) {
                    log.info("Exit!");
                    System.exit(0);
                }

                for (SocketChannel channel : channels) {
                    if (!channel.isConnected()) {
                        channels.remove(channel);
                        continue;
                    }

                    send(channel, scanLine.getBytes());
                }
            }
        }).start();
    }

    private void closeChannel(SelectionKey key){
        SocketChannel channel = (SocketChannel)key.channel();

        try {
            key.cancel();
            channel.close();
            channels.remove(channel);

            log.info("[User left]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
