package app.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Block {
    private List<String> transactions; //our data will be a simple message.
    public int port;
    public String timestamp;

    //Block Constructor.
    public Block(int port) {
        timestamp = new Timestamp(System.currentTimeMillis()).toString();
        this.transactions = new ArrayList<>();
        this.port = port;
    }

    public boolean addTransaction(String data){
        if (checkValidity(data)){
            transactions.add(data);
            return true;
        }
        return false;
    }

    public int getSize(){
        return transactions.size();
    }

    public Block sendX(int blockSize){
        Block newBlock = new Block(this.port);
        for (int i = 0; i < blockSize; i ++){
            String transaction = this.transactions.get(0);
            this.transactions.remove(0);
            newBlock.addTransaction(transaction);
        }
        return newBlock;

    }

    public boolean checkValidity(String data){
        return (data.length() > 3);
    }

}
