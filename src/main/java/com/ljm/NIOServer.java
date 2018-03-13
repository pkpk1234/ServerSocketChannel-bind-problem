package com.ljm;

/**
 * NIO Server
 *
 * @author 李佳明 https://github.com/pkpk1234
 * @date 2018-03-09
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NIOServer {
    public static void main(String[] args) {
        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 8989));
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

    private static void printBuffer(ByteBuffer buffer) {
        System.out.println(String.format("Buffer postion: %d", buffer.position()));
        System.out.println(String.format("Buffer limit: %d", buffer.limit()));
    }
}
