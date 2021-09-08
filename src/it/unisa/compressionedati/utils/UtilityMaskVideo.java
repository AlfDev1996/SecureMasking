package it.unisa.compressionedati.utils;

import it.unisa.compressionedati.gui.WaitingPanelFrame;

import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

public class UtilityMaskVideo {

    public static final String ROOTPATH= new File("").getAbsolutePath();

    private ScheduledExecutorService timer;
    private VideoCapture capture;
    private CascadeClassifier faceCascade;
    private int absoluteFaceSize;
    private VideoWriter writer;
    private String outfile;
    private FileWriter coordsRoiFile;
    private Scanner in;
    private String maskPath;
    private final Semaphore semaforo;
    private String video_in;
    private String classifierType;
    private String fileName;
    private WaitingPanelFrame waitingPanel;
    private int currFrame,totalFrame;
    private String out;
    private ArrayList<byte[]> listFrame;
    private String password;
    private long start;

    public UtilityMaskVideo(String _in_video, String _out_video, String mask, Semaphore semaphore, String classifierType, String fileName, WaitingPanelFrame waitingFrame, String password) throws IOException {
        this.classifierType=classifierType;
        FileUtils.cleanDirectory(new File(_out_video));
        this.faceCascade = new CascadeClassifier();
        if(classifierType.equalsIgnoreCase("Haar Frontal Face"))
            this.faceCascade.load("resources/haarcascades/haarcascade_frontalface_alt.xml");

        this.capture = new VideoCapture(_in_video);

        //this.password= password;
        this.password= UtilCompression.compressTextAndReturnB64(password);
        this.absoluteFaceSize = 0;
        this.currFrame=0;
        this.waitingPanel= waitingFrame;
        this.writer= new VideoWriter();
        this.outfile = _out_video;
        this.maskPath= mask;
        //Path del txt finale con i byte roi del video originale
        this.coordsRoiFile = new FileWriter(_out_video+"/dataFrame.txt");
        this.semaforo=semaphore;
        this.video_in=_in_video;
        this.fileName= fileName;
        this.listFrame = new ArrayList<>();
        this.out="";

    }


    public void startMasking()
    {
        start= System.currentTimeMillis();
        this.waitingPanel.getParent().setVisible(false);
        this.waitingPanel.setVisible(true);
        this.waitingPanel.writeOnConsole(("Inizializzazione Parametri Masking\n"));
        this.waitingPanel.writeOnConsole(("Tipo di Masking: "+classifierType+"\n"));
        Size frameSize = new Size((int) this.capture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) this.capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        this.totalFrame= (int) this.capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        this.waitingPanel.writeOnConsole(("Numero di Frame: "+this.totalFrame+"\n"));
        int fps = (int) this.capture.get(Videoio.CAP_PROP_FPS);

        this.writer.open(outfile+File.separator+fileName+".avi", VideoWriter.fourcc('H', 'F','Y','U'),fps, frameSize, true);

        // grab a frame every 33 ms (30 frames/sec)
        Runnable frameGrabber = new Runnable() {

            @Override
            public void run()
            {

                Mat frame = grabFrame();

            }
        };

        this.timer = Executors.newSingleThreadScheduledExecutor();
        this.timer.scheduleAtFixedRate(frameGrabber, 0, fps, TimeUnit.MILLISECONDS);
    }

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
                    // face detection
                    if(this.classifierType.equalsIgnoreCase("Haar Frontal Face")){
                        this.detectAndDisplayAndMaskFace(frame);
                        }
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

    private void detectAndDisplayAndMaskFace(Mat frame) throws IOException {

        this.waitingPanel.writeOnConsole(("Rilevamento tratti e Masking frame "+currFrame+"/"+this.totalFrame+"\n"));

        MatOfRect faces = new MatOfRect();
        Mat grayFrame = new Mat();

        // conversione frame in scala di grigi
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        // equalize l'istogramma del frame
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // scelgo la misura minima del volto da computare nel frame (20% nel nostro caso)
        if (this.absoluteFaceSize == 0)
        {
            int height = grayFrame.rows();
            if (Math.round(height * 0.2f) > 0)
            {
                this.absoluteFaceSize = Math.round(height * 0.2f);
            }
        }

        // cerca le facce all'internop del frame con dimensione monima specificata e popola il vettore faces passato in input
        this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

        // each rectangle in faces is a face: draw them!
        Rect[] facesArray = faces.toArray();
        // Matrice del frame in input
        Mat matrixImgIn = frame;
        // Copia dell'immagine di partenza per sottrarre le ROI in modo tale che non vengano sovrascritte dopo aver applicato la maschera
        Mat matrixImgInCopy = frame;
        String mask = maskPath;

        String out1="";
        int i = 1;
        for (Rect rect : faces.toArray()) {
            String coords = "";
            Rect roi = new Rect(rect.x, rect.y, rect.width, rect.height);
            // ROI all'interno del frame
            Mat matrixImgROI = matrixImgInCopy.submat(roi);


            // Formato della stringa con le informaizoni di ogni ROI: ID,X,Y
            // usiamo il carattere '-' per dividere le informazioni di ogni ROI
            coords = i + "," + rect.x + "," + rect.y + "-";
            out1+= "coord= "+coords+" ";

            // trasformazione della matrice della ROI In jpeg
            MatOfByte mob=new MatOfByte();
            Imgcodecs.imencode(".jpg", matrixImgROI, mob);
            byte ba[]=mob.toArray();
            byte[] ba2 = UtilCompression.compressImageInJpeg(ba, 0.50f);
            //recupero valore dei byte
            String value = Base64.getEncoder().encodeToString(ba2);


            out1+="value= "+value+" ";
            File f=new File(mask);
            //leggo la maschera e la adatto alla dimensione del volto rilevato
            Mat matrixMask = Imgcodecs.imread(mask);
            Mat matrixMaskResized = new Mat();
            Imgproc.resize(matrixMask, matrixMaskResized, new Size(rect.width, rect.height));
            Mat matrixImgSecure = matrixImgIn.submat(new Rect(rect.x, rect.y, matrixMaskResized.cols(), matrixMaskResized.rows()));
            matrixMaskResized.copyTo(matrixImgSecure);

            i++;

        }

        //salvo il frame come png prima di inserirlo nella lista di tutti i frame

        MatOfByte mob=new MatOfByte();
        Imgcodecs.imencode(".png", frame, mob);
        byte ba[]=mob.toArray();
        listFrame.add(ba);

        //scrivo i dati relativi alle coordinate e ROI nel file dataframe.txt
        if(out1!= "") {
            coordsRoiFile.append(out1 + "\n"); //foreach frame
            out+=out1+"\n";
        }
        else{
            coordsRoiFile.append("/\n"); //foreach frame
            out+="/\n";
        }
        ++currFrame;
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

    private void stopAcquisition()
    {
        if (this.timer!=null && !this.timer.isShutdown())
        {
            try
            {
                // stop the timer

                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);

                    this.waitingPanel.writeOnConsole(("Start: Watermark "+"\n"));
                    //prelevo il primo frame
                    byte[] first_frame = listFrame.get(0);
                    //Salvo l'img nella cartella dello script python blind watermark
                    ByteArrayInputStream bis = new ByteArrayInputStream(first_frame);
                    BufferedImage bImage2 = ImageIO.read(bis);
                    ImageIO.write(bImage2, "jpg", new File(ROOTPATH+"/blind_watermark/examples/pic/ori_img.jpg") );

                    //Prendo il contenuto del file dataframe.txt
                    String input = new String(Files.readAllBytes(Paths.get("data/video/out/dataFrame.txt")));

                    //Lo converto in md5 , scrivendo il contenuto in un file json chiaveformattata,valorenonformattato
                    String name_file = "database.json";
                    String key = UtilCompression.write_content(input,name_file);
                    CRC32 crc = new CRC32();
                    crc.update(key.getBytes());
                    String crc_s=crc.getValue()+"";

                    this.waitingPanel.writeOnConsole(("Inserimento watermark... "+"\n"));
                    //key è il dataframe convertito in una stringa molto + piccola
                    String bit_img_wm = UtilWatermarking.execPythonScriptWatermark(ROOTPATH+"/blind_watermark/examples/my_watermark.py", key);
                    System.out.println(bit_img_wm);
                    this.waitingPanel.writeOnConsole(("Watermark inserito "+"\n"));
                    //Leggo il frame watermarkato
                    BufferedImage bImage = ImageIO.read(new File(ROOTPATH+"/blind_watermark/examples/output/embedded.jpg"));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ImageIO.write(bImage, "jpg", bos );
                    //Scrivo il primo frame nel video
                    byte [] data = bos.toByteArray();
                    Mat mat_first_frame = Imgcodecs.imdecode(new MatOfByte(data),Imgcodecs.IMREAD_UNCHANGED);
                    writer.write(mat_first_frame);


                    for(int i=1; i<listFrame.size();++i){
                        this.waitingPanel.writeOnConsole(("Encoding frame : "+i+"/"+totalFrame));
                        Mat mat = Imgcodecs.imdecode(new MatOfByte(listFrame.get(i)),Imgcodecs.IMREAD_UNCHANGED);
                        writer.write(mat);
                        System.gc();    //chiamo garbage per eliminare a ogni write il Mat contenente il frame dalla memoria
                    }


                    listFrame.clear();
                    this.writer.release();
                    System.out.println("Chiamo garbage collector");
                    System.gc();



                    UtilFFmpeg.execFFmpegConvertVideoToMp4Lossless(outfile+File.separator+fileName+".avi",outfile+File.separator+fileName+"_secure.mp4");
                    File f = new File(outfile+File.separator+fileName+".avi");
                    f.delete();
                    this.waitingPanel.writeOnConsole(("Inserimento traccia audio"));
                    UtilFFmpeg.execFFmpegAddAudioTrack(outfile+File.separator+fileName+"_trackAudio.mp3",outfile+File.separator+fileName+"_secure.mp4",outfile+File.separator+fileName+"_secure_withaudio.mp4");
                    f=new File(outfile+File.separator+fileName+"_secure.mp4");
                    f.delete();
                    this.waitingPanel.writeOnConsole(("Inserimento Metadati"));
                    UtilFFmpeg.execFFmpegWriteMetadataOnVideo(outfile+File.separator+fileName+"_secure_withaudio.mp4","album","bit="+bit_img_wm+";crc="+crc_s+";pass="+this.password+"");
                    f=new File(outfile+File.separator+fileName+"_secure_withaudio.mp4");
                    f.delete();
                    
                    if(coordsRoiFile != null)
                        this.coordsRoiFile.close();
                    this.capture.release();

                    //Rimozione video intermedio codec lossless
                    File intermedieVideo = new File(outfile+File.separator+fileName+".avi");
                    //Rimozione traccia audio intermedia
                    File audioTrack = new File(outfile+File.separator+fileName+"_trackAudio.mp3");
                    if(intermedieVideo.exists())intermedieVideo.delete();
                    if(audioTrack.exists())audioTrack.delete();

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
