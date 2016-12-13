package kiat.anhhong.reboot;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Shell {
    public static List<String> run(String shell, String[] commands, String[] environment,
                                   boolean wantSTDERR) {
        String shellUpper = shell.toUpperCase(Locale.ENGLISH);

        List<String> res = Collections.synchronizedList(new ArrayList<String>());

        try {
            if (environment != null) {
                Map<String, String> newEnvironment = new HashMap<String, String>();
                newEnvironment.putAll(System.getenv());
                int split;
                for (String entry : environment) {
                    if ((split = entry.indexOf("=")) >= 0) {
                        newEnvironment.put(entry.substring(0, split), entry.substring(split + 1));
                    }
                }
                int i = 0;
                environment = new String[newEnvironment.size()];
                for (Map.Entry<String, String> entry : newEnvironment.entrySet()) {
                    environment[i] = entry.getKey() + "=" + entry.getValue();
                    i++;
                }
            }

            Process process = Runtime.getRuntime().exec(shell, environment);
            DataOutputStream STDIN = new DataOutputStream(process.getOutputStream());
            StreamGobbler STDOUT = new StreamGobbler(shellUpper + "-", process.getInputStream(),
                    res);
            StreamGobbler STDERR = new StreamGobbler(shellUpper + "*", process.getErrorStream(),
                    wantSTDERR ? res : null);

            STDOUT.start();
            STDERR.start();
            try {
                for (String write : commands) {
                    STDIN.write((write + "\n").getBytes("UTF-8"));
                    STDIN.flush();
                }
                STDIN.write("exit\n".getBytes("UTF-8"));
                STDIN.flush();
            } catch (IOException e) {
                if (e.getMessage().contains("EPIPE")) {
                } else {
                    throw e;
                }
            }
            
            process.waitFor();
            try {
                STDIN.close();
            } catch (IOException e) {
            }
            STDOUT.join();
            STDERR.join();
            process.destroy();

            if (SU.isSU(shell) && (process.exitValue() == 255)) {
                res = null;
            }
        } catch (IOException e) {
            res = null;
        } catch (InterruptedException e) {
            res = null;
        }
        return res;
    }

    protected static String[] availableTestCommands = new String[]{
            "echo -BOC-",
            "id"
    };

    protected static boolean parseAvailableResult(List<String> ret, boolean checkForRoot) {
        if (ret == null)
            return false;

        boolean echo_seen = false;

        for (String line : ret) {
            if (line.contains("uid=")) {
                return !checkForRoot || line.contains("uid=0");
            } else if (line.contains("-BOC-")) {
                echo_seen = true;
            }
        }

        return echo_seen;
    }

    public static class SU {
        private static String[] suVersion = new String[]{
                null, null
        };
        public static List<String> run(String[] commands) {
            return Shell.run("su", commands, null, false);
        }

        public static boolean available() {

            List<String> ret = run(Shell.availableTestCommands);
            return Shell.parseAvailableResult(ret, true);
        }

        public static synchronized String version(boolean internal) {
            int idx = internal ? 0 : 1;
            if (suVersion[idx] == null) {
                String version = null;

                List<String> ret = Shell.run(
                        internal ? "su -V" : "su -v",
                        new String[]{"exit" },
                        null,
                        false
                );

                if (ret != null) {
                    for (String line : ret) {
                        if (!internal) {
                            if (!line.trim().equals("")) {
                                version = line;
                                break;
                            }
                        } else {
                            try {
                                if (Integer.parseInt(line) > 0) {
                                    version = line;
                                    break;
                                }
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                }

                suVersion[idx] = version;
            }
            return suVersion[idx];
        }

        public static boolean isSU(String shell) {
            int pos = shell.indexOf(' ');
            if (pos >= 0) {
                shell = shell.substring(0, pos);
            }

            pos = shell.lastIndexOf('/');
            if (pos >= 0) {
                shell = shell.substring(pos + 1);
            }

            return shell.equals("su");
        }
    }
}
