package app;

import app.models.Block;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class Utils {

    // Returns a String of a valid html page
    public static String generateHelpHtml(){
        return "<!-- #######  YAY, I AM THE SOURCE EDITOR! #########-->\n" +
                "<h1 style=\"color: #5e9ca0;\">Welcome to BlockChain Server</h1>\n" +
                "<h2 style=\"color: #2e6c80;\">What am I:</h2>\n" +
                "<p>This is a distributed system to handle the well known blockchain. <br />Different servers constructs the blockchain from transactions recieved from clients and then ditribute them through the network.<br /><br /></p>\n" +
                "<h2 style=\"color: #2e6c80;\">Some useful features:</h2>\n" +
                "<ol style=\"list-style: none; font-size: 14px; line-height: 32px; font-weight: bold;\">\n" +
                "<li style=\"clear: both;\"><img style=\"float: left;\" src=\"https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSBq-XG7OFptHky_v7KQ5tN6cj808QwcIxdVIoYq9GJ9qz6Oup5\" alt=\"interactive connection\" width=\"45\" />&nbsp;Add a transaction.<br /><br /></li>\n" +
                "<li style=\"clear: both;\"><img style=\"float: left;\" src=\"https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS-_79XIC03ry7iHZc5xI_RrpnYRGGJr1TGivPu7uB2I-gP3w_F\" alt=\"html cleaner\" width=\"45\" />&nbsp;Get all blocks that are in the blockchain.<br /><br /></li>\n" +
                "<li style=\"clear: both;\"><img style=\"float: left;\" src=\"https://cdn3.iconfinder.com/data/icons/flat-office-icons-1/140/Artboard_1-11-512.png\" alt=\"Word to html\" width=\"45\" />&nbsp;Get all pending blocks ( not yet in the blockchain ).<br /><br /></li>\n" +
                "<li style=\"clear: both;\"><img style=\"float: left;\" src=\"https://html-online.com/img/04-replace.png\" alt=\"replace text\" width=\"45\" />&nbsp;Discover different previous block sizes that were determined by the&nbsp; &nbsp;adaptive batching algorithm.</li>\n" +
                "<li style=\"clear: both;\"><img style=\"float: left;\" src=\"https://cdn0.iconfinder.com/data/icons/social-messaging-ui-color-shapes/128/search-circle-blue-512.png\" alt=\"gibberish\" width=\"45\" />&nbsp;Get a specific block.<br /><br /></li>\n" +
                "<li style=\"clear: both;\"><img style=\"float: left;\" src=\"https://html-online.com/img/01-interactive-connection.png\" alt=\"gibberish\" width=\"45\" />&nbsp;Find out what the server can do.</li>\n" +
                "</ol>";
    }

    /* Creates a Get HTTP Request and returns the matching response.
        ASSUMPTION: parameters are already inserted correctly to url
     */
    public static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }

    /* Creates a Post HTTP Request and returns the matching repsonse
        ASSUMPTION: parameter is a block to be send.
     */
    public static void postHTML(String urlToRead, Block b) throws Exception {
        URL object=new URL(urlToRead);

        HttpURLConnection con = (HttpURLConnection) object.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod("POST");

        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(new Gson().toJson(b));
        wr.flush();
        //display what returns the POST request
        int HttpResult = con.getResponseCode();
        if (HttpResult == HttpURLConnection.HTTP_OK) {
            System.out.println("okay");
        } else {
            System.out.println(con.getResponseMessage());
        }
    }

}
