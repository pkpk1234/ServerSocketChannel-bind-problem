# NIO ServerSocketChannel监听端口问题

今天晓风轻遇到了一个问题，nodejs和tomcat8同时监听同一个网卡的同一个端口，监听不会抛出异常，并且访问该端口，其中某个程序可以正常响应。关闭正则响应的程序，另外一个程序会接替被关闭的程序进行响应。

其实这是NIO监听端口的机制造成的。ServerSocketChannel监听端口时，如果该端口已经被监听，ServerSocketChannel不会抛出异常，而是继续执行。这样就导致了程序看上去正常启动了，但是实际上并没有成果监听。

## 重现问题

首先使用ServerSocketChannel编写一个Server，监听8989端口，如下：

```java
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
                ... ... 省略 ... ...
```

NIOServer在进行selector循环之前，输出server started日志。

执行nc -l 8989，提前监听端口8989。

![](/assets/nc)

然后运行NIOServer，可以看到程序成果输出server started日志。

![](/assets/serverstarted)

此时执行telnet 127.0.0.1 8989连接本地8989端口，可以发现连接上的是nc，如下 ：![](/assets/telnet-nc)关闭nc，再次telnet 127.0.0.1 8989，此时会连接NIOServer，如下：

![](/assets/telnet-nioserver)

综上，即重现了上面说的问题。

## 解决方法

使用Socket连接本地8989端口，如果发现连接成功，则说明端口已经被占用，抛出异常即可。

```java
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
```

修改代码，添加检查

```java
public class NIOServerWithBindCheck {
    public static void main(String[] args) throws IOException {
        int port = 8989;
        if (isPortBinded(port)) {
            throw new IOException("server port " + port + "is already binded");
        }
        ServerSocketChannel serverSocketChannel = null;
        ... ...省略 ... ...
```

效果如下：

![](/assets/nioserver-with-bind-check)

完整代码：https://github.com/pkpk1234/ServerSocketChannel-bind-problem

