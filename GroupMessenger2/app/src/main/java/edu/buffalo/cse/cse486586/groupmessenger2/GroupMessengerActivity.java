package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static String[] REMOTE_PORTS = {"11108","11112", "11116", "11120", "11124"};

    static final int SERVER_PORT = 10000;
    int failedPort;
    final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");   //From OnPTestClickListener to build the URI.
    int sequence_no = -1;
    float proposed_no;
    // Storing the seq no sender along the seq no .
    HashMap<Float, Integer> propPort = new HashMap<Float, Integer>();
    //To store the proposed seq no. for the message
    PriorityQueue<Float> seqProposedQ = new PriorityQueue<Float>(100, Collections.reverseOrder());
    // To store the messages in holdBackQueue
    PriorityQueue<Message> holdBackQueue = new PriorityQueue<Message>(100, new MessageComparator());
    // HashMap to store all the messages received.
    HashMap<String, Message> messageHashMap  = new HashMap<String, Message>();
    String myPort;
    //From OnPTestClickListener
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        //Set the initial proposed no. with the corresponding port
        proposed_no = Float.parseFloat("0." + myPort);
        failedPort = 0;
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        //Sends the message to the server when the send button is pressed.
        final Button sendButton = (Button) findViewById(R.id.button4);
        sendButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                TextView tv = (TextView) findViewById(R.id.textView1);
                final EditText editText = (EditText) findViewById(R.id.editText1);
                String msg = editText.getText().toString() + "\n";                                  //Get input from the textView
                editText.setText("");                                                               // To reset the input box.
                tv.append("\t" + msg +"\n");                                                        // To display the string on the avd.

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            try {
                while(true) {
                    Socket clientSocket = serverSocket.accept();
                    //clientSocket.setSoTimeout(3000);
                    //https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html#getInputStream()   getInputStream to create an input stream to read the data.
                    //https://docs.oracle.com/javase/7/docs/api/java/io/DataInputStream.html            Used to read the data from the client

                    DataInputStream msg_stream = new DataInputStream(clientSocket.getInputStream());

                    String mReceived = msg_stream.readUTF();                                   //A method of DataInputStream to read the string data into the stream.
                    //Log.e("Inside server",mReceived);
                    String[] mesgReceived = mReceived.split("-");
                    //msg_stream.close();

                    //If a port has failed, it will update the avds with the failed port number
                    if (mesgReceived.length == 1) {
                        failedPort = Integer.parseInt(mesgReceived[0]);
                        Log.e("Updating", "Setting failed node "+ mesgReceived[0]);
                    } else {
                        //Spliting the message and storing them in a Message class variable
                        Message mesg = new Message();
                        mesg.msg = mesgReceived[0];
                        Log.e("M[0]", mesgReceived[0]);
                        Log.e("M[1]", mesgReceived[1]);
                        mesg.seq_no = Float.parseFloat(mesgReceived[1]);
                        Log.e("M[2]", mesgReceived[2]);
                        mesg.accepted = Boolean.parseBoolean(mesgReceived[2]);
                        Log.e("M[3]", mesgReceived[3]);
                        mesg.from = mesgReceived[3];
                        mesg.propFrom = Integer.parseInt(mesgReceived[4]);

                        //If the message is seeking for a proposal number i.e accepted = False
                        if (mesg.accepted.equals(Boolean.FALSE)) {
                            proposed_no += 1;                                                                   //proposed no. is of the form : seq.port
                            mesg.seq_no = proposed_no;                                                          //Incrementing the seq no and setting
                            mesg.propFrom = Integer.parseInt(myPort);

                            //http://developer.android.com/reference/android/os/AsyncTask.html                  publishProgress updates the UI by sending the message to be changed onto onProgressUpdate
                            //Store the message in a HashMap with message as the Key (Considering each message to be unique)
                            messageHashMap.put(mesg.msg, mesg);
                            //Adding the message to the Hold Back Queue of the AVD
                            holdBackQueue.add(mesg);
                            //https://docs.oracle.com/javase/7/docs/api/java/io/DataOutputStream.html           Creates a OutputStream to send an acknowledgement back to the sender.
                            //https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html#getOutputStream()  Used to create an outputstream on the socket to send the ack.

                            //Sending the proposed number for the message to the Client
                            DataOutputStream ack = new DataOutputStream(clientSocket.getOutputStream());
                            ack.writeUTF(Float.toString(mesg.seq_no) + "-" + mesg.propFrom);                                                            //A method of DataOutputStream to write string data into the stream
                            ack.close();

                            //Used to close the socket.
                        } else { //If the message has an accepted sequence number.

                            //Updating the seq no. to the max of the current seq no. and the accepted seq no. This will help keep the order.
                            int val = Math.max((int) proposed_no, (int) mesg.seq_no);
                            proposed_no = Float.parseFloat(Integer.toString(val) + "." + myPort);

                            //Extracting the message to update the Hold Back Queue.
                            Message msgToDelete = messageHashMap.get(mesg.msg);
                            holdBackQueue.remove(msgToDelete);
                            // Adding the message with the accepted seq no.
                            holdBackQueue.add(mesg);

                            //Sending an acknowledgement to maintain a Full Duplex
                            DataOutputStream ack = new DataOutputStream(clientSocket.getOutputStream());
                            ack.writeUTF("ack");                                                            //A method of DataOutputStream to write string data into the stream
                            ack.close();
                        }
                        clientSocket.close();
                        Log.e("TopOfQueueBefore", holdBackQueue.peek().msg + holdBackQueue.peek().seq_no + holdBackQueue.peek().accepted + holdBackQueue.peek().from + Integer.toString(failedPort));

                        //Deleting the messages in the Hold Back Queue which are sent from the failed avd.
                        Iterator<Message> itr = holdBackQueue.iterator();
                        while(itr.hasNext()){
                            Message msg = itr.next();
                            if(Integer.parseInt(msg.from)==failedPort){
                                Log.e("Deleting : ", "Removing " + msg.msg + msg.seq_no);
                                holdBackQueue.remove(msg);
                            }
                        }
                        if (failedPort != 0) {
                            while (!holdBackQueue.isEmpty() && (failedPort == Float.parseFloat(holdBackQueue.peek().from))) {
                                Log.e("Delete Before", "Not publishing before");
                                holdBackQueue.poll();
                            }
                        }

                        //Publishes only if the top of the HoldBackQueue is an Accepted Seq no. and if the message is not sent from the failed avd.
                        while (!holdBackQueue.isEmpty() && holdBackQueue.peek().accepted.equals(Boolean.TRUE)) {
                            Log.e("TopOfQueueA", holdBackQueue.peek().msg + holdBackQueue.peek().seq_no + holdBackQueue.peek().accepted + holdBackQueue.peek().from);

                            Message msgDel = holdBackQueue.poll();
                            Log.e("TopOfQueueB", msgDel.msg + msgDel.seq_no + msgDel.accepted + msgDel.from);

                            if (failedPort != 0) {
                                Log.e("removing", "Failed Message " + msgDel.msg + msgDel.seq_no);
                                //Float tempPort = msgDel.seq_no - (int)msgDel.seq_no;
                                //Float fPort = Float.parseFloat("0."+failedPort);
                                Log.e("Compare", msgDel.propFrom + " " + failedPort + " " + msgDel.from);
                                if (failedPort == Float.parseFloat(msgDel.from))
                                    Log.e("Delete", "Not adding to publish" + msgDel.msg + msgDel.from);
                                else {
                                    Log.e("Publishing", "Publishing " + msgDel.msg + msgDel.from);
                                    publishProgress(msgDel.msg);
                                }
                            } else {
                                Log.e("Publishing noerror", "Publishing " + msgDel.msg + msgDel.from);
                                publishProgress(msgDel.msg);
                            }
                        }
                        //clientSocket.close();
                    }
                }

            } catch (StreamCorruptedException e ) {
                Log.e(TAG, "ServerTask StreamCorruptedException");
            } catch(SocketTimeoutException e) {
                Log.e(TAG, "ServerTask SocketTimeoutException");
            }  catch (IOException e) {
                e.printStackTrace();
            } /* catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            return null;
        }

        protected void onProgressUpdate(String...strings) {

            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");

            sequence_no += 1;

            //https://developer.android.com/reference/android/content/ContentValues                 This is used to store the sequence number of the message and message.
            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put(FeedReaderContract.FeedEntry.KEY_NAME, String.valueOf(sequence_no));   //Sequence No. is stored.
            keyValueToInsert.put(FeedReaderContract.FeedEntry.VALUE_NAME, strReceived);                 //Value is stored
            Uri newUri = getContentResolver().insert(mUri, keyValueToInsert);                           //Used to insert the ContentValues into the SQLite using ContentResolver.

        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {


            String msgToSend = msgs[0];
            Message mesg = new Message();

            mesg.msg = msgToSend;
            mesg.seq_no = (float) 0.0;
            mesg.accepted = Boolean.FALSE;
            if (myPort.equals("1112"))
                mesg.from = myPort +"0";
            else
                mesg.from = myPort;
            mesg.propFrom = 0;

            //Store the mesg to send seperated by '-'
            String messageToSend = mesg.msg + "-" + Float.toString(mesg.seq_no) + "-" + Boolean.toString(mesg.accepted) + "-" +mesg.from + "-" + mesg.propFrom;
            Log.e("Testing", messageToSend);

            //Iterating through all the avds to send the message
            for (String remotePort : REMOTE_PORTS) {                                                          //Send to each AVD separately
                try {
                    if (!remotePort.equals(failedPort)) {                                                   //Sending the message only for alive avds
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
                                Integer.parseInt(remotePort));
                        //Setting a Socket Timeout to catch Exception
                        socket.setSoTimeout(2000);
                        //https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html#getOutputStream()  Used to create an outputstream on the client socket to send the message to the server.
                        DataOutputStream output_message = new DataOutputStream(socket.getOutputStream());
                        //System.out.println(mesg);

                        //Sending the message to the server.
                        output_message.writeUTF(messageToSend);

                        //https://docs.oracle.com/javase/7/docs/api/java/io/DataInputStream.html            Used to read the acknowledgement data from the server. This allows 2 way communication between the 2 devices.

                        //Reading the proposed seq no. for the message
                        DataInputStream ackStream = new DataInputStream(socket.getInputStream());
                        String pMesg = ackStream.readUTF();
                        String[] propMesg = pMesg.split("-");
                        //String[] mTemp = pMesg.split(".");

                        //Adding the seq no. to a Priority Queue which sorts by Highest value.
                        seqProposedQ.add(Float.parseFloat(propMesg[0]));
                        propPort.put(Float.parseFloat(propMesg[0]), Integer.parseInt(propMesg[1]));
                        //System.out.println(prop_Mesg);
                        ackStream.close();
                        output_message.close();
                        socket.close();
                    }
                    //Catching exceptiions when the avd fails.
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask Requesting Proposal UnknownHostException");
                } catch (SocketException e ) {
                    Log.e(TAG, "ClientTask Requesting Proposal SocketException");
                    Log.e("1 remote port is ", remotePort);
                    //Setting the failed Port number
                    failedPort = Integer.parseInt(remotePort);

                    //Multicasting the failed avd to all the Alive avds.
                    for (String tempPort : REMOTE_PORTS) {
                        if (!tempPort.equals(remotePort)) {
                            try {
                                Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
                                        Integer.parseInt(tempPort));
                                DataOutputStream output_message = new DataOutputStream(sock.getOutputStream());
                                //System.out.println(mesg);

                                output_message.writeUTF(remotePort);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }

                        }
                    }
                } catch (StreamCorruptedException e ) {
                    Log.e(TAG, "ClientTask Requesting Proposal StreamCorruptedException");
                } catch(SocketTimeoutException e) {
                    Log.e(TAG, "Requesting Proposal SocketTimeoutException");

                } catch (IOException e) {
                    Log.e(TAG, "ClientTask Requesting Proposal socket IOException");
                    Log.e("2 remote port is ", remotePort);
                    //Setting the failed Port number
                    failedPort = Integer.parseInt(remotePort);
                    //Multicasting the failed avd to all the Alive avds.
                    for (String tempPort : REMOTE_PORTS) {
                        if (!tempPort.equals(remotePort)) {
                            try {
                                Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
                                        Integer.parseInt(tempPort));
                                DataOutputStream output_message = new DataOutputStream(sock.getOutputStream());
                                //System.out.println(mesg);

                                output_message.writeUTF(remotePort);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }

                        }
                    }
                }
            }
            //Polls the seq no with the highest value as this is a Priority Queue.
            mesg.seq_no = seqProposedQ.poll();
            mesg.propFrom = propPort.get(mesg.seq_no);
            //Changes the message as an Accepted Message
            mesg.accepted = Boolean.TRUE;
            //System.out.print(mesg.msg + mesg.seq_no.toString());
            //Clearing the Queues for new values for the next message.
            seqProposedQ.clear();
            propPort.clear();
            messageToSend = mesg.msg + "-" + Float.toString(mesg.seq_no) + "-" + Boolean.toString(mesg.accepted) + "-" +mesg.from + "-" + mesg.propFrom;
            for (String remotePort : REMOTE_PORTS){                                                        //Send to each AVD separately
                try {
                    if(!remotePort.equals(failedPort)) {                                                   //Sending the message only for alive avds
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
                                Integer.parseInt(remotePort));
                        //Setting Socket Timeout
                        socket.setSoTimeout(2000);
                        //https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html#getOutputStream()  Used to create an outputstream on the client socket to send the message to the server.
                        //Sending the accepted seq no. to the alive avds.
                        DataOutputStream output_message = new DataOutputStream(socket.getOutputStream());
                        output_message.writeUTF(messageToSend);

                        //Receiving acknowledgement for Full Duplex and catch Failure of avds
                        DataInputStream ackStream = new DataInputStream(socket.getInputStream());
                        String aMesg = ackStream.readUTF();
                        Log.e("ack", aMesg);
                        output_message.close();
                        ackStream.close();
                        socket.close();
                    }
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask Sending Accepted UnknownHostException");
                } catch (SocketException e ) {
                    Log.e(TAG, "ClientTask Sending Accepted SocketException");
                    Log.e("3 remote port is ", remotePort);
                    //Setting the failed Port number
                    failedPort = Integer.parseInt(remotePort);
                    //Multicasting the failed avd to all the Alive avds.
                    for (String tempPort : REMOTE_PORTS) {
                        if (!tempPort.equals(remotePort)) {
                            try {
                                Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
                                        Integer.parseInt(tempPort));
                                DataOutputStream output_message = new DataOutputStream(sock.getOutputStream());
                                output_message.writeUTF(remotePort);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }

                        }
                    }

                } catch (StreamCorruptedException e ) {
                    Log.e(TAG, "ClientTask Sending Accepted StreamCorruptedException");
                } catch(SocketTimeoutException e) {
                    Log.e(TAG, "Sending Accepted SocketTimeoutException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask Sending Accepted socket IOException");
                    Log.e("4 remote port is ", remotePort);
                    //Setting the failed Port number
                    failedPort = Integer.parseInt(remotePort);
                    //Multicasting the failed avd to all the Alive avds.
                    for (String tempPort : REMOTE_PORTS) {
                        if (!tempPort.equals(remotePort)) {
                            try {
                                Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
                                        Integer.parseInt(tempPort));
                                DataOutputStream output_message = new DataOutputStream(sock.getOutputStream());
                                output_message.writeUTF(remotePort);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }

                        }
                    }
                }
            }
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}

/* References :
https://stackoverflow.com/questions/3019376/shortcut-for-adding-to-list-in-a-hashmap
https://cse.buffalo.edu/~stevko/courses/cse486/spring13/lectures/12-multicast2.pdf
https://studylib.net/doc/7830646/isis-algorithm-for-total-ordering-of-messages
https://www.geeksforgeeks.org/float-compare-method-in-java-with-examples/
 */