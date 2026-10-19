package com.projectmister.game;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;
import android.widget.TextView;

/** Native, offline visual system. Lime marks the next action; club colours stay authentic. */
final class BossStyle {
    static final int BACKGROUND=0xff0d100b, SURFACE=0xff1b2016, RAISED=0xff292f21;
    static final int LINE=0xff353e2b, LIME=0xffc2ff37, INK=0xfff5f7ee, MUTED=0xffaeb7a2;
    static final int DARK=0xff141a0c, WARNING=0xffefc775, DANGER=0xffff9b8d;
    static final Typeface BODY=Typeface.create("sans-serif",Typeface.NORMAL);
    static final Typeface MEDIUM=Typeface.create("sans-serif-medium",Typeface.NORMAL);
    static final Typeface DISPLAY=Typeface.create("sans-serif",Typeface.BOLD);
    static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable shape(Context c,int color,float radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));
        if(color==SURFACE||color==RAISED)d.setStroke(dp(c,1),0x1affffff);
        return d;
    }
    static GradientDrawable hero(Context c){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff46552b,0xff242e18,0xff1a2014});
        d.setCornerRadius(dp(c,28));d.setStroke(dp(c,1),0x33c2ff37);return d;
    }
    static Drawable ripple(Context c,int color,float radius){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},shape(c,color==LIME?0xff4a5832:0xff20251a,radius));
        states.addState(new int[]{},shape(c,color,radius));
        return new RippleDrawable(ColorStateList.valueOf(color==LIME?0x26141a0c:0x33c2ff37),states,shape(c,0xffffffff,radius));
    }
    static ColorStateList buttonInk(boolean primary){return new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff838b76,primary?DARK:INK});}
    static void type(TextView text,int size,int color){
        text.setTextSize(size);text.setTextColor(color);text.setTypeface(size>=22?DISPLAY:size>=15?MEDIUM:BODY);
        text.setIncludeFontPadding(false);text.setLineSpacing(dp(text.getContext(),2),1.08f);
        text.setFontFeatureSettings("tnum");
    }
    static Drawable atmosphere(Context c){return new Atmosphere(dp(c,460));}
    private static final class Atmosphere extends Drawable {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final float radius;
        Atmosphere(float radius){this.radius=radius;}
        @Override public void draw(Canvas c){
            c.drawColor(BACKGROUND);Rect b=getBounds();
            p.setShader(new RadialGradient(b.right*.88f,b.top-radius*.12f,radius,new int[]{0xff364819,0xff1e2a12,0x000d100b},new float[]{0,.44f,1},Shader.TileMode.CLAMP));c.drawRect(b,p);p.setShader(null);
        }
        @Override public void setAlpha(int a){} @Override public void setColorFilter(ColorFilter f){} @Override public int getOpacity(){return PixelFormat.OPAQUE;}
    }
    /** Vector icons share one stroke; no emoji font or downloaded icon pack is required. */
    static final class Icon extends View {
        private final String name;private final int color;
        Icon(Context c,String name,int color){super(c);this.name=name;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float size=Math.min(getWidth(),getHeight());c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/24,size/24);drawIcon(c,name,color);c.restore();}
    }
    static Drawable icon(Context c,String name,int color,int size){
        Drawable d=new IconDrawable(name,color);d.setBounds(0,0,dp(c,size),dp(c,size));return d;
    }
    private static final class IconDrawable extends Drawable {
        final String name;final int color;IconDrawable(String n,int c){name=n;color=c;}
        @Override public void draw(Canvas c){Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);drawIcon(c,name,color);c.restore();}
        @Override public void setAlpha(int a){}@Override public void setColorFilter(ColorFilter f){}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private static void drawIcon(Canvas c,String n,int color){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStrokeWidth(1.65f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Path path=new Path();
        switch(n){
            case "back":c.drawLine(14,6,8,12,p);c.drawLine(8,12,14,18,p);break;
            case "arrow":c.drawLine(5,12,19,12,p);c.drawLine(13,6,19,12,p);c.drawLine(19,12,13,18,p);break;
            case "home":path.moveTo(3,10);path.lineTo(12,3);path.lineTo(21,10);path.lineTo(21,21);path.lineTo(15,21);path.lineTo(15,14);path.lineTo(9,14);path.lineTo(9,21);path.lineTo(3,21);path.close();c.drawPath(path,p);break;
            case "squad":c.drawCircle(9,7,3,p);c.drawArc(2,12,16,24,180,180,false,p);c.drawArc(12,4,20,10,-80,170,false,p);c.drawArc(14,12,23,23,-100,100,false,p);break;
            case "tactics":c.drawRoundRect(3,3,21,21,3,3,p);c.drawLine(3,12,21,12,p);c.drawCircle(12,12,3,p);c.drawLine(8,3,8,7,p);c.drawLine(8,7,16,7,p);c.drawLine(16,7,16,3,p);break;
            case "inbox":c.drawRoundRect(3,5,21,20,3,3,p);c.drawLine(3,8,12,14,p);c.drawLine(12,14,21,8,p);break;
            case "more":case "grid":for(int x=0;x<2;x++)for(int y=0;y<2;y++)c.drawRoundRect(4+x*10,4+y*10,10+x*10,10+y*10,1.5f,1.5f,p);break;
            case "bell":path.moveTo(5,17);path.lineTo(7,14);path.lineTo(7,9);path.cubicTo(7,2,17,2,17,9);path.lineTo(17,14);path.lineTo(19,17);path.close();c.drawPath(path,p);c.drawArc(9,17,15,22,20,140,false,p);break;
            case "search":c.drawCircle(10,10,6,p);c.drawLine(14.5f,14.5f,20,20,p);break;
            case "transfer":c.drawLine(4,7,20,7,p);c.drawLine(16,3,20,7,p);c.drawLine(20,7,16,11,p);c.drawLine(20,17,4,17,p);c.drawLine(8,13,4,17,p);c.drawLine(4,17,8,21,p);break;
            case "finance":c.drawRoundRect(3,5,21,20,3,3,p);c.drawLine(3,10,21,10,p);c.drawLine(15,15,18,15,p);break;
            case "stadium":c.drawOval(3,5,21,13,p);c.drawArc(3,11,21,22,0,180,false,p);c.drawLine(3,9,3,16,p);c.drawLine(21,9,21,16,p);c.drawLine(8,13,8,20,p);c.drawLine(16,13,16,20,p);break;
            case "calendar":c.drawRoundRect(3,5,21,21,3,3,p);c.drawLine(3,10,21,10,p);c.drawLine(8,3,8,7,p);c.drawLine(16,3,16,7,p);c.drawLine(8,15,10,15,p);c.drawLine(14,15,16,15,p);break;
            case "trophy":path.moveTo(7,3);path.lineTo(17,3);path.lineTo(17,10);path.cubicTo(17,18,7,18,7,10);path.close();c.drawPath(path,p);c.drawLine(12,16,12,21,p);c.drawLine(8,21,16,21,p);c.drawArc(2,4,9,12,80,200,false,p);c.drawArc(15,4,22,12,-100,200,false,p);break;
            case "training":c.drawLine(3,9,3,15,p);c.drawLine(6,6,6,18,p);c.drawLine(6,12,18,12,p);c.drawLine(18,6,18,18,p);c.drawLine(21,9,21,15,p);break;
            case "settings":c.drawCircle(12,12,4,p);for(int i=0;i<8;i++){c.save();c.rotate(i*45,12,12);c.drawLine(12,2,12,5,p);c.restore();}break;
            case "play":path.moveTo(8,5);path.lineTo(20,12);path.lineTo(8,19);path.close();c.drawPath(path,p);break;
            default:c.drawCircle(12,12,8,p);c.drawLine(9,12,15,12,p);c.drawLine(12,9,12,15,p);
        }
    }
    static String iconFor(String label){
        String s=label.toLowerCase(java.util.Locale.ROOT);
        if(s.contains("scout")||s.contains("search")||s.contains("database"))return "search";
        if(s.contains("tactic")||s.contains("formation")||s.contains("instruction"))return "tactics";
        if(s.contains("transfer")||s.contains("loan"))return "transfer";
        if(s.contains("financ")||s.contains("budget")||s.contains("contract"))return "finance";
        if(s.contains("stadium")||s.contains("facilit"))return "stadium";
        if(s.contains("fixture")||s.contains("calendar"))return "calendar";
        if(s.contains("history")||s.contains("league")||s.contains("competition"))return "trophy";
        if(s.contains("inbox")||s.contains("report"))return "inbox";
        if(s.contains("training"))return "training";
        if(s.contains("squad")||s.contains("staff")||s.contains("manager")||s.contains("board"))return "squad";
        if(s.contains("sound")||s.contains("setting")||s.contains("editor"))return "settings";
        return "grid";
    }
    /** Existing in-game stadium photography, cropped by the view; no extra image download. */
    static final class StadiumPhoto extends View {
        private static Bitmap photo;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        StadiumPhoto(Context c){super(c);if(photo==null)try(java.io.InputStream in=c.getAssets().open("stadium/ground.webp")){photo=BitmapFactory.decodeStream(in);}catch(java.io.IOException ignored){}setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();if(photo!=null){float scale=Math.max(w/photo.getWidth(),h/photo.getHeight());float dw=photo.getWidth()*scale,dh=photo.getHeight()*scale;c.drawBitmap(photo,null,new RectF((w-dw)/2,(h-dh)/2,(w+dw)/2,(h+dh)/2),p);}else c.drawColor(SURFACE);
            p.setShader(new LinearGradient(0,0,w*.4f,h,new int[]{0x22142008,0x99141a0c,0xff11170b},new float[]{0,.45f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        }
    }
    private BossStyle(){}
}
