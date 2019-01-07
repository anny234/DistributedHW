package app.Watchers;

import app.models.BlockChain;
import org.apache.zookeeper.*;

import java.util.Collections;
import java.util.List;

import static app.controllers.ClientController.*;

public class BlockWatcher implements Watcher {
    private final BlockChain blockChain;
    private int loc = 0;

    // ==================================
    // GETTERS AND SETTERS
    public int getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }
    // ==================================

    public BlockWatcher(BlockChain blockChain) {
        this.blockChain = blockChain;
        try {
            if (zk.exists(blockRoot, null) == null) {
                zk.create(blockRoot, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    // SUBSCRIBE TO WATCHER
    public void init(){
        try {
            zk.getChildren(blockRoot, this);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // LISTENER IMPLEMENTATION
    @Override
    public void process(WatchedEvent watchedEvent) {
        synchronized (blockChain) {
            try {
                List<String> children = zk.getChildren(blockRoot, this);
                Collections.sort(children);
                for (int i = loc; i < children.size(); i ++) {
                    String blockPath = children.get(i);
                    byte[] data = zk.getData(blockRoot + "/" + blockPath, true, null);
                    blockChain.addPermenantBlock((new String(data)));
                }
                loc = children.size();
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
