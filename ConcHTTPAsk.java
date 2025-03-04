import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConcHTTPAsk {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java ConcHTTPAsk <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port " + port);
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            handleClient(clientSocket);
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();
        
        // Läs hela HTTP-förfrågan (inte bara första raden!)
        String requestLine = in.readLine();
        if (requestLine == null) {
            sendResponse(out, "400 Bad Request", "Invalid request");
            return;
        }
    
        // Läs resten av headers tills vi når en tom rad (HTTP-specifik)
        while (true) {
            String header = in.readLine();
            if (header == null || header.isEmpty()) {
                break; // Sluta läsa headers vid en tom rad
            }
        }
    
        // Kontrollera om requesten är en korrekt GET-förfrågan
        if (!requestLine.startsWith("GET")) {
            sendResponse(out, "400 Bad Request", "Only GET requests are supported");
            return;
        }
    
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            sendResponse(out, "400 Bad Request", "Malformed request");
            return;
        }
    
        String queryString = parts[1];
        if (!queryString.startsWith("/ask?")) {
            sendResponse(out, "404 Not Found", "Invalid path");
            return;
        }
    
        // Parse query parameters
        Map<String, String> params = parseQuery(queryString.substring(5));
        if (!params.containsKey("hostname") || !params.containsKey("port")) {
            sendResponse(out, "400 Bad Request", "Missing required parameters");
            return;
        }
    
        // Kör TCP-klienten
        try {
            String hostname = params.get("hostname");
            int targetPort = Integer.parseInt(params.get("port"));
            String message = params.getOrDefault("string", "") + "\n";
            
            TCPClient client = new TCPClient(false, 0, 0);
            byte[] response = client.askServer(hostname, targetPort, message.getBytes(StandardCharsets.UTF_8)); 
            String result = new String(response, StandardCharsets.UTF_8);
            sendResponse(out, "200 OK", result);
        } catch (Exception e) {
            sendResponse(out, "500 Internal Server Error", "Error: " + e.getMessage());
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    private void sendResponse(OutputStream out, String status, String content) throws IOException {
        String response = "HTTP/1.1 " + status + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + content.length() + "\r\n" +
                "\r\n" +
                content;
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}

