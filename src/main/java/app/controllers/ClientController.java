package app.controllers;
import app.Utils;
import app.Watchers.BlockWatcher;
import app.Watchers.PortWatcher;
import app.models.Block;
import app.models.BlockChain;
import app.models.RebootResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.zookeeper.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;

import static app.Utils.getHTML;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
public class ClientController implements Watcher{
    public static ZooKeeper zk = null;
    public static String blockRoot = "/BLOCKS";
    public static String portRoot = "/PORTS";
    public static int port;
    static BlockChain blockChain;
    static PortWatcher portWatcher;
    static BlockWatcher blockWatcher;
    @Autowired
    public ClientController(Environment env) {

        String zkHost = env.getProperty("zkHost");
        int port = Integer.parseInt(env.getProperty("server.port"));
        try {
            ClientController.port = port;
            zk = new ZooKeeper(zkHost, 3000, this);
            blockChain = new BlockChain();
            portWatcher = new PortWatcher(blockChain);
            blockWatcher = new BlockWatcher(blockChain);


            //================= REBOOT SUPPORT =================
            // invalidate all previous blocks that hasn't been added yet
            zk.create(blockRoot + "/", String.valueOf(-port + "~" + new Timestamp(System.currentTimeMillis()).toString()).getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT_SEQUENTIAL);
            // take current blockchain from alivePort that hasn't been rebooted
            List<String> alivePorts = zk.getChildren(portRoot, this);
            if(alivePorts.size() > 0){
                synchronized (blockChain){
                    UpdateAllBlocks(alivePorts);
                }
            }
            //================= REBOOT SUPPORT =================
            // add port to membership ports
            zk.create(portRoot + "/" + port, (port + "").getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            portWatcher.init();
            blockWatcher.init();
        } catch (IOException | InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    private void UpdateAllBlocks(List<String> ps) {
        int i = 0;
        while (true) {
            if (ps.size() <= i) {
                return;
            }
            String p = ps.get(i);
            // create an http request
            try {
                String getResult = Utils.getHTML("http://localhost:" + p + "/rebootBlocks");
                RebootResponse res = new Gson().fromJson(getResult, RebootResponse.class);
                blockChain.addAll(res.getBlist());
                blockWatcher.setLoc(res.getLoc());
                return;
            } catch (Exception e) {
                System.out.println("Something happened with reboot of chainblock");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {

    }

    /**
     * ==========================SERVER COMMUNICATION==================================
     */


    @GetMapping("restoreHelp")
    public @ResponseBody Block restoreHelp(@RequestParam("blockData") String blockData){
        Integer port = Integer.parseInt(blockData.split("~")[0]);
        String timestamp = blockData.split("~")[1];
        int loc = Integer.parseInt(blockData.split("~")[2]);
        if (blockChain.getSize() > loc){
            return blockChain.getBlockById(loc);
        }
        else {
            return blockChain.getPendingBy(port, timestamp);
        }

    }

    @PostMapping("/receiveBlock")
    boolean recieveBlock(@RequestBody String blockJson) {
        synchronized (this) {
            System.out.println("got a block from " + blockJson);
            Block block = new Gson().fromJson(blockJson, Block.class);
            blockChain.addPendingBlock(block);
        }
        return true;
    }


    @GetMapping("/rebootBlocks")
    RebootResponse rebootBlocks() {
        RebootResponse b;
        synchronized (blockChain){
            b = new RebootResponse(blockChain.getChain(), blockWatcher.getLoc());
        }
        return b;
    }

    /**
     * ==========================CLIENT COMMUNICATION==================================
     */

    @GetMapping("/help")
    String help(){
        return Utils.generateHelpHtml();
    }

    @GetMapping("/allPending")
    Map<Integer, List<Block>> allPending(){
        Map<Integer, List<Block>> b;
        synchronized (blockChain){
            b = blockChain.getAllPending();
        }
        return b;
    }

    @GetMapping("/allBlocks")
    List<Block> all() {
        List<Block> b;
        synchronized (blockChain){
            b = blockChain.getChain();
        }
        return b;
    }

    @GetMapping("/blockSizeHistory")
    List<Integer> blockSizeHistory() {
        return blockChain.getBlockSizeHistory();
    }


    @PostMapping("/addTransaction")
    String addTransaction(@RequestBody String data) {
        boolean success = false;
        synchronized (blockChain) {
            success = blockChain.addTransaction(data);
        }

        return success ? "Received. Update in progress" : "Bad transaction";
    }

    @GetMapping("/block/{id}")
    Resource<Block> getBlock(@PathVariable int id) {
        Block b = blockChain.getBlockById(id);
        return new Resource<>(b,
                linkTo(methodOn(ClientController.class).getBlock(id)).withSelfRel(),
                linkTo(methodOn(ClientController.class).all()).withRel("blocks"));
    }


}
