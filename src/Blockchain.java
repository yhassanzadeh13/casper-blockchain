import java.util.Iterator;
import java.util.LinkedList;

public class Blockchain<Object> extends LinkedList<Object> {

    public Block find(String hash){
        for (Iterator<Object> it = this.iterator(); it.hasNext(); ) {
            Object b = it.next();
            Block block = (Block) b;
            if(block.hash.compareTo(hash) == 0){
                return block;
            }
        }
        return null;


    }

    public Checkpoint getLastCheckpoint() {
        for (Iterator<Object> it = this.descendingIterator(); it.hasNext(); ) {
            Object b = it.next();
            Block block = (Block) b;
            if(block instanceof Checkpoint){
                return (Checkpoint) block;
            }
        }
        return null;
    }

    public Checkpoint getLastCheckpoint(Checkpoint checkpoint) {
        //returns the last checkpoint before the given checkpoint
        boolean flag = false;
        for (Iterator<Object> it = this.descendingIterator(); it.hasNext(); ) {
            Object b = it.next();
            Block block = (Block) b;
            if(block.hash.compareTo(checkpoint.hash) == 0){
                flag = true;
            }
            if(flag && (block instanceof Checkpoint)){
                return (Checkpoint) block;
            }
        }
        return null;
    }
}
