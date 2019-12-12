package temple.edu.bookcase;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;

public class BookListFragment extends Fragment implements Fetchable{
    private static final String BOOKS = "books";
//    private static final String ARG_PARAM2 = "param2";

    private OnFragmentInteractionListener mListener;

    ArrayList<Book> books;

    public BookListFragment() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters
    public static BookListFragment newInstance(ArrayList<Book> books) {
        BookListFragment fragment = new BookListFragment();
        Bundle args = new Bundle();
        args.putSerializable(BOOKS, books);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            books = (ArrayList<Book>) getArguments().getSerializable(BOOKS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.book_list_fragment, container, false);

        ListView listView = view.findViewById(R.id.ListView);
        ArrayList<String> bookTitles = new ArrayList<>();
        for(Book book : books){
            bookTitles.add(book.title);
        }
        ArrayAdapter adapter = new ArrayAdapter((Context) mListener, android.R.layout.simple_list_item_1, bookTitles);;
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onButtonPressed(i);
            }
        });

        return view;
    }
    @Override
    public ArrayList<Book> getBooks() {
        return books;
    }

    public void onButtonPressed(int index) {
        if (mListener != null) {
            mListener.onFragmentInteraction(index);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int index);
    }
}
