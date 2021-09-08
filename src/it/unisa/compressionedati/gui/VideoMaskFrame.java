package it.unisa.compressionedati.gui;


import it.unisa.compressionedati.utils.UtilityMaskVideo;
import it.unisa.compressionedati.utils.UtilityUnmaskVideo;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.video.Video;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class VideoMaskFrame extends JFrame {

    private final int FRAME_WIDTH = 400;
    private final Semaphore semaforo;
    private final int FRAME_HEIGHT = 550;
    private final String PATH_DATA = StartFrame.ROOTPATH+File.separator+"data";


    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public VideoMaskFrame(){
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setResizable(false);
        createMainPanel();
        semaforo = new Semaphore(1);
    }

    public void createMainPanel(){
        JPanel panel = new JPanel();
        //panel.setLayout(new GridLayout(1, 1, 0, 14));
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new GridLayout(2,1));
        JPanel compression = createCompressionPanel();
        JPanel decompression = createDecompressionPanel();
        JPanel indietro = createIndietroPanel();

        panel.add(innerPanel);
        innerPanel.add(compression);
        innerPanel.add(decompression);
        panel.add(indietro, BorderLayout.SOUTH);

        add(panel);
    }

    public JPanel createCompressionPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 1, 0, 5));

        ArrayList<String> strs = populateComboFileName("compress");
        // Sort dell'arraylist
        strs.sort(String::compareToIgnoreCase);
        JComboBox combo = new JComboBox(strs.toArray());

        ArrayList<String> mask= populateComboMask();
        mask.sort(String::compareToIgnoreCase);
        JComboBox cmbMask = new JComboBox(mask.toArray());

        ArrayList<String> classifier = new ArrayList<>();
        classifier.add("Haar Frontal Face");
        //classifier.add("Haar Eye");
        JComboBox cmbClassifier = new JComboBox(classifier.toArray());

        //ArrayList<String> encryption = populateEncrypt();
        //JComboBox cmbEnc = new JComboBox(encryption.toArray());

        TextField textPassword = new TextField();
        textPassword.setText("password");
        textPassword.setBackground(Color.WHITE);
        JButton btn = new JButton("Compress");

        class clickButton implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                String video = PATH_DATA +File.separator+ "video"+File.separator+"in"+File.separator + combo.getSelectedItem().toString();
                String mask = PATH_DATA + File.separator+"video"+File.separator+"mask"+File.separator+cmbMask.getSelectedItem().toString();
                String PATH_OUT = PATH_DATA+File.separator+"video"+File.separator+"out";
                String classifierType= cmbClassifier.getSelectedItem().toString();
                String fileName= (combo.getSelectedItem().toString()).replace(".mp4","");
                String pass = textPassword.getText();
                try {
                    semaforo.acquire();

                    //Start Masking
                    WaitingPanelFrame waitingFrame= new WaitingPanelFrame(VideoMaskFrame.this);
                    UtilityMaskVideo videoMask = new UtilityMaskVideo(video,PATH_OUT,mask, semaforo,classifierType, fileName,waitingFrame,pass);
                    videoMask.startMasking();


                } catch (IOException | InterruptedException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

        ActionListener listener = new clickButton();
        btn.addActionListener(listener);

        panel.add(new JLabel("Video Input"));
        panel.add(combo);
        panel.add(new JLabel("Maschera"));
        panel.add(cmbMask);
        panel.add(new JLabel("Tipo di Riconoscimento"));
        panel.add(cmbClassifier);

        panel.add(textPassword);
        panel.add(btn);
        panel.setBorder(new TitledBorder(new EtchedBorder(), "Compression"));

        return panel;
    }

    public JPanel createDecompressionPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 0, 5));

        ArrayList<String> strs = populateComboFileName("decompress");
        // Sort dell'arraylist
        strs.sort(String::compareToIgnoreCase);
        JComboBox combo = new JComboBox(strs.toArray());

        TextField textPassword = new TextField();
        textPassword.setText("password");
        textPassword.setBackground(Color.WHITE);
        JButton btn = new JButton("Decompress");
        class clickButton implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                String video = PATH_DATA +File.separator+ "video"+File.separator+"out"+File.separator + combo.getSelectedItem().toString();

                String PATH_OUT = PATH_DATA+File.separator+"video"+File.separator+"out";
                String fileName= (combo.getSelectedItem().toString()).replace(".mp4","");
                String pass = textPassword.getText();
                try {
                    semaforo.acquire();

                    WaitingPanelFrame waitingFrame= new WaitingPanelFrame(VideoMaskFrame.this);
                    UtilityUnmaskVideo videoUnmask = new UtilityUnmaskVideo(video, PATH_OUT, semaforo, fileName, waitingFrame, pass);
                    videoUnmask.startUnmasking();

                } catch (IOException | InterruptedException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

        ActionListener listener = new clickButton();
        btn.addActionListener(listener);

        panel.add(new JLabel("Video Input"));
        panel.add(combo);

        panel.add(textPassword);
        panel.add(btn);
        panel.setBorder(new TitledBorder(new EtchedBorder(), "Decompression"));

        return panel;
    }

    public JPanel createIndietroPanel(){
        JPanel panel = new JPanel();

        JButton btn = new JButton("Back");

        class clickButton implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                StartFrame frame = new StartFrame();
                frame.setLocationRelativeTo(null);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                VideoMaskFrame.this.dispose();
            }
        }

        ActionListener listener = new clickButton();
        btn.addActionListener(listener);

        panel.add(btn);

        return panel;
    }

    public ArrayList<String> populateComboFileName(String mode) {
        File folder = null;
        if(mode.equals("compress"))
            folder = new File(PATH_DATA + File.separator+"video"+File.separator+"in");
        if(mode.equals("decompress"))
            folder = new File(PATH_DATA + File.separator+"video"+File.separator+"out");

        File[] files = folder.listFiles();
        ArrayList<String> classifiers = new ArrayList<>();

        if(files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if(files[i] != null) {
                        if(mode.equals("decompress"))
                            classifiers.add(files[i].getName());
                        else if(mode.equals("compress"))
                            classifiers.add(files[i].getName());
                    }
                }
            }
        } else {
            classifiers.add("empty");
        }
        return classifiers;
    }

    public ArrayList<String> populateComboMask() {
        File folder = new File(PATH_DATA + File.separator+"video"+File.separator+"mask");
        File[] files = folder.listFiles();
        ArrayList<String> masks = new ArrayList<>();


        if(files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if(files[i] != null) {
                        masks.add(files[i].getName());
                    }
                }
            }
        } else {
            masks.add("empty");
        }
        return masks;
    }
    public void cleanDirectory(String directory) {
        File folder = new File(directory);
        File[] files = folder.listFiles();

        if(files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if(files[i] != null) {
                        files[i].delete();
                    }
                }
            }
        }
    }

    public void showDialog(String msg) {
        JOptionPane.showMessageDialog(
                null,
                msg,
                "Compression dialog",
                JOptionPane.PLAIN_MESSAGE
        );
    }
}
