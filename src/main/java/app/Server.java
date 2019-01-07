package app;
import org.apache.zookeeper.KeeperException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;

@SpringBootApplication
public class Server {
    public static void main(String argv[]) throws KeeperException, InterruptedException {
        if (argv.length == 2) {
            String port = argv[0];
            String host = argv[1];
            ClassLoader classLoader = Server.class.getClassLoader();
            File file = new File(classLoader.getResource("application.properties").getFile());

            try {
                FileWriter writer = new FileWriter(file, false);
                writer.write("server.port = " + port + "\n" +
                        "zkHost= " + host);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        SpringApplication.run(Server.class, argv);
    }
}
