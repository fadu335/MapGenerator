import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import point.Point;
import point.PointGenerator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Gen implements OnsetHandler {

    public static List<Point> points = new ArrayList<>();
    public static JTextArea textArea = new JTextArea(10, 30);

    private int lastX = -1;
    private int lastY = -1;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Point Generator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        JPanel panel = new JPanel();
        JButton button = new JButton("Выбрать песню");
        panel.add(button);
        textArea.setEditable(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane);

        frame.getContentPane().add(BorderLayout.CENTER, panel);
        button.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home"), "Downloads"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Audi Files", "mp3", "wav", "flac", "ogg");
            fileChooser.setFileFilter(filter);

            int returnValue = fileChooser.showOpenDialog(null);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();

                startProcessing(selectedFile.getAbsolutePath());
            }
        });

        frame.setVisible(true);
    }

    private static void startProcessing(String filePath) {
        points.clear();
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(filePath, 44100, 512, 0);
        ComplexOnsetDetector onsetDetector = new ComplexOnsetDetector(512, 0.4, 0.03);
        onsetDetector.setHandler(new Gen());
        dispatcher.addAudioProcessor(onsetDetector);
        dispatcher.run();
        textArea.append(PointGenerator.generateMapPoints(points));
    }

    @Override
    public void handleOnset(double time, double salience) {
        int nextX, nextY;

        if (lastX == -1) {
            nextX = ThreadLocalRandom.current().nextInt(0, 3);
            nextY = ThreadLocalRandom.current().nextInt(0, 3);
        } else {
            List<Point> possibleNextPoints = getPoints();

            if (!possibleNextPoints.isEmpty()) {
                Point chosenPoint = possibleNextPoints.get(ThreadLocalRandom.current().nextInt(possibleNextPoints.size()));
                nextX = (int) chosenPoint.x();
                nextY = (int) chosenPoint.y();
            } else {
                nextX = ThreadLocalRandom.current().nextInt(0, 3);
                nextY = ThreadLocalRandom.current().nextInt(0, 3);
            }
        }

        lastX = nextX;
        lastY = nextY;

        Point point = new Point(nextX, nextY, (float) (time * 1000));
        points.add(point);
    }

    private List<Point> getPoints() {
        List<Point> possibleNextPoints = new ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;

                int newX = lastX + i;
                int newY = lastY + j;

                if (newX >= 0 && newX < 3 && newY >= 0 && newY < 3) {
                    possibleNextPoints.add(new Point(newX, newY, 0));
                }
            }
        }
        return possibleNextPoints;
    }
}
