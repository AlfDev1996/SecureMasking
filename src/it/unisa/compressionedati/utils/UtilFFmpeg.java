package it.unisa.compressionedati.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class UtilFFmpeg {

    public static boolean execFFmpegConvertVideoToMp4Lossless(String video_input, String video_out) throws IOException, InterruptedException
    {
        String args="";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            args="cmd /C start ";
        //args += "ffmpeg -i "+video_input+" -vcodec png "+video_out;
        args += "ffmpeg -i "+video_input+" -c:v libx264 -crf 10 -c:a aac -strict -2 "+video_out;
        //-c:v libx264 -crf 1 -c:a aac -strict -2
        System.out.print(args);
        String dir =System.getProperty("user.dir")+"/resources/ffmpeg/";
        Process p = Runtime.getRuntime().exec(args,null, new File(dir));

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(p.getErrorStream()));

        // Read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        // Read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }

        return true;

    }

    public static boolean execFFmpegAddAudioTrack( String audio_input, String video_input, String video_out) throws IOException, InterruptedException
    {

        String args="";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            args="cmd /C start ";
        args += "ffmpeg -i "+video_input+" -i "+audio_input+" -c:v copy -c:a aac "+video_out;

        System.out.print(args);
        String dir =System.getProperty("user.dir")+"/resources/ffmpeg/";
        Process p = Runtime.getRuntime().exec(args,null, new File(dir));

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(p.getErrorStream()));

        // Read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        // Read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }

        return true;

    }


    public static String execFFmpegWriteMetadataOnVideo(String path_video, String name_metadata, String metadata_value) throws IOException {
        String args="";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            args="cmd /C start ";
        //ffmpeg -i a_secure.avi_trackAudio.mp3 -c copy -metadata customMeta="22" a_secure.avi_trackAudio.mp3
        String name_video = path_video.replaceAll(".mp4","");
        name_video+="_mt.mp4";
        args += "ffmpeg -i "+path_video+" -metadata "+name_metadata+"="+metadata_value+" -codec copy "+name_video+"";
        //-metadata album="nframe=22" -codec copy output.avi
        System.out.print(args);
        String dir =System.getProperty("user.dir")+"/resources/ffmpeg/";
        Process p = Runtime.getRuntime().exec(args,null, new File(dir));

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(p.getErrorStream()));

        // Read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        // Read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }

        return name_video;
    }

    public static String execFFprobeExtractMetadata(String path_video, String name_metadata) throws IOException {
        String args="";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            args="cmd /C start ";
        //ffprobe a_secure_withaudio_mt.mp4 -show_entries format_tags=album -of compact=p=0:nk=1 -v 0
        args += "ffprobe "+path_video+" -show_entries format_tags="+name_metadata+" -of compact=p=0:nk=1 -v 0";
        System.out.print(args);
        String dir = System.getProperty("user.dir")+"/resources/ffmpeg/";
        Process p = Runtime.getRuntime().exec(args,null, new File(dir));

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(p.getErrorStream()));

        String metadata = new String();
        // Read the output from the command
        System.out.println("\nHere is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
            metadata = s;
        }
        // Read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }

        return metadata;
    }

}
