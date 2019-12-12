package temple.edu.bookcase;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements BookListFragment.OnFragmentInteractionListener, BookDetailFragment.AudioStartInterface {

    BookListFragment bookListFragment;
    BookDetailFragment bookDetailFragment;
    ViewPageFragment viewPageFragment;
    Fragment f;
    Book theBook;
    ArrayList<Book> bookList;
    RequestQueue requestQueue;
    boolean allBooksLoaded;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(savedInstanceState != null){
            progress = savedInstanceState.getInt("progress");
            playingBook = (Book) savedInstanceState.getSerializable("playingBook");
            connected = savedInstanceState.getBoolean("connected");
            binder = (AudiobookService.MediaControlBinder) savedInstanceState.getBinder("binder");
            Handler audioHandler = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message inputMessage) {
                    // Gets the image task from the incoming Message object.
                    AudiobookService.BookProgress bookProgress = (AudiobookService.BookProgress) inputMessage.obj;
                    if(bookProgress == null) return;
                    progress = bookProgress.getProgress();
                    int id = bookProgress.getBookId();
                    progressBar.setProgress((int)(1.0 * progress / playingBook.duration * 100));
                    Log.e("QQQQ", progress + "");
                }
            };
            binder.setProgressHandler(audioHandler);
        }

        portrait = findViewById(R.id.container_2) == null;

        allBooksLoaded = false;
        URL = "https://kamorris.com/lab/audlib/booksearch.php?search=";
        requestQueue = Volley.newRequestQueue(this);
        theBook = new Book(-1, null, null, -1, null, 0);
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




        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if(fromUser) {
                    binder.seekTo(playingBook.duration / 100 * i);
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
                binder.pause();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binder.stop();
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JsonArrayRequest objectRequest = new JsonArrayRequest(
                        Request.Method.GET,
                        URL + searchText.getText(),
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
        });

        // If there are no fragments at all (first time starting activity)
        if (current1 == null) {
            JsonArrayRequest objectRequest = new JsonArrayRequest(
                    Request.Method.GET,
                    URL,
                    null,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            Log.e("UUUU", "First line in onResponse");
                            bookList = makeBookList(response);
//                            bookListFragment = BookListFragment.newInstance(bookList);
//                            bookDetailFragment = BookDetailFragment.newInstance(theBook);
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

//            bookDetailFragment = (BookDetailFragment) current2;

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

//    @Override
////    protected void onStop() {
////        super.onStop();
////        // Unbind from the service
////        if (connected) {
////            getApplicationContext().unbindService(myConnection);
//////            unbindService(myConnection);
////            connected = false;
////            Log.e("QQQQ", "service unbound");
////        }
////    }

    protected void onSaveInstanceState (Bundle outState){
        super.onSaveInstanceState(outState);
        if(playingBook != null) {
            outState.putInt("progress", progress);
            outState.putSerializable("playingBook", playingBook);
            outState.putBoolean("connected", connected);
            outState.putBinder("binder", binder);

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
                    // Gets the image task from the incoming Message object.
                    AudiobookService.BookProgress bookProgress = (AudiobookService.BookProgress) inputMessage.obj;
                    if(bookProgress == null) return;
                    progress = bookProgress.getProgress();
                    int id = bookProgress.getBookId();
                    progressBar.setProgress((int)(1.0 * progress / playingBook.duration * 100));
                    Log.e("QQQQ", progress + "");
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
                bookArrayList.add(new temple.edu.bookcase.Book(id, title, author, published, coverURL, duration));

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
        if(binder != null){
            playingBook = findBookById(bookList, id);
            binder.play(id);
            playingTitle.setText("Now playing: " + playingBook.title);
        }
    }
}