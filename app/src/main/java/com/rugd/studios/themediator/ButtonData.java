package com.rugd.studios.themediator;

import android.view.View;

/**
 * Created by psybr on 2016/03/14.
 */
public class ButtonData
{
    private View view = null;
    private int pressed = 0;
    private int normal = 0;

    public ButtonData(View view, int pressed, int normal)
    {
        this.view = view;
        this.pressed = pressed;
        this.normal = normal;
    }

    public View getView(){return view;}
    public int getPressed(){return pressed;}
    public int getNormal(){return normal;}
}
