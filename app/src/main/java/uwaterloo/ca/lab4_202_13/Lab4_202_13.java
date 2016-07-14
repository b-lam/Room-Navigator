package uwaterloo.ca.lab4_202_13;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import mapper.MapLoader;
import mapper.MapView;
import mapper.NavigationalMap;

public class Lab4_202_13 extends AppCompatActivity{

    Context context = this;
    LineGraphView graph;
    MapView mapView;
    NavigationalMap map;
    SensorManager sensorManager;
    MySensorEventListener eListener;
    Sensor linAccelerometer, accelerometer, magFieldSensor;
    Spinner spinner;
    ArrayAdapter<CharSequence> adapter;
    PathFinder pathFinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab4_202_13);

        LinearLayout l = (LinearLayout) findViewById(R.id.lLayout1);
        l.setOrientation(LinearLayout.VERTICAL);

        LinearLayout lH = new LinearLayout(getApplicationContext());
        lH.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p1.weight = 1;
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p2.weight = 2;

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        // Relative view to place the compass on top of the map
        RelativeLayout r = new RelativeLayout(getApplicationContext());

        //Creating line graph
        graph = new LineGraphView(getApplicationContext(), 100, Arrays.asList("x","y","z"));

        ImageView imgCompass = new ImageView(getApplicationContext());
        imgCompass.setImageResource(R.drawable.arrow);

        //imgCompass.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        imgCompass.setLayoutParams(new ActionBar.LayoutParams(200, ViewGroup.LayoutParams.WRAP_CONTENT));
        imgCompass.setAdjustViewBounds(true);

        ImageView imgArrow = new ImageView(getApplicationContext());
        imgArrow.setImageResource(R.drawable.arrow);
        imgArrow.setLayoutParams(new ActionBar.LayoutParams(200, ViewGroup.LayoutParams.WRAP_CONTENT));
        imgArrow.setAdjustViewBounds(true);

        Button btnClear = new Button(getApplicationContext());

        final EditText inputLength = new EditText(getApplicationContext());
        inputLength.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputLength.setHint("Input distance per step in meters");
        inputLength.setGravity(Gravity.CENTER);
        inputLength.setTextSize(14);
        inputLength.setLayoutParams(p1);


        Button btnSetStepDist = new Button(getApplicationContext());
        btnSetStepDist.setText("Apply");
        btnSetStepDist.setLayoutParams(p2);
        btnSetStepDist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(inputLength.getText().toString().length() == 0){
                    Toast.makeText(getApplicationContext(), "Please input distance per step!", Toast.LENGTH_SHORT).show();
                }else{
                    try{
                        Constant.stepDistance = Float.parseFloat(inputLength.getText().toString());
                        Toast.makeText(getApplicationContext(), "Distance set", Toast.LENGTH_SHORT).show();
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(inputLength.getWindowToken(), 0);
                    }catch (Exception e){
                        Toast.makeText(getApplicationContext(),"Please enter a number", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        Button btnStep = new Button(getApplicationContext());
        btnStep.setText("STEP");

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        spinner = new Spinner(getApplicationContext());
        adapter = ArrayAdapter.createFromResource(this, R.array.maps, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.getBackground().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getBaseContext(), parent.getItemAtPosition(position) + " selected", Toast.LENGTH_SHORT).show();
                String fileName = parent.getItemAtPosition(position) + ".svg";
                System.out.println(fileName);
                map = MapLoader.loadMap(getExternalFilesDir(null), fileName);
                mapView.setMap(map);
                pathFinder = new PathFinder(context, mapView, map, eListener);
                pathFinder.mapView.setOriginPoint(new PointF(0,0));
                pathFinder.mapView.setDestinationPoint(new PointF(0,0));
                pathFinder.mapView.setUserPoint(0,0);
                pathFinder.mapView.setUserPath(new ArrayList<PointF>());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        map = MapLoader.loadMap(getExternalFilesDir(null), "E2-3344.svg");
        mapView = new MapView(getApplicationContext(), 800, 620, 30, 28);

        registerForContextMenu(mapView);
        mapView.setMap(map);
        pathFinder = new PathFinder(this, mapView, map, eListener);

        //Creating labels for displaying current data
        TextView tvAcceleration = new TextView(getApplicationContext());
        TextView tvSteps = new TextView(getApplicationContext());
        TextView tvOrientation = new TextView(getApplicationContext());

        TextView[] currentSensorLabel = {tvAcceleration, tvSteps, tvOrientation};

        // Add the map and compass to the relative layout
        r.addView(mapView);
        r.addView(imgCompass);
        r.addView(imgArrow);

        // Place the compass on the top right of the screen
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)imgCompass.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams)imgArrow.getLayoutParams();
        params2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        //Add views to layout
        l.addView(r);
        l.addView(graph);
        l.addView(tvAcceleration);
        l.addView(tvSteps);
        l.addView(tvOrientation);
        l.addView(lH);
        lH.addView(inputLength);
        lH.addView(btnSetStepDist);
        l.addView(btnStep);
        l.addView(btnClear);
        l.addView(spinner);

        graph.setVisibility(View.VISIBLE);

        //Set text to black
        tvAcceleration.setTextColor(Color.BLACK);
        tvSteps.setTextColor(Color.BLACK);
        tvOrientation.setTextColor(Color.BLACK);
        inputLength.setTextColor(Color.BLACK);
        inputLength.setHintTextColor(Color.DKGRAY);

        btnClear.setText("Clear");

        //Creating senor manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //Getting sensors
        linAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //Creating sensor event listener
        //Register sensors with event listener
        eListener = new MySensorEventListener(this, graph, currentSensorLabel, btnClear, sensorManager, imgCompass, pathFinder, btnStep, imgArrow);
        sensorManager.registerListener(eListener, linAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(eListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(eListener, magFieldSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(eListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(eListener, linAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(eListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(eListener, magFieldSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        mapView.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        return super.onContextItemSelected(item) || mapView.onContextItemSelected(item);
    }

}
