package com.project.nicki.displaystabilizer.stabilization;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.project.nicki.displaystabilizer.UI.DemoDrawUI;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;
import com.project.nicki.displaystabilizer.dataprocessor.CircularBuffer;
import com.project.nicki.displaystabilizer.dataprocessor.proAcceGyroCali;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by nickisverygood on 1/3/2016.
 */
public class stabilize_v2 implements Runnable {
    //public tcpipdata mtcpip = new tcpipdata();
    public static Handler getDraw;
    public static Handler getSensor;
    //draw to DemoDraw
    public static List<Point> toDraw = new ArrayList<Point>();
    public static BoundingBox bbox = new BoundingBox();
    public static boolean posdrawing = false;
    public static boolean updatefakepos = false;
    public static int oneorminusone = 1;
    //constants
    private static float cX;
    private static float cY;
    //mods
    public boolean CalibrationMode = true;
    public ArrayList<sensordata> stroke2pos;
    public int fakeposposition;
    FileWriter mFileWriter;
    //buffers
    ArrayList<sensordata> strokebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> strokedeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> accebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posbuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posdeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> stastrokebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> stastrokedeltabuffer = new ArrayList<sensordata>();
    //tmps
    long prevTime = 0;
    float[] prevStroke = null;
    boolean drawSTATUS = false;
    boolean prevdrawSTATUS = false;
    boolean init = false;
    sensordata tmpaccesensordata;
    display mdisplay = new display();
    float[] Pos;
    int deltaingStatus = 0;
    sensordata tmp1accesensordata;
    long prev = System.currentTimeMillis();
    private String csvName = "stabilize_v2.csv";
    private String TAG = "stabilize_v2";
    private Context mContext;

    public stabilize_v2(Context context) {
        mContext = context;
    }

    public static float getcY() {
        return cY;
    }

    public static void setcY(float cY) {
        stabilize_v2.cY = cY;
    }

    public static float getcX() {
        return cX;
    }

    //setConstants
    public static void setcX(float cX) {
        stabilize_v2.cX = cX;
    }

    @Override
    public void run() {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = csvName;
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer = null;
        // File exist
        if (f.exists()) {
            f.delete();
        }
        Looper.prepare();
        getSensor = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Bundle bundlegot = msg.getData();
                Pos = bundlegot.getFloatArray("Pos");
                tmpaccesensordata = bundle2sensordata(bundlegot);
                Log.d(TAG, "tmpaccesensordata: " + tmpaccesensordata.getData()[0] + " " + tmpaccesensordata.getData()[1]);
                prevdrawSTATUS = drawSTATUS;
                drawSTATUS = DemoDraw.drawing < 2;

                if (deltaingStatus == 0) {
                    tmp1accesensordata = tmpaccesensordata;
                    tmpaccesensordata = new sensordata(tmp1accesensordata.getTime(), new float[]{0, 0});
                    deltaingStatus = 1;
                } else if (deltaingStatus == 1) {
                    tmpaccesensordata = new sensordata(tmp1accesensordata.getTime(), new float[]{
                            tmpaccesensordata.getData()[0] - tmp1accesensordata.getData()[1],
                            tmpaccesensordata.getData()[1] - tmp1accesensordata.getData()[1]});
                }
                //init
                if (prevdrawSTATUS == false && drawSTATUS == true || init == false) {
                    if (CalibrationMode == true && strokebuffer.size() > 1 && posbuffer.size() > 1) {
                        //cX = Math.abs(getSumArray(strokedeltabuffer).get(strokedeltabuffer.size()-1 +1).getData()[0])/Math.abs(getSumArray(posdeltabuffer).get(posdeltabuffer.size() - 1).getData()[0]);
                        //cY = Math.abs(getSumArray(strokedeltabuffer).get(strokedeltabuffer.size()-1 +1).getData()[1])/Math.abs(getSumArray(posdeltabuffer).get(posdeltabuffer.size()-1).getData()[1]);
                        cX = -Math.abs(strokebuffer.get(strokebuffer.size() - 1).getData()[0] - strokebuffer.get(0).getData()[0]) / Math.abs(posbuffer.get(posbuffer.size() - 1).getData()[0] - posbuffer.get(0).getData()[0]);
                        cY = -Math.abs(strokebuffer.get(strokebuffer.size() - 1).getData()[1] - strokebuffer.get(0).getData()[1]) / Math.abs(posbuffer.get(posbuffer.size() - 1).getData()[1] - posbuffer.get(0).getData()[1]);
                        CalibrationMode = false;
                    }
                    strokebuffer = new ArrayList<sensordata>();
                    strokedeltabuffer = new ArrayList<sensordata>();
                    posbuffer = new ArrayList<sensordata>();
                    posdeltabuffer = new ArrayList<sensordata>();
                    stastrokebuffer = new ArrayList<sensordata>();
                    stastrokedeltabuffer = new ArrayList<sensordata>();
                    prevTime = 0;
                    prevStroke = null;
                    init = true;
                    toDraw = new ArrayList<Point>();
                    tmpaccesensordata = null;
                    Pos = null;
                    deltaingStatus = 0;
                }


            }
        };
        getDraw = new Handler() {

            //noshake
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (tmpaccesensordata == null) {
                    return;
                }
                Bundle bundlegot = msg.getData();
                prevdrawSTATUS = drawSTATUS;
                drawSTATUS = DemoDraw.drawing < 2;


                //init
                //if (prevdrawSTATUS == false && drawSTATUS == true || init == false || System.currentTimeMillis() - prev > 200) {
                if (prevdrawSTATUS == false && drawSTATUS == true || init == false) {
                    fakeposposition = 0;
                    if (updatefakepos == true) {
                        stroke2pos = strokedeltabuffer;
                        Toast.makeText(mContext, "fakepos Updated " + stroke2pos.size(),
                                Toast.LENGTH_LONG).show();
                        updatefakepos = false;
                    }
                    fakeposposition = 0;

                    prev = System.currentTimeMillis();
                    if (stastrokedeltabuffer.size() > 0) {
                        double tmp = 0;
                        for (int i = 0; i < stastrokedeltabuffer.size(); i++) {
                            tmp += Math.pow(Math.pow(stastrokedeltabuffer.get(i).getData()[0], 2) + Math.pow(stastrokedeltabuffer.get(i).getData()[1], 2), 0.5);
                        }
                        tmp /= stastrokedeltabuffer.size();
                        LogCSV("performance", String.valueOf(tmp), "", "", "", "", "");

                        //setcX(cX+1);
                        //setcY(cY+1);
                        CircularBuffer.CONST_K += 0.05;
                        mdisplay.diaplayperformance(String.valueOf(CircularBuffer.CONST_K));
                    }
                    //refresh view
                    Message msg3 = new Message();
                    msg3.what = 1;
                    DemoDraw.mhandler.sendMessage(msg3);
                    if (CalibrationMode == true && strokebuffer.size() > 1 && posbuffer.size() > 1) {
                        //cX = Math.abs(getSumArray(strokedeltabuffer).get(strokedeltabuffer.size()-1 +1).getData()[0])/Math.abs(getSumArray(posdeltabuffer).get(posdeltabuffer.size() - 1).getData()[0]);
                        //cY = Math.abs(getSumArray(strokedeltabuffer).get(strokedeltabuffer.size()-1 +1).getData()[1])/Math.abs(getSumArray(posdeltabuffer).get(posdeltabuffer.size()-1).getData()[1]);
                        //cX = -(Math.abs(strokebuffer.get(strokebuffer.size() - 1).getData()[0] - strokebuffer.get(0).getData()[0])) / (Math.abs(posbuffer.get(posbuffer.size() - 1).getData()[0] - posbuffer.get(0).getData()[0]));
                        //cY = -(Math.abs(strokebuffer.get(strokebuffer.size() - 1).getData()[1] - strokebuffer.get(0).getData()[1])) / (Math.abs(posbuffer.get(posbuffer.size() - 1).getData()[1] - posbuffer.get(0).getData()[1]));
                        CalibrationMode = false;
                        Log.d(TAG, "multiplier: " + cX + " " + cY);
                    }
                    strokebuffer = new ArrayList<sensordata>();
                    strokedeltabuffer = new ArrayList<sensordata>();
                    posbuffer = new ArrayList<sensordata>();
                    posdeltabuffer = new ArrayList<sensordata>();
                    stastrokebuffer = new ArrayList<sensordata>();
                    stastrokedeltabuffer = new ArrayList<sensordata>();
                    prevTime = 0;
                    prevStroke = null;
                    init = true;
                    toDraw = new ArrayList<Point>();
                    tmpaccesensordata = null;
                    Pos = null;
                }



/*
                if (posbuffer.size() > 1) {
                    posdeltabuffer.add(getlatestdelta(posbuffer));
                    if (posdeltabuffer.size() > 1) {
                        if (posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0] * posdeltabuffer.get(posdeltabuffer.size() - 2).getData()[0] < 0) {
                            Log.d(TAG, "reversed: " + posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0]);
                        }
                    }
                }
*/
                //buffer Draw
                strokebuffer.add(bundle2sensordata(bundlegot));

                //mtcpip.tcpipdatasend((float) strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[0], (float) strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[1]);

                //LogCSV(String.valueOf(bundle2sensordata(bundlegot).getData()[0]* com.project.nicki.displaystabilizer.init.pix2m),String.valueOf(bundle2sensordata(bundlegot).getData()[1]* com.project.nicki.displaystabilizer.init.pix2m),"","","","");
                if (strokebuffer.size() > 1) {
                    strokedeltabuffer.add(getlatestdelta(strokebuffer));
                }

                //exit if set to draw2pos
                Log.d(TAG, "fakepos: " + posdrawing);
                if (posdrawing == true && stroke2pos != null && fakeposposition < stroke2pos.size() - 1) {
                    //override pos
                    // if (posdeltabuffer.size() > 1) {
                    //    posdeltabuffer.remove(posdeltabuffer.size() - 1);
                    //}
                    //Log.d(TAG,"compar: "+posdeltabuffer.get(0).getData()[0] + " " + stroke2pos.get(0).getData()[0] );
                    posdeltabuffer.add(new sensordata(stroke2pos.get(fakeposposition).getTime(), new float[]{
                            stroke2pos.get(fakeposposition).getData()[0] * (float) com.project.nicki.displaystabilizer.init.pix2m * 100 * oneorminusone,
                            stroke2pos.get(fakeposposition).getData()[1] * (float) com.project.nicki.displaystabilizer.init.pix2m * 100 * oneorminusone}));
                    fakeposposition++;
                    Log.d(TAG, "fakepos: " + fakeposposition);

                } else {
                    //buffer Pos
                    if (tmpaccesensordata != null) {
                        posbuffer.add(tmpaccesensordata);
                        posdeltabuffer.add(tmpaccesensordata);
                        tmpaccesensordata = null;
                    }
                }
                if (posdeltabuffer != null && stroke2pos != null) {
                    if (posdeltabuffer.size() > 0 && stroke2pos.size() > 0) {
                        Log.d(TAG, "testhowbig " + String.valueOf(posdeltabuffer.get(0).getData()[0] + " " + stroke2pos.get(0).getData()[0] * (float) com.project.nicki.displaystabilizer.init.pix2m));
                    }
                }

                //get stabilized result
                if (strokedeltabuffer.size() > 0 && posdeltabuffer.size() > 0) {
                    sensordata stastrokedelta = new sensordata();
                    stastrokedelta.setTime(strokedeltabuffer.get(strokedeltabuffer.size() - 1).getTime());
                    stastrokedelta.setData(new float[]{
                            strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[0] - posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0] * cX,
                            strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[1] - posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[1] * cY});
                    /*
                    LogCSV(
                            String.valueOf(-strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[0] * com.project.nicki.displaystabilizer.init.pix2m),
                            String.valueOf(posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0]),
                            String.valueOf(-strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[1]* com.project.nicki.displaystabilizer.init.pix2m),
                            String.valueOf(posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[1]), "", "");
                            */
                    stastrokedeltabuffer.add(stastrokedelta);
                    if (prevStroke != null) {
                        //sumof stastrokedeltabuffer
                        prevStroke[0] += stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[0];
                        prevStroke[1] += stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[1];
                        stastrokebuffer.add(new sensordata(stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getTime(), prevStroke));
                        //cal how far is fram prevStroke to finger(now)
                        float tofinger[] = new float[]{strokebuffer.get(strokebuffer.size() - 1).getData()[0] - prevStroke[0],
                                strokebuffer.get(strokebuffer.size() - 1).getData()[1] - prevStroke[1]};
                        toDraw = new ArrayList<Point>();
                        for (sensordata msensordata : stastrokebuffer) {
                            Point todrawPoint = new Point();
                            todrawPoint.x = msensordata.getData()[0] + tofinger[0];
                            todrawPoint.y = msensordata.getData()[1] + tofinger[1];
                            toDraw.add(todrawPoint);
                        }
                        bbox.set(toDraw);
                    } else {
                        prevStroke = new float[]{
                                strokebuffer.get(0).getData()[0],
                                strokebuffer.get(0).getData()[1]};
                        prevStroke = new float[]{0, 0};
                    }
                    Log.d(TAG, "drawpos: " + prevStroke[0] + " " + prevStroke[1]);
                }

                //refresh view
                Message msg3 = new Message();
                msg3.what = 1;
                DemoDraw.mhandler.sendMessage(msg3);

                if (strokedeltabuffer.size() > 0 && posdeltabuffer.size() > 0 && Pos != null) {
                    final ArrayList<sensordata> tmp = getSumArray(strokedeltabuffer);
                    ArrayList<sensordata> tmp2 = getSumArray(posdeltabuffer);
                    proAcceGyroCali.multiplier =
                            (float) (Math.pow(Math.pow(tmp.get(tmp.size() - 1).getData()[0] * com.project.nicki.displaystabilizer.init.pix2m, 2) + Math.pow(tmp.get(tmp.size() - 1).getData()[1] * com.project.nicki.displaystabilizer.init.pix2m, 2), 0.5) /
                                    Math.pow(Math.pow(tmp2.get(tmp2.size() - 1).getData()[0], 2) + Math.pow(tmp2.get(tmp2.size() - 1).getData()[1], 2), 0.5));
                    Log.d(TAG, "multiplier: " + String.valueOf(proAcceGyroCali.multiplier));


                    /*
                    LogCSV(
                            String.valueOf(tmp.get(tmp.size() - 1).getTime()),
                            String.valueOf(tmp.get(tmp.size() - 1).getData()[0] * -1 * com.project.nicki.displaystabilizer.init.pix2m),
                            String.valueOf(tmp.get(tmp.size() - 1).getData()[1] * -1 * com.project.nicki.displaystabilizer.init.pix2m),
                            String.valueOf(tmp2.get(tmp2.size() - 1).getTime()),
                            String.valueOf(tmp2.get(tmp2.size() - 1).getData()[0]),
                            String.valueOf(tmp2.get(tmp2.size() - 1).getData()[1]));
                            */

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // mtcpip.tcpipdatasend((float)(tmp.get(tmp.size() - 1).getData()[0] * -1 * com.project.nicki.displaystabilizer.init.pix2m),(float)(tmp.get(tmp.size() - 1).getData()[1] * -1 * com.project.nicki.displaystabilizer.init.pix2m));
                        }
                    }).start();

                    LogCSV("strokeposlog",
                            String.valueOf(tmp.get(tmp.size() - 1).getTime()),
                            String.valueOf(tmp.get(tmp.size() - 1).getData()[0] * -1 * com.project.nicki.displaystabilizer.init.pix2m),
                            String.valueOf(tmp.get(tmp.size() - 1).getData()[1] * -1 * com.project.nicki.displaystabilizer.init.pix2m),
                            String.valueOf(tmp2.get(tmp2.size() - 1).getTime()),
                            String.valueOf(Pos[0]),
                            String.valueOf(Pos[1]));
                }

                //compare(strokedeltabuffer,posdeltabuffer);

                if (
                        tmpaccesensordata != null &&
                                posbuffer.size() > 0 &&
                                posdeltabuffer.size() > 0 &&
                                strokebuffer.size() > 0 &&
                                strokedeltabuffer.size() > 0 &&
                                stastrokebuffer.size() > 0 &&
                                stastrokedeltabuffer.size() > 0) {
                    /*
                    LogCSV(
                            String.valueOf(tmpaccesensordata.getTime()),
                            String.valueOf(posdeltabuffer.get(posdeltabuffer.size() - 1).getTime()),
                            String.valueOf(strokebuffer.get(strokebuffer.size() - 1).getTime()),
                            String.valueOf(strokedeltabuffer.get(strokedeltabuffer.size() - 1).getTime()),
                            String.valueOf(stastrokebuffer.get(stastrokebuffer.size() - 1).getTime()),
                            String.valueOf(strokedeltabuffer.get(strokedeltabuffer.size() - 1).getTime()));*/
                }

            }


        }

        ;
        Looper.loop();
    }


    private void compare(ArrayList<sensordata> data1, ArrayList<sensordata> data2) {
        int num;
        if (data1.size() > data2.size()) {
            num = data1.size();
        } else {
            num = data2.size();
        }
        for (int j = 0; j < num; j++) {
            if (data1.get(j) == null) {
                data1.add(new sensordata(0, new float[]{0, 0}));
            }
            if (data2.get(j) == null) {
                data2.add(new sensordata(0, new float[]{0, 0}));
            }
            LogCSV("compare",
                    String.valueOf(data1.get(j).getTime()),
                    String.valueOf(data1.get(j).getData()[0]),
                    String.valueOf(data1.get(j).getData()[1]),
                    String.valueOf(data2.get(j).getTime()),
                    String.valueOf(data2.get(j).getData()[0]),
                    String.valueOf(data2.get(j).getData()[1]));
        }
    }

    private float vectlen(float[] data) {
        float returnLength = 0;
        double tmp_power_sum = 0;
        for (int i = 0; i < data.length; i++) {
            tmp_power_sum += Math.pow((double) data[i], 2);
        }
        return (float) Math.pow(tmp_power_sum, 0.5);
    }

    private sensordata bundle2sensordata(Bundle bundle) {
        sensordata returesensordata = new sensordata();
        for (String keys : bundle.keySet()) {
            if (keys == "Time") {
                returesensordata.setTime(bundle.getLong(keys));
            } else {
                returesensordata.setData(bundle.getFloatArray(keys));
            }
        }
        return returesensordata;
    }

    private ArrayList<sensordata> getSumArray(ArrayList<sensordata> sensordatas) {
        ArrayList<sensordata> returnArray = new ArrayList<sensordata>();
        float[] tmp_data_sum = new float[sensordatas.get(0).getData().length];
        returnArray.add(new sensordata(sensordatas.get(0).getTime(), new float[sensordatas.get(0).getData().length]));
        for (int i = 0; i < sensordatas.size() - 1 + 1; i++) {
            for (int j = 0; j < tmp_data_sum.length; j++) {
                tmp_data_sum[j] += sensordatas.get(i).getData()[j];
            }
            returnArray.add(new sensordata(sensordatas.get(i).getTime(), tmp_data_sum));
            //LogCSV(String.valueOf(tmp_data_sum[0]), String.valueOf(tmp_data_sum[1]), "", "", "", "");
        }
        return returnArray;
    }

    private sensordata getlatestdelta(ArrayList<sensordata> strokebuffer) {
        sensordata msensordata = new sensordata();
        //compute delta
        float[] deltaFloat = new float[strokebuffer.get(0).getData().length];
        for (int i = 0; i < strokebuffer.get(0).getData().length; i++) {
            deltaFloat[i] = (strokebuffer.get(strokebuffer.size() - 1).getData()[i] - strokebuffer.get(strokebuffer.size() - 2).getData()[i]);
        }
        msensordata.setTime(strokebuffer.get(strokebuffer.size() - 1).getTime());
        msensordata.setData(deltaFloat);
        return msensordata;
    }

    public void LogCSV(String Name, String a, String b, String c, String d, String g, String h) {
        //init CSV logging
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        //String fileName = csvName;
        String fileName = Name + ".csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer = null;
        // File exist
        if (f.exists() && !f.isDirectory()) {
            try {
                mFileWriter = new FileWriter(filePath, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer = new CSVWriter(mFileWriter);
        } else {
            try {
                writer = new CSVWriter(new FileWriter(filePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            String line = String.format("%s,%s,%s,%s,%s,%s\n", a, b, c, d, g, h);
            mFileWriter.write(line);
        } catch (Exception ex) {
        }
        /*
        catch (IOException e) {
            e.printStackTrace();
        }
        */

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //getsetBoundingBox
    public static class BoundingBox {
        public float Xmin = 0;
        public float Xmax = 0;
        public float Ymin = 0;
        public float Ymax = 0;

        public void set(List<Point> pointArrayList) {
            if (pointArrayList.size() > 0) {
                Xmin = pointArrayList.get(0).x;
                Xmax = pointArrayList.get(0).x;
                Ymin = pointArrayList.get(0).y;
                Ymax = pointArrayList.get(0).y;
                for (Point p : pointArrayList) {
                    if (p.x < Xmin) {
                        Xmin = p.x;
                    }
                    if (p.x > Xmax) {
                        Xmax = p.x;
                    }
                    if (p.y < Ymin) {
                        Ymin = p.y;
                    }
                    if (p.y > Ymax) {
                        Ymax = p.y;
                    }
                }
                Log.d("bbox: ", Xmin + " " + Xmax + " " + Ymin + " " + Ymax);
            }
        }

        public float getXmin() {
            return Xmin;
        }

        public float getXmax() {
            return Xmax;
        }

        public float getYmin() {
            return Ymin;
        }

        public float getYmax() {
            return Ymax;
        }

    }

    public static class Point implements Serializable {
        public float x;
        public float y;
        public float dx;
        public float dy;

        public void setX(float x) {
            this.x = x;
        }

        public void setY(float y) {
            this.y = y;
        }
    }

    public class sensordata {
        private long Time;
        private float[] Data = new float[3];

        public sensordata() {
            this(0, new float[]{0, 0, 0});
        }

        public sensordata(long time, float[] data) {
            this.Time = time;
            for (int i = 0; i < data.length; i++) {
                this.Data[i] = data[i];
            }
        }

        public void setsensordata(long time, float[] data) {
            this.Time = time;
            for (int i = 0; i < data.length; i++) {
                this.Data[i] = data[i];
            }
        }

        public long getTime() {
            return Time;
        }

        public void setTime(long time) {
            this.Time = time;
        }

        public float[] getData() {
            return Data;
        }

        public void setData(float[] data) {
            for (int i = 0; i < data.length; i++) {
                this.Data[i] = data[i];
            }
        }
    }

    public class display {
        public void displaystatus1(final String s) {
            DemoDrawUI.runOnUI(new Runnable() {
                @Override
                public void run() {
                    try {
                        //DemoDrawUI.mlog_acce.setText(s);
                    } catch (Exception ex) {

                    }

                }
            });
        }

        public void displaystatus2(final String s) {
            DemoDrawUI.runOnUI(new Runnable() {
                @Override
                public void run() {
                    try {
                        DemoDrawUI.mlog_gyro.setText(s);
                    } catch (Exception ex) {

                    }

                }
            });
        }

        public void diaplayperformance(final String s) {
            DemoDrawUI.runOnUI(new Runnable() {
                @Override
                public void run() {
                    try {
                        DemoDrawUI.mperformance.setText(s);
                    } catch (Exception ex) {

                    }

                }
            });
        }
    }


    public class tcpipdata {
        InetAddress serverAddr;
        Socket socket;

        public tcpipdata() {
            try {
                Log.d("TCP", "C: Connecting...");
//                mdisplay.displaystatus1("TCP Connecting");
                serverAddr = InetAddress.getByName("192.168.0.115");
                socket = new Socket(serverAddr, 4444);
                Log.d("TCP", "C: Connected.");
                //mdisplay.displaystatus1("TCP Connected");
            } catch (Exception e) {
                Log.e("TCP", "C: Error", e);
            }
        }

        public void tcpipdatasend(float f, float f2) {
            try {
                /*
                Log.d("TCP", "C: Sending: '" + f + "'");
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                //DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
                //DOS.writeFloat(f);
                out.println(String.valueOf(f));
                */

                PrintWriter out2 = new PrintWriter(socket.getOutputStream(), true);
                String line = null;
                line = String.valueOf(f);
                Log.d("TCP", "Sending:" + line);
                out2.write(line + "," + String.valueOf(f2) + "\n");
                out2.flush();


                Log.d("TCP", "C: Sent. " + out2.toString());
                Log.d("TCP", "C: Done.");

            } catch (Exception e) {
                Log.e("TCP", "S: Error", e);
            }
        }

        public void tcpipdatadisconnect() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
}
