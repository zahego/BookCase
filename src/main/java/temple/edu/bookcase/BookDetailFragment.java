package temple.edu.bookcase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;

import androidx.fragment.app.Fragment;

public class BookDetailFragment extends Fragment {
    private static final String ID = "param1";
    private static final String TITLE = "param2";
    private static final String AUTHOR = "param3";
    private static final String PUBLISHED = "param4";
    private static final String COVERURL = "param5";
    private static final String BOOK = "book";

    // TODO: Rename and change types of parameters
    private Book book;
    private TextView textView;
    private TextView textView1;
    private TextView textView2;
    private TextView textView3;
    private TextView textView4;
    private ImageView bookCover;




    public BookDetailFragment() {
        // Required empty public constructor
    }

    public static BookDetailFragment newInstance(Book book) {
        BookDetailFragment fragment = new BookDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ID, book.id);
        args.putSerializable(BOOK, book);
        /*args.putString(TITLE, book.title);
        args.putString(AUTHOR, book.author);
        args.putInt(PUBLISHED, book.published);
        args.putString(COVERURL, book.coverURL);*/
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            /*book.id = getArguments().getInt(ID);
            book.title = getArguments().getString(TITLE);
            book.author = getArguments().getString(AUTHOR);
            book.published = getArguments().getInt(PUBLISHED);
            book.coverURL = getArguments().getString(COVERURL);*/
            book = (Book) getArguments().getSerializable(BOOK);
        }
    }

    public void displayBook(/*String title*/Book book){
        //textView.setText(book.id);
        textView1.setText(book.title);
        textView2.setText(book.author);
        //textView3.setText(book.published);
        //textView4.setText(book.coverURL);
        if(book.title != null)
            textView3.setText(book.published+"");
        else
            textView4.setText(null);
        new DownloadImageTask(bookCover).execute(book.coverURL);
//        bookCover.setImageDrawable(getImage(book.coverURL));
//        bookCover.setImageURI(book.coverURL);


        //textView.setText(title);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.book_details_fragment, container, false);
        //textView = view.findViewById(R.id.BookIDTextView);
        textView1 = view.findViewById(R.id.BookTitleTextView);
        textView2 = view.findViewById(R.id.BookAuthorTextView);
        textView3 = view.findViewById(R.id.BookPublishedTextView);
        //textView4 = view.findViewById(R.id.BookCOVERURLTextView);
        bookCover = view.findViewById(R.id.bookCover);

        displayBook(book);
        return view;
    }
    public Bitmap getImage(String url){
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(url).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.toString());
        }
        return mIcon11;
    }

}

