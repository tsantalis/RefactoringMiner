package org.thoughtcrime.securesms.mms;

import android.content.ContentUris;
import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;

import org.thoughtcrime.securesms.crypto.DecryptingPartInputStream;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.PartDatabase;
import org.thoughtcrime.securesms.providers.PartProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PartAuthority {

  private static final String PART_URI_STRING   = "content://org.thoughtcrime.securesms/part";
  private static final String THUMB_URI_STRING  = "content://org.thoughtcrime.securesms/thumb";
  private static final Uri    PART_CONTENT_URI  = Uri.parse(PART_URI_STRING);
  private static final Uri    THUMB_CONTENT_URI = Uri.parse(THUMB_URI_STRING);

  private static final int PART_ROW  = 1;
  private static final int THUMB_ROW = 2;

  private static final UriMatcher uriMatcher;

  static {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI("org.thoughtcrime.securesms", "part/*/#", PART_ROW);
    uriMatcher.addURI("org.thoughtcrime.securesms", "thumb/*/#", THUMB_ROW);
  }

  public static InputStream getPartStream(Context context, MasterSecret masterSecret, Uri uri)
      throws IOException
  {
    PartDatabase partDatabase = DatabaseFactory.getPartDatabase(context);
    int          match        = uriMatcher.match(uri);

    try {
      switch (match) {
      case PART_ROW:
        PartUriParser partUri = new PartUriParser(uri);
        return partDatabase.getPartStream(masterSecret, partUri.getPartId());
      case THUMB_ROW:
        partUri = new PartUriParser(uri);
        return partDatabase.getThumbnailStream(masterSecret, partUri.getPartId());
      default:
        String tempMediaDir = context.getDir("media", Context.MODE_PRIVATE).getPath();
        if (uri.getPath().startsWith(tempMediaDir))
          return  new DecryptingPartInputStream(new File(uri.getPath()), masterSecret);
        else
          return context.getContentResolver().openInputStream(uri);
      }
    } catch (SecurityException se) {
      throw new IOException(se);
    }
  }

  public static Uri getPublicPartUri(Uri uri) {
    PartUriParser partUri = new PartUriParser(uri);
    return PartProvider.getContentUri(partUri.getPartId());
  }

  public static Uri getPartUri(PartDatabase.PartId partId) {
    Uri uri = Uri.withAppendedPath(PART_CONTENT_URI, String.valueOf(partId.getUniqueId()));
    return ContentUris.withAppendedId(uri, partId.getRowId());
  }

  public static Uri getThumbnailUri(PartDatabase.PartId partId) {
    Uri uri = Uri.withAppendedPath(THUMB_CONTENT_URI, String.valueOf(partId.getUniqueId()));
    return ContentUris.withAppendedId(uri, partId.getRowId());
  }
}
