package com.projectmister.game;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.*;
import android.widget.*;

/** Shared, scroll-safe menus. Message and choices are siblings, never competing AlertDialog panels. */
final class BossDialog {
    private static final int SURFACE=0xff15212d, ROW=0xff223243, INK=0xfff0f5fa, MUTED=0xffb5c5d2, ACCENT=0xff58d9ad;
    static final class Builder {
        final Context context;
        CharSequence title="",message,positive,negative;
        CharSequence[] choices;
        DialogInterface.OnClickListener choiceListener,positiveListener,negativeListener;
        DialogInterface.OnMultiChoiceClickListener multiListener;
        boolean[] checkedItems;
        int checked=-1;
        View custom;
        Builder(Context c){context=c;}
        Builder setTitle(CharSequence v){title=v;return this;}
        Builder setMessage(CharSequence v){message=v;return this;}
        Builder setView(View v){custom=v;return this;}
        Builder setItems(CharSequence[] v,DialogInterface.OnClickListener l){choices=v;choiceListener=l;return this;}
        Builder setSingleChoiceItems(CharSequence[] v,int selected,DialogInterface.OnClickListener l){checked=selected;return setItems(v,l);}
        Builder setMultiChoiceItems(CharSequence[] v,boolean[] selected,DialogInterface.OnMultiChoiceClickListener l){choices=v;checkedItems=selected;multiListener=l;return this;}
        Builder setPositiveButton(CharSequence v,DialogInterface.OnClickListener l){positive=v;positiveListener=l;return this;}
        Builder setNegativeButton(CharSequence v,DialogInterface.OnClickListener l){negative=v;negativeListener=l;return this;}
        int dp(float n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
        TextView text(CharSequence value,int size,int color){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
        GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));return d;}
        Button button(CharSequence label,boolean primary){Button b=new Button(context);b.setText(label);b.setTextSize(14);b.setTextColor(primary?0xff09271f:INK);b.setAllCaps(false);b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);b.setPadding(dp(16),dp(10),dp(16),dp(10));b.setMinHeight(dp(52));b.setMinimumHeight(dp(52));b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x3358d9ad),shape(primary?ACCENT:ROW),null));return b;}
        Dialog show(){
            Dialog dialog=new Dialog(context);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            boolean landscape=context.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            LinearLayout root=new LinearLayout(context);root.setLayerType(View.LAYER_TYPE_SOFTWARE,null);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(landscape?10:16),dp(20),dp(landscape?10:16));root.setBackground(shape(SURFACE));
            LinearLayout brand=new LinearLayout(context);brand.setGravity(Gravity.CENTER_VERTICAL);brand.addView(BrandMark.view(context,landscape?24:36));
            TextView wordmark=text("BOSS XI  /  MANAGER DESK",11,ACCENT);wordmark.setPadding(dp(10),0,0,0);wordmark.setLetterSpacing(.06f);brand.addView(wordmark);root.addView(brand);
            TextView heading=text(title,landscape?18:20,INK);heading.setTypeface(Typeface.DEFAULT_BOLD);heading.setPadding(0,dp(landscape?8:12),0,dp(landscape?8:12));root.addView(heading);
            ScrollView scroll=new ScrollView(context);scroll.setClipToPadding(false);
            LinearLayout body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);
            if(message!=null){TextView copy=text(message,14,MUTED);copy.setLineSpacing(dp(3),1);copy.setPadding(0,0,0,dp(16));body.addView(copy);}
            if(custom!=null){if(custom.getParent() instanceof ViewGroup)((ViewGroup)custom.getParent()).removeView(custom);if(custom instanceof TextView){((TextView)custom).setTextColor(INK);((TextView)custom).setHintTextColor(MUTED);}body.addView(custom);}
            boolean compactChoices=landscape&&choices!=null&&choices.length<=3&&multiListener==null;
            if(compactChoices)for(CharSequence label:choices)if(label.length()>20)compactChoices=false;
            LinearLayout choiceArea=new LinearLayout(context);choiceArea.setOrientation(compactChoices?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);body.addView(choiceArea);
            if(choices!=null)for(int i=0;i<choices.length;i++){
                final int index=i;
                View row;
                if(multiListener!=null){CheckBox c=new CheckBox(context);c.setText(choices[i]);c.setTextColor(INK);c.setTextSize(14);c.setButtonTintList(ColorStateList.valueOf(ACCENT));c.setMinHeight(dp(52));c.setChecked(checkedItems[i]);c.setOnCheckedChangeListener((v,on)->{checkedItems[index]=on;multiListener.onClick(dialog,index,on);});row=c;}
                else{Button b=button((i==checked?"✓  ":"")+choices[i],false);if(i==checked)b.setTextColor(ACCENT);b.setOnClickListener(v->{dialog.dismiss();if(choiceListener!=null)choiceListener.onClick(dialog,index);});row=b;}
                LinearLayout.LayoutParams lp=compactChoices?new LinearLayout.LayoutParams(0,-2,1):new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);if(compactChoices&&i>0)lp.leftMargin=dp(8);choiceArea.addView(row,lp);
            }
            root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
            LinearLayout footer=new LinearLayout(context);footer.setGravity(Gravity.END);footer.setPadding(0,dp(10),0,0);
            if(negative!=null){Button b=button(negative,false);b.setGravity(Gravity.CENTER);b.setOnClickListener(v->{dialog.dismiss();if(negativeListener!=null)negativeListener.onClick(dialog,DialogInterface.BUTTON_NEGATIVE);});footer.addView(b,new LinearLayout.LayoutParams(0,-2,1));}
            if(positive!=null){Button b=button(positive,true);b.setGravity(Gravity.CENTER);b.setOnClickListener(v->{dialog.dismiss();if(positiveListener!=null)positiveListener.onClick(dialog,DialogInterface.BUTTON_POSITIVE);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=negative==null?0:dp(8);footer.addView(b,lp);}
            if(negative==null&&positive==null){Button b=button("Close",false);b.setOnClickListener(v->dialog.dismiss());footer.addView(b);}
            root.addView(footer);dialog.setContentView(root);
            Window window=dialog.getWindow();if(window!=null){window.setWindowAnimations(0);window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
            dialog.show();
            if(window!=null){int width=Math.min(dp(520),context.getResources().getDisplayMetrics().widthPixels-dp(32));window.setLayout(width,-2);
                // Re-evaluate after keyboard/inset changes; fixed footer remains reachable.
                root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{android.graphics.Rect visible=new android.graphics.Rect();root.getWindowVisibleDisplayFrame(visible);int cap=(int)(visible.height()*.88f);int used=root.getPaddingTop()+root.getPaddingBottom()+brand.getHeight()+heading.getHeight()+footer.getHeight();int available=Math.max(dp(52),cap-used);body.measure(View.MeasureSpec.makeMeasureSpec(Math.max(dp(100),width-dp(40)),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int desired=Math.min(body.getMeasuredHeight(),available);if(scroll.getLayoutParams().height!=desired){scroll.getLayoutParams().height=desired;scroll.requestLayout();}});
            }
            return dialog;
        }
    }
}
