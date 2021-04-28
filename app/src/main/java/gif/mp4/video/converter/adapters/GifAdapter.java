package gif.mp4.video.converter.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import gif.mp4.video.converter.R;

public class GifAdapter extends RecyclerView.Adapter<GifAdapter.MyViewHolder> {

    private LayoutInflater inflater;
    private ArrayList<Uri> imageModelArrayList;
    Context context;

    public GifAdapter(Context ctx, ArrayList<Uri> imageModelArrayList){

        inflater = LayoutInflater.from(ctx);
        this.imageModelArrayList = imageModelArrayList;
        context = ctx;
    }

    @Override
    public GifAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = inflater.inflate(R.layout.view_gif, parent, false);
        MyViewHolder holder = new MyViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(GifAdapter.MyViewHolder holder, int position) {

        Glide.with(context).asGif().load(imageModelArrayList.get(position)).into(holder.iv);

    }

    @Override
    public int getItemCount() {
        return imageModelArrayList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder{

        ImageView iv;

        public MyViewHolder(View itemView) {
            super(itemView);

            iv = (ImageView) itemView.findViewById(R.id.iv);
        }

    }
}