package com.james.papertales;

import android.app.Activity;
import android.app.Application;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.google.gson.Gson;
import com.james.papertales.adapters.AboutAdapter;
import com.james.papertales.data.AuthorData;
import com.james.papertales.data.WallData;
import com.james.papertales.utils.ElementUtils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class Supplier extends Application {

    private String[] urls;
    private int[] pages;

    private ArrayList<AuthorData> authors;
    private ArrayList<WallData> wallpapers;
    private ArrayList<String> tags;

    private ArrayList<String> favWallpapers;
    private ArrayList<String> selectedTags;

    private SharedPreferences prefs;
    private Gson gson;

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        gson = new Gson();

        urls = getResources().getStringArray(R.array.people_wps);
        pages = new int[urls.length];

        favWallpapers = new ArrayList<>();

        int favSize = prefs.getInt("favorites-size", 0);
        for (int i = 0; i < favSize; i++) {
            favWallpapers.add(prefs.getString("favorites-" + i, null));
        }

        selectedTags = new ArrayList<>();

        int tagSize = prefs.getInt("tags-size", 0);
        for (int i = 0; i < tagSize; i++) {
            selectedTags.add(prefs.getString("tags-" + i, null));
        }
    }

    public boolean getNetworkResources() {
        //download any resources needed for the voids below while the splash screen is showing
        //yes, this is thread-safe
        //no, it is not needed for the current setup since all the resources are in res/values/strings.xml

        authors = new ArrayList<>();
        wallpapers = new ArrayList<>();
        tags = new ArrayList<>();

        for (int i = 0; i < urls.length; i++) {
            try {
                Document document = ElementUtils.getDocument(new URL(urls[i]));
                if (document == null) continue;

                String title = ElementUtils.getTitle(document);
                if (title == null)
                    title = urls[i].substring(urls[i].indexOf('/', 6), urls[i].indexOf('.', 8));

                AuthorData author = new AuthorData(title, ElementUtils.getDescription(document), i, urls[i].substring(0, urls[i].length() - 5), urls[i]);
                authors.add(author);

                Elements elements = document.select("item");
                for (Element element : elements) {
                    WallData data = new WallData(ElementUtils.getName(element), ElementUtils.getDescription(element), ElementUtils.getDate(element), ElementUtils.getLink(element), ElementUtils.getComments(element), ElementUtils.getImages(element), ElementUtils.getCategories(element), author.name, author.id);
                    wallpapers.add(data);

                    for (String tag : data.categories) {
                        if (!tags.contains(tag)) tags.add(tag);
                    }
                }
                // etc
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    //get a list of the different sections
    public ArrayList<AuthorData> getAuthors() {
        return authors;
    }

    @Nullable
    public AuthorData getAuthor(int id) {
        if (id < 0 || id >= authors.size()) return null;
        else return authors.get(id);
    }

    //get a list of the different wallpapers
    public ArrayList<WallData> getWallpapers() {
        ArrayList<WallData> walls = new ArrayList<>();
        walls.addAll(wallpapers);

        return walls;
    }

    public ArrayList<WallData> getWallpapers(int authorId) {
        ArrayList<WallData> walls = new ArrayList<>();

        for (WallData wallpaper : wallpapers) {
            if (wallpaper.authorId == authorId) walls.add(wallpaper);
        }

        return walls;
    }

    public void getWallpapers(final int id, final AsyncListener<ArrayList<WallData>> listener) {
        if (id < 0 || id >= pages.length) return;

        new Thread() {
            @Override
            public void run() {
                final ArrayList<WallData> walls = new ArrayList<>();

                Document document;
                try {
                    document = ElementUtils.getDocument(new URL(urls[id] + "?paged=" + String.valueOf(pages[id] + 2)));
                } catch (IOException e) {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onFailure();
                        }
                    });
                    return;
                }

                if (document == null) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onFailure();
                        }
                    });
                    return;
                }

                Elements elements = document.select("item");
                for (Element element : elements) {
                    WallData data = new WallData(ElementUtils.getName(element), ElementUtils.getDescription(element), ElementUtils.getDate(element), ElementUtils.getLink(element), ElementUtils.getComments(element), ElementUtils.getImages(element), ElementUtils.getCategories(element), authors.get(id).name, id);
                    walls.add(data);
                }

                wallpapers.addAll(walls);
                pages[id]++;

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onTaskComplete(walls);
                    }
                });
            }
        }.start();
    }

    public ArrayList<String> getTags() {
        ArrayList<String> strings = new ArrayList<>();

        strings.addAll(tags);
        return strings;
    }

    public ArrayList<String> getSelectedTags() {
        ArrayList<String> strings = new ArrayList<>();

        strings.addAll(selectedTags);
        return strings;
    }

    public boolean setSelectedTags() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("tags-size", selectedTags.size());

        for (int i = 0; i < selectedTags.size(); i++) {
            editor.putString("tags-" + i, selectedTags.get(i));
        }

        return editor.commit();
    }

    public boolean isSelected(String tag) {
        return selectedTags.contains(tag);
    }

    public boolean selectTag(String tag) {
        if (isSelected(tag)) return false;

        selectedTags.add(tag);
        return setSelectedTags();
    }

    public boolean deselectTag(String tag) {
        if (!isSelected(tag)) return false;

        selectedTags.remove(tag);
        return setSelectedTags();
    }

    public ArrayList<WallData> getFavoriteWallpapers() {
        ArrayList<WallData> walls = new ArrayList<>();
        for (String string : favWallpapers) {
            walls.add(gson.fromJson(string, WallData.class));
        }

        return walls;
    }

    public boolean setFavoriteWallpapers() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("favorites-size", favWallpapers.size());

        for (int i = 0; i < favWallpapers.size(); i++) {
            editor.putString("favorites-" + i, favWallpapers.get(i));
        }

        return editor.commit();
    }

    public boolean isFavorite(WallData data) {
        return favWallpapers.contains(gson.toJson(data));
    }

    public boolean favoriteWallpaper(WallData data) {
        if (isFavorite(data)) return false;

        favWallpapers.add(gson.toJson(data));
        return setFavoriteWallpapers();
    }

    public boolean unfavoriteWallpaper(WallData data) {
        if (!isFavorite(data)) return false;

        favWallpapers.remove(gson.toJson(data));
        return setFavoriteWallpapers();
    }

    //additional info to put in the about section
    public ArrayList<AboutAdapter.Item> getAdditionalInfo(Activity activity) {
        ArrayList<AboutAdapter.Item> headers = new ArrayList<>();
        headers.add(new AboutAdapter.HeaderItem(activity, null, getResources().getString(R.string.alex), true, "https://github.com/cadialex"));
        return headers;
    }

    public AlertDialog getCreditDialog(Context context, DialogInterface.OnClickListener onClickListener) {
        //dialog to be shown when credit is required
        return new AlertDialog.Builder(context)
                .setTitle(R.string.credit_required)
                .setMessage(R.string.credit_required_msg)
                .setPositiveButton("OK", onClickListener)
                .create();
    }

    public void downloadWallpaper(Context context, String name, String url) {
        //start a download
        DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
        r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name + ".png");
        r.allowScanningByMediaScanner();
        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        dm.enqueue(r);
    }

    public AlertDialog getDownloadedDialog(Context context, DialogInterface.OnClickListener onClickListener) {
        //dialog to be shown upon completion of a download
        return new AlertDialog.Builder(context).setTitle(R.string.download_complete).setMessage(R.string.download_complete_msg).setPositiveButton("View", onClickListener).create();
    }

    //share a wallpaper
    public void shareWallpaper(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, String.valueOf(Uri.parse(url)));
        context.startActivity(intent);
    }

    public interface AsyncListener<E> {
        void onTaskComplete(E value);

        void onFailure();
    }
}
