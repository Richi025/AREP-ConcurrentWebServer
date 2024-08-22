package com.server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class SimpleWebServer {
    private static final int PORT = 8080;
    public static final String WEB_ROOT = "src/main/resources/webroot";
    public static final Map<String, RESTService> services = new HashMap<>();
    private static boolean isRun = true;

    public static void main(String[] args) throws IOException {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        ServerSocket serverSocket = new ServerSocket(PORT);
        addServices();
        while (isRun) {
            Socket clientSocket = serverSocket.accept();
            threadPool.submit(new ClientHandler(clientSocket));
        }
    }

    private static void addServices() {
        services.put("hello", new helloService());
    }

    public static void stop() {
        isRun = false;
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    /**
     * Handles the incoming client request by reading the input stream, 
     * parsing the request, and dispatching it to the appropriate handler 
     * based on the HTTP method and requested resource.
     */
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedOutputStream dataOut = new BufferedOutputStream(clientSocket.getOutputStream())) {

            // Leer la primera línea de la solicitud
            String requestLine = in.readLine();
            if (requestLine == null) return;

            // Parsear la línea de la solicitud
            String[] tokens = requestLine.split(" ");
            String method = tokens[0];
            String fileRequested = tokens[1];

            // Imprimir el encabezado de la solicitud
            printRequestHeader(requestLine, in);

            if (fileRequested.startsWith("/app")) {
                handleAppRequest(method, fileRequested, out);
            } else {
                if (method.equals("GET")) {
                    handleGetRequest(fileRequested, out, dataOut);
                } else if (method.equals("POST")) {
                    handlePostRequest(fileRequested, out, dataOut, in);
                } else {
                    out.println("HTTP/1.1 405 Method Not Allowed");
                    out.println("Content-type: text/html");
                    out.println();
                    out.println("<html><body><h1>405 Method Not Allowed</h1></body></html>");
                    out.flush();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close(); // Cerrando el socket aquí, después de procesar la solicitud
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Prints the HTTP request headers to the console.
     *
     * @param requestLine The first line of the HTTP request, typically containing the method, URI, and HTTP version.
     * @param in The BufferedReader used to read the rest of the HTTP headers from the input stream.
     * @throws IOException If an I/O error occurs while reading the headers.
     */
    private void printRequestHeader(String requestLine, BufferedReader in) throws IOException {
        System.out.println("header: " + requestLine);
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            System.out.println("header: " + inputLine);
            if (!in.ready()) {
                break;
            }
        }
    }

    /**
     * Handles a POST request by reading the body of the request and responding with a confirmation message.
     *
     * @param fileRequested The path of the requested file or resource.
     * @param out The PrintWriter used to send the response headers and body back to the client.
     * @param dataOut The BufferedOutputStream used to send any binary data (if necessary) back to the client.
     * @param in The BufferedReader used to read the body of the POST request.
     * @throws IOException If an I/O error occurs while reading the request or writing the response.
     */
    private void handlePostRequest(String fileRequested, PrintWriter out, BufferedOutputStream dataOut, BufferedReader in)
            throws IOException {
        if (fileRequested.startsWith("/app/hellopost")) {
            StringBuilder body = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null && !inputLine.isEmpty()) {
                body.append(inputLine).append("\n");
            }
            System.out.println("Body: " + body.toString());
            out.println("HTTP/1.1 200 OK");
            out.println("Content-type: text/html");
            out.println();
            out.flush();
            out.println("<html><body><h1>Post Request Handled</h1></body></html>");
            out.flush();
        }
    }

    /**
     * Handles a GET request by determining whether the requested resource is a dynamic service or a static file,
     * and responds accordingly.
     *
     * @param fileRequested The path of the requested file or resource.
     * @param out The PrintWriter used to send the response headers and, in some cases, the response body back to the client.
     * @param dataOut The BufferedOutputStream used to send the binary data of the file (if necessary) back to the client.
     * @throws IOException If an I/O error occurs while reading the requested file or writing the response.
     */
    private void handleGetRequest(String fileRequested, PrintWriter out, BufferedOutputStream dataOut)
            throws IOException {
        if (fileRequested.startsWith("/app/hello")) {
            out.println("HTTP/1.1 200 OK");
            out.println("Content-type: application/json");
            out.println();
            out.println(SimpleWebServer.services.get("hello").response(fileRequested));
        } else {
            File file = new File(SimpleWebServer.WEB_ROOT, fileRequested);
            int fileLength = (int) file.length();
            String content = getContentType(fileRequested);

            if (file.exists()) {
                byte[] fileData = readFileData(file, fileLength);
                out.println("HTTP/1.1 200 OK");
                out.println("Content-type: " + content);
                out.println("Content-length: " + fileLength);
                out.println();
                out.flush();
                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
            } else {
                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-type: text/html");
                out.println();
                out.flush();
                out.println("<html><body><h1>File Not Found</h1></body></html>");
                out.flush();
            }
        }
    }

    /**
     * Handles requests to application-specific endpoints by determining the HTTP method and invoking the appropriate service.
     * Supports both GET and POST methods for the `/app/hello` endpoint. Responds with a 405 status code for unsupported methods.
     *
     * @param method The HTTP method of the request (e.g., GET, POST).
     * @param fileRequested The path of the requested resource within the application context.
     * @param out The PrintWriter used to send the response headers and body back to the client.
     */
    private void handleAppRequest(String method, String fileRequested, PrintWriter out) {
        if (method.equals("GET")) {
            String serviceRequest = fileRequested.substring(fileRequested.indexOf("/app/") + 5);
            String response = SimpleWebServer.services.get("hello").response(serviceRequest);
            out.println("HTTP/1.1 200 OK");
            out.println("Content-type: text/html");
            out.println();
            out.println(response);
        } else if (method.equals("POST")) {
            String serviceRequest = fileRequested.substring(fileRequested.indexOf("/app/") + 5);
            String response = SimpleWebServer.services.get("hello").response(serviceRequest);
            out.println("HTTP/1.1 200 OK");
            out.println("Content-type: text/html");
            out.println();
            out.println(response);
        } else {
            // Manejar métodos no permitidos
            out.println("HTTP/1.1 405 Method Not Allowed");
            out.println("Content-type: text/html");
            out.println();
            out.println("{\"error\": \"Unsupported HTTP method\"}<br />Unsupported HTTP method");
        }
        out.flush();
    }

    /**
     * Determines the MIME type of a requested file based on its file extension.
     *
     * @param fileRequested The name or path of the requested file.
     * @return The MIME type as a string corresponding to the file's extension. Returns "text/plain" if the extension is unrecognized.
     */
    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".html"))
            return "text/html";
        else if (fileRequested.endsWith(".css"))
            return "text/css";
        else if (fileRequested.endsWith(".js"))
            return "application/javascript";
        else if (fileRequested.endsWith(".png"))
            return "image/png";
        else if (fileRequested.endsWith(".jpg"))
            return "image/jpeg";
        return "text/plain";
    }
    
    /**
     * Reads the data from a specified file into a byte array.
     *
     * @param file The file to be read.
     * @param fileLength The length of the file in bytes.
     * @return A byte array containing the data read from the file.
     * @throws IOException If an I/O error occurs while reading the file.
     */
    private byte[] readFileData(File file, int fileLength) throws IOException {
        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] fileData = new byte[fileLength];
            fileIn.read(fileData);
            return fileData;
        }
    }
}
