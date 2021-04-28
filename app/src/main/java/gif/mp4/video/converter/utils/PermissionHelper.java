package gif.mp4.video.converter.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import gif.mp4.video.converter.HomeFragment;

public class PermissionHelper {

    private final static int PERMISSIONCODE = 100;
    public static boolean checkPermissions(Context context){
        boolean result =  ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        return result;
    }

    public static void requestPermissions(HomeFragment homeFragment){
        homeFragment.requestPermissions(
                new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONCODE);
    }

    public static boolean checkRequest(int requestCode,int[] grantResults){
        boolean result = false;
        result = requestCode == PERMISSIONCODE
                && grantResults.length == 2
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED;
        return result;
    }

}
