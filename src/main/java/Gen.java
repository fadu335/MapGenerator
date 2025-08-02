import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import com.formdev.flatlaf.FlatLightLaf;
import difficulty.Difficulty;
import point.Point;
import point.PointGenerator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Gen implements OnsetHandler {

    private static final List<Point> points = new ArrayList<>();
    private static final JTextArea textArea = new JTextArea(15, 30);

    private int lastX = -1;
    private int lastY = -1;
    private static Difficulty difficulty = Difficulty.EASY;

    public static void main(String[] args) {
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(Gen::createAndShowGui);
    }

    private static void createAndShowGui() {
        JFrame frame = new JFrame("Point Generator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 900);
        JPanel panel = new JPanel();
        JButton button = new JButton("Choose a song");
        JButton copyButton = new JButton("Copy");
        JButton difficultyButton = new JButton("Difficulty: EASY");

        panel.add(button);
        panel.add(copyButton);
        panel.add(difficultyButton);

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

        copyButton.addActionListener(e -> {
            String textToCopy = textArea.getText();
            if (!textToCopy.isEmpty()) {
                StringSelection stringSelection = new StringSelection(textToCopy);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
                JOptionPane.showMessageDialog(frame, "The text has been copied to the clipboard!");
            } else {
                JOptionPane.showMessageDialog(frame, "There is no data to copy", "Error", JOptionPane.WARNING_MESSAGE);
            }
        });

        difficultyButton.addActionListener(e -> {
            difficulty = difficulty.next();
            difficultyButton.setText("Difficulty: " + difficulty.name());
        });

        frame.pack();
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
        generatePoint(time);

        if (difficulty == Difficulty.HARD && ThreadLocalRandom.current().nextDouble() < 0.15) {
            generatePoint(time + 0.03);
        }
    }

    private void generatePoint(double time) {
        int nextX, nextY;

        if (lastX == -1) {
            nextX = ThreadLocalRandom.current().nextInt(0, 3);
            nextY = ThreadLocalRandom.current().nextInt(0, 3);
        } else {
            if (difficulty == Difficulty.HARD) {
                if (ThreadLocalRandom.current().nextDouble() < 0.7) {
                    int patternType = ThreadLocalRandom.current().nextInt(0, 3);
                    nextY = switch (patternType) {
                        case 0 -> {
                            nextX = Math.min(lastX + 1, 2);
                            yield lastY;
                        }
                        case 1 -> {
                            nextX = Math.min(lastX + 1, 2);
                            yield Math.min(lastY + 1, 2);
                        }
                        default -> {
                            nextX = ThreadLocalRandom.current().nextInt(0, 3);
                            yield ThreadLocalRandom.current().nextInt(0, 3);
                        }
                    };
                } else {
                    nextX = ThreadLocalRandom.current().nextInt(0, 3);
                    nextY = ThreadLocalRandom.current().nextInt(0, 3);
                }
            } else if (difficulty == Difficulty.EASY || difficulty == Difficulty.MEDIUM) {
                List<Point> possibleNextPoints = getPoints();
                Point chosen = possibleNextPoints.get(ThreadLocalRandom.current().nextInt(possibleNextPoints.size()));
                nextX = (int) chosen.x();
                nextY = (int) chosen.y();
            } else {
                nextX = ThreadLocalRandom.current().nextInt(0, 3);
                nextY = ThreadLocalRandom.current().nextInt(0, 3);
            }
        }

        nextX = Math.max(0, Math.min(2, nextX));
        nextY = Math.max(0, Math.min(2, nextY));

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
