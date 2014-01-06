package com.yasiralijaved.genradar.main;



import java.util.ArrayList;
import java.util.List;

import com.yasiralijaved.genradar.utils.LowPassFilter;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 * @author Yasir.Ali <ali.yasir0@gmail.com>
 *
 */
public class GenRadarManager implements SensorEventListener {

	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mMagnetometer;
	private Context mContext;

	private GenRadarPoint mCenterRadarPoint;
	private List<GenRadarPoint> mRadarPoints;

	private float[] mCompassVals;
	private float[] mAccelVals;

	// CHANGE THIS: minimum padding in pixel
	private static int MINIMUM_IMAGE_PADDING_IN_PX = 63;

	// formula for quarter PI
	private final double QUARTERPI = Math.PI / 4.0;

	double minXY [] = new double[]{-1,-1};

	double maxXY [] = new double[] {-1,-1};

	public static int MAP_WIDTH;
	public static int MAP_HEIGHT;
	public static int X_TRANSFORMATION;
	public static int Y_TRANSFORMATION;
	public static float POINT_RADIUS = 1.5f;

	// record the compass picture angle turned
	private float currentDegree = 0f;

	private GenRadarSprite mRadarSprite;
	private LinearLayout mRadarContainer;

	@SuppressWarnings("static-access")
	public GenRadarManager(Context context, LinearLayout radarContainer){
		mContext = context;
		mSensorManager = (SensorManager)context.getSystemService(context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mRadarContainer = radarContainer;

		MAP_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, context.getResources().
getDisplayMetrics());
		MAP_WIDTH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, context.getResources().
getDisplayMetrics());

	}

	public void setCenterRadarPoint(GenRadarPoint center){
		mCenterRadarPoint = center;
	}

	public void initRadarlayout() {

		mRadarPoints = new ArrayList<GenRadarPoint>();

		//mRadarPoints.add(new MyRadarPoint("Center Point", 33.683232, 72.988972, 0, 0, Radar.POINT_RADIUS, Color.RED));

		mRadarSprite = new GenRadarSprite(mContext, mRadarPoints);
		LayoutParams params = new LayoutParams(MAP_WIDTH, MAP_HEIGHT);
		mRadarSprite.setLayoutParams(params);

		mRadarContainer.addView(mRadarSprite);
		//child.invalidate();

	}

	public void registerListeners(){
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_FASTEST);
	}

	public void updateRadarWithPoints(List<GenRadarPoint> genRadarPoints){

		mRadarPoints = null;
		mRadarPoints = new ArrayList<GenRadarPoint>(genRadarPoints);

		mRadarPoints.add(0, mCenterRadarPoint);

		applyMercatorProjection();

		adjustForNegativeValues();

		//mRadarPoints = null;
		
		mRadarPoints = new ArrayList<GenRadarPoint>( applyCenterTransformation() );

		mRadarSprite.updateUIWithNewRadarPoints(mRadarPoints);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onSensorChanged(SensorEvent event) {

		if(mRadarContainer != null){
			// thank you http://www.codingforandroid.com/2011/01/using-orientation-sensors-simple.html
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
				mAccelVals = LowPassFilter.filter( event.values.clone(), mAccelVals );

			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
				mCompassVals = LowPassFilter.filter( event.values.clone(), mCompassVals );

			if (mAccelVals != null && mCompassVals != null) {
				float R[] = new float[9];
				float I[] = new float[9];
				boolean success = SensorManager.getRotationMatrix(R, I, mAccelVals, mCompassVals);

				if (success) {
					float orientation[] = new float[3];
					SensorManager.getOrientation(R, orientation);

					float degree = (float) Math.toDegrees( orientation[0]);

					// create a rotation animation (reverse turn degree degrees)
					RotateAnimation ra = new RotateAnimation(
							currentDegree, 
							-degree,
							Animation.RELATIVE_TO_SELF, 0.5f, 
							Animation.RELATIVE_TO_SELF, 0.5f);

					// how long the animation will take place
					ra.setDuration(210);

					// set the animation after the end of the reservation status
					ra.setFillAfter(true);

					// Start the animation
					mRadarContainer.startAnimation(ra);
					currentDegree = -degree;

				}
			}
		}
	}

	private void applyMercatorProjection() {

		// for every Point, convert the longitude/latitude to X/Y using Mercator projection formula
		for(int i = 0; i < mRadarPoints.size(); i++){

			//if(mRadarPoints.get(i).isVisibleOnRadar()){
				// convert to radian
				double longitude = mRadarPoints.get(i).getLng() * Math.PI / 180;
				double latitude = mRadarPoints.get(i).getLat() * Math.PI / 180;

				double x = longitude;
				double y = Math.log(Math.tan(QUARTERPI + 0.5 * latitude));

				// The reason we need to determine the min X and Y values is because in order to draw the map,
				// we need to offset the position so that there will be no negative X and Y values
				minXY[0] = (minXY[0] == -1) ? x : Math.min(minXY[0], x);
				minXY[1] = (minXY[1] == -1) ? y : Math.min(minXY[1], y);

				mRadarPoints.get(i).setX((float) x);
				mRadarPoints.get(i).setY((float) y);
			//}
		}
	}

	private void adjustForNegativeValues() {
		// re-adjust coordinate to ensure there are no negative values
		int size = mRadarPoints.size();

		for(int i = 0; i < size; i++){
			//if(mRadarPoints.get(i).isVisibleOnRadar()){
				mRadarPoints.get(i).setX( (float) (mRadarPoints.get(i).getX() - minXY[0]) );
				mRadarPoints.get(i).setY( (float) (mRadarPoints.get(i).getY() - minXY[1]) );

				// now, we need to keep track the max X and Y values
				maxXY[0] = (maxXY[0] == -1) ? mRadarPoints.get(i).getX() : Math.max(maxXY[0], mRadarPoints.get(i).getX());
				maxXY[1] = (maxXY[1] == -1) ? mRadarPoints.get(i).getY() : Math.max(maxXY[1], mRadarPoints.get(i).getY());
			//}
		}
	}

	private List<GenRadarPoint> applyCenterTransformation() {
		int paddingBothSides = MINIMUM_IMAGE_PADDING_IN_PX * 2;

		// the actual drawing space for the map on the image
		int mapWidth = (int) (MAP_WIDTH - paddingBothSides);
		int mapHeight = (int) (MAP_HEIGHT - paddingBothSides);

		// determine the width and height ratio because we need to magnify the map to fit into the given image dimension
		double mapWidthRatio = mapWidth / maxXY[0];
		double mapHeightRatio = mapHeight / maxXY[1];

		// using different ratios for width and height will cause the map to be stretched. So, we have to determine
		// the global ratio that will perfectly fit into the given image dimension
		double globalRatio = Math.min(mapWidthRatio, mapHeightRatio);

		// now we need to readjust the padding to ensure the map is always drawn on the center of the given image dimension
		double heightPadding = (MAP_HEIGHT - (globalRatio * maxXY[1])) / 2;
		double widthPadding = (MAP_WIDTH - (globalRatio * maxXY[0])) / 2;

		// for each point, draw on UI
		int size = mRadarPoints.size();

		for(int i = 0; i < size; i++){

			//if(mRadarPoints.get(i).isVisibleOnRadar()){
				int adjustedX = (int) (widthPadding + (mRadarPoints.get(i).getX() * globalRatio));

				// need to invert the Y since 0,0 starts at top left
				int adjustedY = (int) (MAP_HEIGHT - heightPadding - (mRadarPoints.get(i).getY() * globalRatio));

				mRadarPoints.get(i).setX(adjustedX);
				mRadarPoints.get(i).setY(adjustedY);

			//}
		}


		// Update X Coordinate
		X_TRANSFORMATION = (int) ((MAP_WIDTH / 2) - mCenterRadarPoint.getX());
		mCenterRadarPoint.setX( X_TRANSFORMATION + mCenterRadarPoint.getX() );

		Log.d("X_TRANSFORMATION",""+X_TRANSFORMATION);

		// Update Y Coordinate
		Y_TRANSFORMATION = (int) ((MAP_HEIGHT / 2) - mCenterRadarPoint.getY());
		mCenterRadarPoint.setY( Y_TRANSFORMATION + mCenterRadarPoint.getY() );

		Log.d("Y_TRANSFORMATION",""+Y_TRANSFORMATION);

		for(int i = 1; i < mRadarPoints.size(); i++){
			//if(mRadarPoints.get(i).isVisibleOnRadar()){
				mRadarPoints.get(i).setX(mRadarPoints.get(i).getX() + X_TRANSFORMATION);
				mRadarPoints.get(i).setY(mRadarPoints.get(i).getY() + Y_TRANSFORMATION);
			//}
		}


		List<GenRadarPoint> finalPointsToDraw = new ArrayList<GenRadarPoint>();
		// Remove the locations which are now out of map

		for(int i = 0; i < mRadarPoints.size(); i++){
			//if(mRadarPoints.get(i).isVisibleOnRadar()){
				Log.d("circle", mRadarPoints.get(i).toString());
				if(mRadarPoints.get(i).getX() > MAP_WIDTH || mRadarPoints.get(i).getY() > MAP_HEIGHT){
					Log.d("oops! this circle not in radar range", "removing this circle...");				
				}else{
					finalPointsToDraw.add(mRadarPoints.get(i));
				}
			//}
		}
		return finalPointsToDraw;
	}
	
	public void initAndUpdateRadarWithPoints(GenRadarPoint center, List<GenRadarPoint> genRadarPoints){
		this.setCenterRadarPoint(center);		
		this.initRadarlayout();
		this.registerListeners();		
		this.updateRadarWithPoints(genRadarPoints);
	}



	public void unregisterListeners(){
		// to stop the listener and save battery
		mSensorManager.unregisterListener(this);
	}

}
