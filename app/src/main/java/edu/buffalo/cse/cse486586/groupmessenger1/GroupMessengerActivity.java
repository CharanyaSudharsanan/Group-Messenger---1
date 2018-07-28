package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
//import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Context;
//import android.nfc.Tag;
//import android.os.AsyncTask;
//import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.ServerSocket;
import android.widget.Button;
import android.util.Log;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] ports = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    public int counter = -1;
    //public String myPort;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));


        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
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

        final EditText editText = (EditText) findViewById(R.id.editText1);


        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString().trim() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView tv = (TextView) findViewById(R.id.textView1);
                tv.append("\t" + msg); // This is one way to display a string.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }



    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
            while (true) {
                try {


                    Socket sock = serverSocket.accept();
                    InputStreamReader inputstreamreader = new InputStreamReader(sock.getInputStream());
                    BufferedReader buf = new BufferedReader(inputstreamreader);
                    String message = buf.readLine();
                    //sending an ack to client to close the socket!-----------------------------
                    PrintWriter bufOut = new PrintWriter(sock.getOutputStream(),true);
                    bufOut.write("ack");
                    //--------------------------------------------------------------------------
                    Log.d("Server", "Message received: " + message);

                    ContentValues cv = new ContentValues();
                    counter++;
                    cv.put("key", Integer.toString(counter));
                    cv.put("value", message);
                    getContentResolver().insert(mUri, cv);
                    buf.close();
                    inputstreamreader.close();
                    sock.close();
                    publishProgress(message);
                    Log.d("Server", "Message complete");


                } catch (Exception e) {
                    Log.e(TAG, "No Connection");
                    e.printStackTrace();
                }

              /*
             * TODO: Fill in your server code that receives messages and passes them

             * to onProgressUpdate().
             */

            }
        }


        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            //if (strings.length ==0 || strings[0] == null)
            //    return;
            String strReceived = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\t\n");

            return;


        }
    }



    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
                    for (int i = 0; i < ports.length; i++) {
                        try {

                            String remotePort = ports[i];
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));
                            String msgToSend = msgs[0];
                            Log.d("CLIENT", "message to send: " + msgToSend);
                            PrintWriter bufOut = new PrintWriter(socket.getOutputStream(),true);
                            //DataOutputStream bufOut = new DataOutputStream(socket.getOutputStream());
                            bufOut.write(msgToSend);
                            bufOut.flush();
                            //bufOut.close();

                            //socket.close();


                            //Get an acknowledgement from server that the message has reached!
                            InputStreamReader inputstreamreader = new InputStreamReader(socket.getInputStream());
                            BufferedReader buf = new BufferedReader(inputstreamreader);
                            String message = buf.readLine();
                            if(message == "ack")
                            {
                                socket.close();
                            }

                        } catch (UnknownHostException e) {
                            Log.e(TAG, "ClientTask UnknownHostException");
                        } catch (IOException e) {
                            Log.e(TAG, "ClientTask socket IOException");
                        }


                    }
            return null;

        }
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}