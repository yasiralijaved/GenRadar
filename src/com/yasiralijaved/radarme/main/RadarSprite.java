package com.yasiralijaved.radarme.main;

/**
 * @author Yasir.Ali <ali.yasir0@gmail.com>
 *
 */

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class RadarSprite extends View {
	private Paint mPaint;
    private List<RadarPoint> mRadarPoints;
    
    public RadarSprite(Context context, List<RadarPoint> radarPoints) {
        super(context);
        this.mRadarPoints = new ArrayList<RadarPoint>(radarPoints);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);     
        for(int i = 0; i < mRadarPoints.size(); i++){
        	mPaint.setColor(mRadarPoints.get(i).getColor());
        	canvas.drawCircle(mRadarPoints.get(i).getX(), mRadarPoints.get(i).getY(), mRadarPoints.get(i).getRaduis(), mPaint);
        }        
    }
    
    public void updateUIWithNewRadarPoints(List<RadarPoint> radarPoints){
    	this.mRadarPoints = new ArrayList<RadarPoint>(radarPoints);
    	this.invalidate();
    }
}