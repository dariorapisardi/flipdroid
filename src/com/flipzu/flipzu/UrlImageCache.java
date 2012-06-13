/**
 * Based on https://github.com/koush/UrlImageViewHelper
 */
package com.flipzu.flipzu;

import android.graphics.drawable.Drawable;

public final class UrlImageCache extends WeakReferenceHashTable<String, Drawable> {
    private static UrlImageCache mInstance = new UrlImageCache();
    
    public static UrlImageCache getInstance() {
        return mInstance;
    }
    
    private UrlImageCache() {
    }
}