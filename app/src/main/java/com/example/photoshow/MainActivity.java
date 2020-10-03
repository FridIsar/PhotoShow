package com.example.photoshow;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> gallerie = new ArrayList<String>();
    OutputStream outputStream;
    static final String imgUrl = "https://thispersondoesnotexist.com/image";
    private long lastcall = 0;
    protected int gallerieIndex = 0;

    private static Context c;

    private String CANCEL_STRING;
    private String SUCCESS_STRING;
    private String FETCHING_STRING;
    private String WAIT_STRING;


    AsyncTask<String, Void, Pair<String, Bitmap>> downloadtask = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        c = this;
        CANCEL_STRING = c.getString(R.string.cancel_notif);
        SUCCESS_STRING = c.getString(R.string.success_notif);
        FETCHING_STRING = c.getString(R.string.fetching_notif);
        WAIT_STRING = c.getString(R.string.wait_notif);

        super.onCreate(savedInstanceState);//is null?
        setContentView(R.layout.activity_main);
        ImageView imgMain = findViewById(R.id.mainImage);
        nextImage(imgMain);
    }

    public void setImage(String fileName, int imageId)  {
        ImageView iv = (ImageView) findViewById(imageId);
        if (gallerie.contains(fileName) || fileName != "")    {
            Bitmap bMap = BitmapFactory.decodeFile(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + fileName);
            iv.setImageBitmap(bMap);
        }
        else    {
            iv.setImageResource(R.drawable.questionmark);
        }
    }

    public void refreshImages() {
        if (gallerieIndex > 0)   {
            if (gallerieIndex > 1)   {//le cas ou on est pas a la premiere image
                setImage(gallerie.get(gallerieIndex - 2), R.id.previousImage);
            }
            else    {
                setImage("", R.id.previousImage);
            }


            setImage(gallerie.get(gallerieIndex - 1), R.id.mainImage);

            if (gallerie.size() > gallerieIndex) {  //le cas ou on est pas a la derniere image
                setImage(gallerie.get(gallerieIndex), R.id.nextImage);
            }
            else    {
                setImage("", R.id.nextImage);
            }
        }
        else    {
            setImage("", R.id.mainImage);
        }
        final TextView tv = findViewById(R.id.textFeedback);
        tv.setText("Image n°"+Integer.toHexString(gallerieIndex));
    }

    public void deleteImage(View view)   {
        File filepath = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(filepath+File.separator+gallerie.get(gallerieIndex-1));
        file.delete();
        gallerie.remove(gallerieIndex-1);
        if (gallerie.size() == gallerieIndex-1)   {
            gallerieIndex-=1;
        }
        System.out.println("deleted galleries :"+gallerie);
        refreshImages();
    }


    public void nextImage(View view)    {
        if (gallerieIndex == gallerie.size()) {
            fetchImage(view);
        }
        else {
            gallerieIndex += 1;
            refreshImages();
        }
    }

    public void previousImage(View view)    {
        if (gallerieIndex > 1)   {
            gallerieIndex -= 1;
        }
        refreshImages();
    }


    public void fetchImage(View view)   {
        final TextView tv = findViewById(R.id.textFeedback);
        long ts = System.currentTimeMillis()/1000;
        if ((ts > lastcall) && (downloadtask == null)){
            lastcall = ts;
            tv.setText(FETCHING_STRING);
            gallerieIndex += 1;
            downloadtask = new fetchImageTask().execute(Integer.toString(gallerie.size()));//crée la tache asynchrone
        }
        else    {
            tv.setText(WAIT_STRING);//download task not null ou ts <= lastcall
            if ((ts-1 > lastcall) && (downloadtask != null)){
                lastcall = ts;
                downloadtask.cancel(true);
                downloadtask = null;
                tv.setText(CANCEL_STRING);
                //ca fait plus de 2 secondes et seulement ca peut rendre le dltask nul d'ou le passage obligatoire par ici
                //cancelled la premiere fois a cause du fetchImage d'initialisation
            }
        }
    }

    private class fetchImageTask extends AsyncTask<String, Void, Pair<String, Bitmap>>   {
        protected Pair<String, Bitmap> doInBackground(String... num) {
            Bitmap image = null;
            String success;
            try {
                InputStream is = new URL(MainActivity.imgUrl).openStream(); //réalise la connexion
                image = BitmapFactory.decodeStream(is);
                success = SUCCESS_STRING;
            }
            catch (Exception e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                success = errors.toString();
            }
            return new Pair<String, Bitmap>(success, image);
        }
        protected void onPostExecute(Pair<String, Bitmap> result) {//on recupere ce qui a été fait dans doinback
            final TextView tv = findViewById(R.id.textFeedback);
            if(result.second != null)    {
                Bitmap bitmap = result.second;


                File filepath = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                String filename = System.currentTimeMillis()+".jpg";
                gallerie.add(filename);
                File file = new File(filepath, filename);//enregistrement du fichier

                try {
                    outputStream = new FileOutputStream(file);
                }   catch (FileNotFoundException e){
                    e.printStackTrace();
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                try {
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else    {
                tv.setText(result.first);
            }
            System.out.println("galleries :"+gallerie);
            refreshImages();
        }
    }
    @Override
    protected void onDestroy()  {
        super.onDestroy();
        if (downloadtask != null)   {
            downloadtask.cancel(true);
        }
    }
}
