package com.red.redbluetooth;

/**
 * Created by Red on 2017/4/30.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.maxproj.simplewaveform.SimpleWaveform;
import com.red.redbluetooth.bean.DataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity implements View.OnClickListener, Toolbar.OnMenuItemClickListener {

    /* 一些常量，代表服务器的名称 */
    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";

    private static String TAG = "Main";

    private ListView mListView;
    private Button sendButton;
    private Button disconnectButton;
    private EditText editMsgView;
    private ArrayAdapter<String> mAdapter;
    private List<String> msgList = new ArrayList<String>();
    Context mContext;

    private BluetoothServerSocket mserverSocket = null;
    private ServerThread startServerThread = null;
    private clientThread clientConnectThread = null;
    private BluetoothSocket socket = null;
    private BluetoothDevice device = null;
    private readThread mreadThread = null;

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    LinkedList<Integer> ampList;
    LinearLayoutManager linearLayoutManager;
    RecyclerView recycler_view;
    LinkedList<LinkedList<Integer>> amp_list_list;

    // 创建绘制柱状图的画笔
    Paint barPencilSecond = new Paint();
    // 创建绘制折线的画笔
    Paint peakPencilSecond = new Paint();
    // 创建绘制x轴的画笔
    Paint xAxisPencil = new Paint();
    // 新增 定时器
    private Timer timer;

    private MyDatabaseHelper helper;
    private SQLiteDatabase db;

    private static String INSERT_DATA = "insert into DataSource(get_or_send,content,length,groupNum) values(?,?,?,?)";
    private static String FIND_DATA = "select * from DataSource";
    private static String CLEAR_STORE = "delete from DataSource where get_or_send <= ?";

    private List<DataSource> dataSources;

    private int data_group = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        mContext = this;
        initDba();
        init();
        initWave();
    }

    private void initDba() {
        helper = new MyDatabaseHelper(this, "DataStore.db", null, 1);
        db = helper.getWritableDatabase();
        dataSources = new ArrayList<>();

        Cursor cursor = db.rawQuery(FIND_DATA, null);
        while (cursor.moveToNext()) {
            DataSource source = new DataSource();
            source.setID(cursor.getInt(cursor.getColumnIndex("id")));
            source.setContent(cursor.getString(cursor.getColumnIndex("content")));
            source.setGET_OR_SEND(cursor.getInt(cursor.getColumnIndex("get_or_send")));
            source.setLength(cursor.getInt(cursor.getColumnIndex("length")));
            source.setGroup(cursor.getInt(cursor.getColumnIndex("groupNum")));
            dataSources.add(source);
        }

        if (cursor.moveToLast()) {
            data_group = cursor.getInt(cursor.getColumnIndex("groupNum"));
            data_group++;
            Log.d(TAG, "initDba: " + data_group);
        }


        cursor.close();
    }

    private void init() {

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, msgList);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setFastScrollEnabled(true);
        editMsgView = (EditText) findViewById(R.id.MessageText);
        editMsgView.clearFocus();

        sendButton = (Button) findViewById(R.id.btn_msg_send);
        sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                String msgText = editMsgView.getText().toString();
                if (msgText.length() > 0) {
                    sendMessageHandle(msgText);
                    editMsgView.setText("");
                    editMsgView.clearFocus();
                    //close InputMethodManager
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editMsgView.getWindowToken(), 0);
                } else
                    Toast.makeText(mContext, "发送内容不能为空！", Toast.LENGTH_SHORT).show();
            }
        });

        disconnectButton = (Button) findViewById(R.id.btn_disconnect);
        disconnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.CILENT) {
                    shutdownClient();
                } else if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.SERVICE) {
                    shutdownServer();
                }
                BluetoothMsg.isOpen = false;
                BluetoothMsg.serviceOrCilent = BluetoothMsg.ServerOrCilent.NONE;
                Toast.makeText(mContext, "已断开连接！", Toast.LENGTH_SHORT).show();
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.toolbar_name);
        toolbar.setNavigationOnClickListener(this);
        toolbar.setOnMenuItemClickListener(this);

        recycler_view = (RecyclerView) findViewById(R.id.recycler_view);

    }

    private Handler LinkDetectedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                String s = (String) msg.obj;
                msgList.add(s);


                // 新增：
                ampList.add(s.getBytes().length * (-1));
                drawWave();
                DataSource source = new DataSource();
                source.setContent(s);
                source.setGET_OR_SEND(0);
                source.setLength(s.getBytes().length);
                source.setGroup(data_group);
                saveData(source);
                //
            } else if (msg.what == 2) {
                sendMessageHandle(randomStr());
            } else {
                msgList.add((String) msg.obj);
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(msgList.size() - 1);
        }
    };

    @Override
    protected void onResume() {
        // 设置客户端 or 服务端
        BluetoothMsg.serviceOrCilent = BluetoothMsg.ServerOrCilent.CILENT;

        if (BluetoothMsg.isOpen) {
            Toast.makeText(mContext, "连接已经打开，可以通信。如果要再建立连接，请先断开！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.CILENT) {
            String address = BluetoothMsg.BlueToothAddress;
            if (!address.equals("null")) {
                device = mBluetoothAdapter.getRemoteDevice(address);
                clientConnectThread = new clientThread();
                clientConnectThread.start();
                BluetoothMsg.isOpen = true;
            } else {
                Toast.makeText(mContext, "address is null !", Toast.LENGTH_SHORT).show();
            }
        } else if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.SERVICE) {
            startServerThread = new ServerThread();
            startServerThread.start();
            BluetoothMsg.isOpen = true;
        }
        super.onResume();
    }

    //开启客户端
    private class clientThread extends Thread {
        @Override
        public void run() {
            try {
                //创建一个Socket连接：只需要服务器在注册时的UUID号
                // socket = device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                //连接
                Message msg2 = new Message();
                msg2.obj = "请稍候，正在连接服务器:" + BluetoothMsg.BlueToothAddress;
                msg2.what = 0;
                LinkDetectedHandler.sendMessage(msg2);

                socket.connect();

                Message msg = new Message();
                msg.obj = "已经连接上服务端！可以发送信息。";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);
                //启动接受数据
                mreadThread = new readThread();
                mreadThread.start();
            } catch (IOException e) {
                Log.e("connect", "", e);
                Message msg = new Message();
                msg.obj = "连接服务端异常！断开连接重新试一试。";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);
            }
        }
    }



    //开启服务器
    private class ServerThread extends Thread {
        @Override
        public void run() {

            try {
                    /* 创建一个蓝牙服务器
                     * 参数分别：服务器名称、UUID   */
                mserverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM,
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                Log.d("server", "wait cilent connect...");

                Message msg = new Message();
                msg.obj = "请稍候，正在等待客户端的连接...";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);

                    /* 接受客户端的连接请求 */
                socket = mserverSocket.accept();
                Log.d("server", "accept success !");

                Message msg2 = new Message();
                String info = "客户端已经连接上！可以发送信息。";
                msg2.obj = info;
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg2);
                //启动接受数据
                mreadThread = new readThread();
                mreadThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    /* 停止服务器 */
    private void shutdownServer() {
        new Thread() {
            @Override
            public void run() {
                if (startServerThread != null) {
                    startServerThread.interrupt();
                    startServerThread = null;
                }
                if (mreadThread != null) {
                    mreadThread.interrupt();
                    mreadThread = null;
                }
                try {
                    if (socket != null) {
                        socket.close();
                        socket = null;
                    }
                    if (mserverSocket != null) {
                        mserverSocket.close();/* 关闭服务器 */
                        mserverSocket = null;
                    }
                } catch (IOException e) {
                    Log.e("server", "mserverSocket.close()", e);
                }
            }

            ;
        }.start();
    }

    /* 停止客户端连接 */
    private void shutdownClient() {
        new Thread() {
            @Override
            public void run() {
                if (clientConnectThread != null) {
                    clientConnectThread.interrupt();
                    clientConnectThread = null;
                }
                if (mreadThread != null) {
                    mreadThread.interrupt();
                    mreadThread = null;
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    socket = null;
                }
            }

            ;
        }.start();
    }

    //发送数据
    private void sendMessageHandle(String msg) {
        if (socket == null) {
            Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            OutputStream os = socket.getOutputStream();
            os.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //  新增：
        ampList.add(msg.getBytes().length);
        drawWave();
        DataSource source = new DataSource();
        source.setContent(msg);
        source.setGET_OR_SEND(1);
        source.setLength(msg.getBytes().length);
        source.setGroup(data_group);
        saveData(source);
        //

        msgList.add(msg);
        mAdapter.notifyDataSetChanged();
        mListView.setSelection(msgList.size() - 1);
    }

    //读取数据
    private class readThread extends Thread {
        @Override
        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;
            InputStream mmInStream = null;

            try {
                mmInStream = socket.getInputStream();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            while (true) {
                try {
                    // Read from the InputStream
                    if ((bytes = mmInStream.read(buffer)) > 0) {
                        byte[] buf_data = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf_data[i] = buffer[i];
                        }
                        String s = new String(buf_data);
                        Message msg = new Message();
                        msg.obj = s;
                        msg.what = 1;
                        LinkDetectedHandler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    try {
                        mmInStream.close();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.toolbar:
                //finish();
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_data:
                recycler_view.setVisibility(View.VISIBLE);
                break;
            case R.id.hide_data:
                recycler_view.setVisibility(View.GONE);
                break;
            case R.id.clear_save:
                db.execSQL(CLEAR_STORE, new String[]{"1"});
                amp_list_list = new LinkedList();
                ampList = new LinkedList<>();
                amp_list_list.add(ampList);
                drawWave();
                break;
            case R.id.auto_send:
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Message msg = new Message();
                        msg.what = 2;
                        LinkDetectedHandler.sendMessage(msg);
                    }
                }, 2000, 2000);
                break;
            case R.id.stop_send:
                timer.cancel();
                break;
        }
        return true;
    }


    private int randomInt(int min, int max) {
        Random rand = new Random();
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    private String randomStr() {
        int i = randomInt(1, 10);
        String str = "";
        for (int j = 0; j < i; j++) {
            str = str + "b";
        }
        return str;
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
        LinkedList<LinkedList<Integer>> amp_list_list;

        public RecyclerViewAdapter(LinkedList<LinkedList<Integer>> amp_list_list) {
            this.amp_list_list = amp_list_list;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public SimpleWaveform simpleWaveform;

            public ViewHolder(View itemView) {
                super(itemView);
                this.simpleWaveform = (SimpleWaveform) itemView
                        .findViewById(R.id.simplewaveform_row);
            }
        }


        @Override
        public int getItemCount() {
            Log.d("", "SimpleWaveform: amp_list_list.size() " + amp_list_list.size());
            return amp_list_list.size();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Log.d("", "SimpleWaveform: position " + position);

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.simpleWaveform.getLayoutParams();
            int length = amp_list_list.get(position).size();

            Log.d(TAG, "onBindViewHolder: length = " + length + " position = " + position);
            params.width = length * 15 + (length - 1) * 35;


            holder.simpleWaveform.setLayoutParams(params);
            holder.simpleWaveform.setDataList(amp_list_list.get(position));

            holder.simpleWaveform.barGap = 50;
            //define x-axis direction
            holder.simpleWaveform.modeDirection = SimpleWaveform.MODE_DIRECTION_LEFT_RIGHT;

            //define if draw opposite pole when show bars
            holder.simpleWaveform.modeAmp = SimpleWaveform.MODE_AMP_ORIGIN;
            //define if the unit is px or percent of the view's height
            holder.simpleWaveform.modeHeight = SimpleWaveform.MODE_HEIGHT_PERCENT;
            //define where is the x-axis in y-axis
            holder.simpleWaveform.modeZero = SimpleWaveform.MODE_ZERO_CENTER;
            //if show bars?
            holder.simpleWaveform.showBar = true;

            //define how to show peaks outline
            holder.simpleWaveform.modePeak = SimpleWaveform.MODE_PEAK_ORIGIN;
            //if show peaks outline?
            holder.simpleWaveform.showPeak = true;


            //show x-axis
            holder.simpleWaveform.showXAxis = true;
            xAxisPencil.setStrokeWidth(1);
            xAxisPencil.setColor(0x88ffffff);
            holder.simpleWaveform.xAxisPencil = xAxisPencil;

            //define pencil to draw bar
            barPencilSecond.setStrokeWidth(15);
            barPencilSecond.setColor(0xff1dcfcf);
            holder.simpleWaveform.barPencilSecond = barPencilSecond;

            //define pencil to draw peaks outline
            peakPencilSecond.setStrokeWidth(5);
            peakPencilSecond.setColor(0xfffeef3f);
            holder.simpleWaveform.peakPencilSecond = peakPencilSecond;

            //the first part will be draw by PencilFirst
            holder.simpleWaveform.refresh();
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = View.inflate(parent.getContext(),
                    R.layout.row_recycler, null);
            ViewHolder holder = new ViewHolder(view);
            Log.d("", "SimpleWaveform: onCreateViewHolder ");
            return holder;
        }

    }

    private void drawWave() {
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recycler_view.setLayoutManager(linearLayoutManager);
        RecyclerViewAdapter waveAdapter = new RecyclerViewAdapter(amp_list_list);
        recycler_view.setAdapter(waveAdapter);
        recycler_view.scrollToPosition(amp_list_list.size() - 1);
        Log.d(TAG, "demoAdvance2: " + amp_list_list.size());
    }


    public void initWave() {
        fillWave();
        drawWave();
        newData();
    }

    private void newData() {
        ampList = new LinkedList<>();
        amp_list_list.add(ampList);

    }


    private void fillWave() {
        amp_list_list = new LinkedList();
        for (int i = 0, j = 0; j < dataSources.size(); i++) {
            LinkedList<Integer> integers = new LinkedList<>();
            amp_list_list.add(integers);
            while (dataSources.get(j).getGroup() == i) {
                if (dataSources.get(j).getGET_OR_SEND() == 1) {
                    integers.add(dataSources.get(j).getLength());
                    j++;
                } else {
                    integers.add(dataSources.get(j).getLength() * (-1));
                    j++;
                }
                if (j == dataSources.size())
                    break;
            }
        }
    }

    private void saveData(DataSource s) {
        String get_or_send = String.valueOf(s.getGET_OR_SEND());
        String content = s.getContent();
        String length = String.valueOf(s.getLength());
        String group = String.valueOf(s.getGroup());
        db.execSQL(INSERT_DATA, new String[]{get_or_send, content, length, group});
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: 1");
        if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.CILENT) {
            shutdownClient();
        } else if (BluetoothMsg.serviceOrCilent == BluetoothMsg.ServerOrCilent.SERVICE) {
            shutdownServer();
        }
        BluetoothMsg.isOpen = false;
        BluetoothMsg.serviceOrCilent = BluetoothMsg.ServerOrCilent.NONE;
        db.close();
        Log.d(TAG, "onDestroy: 2");
    }


}