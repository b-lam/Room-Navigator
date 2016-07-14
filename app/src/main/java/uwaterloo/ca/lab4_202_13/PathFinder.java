package uwaterloo.ca.lab4_202_13;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import mapper.InterceptPoint;
import mapper.LineSegment;
import mapper.MapView;
import mapper.NavigationalMap;
import mapper.PositionListener;

/**
 * Created by Brandon on 7/5/2016.
 */
public class PathFinder{

    Context context;
    MapView mapView;
    List<InterceptPoint> interceptionUserDest = new ArrayList<>();

    List<PointF> userPath = new ArrayList<>();
    NavigationalMap map;
    PointF a,b,s,t;

    boolean watchdogBreak = false;

    final MySensorEventListener sensorEventListener;

    private static final String TAG = "Lab4_202_13"; //For debugging purposes

    PositionListener positionListener = new PositionListener() {
        @Override
        public void originChanged(MapView source, PointF loc) {
            source.setUserPoint(loc);
            calculateNewPath();
        }

        @Override
        public void destinationChanged(MapView source, PointF dest) {
            source.setDestinationPoint(dest);
            calculateNewPath();
        }
    };

    public PathFinder(Context context, MapView mapView, NavigationalMap map, final MySensorEventListener sensorEventListener){
        this.context = context;
        this.mapView = mapView;
        this.map = map;
        this.sensorEventListener = sensorEventListener;
        mapView.addListener(positionListener);
    }

    public void calculateNewPath(){
        userPath.clear();
        s = mapView.getOriginPoint();
        t = mapView.getDestinationPoint();
        List<PointF> ab;
        List<PointF> cd;
        interceptionUserDest = map.calculateIntersections(s,t);
        userPath.add(s);
        if(interceptionUserDest.isEmpty()){
            userPath.add(t);
            mapView.setUserPath(userPath);
        }else{
            ab = calculateAB();
            if(ab.get(0).equals(0,0) && ab.get(1).equals(0,0)){
                userPath.clear();
                Toast.makeText(context, "No path found", Toast.LENGTH_SHORT).show();
            }else{
                cd = findShortestPath();
                ab = cd;
                userPath.add(ab.get(0));
                userPath.add(ab.get(1));
                userPath.add(t);
            }
        }
        mapView.setUserPath(userPath);
    }

    public List<PointF> calculateAB(){
        watchdogBreak = false;
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                watchdogBreak = true;
            }
        };
        timer.schedule(timerTask, 1000);

        ArrayList<PointF> connect = new ArrayList<>();
        a = new PointF();
        b = new PointF();
        for(float n = -2.1f; n <= 2.1f; n+=0.1f) {
            if(watchdogBreak == true){
                return Arrays.asList(new PointF(0,0), new PointF(0,0));
            }
            a.x = s.x + n;
            b.x = t.x + n;
            a.y = s.y;
            b.y = s.y + n;
            if(!map.calculateIntersections(b,t).isEmpty()){
                a.y = t.y;
                b.y = t.y + n;
            }
            while (map.calculateIntersections(a, s).isEmpty() && map.calculateIntersections(b, t).isEmpty()) {
                if (map.calculateIntersections(a, b).isEmpty()) {
                    connect.add(a);
                    connect.add(b);
                    Log.d("Lab4_202_13", a.toString() + ", " + b.toString());
                    return connect;
                }
                a.y = a.y + 0.05f;
                b.y = b.y + 0.05f;
            }
            a.y = s.y;
            b.y = s.y + n;
            while (map.calculateIntersections(a, s).isEmpty() && map.calculateIntersections(b, t).isEmpty()) {
                if (map.calculateIntersections(a, b).isEmpty()) {
                    connect.add(a);
                    connect.add(b);
                    return connect;
                }
                a.y = a.y - 0.05f;
                b.y = b.y - 0.05f;
            }
        }
        return Arrays.asList(new PointF(0,0), new PointF(0,0));
    }

    public List<PointF> findShortestPath(){
        watchdogBreak = false;
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                watchdogBreak = true;
            }
        };
        timer.schedule(timerTask, 1000);

        ArrayList<PointF> connect = new ArrayList<>();
        PointF c = a;
        PointF d = b;
        while(map.calculateIntersections(s,c).isEmpty() && map.calculateIntersections(c,d).isEmpty() && c.x != d.x){
            if(watchdogBreak == true){
                return Arrays.asList(a,b);
            }
            c.x = c.x + (b.x - a.x)/200;
            c.y = a.y + ((b.y - a.y)/(b.x - a.x))*(c.x - a.x);
        }
        while(map.calculateIntersections(t,d).isEmpty() && map.calculateIntersections(c,d).isEmpty() && c.x != d.x){
            if(watchdogBreak == true){
                return Arrays.asList(a,b);
            }
            d.x = d.x + (a.x - b.x)/200;
            d.y = a.y + ((b.y - a.y)/(b.x - a.x))*(d.x - a.x);
        }
        c.x = c.x - (b.x - a.x)/200;
        c.y = a.y + ((b.y - a.y)/(b.x - a.x))*(c.x - a.x);
        d.x = d.x - (a.x - b.x)/200;
        d.y = a.y + ((b.y - a.y)/(b.x - a.x))*(d.x - a.x);
        connect.add(c);
        connect.add(d);
        return connect;
    }

    public void calculateNewPathWall(){
        PointF nextPoint;
        List<LineSegment> wallThroughPt;
        userPath.clear();
        s = mapView.getUserPoint();
        t = mapView.getDestinationPoint();
        interceptionUserDest = map.calculateIntersections(s,t);
        userPath.add(s);
        if(interceptionUserDest.isEmpty()){
            userPath.add(t);
        }else {

            nextPoint = mapView.getUserPoint();
            userPath.add(interceptionUserDest.get(0).getPoint());
            for(int i = 0; i < 2; i++){
                interceptionUserDest = map.calculateIntersections(nextPoint, t);
                nextPoint = interceptionUserDest.get(0).getLine().end;
                userPath.add(nextPoint);

                Log.d("Intercept", interceptionUserDest.get(0).getPoint().toString());
                wallThroughPt = map.getGeometryAtPoint(nextPoint);
                if (!wallThroughPt.isEmpty()){
                    nextPoint = wallThroughPt.get(1).end;
                    userPath.add(nextPoint);
                    System.out.println("ADDED POINT!");
                }
            }
        }
        mapView.setUserPath(userPath);
    }
}
