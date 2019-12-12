package temple.edu.bookcase;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import edu.temple.audiobookplayer.AudiobookService;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;


public class MainActivity extends AppCompatActivity implements BookListFragment.OnFragmentInteractionListener, BookDetailFragment.AudioStartInterface {

    BookListFragment bookListFragment;
    BookDetailFragment bookDetailFragment;
    ViewPageFragment viewPageFragment;
    Fragment f;
    Book theBook;
    ArrayList<Book> bookList;
    RequestQueue requestQueue;
    FragmentManager fm;
    boolean portrait;
    Button searchButton;
    EditText searchText;
    String URL;
    Fetchable current1;
    Fragment current2;

    Button pauseButton;
    Button stopButton;
    SeekBar progressBar;
    edu.temple.audiobookplayer.AudiobookService audiobookService;
    edu.temple.audiobookplayer.AudiobookService.MediaControlBinder binder;
    boolean connected = false;
    Book playingBook;
    TextView playingTitle;
    int progress;
    private static final int PERMISSION_STORAGE_CODE = 1000;
    public String appDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BookCase";
    MediaPlayer player;
    File downloadFolder;

    File downloadedState;
    File nowPlayingState;
    File searchResultState;
    File progressState;

    Hashtable<Integer, Integer> downloadedDict;
    Hashtable<Integer, Integer> progressDict;

    Runnable runnable;
    Handler handler;

    String searchQuerry = "";
    int downloadId;
    boolean listeningLocally = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadedDict = new Hashtable<>();
        progressDict = new Hashtable<>();
//        downloadFolder  = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        downloadFolder = new File(Environment.getExternalStorageDirectory() + "/BookCase");
        downloadedState = new File(downloadFolder, "downloaded.txt");
        nowPlayingState = new File(downloadFolder, "nowPlaying.txt");
        searchResultState = new File(downloadFolder, "searchResult.txt");
        progressState = new File(downloadFolder, "progress.txt");
        handler = new Handler();
        URL = "https://kamorris.com/lab/audlib/booksearch.php?search=";

        if(savedInstanceState != null){
            progress = savedInstanceState.getInt("progress");
            playingBook = (Book) savedInstanceState.getSerializable("playingBook");
            connected = savedInstanceState.getBoolean("connected");
            binder = (AudiobookService.MediaControlBinder) savedInstanceState.getBinder("binder");
            Handler audioHandler = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message inputMessage) {
                    if(!listeningLocally) {
                        // Gets the image task from the incoming Message object.
                        AudiobookService.BookProgress bookProgress = (AudiobookService.BookProgress) inputMessage.obj;
                        if (bookProgress == null) return;
                        progress = bookProgress.getProgress();
                        int id = bookProgress.getBookId();
                        progressDict.put(id, progress);
                        progressBar.setProgress((int) (progress));
                        Log.e("QQQQ", progress + "");
                    }
                }
            };
            binder.setProgressHandler(audioHandler);
        }
        final File appDir = new File(appDirPath);
        appDir.mkdirs();

        portrait = findViewById(R.id.container_2) == null;

        requestQueue = Volley.newRequestQueue(this);
        theBook = new Book(-1, null, null, -1, null, 1, false, 0);
        bookDetailFragment = BookDetailFragment.newInstance(theBook);
        searchButton = findViewById(R.id.searchButton);
        searchText = findViewById(R.id.searchText);

        fm = getSupportFragmentManager();
        current1 = (Fetchable) fm.findFragmentById(R.id.container_1);
        current2 = fm.findFragmentById(R.id.container_2);

        pauseButton = findViewById(R.id.pauseButton);
        stopButton = findViewById(R.id.stopButton);
        progressBar = findViewById(R.id.progressBar);
        playingTitle = findViewById(R.id.playingTitle);

        loadState();




        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if(fromUser) {
                    binder.seekTo(playingBook.duration / 100 * i);
                    if(listeningLocally){
                        player.seekTo(i*1000);
                    }
                    else {
                        binder.seekTo(i);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listeningLocally){
                    if(player!=null) {
                        if(player.isPlaying())
                            player.pause();
                        else
                            player.start();
                    }
                }
                else {
                    binder.pause();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDict.put(playingBook.id, 0);
                progressBar.setProgress(0);
                if(listeningLocally){
                    if(player!=null)
                        stopPlayer();
                }
                else {
                    binder.stop();

                }
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchQuerry = searchText.getText().toString();
                getBooksFromSite(searchQuerry);
            }
        });

        // If there are no fragments at all (first time starting activity)
        if (current1 == null) {
            JsonArrayRequest objectRequest = new JsonArrayRequest(
                    Request.Method.GET,
                    URL + searchQuerry,
                    null,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            Log.e("UUUU", "First line in onResponse");
                            bookList = makeBookList(response);

                            FragmentTransaction ft= fm.beginTransaction();

                            if (portrait) {
                                fm.beginTransaction()
                                        .add(R.id.container_1, ViewPageFragment.newInstance(bookList))
                                        .commit();
                            } else {
                                bookDetailFragment = BookDetailFragment.newInstance(theBook);
                                fm.beginTransaction()
                                        .add(R.id.container_1, BookListFragment.newInstance(bookList))
                                        .add(R.id.container_2, bookDetailFragment)
                                        .commit();
                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.i("UUUUResponse", error.toString());
                        }
                    }
            );

            requestQueue.add(objectRequest);


        } else {
            // Fragments already exist (activity was restarted)
            bookList = current1.getBooks();
            if (portrait) {
                if (current1 instanceof BookListFragment) {
                    // If we have the wrong fragment for this configuration, remove it and add the correct one
                    fm.beginTransaction()
                            .remove((Fragment) current1)
                            .add(R.id.container_1, ViewPageFragment.newInstance(bookList))
                            .commit();
                }
            } else {
                if (current1 instanceof ViewPageFragment) {
                    fm.beginTransaction()
                            .remove((Fragment) current1)
                            .add(R.id.container_1, BookListFragment.newInstance(bookList))
                            .commit();
                }
                if (current2 instanceof BookDetailFragment)
                    bookDetailFragment = (BookDetailFragment) current2;
                else {
                    bookDetailFragment = BookDetailFragment.newInstance(theBook);
                    fm.beginTransaction()
                            .add(R.id.container_2, bookDetailFragment)
                            .commit();
                }
            }

//            bookDetailFragment = (BookDetailFragment) current2;

        }
    }
    void permissionDownload() {
        // if OS is Marshmallow or above, handle permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_DENIED) {
                // permission denied, request it
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                // show popup for runtime permission
                requestPermissions(permissions, PERMISSION_STORAGE_CODE);
            } else {
                startDownloading();
            }
        } else {
            // system OS is less than marshmallow, perform download
            startDownloading();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service
        if(binder == null) {
            Intent serviceIntent = new Intent(MainActivity.this, edu.temple.audiobookplayer.AudiobookService.class);
            getApplicationContext().bindService(serviceIntent, myConnection, Context.BIND_AUTO_CREATE);
//        bindService(serviceIntent, myConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        saveState();
    }

    protected void onSaveInstanceState (Bundle outState){
        super.onSaveInstanceState(outState);
        if(playingBook != null) {
            outState.putInt("progress", progress);
            outState.putSerializable("playingBook", playingBook);
            outState.putBoolean("connected", connected);
            outState.putBinder("binder", binder);
            outState.putString("searchQuerry", searchQuerry);

        }
    }


    ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("QQQQ", "made new connection");
            binder = (edu.temple.audiobookplayer.AudiobookService.MediaControlBinder) service;
//            audiobookService = binder.getService();
            connected = true;
            Handler audioHandler = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message inputMessage) {
                    if(!listeningLocally) {
                        // Gets the image task from the incoming Message object.
                        AudiobookService.BookProgress bookProgress = (AudiobookService.BookProgress) inputMessage.obj;
                        if (bookProgress == null) return;
                        progress = bookProgress.getProgress();
                        int id = bookProgress.getBookId();
                        progressDict.put(id, progress);
                        progressBar.setProgress((int) (progress));
                        Log.e("QQQQ", progress + "");
                    }
                }
            };
            binder.setProgressHandler(audioHandler);
//            if(playingBook != null){
//                binder.play(playingBook.id, progress);
//                playingTitle.setText("Now playing: " + playingBook.title);
//            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connected = false;
        }
    };

    @Override
    public void onFragmentInteraction(int index) {
        Log.d("UUUU" ,"Book index:" + index);
        bookDetailFragment.displayBook(bookList.get(index));
    }

    ArrayList<Book> makeBookList(JSONArray response){
        ArrayList<Book> bookArrayList = new ArrayList<>();
        Log.i("UUUU", response.length() + "");
        for(int i=0; i<response.length(); i++) {
            try {
                JSONObject bookJson = response.getJSONObject(i);
                int id = bookJson.getInt("book_id");
                String title = bookJson.getString("title");
                String author = bookJson.getString("author");
                int published = bookJson.getInt("published");
                String coverURL = bookJson.getString("cover_url");
                int duration = bookJson.getInt("duration");
                boolean tempDownloaded = false;
                int tempProgress = 0;
                if(downloadedDict.containsKey(id)){
                    tempDownloaded = downloadedDict.get(id)==1;
                }
                if(progressDict.containsKey(id)){
                    tempProgress = progressDict.get(id);
                }
                bookArrayList.add(new temple.edu.bookcase.Book(id, title, author, published, coverURL, duration, tempDownloaded, tempProgress));

                Log.i("UUUU","Title: " + title);
            } catch (JSONException e) {
                Log.e("UUUU", e.toString());
                e.printStackTrace();
            }
        }
        return bookArrayList;
    }

    ArrayList<String> makeBookTitleList(ArrayList<Book> bookList){
        ArrayList<String> titles = new ArrayList<>();
        for(Book book : bookList){
            titles.add(book.title);
        }
        return titles;
    }
    Book findBookById(ArrayList<Book> bookList, int id){
        for(Book b : bookList){
            if(b.id == id){
                return b;
            }
        }
        return null;
    }

    @Override
    public void startAudio(int id) {
        boolean downloaded = getDownloaded(id);
        int progress = getProgress(id);

        if(player!=null){
            stopPlayer();

        }
        if(binder!=null){
            binder.stop();
        }
        if(downloaded){
            listeningLocally = true;
            playingBook = findBookById(bookList, id);
            playingTitle.setText("Now playing: " + playingBook.title);
            player = MediaPlayer.create(getApplicationContext(), Uri.parse(downloadFolder.getAbsolutePath()+"/book_"+id+".mp3"));
            mediaPlayerChangeSeekBar();
            if (player == null) Log.e("QQQQ", "player is not initiated");
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlayer();
                }
            });

            player.seekTo(progress*1000);
            player.start();
            Toast.makeText(getApplicationContext(), "playing locally", Toast.LENGTH_SHORT).show();
        }

        else {
            listeningLocally = false;
            if (binder != null) {
                playingBook = findBookById(bookList, id);
//                Toast.makeText(getApplicationContext(), "id:"+id+", duration:"+playingBook.duration+", progress:"+progress, Toast.LENGTH_SHORT).show();
                binder.play(id);
//                binder.seekTo(progress);
                playingTitle.setText("Now playing: " + playingBook.title);
            }
        }
        progressBar.setMax(playingBook.duration);
    }

    // TODO: implement downloadDelete
    @Override
    public boolean downloadDelete(int id) {
        Book b = findBookById(bookList, id);
        b.downloaded = !b.downloaded;
        if(b.downloaded)
            downloadedDict.put(id, 1);
        else
            downloadedDict.put(id, 0);
        if(b.downloaded){
            downloadId = id;
//            startDownloading();
            permissionDownload();
        } else{
            File file = new File(downloadFolder, "book_"+id+".mp3");
            if(file.exists())
                file.delete();
            Toast.makeText(getApplicationContext(), "Deleted", Toast.LENGTH_SHORT).show();
        }
        return b.downloaded;
    }



    public void startDownloading(){
        (new DownloadingTask()).execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode){
            case PERMISSION_STORAGE_CODE:{
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED){
                    startDownloading();
                }
                else{
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    void saveState(){
        ArrayList<String> downloadedArray = new ArrayList<String>();
        ArrayList<String> progressArray = new ArrayList<String>();
        ArrayList<String> nowPlayingArray = new ArrayList<String>();
        ArrayList<String> searchResultArray = new ArrayList<String>();
        for(int id : downloadedDict.keySet()){
            downloadedArray.add(id + ":" + downloadedDict.get(id));
        }
        writeFile(downloadedArray, downloadedState);

        for(int id : progressDict.keySet()){
            progressArray.add(id+":"+progressDict.get(id));
        }
        writeFile(progressArray, progressState);
        if(playingBook != null) {
            nowPlayingArray.add("id:" + playingBook.id);
            nowPlayingArray.add("title:" + playingBook.title);
            nowPlayingArray.add("duration:" + playingBook.duration);
            writeFile(nowPlayingArray, nowPlayingState);
            if(nowPlayingState.length()!=0){
                Log.e("minhsan", "file not null");
            }
        }
        searchResultArray.add(searchQuerry);
        writeFile(searchResultArray, searchResultState);
    }

    void loadState(){
        if(downloadedState.exists()){
            ArrayList<String> downloadedArray = readFile(downloadedState);
            for(String s: downloadedArray){
                String pieces[] = s.split(":");
                downloadedDict.put(Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]));
            }
        }
        if(progressState.exists()){
            ArrayList<String> progressArray = readFile(progressState);
            for(String s: progressArray){
                String pieces[] = s.split(":");
                progressDict.put(Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]));
            }
        }

        playingBook = nowPlayingBookFromFile();

        progressBar.setMax(playingBook.duration);
        progressBar.setProgress(getProgress(playingBook.id));
        playingTitle.setText("Now playing: " + playingBook.title);

        if(searchResultState.exists()){
            ArrayList<String> searchResultArray = readFile(searchResultState);
            if(searchResultArray.size()>0) {
                searchQuerry = searchResultArray.get(0);
            }
            searchText.setText(searchQuerry);
//            getBooksFromSite(searchQuerry);
//            mainBody();
        }
    }

    Book nowPlayingBookFromFile(){
        Hashtable<String, String> nowPlayingDict = new Hashtable<>();
        Book newBook = null;
        if(nowPlayingState.exists()){
            Log.e("minhsan", "it exist");
            ArrayList<String> nowPlayingArray = readFile(nowPlayingState);
            for(String s: nowPlayingArray){
                String pieces[] = s.split(":");
                nowPlayingDict.put(pieces[0], pieces[1]);
            }
            int id = Integer.parseInt(nowPlayingDict.get("id"));
            int tempProgress = 0;
            if(progressDict.containsKey(id)){
                tempProgress = progressDict.get(id);
            }
            newBook = new Book(id,
                    nowPlayingDict.get("title"),
                    //TODO: get actual book author and publish date
                    "minh",
                    2019,
                    "coverURL",
                    100,//Integer.parseInt(nowPlayingDict.get("duration")),
                    false,
                    tempProgress);
        }
        else{
            newBook= new Book(1, "hello", null, -1, null, 1, false, 0);
        }
        Log.e("minhsan", "this is duration: "+newBook.duration);
        return newBook;
    }

    void writeFile(ArrayList<String> data, File file){
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(file);
        }
        catch (FileNotFoundException e) {e.printStackTrace();}
        try
        {
            try
            {
                for (int i = 0; i<data.size(); i++)
                {
                    fos.write(data.get(i).getBytes());
                    if (i < data.size()-1)
                    {
                        fos.write("\n".getBytes());
                    }
                }
            }
            catch (IOException e) {e.printStackTrace();}
        }
        finally
        {
            try
            {
                fos.close();
            }
            catch (IOException e) {e.printStackTrace();}
        }
    }

    public ArrayList<String> readFile(File file) {
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {e.printStackTrace();}
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);

        String test;
        int anzahl=0;
        try
        {
            while ((test=br.readLine()) != null)
            {
                anzahl++;
            }
        }
        catch (IOException e) {e.printStackTrace();}

        try
        {
            fis.getChannel().position(0);
        }
        catch (IOException e) {e.printStackTrace();}

//        String[] array = new String[anzahl];
        ArrayList<String> array = new ArrayList<String>();
        String line;
        int i = 0;
        try
        {
            while((line=br.readLine())!=null)
            {
                array.add(line);
//                array[i] = line;
//                i++;
            }
        }
        catch (IOException e) {e.printStackTrace();}
        return array;
    }

    public void play(View v) {
        if (player == null) {
            player = MediaPlayer.create(getApplicationContext(), Uri.parse(downloadFolder.getAbsolutePath()+"/book1.mp3"));
            if (player == null) Log.e("QQQQ", "player is not initiated");
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlayer();
                }
            });
        }

        player.start();
    }

    public void pause(View v) {
        if (player != null) {
            player.pause();
        }
    }

    public void stop(View v) {
        stopPlayer();
    }

    private void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;
            Toast.makeText(this, "MediaPlayer released", Toast.LENGTH_SHORT).show();
        }
    }

    void getBooksFromSite(String querry){
        Log.e("QQQuery", URL+querry);
        JsonArrayRequest objectRequest = new JsonArrayRequest(
                Request.Method.GET,
                URL + querry,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        bookList = makeBookList(response);
                        current1 = (Fetchable) fm.findFragmentById(R.id.container_1);
                        current2 = fm.findFragmentById(R.id.container_2);
                        // Fragments already exist (activity was restarted)
                        if (portrait) {
                            Log.i("UUUU", "current1 is booklistfragment");
                            fm.beginTransaction()
                                    .remove((Fragment) current1)
                                    .add(R.id.container_1, ViewPageFragment.newInstance(bookList))
                                    .commit();
                        } else {
                            bookDetailFragment = BookDetailFragment.newInstance(theBook);
                            fm.beginTransaction()
                                    .remove((Fragment) current1)
                                    .remove(current2)
                                    .add(R.id.container_1, BookListFragment.newInstance(bookList))
                                    .add(R.id.container_2, bookDetailFragment)
                                    .commit();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("UUUUResponse", error.toString());
                    }
                }
        );
        requestQueue.add(objectRequest);
    }

    void mainBody(){
        if (current1 == null) {
            JsonArrayRequest objectRequest = new JsonArrayRequest(
                    Request.Method.GET,
                    URL + searchQuerry,
                    null,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            Log.e("UUUU", "First line in onResponse");
                            bookList = makeBookList(response);
//                            bookListFragment = BookListFragment.newInstance(bookList);
//                            bookDetailsFragment = BookDetailsFragment.newInstance(theBook);
//                            viewPageFragment = ViewPageFragment.newInstance(bookList);


                            FragmentTransaction ft= fm.beginTransaction();
//                            ConstraintLayout container_2 = (ConstraintLayout) findViewById(R.id.container_2);

                            if (portrait) {
                                fm.beginTransaction()
                                        .add(R.id.container_1, ViewPageFragment.newInstance(bookList))
                                        .commit();
                            } else {
                                bookDetailFragment = BookDetailFragment.newInstance(theBook);
                                fm.beginTransaction()
                                        .add(R.id.container_1, BookListFragment.newInstance(bookList))
                                        .add(R.id.container_2, bookDetailFragment)
                                        .commit();
                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.i("UUUUResponse", error.toString());
                        }
                    }
            );

            requestQueue.add(objectRequest);


        } else {
            // Fragments already exist (activity was restarted)
            bookList = current1.getBooks();
            if (portrait) {
                if (current1 instanceof BookListFragment) {
                    // If we have the wrong fragment for this configuration, remove it and add the correct one
                    fm.beginTransaction()
                            .remove((Fragment) current1)
                            .add(R.id.container_1, ViewPageFragment.newInstance(bookList))
                            .commit();
                }
            } else {
                if (current1 instanceof ViewPageFragment) {
                    fm.beginTransaction()
                            .remove((Fragment) current1)
                            .add(R.id.container_1, BookListFragment.newInstance(bookList))
                            .commit();
                }
                if (current2 instanceof BookDetailFragment)
                    bookDetailFragment = (BookDetailFragment) current2;
                else {
                    bookDetailFragment = BookDetailFragment.newInstance(theBook);
                    fm.beginTransaction()
                            .add(R.id.container_2, bookDetailFragment)
                            .commit();
                }
            }

//            bookDetailsFragment = (BookDetailsFragment) current2;

        }
    }

    boolean getDownloaded(int id){
        boolean downloaded = false;
        if(downloadedDict.containsKey(id)){
            downloaded = downloadedDict.get(id) == 1;
        }
        return downloaded;
    }

    int getProgress(int id){
        int progress = 0;
        if(progressDict.containsKey(id)){
            progress = progressDict.get(id);
        }
        return progress;
    }

    void mediaPlayerChangeSeekBar(){
        if(player==null) return;
        int progress = player.getCurrentPosition()/1000;
        progressBar.setProgress(progress);
        progressDict.put(playingBook.id, progress);
        runnable = new Runnable() {
            @Override
            public void run() {
                mediaPlayerChangeSeekBar();
            }
        };
        handler.postDelayed(runnable, 1000);

    }

    private class DownloadingTask extends AsyncTask<Void, Void, Void> {
        File outputFile = null;

        @Override
        protected void onPostExecute(Void result) {

            try {
                if (outputFile != null) {
                    //Susses Download
                    Toast.makeText(getApplicationContext(), "Download Success", Toast.LENGTH_SHORT).show();
                } else {
                    //Failed Download
                    Toast.makeText(getApplicationContext(), "Download Failed", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Download Failed", Toast.LENGTH_SHORT).show();
            }

            super.onPostExecute(result);

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                //That is url file you want to download
//                URL url = new URL(domain+imagepath + getIntent().getStringExtra("url"));
                java.net.URL url = new URL("https://kamorris.com/lab/audlib/download.php?id=" + downloadId);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                c.connect();

                Log.e("QQQQ", downloadFolder.toString());

                if (!downloadFolder.exists()) {
                    //Create Folder From Path
                    downloadFolder.mkdir();
                }

                //Path And Filename.type
                outputFile = new File(downloadFolder, "book_"+downloadId+".mp3");

                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                    Log.e("FileTest", "File Created");
                }

                FileOutputStream fos = new FileOutputStream(outputFile);

                InputStream is = c.getInputStream();

                byte[] buffer = new byte[1024];
                int len1 = 0;
                while ((len1 = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len1);
                }

                fos.close();
                is.close();

            } catch (Exception e) {

            }
            return null;
        }
    }
}