package javax.microedition.io;

import com.sun.squawk.microedition.io.FileConnection;

import java.io.*;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Alex on 3/24/14.
 */
public class Connector {

    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_WRITE = 3;

    public static javax.microedition.io.Connection open(java.lang.String name) throws java.io.IOException {
        if (name.startsWith("serversocket"))
            return new ServerSocketConnectionConn(new ServerSocket(Integer.parseInt(getActualName(name))));
        else
            return new FileFileConnection(new File(getActualName(name)));
    }

    private static String getActualName(String name) {
        if (name.startsWith("serversocket"))
            return name.substring(name.lastIndexOf(':') + 1);

        else {
            String substring = name.substring(name.indexOf(':') + 1);
            while (substring.startsWith("/"))
                substring = substring.substring(1);
            return substring;
        }
    }

    public static javax.microedition.io.Connection open(java.lang.String name, int mode) throws java.io.IOException {
        return open(name);
    }

    public static javax.microedition.io.Connection open(java.lang.String name, int mode, boolean timeouts) throws java.io.IOException {
        return open(name);
    }

    public static java.io.InputStream openInputStream(java.lang.String name) throws java.io.IOException {
        return new FileInputStream(getActualName(name));
    }

    public static java.io.OutputStream openOutputStream(java.lang.String name) throws java.io.IOException {
        return new FileOutputStream(getActualName(name));
    }

    public static java.io.DataInputStream openDataInputStream(java.lang.String name) throws java.io.IOException {
        return new DataInputStream(openInputStream(getActualName(name)));
    }

    public static java.io.DataOutputStream openDataOutputStream(java.lang.String name) throws java.io.IOException {
        return new DataOutputStream(openOutputStream(getActualName(name)));
    }

    public static class FileFileConnection implements FileConnection {
        private File file;
        private boolean opened;

        public FileFileConnection(File file) {
            this.file = file;
            this.opened = false;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public boolean isOpen() {
            return opened;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            opened = true;
            return Connector.openInputStream(file.getAbsolutePath());
        }

        @Override
        public DataInputStream openDataInputStream() throws IOException {
            opened = true;
            return Connector.openDataInputStream(file.getAbsolutePath());
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            opened = true;
            return Connector.openOutputStream(file.getAbsolutePath());
        }

        @Override
        public DataOutputStream openDataOutputStream() throws IOException {
            opened = true;
            return Connector.openDataOutputStream(file.getAbsolutePath());
        }

        @Override
        public OutputStream openOutputStream(long l) throws IOException {
            return Connector.openOutputStream(file.getAbsolutePath());
        }

        @Override
        public long fileSize() throws IOException {
            return file.length();
        }

        @Override
        public void create() throws IOException {
            file.createNewFile();
        }

        @Override
        public boolean exists() {
            return file.exists();
        }

        @Override
        public boolean isDirectory() {
            return file.isDirectory();
        }

        @Override
        public void delete() throws IOException {
            file.delete();
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public String getPath() {
            return file.getAbsolutePath();
        }

        @Override
        public String getURL() {
            try {
                return file.toURI().toURL().toExternalForm();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return "";
        }


    }

    public static class ServerSocketConnectionConn implements ServerSocketConnection {
        private ServerSocket serverSocket;

        public ServerSocketConnectionConn(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        @Override
        public String getLocalAddress() throws IOException {
            return serverSocket.getLocalSocketAddress().toString();
        }

        @Override
        public int getLocalPort() throws IOException {
            return serverSocket.getLocalPort();
        }

        @Override
        public StreamConnection acceptAndOpen() throws IOException {
            final Socket socket = serverSocket.accept();
            return new StreamConnection() {
                @Override
                public InputStream openInputStream() throws IOException {
                    return socket.getInputStream();
                }

                @Override
                public DataInputStream openDataInputStream() throws IOException {
                    return new DataInputStream(openInputStream());
                }

                @Override
                public OutputStream openOutputStream() throws IOException {
                    return socket.getOutputStream();

                }

                @Override
                public DataOutputStream openDataOutputStream() throws IOException {
                    return new DataOutputStream(openOutputStream());
                }

                @Override
                public void close() throws IOException {
                    socket.close();
                }
            };
        }

        @Override
        public void close() throws IOException {

        }
    }

//    @Override
//    public void setSocketOption(byte b, int i) throws IOException {
//
//    }
//
//    @Override
//    public int getSocketOption(byte b) throws IOException {
//        return 0;
//    }
//
//    @Override
//    public String getLocalAddress() throws IOException {
//        System.out.println("getting local address");
//        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//        while (interfaces.hasMoreElements()) {
//            NetworkInterface current = interfaces.nextElement();
//            System.out.println(current);
//            if (!current.isUp() || current.isLoopback() || current.isVirtual()) continue;
//            Enumeration<InetAddress> addresses = current.getInetAddresses();
//            while (addresses.hasMoreElements()) {
//                InetAddress current_addr = addresses.nextElement();
//                if (current_addr.isLoopbackAddress()) continue;
//                if (current_addr instanceof Inet4Address)
//                    return current_addr.getHostAddress();
//                else if (current_addr instanceof Inet6Address)
//                    return current_addr.getHostAddress();
//            }
//        }
//        return "127.0.0.1";
//    }
}
