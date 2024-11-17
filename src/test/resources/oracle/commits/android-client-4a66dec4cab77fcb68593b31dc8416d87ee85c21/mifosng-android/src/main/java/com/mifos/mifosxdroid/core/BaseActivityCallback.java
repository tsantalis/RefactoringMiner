/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.core;

/**
 * @author fomenkoo
 */
public interface BaseActivityCallback {
    void showProgress(String message);

    void setToolbarTitle(String title);

    void hideProgress();

    void logout();
}
