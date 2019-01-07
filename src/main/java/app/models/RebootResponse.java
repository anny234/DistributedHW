package app.models;

import java.util.List;

public class RebootResponse {
    List<Block> blist;
    int loc;

    public List<Block> getBlist() {
        return blist;
    }

    public int getLoc() {
        return loc;
    }

    public RebootResponse(List<Block> blist, int loc) {
        this.blist = blist;
        this.loc = loc;
    }
}
