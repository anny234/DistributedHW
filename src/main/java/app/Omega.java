package app;
import org.apache.zookeeper.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
public class Omega  implements Watcher {
    static ZooKeeper zk = null;
    static String root = "/OMEGA";
    static int ID;
    static int elected;
    Object lock = new Object();
    public Omega(String zkHost, int id) {
        try {
            zk = new ZooKeeper(zkHost, 3000, this);
            ID = id;
            elected = -1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void propose() throws KeeperException, InterruptedException {
        if (zk.exists(root, true) == null) {
            zk.create(root, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        zk.create(root + "/", String.valueOf(ID).getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
    }
    public void electLeader() throws KeeperException, InterruptedException {
        synchronized (lock) {
            List<String> children = zk.getChildren(root, true);
            Collections.sort(children);
            byte[] data = null;
            for (String leader : children) {
                data = zk.getData(root + "/" + leader, true , null);
                if (data != null) {
                    break;
                }
            }
            if (data != null) {
                elected = Integer.parseInt(new String(data));
            }
        }


    }
    public int getLeader() {
        synchronized (lock) {
            return elected;
        }
    }
    public void process(WatchedEvent watchedEvent) {
        final Event.EventType eventType = watchedEvent.getType();
        System.out.println("got event " + eventType);

        try {
            electLeader();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
