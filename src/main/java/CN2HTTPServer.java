
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class CN2HTTPServer implements Runnable {

    private final Socket clientConnection;
    private static Logger logger;

    public CN2HTTPServer(Socket clientConnection) {
        this.clientConnection = clientConnection;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        try {
            logger = LoggerFactory.getLogger(CN2HTTPServer.class.getSimpleName());
            var serverConnection = new ServerSocket(Config.PORT);
            logger.info(String.format("Server listening for connections on port : %d\n", Config.PORT));

            while (true) {
                var myServer = new CN2HTTPServer(serverConnection.accept());
                logger.info(String.format("Connection opened. (%s)", new Date()));
                var thread = new Thread(myServer);
                thread.start();
            }
        } catch (IOException e) {
            logger.error(String.format("Server Connection error : %s", e.getMessage()));
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;

        try {
            in = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
            out = new PrintWriter(clientConnection.getOutputStream());
            dataOut = new BufferedOutputStream(clientConnection.getOutputStream());

            var input = in.readLine();
            var parse = new StringTokenizer(input);
            var method = parse.nextToken().toUpperCase();
            fileRequested = parse.nextToken().toLowerCase();

            logger.info(String.format("Requested file %s from %s", fileRequested, clientConnection.getRemoteSocketAddress().toString()));

            if (!method.equals(Config.Methods.GET.get()) && !method.equals(Config.Methods.HEAD.get())) methodNotSupported(out, dataOut, method);
            else methodSupported(out, dataOut, fileRequested, method);
        } catch (FileNotFoundException | NullPointerException e) {
            try {
                fileNotFound(out, dataOut, fileRequested);
            } catch (IOException ioe) {
                logger.error(ioe.getMessage());
            }
        } catch (IOException e) {
            logger.error("Server error : " + e);
        } finally {
            close(in, out, dataOut);
        }
    }

    private void close(BufferedReader in, PrintWriter out, BufferedOutputStream dataOut) {
        try {
            in.close();
            out.close();
            dataOut.close();
            clientConnection.close();
        } catch (NullPointerException | IOException e) {
            logger.error("Error closing stream : " + e.getMessage());
        }
        logger.info("Connection closed.\n");
    }

    private void methodSupported(PrintWriter out, BufferedOutputStream dataOut, String fileRequested, String method) throws IOException {
        if (fileRequested.endsWith("/")) fileRequested += Config.INDEX_HTML;

        var file = new File(Config.WEB_ROOT, fileRequested);
        var fileLength = (int) file.length();
        var content = getContentType(fileRequested);

        if (method.equals(Config.Methods.GET.get())) {
            var fileData = readFileData(file, fileLength);
            out.println(Config.Codes.OK.get());
            writeHeader(out, dataOut, fileLength, content, fileData);
        }

        logger.info("File " + fileRequested + " of type " + content + " returned");
    }

    private void methodNotSupported(PrintWriter out, BufferedOutputStream dataOut, String method) throws IOException {
        logger.info("501 Not Implemented : " + method + " method.");

        var file = new File(Config.WEB_ROOT, Config.UNSUPPORTED_FILE);
        var fileLength = (int) file.length();
        var fileData = readFileData(file, fileLength);

        out.println(Config.Codes.NOT_IMPLEMENTED.get());
        writeHeader(out, dataOut, fileLength, Config.MimeTypes.HTML.get(), fileData);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn;
        var fileData = new byte[fileLength];
        fileIn = new FileInputStream(file);
        fileIn.read(fileData);
        fileIn.close();
        return fileData;
    }

    private String getContentType(String fileRequested) {
        return (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html")) ? Config.MimeTypes.HTML.get() : Config.MimeTypes.PLAIN.get();
    }

    private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
        var file = new File(Config.WEB_ROOT, Config.NOT_FOUND_FILE);
        var fileLength = (int) file.length();
        var fileData = readFileData(file, fileLength);

        out.println(Config.Codes.NOT_FOUND.get());
        writeHeader(out, dataOut, fileLength, Config.MimeTypes.HTML.get(), fileData);

        logger.warn("File " + fileRequested + " not found");
    }

    private void writeHeader(PrintWriter out, OutputStream dataOut, int fileLength, String content, byte[] fileData) throws IOException {
        out.println(Config.Headers.SERVER.get());
        out.println(Config.Headers.DATE.get(new Date()));
        out.println(Config.Headers.CONTENT_TYPE.get(content));
        out.println(Config.Headers.CONtENT_LENGTH.get(fileLength));
        out.println();
        out.flush();

        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
    }
}
