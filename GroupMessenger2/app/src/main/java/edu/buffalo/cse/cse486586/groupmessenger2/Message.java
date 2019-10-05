package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;



public class Message{
    String msg;
    float seq_no;
    String from;
    Boolean accepted;
    int propFrom;


}

class MessageComparator implements Comparator<Message>{
    public int compare(Message m1, Message m2){
        if(m2.seq_no < m1.seq_no)
            return 1;
        else if(m2.seq_no > m1.seq_no)
            return -1;
        else
            return 0;
    }
}
/* To implement the priority queue which will keep the lowest sequence no. at the top of the queue.
References : https://www.geeksforgeeks.org/implement-priorityqueue-comparator-java/
*/