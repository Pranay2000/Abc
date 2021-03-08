package com.example.assessment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Set<Number>>, AdapterView.OnItemClickListener {

    protected Settings settings;
    CoordinatorLayout coordinatorLayout;

    ListView list;
    ArrayAdapter<Number> adapter;

    protected String[] fileList;
    protected static File basePath;
    protected static final int DIALOG_LOAD_FILE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        coordinatorLayout = findViewById(R.id.coordinatorLayout);

        list = findViewById(R.id.numbers);
        list.setAdapter(adapter = new MyAdapter(this));
        list.setOnItemClickListener(this);

        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getMenuInflater().inflate(R.menu.blacklist_delete_numbers, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.delete) {
                    deleteSelectedNumbers();
                    actionMode.finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
            }
        });

        requestPermissions();

        getLoaderManager().initLoader(0, null, this);
    }

    protected void requestPermissions() {
        List<String> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Manifest.permission.CALL_PHONE);
        requiredPermissions.add(Manifest.permission.READ_PHONE_STATE);
        requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        requiredPermissions.add(Manifest.permission.READ_CALL_LOG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requiredPermissions.add(Manifest.permission.ANSWER_PHONE_CALLS);
        }

        List<String> missingPermissions = new ArrayList<>();

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    missingPermissions.toArray(new String[0]), 0);
        }
    }

    private static class DeleteFromDB extends AsyncTask<Void, Void, Void> {

        private List<String> numbers;
        private MainActivity context;

        DeleteFromDB(MainActivity context, List<String> numbers) {
            this.numbers = numbers;
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            DbHelper dbHelper = new DbHelper(context);
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                for (String number : numbers)
                    db.delete(Number._TABLE, Number.NUMBER + "=?", new String[] { number });
            } finally {
                dbHelper.close();
            }

            context.getLoaderManager().restartLoader(0, null, context);
            return null;
        }
    }

    protected void deleteSelectedNumbers() {
        final List<String> numbers = new LinkedList<>();

        SparseBooleanArray checked = list.getCheckedItemPositions();
        for (int i = checked.size() - 1; i >= 0; i--)
            if (checked.valueAt(i)) {
                int position = checked.keyAt(i);
                numbers.add(adapter.getItem(position).number);
            }

        new DeleteFromDB(this, numbers).execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean ok = true;
        if (grantResults.length != 0) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    ok = false;
                    break;
                }
            }
        } else {
            // treat cancellation as failure
            ok = false;
        }

        if (!ok)
            Snackbar.make(coordinatorLayout, R.string.blacklist_permissions_required, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.blacklist_request_permissions, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            requestPermissions();
                        }
                    })
                    .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.block_hidden_numbers).setChecked(settings.blockHiddenNumbers());
        menu.findItem(R.id.notifications).setChecked(settings.showNotifications());
        menu.findItem(R.id.block_out_of_list).setChecked(settings.blockOutOfList());
        return true;
    }

    public void onBlockHiddenNumbers(MenuItem item) {
        settings.blockHiddenNumbers(!item.isChecked());
    }

    public void onShowNotifications(MenuItem item) {
        settings.showNotifications(!item.isChecked());
    }

    public void onBlockOutOfList(MenuItem item) {
        settings.blockOutOfList(!item.isChecked());
    }

    public void addNumber(View view) {
        startActivity(new Intent(this, EditNumber.class));
    }

    @Override
    public Loader<Set<Number>> onCreateLoader(int i, Bundle bundle) {
        return new NumberLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Set<Number>> loader, Set<Number> numbers) {
        adapter.clear();
        adapter.addAll(numbers);
    }

    @Override
    public void onLoaderReset(Loader<Set<Number>> loader) {
        adapter.clear();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Number number = adapter.getItem(position);

        Intent intent = new Intent(this, EditNumber.class);
        intent.putExtra(EditNumber.EXTRA_NUMBER, number.number);
        startActivity(intent);
    }

    protected static class NumberLoader extends AsyncTaskLoader<Set<Number>> implements BlacklistObserver.Observer {

        NumberLoader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            BlacklistObserver.addObserver(this, true);
        }

        @Override
        public Set<Number> loadInBackground() {
            DbHelper dbHelper = new DbHelper(getContext());
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                Set<Number> numbers = new LinkedHashSet<>();
                Cursor c = db.query(Number._TABLE, null, null, null, null, null, Number.ID + " ASC");
                while (c.moveToNext()) {
                    ContentValues values = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(c, values);
                    numbers.add(Number.fromValues(values));
                }
                c.close();

                return numbers;
            } finally {
                dbHelper.close();
            }
        }

        @Override
        public void onBlacklistUpdate() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            BlacklistObserver.removeObserver(this);
        }

    }
}