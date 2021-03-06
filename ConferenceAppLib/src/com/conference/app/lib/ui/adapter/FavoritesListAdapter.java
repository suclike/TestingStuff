package com.conference.app.lib.ui.adapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.conference.app.lib.R;
import com.conference.app.lib.database.DatabaseAdapter;
import com.conference.app.lib.database.tables.SessionTable;
import com.conference.app.lib.ui.Favorites;

public class FavoritesListAdapter extends CursorAdapter {
    private static final String TAG = FavoritesListAdapter.class.getName();
    private static final boolean DEBUG = false;

    private static final int MAX_NAME_LENGTH = 47;

    private final Map<Integer, String> timesCategories = new TreeMap<Integer, String>();
    private final Favorites activity;
    private final int resId;
    private final DatabaseAdapter dbAdapter;
    private final Locale language;
    private final SQLiteDatabase db;
    private Cursor cursor;

    private static Bitmap cacheSessionImg;
    private static Bitmap cacheWorkshopImg;
    private static Bitmap cacheKeyNoteImg;

    public FavoritesListAdapter(final Favorites ctx, final Cursor cursor, final SQLiteDatabase db) {
        super(ctx, cursor);

        this.db = db;
        this.activity = ctx;

        // new TrackColorLoader().execute();

        this.cursor = cursor;
        this.resId = R.layout.schedule_item;
        this.dbAdapter = new DatabaseAdapter(ctx);
        final String langCode = ctx.getString(R.string.lang_code);
        if ("DE".equals(langCode)) {
            language = Locale.GERMAN;
        } else {
            language = Locale.US;
        }
    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);

        if (!this.cursor.isClosed()) {
            this.cursor.close();
        }

        this.cursor = cursor;
    }

    @Override
    public void notifyDataSetChanged() {
        timesCategories.clear();
        super.notifyDataSetChanged();
        if (getCursor().getCount() == 0) {
            activity.showNoFavoritesText();
        }
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View rowView;
        if (convertView == null) {
            rowView = newView(this.activity, cursor, parent);
        } else {
            rowView = convertView;
        }

        cursor.moveToPosition(position);

        try {
            final ViewHolder viewHolder = (ViewHolder) rowView.getTag();

            final String date = cursor.getString(cursor.getColumnIndex(SessionTable.START_DATE));
            final String day = new SimpleDateFormat("EEE dd/MM/yyyy", language).format(new SimpleDateFormat("dd.MM.yyyy HH:mm",
                    Locale.GERMAN).parse(date));

            if ((timesCategories.containsValue(day) && timesCategories.get(position) != null && timesCategories.get(position).equals(
                    day))
                    || !timesCategories.containsValue(day)) {
                viewHolder.timeRow.setVisibility(View.VISIBLE);
                viewHolder.timeDivider.setVisibility(View.VISIBLE);
                viewHolder.timeRow.setText(day);

                timesCategories.put(position, day);

                bindView(rowView, this.activity, cursor);
            } else {
                viewHolder.timeRow.setVisibility(View.GONE);
                viewHolder.timeDivider.setVisibility(View.GONE);

                bindView(rowView, this.activity, cursor);
            }
        } catch (ParseException e) {
            Log.e("ERROR", e.getMessage(), e);
        }

        return rowView;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        final String uniqueSessionId = cursor.getString(cursor.getColumnIndex(SessionTable.UNIQUE_ID));
        final String sessionName = cursor.getString(cursor.getColumnIndex(SessionTable.SHORT_NAME));

        if (sessionName != null && !"null".equals(sessionName) && sessionName.length() > 0) {
            viewHolder.title.setText(sessionName);
        } else {
            final String title = cursor.getString(cursor.getColumnIndex(SessionTable.NAME));
            if (title.length() > MAX_NAME_LENGTH) {
                viewHolder.title.setText(title.substring(0, MAX_NAME_LENGTH).concat("..."));
            } else {
                viewHolder.title.setText(title);
            }
        }

        viewHolder.star.setChecked(cursor.getInt(cursor.getColumnIndex(SessionTable.FAVORITE)) != 0);
        viewHolder.star.setTag(uniqueSessionId);
        viewHolder.star.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                if (buttonView.isPressed()) {
                    final String uniqueSessionId = (String) buttonView.getTag();
                    dbAdapter.setFavorite(uniqueSessionId, isChecked);

                    final Cursor cursor = db.query(SessionTable.TABLE_NAME, new String[] { SessionTable.ID, SessionTable.TYPE,
                            SessionTable.NAME, SessionTable.SHORT_NAME, SessionTable.UNIQUE_ID, SessionTable.ROOM_NAME,
                            SessionTable.FAVORITE, SessionTable.START_DATE, SessionTable.END_DATE }, SessionTable.FAVORITE + "=1",
                            null, null, null, SessionTable.START_DATE + ", " + SessionTable.NAME + " COLLATE NOCASE");

                    FavoritesListAdapter.this.notifyDataSetInvalidated();
                    FavoritesListAdapter.this.changeCursor(cursor);
                    FavoritesListAdapter.this.notifyDataSetChanged();

                }
            }
        });

        String session = "SESSION".toLowerCase();
        String workshop = "WORKSHOP".toLowerCase();
        String keynote = "KEYNOTE".toLowerCase();
        String sessionType = cursor.getString(cursor.getColumnIndex(SessionTable.TYPE)).toLowerCase();
        if (session.toLowerCase().equals(sessionType)) {
            if (cacheSessionImg == null) {
                cacheSessionImg = BitmapFactory.decodeResource(activity.getResources(), R.drawable.session_icon);
            }
            viewHolder.trackImg.setImageBitmap(cacheSessionImg);
        } else if (workshop.toLowerCase().equals(sessionType)) {
            if (cacheWorkshopImg == null) {
                cacheWorkshopImg = BitmapFactory.decodeResource(activity.getResources(), R.drawable.workshop_icon);
            }
            viewHolder.trackImg.setImageBitmap(cacheWorkshopImg);
        } else if (keynote.toLowerCase().equals(sessionType)) {
            if (cacheKeyNoteImg == null) {
                cacheKeyNoteImg = BitmapFactory.decodeResource(activity.getResources(), R.drawable.keynote_icon);
            }
            viewHolder.trackImg.setImageBitmap(cacheKeyNoteImg);
        }

//        String room = cursor.getString(cursor.getColumnIndex(SessionTable.ROOM_NAME));
//        viewHolder.row2text.setText(context.getString(R.string.room_label) + " " + room);
        viewHolder.row2text.setText(getDateRowText(cursor)); //Eddie Li Change to date

        // viewHolder.trackColor.setBackgroundColor(Color.parseColor(dbAdapter.getTrackColorByUniqueSessionId(cursor
        // .getString(cursor.getColumnIndex(SessionTable.UNIQUE_ID)))));
        // viewHolder.trackColor.setVisibility(View.VISIBLE);
    }
    
    private String getDateRowText(final Cursor cursor) {
        String s = "";
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        try {
            String startdate = cursor.getString(cursor.getColumnIndex(SessionTable.START_DATE));
            String enddate = cursor.getString(cursor.getColumnIndex(SessionTable.END_DATE));
            String dateOnly = dateFormat.format(dateTimeFormat.parse(startdate));
            String starttime = timeFormat.format(dateTimeFormat.parse(startdate));
            String endtime = timeFormat.format(dateTimeFormat.parse(enddate));

            s = dateOnly + " / " + starttime + " - " + endtime;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final LayoutInflater inflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View rowView = inflater.inflate(resId, null, true);
        final ViewHolder viewHolder = new ViewHolder();

        viewHolder.title = (TextView) rowView.findViewById(R.id.scheduleItemSessionTitle);
        // viewHolder.trackColor = rowView.findViewById(R.id.scheduleItemTrackColor);
        // viewHolder.sessionRow = rowView.findViewById(R.id.scheduleItemSessionRow);
        viewHolder.timeRow = (TextView) rowView.findViewById(R.id.scheduleItemTimeRow);
        viewHolder.star = (ToggleButton) rowView.findViewById(R.id.scheduleItemFav);
        viewHolder.trackImg = (ImageView) rowView.findViewById(R.id.scheduleItemSessionImg);
        viewHolder.timeDivider = rowView.findViewById(R.id.scheduleItemTimeDivider);
        viewHolder.row2text = (TextView) rowView.findViewById(R.id.row2text);

        rowView.setTag(viewHolder);

        return rowView;
    }

    static class ViewHolder {
        public TextView timeRow;
        // public View sessionRow;
        public View timeDivider;

        public ToggleButton star;
        public ImageView trackImg;
        public TextView title;
        public TextView row2text;
        // public View trackColor;
    }

    // private class TrackColorLoader extends AsyncTask<Void, Void, Void> {
    // @Override
    // protected Void doInBackground(final Void... params) {
    // final DatabaseHelper helper = new DatabaseHelper(activity);
    // final SQLiteDatabase db = helper.getReadableDatabase();
    // final Cursor cursor = db.query(SessionTable.TABLE_NAME, new String[] { SessionTable.ID,
    // SessionTable.UNIQUE_ID }, null, null, null, null, null);
    // final DatabaseAdapter dbAdapter = new DatabaseAdapter(activity);
    //
    // while (cursor.moveToNext()) {
    // dbAdapter
    // .getTrackColorByUniqueSessionId(cursor.getString(cursor.getColumnIndex(SessionTable.UNIQUE_ID)));
    // }
    //
    // cursor.close();
    // if (db.isOpen()) {
    // db.close();
    // }
    //
    // return null;
    // }
    //
    // @Override
    // protected void onPostExecute(final Void result) {
    // FavoritesListAdapter.this.notifyDataSetChanged();
    // }
    // }
}
