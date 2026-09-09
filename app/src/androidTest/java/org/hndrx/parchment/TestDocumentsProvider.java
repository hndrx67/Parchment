package org.hndrx.parchment;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/** Test-only provider in a separate process, with no dependency on the app runtime. */
public class TestDocumentsProvider extends ContentProvider {
    private File file(String id) {
        File root = new File(getContext().getCacheDir(), "test-documents");
        root.mkdirs();
        if (id.equals("root")) return root;
        if (!id.matches("[a-f0-9-]+")) throw new IllegalArgumentException(id);
        return new File(root, id);
    }
    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        String id = DocumentsContract.getDocumentId(uri);
        File document = file(id);
        String[] columns = projection != null ? projection : new String[] { "document_id", "_display_name", "mime_type", "flags" };
        MatrixCursor result = new MatrixCursor(columns);
        if (!document.exists()) return result;
        MatrixCursor.RowBuilder row = result.newRow();
        for (String column : columns) {
            Object value = null;
            switch (column) {
                case "document_id": value = id; break;
                case "_display_name": value = document.getName(); break;
                case "mime_type": value = id.equals("root") ? DocumentsContract.Document.MIME_TYPE_DIR : "application/pdf"; break;
                case "flags": value = DocumentsContract.Document.FLAG_SUPPORTS_WRITE | DocumentsContract.Document.FLAG_SUPPORTS_DELETE | DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE; break;
            }
            row.add(column, value);
        }
        return result;
    }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        Uri uri = extras.getParcelable("uri");
        Bundle result = new Bundle();
        if (method.equals("android:createDocument")) {
            if (!DocumentsContract.getDocumentId(uri).equals("root")) throw new IllegalArgumentException();
            String id = UUID.randomUUID().toString();
            try { if (!file(id).createNewFile()) throw new IOException(); }
            catch (IOException error) { throw new IllegalStateException(error); }
            result.putParcelable("uri", DocumentsContract.buildDocumentUriUsingTree(uri, id));
        } else if (method.equals("android:deleteDocument")) {
            String id = DocumentsContract.getDocumentId(uri);
            if (id.equals("root") || !file(id).delete()) throw new IllegalStateException("Delete failed");
        } else throw new UnsupportedOperationException(method);
        return result;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(file(DocumentsContract.getDocumentId(uri)), ParcelFileDescriptor.parseMode(mode));
    }
    @Override public String getType(Uri uri) { return "application/pdf"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
