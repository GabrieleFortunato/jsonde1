package com.jsonde.util.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.JOptionPane;

public class FileUtils {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");
    public static final String USER_HOME = System.getProperty(user());

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static boolean createFile(File file) {

        if (!file.exists()) {

            File directory = file.getParentFile();

            if (!directory.exists()) {
                return directory.mkdirs();
            } else {
                return true;
            }

        } else {
            return true;
        }

    }

    public static String canonizePath(String path) throws IOException {
        File file = new File(path);
        return file.getCanonicalPath();
    }
    
    /**
	 * Legge da file il nome utente di MySQL
	 * @return
	 */
	public static String user() {
		try {
			@SuppressWarnings("resource")
			BufferedReader buffer = new BufferedReader(new FileReader("user.txt"));
			s = buffer.readLine();
		} catch (IOException e) {
			JOptionPane.showMessageDialog (
				null , "Problemi di lettura da file user"
			);
		}
		return s;
	}

}
