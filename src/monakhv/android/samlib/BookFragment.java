package monakhv.android.samlib;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;


import monakhv.android.samlib.adapter.BookCursorAdapter;
import monakhv.android.samlib.data.SettingsHelper;
import monakhv.android.samlib.recyclerview.DividerItemDecoration;
import monakhv.android.samlib.service.DownloadBookServiceIntent;
import monakhv.android.samlib.sql.AuthorController;
import monakhv.android.samlib.sql.AuthorProvider;
import monakhv.android.samlib.sql.SQLController;
import monakhv.android.samlib.sql.entity.Book;
import monakhv.android.samlib.sql.entity.SamLibConfig;

/*
 * Copyright 2014  Dmitry Monakhov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 12/11/14.
 */
public class BookFragment extends Fragment implements ListSwipeListener.SwipeCallBack {
    private static final String DEBUG_TAG="BookFragment";
    public static final String AUTHOR_ID = "AUTHOR_ID";
    private RecyclerView bookRV;
    private long author_id;
    private BookCursorAdapter adapter;
    private SortOrder order;
    private GestureDetector detector;
    private SettingsHelper settings;
    ProgressDialog progress;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity().getIntent().getExtras() == null) {
            author_id = 0;
        } else {
            author_id = getActivity().getIntent().getExtras().getLong(AUTHOR_ID, 0);
        }
        Log.i(DEBUG_TAG,"author_id = "+author_id);
        order=SortOrder.BookDate;
        detector = new GestureDetector(getActivity(), new ListSwipeListener(this));

        settings= new SettingsHelper(getActivity().getApplicationContext());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.book_fragment,
                container, false);
        Log.i(DEBUG_TAG,"Done making view");
        bookRV = (RecyclerView) view.findViewById(R.id.bookRV);
        String selection;
        if (author_id ==  SamLibConfig.SELECTED_BOOK_ID){
            selection = SQLController.COL_BOOK_GROUP_ID+"="+ Book.SELECTED_GROUP_ID;
        }
        else {
            selection = SQLController.COL_BOOK_AUTHOR_ID + "=" + author_id;
        }
        Log.i(DEBUG_TAG,"selection = "+selection);


        Cursor c = getActivity().getContentResolver().query(AuthorProvider.BOOKS_URI, null, selection, null, order.getOrder());

        adapter = new BookCursorAdapter(getActivity(),c);
        bookRV.setHasFixedSize(true);
        bookRV.setLayoutManager(new LinearLayoutManager(getActivity()));
        bookRV.setAdapter(adapter);


        bookRV.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        bookRV.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                detector.onTouchEvent(event);
                return false;
            }
        });

        return view;
    }

    @Override
    public boolean singleClick(MotionEvent e) {
        int position = bookRV.getChildPosition(bookRV.findChildViewUnder(e.getX(),e.getY()));
        adapter.toggleSelection(position);


        Book book =adapter.getSelected();
        if (book == null){
            return false;
        }
        loadBook(book);
        bookRV.playSoundEffect(SoundEffectConstants.CLICK);
        return true;
    }

    @Override
    public boolean swipeRight(MotionEvent e) {
        return false;
    }

    @Override
    public boolean swipeLeft(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int sel = item.getItemId();
        if (sel == android.R.id.home) {
            getActivity().finish();
        }
        return super.onOptionsItemSelected(item);
    }
    private void loadBook(Book book){
        book.setFileType(settings.getFileType());
        if (book.needUpdateFile()) {
            progress = new ProgressDialog(getActivity());
            progress.setMessage(getActivity().getText(R.string.download_Loading));
            progress.setCancelable(true);
            progress.setIndeterminate(true);
            progress.show();
            DownloadBookServiceIntent.start(getActivity(), book.getId(), true);


        } else {

            launchReader(book);
        }

    }
    /**
     * Launch Reader to read the book considering book is downloaded
     *
     * @param book the book to read
     */
    void launchReader(Book book) {

        Intent launchBrowser = new Intent();
        launchBrowser.setAction(android.content.Intent.ACTION_VIEW);
        launchBrowser.setDataAndType(Uri.parse(book.getFileURL()), book.getFileMime());


        if (settings.getAutoMarkFlag()) {
            adapter.makeSelectedRead();
        }
        startActivity(launchBrowser);
    }

    public enum SortOrder {

        DateUpdate(R.string.sort_book_mtime, SQLController.COL_BOOK_MTIME + " DESC"),
        BookName(R.string.sort_book_title, SQLController.COL_BOOK_ISNEW + " DESC, " + SQLController.COL_BOOK_TITLE),
        BookDate(R.string.sort_book_date, SQLController.COL_BOOK_ISNEW + " DESC, " + SQLController.COL_BOOK_DATE+" DESC"),
        BookSize(R.string.sort_book_size, SQLController.COL_BOOK_ISNEW + " DESC, " + SQLController.COL_BOOK_SIZE+" DESC");

        private final int name;
        private final String order;

        private SortOrder(int name, String order) {
            this.name = name;
            this.order = order;
        }

        public String getOrder(){
            return order;
        }

        public static String[] getTitles(Context ctx) {
            String[] res = new String[values().length];
            int i = 0;
            for (SortOrder so : values()) {
                res[i] = ctx.getString(so.name);
                ++i;
            }
            return res;
        }
    }
}