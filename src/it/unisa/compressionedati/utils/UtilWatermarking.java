package it.unisa.compressionedati.utils;
import java.io.*;

public class UtilWatermarking {

    public static final String ROOTPATH= new File("").getAbsolutePath()+"/blind_watermark/examples/";


    static public String execPythonScriptWatermark(String script,String wm) throws IOException {

        String[] args = new String[] { "/home/alfonso/anaconda3/bin/python", script, ROOTPATH , wm};


        Process process = new ProcessBuilder(args).start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Leggo il ritorno dello script in python
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String s=in.readLine();

        //System.out.println(s);
        return s;
    }

    static public String execPythonScriptExtract(String script,String wm) throws IOException {

        String[] args = new String[] { "/home/alfonso/anaconda3/bin/python", script, ROOTPATH , wm};


        Process process = new ProcessBuilder(args).start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Leggo il ritorno dello script in python
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String s=in.readLine();

        //System.out.println(s);
        return s;
    }
}
