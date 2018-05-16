package com.rugd.studios.themediator;
/**
 * Created by psybr on 2016/03/12.
 */
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;


public class BoSAgent
{
    //private Socket client						= null;
    //private ServerSocket server					= null;
    private DatagramSocket server				= null;
    private Item item							= null;
    private int price							= 0;
    private String[] funcs						= {"","sin","cos","tan","ln","e","log","sec","cosec","cot"};
    private String destIP						= "";
    private boolean connected					= true;
    private ArrayList<Message> messages			= null;
    private Context context                     = null;

    public BoSAgent(Item item, String destIP, int price,Context context)
    {
        this.item = item;
        this.price = price;
        this.destIP = destIP;
        this.context = context;

        try
        {
            server = new DatagramSocket(4243);
            //server = new ServerSocket(4243);
            //System.out.println("Local server started.");
        }
        catch (SocketException e)
        {
            Toast.makeText(context,"Couldn't start client socket [make sure you allowed this program through your firewall]: " + e.getMessage(),Toast.LENGTH_LONG).show();
            System.exit(-1);
        }
        //Attempt to read messages from disc
        messages = WritersAndReaders.loadMessages("msgData.dat");
        //Instantiate messages ArrayList for fallback
        if(messages==null)
            messages = new ArrayList<>();
    }

    //Sends the item
    public void transfer() throws IOException
    {
        if(item != null)
        {

            //System.out.println("Item stat: " + item.getRecStat());
            //if it has not been sent or has been sent but not delivered.
            if(item.getRecStat()== -1 || item.getRecStat()== 0)
            {
                //System.out.println("Sending");
                int r = new Random().nextInt((9 - 1) + 1)+1;//1-9
                String eq = String.valueOf(item.getQuantity()* r * 2);// + "" + r;
                sendMessage("COMPUTE\t" + funcs[r] + "(" + eq + ")" + "\t" + item.getID() + "\t" + item.getDetails());
            }
            else
            {
                System.out.println("Item is in good standing..");
            }
        }
        else
        {
            System.out.println("Empty stack.");
        }
    }

    public DatagramSocket getLocalServer()
    {
        return server;
    }

    public void sendMessage(String msg) throws IOException
    {
        try
        {
            DatagramPacket outbound = new DatagramPacket(msg.getBytes(), msg.getBytes().length,InetAddress.getByName(destIP),4242);
            server.send(outbound);
			/*client = new Socket(InetAddress.getByName(destIp),4242);//connect to server
			//DatagramPacket outbound = new DatagramPacket(msg.getBytes(), msg.getBytes().length,InetAddress.getByName("104.236.91.104"),4242);
			PrintWriter out = new PrintWriter(client.getOutputStream());
			out.println(msg);
			out.flush();
			out.close();
			client.close();
			connected = true;*/
        }
        catch (UnknownHostException e)
        {
            connected = false;
            Toast.makeText(context,"Could not send request:" + e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public boolean isConnected()
    {
        return connected;
    }

    public void startListener() throws IOException
    {
        byte[] buffer = new byte[4096];
        DatagramPacket inbound = new DatagramPacket(buffer,buffer.length);
        System.out.println("Local listener is running.");
        while(true)
        {
            server.receive(inbound);
            String response = new String(inbound.getData(),0,inbound.getLength());
            StringTokenizer tokenizer = new StringTokenizer(response,"\t");
            String cmd = tokenizer.nextToken();
            String msg = "",stat = "";
            int priority = Message.NORMAL;
            switch(cmd)
            {
                case "MSG":
                    msg = tokenizer.nextToken();
                    priority = Integer.valueOf(tokenizer.nextToken());
                    //String msgID = tokenizer.nextToken();
                    messages.add(new Message(0, msg, "MSG", "ME",priority));
                    //
                    /*AlertDialog.Builder message = new AlertDialog.Builder(context);
                    message.setTitle("Message from The Psyentists");
                    message.setMessage(msg);
                    message.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    message.show();*/
                    //
                    Toast.makeText(context,msg,Toast.LENGTH_LONG).show();
                    WritersAndReaders.saveMessages(messages, "msgData.dat");
                    break;
                case "STAT":
                    stat = tokenizer.nextToken();
                    item.setRecStat(Integer.valueOf(stat));
                    WritersAndReaders.saveItem(item, "data.dat");
                    //JOptionPane.showMessageDialog(null, "ID Mismatch - delete data.dat and start the program again and make your order again","ID Mismatch!",JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "NOTIF":
                    msg = tokenizer.nextToken();
                    priority = Integer.valueOf(tokenizer.nextToken());
                    messages.add(new Message(0, msg, "NOTIF", "ME",priority));
                    WritersAndReaders.saveMessages(messages, "msgData.dat");
                    break;
                case "PRICE":
                    String prc = tokenizer.nextToken();
                    int p = Integer.valueOf(prc);
                    price = p;
                    break;
                default:
                    System.err.println("UNKNOW COMMAND.");
                    break;
            }
        }
    }

    public void setPrice(int price)
    {
        this.price = price;
    }
}
