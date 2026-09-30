import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;

public class AdvancedFileOrganizer extends JFrame {

    private JTextField pathField;
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JButton btnBrowse, btnOrganize, btnFindDuplicates;

    // Extension Mappings
    private static final Map<String, String> EXTENSION_MAP = new HashMap<>();

    static {
        // Media
        EXTENSION_MAP.put("jpg", "Images"); EXTENSION_MAP.put("jpeg", "Images");
        EXTENSION_MAP.put("png", "Images"); EXTENSION_MAP.put("gif", "Images");
        EXTENSION_MAP.put("mp4", "Videos"); EXTENSION_MAP.put("mkv", "Videos");
        EXTENSION_MAP.put("mp3", "Audio");  EXTENSION_MAP.put("wav", "Audio");

        // Documents
        EXTENSION_MAP.put("pdf", "Documents"); EXTENSION_MAP.put("docx", "Documents");
        EXTENSION_MAP.put("txt", "Documents"); EXTENSION_MAP.put("xlsx", "Documents");
        EXTENSION_MAP.put("pptx", "Documents");

        // Code
        EXTENSION_MAP.put("java", "Code_Files"); EXTENSION_MAP.put("html", "Code_Files");
        EXTENSION_MAP.put("css", "Code_Files");   EXTENSION_MAP.put("js", "Code_Files");
        EXTENSION_MAP.put("py", "Code_Files");   EXTENSION_MAP.put("cpp", "Code_Files");

        // Compressed & Apps
        EXTENSION_MAP.put("zip", "Archives"); EXTENSION_MAP.put("rar", "Archives");
        EXTENSION_MAP.put("exe", "Installers"); EXTENSION_MAP.put("msi", "Installers");
    }

    public AdvancedFileOrganizer() {
        setTitle("Smart File Organizer & Cleanup Utility");
        setSize(650, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Panel: Folder Selection
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        pathField = new JTextField();
        pathField.setEditable(false);
        btnBrowse = new JButton("Browse Folder");

        topPanel.add(new JLabel("Target Directory: "), BorderLayout.WEST);
        topPanel.add(pathField, BorderLayout.CENTER);
        topPanel.add(btnBrowse, BorderLayout.EAST);

        // Center Panel: Logs Output
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);

        // Bottom Panel: Actions & Progress Bar
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));

        btnOrganize = new JButton("Organize Files");
        btnFindDuplicates = new JButton("Find Duplicates");
        btnGroup.add(btnOrganize);
        btnGroup.add(btnFindDuplicates);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        bottomPanel.add(btnGroup, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Action Listeners
        btnBrowse.addActionListener(e -> selectFolder());
        btnOrganize.addActionListener(e -> processOrganization());
        btnFindDuplicates.addActionListener(e -> processDuplicates());
    }

    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            logArea.setText("Selected Path: " + chooser.getSelectedFile().getAbsolutePath() + "\n");
        }
    }

    // Task 1: Organize Files in Background Thread
    private void processOrganization() {
        String dirPath = pathField.getText().trim();
        if (dirPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pehle ek folder select kijiye!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        toggleControls(false);
        logArea.append("\n--- Starting File Organization ---\n");

        SwingWorker<Integer, String> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                File targetFolder = new File(dirPath);
                File[] files = targetFolder.listFiles(File::isFile);

                if (files == null || files.length == 0) {
                    publish("No loose files found to organize.");
                    return 0;
                }

                int movedCount = 0;
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    String ext = getFileExtension(file.getName()).toLowerCase();

                    if (EXTENSION_MAP.containsKey(ext)) {
                        String folderName = EXTENSION_MAP.get(ext);
                        Path targetDir = Paths.get(dirPath, folderName);

                        try {
                            if (!Files.exists(targetDir)) {
                                Files.createDirectories(targetDir);
                            }
                            Path dest = targetDir.resolve(file.getName());
                            Files.move(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                            publish("Moved: " + file.getName() + " -> " + folderName + "/");
                            movedCount++;
                        } catch (IOException ex) {
                            publish("Error moving: " + file.getName());
                        }
                    }
                    int progress = (int) (((i + 1) / (double) files.length) * 100);
                    setProgress(progress);
                }
                return movedCount;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    logArea.append(msg + "\n");
                }
            }

            @Override
            protected void done() {
                try {
                    int count = get();
                    logArea.append("\nDone! Total " + count + " files organized successfully.\n");
                } catch (Exception e) {
                    logArea.append("Process Error: " + e.getMessage() + "\n");
                }
                toggleControls(true);
                progressBar.setValue(100);
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                progressBar.setValue((Integer) evt.getNewValue());
            }
        });

        worker.execute();
    }

    // Task 2: SHA-256 Duplicate File Finder
    private void processDuplicates() {
        String dirPath = pathField.getText().trim();
        if (dirPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pehle ek folder select kijiye!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        toggleControls(false);
        logArea.append("\n--- Scanning for Duplicate Files (SHA-256) ---\n");

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                File targetFolder = new File(dirPath);
                File[] files = targetFolder.listFiles(File::isFile);

                if (files == null || files.length == 0) {
                    publish("No files found to scan.");
                    return null;
                }

                Map<String, List<File>> hashMap = new HashMap<>();

                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    String hash = getFileChecksum(file);

                    if (hash != null) {
                        hashMap.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
                    }

                    int progress = (int) (((i + 1) / (double) files.length) * 100);
                    setProgress(progress);
                }

                publish("\n--- Scan Results ---");
                int duplicatesFound = 0;
                for (Map.Entry<String, List<File>> entry : hashMap.entrySet()) {
                    List<File> fileList = entry.getValue();
                    if (fileList.size() > 1) {
                        duplicatesFound++;
                        publish("\nDuplicate Group (Hash: " + entry.getKey().substring(0, 8) + "...):");
                        for (File f : fileList) {
                            publish("  - " + f.getName());
                        }
                    }
                }

                if (duplicatesFound == 0) {
                    publish("No duplicate files found.");
                } else {
                    publish("\nTotal duplicate groups found: " + duplicatesFound);
                }

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    logArea.append(msg + "\n");
                }
            }

            @Override
            protected void done() {
                toggleControls(true);
                progressBar.setValue(100);
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                progressBar.setValue((Integer) evt.getNewValue());
            }
        });

        worker.execute();
    }

    // SHA-256 Helper Method
    private String getFileChecksum(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }

            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0 && lastDot < fileName.length() - 1) ? fileName.substring(lastDot + 1) : "";
    }

    private void toggleControls(boolean enable) {
        btnBrowse.setEnabled(enable);
        btnOrganize.setEnabled(enable);
        btnFindDuplicates.setEnabled(enable);
        if (!enable) progressBar.setValue(0);
    }

    public static void main(String[] args) {
        // System Native Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new AdvancedFileOrganizer().setVisible(true);
        });
    }
}
