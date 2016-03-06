package com.project.nicki.displaystabilizer.dataprocessor;

import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.utils.Filters.filterSensorData;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;

import java.util.List;

/**
 * Created by nickisverygood on 3/6/2016.
 */
public class calRk4 {
    //init filters
    filterSensorData filtercalRk4_ACCE = new filterSensorData.Builder().build();
    filterSensorData filtercalRk4_VELO = new filterSensorData.Builder().build();
    filterSensorData filtercalRk4_POSI = new filterSensorData.Builder().build();

    static final float NS2S = 1.0f / 1000000000.0f;
    long last_timestamp = 0;
    public Position[] prevPosition = new Position[3];
    private double prevPos;
    private double deltaPos = 0;

    public calRk4() {
        for (int i = 0; i < prevPosition.length; i++) {
            prevPosition[i] = new Position(0, 0);
        }
    }

    public float[] calcList(List<SensorCollect.sensordata> msensordataList) {
        for (SensorCollect.sensordata msensordata : msensordataList) {
            calc(msensordata);
        }
        return new float[]{(float) prevPosition[0].pos, (float) prevPosition[1].pos, (float) prevPosition[2].pos};
    }

    public float[] calc(SensorCollect.sensordata msensordata) {
        //filter
        msensordata.setData(filtercalRk4_ACCE.filter(msensordata.getData()));
        for (int i = 0; i < prevPosition.length; i++) {
            prevPosition[i].pos = filtercalRk4_POSI.filter(new float[]{(float) prevPosition[0].pos,(float) prevPosition[1].pos,(float) prevPosition[2].pos})[i];
            prevPosition[i].v = filtercalRk4_VELO.filter(new float[]{(float) prevPosition[0].v,(float) prevPosition[1].v,(float) prevPosition[2].v})[i];
        }


        for (int i = 0; i < 3; i++) {
            integrate(prevPosition[i], msensordata.getTime(), 0.1, msensordata.getData()[i]);
        }
        return new float[]{(float) prevPosition[0].pos, (float) prevPosition[1].pos, (float) prevPosition[2].pos};
    }

    public double getDeltaPos() {
        return deltaPos;
    }

    public Position integrate(Position position, long t, double dt, double acceleration) { //Heart of the RK4 integrator - I don't know what most of this is
        //save previous
        //double dt = (t - last_timestamp)/1000;
        last_timestamp = t;

        Derivative a = evaluate(position, t, 0, new Derivative(0, 0), acceleration);
        Derivative b = evaluate(position, t + dt * 0.5, dt * 0.5, a, acceleration);
        Derivative c = evaluate(position, t + dt * 0.5, dt * 0.5, b, acceleration);
        Derivative d = evaluate(position, t + dt, dt, c, acceleration);

        double dpdt = 1.0 / 6.0 * (a.dp + 2.0 * (b.dp + c.dp) + d.dp);
        double dvdt = 1.0 / 6.0 * (a.dv + 2.0 * (b.dv + c.dv) + d.dv);

        position.pos += dpdt * dt;
        position.v += dvdt * dt;

        deltaPos = position.pos - prevPos;
        prevPos = position.pos;
        Log.d("debug_rk4",String.valueOf(t)+" "+String.valueOf(dt)+" "+ String.valueOf(prevPosition[0].pos) + " " + String.valueOf(prevPosition[1].pos) + " " + String.valueOf(prevPosition[1].pos));

        last_timestamp = t;
        return position;
    }

    public double acceleration(Position position, double t) {        //Calculate all acceleration here - modify as needed
        double f = position.a;
        System.out.println(position.a);
        return f;
    }

    public Derivative evaluate(Position initial, double t, double dt, Derivative d, double acceleration) {   //Calculate new position based on change over time
        Position position = new Position(initial.pos + d.dp * dt, initial.v + d.dv * dt);       //New state influenced by derivatives of pos and v
        return new Derivative(position.v, acceleration);//acceleration(position, t));   //Calculate new derivative for new position
    }

    public class Position {
        public double pos = 6;      //position
        public double v = 6;        //velocity
        public double a = 6;        //acceleration

        public Position(Position mposition) {
            this.pos = mposition.pos;
            this.v = mposition.v;
        }

        public Position(double pos, double v) {
            this.pos = pos;
            this.v = v;
            a = 0;
        }
    }

    public class Derivative {
        public double dp;       //change in position
        public double dv;       //change in velocity

        public Derivative(double dp, double dv) {
            this.dp = dp;
            this.dv = dv;
        }
    }
}