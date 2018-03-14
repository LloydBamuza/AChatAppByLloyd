package com.example.lloyd.achatappbylloyd;

import android.Manifest;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView txtConnStat;
    Button btnConnect, btnSend;
    Context APP_CONTEXT;
    static Boolean isConToServer = false, isConToClient = false;
    EditText edtIpAd;
    static Socket socketIn;
    static List<String> allMessages = new ArrayList<String>();
    ListView lstV;
    static  ArrayAdapter arrayAdapter;
    static Socket clientSocket;
    EditText edtMsg;
    Button test;
    static String deviceIp = "",friendIp = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //Get view references
        txtConnStat = (TextView) findViewById(R.id.txtConnectionStatus);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnSend = (Button) findViewById(R.id.btnSend);
        edtIpAd = (EditText) findViewById(R.id.edtIp);
        lstV = (ListView) findViewById(R.id.lstMsgs);
        edtMsg = (EditText) findViewById(R.id.edtMsg);


        //capture main application context
        APP_CONTEXT = getApplicationContext();

        //Request permissions
        ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.INTERNET},0);

        //display current connection status and device IP
        if((isConToClient == false) || (isConToServer == false))
        {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            deviceIp = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
            txtConnStat.setText("Enter: "+deviceIp+ " on your friend's phone");

        }

        //Start server and listen for connection
        startServer();

        //set up list view to display messages
        arrayAdapter = new ArrayAdapter(APP_CONTEXT,android.R.layout.simple_list_item_1,allMessages);
        lstV.setAdapter(arrayAdapter);

        btnConnect.setOnClickListener(e->{
            String friendIp = edtIpAd.getText().toString();

            if(friendIp != null && friendIp != "" && friendIp.length() > 4 )
            {
                connectToFriend(friendIp);
            }
            else
                Toast.makeText(this,"Invalid ip",Toast.LENGTH_LONG).show();
        });



    btnSend.setOnClickListener(p->{
            if(!isConToServer && !isConToClient) return;
            OutputStream out = null;
            try {
                if(isConToClient) {
                    out = clientSocket.getOutputStream();
                }
                else if(isConToServer)
                {
                    out = socketIn.getOutputStream();
                }
                else{
                    Toast.makeText(this,"Not connected",Toast.LENGTH_LONG).show();
                    return;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            writeMessage(edtMsg.getText().toString(), out);
            edtMsg.setText("");

            arrayAdapter.notifyDataSetChanged();
            lstV.setAdapter(arrayAdapter);

    });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isConToClient = false;
        isConToServer = false;
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        deviceIp = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        txtConnStat.setText("Enter: "+deviceIp+ " on your friend's phone");
    }

    public void readMsg(InputStream inputStream)
    {



        try
        {
            //read message in a seperate thread
            new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        int bytesRead;
                        String message = "";
                        byte[] buffer = new byte[3000];
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(3000);
                        Boolean stop = false;

                        bytesRead = inputStream.read(buffer);

                        if(bytesRead >= 1)
                        {

                            byteArrayOutputStream.write(buffer,0,bytesRead);
                            message = byteArrayOutputStream.toString();

                            String finalMessage = message;
                            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  Toast.makeText(APP_CONTEXT,"New message received!",Toast.LENGTH_SHORT).show();

                              }
                          });

                           // byteArrayOutputStream.flush();
                        }

                        allMessages.add("Friend: "+ message);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                arrayAdapter.notifyDataSetChanged();
                                lstV.setAdapter(arrayAdapter);
                                lstV.refreshDrawableState();
                            }
                        });

                    }
                    catch (Exception exc)
                    {
                        exc.printStackTrace();
                    }
                }
            }.start();
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
        }
    }

    void writeMessage(String theMsg,OutputStream outputStream)
    {
        //read message in new thread
        new Thread()
        {
            @Override
            public void run()
            {

                try {
                    outputStream.write(theMsg.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                allMessages.add("You: "+theMsg);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        arrayAdapter.notifyDataSetChanged();
                        lstV.setAdapter(arrayAdapter);
                        lstV.refreshDrawableState();
                    }
                });
            }
        }.start();
    }

    void checkForMessages(InputStream input)
    {
        new Thread()
        {

                @Override
                public void run ()
                {
                    while(true)
                    {
                    readMsg(input);
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(APP_CONTEXT,"Checking for msgs", Toast.LENGTH_SHORT).show();
                            }
                        });
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
         }.start();
    }




    void connectToFriend(String ip)
    {
        final InetAddress[] inetAddress = new InetAddress[1];

        new Thread(new Runnable() {
            @Override
            public void run() {
                //Connect to friend
                try
                {
                    inetAddress[0] = InetAddress.getByName(ip);

                    socketIn =new Socket(inetAddress[0],12345);

                    //update connection status
                    isConToServer = true;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

            }
        }).start();







            if(isConToServer) {
                //show connection success message
                runOnUiThread(() ->
                {
                    Toast.makeText(APP_CONTEXT, "Successfully connected to friend.", Toast.LENGTH_LONG).show();
                    txtConnStat.setText("Connected to: " + inetAddress[0].getHostAddress() + "\n" + "Your IP: " + deviceIp);
                });


                try {
                    checkForMessages(socketIn.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }





    void startServer()
    {
         final ServerSocket serverSocket;

        try
        {
            //create server socket by opening port
            serverSocket = new ServerSocket(12345);

            //listen for client connection attempts on port in a new thread
            new Thread()
            {
                @Override
                public void run()
                {
                        clientSocket = null;
                        try {

                            clientSocket = serverSocket.accept();

                            //update connection status
                            isConToClient = true;

                            //start thread to check for new messages
                            checkForMessages(clientSocket.getInputStream());

                            //show connection success message
                            runOnUiThread(() ->
                            {
                                txtConnStat.setText("Connected to: " + clientSocket.getRemoteSocketAddress() + "\n" + "Your IP: " + deviceIp);

                                Toast.makeText(APP_CONTEXT, "Friend has connected", Toast.LENGTH_LONG).show();
                            });
                        } catch (Exception exc) {
                            exc.printStackTrace();
                        }

                }
            }.start();
        }
        catch(Exception exc)
        {
            exc.printStackTrace();
        }

    }
}
