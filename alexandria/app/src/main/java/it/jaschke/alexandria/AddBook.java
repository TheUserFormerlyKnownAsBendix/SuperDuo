package it.jaschke.alexandria;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.CharacterPickerDialog;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;

import it.jaschke.alexandria.callback.OnScanCompleteListener;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    public static final String MESSAGE_EVENT = "FOUND_BOOK";
    public static final String MESSAGE_ISBN = "ISBN";
    public static final String MESSAGE_TITLE = "TITLE";
    public static final String MESSAGE_SUBTITLE = "SUBTITLE";
    public static final String MESSAGE_DESC = "DESC";
    public static final String MESSAGE_AUTHORS = "AUTHORS";
    public static final String MESSAGE_IMG_URL = "IMG_URL";
    public static final String MESSAGE_CATEGORIES = "CATEGORIES";

    private MainActivity context;
    private EditText ean;
    private Button scan;
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT="eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";

    private String isbn;
    private String bookTitle;
    private String bookSubTitle;
    private String desc;
    private String authors;
    private String imgUrl;
    private String categories;

    private BroadcastReceiver receiver;


    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(ean!=null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {

        context = (MainActivity) getActivity();

        receiver = new MessageReciever();
        IntentFilter filter = new IntentFilter(MESSAGE_EVENT);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver,filter);

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) rootView.findViewById(R.id.ean);
        scan = (Button)rootView.findViewById(R.id.scan_button);


        if(savedInstanceState!=null){
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        context.setOnScanCompleteListener(new OnScanCompleteListener() {
            @Override
            public void onScanComplete(String isbn) {
                if (ean != null) {
                    Log.d("", "EAN: " + isbn);
                    ean.setText(isbn);
                }
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.startScan();
            }
        });

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean = s.toString();
                //catch isbn10 numbers
                if (ean.length() == 10 && !ean.startsWith("978")) {
                    ean = "978" + ean;
                }

                if(ean.length() == 13) {
                    //Once we have an ISBN, start a book intent
                    Intent bookIntent = new Intent(getActivity(), BookService.class);
                    bookIntent.putExtra(BookService.EAN, ean);
                    bookIntent.setAction(BookService.FETCH_BOOK);
                    getActivity().startService(bookIntent);
                    AddBook.this.restartLoader();
                }
            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BookService.writeBackBook(context, isbn, bookTitle, bookSubTitle, desc, imgUrl);
                try {
                    if (authors != null)
                        BookService.writeBackAuthors(context, isbn, new JSONArray(authors));
                    if (categories != null)
                        BookService.writeBackCategories(context, isbn, new JSONArray(categories));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ean.setText("");
                clearFields();
            }
        });

        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                ean.setText("");
                clearFields();
            }
        });

        return rootView;
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(ean.getText().length()==0){
            return null;
        }
        String eanStr= ean.getText().toString();
        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    private class MessageReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            isbn = intent.getStringExtra(MESSAGE_ISBN);

            bookTitle = intent.getStringExtra(MESSAGE_TITLE);
            Log.d("", "Setting book title: "+bookTitle);
            ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

            bookSubTitle = intent.getStringExtra(MESSAGE_SUBTITLE);
            ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

            desc = intent.getStringExtra(MESSAGE_DESC);

            try {
                authors = intent.getStringExtra(MESSAGE_AUTHORS);
                JSONArray a = new JSONArray(authors);
                String a2 = "";
                for (int i = 0; i < a.length(); i++) {
                    a2 += a.getString(i);
                    if (i < a.length() - 1) a2 += "\n";
                }
                ((TextView) rootView.findViewById(R.id.authors)).setLines(a.length());
                ((TextView) rootView.findViewById(R.id.authors)).setText(a2);
            } catch (Exception e) {
                ((TextView) rootView.findViewById(R.id.authors)).setText("");
            }

            try {
                categories = intent.getStringExtra(MESSAGE_CATEGORIES);
                JSONArray c = new JSONArray(categories);
                String c2 = "";
                for (int i = 0; i < c.length(); i++) {
                    c2 += c.getString(i);
                    if (i < c.length() - 1) c2 += ", ";
                }
                ((TextView) rootView.findViewById(R.id.categories)).setText(c2);
            } catch (Exception e) {
                ((TextView) rootView.findViewById(R.id.categories)).setText("");
            }

            imgUrl = intent.getStringExtra(MESSAGE_IMG_URL);
            if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
                new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
                rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
            }

            rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
        }
    }

}
