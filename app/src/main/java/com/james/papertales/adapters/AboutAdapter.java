package com.james.papertales.adapters;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.james.papertales.R;
import com.james.papertales.utils.StaticUtils;

import java.util.ArrayList;

public class AboutAdapter extends RecyclerView.Adapter<AboutAdapter.ViewHolder> {

    private Activity activity;
    private ArrayList<Item> items;

    public AboutAdapter(Activity activity, ArrayList<Item> items) {
        this.activity = activity;
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public View v;

        public ViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }


    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof HeaderItem) return 0;
        else return 1;
    }

    @Override
    public AboutAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        switch (viewType) {
            case 0:
                return new ViewHolder(inflater.inflate(R.layout.layout_header, parent, false));
            case 1:
                return new ViewHolder(inflater.inflate(R.layout.layout_text, parent, false));
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(AboutAdapter.ViewHolder holder, int position) {
        items.get(position).bindView(holder);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class HeaderItem extends Item {

        public boolean centered;
        @Nullable
        public String name, content, url;

        public HeaderItem(Activity activity, @Nullable String name, @Nullable String content, boolean centered, @Nullable String url) {
            super(activity);

            this.centered = centered;
            this.name = name;
            this.content = content;
            this.url = url;
        }

        @Override
        public void bindView(ViewHolder holder) {
            if (name != null && name.length() > 0) {
                TextView header = (TextView) holder.v.findViewById(R.id.header);
                header.setVisibility(View.VISIBLE);
                header.setText(name);
                if (centered) header.setGravity(Gravity.CENTER_HORIZONTAL);
            } else holder.v.findViewById(R.id.header).setVisibility(View.GONE);

            if (content != null && content.length() > 0) {
                TextView desc = (TextView) holder.v.findViewById(R.id.content);
                desc.setVisibility(View.VISIBLE);
                desc.setText(content);
                if (centered) desc.setGravity(Gravity.CENTER_HORIZONTAL);
            } else holder.v.findViewById(R.id.content).setVisibility(View.GONE);

            if (url != null) {
                holder.v.setClickable(true);
                holder.v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StaticUtils.launchCustomTabs(getContext(), Uri.parse(url));
                    }
                });
            } else holder.v.setClickable(false);
        }
    }

    public static class TextItem extends Item {

        @Nullable
        public String name, content, primary;

        public TextItem(Activity activity, @Nullable String name, @Nullable String content, @Nullable String primary) {
            super(activity);

            this.name = name;
            this.content = content;
            this.primary = primary;
        }

        @Override
        public void bindView(ViewHolder holder) {

            if (name != null && name.length() > 0) {
                TextView header = (TextView) holder.v.findViewById(R.id.header);
                header.setVisibility(View.VISIBLE);
                header.setText(name);
            } else holder.v.findViewById(R.id.header).setVisibility(View.GONE);

            if (content != null && content.length() > 0) {
                TextView desc = (TextView) holder.v.findViewById(R.id.content);
                desc.setVisibility(View.VISIBLE);
                desc.setText(content);
            } else holder.v.findViewById(R.id.content).setVisibility(View.GONE);

            if (primary != null) {
                View card = holder.v.findViewById(R.id.card);
                card.setClickable(true);
                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StaticUtils.launchCustomTabs(getContext(), Uri.parse(primary));
                    }
                });
            } else holder.v.findViewById(R.id.card).setClickable(false);
        }
    }

    public static class Item {

        private Activity context;

        public Item(Activity context) {
            this.context = context;
        }

        public Activity getContext() {
            return context;
        }

        public void bindView(AboutAdapter.ViewHolder holder) {
        }
    }
}
