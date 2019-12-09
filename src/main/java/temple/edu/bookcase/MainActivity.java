package temple.edu.bookcase;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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


public class MainActivity extends AppCompatActivity implements BookListFragment.OnFragmentInteractionListener {

    BookListFragment bookListFragment;
    BookDetailFragment bookDetailsFragment;
    ViewPageFragment viewPageFragment;
    Fragment f;
    Book theBook;
    ArrayList<Book> bookList;
    RequestQueue requestQueue;
    boolean allBooksLoaded;
    FragmentManager fm;
    boolean onePane;
    Button searchButton;
    EditText searchText;
    String URL;
    Fetchable current1;
    Fragment current2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        onePane = findViewById(R.id.container_2) == null;

        allBooksLoaded = false;
        URL = "https://kamorris.com/lab/audlib/booksearch.php?search=";
        requestQueue = Volley.newRequestQueue(this);
        theBook = new Book(-1, null, null, -1, null);
        bookDetailsFragment = BookDetailFragment.newInstance(theBook);
        searchButton = findViewById(R.id.searchButton);
        searchText = findViewById(R.id.searchText);

        fm = getSupportFragmentManager();
        current1 = (Fetchable) fm.findFragmentById(R.id.container_1);
        current2 = fm.findFragmentById(R.id.container_2);

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
                                if (onePane) {
                                    Log.i("UUUU", "current1 is booklistfragment");
                                    fm.beginTransaction()
                                            .remove((Fragment) current1)
                                            .add(R.id.container_1, ViewPageFragment.newInstance(bookList))
                                            .commit();
                                } else {
                                    bookDetailsFragment = BookDetailFragment.newInstance(theBook);
                                    fm.beginTransaction()
                                            .remove((Fragment) current1)
                                            .remove(current2)
                                            .add(R.id.container_1, BookListFragment.newInstance(bookList))
                                            .add(R.id.container_2, bookDetailsFragment)
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
//                            bookDetailsFragment = BookDetailsFragment.newInstance(theBook);
//                            viewPageFragment = ViewPageFragment.newInstance(bookList);


                            FragmentTransaction ft= fm.beginTransaction();
//                            ConstraintLayout container_2 = (ConstraintLayout) findViewById(R.id.container_2);

                            if (onePane) {
                                fm.beginTransaction()
                                        .add(R.id.container_1, ViewPageFragment.newInstance(bookList))
                                        .commit();
                            } else {
                                bookDetailsFragment = BookDetailFragment.newInstance(theBook);
                                fm.beginTransaction()
                                        .add(R.id.container_1, BookListFragment.newInstance(bookList))
                                        .add(R.id.container_2, bookDetailsFragment)
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
            if (onePane) {
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
                    bookDetailsFragment = (BookDetailFragment) current2;
                else {
                    bookDetailsFragment = BookDetailFragment.newInstance(theBook);
                    fm.beginTransaction()
                            .add(R.id.container_2, bookDetailsFragment)
                            .commit();
                }
            }

//            bookDetailsFragment = (BookDetailsFragment) current2;

        }
    }

    @Override
    public void onFragmentInteraction(int index) {
        Log.d("UUUU" ,"Book index:" + index);
        bookDetailsFragment.displayBook(bookList.get(index));
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
                bookArrayList.add(new temple.edu.bookcase.Book(id, title, author, published, coverURL));
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
}