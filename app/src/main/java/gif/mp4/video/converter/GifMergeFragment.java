package gif.mp4.video.converter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.otaliastudios.gif.GIFCompressor;
import com.otaliastudios.gif.GIFListener;
import com.otaliastudios.gif.GIFOptions;
import com.otaliastudios.gif.sink.DataSink;
import com.otaliastudios.gif.sink.DefaultDataSink;
import com.otaliastudios.gif.strategy.DefaultStrategy;
import com.otaliastudios.gif.strategy.Strategy;
import com.otaliastudios.gif.strategy.size.AspectRatioResizer;
import com.otaliastudios.gif.strategy.size.FractionResizer;
import com.otaliastudios.gif.strategy.size.PassThroughResizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.Future;

import gif.mp4.video.converter.adapters.GifAdapter;
import gif.mp4.video.converter.databinding.FragmentGifMergeBinding;

public class GifMergeFragment extends Fragment implements GIFListener, AdapterView.OnItemSelectedListener {


    private static final String FILE_PROVIDER_AUTHORITY = "gif.mp4.video.converter.fileprovider";
    FragmentGifMergeBinding binding;
    View mView;
    ArrayList<Uri> selectedGifUriList = new ArrayList<>();
    GifAdapter gifAdapter;
    private static final int PROGRESS_BAR_MAX = 1000;
    private Strategy mStrategy;
    float mSpeed;
    int mRotation;
    private static final String TAG = "GifMergeFragment";
    private File mOutputFile;
    private long mStartTime;
    private boolean mIsCompressing;
    private Future<Void> mCompressionFuture;
    private Uri internalUri;
    private InterstitialAd mInterstitialAd;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentGifMergeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        mView = view;

        getExtrasFromBundle();
        initListeners();


        populateGifRecyclerView();


        binding.progress.setMax(PROGRESS_BAR_MAX);
        initAds();

        return view;
    }

    private void initAds() {

        AdView mAdView = binding.adView;
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        initInterstitialAds();
    }

    private void initInterstitialAds() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(getActivity(),getResources().getString(R.string.interstitial_ads), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                // The mInterstitialAd reference will be null until
                // an ad is loaded.
                mInterstitialAd = interstitialAd;
                Log.i(TAG, "onAdLoaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error
                Log.i(TAG, loadAdError.getMessage());
                mInterstitialAd = null;
            }
        });
    }

    private void populateGifRecyclerView() {
        gifAdapter = new GifAdapter(getActivity(), selectedGifUriList);
        binding.rvGif.setAdapter(gifAdapter);
        binding.rvGif.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));

    }

    private void initListeners() {
        binding.spFrameRate.setOnItemSelectedListener(this);
        binding.spResolution.setOnItemSelectedListener(this);
        binding.spAspectRatio.setOnItemSelectedListener(this);
        binding.spRotation.setOnItemSelectedListener(this);
        binding.spPlaybackSpeed.setOnItemSelectedListener(this);

        binding.btnCombineGifs.setOnClickListener(v -> {
            if (!mIsCompressing) {
                transcodeGifs();
            } else {
                mCompressionFuture.cancel(true);
            }
        });

        binding.btnPlayVideo.setOnClickListener(v -> {
            playVideo();
        });

        binding.btnShareVideo.setOnClickListener(v -> {
            shareVideo(internalUri);
        });
    }

    private void transcodeGifs() {
        // Create a temporary file for output.
        try {
            File outputDir = new File(getActivity().getExternalFilesDir(null), "vid_outputs");
            //noinspection ResultOfMethodCallIgnored
            outputDir.mkdir();
            mOutputFile = File.createTempFile("transcode_test", ".mp4", outputDir);
            Log.e(TAG, "Transcoding into " + mOutputFile);
        } catch (IOException e) {
            Log.e(TAG, "transcodeGifs: Failed to create temporary file.");
            Toast.makeText(getActivity(), "Failed to create temporary file.", Toast.LENGTH_LONG).show();
            return;
        }

        // Launch the transcoding operation.
        mStartTime = SystemClock.uptimeMillis();
        setIsCompressing(true);
        DataSink sink = new DefaultDataSink(mOutputFile.getAbsolutePath());
        GIFOptions.Builder builder = GIFCompressor.into(sink);
        for (int i = 0; i < selectedGifUriList.size(); i++) {
            builder.addDataSource(getActivity(), selectedGifUriList.get(i));
        }
        mCompressionFuture = builder.setListener(this)
                .setStrategy(mStrategy)
                .setRotation(mRotation)
                .setSpeed(mSpeed)
                .compress();
    }


    private void syncParameters() {
        int frames;
        switch (binding.spFrameRate.getSelectedItemPosition()) {
            case 0:
                frames = 20;
                break;
            case 1:
                frames = 24;
                break;
            case 2:
                frames = 30;
                break;
            case 3:
                frames = 36;
                break;
            case 4:
                frames = 40;
                break;
            case 5:
                frames = 60;
                break;
            default:
                frames = DefaultStrategy.DEFAULT_FRAME_RATE;
        }
        float fraction;
        switch (binding.spResolution.getSelectedItemPosition()) {
            case 0:
                fraction = 1F;
                break;
            case 1:
                fraction = 0.8F;
                break;
            case 2:
                fraction = 0.5F;
                break;
            default:
                fraction = 1F;
        }
        float aspectRatio;
        switch (binding.spAspectRatio.getSelectedItemPosition()) {
            case 0:
                aspectRatio = 0F;
                break;
            case 1:
                aspectRatio = 16F / 9F;
                break;
            case 2:
                aspectRatio = 4F / 3F;
                break;
            case 3:
                aspectRatio = 1F;
                break;
            default:
                aspectRatio = 0F;
        }

        int rotation;
        switch (binding.spRotation.getSelectedItemPosition()) {
            case 0:
                rotation = 0;
                break;
            case 1:
                rotation = 90;
                break;
            case 2:
                rotation = 180;
                break;
            case 3:
                rotation = 270;
                break;
            default:
                rotation = 0;
        }

        float speed;
        switch (binding.spPlaybackSpeed.getSelectedItemPosition()) {
            case 0:
                speed = 1F;
                break;
            case 1:
                speed = 0.5F;
                break;
            case 2:
                speed = 2F;
                break;
            default:
                speed = 1F;
        }

        mSpeed = speed;
        mRotation = rotation;
        mStrategy = new DefaultStrategy.Builder()
                .addResizer(aspectRatio > 0 ? new AspectRatioResizer(aspectRatio) : new PassThroughResizer())
                .addResizer(new FractionResizer(fraction))
                .frameRate(frames)
                .build();
    }


    private void getExtrasFromBundle() {
        selectedGifUriList = getArguments().getParcelableArrayList("gif_uri");
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }


    @Override
    public void onGIFCompressionProgress(double progress) {
        if (progress < 0) {
            binding.progress.setIndeterminate(true);
        } else {
            binding.progress.setIndeterminate(false);
            binding.progress.setProgress((int) Math.round(progress * PROGRESS_BAR_MAX));
        }
    }

    @Override
    public void onGIFCompressionCompleted() {
        Log.e(TAG, "Compression took " + (SystemClock.uptimeMillis() - mStartTime) + "ms");
        onCompressionFinished(true, "Compressed video placed on " + mOutputFile);
        File file = mOutputFile;
        Uri uri = FileProvider.getUriForFile(getActivity(),
                FILE_PROVIDER_AUTHORITY, file);
        internalUri = uri;
        saveVideo(uri);


        binding.lySettings.setVisibility(View.GONE);
        binding.lyDownload.setVisibility(View.VISIBLE);
    }

    private void shareVideo(Uri uri) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("video/mp4");
        startActivity(Intent.createChooser(shareIntent, "Share Video Using "));
    }

    @Override
    public void onGIFCompressionCanceled() {
        onCompressionFinished(false, "GIFCompressor canceled.");

    }

    @Override
    public void onGIFCompressionFailed(@NonNull Throwable exception) {
        onCompressionFinished(false, "GIFCompressor canceled.");

    }

    private void onCompressionFinished(boolean isSuccess, @NonNull String toastMessage) {
        binding.progress.setIndeterminate(false);
        binding.progress.setProgress(isSuccess ? PROGRESS_BAR_MAX : 0);
        setIsCompressing(false);
        Toast.makeText(getActivity(), toastMessage, Toast.LENGTH_LONG).show();

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        syncParameters();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void setIsCompressing(boolean isCompressing) {
        mIsCompressing = isCompressing;
        binding.btnCombineGifs.setText(mIsCompressing ? "Cancel Compression" : "Compress");
    }


    private void saveVideo(Uri uri) {

        String videoFileName = "video_" + System.currentTimeMillis() + ".mp4";
        FileOutputStream out = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues valuesvideos = new ContentValues();
            valuesvideos.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/GifToVideo");
            valuesvideos.put(MediaStore.Video.Media.TITLE, videoFileName);
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesvideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesvideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            valuesvideos.put(MediaStore.Video.Media.IS_PENDING, 1);
            final Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Uri uriSavedVideo = getActivity().getContentResolver().insert(collection, valuesvideos);

            ParcelFileDescriptor pfd = null;
            try {
                pfd = getActivity().getContentResolver().openFileDescriptor(uriSavedVideo, "w");
                out = new FileOutputStream(pfd.getFileDescriptor());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                byte[] buf = new byte[8192];
                int len;
                while ((len = inputStream.read(buf)) > 0) {

                    out.write(buf, 0, len);
                }

                out.close();
                inputStream.close();
                pfd.close();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }


            valuesvideos.clear();
            valuesvideos.put(MediaStore.Video.Media.IS_PENDING, 0);
            getActivity().getContentResolver().update(uriSavedVideo, valuesvideos, null, null);

        } else {
            File vidFIle = null;
            try {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/GifToVideo/");
                if (!file.exists()) {
                    file.mkdirs();
                }

                String vidDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/GifToVideo";
                vidFIle = new File(vidDir, videoFileName);
                out = new FileOutputStream(vidFIle);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                byte[] buf = new byte[8192];
                int len;
                while ((len = inputStream.read(buf)) > 0) {

                    out.write(buf, 0, len);
                }

                out.close();
                inputStream.close();
                Toast.makeText(getActivity(), "Saved Successfully" + vidFIle.getAbsolutePath(), Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }


        binding.tvPath.setText(videoFileName);
        binding.tvInfo.setText("Video was saved to  your gallery. \n  Open your GALLERY, then select VIDEO, then open the GIFTOVIDEO folder \n or Go to \n /My Files/Internal Storage/Movies/GifToVideo/" + videoFileName);
        showInterstitialAds();
    }

    private void showInterstitialAds() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(getActivity());
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
        }
    }

    private void playVideo() {
        String type = "video/mp4";
        startActivity(new Intent(Intent.ACTION_VIEW)
                .setDataAndType(internalUri, type)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }
}