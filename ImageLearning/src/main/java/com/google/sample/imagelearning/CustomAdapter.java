package com.google.sample.imagelearning;

/**
 * A custom adapter for views inside the list in the SelectLanguageDialog.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CustomAdapter  extends BaseAdapter{
    private int data;
    private Activity activity;
    private static LayoutInflater inflater=null;
    String[] langs;
    int[] langIconsIDs;
    private int selectedIndex;

    public CustomAdapter(Activity a, int data) {
        activity = a;
        this.data = data;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         langs = new String[]{"English","Italiano", "French", "Espanol", "German"};
        //TODO: change this icons with proper state flag.
        langIconsIDs = new int[]{R.mipmap.ic_launcher,R.mipmap.ic_launcher,R.mipmap.ic_launcher,R.mipmap.ic_launcher,R.mipmap.ic_launcher};
        selectedIndex = -1;
    }

    /**
     * Get the number of elements that are inside the list
     * @return
     */
    @Override
    public int getCount() {
        return data;
    }

    /**
     * Set selection on the list. This is used when an item on the list (of languages) is clicked.
     * @param ind
     */
    public void setSelectedIndex(int ind)
    {
        selectedIndex = ind;
        notifyDataSetChanged();
    }

    public Object getItem(int position) {
            return position;
        }

    public long getItemId(int position) {
        return position;
    }

    /**
     * Gets the current view. In particular, it checks if the view is selected and changes the background to it, to
     * make the user understand which language was selected.
     * @param position the position of the item
     * @param convertView the view of the current item
     * @param parent the parent group of views.
     * @return the view of the current item.
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        ViewHolder holder;
        if(convertView==null) {
            vi = inflater.inflate(R.layout.list_row, null);
            holder = new ViewHolder();

            holder.tv = (RelativeLayout) vi;

            vi.setTag(holder);
        }else {
            holder = (ViewHolder) vi.getTag();
        }

        TextView language = (TextView) vi.findViewById(R.id.language_textview);

        ImageView icon = (ImageView) vi.findViewById(R.id.list_image);

        language.setText(langs[position]);
        icon.setImageResource(langIconsIDs[position]);


        if(selectedIndex!= -1 && position == selectedIndex)
        {
            holder.tv.setBackgroundColor(Color.GREEN);
        }
        else
        {
            holder.tv.setBackgroundColor(Color.WHITE);
        }

        return vi;
    }

    /**
     * private class used to know if an item is selected.
     */
    private class ViewHolder{
        RelativeLayout tv;
    }
}

