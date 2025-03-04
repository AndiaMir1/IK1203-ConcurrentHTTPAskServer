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

    public byte[] askServer(String hostname, int port, byte[] toServerBytes) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Socket socket = new Socket(hostname, port);

        try {
            if (timeout != null) {
                socket.setSoTimeout(timeout);
            }

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            out.write(toServerBytes);
            if (shutdown) {
                socket.shutdownOutput();
            }

            byte[] buffer = new byte[1024];
            int bytesRead;
            int totalBytesRead = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                if (limit != null && totalBytesRead >= limit) {
                    break;
                }
            }
        } finally {
            socket.close();
        }

        return byteArrayOutputStream.toByteArray();
    }

    public byte[] askServer(String hostname, int port) throws IOException {
        return askServer(hostname, port, new byte[0]);
    }
}