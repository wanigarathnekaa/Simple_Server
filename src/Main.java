import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static java.nio.file.Files.createTempFile;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 2728;

        try {
            System.out.println("Waiting for User");
            ServerSocket ss = new ServerSocket(port);
            System.out.println("Server Listening on port " + port);


            while (true) {
                //accept() method accept a connection request from a client
                Socket socket = ss.accept();

                new RequestHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class RequestHandler extends Thread {
        private Socket socket;

        public RequestHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                //This bufferReader used for read input from the socket, (it takes the input from a source)
                //Here from InputStreamReader it reads data as characters
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                OutputStream outputStream = socket.getOutputStream();
//              PrintWriter serverResponse = new PrintWriter(outputStream, true);

                String request = bufferedReader.readLine();
                System.out.println(request);

                if (request != null) {
                    String[] requestParts = request.split(" ");
                    String method = requestParts[0];
                    String filePath = requestParts[1];
                    String[] correctFilepath = filePath.split("\\?");
                    filePath = "htdocs"+correctFilepath[0];
                    System.out.println(filePath);
                    if(filePath.equals("htdocs/")){
                        filePath = "htdocs/index.php";
                    }
                    if (method.equals("GET") ) {
                        if (filePath.endsWith(".php")) {
                            if(correctFilepath.length>1) {
                                String params = correctFilepath[1];
                                executePhpScript(filePath, params, outputStream);
                            } //for get method php forms
                            else {
                                executePhpScript(filePath, outputStream);
                            }
                        } else {
                            // Serve static files
                            serveFile(filePath, outputStream);
                        }
                    } else {
                        handlePostRequest(bufferedReader ,filePath ,outputStream);
                    }
                }

                bufferedReader.close();
                outputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void serveFile(String filePath, OutputStream outputStream) throws IOException {
//        String[] fileName = filePath.split("/");
//        File file = new File(fileName[1]);
//        System.out.println(fileName[1]);

        File file = new File(filePath);

        if (file.exists() && file.isFile()) {
            //fileReader object reads the input of the file in the filePath
            BufferedReader fileReader = new BufferedReader(new FileReader(file));

            //Created a stringBuilder; s a class in Java that provides a way to efficiently construct and manipulate strings
            StringBuilder fileContent = new StringBuilder();
            String line = "";

            while ((line = fileReader.readLine()) != null) {
                //Get all the content in the file by appending each line the file
                fileContent.append(line).append("\n");
            }

            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + file.length() + "\r\n" +
                    "Connection: close\r\n\r\n";

            outputStream.write(responseHeaders.getBytes());
            outputStream.write(fileContent.toString().getBytes());
        }
        else
        {
            sendResponse(outputStream, "HTTP/1.1 404 Not Found", "File Not Found");
        }
    }
    private static void sendResponse(OutputStream outputStream, String statusLine, String message) throws IOException {
        String response = statusLine + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + message.length() + "\r\n" +
                "Connection: close\r\n\r\n" +
                message;

        outputStream.write(response.getBytes());
    }

    private static void executePhpScript(String filePath,String params,OutputStream outputStream) throws IOException {
        try {
            // Create a ProcessBuilder to run the PHP interpreter with the script file
            System.out.println(filePath);
            String tempFileName = createTempFile(filePath);
            ProcessBuilder pb = new ProcessBuilder("php", tempFileName ,params);

            // Redirect error stream to the output stream
            pb.redirectErrorStream(true);

            // Start the PHP process
            Process process = pb.start();

            // Get the input stream of the PHP process (the output of the script)
            InputStream scriptOutput = process.getInputStream();

            // Create a buffer to read the script output
            byte[] buffer = new byte[1024];
            int bytesRead;

            // Write the HTTP response headers
            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +   // Set the content type to text/html for PHP output
                    "Connection: close\r\n\r\n";
            outputStream.write(responseHeaders.getBytes());

            // Read and write the script output to the client
            while ((bytesRead = scriptOutput.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // Wait for the PHP process to complete
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                // Handle PHP execution errors if needed
                // You can send an error response or log the error here
            }

            // Close the output stream
            outputStream.close();

            // Delete temp file
            File tempFile = new File(tempFileName);
            tempFile.delete();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String createTempFile (String filePath) throws IOException {
        Writer fileWriter = null;
        try {
            Path fileName = Path.of(filePath);
            String str = Files.readString(fileName);

            String tempFileName = "./" + Instant.now().toEpochMilli() + ".php";
            fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileName), "utf-8"));

            // Append php argument reading line
            str = "<?php parse_str(implode('&', array_slice($argv, 1)), $_GET); ?> \n\n" + str;
            fileWriter.write(str);

            return tempFileName;
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            fileWriter.close();
        }

        return filePath;
    }

    private static void executePhpScript(String filePath, OutputStream outputStream) throws IOException {
        try {
            // Create a ProcessBuilder to run the PHP interpreter with the script file
            ProcessBuilder processBuilder = new ProcessBuilder("php", filePath );

            // Redirect error stream to the output stream
            processBuilder.redirectErrorStream(true);

            // Start the PHP process
            Process process = processBuilder.start();

            // Get the input stream of the PHP process (the output of the script)
            InputStream scriptOutput = process.getInputStream();

            // Create a buffer to read the script output
            byte[] buffer = new byte[1024];
            int bytesRead;

            // Write the HTTP response headers
            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Connection: close\r\n\r\n";
            outputStream.write(responseHeaders.getBytes());

            // Read and write the script output to the client
            while ((bytesRead = scriptOutput.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            // Close the output stream
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handlePostRequest(BufferedReader bufferedReader,String filePath, OutputStream outputStream) throws IOException {
        // Read and process the POST request body
        //StringBuilder requestBody = new StringBuilder();
        int contentLength = 0;
        String line;

        // Read the Content-Length header to determine the length of the request body
        while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
            }
        }

        // Read the request body
        char[] buffer = new char[contentLength];
        int bytesRead = 0;
        while (bytesRead < contentLength) {
            int read = bufferedReader.read(buffer, bytesRead, contentLength - bytesRead);
            if (read == -1) {
                break;
            }
            bytesRead += read;
        }

        // Convert the request body to a string
        String postBody = new String(buffer);

        executePhpScript(filePath, postBody, outputStream);
        }
}

/*
    String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + file.length() + "\r\n" +
                    "Connection: close\r\n\r\n";

    These headers are used to provide information about the response to the
    client, including the content type and other relevant details

    HTTP/1.1 200 OK: This is the HTTP status line. It indicates that the response is using HTTP version 1.1
    and has a status code of 200, which means "OK" or a successful response.

    This header specifies the type of content being sent in the response body. In this case, it's indicating
    that the content is in HTML format. You can change this to other content types like "application/json"
    for JSON data or "text/plain" for plain text.

     This header indicates that the connection between the client and the server should be closed after
     this response is sent


     while ((bytesRead = scriptOutput.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

     The purpose of this code is to efficiently transfer the output of the PHP script to the client's browser.
     It reads chunks of data from the PHP script's output and writes them to the client's output stream until
     there is no more data to read (read returns -1), ensuring that the PHP-generated content is sent to the
     client in smaller, manageable chunks rather than attempting to load the entire content into memory at once.
*/