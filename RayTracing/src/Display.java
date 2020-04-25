
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;

class Display extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    // Global variables
    static int WIDTH = RayMain.WIDTH;
    static int HEIGHT = RayMain.HEIGHT;
    static int SPP = RayMain.SPP;
    static int MAX_DEPTH = RayMain.MAX_DEPTH;
    static int THREADS = RayMain.THREADS;
    public static ImageWindow img;

    private String sString, dString, nString;
    private JTextField sText, dText, nText;

    public Display(String title) {
        super(title); // calling JFrame constructor
        //Create JFrame
        int buffer = 100;
        this.setBounds(10, 10, (int) ((1.5 * this.WIDTH) + buffer),
                this.HEIGHT + buffer);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setBackground(Color.ORANGE);

        //Add button panel
        JPanel leftPanel = new JPanel();
        JPanel leftWindow = new JPanel();
        JPanel leftBuffer = new JPanel();
        leftPanel.setPreferredSize(new Dimension(250, HEIGHT / 2));
        leftWindow.setPreferredSize(new Dimension(250, HEIGHT));
        leftBuffer.setPreferredSize(new Dimension(250, HEIGHT / 2));
        leftWindow.setLayout(new BorderLayout());
        leftPanel.setLayout(new GridLayout(0, 3, 5, 20));
        leftWindow.add(leftPanel, BorderLayout.PAGE_START);
        leftWindow.add(leftBuffer, BorderLayout.PAGE_END);
        this.add(leftWindow, BorderLayout.WEST);

        //Samples Per Thread
        //Add Label
        JLabel sampleLabel = new JLabel("Samples Per Pixel");
        leftPanel.add(sampleLabel);
        //Add button
        JButton sampleButton = new JButton("Update");
        sampleButton.setPreferredSize(new Dimension(40, 40));
        sampleButton.addActionListener(this);
        sampleButton.setMaximumSize(new Dimension(40, 40));
        leftPanel.add(sampleButton);
        //Text Panel
        this.sText = new JTextField();
        leftPanel.add(this.sText);
        this.sString = String.valueOf(this.SPP);
        this.sText.setText(this.sString);

        //Max depth
        //Add Label
        JLabel depthLabel = new JLabel("Max Depth");
        leftPanel.add(depthLabel);
        //Add button
        JButton depthButton = new JButton("Update");
        depthButton.setPreferredSize(new Dimension(40, 40));
        depthButton.addActionListener(this);
        leftPanel.add(depthButton);
        //Text Panel
        this.dText = new JTextField();
        leftPanel.add(this.dText);
        this.dString = String.valueOf(this.MAX_DEPTH);
        this.dText.setText(this.dString);

        //Max threads
        //Add Label
        JLabel threadLabel = new JLabel("Num of Threads");
        leftPanel.add(threadLabel);
        //Add button
        JButton threadButton = new JButton("Update");
        threadButton.setPreferredSize(new Dimension(40, 40));
        threadButton.addActionListener(this);
        leftPanel.add(threadButton);
        //Text Panel
        this.nText = new JTextField();
        leftPanel.add(this.nText);
        this.nString = String.valueOf(this.THREADS);
        this.nText.setText(this.nString);

        //Render Button
        JButton renderButton = new JButton("Render");
        renderButton.setPreferredSize(new Dimension(40, 40));
        renderButton.addActionListener(this);
        leftPanel.add(renderButton);

        //Add menu panel
        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);
        JMenu file = new JMenu("File");

        //Save button
        JMenuItem save = new JMenuItem("Save Image");
        save.addActionListener(this);
        file.add(save);
        //Quit button
        JMenuItem quit = new JMenuItem("Quit");
        quit.addActionListener(this);
        file.add(quit);

        menuBar.add(file);

        //Add Rendering Window
        img = new ImageWindow(WIDTH, HEIGHT);
        img.setBackground(Color.RED);
        this.add(img);
        this.validate();

    }

    // Button Functions
    @Override
    public void actionPerformed(ActionEvent ae) {
        String choice = ae.getActionCommand();
        //Render the drawing again
        //Call a new thread to start the rendering process
        if (choice.equals("Render")) {
            //Reset drawing space
            img.img.getGraphics().clearRect(0, 0, WIDTH, HEIGHT);
            //Call render with new thread
            Thread split = new Thread() {
                @Override
                public void run() {
                    RayMain.callRayMain(img);
                }
            };
            split.start();
        } else if (choice.equals("Update")) {
            SPP = Integer.parseInt(this.sText.getText());
            MAX_DEPTH = Integer.parseInt(this.dText.getText());
            THREADS = Integer.parseInt(this.nText.getText());
            RayMain.update();
        } else if (choice.equals("Save Image")) {
            //Not yet implemented
        } else if (choice.equals("Quit")) {
            System.exit(0);
        } else {
            System.out.print("fail");

        }
    }

    public static void main() {
        Display myApp = new Display("Java Ray Tracing");
        myApp.setVisible(true);
        myApp.pack();

    }
}
