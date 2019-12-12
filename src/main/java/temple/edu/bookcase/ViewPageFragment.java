package temple.edu.bookcase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

public class ViewPageFragment extends Fragment implements Fetchable{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String BOOKS = "books";

    // TODO: Rename and change types of parameters
    private static ArrayList<Book> books;


    public ViewPageFragment() {
        // Required empty public constructor
    }

    public static ViewPageFragment newInstance(ArrayList<Book> books) {
        ViewPageFragment fragment = new ViewPageFragment();
        Bundle args = new Bundle();
        args.putSerializable(BOOKS, books);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            books = (ArrayList<Book>)getArguments().getSerializable(BOOKS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.view_page_fragment, container, false);
        ViewPager viewPager = view.findViewById(R.id.viewPager);
        BookDetailFragment[] detailArray = new BookDetailFragment[books.size()];
        Book theBook = new Book(10, "ten", "ten ten", 1010, "10101010", 1);
        for(int i=0; i<detailArray.length; i++){
            detailArray[i] = BookDetailFragment.newInstance(books.get(i));
        }
        PagerAdapter pagerAdapter = new PagerAdapter(getFragmentManager(), detailArray);
        viewPager.setAdapter(pagerAdapter);
        return view;
    }

    @Override
    public ArrayList<Book> getBooks() {
        return books;
    }
}
