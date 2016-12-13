package kiat.anhhong.reboot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class StreamGobbler extends Thread {
    public interface OnLineListener {
        void onLine(String line);
    }

    private String shell = null;
    private BufferedReader reader = null;
    private List<String> writer = null;
    private OnLineListener listener = null;

    public StreamGobbler(String shell, InputStream inputStream, List<String> outputList) {
        this.shell = shell;
        reader = new BufferedReader(new InputStreamReader(inputStream));
        writer = outputList;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (writer != null) writer.add(line);
                if (listener != null) listener.onLine(line);
            }
        } catch (IOException e) {
        }
        try {
            reader.close();
        } catch (IOException e) {
        }
    }
}
