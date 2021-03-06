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
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;


import monakhv.android.samlib.adapter.BookCursorAdapter;

import monakhv.android.samlib.data.DataExportImport;
import monakhv.android.samlib.data.SettingsHelper;
import monakhv.android.samlib.dialogs.ContextMenuDialog;
import monakhv.android.samlib.dialogs.MyMenuData;
import monakhv.android.samlib.dialogs.SingleChoiceSelectDialog;
import monakhv.android.samlib.recyclerview.DividerItemDecoration;
import monakhv.android.samlib.service.DownloadBookServiceIntent;
import monakhv.android.samlib.sortorder.BookSortOrder;
import monakhv.android.samlib.sql.AuthorProvider;

import monakhv.samlib.db.SQLController;
import monakhv.samlib.db.entity.Book;
import monakhv.samlib.db.entity.SamLibConfig;

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
    private static final String DEBUG_TAG = "BookFragment";
    public static final String AUTHOR_ID = "AUTHOR_ID";
    private RecyclerView bookRV;
    private long author_id;
    private BookCursorAdapter adapter;
    private Book book = null;//for context menu
    private BookSortOrder order;
    private GestureDetector detector;
    private SettingsHelper settings;
    ProgressDialog progress;
    ContextMenuDialog contextMenuDialog;
    private String selection;
    private TextView emptyText;
    private DataExportImport dataExportImport;
    private SingleChoiceSelectDialog dialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(DEBUG_TAG, "onCreate");

        if (getActivity().getIntent().getExtras() == null) {
            author_id = 0;
        } else {
            author_id = getActivity().getIntent().getExtras().getLong(AUTHOR_ID, 0);
        }
        Log.i(DEBUG_TAG, "author_id = " + author_id);

        detector = new GestureDetector(getActivity(), new ListSwipeListener(this));

        settings = new SettingsHelper(getActivity().getApplicationContext());
        order = BookSortOrder.valueOf(settings.getBookSortOrderString());
        dataExportImport = new DataExportImport(getActivity().getApplicationContext());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(DEBUG_TAG, "onCreateView");

        View view = inflater.inflate(R.layout.book_fragment,
                container, false);
        Log.i(DEBUG_TAG, "Done making view");
        bookRV = (RecyclerView) view.findViewById(R.id.bookRV);
        emptyText = (TextView) view.findViewById(R.id.id_empty_book_text);

        setSelection();
        Log.i(DEBUG_TAG, "selection = " + selection);


        Cursor c = getActivity().getContentResolver().query(AuthorProvider.BOOKS_URI, null, selection, null, order.getOrder());

        adapter = new BookCursorAdapter(getActivity(), c);
        adapter.setAuthor_id(author_id);
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

        makeEmpty();
        adapter.registerAdapterDataObserver(observer);
        return view;
    }

    private RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            Log.d(DEBUG_TAG, "Observed: makeEmpty");
            makeEmpty();
        }
    };

    /**
     * Construction selection string using author_id parameter
     */
    private void setSelection() {
        if (author_id == SamLibConfig.SELECTED_BOOK_ID) {
            selection = SQLController.COL_BOOK_GROUP_ID + "=" + Book.SELECTED_GROUP_ID;
        } else {
            selection = SQLController.COL_BOOK_AUTHOR_ID + "=" + author_id;
        }
    }

    /**
     * Make empty text view
     */
    private void makeEmpty() {
        if (adapter.getItemCount() == 0) {
            bookRV.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            if (author_id == SamLibConfig.SELECTED_BOOK_ID) {
                emptyText.setText(R.string.no_selected_books);
            } else {
                emptyText.setText(R.string.no_new_books);
            }

        } else {
            bookRV.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }
    }

    private void updateAdapter() {
        //very ugly hack
        if (order == null) {
            Context ctx = getActivity().getApplicationContext();
            if (ctx == null) {
                Log.e(DEBUG_TAG, "Context is NULL");
            }
            settings = new SettingsHelper(ctx);
            order = BookSortOrder.valueOf(settings.getBookSortOrderString());
        }
        String so = order.getOrder();
        adapter.changeCursor(getActivity().getContentResolver().query(AuthorProvider.BOOKS_URI, null, selection, null, so));
    }

    /**
     * Set new author_id and update selection,adapter and empty view
     *
     * @param id Author id or special parameters
     */
    public void setAuthorId(long id) {
        author_id = id;
        setSelection();
        updateAdapter();
        makeEmpty();
        adapter.setAuthor_id(id);
        adapter.cleanSelection();
    }

    @Override
    public boolean singleClick(MotionEvent e) {
        int position = bookRV.getChildAdapterPosition(bookRV.findChildViewUnder(e.getX(), e.getY()));
        adapter.toggleSelection(position);


        Book book = adapter.getSelected();
        if (book == null) {
            return false;
        }
        loadBook(book);
        bookRV.playSoundEffect(SoundEffectConstants.CLICK);
        return true;
    }

    @Override
    public boolean swipeRight(MotionEvent e) {
        Log.v(DEBUG_TAG,"making swipeRight");
        int position = bookRV.getChildAdapterPosition(bookRV.findChildViewUnder(e.getX(), e.getY()));
        adapter.toggleSelection(position,false);

        book = adapter.getSelected();

        if (book == null) {
            Log.e(DEBUG_TAG,"Book is null");
            return false;
        }
        adapter.makeSelectedRead(true);
        return true;
    }

    @Override
    public boolean swipeLeft(MotionEvent e) {
        int position = bookRV.getChildAdapterPosition(bookRV.findChildViewUnder(e.getX(), e.getY()));
        adapter.toggleSelection(position);

        book = adapter.getSelected();

        if (book == null) {
            return false;
        }
        launchBrowser(book);
        return true;
    }

    private final int menu_mark_read = 1;
    private final int menu_browser = 2;
    private final int menu_selected = 3;
    private final int menu_deselected = 4;
    private final int menu_reload = 10;
    private final int menu_fixed = 6;
    private final int menu_choose_version = 7;

    @Override
    public void longPress(MotionEvent e) {
        int position = bookRV.getChildAdapterPosition(bookRV.findChildViewUnder(e.getX(), e.getY()));
        adapter.toggleSelection(position);

        book = adapter.getSelected();

        if (book == null) {
            return;
        }
        final MyMenuData menu = new MyMenuData();

        if (book.isIsNew()) {
            menu.add(menu_mark_read, getString(R.string.menu_read));
        }
        menu.add(menu_browser, getString(R.string.menu_open_web));
        if (book.getGroup_id() == Book.SELECTED_GROUP_ID) {
            menu.add(menu_deselected, getString(R.string.menu_deselected));
        } else {
            menu.add(menu_selected, getString(R.string.menu_selected));
        }
        menu.add(menu_reload, getString(R.string.menu_reload));
        if (book.isPreserve()) {
            menu.add(menu_fixed, getString(R.string.menu_set_unfixed));
            menu.add(menu_choose_version, getString(R.string.menu_version_choose));
        } else {
            menu.add(menu_fixed, getString(R.string.menu_set_fixed));
        }


        contextMenuDialog = ContextMenuDialog.getInstance(menu, new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int item = menu.getIdByPosition(position);
                contextSelector(item);
                contextMenuDialog.dismiss();
            }
        }, null);

        contextMenuDialog.show(getActivity().getSupportFragmentManager(), "bookContext");


    }

    private void contextSelector(int item) {
        if (item == menu_browser) {
            launchBrowser(book);
        }
        if (item == menu_mark_read) {
            adapter.makeSelectedRead(true);
        }
        if (item == menu_selected) {
            book.setGroup_id(Book.SELECTED_GROUP_ID);
            adapter.update(book);
        }
        if (item == menu_deselected) {
            book.setGroup_id(0);
            adapter.update(book);
        }
        if (item == menu_reload) {

            settings.cleanBookFile(book);


            loadBook(book);
        }
        if (item == menu_fixed) {

            if (book.isPreserve()) {
                Log.i(DEBUG_TAG, "making book UNpreserved " + book.getUri());
                //TODO: alert Dialogs
                //TODO: clean all copies and reload
                book.setPreserve(false);
            } else {
                Log.i(DEBUG_TAG, "making book preserved " + book.getUri());
                settings.makePreserved(book);
                book.setPreserve(true);
            }
            adapter.update(book);

        }
        if (item == menu_choose_version) {
            final String[] files = settings.getBookFileVersions(book);
            if (files.length == 0l) {
                Log.i(DEBUG_TAG, "file is NULL");
                //TODO: alarm no version is found
                return;
            }
            dialog = SingleChoiceSelectDialog.getInstance(files, new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    dialog.dismiss();
                    String file = files[position];

                    launchReader(book, file);
                }
            }, getString(R.string.menu_version_choose));
            dialog.show(getActivity().getSupportFragmentManager(), "readVersion");

        }
    }

    /**
     * Launch Browser to load book from web server
     *
     * @param book book to read
     */
    private void launchBrowser(Book book) {

        String surl = book.getUrlForBrowser(settings);

        Log.d(DEBUG_TAG, "book url: " + surl);

        Uri uri = Uri.parse(surl);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
        SettingsHelper setting = new SettingsHelper(getActivity());
        if (setting.getAutoMarkFlag()) {
            adapter.makeSelectedRead(false);
        }

        startActivity(launchBrowser);
    }


    private SingleChoiceSelectDialog sortDialog;


    /**
     * Show Dialog to select sort order for Book list
     */
    public void selectSortOrder() {
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BookSortOrder so = BookSortOrder.values()[position];
                setSortOrder(so);
                sortDialog.dismiss();

            }
        };
        sortDialog = SingleChoiceSelectDialog.getInstance(BookSortOrder.getTitles(getActivity()), listener, this.getString(R.string.dialog_title_sort_book), getSortOrder().ordinal());
        sortDialog.show(getActivity().getSupportFragmentManager(), "DoBookSortDialog");
    }

    void setSortOrder(BookSortOrder so) {
        order = so;
        updateAdapter();
    }

    BookSortOrder getSortOrder() {
        return order;
    }


    private void loadBook(Book book) {
        book.setFileType(settings.getFileType());
        if (dataExportImport.needUpdateFile(book)) {
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
        launchBrowser.setDataAndType(Uri.parse(settings.getBookFileURL(book)), book.getFileMime());


        if (settings.getAutoMarkFlag()) {
            adapter.makeSelectedRead(false);
        }
        startActivity(launchBrowser);
    }

    /**
     * Launch Reader to read the book considering book is downloaded
     * To read given version
     *
     * @param book the book to read
     * @param file version file name
     */
    void launchReader(Book book, String file) {

        Intent launchBrowser = new Intent();
        launchBrowser.setAction(android.content.Intent.ACTION_VIEW);
        launchBrowser.setDataAndType(Uri.parse(settings.getBookFileURL(book, file)), book.getFileMime());


        if (settings.getAutoMarkFlag()) {
            adapter.makeSelectedRead(false);
        }
        startActivity(launchBrowser);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.clear();
            adapter.unregisterAdapterDataObserver(observer);
        }
    }
}
