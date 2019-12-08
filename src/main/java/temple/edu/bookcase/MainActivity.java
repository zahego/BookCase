package temple.edu.bookcase;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends AppCompatActivity implements BookListFragment.OnFragmentInteractionListener {
    BookListFragment bookListFragment;
    BookDetailFragment bookDetailFragment;
    String[] bookList;
    ViewPageFragment viewPageFragment;
    Fragment f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Resources res= getResources();
        bookList = res.getStringArray(R.array.booklist);
        bookListFragment = BookListFragment.newInstance(bookList);
        bookDetailFragment = BookDetailFragment.newInstance("");
        viewPageFragment = ViewPageFragment.newInstance(bookList);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft= fragmentManager.beginTransaction();
        ConstraintLayout container_2 = (ConstraintLayout) findViewById(R.id.container_2);

        if (container_2 == null){
            //portrait
            f = fragmentManager.findFragmentByTag("list");
            if(f!=null) ft.remove(f);
            ft.add(R.id.container_1, viewPageFragment, "pager");
        } else{
            f = fragmentManager.findFragmentByTag("pager");
            if(f!=null) ft.remove(f);
            ft.add(R.id.container_1, bookListFragment, "list");
            ft.add(R.id.container_2, bookDetailFragment, "detail");
        }
        ft.commit();

    }

    @Override
    public void onFragmentInteraction(int index) {
        Log.d("UUUU" ,"Book index:" + index);
        bookDetailFragment.displayBook(bookList[index]);
    }
}
