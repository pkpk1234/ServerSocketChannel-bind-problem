package com.ljm;

/**
 * NIO Server
 *
 * @author 李佳明 https://github.com/pkpk1234
 * @date 2018-03-09
 */

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NIOServerWithBindCheck {
    public static void main(String[] args) throws IOException {
        int port = 8989;
        if (isPortBinded(port)) {
            throw new IOException("server port " + port + "is already binded");
        }
        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", port));
            serverSocketChannel.configureBlocking(false);
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            ByteBuffer buffer = ByteBuffer.allocate(64);

            System.out.println("server started");

            while (true) {
                selector.select();
                Iterator<SelectionKey> selectKeyIt = selector.selectedKeys().iterator();

                while (selectKeyIt.hasNext()) {
                    try {
                        SelectionKey selectKey = selectKeyIt.next();
                        selectKeyIt.remove();
                        if (selectKey.isAcceptable()) {
                            ServerSocketChannel serverChannel =
                                    (ServerSocketChannel) selectKey.channel();
                            SocketChannel clientSocketChannel = serverChannel.accept();
                            System.out.println(
                                    String.format(
                                            "client connected from: %s",
                                            clientSocketChannel.getRemoteAddress()));
                            clientSocketChannel.configureBlocking(false);
                            clientSocketChannel.register(
                                    selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, buffer);
                        } else if (selectKey.isReadable()) {
                            SocketChannel clientSocketChannel = (SocketChannel) selectKey.channel();
                            buffer = (ByteBuffer) selectKey.attachment();
                            System.out.println(
                                    String.format(
                                            "is connected:%s,is opened:%s \n",
                                            clientSocketChannel.isConnected(),
                                            clientSocketChannel.isOpen()));
                            try {
                                clientSocketChannel.read(buffer);
                                printBuffer(buffer);
                                buffer.clear();

                            } catch (Exception e) {
                                e.printStackTrace();
                                selectKey.cancel();
                            }
                        } else if (selectKey.isWritable()) {
                            SocketChannel clientSocketChannel = (SocketChannel) selectKey.channel();
                            buffer = (ByteBuffer) selectKey.attachment();
                            buffer.flip();
                            try {
                                clientSocketChannel.write(buffer);
                                buffer.compact();
                            } catch (Exception e) {
                                e.printStackTrace();
                                selectKey.cancel();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocketChannel != null) {
                try {
                    serverSocketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 检查端口是否已经被占用
     *
     * @param port
     * @return
     */
    private static boolean isPortBinded(int port) {
        boolean isBinded = false;
        Socket sc = new Socket();
        try {
            //连接本地端口，设置半秒超时
            sc.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 500);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //判断是否成功连接
            if (sc.isConnected()) {
                isBinded = true;
            }
            try {
                sc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isBinded;
    }

    private static void printBuffer(ByteBuffer buffer) {
        System.out.println(String.format("Buffer postion: %d", buffer.position()));
        System.out.println(String.format("Buffer limit: %d", buffer.limit()));
    }
}
