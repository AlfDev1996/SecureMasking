package it.unisa.compressionedati.gui;


import it.unisa.compressionedati.utils.UtilFiles;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;

public class StartFrame extends JFrame {

    private final int FRAME_WIDTH = 280;
    private final int FRAME_HEIGHT = 370;
    public static final String ROOTPATH= new File("").getAbsolutePath();
    public StartFrame(){

        //read prop file and set some paths
        Properties prop = UtilFiles.readPropertyFile("settings.properties");

        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setResizable(false);
        createMainPanel();
    }

    public void createMainPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(8, 1, 0, 14));

        JPanel title = createTitlePanel();
        JPanel project = createProjectPanel();
        JPanel videoMask = createVideoCompressPanel();
        JPanel exit = createExitPanel();
        JPanel authors = createAuthorsPanel();

        panel.add(title);
        panel.add(project);
        panel.add(videoMask);
        panel.add(exit);
        panel.add(authors);

        add(panel);
    }

    public JPanel createTitlePanel(){
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Data Compression Project");
        label.setBorder(BorderFactory.createEmptyBorder(9, 5, 0, 0));
        label.setFont(new Font("SansSerif", Font.PLAIN , 14));
        panel.add(label);
        return panel;
    }

    public JPanel createProjectPanel(){
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Secure Masking Video");
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        label.setFont(new Font("SansSerif", Font.BOLD , 16));
        panel.add(label);
        return panel;
    }



    public JPanel createExitPanel(){
        JPanel panel = new JPanel();
        JButton btn = new JButton("Exit");
        //btn.setPreferredSize(new Dimension(150, 31));
        panel.add(btn);

        class clickButton implements ActionListener{
            public void actionPerformed(ActionEvent e) {
                StartFrame.this.dispose();
            }
        }

        ActionListener listener = new clickButton();
        btn.addActionListener(listener);

        return panel;
    }

    public JPanel createVideoCompressPanel(){
        JPanel panel = new JPanel();
        JButton btn = new JButton("Video Mask");
        //btn.setPreferredSize(new Dimension(150, 31));
        panel.add(btn);

        class clickButton implements ActionListener{
            public void actionPerformed(ActionEvent e) {
                VideoMaskFrame frame = new VideoMaskFrame();
                frame.setLocationRelativeTo(null);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                StartFrame.this.dispose();
            }
        }

        ActionListener listener = new clickButton();
        btn.addActionListener(listener);

        return panel;
    }

    public JPanel createAuthorsPanel(){
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Buonincontri,Dragone,Rianna");
        label.setFont(new Font("SansSerif", Font.PLAIN , 14));
        panel.add(label);

        return panel;
    }

}
