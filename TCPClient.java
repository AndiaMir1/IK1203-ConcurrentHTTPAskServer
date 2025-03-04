
import java.io.*;
import java.net.*;

public class TCPClient {
    private boolean shutdown;
    private Integer timeout;
    private Integer limit;

    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
        this.shutdown = shutdown;
        this.timeout = timeout;
        this.limit = limit;
    }

    public byte[] askServer(String hostname, int port, byte[] bytesToServer) throws IOException {
        Socket socket = null;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            socket = new Socket(hostname, port);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            if (timeout != null) {
                socket.setSoTimeout(timeout);
            }

            out.write(bytesToServer);
            out.flush();

            if (shutdown) {
                socket.shutdownOutput();
            }

            byte[] tempBuff = new byte[2048];
            int bytesRead;
            int totalBytesRead = 0;
            try {
                while (true) {
                    try {
                        bytesRead = in.read(tempBuff);
                        if (bytesRead == -1) break;
                        buffer.write(tempBuff, 0, bytesRead);
                        totalBytesRead += bytesRead;
            
                        if (limit != null && totalBytesRead >= limit) {
                            break;
                        }
                    } catch (SocketTimeoutException e) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
                throw e;
            }
            
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            throw e;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
        return buffer.toByteArray();
    }

    public byte[] askServer(String hostname, int port) throws IOException {
        return askServer(hostname, port, new byte[0]);
    }
}