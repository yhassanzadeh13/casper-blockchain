import java.io.Serializable;

public abstract class Message implements Serializable {

    public Message(){
        System.out.println("no arg constructor used for message");

    }

    /*
    use cases:
    - votes
    - when a commandment violation is detected (maybe not, waiting confirmation from Yahya)
    - when a node wants to become a validator
    - when a node wants to quit the job of validator
     */




}
