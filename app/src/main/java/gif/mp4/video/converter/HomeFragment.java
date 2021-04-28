package gif.mp4.video.converter;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gif.mp4.video.converter.adapters.GifAdapter;
import gif.mp4.video.converter.databinding.FragmentHomeBinding;
import gif.mp4.video.converter.utils.PermissionHelper;

public class HomeFragment extends Fragment {


    private static final int REQUEST_CODE_PICK = 104;
    FragmentHomeBinding binding;
    private static final String TAG = "HomeFragment";
    ArrayList<Uri> selectedGifUrl = new ArrayList<>();
    View mView;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        mView = view;

        initListeners();


        return view;
    }

    private void initListeners() {
        binding.btnCombineGifs.setOnClickListener(v -> {
            openFileSelectDialog();
        });

        binding.btnSavedFiles.setOnClickListener(v -> {
            goToSavedVideosPage();
        });
    }

    private void goToSavedVideosPage() {
        Navigation.findNavController(mView).navigate(R.id.action_HomeFragment_to_savedVideosFragment);
    }

    private void openFileSelectDialog() {
        if (PermissionHelper.checkPermissions(getActivity())) {
            showFileSelectDialog();
        } else {
            PermissionHelper.requestPermissions(this);
        }
    }

    private void showFileSelectDialog() {
        startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/gif")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true), REQUEST_CODE_PICK);
    }

    private void gotoGIFMergePage() {

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK
                && resultCode == getActivity().RESULT_OK
                && data != null) {

            if (data.getData() != null) {
                selectedGifUrl.clear();
                selectedGifUrl.add(data.getData());
            } else if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                selectedGifUrl.clear();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    selectedGifUrl.add(clipData.getItemAt(i).getUri());
//                  Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), clipData.getItemAt(i).getUri());
//                  binding.imgLogo.setImageBitmap(bitmap);
                }


            }

            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("gif_uri", selectedGifUrl);
            Navigation.findNavController(mView).navigate(R.id.action_FirstFragment_to_GifMergeFragment, bundle);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionHelper.checkRequest(requestCode, grantResults)) {
            showFileSelectDialog();
        } else {
            Toast.makeText(getActivity(), "Permission is Required", Toast.LENGTH_SHORT).show();
        }
    }
}