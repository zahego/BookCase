package temple.edu.bookcase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class BookDetailFragment extends Fragment {
    private static final String TITLE = "param1";

    // TODO: Rename and change types of parameters
    private String title;
    TextView textView;


    public BookDetailFragment() {
        // Required empty public constructor
    }

    public static BookDetailFragment newInstance(String title) {
        BookDetailFragment fragment = new BookDetailFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(TITLE);
        }
    }

    public void displayBook(String title){
        textView.setText(title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.book_details_fragment, container, false);
        textView = view.findViewById(R.id.BookTitleTextView);
        displayBook(title);
        return view;
    }

}

