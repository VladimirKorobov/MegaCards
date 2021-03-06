package com.mega.megacards;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Map<Integer, Object[]> intendMap = new HashMap<Integer, Object[]>();
    private int ADD_SCREENSHOT_CODE = 11;

    private static final int ALL_PERMISSIONS_RESULT = 1011;
    ArrayList<String> permissions;
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private int selectedPosition = -1;

    static class thumbHolder {
        public thumbHolder(MainActivity This) {
            bkColor = This.getBackground();
        }
        String fileName = "";
        String iconName = "";
        String title="";
        String tab = "";
        Bitmap thumb;
        int bkColor;
    }

    GridView gridView;
    ArrayList<thumbHolder> thumbList;
    SettingsHolder settingsHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView = (GridView) findViewById(R.id.gridview);
        thumbList = new ArrayList<>();
        permissions = new ArrayList<>();
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        permissionsToRequest = new ArrayList<String>(permissions);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(
                        permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                        ALL_PERMISSIONS_RESULT);
            }
        }
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        settingsHolder = new SettingsHolder(this);
        //settingsHolder.importSettings("/storage/emulated/0/Downloads/export/settingsDir");
        settingsHolder.readSettings(thumbList);

        gridView.setAdapter(new imageAdapter(getApplicationContext(), thumbList));
        //item click listner
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                if(position == selectedPosition) {
                    setItemColor(position, getBackground());
                    selectedPosition = -1;
                    ((imageAdapter)gridView.getAdapter()).notifyDataSetChanged();
                }
                else {
                    Intent intent = new Intent(MainActivity.this, ViewActivity.class);
                    thumbHolder holder = (thumbHolder) gridView.getAdapter().getItem(position);

                    intent.putExtra("fileName", holder.fileName);
                    intent.putExtra("Title", holder.title);
                    startActivity(intent);
                }
            }
        });
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView parent, View view,
                                           int position, long id) {
                setItemColor(selectedPosition, getBackground());
                setItemColor(position, getSelection());
                selectedPosition = position;
                ((imageAdapter)gridView.getAdapter()).notifyDataSetChanged();
                return true;
            }
        });
    }

    private void setItemColor(int position, int color) {
        if(position >= 0 && position <= thumbList.size()) {
            thumbList.get(position).bkColor = color;
            /*
            int firstVisible = gridView.getFirstVisiblePosition();
            View view = gridView.getChildAt(position - firstVisible);
            ImageView imageView;
            imageView = (ImageView) view.findViewById(R.id.image);
            imageView.setBackgroundColor(color);

             */
        }
    }
    private Boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel(
                                    "\"These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.toArray(
                                                        new String[permissionsRejected.size()]),
                                                        ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();

    }

    public int getBackground() {
        final int color = ContextCompat.getColor(this, R.color.colorBackground);
        return color;
    }
    public int getSelection() {
        final int color = ContextCompat.getColor(this, R.color.colorSelection);
        return color;
    }

    private Bitmap createThimb(File file, String title) {
        Bitmap canvasBitmap = null;
        if(file.isFile()) try {
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            InputStream stream = new FileInputStream(file);
            BitmapFactory.decodeStream(stream, null, bitmapOptions);
            stream.close();

            float maxWidth = getResources().getDimensionPixelSize(R.dimen.imagewidth);
            float maxHeight = getResources().getDimensionPixelSize(R.dimen.imageheight);
            Resources r = getResources();

            int maxSize = Math.max(bitmapOptions.outWidth, bitmapOptions.outHeight);
            int sampleSize = (int) ((double) maxSize / maxWidth + 0.5);

            bitmapOptions.inJustDecodeBounds = false;
            bitmapOptions.inSampleSize = sampleSize;
            //bitmapOptions.inMutable = true;

            stream = new FileInputStream(file);
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, bitmapOptions);
            if (title != null && title.length() != 0) {
                // Write title at the bottom
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                float baseline = -paint.ascent();
                canvasBitmap = Bitmap.createBitmap((int)maxWidth, (int)maxHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(canvasBitmap);
                canvas.drawBitmap(bitmap,
                        new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                        new Rect((canvasBitmap.getWidth() - bitmap.getWidth())/2, 0,
                                (canvasBitmap.getWidth() + bitmap.getWidth())/2, canvasBitmap.getHeight()),
                                paint);
                float textSize = canvasBitmap.getHeight() / 6;
                paint.setTextSize(textSize);
                int width = (int) (paint.measureText(title) + 0.5f);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextAlign(Paint.Align.LEFT);

                canvas.drawRect(0, canvasBitmap.getHeight()- textSize - textSize/8, canvasBitmap.getWidth(), canvasBitmap.getHeight(), paint);
                paint.setColor(Color.BLACK);
                canvas.drawText(title, (canvasBitmap.getWidth() - width) / 2, canvasBitmap.getHeight() - textSize/8, paint);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return canvasBitmap;
    }

     private void OpenFileDirectory(File dir) {

        if (dir.isDirectory()) {
            thumbList.clear();
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            File[] files = dir.listFiles((FilenameFilter) null);

            float dip = 100f;
            Resources r = getResources();
            float maxWidth = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dip,
                    r.getDisplayMetrics()
            );
            for (File file : files) {
                if (file.isFile()) try {
                    InputStream stream = new FileInputStream(file);
                    BitmapFactory.decodeStream(stream, null, bitmapOptions);

                    int maxSize = Math.max(bitmapOptions.outWidth, bitmapOptions.outHeight);
                    int sampleSize = (int) ((double) maxSize / maxWidth + 0.5);

                    bitmapOptions.inJustDecodeBounds = false;
                    bitmapOptions.inSampleSize = sampleSize;

                    thumbHolder holder = new thumbHolder(this);
                    holder.fileName = file.getPath();
                    holder.thumb = BitmapFactory.decodeFile(holder.fileName, bitmapOptions);
                    thumbList.add(holder);

                    bitmapOptions.inSampleSize = 0;

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.id_addscreenshot:
                AddScreenshot();
                break;
            case R.id.id_deletescreenshot:
                DeleteScreenshots();
                break;
            case R.id.id_editscreenshot:
                EditScreenshot();
                break;
            case R.id.id_exportsettings:
                ExportSettings();
                break;
            case R.id.id_exit:
                Exit();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
    }

    private void Exit() {
        finish();
    }

    private void ExportSettings() {
        String exportDir = "/storage/emulated/0/Downloads/export";
        settingsHolder.exportSettings(exportDir);
    }
    private void EditScreenshot() {
        // Get selection
        if(selectedPosition >= 0 && selectedPosition < thumbList.size()) {
            EditScreenshot(thumbList.get(selectedPosition));
        }
    }

    private void EditScreenshot(final thumbHolder holder) {
        final EditCardDialog dlg = new EditCardDialog(this);
        dlg.setPositiveButton("OK", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                List<EditText> edits = dlg.getTextEdits();
                holder.title = edits.get(0).getText().toString();
                try {
                    Bitmap bitmap = createThimb(new File(holder.fileName), holder.title);
                    OutputStream out =  new  FileOutputStream(holder.iconName);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                    out.close();
                    holder.thumb = bitmap;
                    settingsHolder.writeSettings(thumbList);
                    ((imageAdapter)gridView.getAdapter()).notifyDataSetChanged();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            }
        });

        dlg.show(holder);
    }

    private void DeleteScreenshots() {
        //SparseBooleanArray checked = gridView.getCheckedItemPositions();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete all selected screenshots");
        builder.setMessage("Are you sure?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(selectedPosition >= 0 && selectedPosition < thumbList.size()) {
                    thumbHolder holder = thumbList.get(selectedPosition);
                    try {
                        File f = new File(holder.fileName);
                        if(f.exists()) {
                            f.delete();
                        }
                        f = new File(holder.iconName);
                        if(f.exists()) {
                            f.delete();
                        }
                        thumbList.remove(selectedPosition);
                    }
                    catch(Exception ex) {
                        String s = ex.getMessage();
                        s = "";

                    }
                    settingsHolder.writeSettings(thumbList);
                    ((imageAdapter)gridView.getAdapter()).notifyDataSetChanged();
                }
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // I do not need any action here you might
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void AddScreenshot() {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(chooseFile, ADD_SCREENSHOT_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ADD_SCREENSHOT_CODE) {
                if (resultData != null) {
                    BufferedReader reader = null;
                    try {
                        Uri uri = resultData.getData();
                        String src = getFileName(uri);
                        String randName = UUID.randomUUID().toString();
                        String uniqueSrc = randName + src.substring(src.lastIndexOf('.'));
                        InputStream in = getContentResolver().openInputStream(uri);
                        File mydir = this.getDir("imagedir", this.MODE_PRIVATE); //Creating an internal dir;
                        final String outFileName = mydir.getPath() + "/" + uniqueSrc;
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(outFileName));
                        CopyFile(in, out);
                        in.close();
                        out.close();

                        // Edit card info
                        mydir = this.getDir("thumbdir", this.MODE_PRIVATE);
                        final String iconFileName = mydir.getPath() + "/" + randName + ".png";
                        final thumbHolder holder = new thumbHolder(this);
                        holder.fileName = outFileName;
                        holder.iconName = iconFileName;
                        thumbList.add(holder);
                        EditScreenshot(holder);
                    }
                    catch(Exception ex) {

                    }
                }
            }
        }
    }

    private void CopyFile(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void writeSettings1() {
        //File mydir = this.getDir("settingsDir", this.MODE_PRIVATE); //Creating an internal dir;
        File mydir = this.getDir("settingsDir", this.MODE_PRIVATE); //Creating an internal dir;
        try {
            File settings = new File(mydir.getPath() + "/settings.txt");
            if(settings.exists()) {
                settings.delete();
            }

            FileOutputStream fw = new FileOutputStream(settings, true);
            for(thumbHolder holder: thumbList) {
                fw.write((holder.fileName + "\n").getBytes());
                fw.write((holder.iconName + "\n").getBytes());
                fw.write((holder.title + "\n").getBytes());
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // ...
    }
    private void readSettings1() {
        File mydir = this.getDir("settingsDir", this.MODE_PRIVATE); //Creating an internal dir;
        File settings = new File(mydir.getPath() + "/settings.txt");
        if(settings.exists()) {
            try {
                BufferedReader fr = new BufferedReader(new FileReader(settings));
                String fileName;
                while((fileName = fr.readLine()) != null) {
                    thumbHolder holder = new thumbHolder(this);
                    holder.fileName = fileName;
                    holder.iconName = fr.readLine();
                    holder.title = fr.readLine();
                    holder.thumb =  BitmapFactory.decodeFile(holder.iconName);
                    thumbList.add(holder);
                }
                fileName = "";
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception ex) {
                String m = ex.getMessage();
                ex.printStackTrace();
            }

        }
    }


}
