package com.rugd.studios.themediator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Random;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity
{
    private TextView txtNotif                               = null;
    private TextView txtEq                                  = null;
    private Button iconShift                                = null;
    private Button iconInput                                = null;
    private Button iconInternet                             = null;
    private Button iconStatus                               = null;

    public static String version                            = "2.5";
    private ArrayList<ButtonData> buttons                   = new ArrayList<ButtonData>();
    private String equation									= "";
    private boolean bShift,bM, bE, bPow						= false;
    private boolean bInternet								= false;
    private boolean bOrdering								= false;
    private boolean menuInitd								= false;
    private final static String SRV_IP		                = "104.236.46.5";
    private String text                                     = "";
    private String details                                  = "";
    private int amnt                                        = 0;
    private int price										= 10;

    private static Socket conn								= null;
    private Timer calcTimer									= null;
    private Item item										= null;
    private BoSAgent agent									= null;
    private Thread tListen									= null;
    private Timer tSend										= null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Configuration config = getResources().getConfiguration();
        /*if (config.smallestScreenWidthDp >= 600)
        {
            setContentView(R.layout.activity_main);
        } else {
            //setContentView(R.layout.main_activity);
        }*/

        setContentView(R.layout.activity_main);

        txtNotif = (TextView)findViewById(R.id.txtNotif);
        txtEq = (TextView)findViewById(R.id.txtEquation);
        iconShift = (Button)findViewById(R.id.iconShift);
        iconInput = (Button)findViewById(R.id.iconInput);
        iconInternet = (Button)findViewById(R.id.iconInternet);
        iconStatus = (Button)findViewById(R.id.iconStatus);

        //
        //Attemp to read in locally saved data
        item = WritersAndReaders.loadItem("data.dat");

        //Instantaite for fallback
        if(item==null)
            item = new Item(0, getID(),"</>");

        //Instantiate agent
        agent = new BoSAgent(item,SRV_IP,price,this);

        //System.out.println("Checkpoint");

        //First time internet check
        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Looper.prepare();
                bInternet = getConnectionStatus();
            }
        });
        t.start();

        calcTimer = new Timer();
        calcTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                //txtEq = (TextView)findViewById(R.id.txtEquation);
                //Button icnInput = (Button)findViewById(R.id.iconInput);
                //Button icnShift = (Button)findViewById(R.id.iconShift);

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if(bShift)
                            if(iconShift!=null)iconShift.setBackgroundResource(R.drawable.shifting);
                        else
                            if(iconShift!=null)iconShift.setBackgroundResource(R.drawable.shift);

                        //
                        //Check combination status
                        if(bOrdering)
                        {

                            if(!menuInitd)
                            {
                                Thread t = new Thread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        bInternet = getConnectionStatus();
                                    }
                                });
                                menuInitd = true;
                            }

                            //Check internet status
                            if(bInternet)
                            {
                                if(iconInternet!=null)iconInternet.setBackgroundResource(R.drawable.internet);
                                if(iconInput!=null)iconInput.setBackgroundResource(R.drawable.input);//tell user to input order
                                //txtNotif.setText("<Empty>");
                            }
                            else
                            {
                                if(iconInput!=null)iconInput.setBackgroundResource(R.drawable.no_input);//no internet - input would be kinda useless - for now
                                if(iconInternet!=null)iconInternet.setBackgroundResource(R.drawable.no_internet);
                            }

                            //Set status
                            switch(item.getRecStat())
                            {
                                case 0:
                                    if(iconStatus!=null)iconStatus.setBackgroundResource(R.drawable.exclaim);
                                    break;
                                case 1:
                                    if(iconStatus!=null)iconStatus.setBackgroundResource(R.drawable.pending);
                                    break;
                                case 2:
                                    if(iconStatus!=null)iconStatus.setBackgroundResource(R.drawable.ready);
                                    break;
                                case 3:
                                    if(iconStatus!=null)iconStatus.setBackgroundResource(R.drawable.cancelled);
                                    break;
                                default:
                                    if(iconStatus!=null)iconStatus.setBackgroundResource(R.drawable.question);
                                    break;
                            }
                        }
                        else
                        {
                            //Reset icons
                            if(txtNotif!=null)txtNotif.setText("");
                            if(iconInput!=null)iconInput.setBackgroundResource(R.drawable.rad);
                            if(iconInternet!=null)iconInternet.setBackgroundResource(R.color.black);
                            if(iconStatus!=null)iconStatus.setBackgroundResource(R.color.black);
                        }
                    }
                });
            }
        },0,1000);

        Timer ping = new Timer();
        ping.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                if(bOrdering)
                {
                    //Check internet connectivity
                    bInternet = getConnectionStatus();

                    if(agent!=null)
                    {
                        //Load messages
                        ArrayList<Message> messages = null;
                        messages = agent.getMessages();
                        final ArrayList<Message> msgs = messages;

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                txtNotif.setText("");

                                if (msgs != null)
                                {
                                    String important = "";
                                    int highest = 10;//Shouldn't go higher than 10
                                    for (Message m : msgs)
                                    {
                                        if(highest == 10)
                                            highest = m.getPriority();

                                        if(m.getPriority()>=highest)
                                        {
                                            highest = m.getPriority();
                                            important = m.getMsg();
                                        }
                                    }
                                    if(txtNotif!=null)txtNotif.setText(">>" + important);
                                    if(txtNotif!=null)if(important.isEmpty())txtNotif.setText("<Empty>");
                                }
                            }
                        });

                        //Send a Ping
                        try
                        {
                            if(item!=null)
                            {
                                agent.sendMessage("PING\t" + item.getID() + "\t" + version);
                                System.out.println("PINGED");
                            }
                            else
                                System.err.println("Item instance is null.");
                        }
                        catch (IOException e1)
                        {
                            System.err.println("Could not ping server: " + e1.getMessage());
                        }

                        //set price
                        agent.setPrice(price);
                    }
                    else
                    {
                        System.err.println("BoSAgent instance is null");
                    }
                }
            }
        },0,10000);

        //Start listener thread
        if(tListen==null)
        {
            tListen = new Thread(new Runnable()
            {

                @Override
                public void run()
                {
                    try
                    {
                        Looper.prepare();
                        agent.startListener();
                    }
                    catch (IOException e)
                    {
                        AlertDialog.Builder err = new AlertDialog.Builder(getApplicationContext());
                        err.setTitle("Error");
                        err.setMessage(e.getMessage());
                        err.setPositiveButton("Okay", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {

                            }
                        });
                        err.show();
                    }
                }
            });
            tListen.start();
        }
    }

    public void notifClick(View v)
    {
        String s = txtNotif.getText().toString();
        if(!s.contains("<Empty>") && !s.equals(""))
        {
            String msg = s.substring(s.lastIndexOf(">")+1,s.length());
            if(agent!=null)
            {
                try
                {
                    //Iterator<Message> i = agent.getMessages().iterator();
                    for (Message m : agent.getMessages())
                    {
                        //i.next();
                        if (m.getMsg().equals(msg))
                        {
                            //i.remove();
                            m.setPriority(m.getPriority()-1);
                            WritersAndReaders.saveMessages(agent.getMessages(), "msgData.dat");
                            Toast.makeText(this,"Message dismissed",Toast.LENGTH_LONG).show();
                        }
                    }
                }
                catch (ConcurrentModificationException e)
                {
                    Log.d("CME ", e.getMessage());
                }
            }
        }
    }

    private void falsifyCombo()
    {
        if(bM || bE || bPow && !bOrdering)
        {
            bM = false;
            bE = false;
            bPow = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        AlertDialog.Builder msg = new AlertDialog.Builder(this);
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.about:
                msg.setTitle("About: The Mediator");
                msg.setMessage("Developed by TheMadPsyentist_ at Rugd Labs\nI hope you dig the app...\n" +
                        "Not much more information about it really.\n\nDeuces XD");
                msg.setNeutralButton("Awesome", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                msg.show();
                return true;
            case R.id.legal:
                msg.setTitle("Legal");
                msg.setMessage("Luuulz XD,\nNothin much to say about this... \nThis app may be taken down(well the server anyway) by me at any time.");
                msg.setNeutralButton("Cool", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                msg.show();
                return true;
            case R.id.instructions:
                msg.setTitle("Instructions");
                if(bOrdering)
                {
                    String s =
                            "Make sure you've typed in the command[to activate merchant mode] first\n" +
                                    "1] Type in the amount of items you want using the calculator keypad.\n" +
                                    "2] Click the '=' button to send your request\n" +
                                    "3] Type in your address & your phone number in the popup dialog\n" +
                                    "4] Wait to be lifted - We'll call you on the entered number when we deliver the item[s].\n\n" +
                                    "The icon panel is a great way to keep track of the status of your order.\n" +
                                    "Click the different icons on the notification panel to see the messages they have for you.\n" +
                                    "Tap on the notification message to dismiss it\n" +
                                    "There will be notifications[text] on the application notification panel sometimes.\n" +
                                    "Tap on the notification to scroll through all your notifications.\n" +
                                    "The 'Reset' option on the menu disables merchant mode.";
                    msg.setMessage(s);
                }
                else
                {
                    msg.setMessage("Just use it!");
                }
                msg.setNeutralButton("Ok, Go away", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                msg.show();
                return true;
            case R.id.reset:
                bOrdering = false;
                bShift = false;
                bM = false;
                bE = false;
                bPow = false;
                Toast.makeText(this,"Variables have been reset",Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Computes a given equation - use with caution - operator precedence!
     * @param equ Equation to be computed.
     * @param op Operator to be searched for.
     * @return simplified equation without the supplied operator.
     */
    private String compute(String equ,String op)
    {
        String equation = equ;
        Pattern mdPattern = Pattern.compile("(\\d+([.]\\d+)*)((["+op+"]))(\\d+([.]\\d+)*)");
        Matcher matcher		= mdPattern.matcher(equation);
        while(matcher.find())
        {
            String[] arr = null;
            double ans = 0;
            String eq = matcher.group(0);//get form x*y
            if(eq.contains(op))
            {
                arr = eq.split("\\"+op);//make arr
                if(op.equals("*"))
                    ans = Double.valueOf(arr[0])*Double.valueOf(arr[1]);//compute
                if(op.equals("/"))
                    ans = Double.valueOf(arr[0])/Double.valueOf(arr[1]);//compute
                if(op.equals("+"))
                    ans = Double.valueOf(arr[0])+Double.valueOf(arr[1]);//compute
                if(op.equals("-"))
                    ans = Double.valueOf(arr[0])-Double.valueOf(arr[1]);//compute
            }

            equation = matcher.replaceFirst(String.valueOf(ans));//replace in equation
            matcher = mdPattern.matcher(equation);//look for more matches
        }
        return equation;
    }

    private String getID()
    {
        //excludes \ & "
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890!@#$%^&*()_+{}|:<>?/.,';][=-`~";
        String id = "";
        int max = new Random().nextInt((16 - 12) + 1)+12;
        for(int i=0;i<16;i++)
        {
            int r = new Random().nextInt((chars.length()-1 - 0) + 1)+0;
            id+=chars.charAt(r);
        }
        return id;
    }

    private static boolean getConnectionStatus()
    {
        try
        {
            //System.out.println("Sending req to google.com");
            conn = new Socket(InetAddress.getByName("google.com"),80);//173.194.45.52
            PrintWriter out = new PrintWriter(conn.getOutputStream());
            String req = 	"GET / HTTP/1.1\r\n";
            //"User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64)\r\n" +
            //" AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 Safari/537.36";
            out.print(req);
            req = "Host: google.com\r\n\r\n";
            out.print(req);
            out.flush();
            //System.out.println("Sent Request");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = in.readLine();
            //System.out.println("Response: "  + line);
            in.close();
            if(line.contains("302 Found"))
                return true;
            else
                return false;
        }
        catch (UnknownHostException e)
        {
            System.err.println("Cannot connect to the internet[UHE]: "  + e.getMessage());
            return false;
        }
        catch (IOException e)
        {
            System.err.println("Cannot connect to the internet[IOE]: "  + e.getMessage());
            return false;
        }
    }

    public void shiftIconClick(View v)
    {
        AlertDialog.Builder msg = new AlertDialog.Builder(this);
        msg.setTitle("Shift");
        msg.setMessage("Just shows the status of the shift button");
        msg.setNeutralButton("Cool", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        msg.show();
    }

    public void inputIconClick(View v)
    {
        AlertDialog.Builder msg = new AlertDialog.Builder(this);
        if(bOrdering)
        {
            msg.setTitle("Input");
            msg.setMessage("If it says 'Input', you should input the amount of items you want followed by '='.\n" +
                    "If it says 'Rad', it doesn't really mean anything, just means it's using\n" +
                    " Radians as opposed to degrees.");
        }
        else
            msg.setMessage("It just means it's using\n"+
                "Radians as opposed to degrees or vice versa.");

        msg.setNeutralButton("Cool", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        msg.show();
    }

    public void internetIconClick(View v)
    {
        AlertDialog.Builder msg = new AlertDialog.Builder(this);
        msg.setTitle("Internet");
        if(bOrdering)
        {
            msg.setMessage("Tells you whether this app has access to the internet or not");
            msg.setNeutralButton("Cool", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            msg.show();
        }
    }

    public void statusIconClick(View v)
    {
        AlertDialog.Builder msg = new AlertDialog.Builder(this);
        msg.setTitle("?/!/Pending/Ready");
        if(bOrdering)
        {
            msg.setMessage("If it shows '?', it means the message has NOT been sent\n" +
                    "Or that you have no previous orders.\n" +
                    "If it shows '!', it means that the message has been sent and now awaiting delivery approval.\n" +
                    "If it shows 'Pending', it means that we have received your order and we are now processing it.\n" +
                    "If it shows 'Ready', it means that your order is ready and will be delivered soon.\n" +
                    "If it shows 'Cancelled', although rare - but nonetheless, it means your order has been cancelled for some reason.\n" +
                    "Or there was a server error, but try again.");
            msg.setNeutralButton("Cool", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            msg.show();
        }
    }

    public void shiftClick(View v)
    {
        focusAnimations(v, R.drawable.btnshift_focused, R.drawable.btnshift);
        bShift = bShift?false:true;
    }

    public void mClick(View v)
    {
        focusAnimations(v,R.drawable.btnm_focused,R.drawable.btnm);
        //Calculate
        equation = txtEq.getText().toString();
        equation = compute(equation,"*");
        equation = compute(equation,"/");
        equation = compute(equation,"+");
        equation = compute(equation,"-");

        txtEq.setText("");
        txtEq.setText(equation);

        bM = bM?false:true;
    }

    public void eClick(View v)
    {
        focusAnimations(v, R.drawable.btne_focused, R.drawable.btne);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");

        txtEq.append("e^");
        bE = bE?false:true;
    }

    public void powerClick(View v)
    {
        focusAnimations(v,R.drawable.btnpower_focused,R.drawable.btnpower);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");

        txtEq.append("^");
        bPow = bPow?false:true;

        if(bShift && bM && bE && bPow)
        {
            bOrdering = true;
            AlertDialog.Builder greet = new AlertDialog.Builder(this);
            greet.setTitle("Welcome");
            greet.setMessage("Welcome to The Mediator\nClick on 'instructions' under the options menu to see more info.\n" +
                    "\n'The mediator between the head and the hands must truly be the heart' :P xD :D");
            greet.setNeutralButton("Okay, Awesome", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            greet.show();

            //System.out.println("Combination detected!");
        }
    }

    public void plusClick(View v)
    {
        focusAnimations(v,R.drawable.btnplus_focused,R.drawable.btnplus);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("+");
        falsifyCombo();
    }

    public void minusClick(View v)
    {
        focusAnimations(v,R.drawable.btnminus_focused,R.drawable.btnminus);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("-");
        falsifyCombo();
    }

    public void multiplyClick(View v)
    {
        focusAnimations(v,R.drawable.btnmultiply_focused,R.drawable.btnmultiply);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("*");
        falsifyCombo();
    }

    public void divideClick(View v)
    {
        focusAnimations(v,R.drawable.btndivide_focused,R.drawable.btndivide);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("/");
        falsifyCombo();
    }

    public void lnClick(View v)
    {
        focusAnimations(v,R.drawable.btnln_focused,R.drawable.btnln);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("ln");
        falsifyCombo();
    }

    public void logClick(View v)
    {
        focusAnimations(v,R.drawable.btnlog_focused,R.drawable.btnlog);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("log");
        falsifyCombo();
    }

    public void piClick(View v)
    {
        focusAnimations(v,R.drawable.btn9_focused,R.drawable.btnpi);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("pi");
        falsifyCombo();
    }

    public void clearClick(View v)
    {
        focusAnimations(v, R.drawable.btnclear_focused, R.drawable.btnclear);
        txtEq.setText("");
        /*bOrdering = false;
        bShift = false;
        bM = false;
        bE = false;
        bPow = false;*/
    }

    private void focusAnimations(View v,int focused, int normal)
    {
        //getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
        //Remove focus from all the other buttons
        boolean found = false;
        for(ButtonData btn:buttons)
            if(btn.getView()==v)
                found=true;

        if(!found)
        {
            buttons.add(new ButtonData(v,focused,normal));
        }

        for(ButtonData btn:buttons)
            if(btn.getView()!=v)
            {
                btn.getView().setPressed(false);
                btn.getView().setBackgroundResource(btn.getNormal());
                btn.getView().invalidate();
            }
            else
                btn.getView().setPressed(true);

        if(v.isPressed())
            v.setBackgroundResource(focused);
        else
            v.setBackgroundResource(normal);
    }

    //numbers - lower half
    public void nineClick(View v)
    {
        focusAnimations(v,R.drawable.btn9_focused,R.drawable.btn9);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("9");
        falsifyCombo();
    }

    public void eightClick(View v)
    {
        focusAnimations(v,R.drawable.btn8_focused,R.drawable.btn8);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("8");
        falsifyCombo();
    }

    public void sevenClick(View v)
    {
        focusAnimations(v,R.drawable.btn7_focused,R.drawable.btn7);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("7");
        falsifyCombo();
    }

    public void sixClick(View v)
    {
        focusAnimations(v,R.drawable.btn6_focused,R.drawable.btn6);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("6");
        falsifyCombo();
    }

    public void fiveClick(View v)
    {
        focusAnimations(v,R.drawable.btn5_focused,R.drawable.btn5);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("5");
        falsifyCombo();
    }

    public void fourClick(View v)
    {
        focusAnimations(v,R.drawable.btn4_focused,R.drawable.btn4);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("4");
        falsifyCombo();
    }

    public void threeClick(View v)
    {
        focusAnimations(v,R.drawable.btn3_focused,R.drawable.btn3);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("3");
        falsifyCombo();
    }

    public void twoClick(View v)
    {
        focusAnimations(v,R.drawable.btn2_focused,R.drawable.btn2);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("2");
        falsifyCombo();
    }

    public void oneClick(View v)
    {
        focusAnimations(v,R.drawable.btn1_focused,R.drawable.btn1);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("1");
        falsifyCombo();
    }

    public void dotClick(View v)
    {
        focusAnimations(v,R.drawable.btndot_focused,R.drawable.btndot);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        if(txtEq.getText().length()>0)
            if(txtEq.getText().toString().charAt(txtEq.getText().length()-1)!='.')
                txtEq.append(".");
        falsifyCombo();
    }

    public void zeroClick(View v)
    {
        focusAnimations(v,R.drawable.btn0_focused,R.drawable.btn0);
        if(txtEq.getText().toString().equals("0"))
            txtEq.setText("");
        txtEq.append("0");
        falsifyCombo();
    }

    public void backspaceClick(View v)
    {
        focusAnimations(v,R.drawable.btnbackspace_focused,R.drawable.btnbackspace);
        //Remove a character from the back
        if(txtEq.getText().length()>0)
            txtEq.setText(txtEq.getText().toString().substring(0, txtEq.getText().toString().length() - 1));
        falsifyCombo();
    }

    private class AsciiMap
    {
        private char letter;
        private String ascii;
        public AsciiMap(char letter,String ascii){this.letter=letter; this.ascii=ascii;}
        public String[] getMapping(){return new String[]{String.valueOf(letter),String.valueOf(ascii)};}
        public char getChar(){return letter;}
        public String getAscii(){return ascii;}
    }


    private String encrypt(String msg)
    {
        AsciiMap[] chars = {
                new AsciiMap(' ',"032"),new AsciiMap('!',"033"),
                new AsciiMap('\"', "034"),new AsciiMap('#',"035"),new AsciiMap('$',"036"),
                new AsciiMap('%', "037"),new AsciiMap('&',"038"),new AsciiMap('\'',"039"),
                new AsciiMap('(', "040"),new AsciiMap(')',"041"),new AsciiMap('*',"042"),
                new AsciiMap('+', "043"),new AsciiMap(',',"044"),new AsciiMap('-',"045"),
                new AsciiMap('.', "046"),new AsciiMap('/',"047"),
                new AsciiMap('0',"048"),new AsciiMap('1',"049"),new AsciiMap('2',"050"),new AsciiMap('3',"051"),
                new AsciiMap('4',"052"),new AsciiMap('5',"053"),new AsciiMap('6',"054"),new AsciiMap('7',"055"),
                new AsciiMap('8',"056"),new AsciiMap('9',"057"),
                new AsciiMap(':',"058"),new AsciiMap(';',"059"),new AsciiMap('<',"060"),
                new AsciiMap('=',"061"),new AsciiMap('>',"062"),new AsciiMap('?',"063"),
                new AsciiMap('@',"064"),
                new AsciiMap('A',"065"),new AsciiMap('B',"066"),new AsciiMap('C',"067"),new AsciiMap('D',"068"),
                new AsciiMap('E',"069"),new AsciiMap('F',"070"),new AsciiMap('G',"071"),new AsciiMap('H',"072"),
                new AsciiMap('I',"073"),new AsciiMap('J',"074"),new AsciiMap('K',"075"),new AsciiMap('L',"076"),
                new AsciiMap('M',"077"),new AsciiMap('N',"078"),new AsciiMap('O',"079"),new AsciiMap('P',"080"),
                new AsciiMap('Q',"081"),new AsciiMap('R',"082"),new AsciiMap('S',"083"),new AsciiMap('T',"084"),
                new AsciiMap('U',"085"),new AsciiMap('V',"086"),new AsciiMap('W',"087"),new AsciiMap('X',"088"),
                new AsciiMap('Y',"089"),new AsciiMap('Z',"090"),
                new AsciiMap('[',"091"),new AsciiMap('\\',"092"),
                new AsciiMap(']',"093"),new AsciiMap('^',"094"),new AsciiMap('_',"095"),
                new AsciiMap('`',"096"),
                new AsciiMap('a',"097"),new AsciiMap('b',"098"),new AsciiMap('c',"099"),new AsciiMap('d',"100"),
                new AsciiMap('e',"101"),new AsciiMap('f',"102"),new AsciiMap('g',"103"),new AsciiMap('h',"104"),
                new AsciiMap('i',"105"),new AsciiMap('j',"106"),new AsciiMap('k',"107"),new AsciiMap('l',"108"),
                new AsciiMap('m',"109"),new AsciiMap('n',"110"),new AsciiMap('o',"111"),new AsciiMap('p',"112"),
                new AsciiMap('q',"113"),new AsciiMap('r',"114"),new AsciiMap('s',"115"),new AsciiMap('t',"116"),
                new AsciiMap('u',"117"),new AsciiMap('v',"118"),new AsciiMap('w',"119"),new AsciiMap('x',"120"),
                new AsciiMap('y',"121"),new AsciiMap('z',"122"),
                new AsciiMap('{', "123"),new AsciiMap('|',"124"),new AsciiMap('}',"125"),
                new AsciiMap('~', "126")
                            };
        String temp = "";
        for(char c:msg.toCharArray())
        {
            AsciiMap a = findCharMapping(c,chars);
            temp+=a==null?c:"["+a.getAscii()+"]";
        }
        return temp;
        //char[] letters = " d";
    }

    private AsciiMap findCharMapping(char c,AsciiMap[] chars)
    {
        for(AsciiMap a:chars)
        {
            if(a.getChar()==c)
                return a;
        }
        return null;//not found
    }

    public void equalClick(View v)
    {
        focusAnimations(v,R.drawable.btnequals_focused,R.drawable.btnequals);
        //Compute
        //Calculate/Parse
        equation = txtEq.getText().toString();
        equation = compute(equation,"*");
        equation = compute(equation,"/");
        equation = compute(equation,"+");
        equation = compute(equation,"-");

        txtEq.setText("");
        txtEq.setText(equation);

        if(bOrdering)
        {
            String amount = txtEq.getText().toString();
            if(!amount.equals("0") && !amount.isEmpty())
            {
                    try
                    {
                        amnt = Integer.valueOf(amount);

                        final EditText edt = new EditText(this);

                        AlertDialog.Builder alert = new AlertDialog.Builder(this);
                        alert.setTitle("Gimme Some Info");
                        alert.setMessage("Enter your address followed by your phone number below [& any other comments]:");
                        edt.setInputType(InputType.TYPE_CLASS_TEXT);
                        alert.setView(edt);

                        alert.setPositiveButton("Okay", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                details = edt.getText().toString();
                                dialog.dismiss();

                                if(!details.isEmpty())
                                {
                                    AlertDialog.Builder confirm = new AlertDialog.Builder(MainActivity.this);
                                    confirm.setTitle("Send the order?");
                                    confirm.setMessage("Would you like to proceed with your purchase[R" + (amnt * price) + "]?");

                                    confirm.setPositiveButton("Send order", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            text = edt.getText().toString();

                                            item.setQuantity(amnt);
                                            item.setRecStat(0);//Sent - but not delivered
                                            item.setDetails(encrypt(details));

                                            //Back up item to disc
                                            WritersAndReaders.saveItem(item, "data.dat");

                                            //Start agent
                                            if (agent == null)
                                                agent = new BoSAgent(item, SRV_IP, price, getApplicationContext());//104.236.87.104

                                            //Start sender timer - check status of item every 5 seconds
                                            if (tSend == null) {
                                                tSend = new Timer();
                                                tSend.scheduleAtFixedRate(new TimerTask() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            agent.transfer();//Start the transfer service
                                                        } catch (IOException e) {
                                                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                                            tSend.cancel();
                                                            tSend = null;
                                                        }
                                                    }
                                                }, 0, 5000);
                                            }

                                            Toast.makeText(getApplicationContext(), "Request sent.", Toast.LENGTH_LONG).show();
                                        }
                                    });

                                    confirm.setNegativeButton("Cancel Order", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            item.setRecStat(3);//Cancelled
                                            Toast.makeText(getApplicationContext(), "Order cancelled.", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                    confirm.show();
                                }
                                else
                                {
                                    AlertDialog.Builder err = new AlertDialog.Builder(getApplicationContext());
                                    err.setTitle("Error");
                                    err.setMessage("Input[your details] is empty.");
                                    err.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    });
                                    err.show();
                                    //Toast.makeText(this,"Empty details!",Toast.LENGTH_LONG).show();
                                }
                            }
                        });

                        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                item.setRecStat(3);//Cancelled
                            }
                        });

                        alert.show();
                    }
                    catch(NumberFormatException e)
                    {
                        AlertDialog.Builder err = new AlertDialog.Builder(this);
                        err.setTitle("Error");
                        err.setMessage("Input is not a number");
                        err.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        err.show();
                        //Toast.makeText(this,"Invalid number",Toast.LENGTH_LONG).show();
                    }
            }
            else
            {
                AlertDialog.Builder err = new AlertDialog.Builder(this);
                err.setTitle("Error");
                err.setMessage("Invalid amount");
                err.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                err.show();
                //Toast.makeText(this,"Invalid amount",Toast.LENGTH_LONG).show();
            }
        }
        else
            falsifyCombo();
    }

    /*private AlertDialog.Builder getInputDialog(String title,String msg,String btnPos,String btnNeg,Context cxt)
    {
        final EditText edt = new EditText(this);
        AlertDialog.Builder alert = new AlertDialog.Builder(cxt);
        alert.setTitle(title);
        alert.setMessage(msg);
        alert.setView(edt);

        return alert;
    }*/

}
