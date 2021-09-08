package it.unisa.compressionedati.utils;

import it.unisa.compressionedati.gui.WaitingPanelFrame;
import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.json.simple.parser.ParseException;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

public class UtilityUnmaskVideo {

    public static final String ROOTPATH= new File("").getAbsolutePath();


    private ScheduledExecutorService timer;
    private VideoCapture capture;
    private int absoluteFaceSize;
    private VideoWriter writer;
    private String outfile;
    private Scanner in;
    private final Semaphore semaforo;
    private String video_in;
    private String fileName;
    private WaitingPanelFrame waitingPanel;
    private int currFrame,totalFrame;
    private String out;
    private ArrayList<byte[]> listFrame;
    private String password;
    private String checkPass;
    private long start;


    public UtilityUnmaskVideo(String _in_video, String _out_video, Semaphore semaphore, String fileName, WaitingPanelFrame waitingFrame, String password) throws IOException {
        this.capture = new VideoCapture(_in_video);


        this.password = UtilCompression.compressTextAndReturnB64(password);
        this.currFrame=0;

        this.writer= new VideoWriter();
        this.outfile = _out_video;
        this.waitingPanel= waitingFrame;
        this.semaforo=semaphore;
        this.video_in=_in_video;
        this.fileName= fileName;
        this.listFrame = new ArrayList<>();

        this.waitingPanel.getParent().setVisible(false);
        this.waitingPanel.setVisible(true);
        this.waitingPanel.writeOnConsole(("Start: Extraction Watermark "+"\n"));


        String watermark = new String();


        watermark=extractMetadataAndWatermark();
        this.checkPass = watermark;
        if(watermark==null)
            watermark="";
        in = new Scanner(watermark);

    }

    public void startUnmasking() throws IOException {

        if(this.checkPass.equals("Error-Password")) {
            this.waitingPanel.getParent().setVisible(true);
            waitingPanel.dispatchEvent(new WindowEvent(waitingPanel, WindowEvent.WINDOW_CLOSING));
            System.exit(1);
            return;
        }
        start= System.currentTimeMillis();
        this.waitingPanel.writeOnConsole(("Inizializzazione Parametri Unmasking\n"));
        Size frameSize = new Size((int) this.capture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) this.capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        this.totalFrame= (int) this.capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        this.waitingPanel.writeOnConsole(("Numero di Frame: "+this.totalFrame+"\n"));
        int fps = (int) this.capture.get(Videoio.CAP_PROP_FPS);

        this.writer.open(outfile+File.separator+fileName+"_decompressed.avi", VideoWriter.fourcc('x', '2','6','4'),fps, frameSize, true);

        // grab a frame every 33 ms (30 frames/sec)
        Runnable frameGrabber = new Runnable() {

            @Override
            public void run()
            {
                // effectively grab and process a single frame
                Mat frame = grabFrame();
            }
        };

        this.timer = Executors.newSingleThreadScheduledExecutor();
        this.timer.scheduleAtFixedRate(frameGrabber, 0, fps, TimeUnit.MILLISECONDS);
    }



    private String extractMetadataAndWatermark() throws IOException {



        ArrayList<byte[]> listWatermark = new ArrayList<>();

        //I metadata vengono inseriti nella voce album
        String metadata = UtilFFmpeg.execFFprobeExtractMetadata(video_in, "album");
        String[] spl = metadata != null ? metadata.split(";") : null;

        if(spl!=null && spl.length==3){
            this.waitingPanel.writeOnConsole(("Metadata individuati dal video "+"\n"));
            System.out.println(spl.toString());
            //bit for extract watermark
            String bit = spl[0].replace("bit=","");
            String crc_metadata_video = spl[1].replace("crc=","");

            String target_password = spl[2].replace("pass=","");
            if(!(target_password.toString()).equals(password)) {
                JOptionPane.showMessageDialog(this.waitingPanel, "Password Inserita non valida per l'unmasking", "Error Password", JOptionPane.INFORMATION_MESSAGE);

                return "Error-Password";
            }

                VideoCapture watermarkCapture = new VideoCapture(video_in);
            for(int i=0; i<1; ++i){
                this.waitingPanel.writeOnConsole(("Estrazione del watermark dai frame "+"\n"));
                Mat frame = new Mat();
                // check if the capture is open
                if (watermarkCapture.isOpened())
                {
                    try
                    {
                        // read the current frame and save in 'frame'
                        watermarkCapture.read(frame);
                    }
                    catch (Exception e)
                    {
                        // log the (full) error
                        System.err.println("Exception while collecting stepanographed frames: " + e);
                        this.waitingPanel.writeOnConsole(("Exception while collecting stepanographed frames: " + e+"\n"));
                        e.printStackTrace();
                    }
                }
                MatOfByte mob=new MatOfByte();
                Imgcodecs.imencode(".png", frame, mob);
                byte ba[]=mob.toArray();
                listWatermark.add(ba);
            }
            watermarkCapture.release();
            if(listWatermark!=null && listWatermark.size()>0){
                this.waitingPanel.writeOnConsole(("Estrazione dei frame con watermark completata, estrazione del watermark in corso... "+"\n"));
                byte[] img_watermarked = listWatermark.get(0);
                if(img_watermarked!=null){
                    // convert byte[] back to a BufferedImage
                    InputStream is = new ByteArrayInputStream(img_watermarked);
                    BufferedImage newBi = ImageIO.read(is);

                    File img_to_write = new File("blind_watermark/examples/output/embedded.png");
                    ImageIO.write(newBi, "PNG", img_to_write);

                    String wm_extracted = UtilWatermarking.execPythonScriptExtract(ROOTPATH+"/blind_watermark/examples/my_extract.py", bit);
                    System.out.println(wm_extracted);
                    if(wm_extracted!=null && wm_extracted.length()>0){
                        try {
                            List<String> keys = UtilCompression.read_content("database.json");
                            double max=-1.0;
                            String key_found=null;
                            if(keys!=null && keys.size()>0){
                                for(String key : keys){
                                    //Cerco la chiave dal db e calcolo il punteggio della migliore


                                    double distance = StringUtils.getJaroWinklerDistance(key, wm_extracted);
                                    if(distance>=0.70){
                                        if(distance>max){
                                            key_found = key;
                                            max = distance;
                                        }
                                    }
                                }
                            }
                            if(key_found!=null){
                                //Check CRC
                                String content=UtilCompression.find_content_from_key_json("database.json",key_found);
                                CRC32 crc_ = new CRC32();
                                crc_.update(key_found.getBytes());
                                String crc_s=crc_.getValue()+"";
                                if(crc_s.equals(crc_metadata_video)){
                                    this.waitingPanel.writeOnConsole(("END: Watermark Estratto "+"\n"));
                                    return content;
                                }

                            }else{
                                this.waitingPanel.writeOnConsole(("Key watermark non trovata, key : "+wm_extracted+"\n"));
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }else{
                        this.waitingPanel.writeOnConsole(("Estrazione watermark fallita, wm =  "+wm_extracted+"\n"));
                    }


                }
            }else{
                this.waitingPanel.writeOnConsole(("Problemi durante l'estrazione dei frame con watermark"+"\n"));
            }

        }else{
            this.waitingPanel.writeOnConsole(("Problemi durante la lettura dei metadata dal video "+"\n"));
            this.waitingPanel.writeOnConsole(("Metadata richiesti : bit, crc \t num metadata presenti "+spl.length+"\n"));
        }

        this.waitingPanel.writeOnConsole(("Problemi durante l'estrazione del watermark "+"\n"));
        return null;
    }





    /**
     * @return
     */
    private Mat grabFrame()
    {
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened())
        {
            try
            {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty())
                {
                        this.detectAndDisplayAndUnmask(frame);
                }else{
                    this.waitingPanel.writeOnConsole(("Estrazione Traccia Audio"+"\n"));
                    this.extractAudioTrack(video_in,outfile+File.separator+fileName+"_trackAudio.mp3");
                    stopAcquisition();
                    System.out.println("FINE");
                    long end = System.currentTimeMillis();
                    long elapsedTime = end - start;
                    System.out.println("Tempo di Esecuzione : "+elapsedTime);
                    semaforo.release();
                }

            }
            catch (Exception e)
            {
                // log the (full) error
                System.err.println("Exception during the image elaboration: " + e);
                e.printStackTrace();
            }
        }

        return frame;
    }

    private void extractAudioTrack(String _input, String _output){

        String videoPath = _input;
        //audio path
        String extractAudio=_output;
        try{
            //check the audio file exist or not ,remove it if exist
            File extractAudioFile = new File(extractAudio);
            if (extractAudioFile.exists()) {
                extractAudioFile.delete();
            }
            //audio recorder，extractAudio:audio path，2:channels
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(extractAudio, 2);
            recorder.setAudioOption("crf", "0");
            recorder.setAudioQuality(0);
            //bit rate
            recorder.setAudioBitrate(192000);
            //sample rate
            recorder.setSampleRate(44100);
            recorder.setAudioChannels(2);
            //encoder
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
            //start
            recorder.start();
            //load video
            FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(videoPath);

            grabber.start();
            Frame f=null;
            //get audio sample and record it
            while ((f = grabber.grabSamples()) != null) {
                recorder.record(f);
            }
            // stop to save
            grabber.stop();
            recorder.release();
            //output audio path
            //LOGGER.info(extractAudio);
        } catch (Exception e) {
            //LOGGER.err("", e);
        }

    }

    private void detectAndDisplayAndUnmask(Mat frame) throws IOException {

        this.waitingPanel.writeOnConsole(("Unmasking frame "+currFrame+"/"+this.totalFrame+"\n"));

        ArrayList<Roi> rois = new ArrayList<>();
        Mat matrixImgIn = frame;

        // in=> campo value corrispondente alla chiave estratta dal database.json
        if(in.hasNextLine()){
            String line = in.nextLine();    //leggo una riga corrispondente a un frame
            String[] parts = line.split("\\s+");    //splitto la riga in parti separate da whitespaces
            if(!parts[0].equals("/")){
                for(int i=0; i<parts.length; i++) {
                    int x = 0;
                    int y = 0;
                    String value = null;
                    if(parts[i].equals("coord=")){
                        String[] coord = parts[++i].substring(0, parts[i].lastIndexOf("-")).split("[,]");
                        x = Integer.parseInt(coord[1]);
                        y = Integer.parseInt(coord[2]);
                    }
                    if(parts[++i].equals("value=")){
                        value = parts[++i];
                    }
                    Roi r = new Roi(x, y, value);
                    rois.add(r);
                    //System.out.println("x: " + x + ", y: " + y + ", value : " + value);
                }
                for(Roi r : rois){
                    String value = r.getValue();
                    //byte[] ba = null;
                    //Base64.getDecoder().decode(ba);
                    byte[] ba = Base64.getDecoder().decode(value);
                    Mat mat = Imgcodecs.imdecode(new MatOfByte(ba),Imgcodecs.IMREAD_UNCHANGED); //immagine della roi
                    Rect roi = new Rect(r.getX(), r.getY(), mat.cols(), mat.rows());    //coordinate della roi

                    Mat matrixImg = matrixImgIn.submat(new Rect(roi.x, roi.y, roi.width, roi.height));
                    mat.copyTo(matrixImg);
                }
            }
        }

        MatOfByte mob=new MatOfByte();
        Imgcodecs.imencode(".png", frame, mob);
        byte ba[]=mob.toArray();
        listFrame.add(ba);
        ++currFrame;
    }

    private void stopAcquisition()
    {
        if (this.timer!=null && !this.timer.isShutdown())
        {
            try
            {
                // stop the timer

                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);

                    String video_out = outfile+File.separator+fileName;
                    String name_video = video_out.replaceAll("_secure_withaudio_mt.mp4","");
                    //remove old video output
                    File f = new File(name_video+"_decompressed.mp4");
                    f.delete();

                    for(int i=0; i<listFrame.size();++i){
                        this.waitingPanel.writeOnConsole(("Scrittura frame: "+i+"/"+totalFrame));
                        Mat mat = Imgcodecs.imdecode(new MatOfByte(listFrame.get(i)),Imgcodecs.IMREAD_UNCHANGED);
                        writer.write(mat);
                        System.gc();    //chiamo garbage per eliminare a ogni write il Mat contenente il frame dalla memoria
                    }

                    listFrame.clear();
                    this.writer.release();
                    System.out.println("Chiamo garbage collector");
                    System.gc();

                    this.waitingPanel.writeOnConsole(("Inserimento traccia audio"));
                    UtilFFmpeg.execFFmpegAddAudioTrack(outfile+File.separator+fileName+"_trackAudio.mp3",outfile+File.separator+fileName+"_decompressed.avi",name_video+"_decompressed.mp4");

                    this.waitingPanel.writeOnConsole(("Eliminazione file intermedi"+"\n"));
                    f = new File(outfile+File.separator+fileName+"_decompressed.avi");
                    f.delete();
                    File audioTrack = new File(outfile+File.separator+fileName+"_trackAudio.mp3");
                    audioTrack.delete();

                    if(in != null)
                        this.in.close();
                    this.capture.release();

                    this.waitingPanel.getParent().setVisible(true);
                    waitingPanel.dispatchEvent(new WindowEvent(waitingPanel, WindowEvent.WINDOW_CLOSING));

            }
            catch (InterruptedException | IOException e)
            {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (this.capture.isOpened())
        {
            // release the camera
            this.capture.release();
        }
    }



}
