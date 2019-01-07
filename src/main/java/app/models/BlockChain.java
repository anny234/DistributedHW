package app.models;

import app.Utils;
import app.controllers.ClientController;
import com.google.gson.Gson;
import org.apache.zookeeper.*;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import static app.Utils.postHTML;
import static app.controllers.ClientController.*;

public class BlockChain {
    private long start_batch = System.nanoTime();
    private double ratio = 10;
    private double delta = 0.1;
    private List<Block> blist = new ArrayList<>();
    private Map<Integer, List<Block>> pendingBlocks = new HashMap<>();
    private Block currentBlock = new Block(ClientController.port);
    private List<Integer> blockSizeHistory = new ArrayList<>();
    private int maxBlockSize = 1;


    public List<Block> getChain(){
        return blist;
    }

    public boolean addTransaction(String data) {
        Boolean success = this.currentBlock.addTransaction(data);
        if (currentBlock.getSize() >= maxBlockSize) {
            long end_batch = System.nanoTime();
            long batch_time = end_batch - start_batch;
            Block candidateBlock = currentBlock.sendX(maxBlockSize);
            start_batch = System.nanoTime();
            try {
                // first we send everyone the block
                List<String> children = zk.getChildren(portRoot, null);
                long start_send = System.nanoTime();
                for (String port : children){
                    if (Integer.parseInt(port) == ClientController.port){
                        this.addPendingBlock(candidateBlock);
                    }
                    else {
                        postHTML("http://localhost:" + port + "/receiveBlock", candidateBlock);
                    }
                }
                /**
                 * DIE
                 * FOR
                 * DEMO
                 */
                //System.exit(1);

                // port is enough because we use http which is above TCP
                // so any blocks server A sends server B will always comes in the right order
                zk.create(blockRoot + "/", String.valueOf(port + "~" + candidateBlock.timestamp).getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT_SEQUENTIAL);
                // blocks won't be deleted before next demo
                long end_send = System.nanoTime();
                long send_time = end_send - start_send;
                this.blockSizeHistory.add(maxBlockSize);
                double current_ratio = (double) batch_time / send_time;
                if (current_ratio-ratio > delta){
                    maxBlockSize = (maxBlockSize == 1) ? 1 : maxBlockSize / 2;
                }
                else if (ratio - current_ratio > delta){
                    maxBlockSize *= 2;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return success;
    }

    public int getSize(){
        return blist.size();
    }

    public Block getBlockById(int id) {
        return blist.size() > id ? blist.get(id) : null;
    }

    public void addPendingBlock(Block block) {
        if (this.pendingBlocks.containsKey(block.port)){
            this.pendingBlocks.get(block.port).add(block);
        }
        else {
            ArrayList<Block> pendingList = new ArrayList<>();
            pendingList.add(block);
            this.pendingBlocks.put(block.port, pendingList);
        }
    }

    private void restoreBlock(String blockData){
        try {
            List<String> alivePorts = zk.getChildren(portRoot, null);
            for (String port : alivePorts){
                if (ClientController.port == Integer.parseInt(port)){
                    continue;
                }
                String blockJson = Utils.getHTML("http://localhost:" + port + "/restoreHelp?blockData=" + URLEncoder.encode(blockData,"UTF-8"));
                if (blockJson.equals("") || blockJson.contains("null") || blockJson.equals("{}")){
                    continue;
                }
                Block block = new Gson().fromJson(blockJson, Block.class);
                blist.add(block);
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addPermenantBlock(String blockData){
        int port = Integer.parseInt(blockData.split("~")[0]);
        String timestamp = blockData.split("~")[1];
        if (port < 0){
            if (this.pendingBlocks.containsKey(port)) {
                this.pendingBlocks.put(port, this.pendingBlocks.get(port).stream()
                        .filter(b -> Timestamp.valueOf(b.timestamp).after(Timestamp.valueOf(timestamp)))
                        .collect(Collectors.toList()));
            }
            return;
        }
        if (this.pendingBlocks.containsKey(port)){
            List<Block> blocks = this.pendingBlocks.get(port);
            Block block = blocks.get(0);
            if (block.timestamp.equals(timestamp)) {
                this.blist.add(block);
                blocks.remove(0);
            }
            else {
                restoreBlock(blockData + "~" + blist.size());
            }
        }
        else {
            restoreBlock(blockData + "~" + blist.size());
        }
    }

    public void removeBlocksOfServer(List<String> alivePorts){
        Set<Integer> pendingPorts = this.pendingBlocks.keySet();
        for (Integer i : pendingPorts){
            if (! alivePorts.contains(i.toString())){
                this.pendingBlocks.remove(i);
            }
        }
    }

    public List<Integer> getBlockSizeHistory(){
        return this.blockSizeHistory;
    }

    public Block getPendingBy(Integer port, final String timestamp) {
        if (this.pendingBlocks.containsKey(port)){
            List<Block> queryBlocks = this.pendingBlocks.get(port).stream().filter(b -> timestamp.equals(b.timestamp)).collect(Collectors.toList());
            if (queryBlocks.size() > 0){
                return queryBlocks.get(0);
            }
        }
        return null;
    }

    public Map<Integer, List<Block>> getAllPending() {
        return this.pendingBlocks;
    }

    public void addAll(List<Block> blist) {
        this.blist = blist;
    }
}
