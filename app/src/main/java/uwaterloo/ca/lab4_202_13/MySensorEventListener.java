package uwaterloo.ca.lab4_202_13;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import mapper.InterceptPoint;

/**
 * Created by Brandon on 5/17/2016.
 */

public class MySensorEventListener implements SensorEventListener{

    SensorManager sensorManager;
    TextView[] output;
    LineGraphView graph;
    float[] gravity = {0,0,0}; float[] geomagnetic = {0,0,0}; float[] orientation = {0,0,0};
    String acceleration;
    Button clear, step;
    int stepCount = 0, state = 0;
    float accZPre = 0, accYPre = 0, accZPeak = 0, accYPeak = 0, currentDegree = 0f, degree = 0f, displacementNS = 0, displacementWE = 0, currentDegreeArrow = 0f;
    float[] R = new float[9]; float[] I = new float[9];
    ImageView imgCompass, imgArrow;
    final PathFinder pathFinder;
    Context context;
    PointF node;
    PointF previousNode = new PointF();
    AlertDialog.Builder dialogBuilder;


    private static final String TAG = "Lab4_202_13"; //For debugging purposes

    public MySensorEventListener(final Context context, LineGraphView g, TextView[] current, Button btn, SensorManager sensorManager, ImageView imgCompass, final PathFinder pathFinder, Button btnStep, ImageView imgArrow){
        this.context = context;
        output = current;
        graph = g;
        clear = btn;
        this.sensorManager = sensorManager;
        this.imgCompass = imgCompass;
        this.pathFinder = pathFinder;
        step = btnStep;
        this.imgArrow = imgArrow;

        clear.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v){

                pathFinder.mapView.setUserPoint(pathFinder.mapView.getOriginPoint());

                if(!pathFinder.userPath.isEmpty()){
                    previousNode = pathFinder.a;
                    node = new PointF();
                }

                //Clear the graph
                graph.purge();

                //Reset step counter
                stepCount = 0;
                displacementNS = 0;
                displacementWE = 0;
                output[2].setText("Steps taken: " + stepCount + "   N/S: " + displacementNS + "   E/W: " + displacementWE);
            }
        });

        step.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PointF nextStep = new PointF();
                List<InterceptPoint> interceptionUserNextStep = new ArrayList<>();
                float deltaX, deltaY;
                deltaY = (float)(Math.cos(Math.toRadians(degree)))*Constant.stepDistance;
                deltaX = (float)(Math.sin(Math.toRadians(degree)))*Constant.stepDistance;
                nextStep.x = (pathFinder.mapView.getUserPoint().x + deltaX);
                nextStep.y = (pathFinder.mapView.getUserPoint().y - deltaY);
                interceptionUserNextStep = pathFinder.map.calculateIntersections(pathFinder.mapView.getUserPoint(), nextStep);
                if(interceptionUserNextStep.isEmpty()) {
                    pathFinder.mapView.setUserPoint(nextStep);
                    stepCount++;
                    displacementNS += deltaY*-1;
                    displacementWE += deltaX;
                    if (Math.abs(pathFinder.mapView.getUserPoint().x - pathFinder.mapView.getDestinationPoint().x) < 1 && Math.abs(pathFinder.mapView.getUserPoint().y - pathFinder.mapView.getDestinationPoint().y) < 1 && !pathFinder.userPath.isEmpty()) {
//                        Toast.makeText(context, "Destination Reached!", Toast.LENGTH_SHORT).show();
                        createDialog();
                    }
                }else{
                    Toast.makeText(context, "Step not counted!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void onAccuracyChanged(Sensor s, int i) {}

    public void onSensorChanged(SensorEvent event){
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){

            avgValues(gravity, event.values);

        }
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            //Low-pass filter
            float[] smoothAcc = new float[]{0, 0, 0};
            for (int i = 0; i < 3; i++) {
                smoothAcc[i] += (event.values[i] - smoothAcc[i] / 100);
            }

            //acceleration = "Acceleration : " + String.format("(%.2f, %.2f, %.2f)", event.values[0], event.values[1], event.values[2]);
            //output[0].setText(acceleration);
            graph.addPoint(smoothAcc);

            //FSM for step counting
            if(pedometer(smoothAcc)){
                PointF nextStep = new PointF();
                List<InterceptPoint> interceptionUserNextStep = new ArrayList<>();
                float deltaX, deltaY;
                deltaY = (float)(Math.cos(Math.toRadians(degree)))*Constant.stepDistance;
                deltaX = (float)(Math.sin(Math.toRadians(degree)))*Constant.stepDistance;
                nextStep.x = (pathFinder.mapView.getUserPoint().x + deltaX);
                nextStep.y = (pathFinder.mapView.getUserPoint().y - deltaY);
                interceptionUserNextStep = pathFinder.map.calculateIntersections(pathFinder.mapView.getUserPoint(), nextStep);
                if(interceptionUserNextStep.isEmpty()){
                    pathFinder.mapView.setUserPoint(nextStep);
                    stepCount++;
                    displacementNS += deltaY*-1;
                    displacementWE += deltaX;
                    if(Math.abs(pathFinder.mapView.getUserPoint().x - pathFinder.mapView.getDestinationPoint().x) < 1 && Math.abs(pathFinder.mapView.getUserPoint().y - pathFinder.mapView.getDestinationPoint().y) < 1 && !pathFinder.userPath.isEmpty()){
//                        Toast.makeText(context, "Destination Reached!", Toast.LENGTH_SHORT).show();
                        createDialog();
                    }
                }else{
                    Toast.makeText(context, "Step not counted!", Toast.LENGTH_SHORT).show();
//                    pathFinder.mapView.setUserPoint(interceptionUserNextStep.get(0).getPoint());
                }
//                pathFinder.calculateNewPath();
            }

            if(!pathFinder.userPath.isEmpty()){
                output[0].setText(navigateUser());
            }
            output[1].setText("Steps taken: " + stepCount + "   N/S: " + String.format("%.2f",displacementNS) + "   E/W: " + String.format("%.2f",displacementWE));
        }

        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

            avgValues(geomagnetic, event.values);

        }

        if(gravity != null && geomagnetic != null){
            if(sensorManager.getRotationMatrix(R,I, gravity, geomagnetic)){
                sensorManager.getOrientation(R, orientation);

                degree = (float)Math.toDegrees(orientation[0])+22.5f;
                degree = (degree+360)%360;

                output[2].setText("Heading: " + Math.round(degree) + "°");
//                output[3].setText("Azimuth: " + degree + "\nPitch: " + 57.3*orientation[1] + "\nRoll: " + 57.3*orientation[2]);

                RotateAnimation ra = new RotateAnimation(currentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                ra.setDuration(10);
                ra.setFillAfter(true);
                imgCompass.startAnimation(ra);
                currentDegree = -degree;
            }
        }
    }


    public boolean pedometer(float[] input){

                switch(state)
                {
                    case Constant.STATE_INITIAL:
                        if(input[2] > accZPre){
                            state = Constant.STATE_RISE;
                        }else{
                            state = Constant.STATE_DROP;
                        }
                        break;

                    case Constant.STATE_DROP:
                        if(input[2] > accZPre) {
                            state = Constant.STATE_RISE;
                        }
                        break;

                    case Constant.STATE_RISE:
                        if (input[2] < accZPre) {
                            accZPeak = accZPre;
                            accYPeak = accYPre;
                            state = Constant.STATE_PEAK;
                        }
                        break;

                    case Constant.STATE_PEAK:
                        //Log.d(TAG, "Z = " + accZPeak);
                        //Log.d(TAG, "Y = " + accYPeak);

                        if(accZPeak > 0.7 && accZPeak < 4 && Math.abs(accYPeak) < 4) {
                            state = Constant.STATE_DROP;
                            accYPre = input[1];
                            accZPre = input[2];
                            return true;
                        }
                        state = Constant.STATE_DROP;
                        break;
                }

                accYPre = input[1];
                accZPre = input[2];

                return false;

    }

    public void avgValues(float[] preVal, float[] newVal){
        preVal[0] = Constant.ALPHA * preVal[0] + (1-Constant.ALPHA) * newVal[0];
        preVal[1] = Constant.ALPHA * preVal[1] + (1-Constant.ALPHA) * newVal[1];
        preVal[2] = Constant.ALPHA * preVal[2] + (1-Constant.ALPHA) * newVal[2];
    }

    public void getAbsMax(float[] sensorVal, float[] maxVal){
        if(Math.abs(sensorVal[0]) > Math.abs(maxVal[0])){
            maxVal[0] = sensorVal[0];
        }
        if(Math.abs(sensorVal[1]) > Math.abs(maxVal[1])){
            maxVal[1] = sensorVal[1];
        }
        if(Math.abs(sensorVal[2]) > Math.abs(maxVal[2])){
            maxVal[2] = sensorVal[2];
        }
    }

    public String navigateUser(){
        String directions = "";
        if(pathFinder.map.calculateIntersections(pathFinder.mapView.getUserPoint(), pathFinder.mapView.getDestinationPoint()).isEmpty()){
            node = pathFinder.mapView.getDestinationPoint();
        }else if(pathFinder.map.calculateIntersections(pathFinder.mapView.getUserPoint(), pathFinder.b).isEmpty()){
            node = pathFinder.b;
        }else if(pathFinder.map.calculateIntersections(pathFinder.mapView.getUserPoint(), pathFinder.a).isEmpty()){
            node = pathFinder.a;
        }else{
            node = previousNode;
        }

        previousNode = node;

        if(pathFinder.mapView.getOriginPoint().x < pathFinder.mapView.getDestinationPoint().x){
            if(pathFinder.mapView.getUserPoint().y < node.y){
                if(pathFinder.mapView.getUserPoint().x < node.x){
                    directions = "Walk " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().y - node.y)) + " m South and " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().x - node.x)) + " m East";
                }else{
                    directions = "Walk " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().y - node.y)) + " m South and " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().x - node.x)) + " m West";

                }
            }else{
                if(pathFinder.mapView.getUserPoint().x < node.x){
                    directions = "Walk " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().y - node.y)) + " m North and " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().x - node.x)) + " m East";
                }else{
                    directions = "Walk " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().y - node.y)) + " m North and " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().x - node.x)) + " m West";
                }
            }
        }else {
            if (pathFinder.mapView.getUserPoint().y < node.y) {
                if (pathFinder.mapView.getUserPoint().x > node.x) {
                    directions = "Walk " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().y - node.y)) + " m South and " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().x - node.x)) + " m West";
                } else {
                    directions = "Walk " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().y - node.y)) + " m South and " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().x - node.x)) + " m East";
                }
            } else {
                if (pathFinder.mapView.getUserPoint().x < node.x) {
                    directions = "Walk " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().y - node.y)) + " m North and " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().x - node.x)) + " m East";
                } else {
                    directions = "Walk " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().y - node.y)) + " m North and " + String.format("%.2f",Math.abs(pathFinder.mapView.getUserPoint().x - node.x)) + " m West";
                }
            }
        }

        RotateAnimation ra = new RotateAnimation(currentDegreeArrow, ((-1*getAngle(pathFinder.mapView.getUserPoint(), node))-90), Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(50);
        ra.setFillAfter(true);
        imgArrow.startAnimation(ra);
        currentDegreeArrow = ((-1*getAngle(pathFinder.mapView.getUserPoint(), node))-90);

        return directions;
    }

    public float getAngle(PointF location, PointF target) {
        float angle = (float) Math.toDegrees(Math.atan2(target.y - location.y, target.x - location.x));

        return angle;
    }

    public void createDialog(){
        dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage("You have reached your destination!");
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }
}
