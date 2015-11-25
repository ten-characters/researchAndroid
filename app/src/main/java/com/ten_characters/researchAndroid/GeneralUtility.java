package com.ten_characters.researchAndroid;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import com.google.android.gms.maps.model.LatLng;
import com.ten_characters.researchAndroid.userInfo.Address;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by austin on 10/07/15.
 */
public class GeneralUtility {
    public static final String USER_INTENT_KEY = "user";
    public static final String SHIPMENT_INTENT_KEY = "shipment";
    public static final String EXPIRATION_INTENT_KEY = "expiration";
    public static final String PAST_SHIPMENT_LIST_INTENT_KEY = "past_shipments";
    public static final String FINISHED_SHIPMENT_INTENT_KEY = "finished_shipment";
    public static final String FINISHED_RATING_INTENT_KEY = "finished_rating";
    public static final String FILENAME_INTENT_KEY = "filename";
    public static final String PROMOCODE_INTENT_KEY = "promocode";

    public static final String INSTRUCTS_DIALOG_TYPE = "instruction_type";


    /* For profile use */
    public static final String HEADER_KEY = "header";
    public static final String MOVES_KEY = "moves";
    public static final String PROFIT_KEY = "profit";
    public static final String WEEK_HEADER = "Week";
    public static final String WEEK_MOVES_KEY = "week_moves";
    public static final String WEEK_PROFIT_KEY = "week_profit";
    public static final String YEAR_HEADER = "Year";
    public static final String YEAR_MOVES_KEY = "year_moves";
    public static final String YEAR_PROFIT_KEY = "year_profit";
    public static final String ALLTIME_HEADER = "All Time";
    public static final String ALLTIME_MOVES_KEY = "total_moves";
    public static final String ALLTIME_PROFIT_KEY = "total_profit";

    /* For Equipment use */
    public static final String MAKE_HEADER = "make";
    public static final String MODEL_HEADER = "model";
    //public static final String YEAR_HEADER = "year";
    public static final String VIN_HEADER = "vin";
    public static final String PLATE_HEADER = "plate";

    // This is the static place where we will store the profile picture
    // Will be deleted upon logout
    public static final String PROFILE_PHOTO_EXT ="/propic";


    public static boolean intToBool(int i) {
        return i == 1;
    }

    // Basically just are you connected or arent ya
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager conMan = // lolo so witty
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = conMan.getActiveNetworkInfo();
        return activeNetInfo != null && activeNetInfo.isConnected();
    }

    public static double metersToMiles(float meters) {
        return meters / 1609.34;
    }

    public static double milesBetween(LatLng start, LatLng end) {
        float[] distanceMeters = new float[1];
        Location.distanceBetween(start.latitude,
                start.longitude,
                end.latitude,
                end.longitude, distanceMeters);

        return metersToMiles(distanceMeters[0]);
    }

    public static LatLng getCurrentLatLng(Context context) {
        Location currentLocation = getCurrentLocation(context);
        if (currentLocation == null)
            return null;

        return new LatLng(currentLocation.getLatitude(),
                currentLocation.getLongitude());
    }

    public static Location getCurrentLocation(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = lm.getBestProvider(criteria, true);
        Location currentLocation = lm.getLastKnownLocation(provider);

        if(currentLocation == null) {
            for(String prov: lm.getAllProviders()) {
                currentLocation = lm.getLastKnownLocation(prov);
                if (currentLocation != null) {
                    break;
                }
            }
        }

        // If we unfortunately can't get a current location, just return null
        if (currentLocation == null)
            return null;

        return currentLocation;
    }

    /* SECTION */
    /* Date Stuff */
    public static String formatDateDisplay(Date toFormat) {
        return SimpleDateFormat.getDateTimeInstance().format(toFormat);
    }

    public static class DateException extends Exception {}
    public static Date parseDate(String dateString) throws DateException{
        DateFormat[] formatters = {
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssssss", Locale.US),
                new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy", Locale.US)
        };

        for (DateFormat formatter: formatters) {
            try {
                return formatter.parse(dateString);
            } catch (ParseException e) {}
        }
        throw new DateException();
    }


    /* SECTION */
    /* File Stuff */
    public static String saveToInternalStorage(Context context, Bitmap image) throws IOException {
        ContextWrapper contextWrapper = new ContextWrapper(context);

        // Path to our apps directory
        File directory = contextWrapper.getDir("imageDir", Context.MODE_PRIVATE);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "DOC_PNG_" + timeStamp + "_.png";
        File imagePath = new File(directory, imageFileName);

        FileOutputStream fos = new FileOutputStream(imagePath);
        // Compress and save at max quality
        image.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();

        return imagePath.getAbsolutePath();
    }

    public static File createTemporaryFile(String pre, String ext) throws IOException {
        File tempDir = Environment.getExternalStorageDirectory();
        tempDir = new File(tempDir.getAbsolutePath() + "/pallet/");
        if(!tempDir.exists()) {
            tempDir.mkdir();
        }
        return File.createTempFile(pre, ext, tempDir);
    }

    public static File zipFile(File toZip) throws IOException {
        // Create zip file with the same prefix as the file to zip
        String filename = (toZip.getName());
        filename = (String) filename.subSequence(0, filename.indexOf('.'));

        // Store in a temp file!
        File zip = createTemporaryFile(filename, ".zip");
        BufferedInputStream origin;
        FileOutputStream dest = new FileOutputStream(zip);

        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

        // Zip files like buffers even if there is only one file being zipped
        byte data[] = new byte[2048];
        FileInputStream fi = new FileInputStream(toZip);
        origin = new BufferedInputStream(fi, 2048);
        String toZipName = toZip.toString();
        ZipEntry entry = new ZipEntry(toZipName.substring(toZipName.lastIndexOf("/") + 1));
        out.putNextEntry(entry);
        int count;
        while ( (count = origin.read(data, 0, 2048)) != -1) {
            out.write(data, 0, count);
        }
        origin.close();
        out.close();
        // Trying to rid the memory leaks
        // Not referenced after this so should be garbage collected
        // But why not
        data = null;
        return zip;
    }

    public static void copyFile(File src, File dst) throws IOException {
        InputStream is = new FileInputStream(src);
        OutputStream os = new FileOutputStream(dst);

        // Copy all bytes from in to out, with a lil buffer
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0)
            os.write(buf, 0, len);

        is.close();
        os.close();
        buf = null;
    }

    /* SECTION */
    /* Profile picture stuff */
    public static void saveProfilePicture(Context context, File propic) {
        File savedPropic = new File(context.getFilesDir() + PROFILE_PHOTO_EXT);
        savedPropic.delete();
        propic.renameTo(savedPropic);
    }

    public static File getProfilePictureFile(Context context) {
        File propic = new File(context.getFilesDir() + PROFILE_PHOTO_EXT);
        // Return null if there is no propic or if it has been deleted
        if (propic.getTotalSpace() == 0)
            return null;
        return propic;
    }

    public static Bitmap getProfilePictureBitmap(Context context) {
        return BitmapFactory.decodeFile(context.getFilesDir() + PROFILE_PHOTO_EXT);
    }


    public static void deleteProfilePicture(Context context) {
        File propic = new File(context.getFilesDir() + PROFILE_PHOTO_EXT);
        propic.delete();
    }

    public static String getFormattedPrice(Context context, double price) {
        return String.format(context.getString(R.string.format_price), price);
    }

    public static String getFormattedAddressCityState(Context context, Address address) {
        return String.format(context.getString(R.string.format_address_city_state), address.getCity(), address.getState());
    }

    public static String getFormattedDate(Context context, Date date) {
        String dateString;

        // Determine if it is today, tomorrow, or later
        // if later, just display the date in mm/dd/yy format
        Calendar todayMidnight = GregorianCalendar.getInstance();
        todayMidnight.setTime(new Date());
        todayMidnight.add(Calendar.DATE, 1);
        todayMidnight.set(Calendar.HOUR_OF_DAY, 0);
        todayMidnight.set(Calendar.MINUTE, 0);
        todayMidnight.set(Calendar.SECOND, 0);
        todayMidnight.set(Calendar.MILLISECOND, 0);


        // Trying this new thing called comparing with calendars
        // Supposedly its cool but I don't get the hype
        // Might go back to dates cuz lame
        Calendar tomorrowMidnight = GregorianCalendar.getInstance();
        tomorrowMidnight.setTime(new Date());
        tomorrowMidnight.add(Calendar.DATE, 2);
        tomorrowMidnight.set(Calendar.HOUR_OF_DAY, 0);
        tomorrowMidnight.set(Calendar.MINUTE, 0);
        tomorrowMidnight.set(Calendar.SECOND, 0);
        tomorrowMidnight.set(Calendar.MILLISECOND, 0);

        Calendar compareCal = GregorianCalendar.getInstance();
        // First check if the date is today
        compareCal.setTime(date);
        if (compareCal.before(todayMidnight)) {
            // Today !
            dateString = "Today";
        } else if (compareCal.before(tomorrowMidnight)) {
            // OO OO OO it's tomorrow !
            dateString = "Tomorrow";
        } else {
            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy");
            dateString = df.format(date);
        }

        SimpleDateFormat amFormat = new SimpleDateFormat("hh:mm a", Locale.US);
        //dateString = dateString + ", " + amFormat.format(date);
        // Not totally sold on this udacity way of doin' things but here goes
        return String.format(context.getString(R.string.format_date_am_pm), dateString, amFormat.format(date));
    }
}
