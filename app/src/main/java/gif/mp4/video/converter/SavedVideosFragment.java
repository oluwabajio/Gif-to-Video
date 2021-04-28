package gif.mp4.video.converter;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import gif.mp4.video.converter.adapters.VideoAdapter;
import gif.mp4.video.converter.databinding.FragmentSavedVideosBinding;
import gif.mp4.video.converter.models.VideoModel;


public class SavedVideosFragment extends Fragment {

    FragmentSavedVideosBinding binding;
    public static ArrayList<VideoModel > videoArrayList;
    RecyclerView recyclerView;
    public static final int PERMISSION_READ = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSavedVideosBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        videoList();
        return view;
    }

    public void videoList() {
        recyclerView = binding.rvSavedVideos;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        videoArrayList = new ArrayList<>();
        getVideos();
    }

    //get video files from storage
    public void getVideos() {
        ContentResolver contentResolver = getActivity().getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = contentResolver.query(uri, null, null, null, null);

        //looping through all rows and adding to list
        if (cursor != null && cursor.moveToFirst()) {
            do {

                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE));
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                VideoModel  videoModel  = new VideoModel ();
                videoModel .setVideoTitle(title);
                videoModel .setVideoUri(Uri.parse(data));
                videoModel .setVideoDuration(timeConversion(Long.parseLong(duration)));
                videoArrayList.add(videoModel);

            } while (cursor.moveToNext());
        }

        VideoAdapter adapter = new VideoAdapter (getActivity(), videoArrayList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new VideoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int pos, View v) {

              playVideo(videoArrayList.get(pos).getVideoUri());
            }
        });

    }

    private void playVideo(Uri uri) {
        String type = "video/mp4";
        startActivity(new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, type)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }

    //time conversion
    public String timeConversion(long value) {
        String videoTime;
        int dur = (int) value;
        int hrs = (dur / 3600000);
        int mns = (dur / 60000) % 60000;
        int scs = dur % 60000 / 1000;

        if (hrs > 0) {
            videoTime = String.format("%02d:%02d:%02d", hrs, mns, scs);
        } else {
            videoTime = String.format("%02d:%02d", mns, scs);
        }
        return videoTime;
    }

}