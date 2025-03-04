import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class ConcHTTPAsk {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Användning: java ConcHTTPAsk <portnummer>");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start();
                } catch (IOException e) {
                    System.err.println("Klientanslutningsfel: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Kunde inte lyssna på port " + portNumber);
            System.exit(-1);
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        try {
            handleClient(clientSocket);
        } catch (IOException e) {
            System.err.println("Fel vid hantering av klient: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));

        String requestLine = in.readLine();
        if (requestLine == null || !requestLine.matches("^GET /[^ ]* HTTP/1\\.1$")) {
            sendResponse(out, "400 Bad Request", "Ogiltig begäran");
            return;
        }

        String[] requestParts = requestLine.split(" ");
        if (requestParts.length < 2) {
            sendResponse(out, "400 Bad Request", "Ogiltig begäran");
            return;
        }

        String query = requestParts[1];
        if (!query.startsWith("/ask?")) {
            sendResponse(out, "404 Not Found", "Resursen hittades inte");
            return;
        }

        Map<String, String> params = parseQuery(query.substring(5));
        String hostname = params.get("hostname");
        String portStr = params.get("port");
        String string = params.get("string");
        String shutdownStr = params.get("shutdown");
        String limitStr = params.get("limit");
        String timeoutStr = params.get("timeout");

        if (hostname == null || portStr == null) {
            sendResponse(out, "400 Bad Request", "Saknade obligatoriska parametrar");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            sendResponse(out, "400 Bad Request", "Ogiltigt portnummer");
            return;
        }

        boolean shutdown = Boolean.parseBoolean(shutdownStr);
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 0;
        int timeout = timeoutStr != null ? Integer.parseInt(timeoutStr) : 0;

        try {
            TCPClient tcpClient = new TCPClient(shutdown, timeout, limit);
            byte[] toServerBytes = (string != null) ? (string + "\n").getBytes("UTF-8") : new byte[0];
            byte[] resultBytes = tcpClient.askServer(hostname, port, toServerBytes);
            String result = new String(resultBytes, "UTF-8");
            sendResponse(out, "200 OK", result);
        } catch (IOException e) {
            sendResponse(out, "500 Internal Server Error", "Fel vid kommunikation med servern");
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                try {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return params;
    }

    private void sendResponse(BufferedWriter out, String status, String content) throws IOException {
        out.write("HTTP/1.1 " + status + "\r\n");
        out.write("Content-Type: text/plain\r\n");
        out.write("Content-Length: " + content.length() + "\r\n");
        out.write("\r\n");
        out.write(content);
        out.flush();
    }
}

