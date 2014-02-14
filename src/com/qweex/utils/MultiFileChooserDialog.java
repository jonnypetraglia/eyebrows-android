package com.qweex.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// This class is designed to be an extremely simple mutli-file-selection dialog.
// To use it:
//          new FilePickerDialog(context, callback)
//   args:
//          `context` is used to create the Dialog.
//          `callback` is used to return the data after the user is finished selecting files or cancels.
//
//   callback:
//          If the user pressed "Ok", it sends a Message with a ArrayList<String> called "files" in the data.
//              This contains the complete paths for the files selected.
//          If the user canceled out, it sends an empty Message.
//              (Hint: check `message.getData()==null` to see if the user canceled)
//
// Resources Needed:
//    The only resource needed is really just `R.layout.file_chooser_item`, and `R.string.upload_files` for the title of
//    the Dialog.
//
//    MAKE SURE YOU CHANGE THE FOLLOWING LINE TO YOUR PACKAGE PATH
import com.qweex.eyebrows.R;


public class MultiFileChooserDialog {

    static ArrayList<String> last_path;
    ListView listview;
    ArrayList<String> path;
    List<Triplet<String,Boolean,Boolean>> tripletList;
    OnFilesChosen callback;
    TextView pathView;

    // Constructor: default startFolder to the root of the external memory
    public MultiFileChooserDialog(Context context, OnFilesChosen callback) {
        this(context, callback, null);
    }

    // Constructor
    public MultiFileChooserDialog(Context context, OnFilesChosen callback, String startFolder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Upload Files");
        this.callback = callback;
        path = new ArrayList<String>(Arrays.asList(TextUtils.split(startFolder, "/")));

        getTripletList();
        FilePickerAdapter adap = new FilePickerAdapter(context, R.layout.file_chooser_item, R.id.filename, tripletList);

        listview = new ListView(context);
        listview.setAdapter(adap);
        listview.setOnItemClickListener(clickFolder);

        pathView = new TextView(context);
        pathView.setPadding(5,5,5,5);
        pathView.setTextColor(0xffffffff);
        pathView.setBackgroundColor(0xff555555);
        pathView.setText("/" + TextUtils.join("/", path));
        pathView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);

        LinearLayout ll = new LinearLayout(context);
        ll.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(pathView);
        ll.addView(listview);

        builder.setView(ll);
        builder.setPositiveButton(android.R.string.ok, ok);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setOnCancelListener(cancel);
        builder.show();
    }

    ////////////////////////////// Private stuff! //////////////////////////////

    // Updates the ArrayList of Triplets, according to what the value of "path" is.
    private void getTripletList() {
        List<String> files = new ArrayList<String>(Arrays.asList(new File(Environment.getExternalStorageDirectory(), TextUtils.join("/", path)).list(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return new File(dir, filename).isFile();
            }
        })));

        Collections.sort(files);
        List<String> folders = new ArrayList<String>(Arrays.asList(new File(Environment.getExternalStorageDirectory(), TextUtils.join("/", path)).list(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return new File(dir, filename).isDirectory();
            }
        })));
        Collections.sort(folders);
        if(path.size()>0)
            folders.add(0, "..");

        tripletList = new ArrayList<Triplet<String, Boolean,Boolean>>();
        for(String f : folders)
            tripletList.add(new Triplet<String,Boolean,Boolean>(f, false, false));
        for(String f : files)
            tripletList.add(new Triplet<String,Boolean,Boolean>(f, true, false));
    }

    // Handles the user pressing "Ok" and finalizing the option
    private DialogInterface.OnClickListener ok = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            File workingDir = new File(Environment.getExternalStorageDirectory(), TextUtils.join("/", path));
            ArrayList<String> files = new ArrayList<String>();
            for(Triplet<String,Boolean,Boolean> trip : tripletList) {
                if(trip.third && trip.second) {
                    files.add(new File(workingDir, trip.first).getAbsolutePath());
                }
            }
            last_path = new ArrayList<String>(path);
            callback.onFilesChosen(TextUtils.join("/", path), files);
        }
    };

    // Sends an empty message to the callback if the user cancels
    private DialogInterface.OnCancelListener cancel = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialogInterface) {
            callback.onFilesChosen(null, null);
        }
    };

    // Click a folder so load it
    private ListView.OnItemClickListener clickFolder = new ListView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
            Log.w("PATH: ", path + "!");
            if(pos==0 && path.size()>0)
                path.remove(path.size()-1);
            else
                path.add(((TextView) view.findViewById(R.id.foldername)).getText().toString());
            pathView.setText("/" + TextUtils.join("/", path));
            FilePickerAdapter fpa = (FilePickerAdapter)listview.getAdapter();
            fpa.clear();
            getTripletList();
            fpa.addAll(tripletList);
            listview.setSelectionAfterHeaderView();
        }
    };

    // Click a file
    private View.OnClickListener clickCheck = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            // Ensure that it is a File
            if(view.findViewById(R.id.filename).getVisibility()==View.VISIBLE) {
                Integer pos = (Integer) ((View)view.getParent()).getTag();
                tripletList.get(pos).setThird(((CheckBox)view.findViewById(R.id.filename)).isChecked());
            }
        }
    };

    // Adapter for the ListView; args are <Filename, IsFile, IsChecked>
    public class FilePickerAdapter extends ArrayAdapter<Triplet<String,Boolean,Boolean>> {
        Context context;
        int res_id;
        List<Triplet<String, Boolean, Boolean>> objects;

        public FilePickerAdapter(Context context, int resource, int textViewResourceId, List<Triplet<String,Boolean, Boolean>> objects) {
            super(context, resource, textViewResourceId, objects);
            this.context = context;
            this.res_id = resource;
            this.objects = objects;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent)
        {
            String item = objects.get(pos).first;
            boolean isfile = objects.get(pos).second;
            if(convertView==null)
                convertView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(res_id, null, false);

            // Set the tag so we can get the pos later via an onClickListener for the checkmark
            convertView.setTag(pos);

            // Views!
            CheckBox filename = (CheckBox)convertView.findViewById(R.id.filename);
            TextView foldername = (TextView)convertView.findViewById(R.id.foldername);
            filename.setOnClickListener(clickCheck);

            // Show view for file
            if(isfile) {
                foldername.setVisibility(View.GONE);
                filename.setVisibility(View.VISIBLE);
                filename.setText(item);
                filename.setChecked(tripletList.get(pos).third);
            // Show view for folder
            } else {
                filename.setVisibility(View.GONE);
                foldername.setVisibility(View.VISIBLE);
                foldername.setText(item);
            }

            return convertView;
        }
    }

    //Because "Pair" is all that's available in Java.
    class Triplet<T, U, V>
    {
        public T first;
        public U second;
        public V third;

        Triplet(T a, U b, V c)
        {
            this.first = a;
            this.second = b;
            this.third = c;
        }
        public String toString() { return "[" + first.toString() + ", " + second.toString() + ", " + third.toString() + "]";}
        public void setFirst(T f) { first = f; }
        public void setSecond(U s) { second = s; }
        public void setThird(V t) { third = t; }
    }

    public interface OnFilesChosen {
        public void onFilesChosen(String path, List<String> files);
    }
}
