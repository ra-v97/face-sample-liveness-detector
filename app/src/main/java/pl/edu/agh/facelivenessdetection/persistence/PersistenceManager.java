package pl.edu.agh.facelivenessdetection.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.common.base.Strings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

public class PersistenceManager {

    public static final String TAG = "PersistenceManager";

    private static final String DETECTION_KEY = "detections";

    private static final String RESULTS_DIR = "data";

    private static final String FILENAME_FORMAT = "%s.json";

    private final Context context;

    public PersistenceManager(Context context) {
        this.context = context;
    }

    private boolean checkIsPersistenceActive() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("logging_active_switch", false);
    }

    private String resolveFileName() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String activeMethodStr = prefs.getString("log_file_name", "");
        return String.format(FILENAME_FORMAT, activeMethodStr);
    }

    public void writeFileOnInternalStorage(ActivityDetectionStatus data) {
        if (!checkIsPersistenceActive()) {
            Log.i(TAG, "Persistence is inactive");
            return;
        }
        final File dir = new File(context.getFilesDir(), RESULTS_DIR);
        if (!dir.exists()) {
            Log.i(TAG, " Creating new results directory");
            dir.mkdir();
        }
        final String sFileName = resolveFileName();

        if (Strings.isNullOrEmpty(sFileName)) {
            Log.e(TAG, "Cannot write data to file without name");
            return;
        }
        final File outputFile = new File(dir, sFileName);

        JSONArray dataToWrite = new JSONArray();

        if (outputFile.exists()) {
            final Optional<JSONArray> existingData = resolveExistingJsonArray(outputFile.getAbsolutePath());
            dataToWrite = existingData.orElse(new JSONArray());
        }

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile))) {
            dataToWrite.put(data.getJson());
            JSONObject currentJsonObject = new JSONObject();
            currentJsonObject.put(DETECTION_KEY, dataToWrite);

            bufferedWriter.write(currentJsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Cannot save json data");
        }
    }

    private Optional<JSONArray> resolveExistingJsonArray(String filePath) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line).append("\n");
                line = bufferedReader.readLine();
            }
            final JSONObject PreviousJsonObj = new JSONObject(stringBuilder.toString());
            return Optional.of(PreviousJsonObj.getJSONArray(DETECTION_KEY));
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Cannot read old data");
        }
        return Optional.empty();
    }
}
