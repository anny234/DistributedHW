package app.Watchers;

import app.models.BlockChain;
import org.apache.zookeeper.*;


import java.util.List;

import static app.controllers.ClientController.*;


public class PortWatcher implements Watcher {
    private final BlockChain blockChain;

    public PortWatcher(BlockChain blockChain) {

        this.blockChain = blockChain;
        try {
            if (zk.exists(portRoot, null) == null) {
                zk.create(portRoot, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void init(){
        try {
            if (zk.exists(portRoot, this) == null) {
                zk.create(portRoot, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            zk.getChildren(portRoot, this);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        System.out.println("port watched!");
        synchronized (blockChain) {
            try {
                List<String> children = zk.getChildren(portRoot, this);
                blockChain.removeBlocksOfServer(children);
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
