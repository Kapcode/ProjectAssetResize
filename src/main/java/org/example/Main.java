package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static File currentDirectory;

    public static void main(String[] args) {
        ImageIO.setUseCache(false);
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Asset Resizer");
            setAppIcon(frame);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            // --- UI Components ---
            DefaultListModel<String> listModel = new DefaultListModel<>();
            JList<String> assetList = new JList<>(listModel);
            assetList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            JScrollPane assetListScrollPane = new JScrollPane(assetList);

            JPanel optionsPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; gbc.gridy = 0; optionsPanel.add(new JLabel("Width:"), gbc);
            gbc.gridx = 1; JTextField widthField = new JTextField(5); optionsPanel.add(widthField, gbc);
            gbc.gridx = 0; gbc.gridy = 1; optionsPanel.add(new JLabel("Height:"), gbc);
            gbc.gridx = 1; JTextField heightField = new JTextField(5); optionsPanel.add(heightField, gbc);
            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
            JButton resizeButton = new JButton("Resize");
            optionsPanel.add(resizeButton, gbc);

            JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, assetListScrollPane, optionsPanel);
            leftSplitPane.setDividerLocation(400);

            JPanel rightPane = new JPanel(new BorderLayout());
            JLabel imageLabel = new JLabel("(No image selected)", SwingConstants.CENTER);
            JLabel sizeLabel = new JLabel("Size: N/A", SwingConstants.CENTER);
            rightPane.add(new JScrollPane(imageLabel), BorderLayout.CENTER);
            rightPane.add(sizeLabel, BorderLayout.SOUTH);

            JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, rightPane);
            mainSplitPane.setDividerLocation(250);

            frame.getContentPane().add(mainSplitPane, BorderLayout.CENTER);

            // --- Menu Bar ---
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            JMenuItem openMenuItem = new JMenuItem("Open Directory...");
            JMenuItem saveAsMenuItem = new JMenuItem("Save As...");
            menuBar.add(fileMenu);
            fileMenu.add(openMenuItem);
            fileMenu.add(saveAsMenuItem);
            frame.setJMenuBar(menuBar);

            // --- Action Listeners ---
            assetList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    String selectedAsset = assetList.getSelectedValue();
                    if (selectedAsset == null) return;

                    try {
                        File assetFile = new File(currentDirectory, selectedAsset);
                        BufferedImage image = ImageIO.read(assetFile);
                        if (image != null) {
                            imageLabel.setIcon(new ImageIcon(image));
                            imageLabel.setText(null);
                            sizeLabel.setText("Size: " + image.getWidth() + "x" + image.getHeight());
                        } else {
                            imageLabel.setIcon(null);
                            imageLabel.setText("Not an image file");
                            sizeLabel.setText("Size: N/A");
                        }
                    } catch (IOException ex) {
                        imageLabel.setIcon(null);
                        imageLabel.setText("Error reading image");
                        sizeLabel.setText("Size: N/A");
                    }
                }
            });

            openMenuItem.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    loadAssetsFromDirectory(listModel, fileChooser.getSelectedFile());
                }
            });

            resizeButton.addActionListener(e -> {
                List<String> selectedAssets = assetList.getSelectedValuesList();
                if (selectedAssets.isEmpty()) return;

                try {
                    int width = Integer.parseInt(widthField.getText());
                    int height = Integer.parseInt(heightField.getText());
                    if (width <= 0 || height <= 0) {
                        JOptionPane.showMessageDialog(frame, "Width and height must be positive numbers.", "Invalid Dimensions", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    List<String> failedFiles = new ArrayList<>();
                    for (String selectedAsset : selectedAssets) {
                        File assetFile = new File(currentDirectory, selectedAsset);
                        try {
                            BufferedImage originalImage = ImageIO.read(assetFile);
                            if (originalImage == null) {
                                failedFiles.add(selectedAsset + " (not a valid image)");
                                continue;
                            }
                            int imageType = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
                            BufferedImage resizedImage = new BufferedImage(width, height, imageType);
                            Graphics2D g2d = resizedImage.createGraphics();
                            g2d.drawImage(originalImage, 0, 0, width, height, null);
                            g2d.dispose();

                            String formatName = selectedAsset.substring(selectedAsset.lastIndexOf('.') + 1);
                            if (!ImageIO.write(resizedImage, formatName, assetFile)) {
                                failedFiles.add(selectedAsset + " (write failed)");
                            }
                        } catch (IOException ex) {
                            failedFiles.add(selectedAsset + " (IO Error)");
                        }
                    }

                    int[] selectedIndices = assetList.getSelectedIndices();
                    assetList.clearSelection();
                    assetList.setSelectedIndices(selectedIndices);

                    if (failedFiles.isEmpty()) {
                        JOptionPane.showMessageDialog(frame, "Resize complete!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(frame, "Could not resize the following files:\n" + String.join("\n", failedFiles), "Resize Error", JOptionPane.ERROR_MESSAGE);
                    }

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Please enter valid numbers for width and height.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                }
            });

            saveAsMenuItem.addActionListener(e -> {
                if (assetList.getSelectedValuesList().size() != 1) {
                    JOptionPane.showMessageDialog(frame, "Please select a single image to save.", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String selectedAsset = assetList.getSelectedValue();

                try {
                    int width = Integer.parseInt(widthField.getText());
                    int height = Integer.parseInt(heightField.getText());
                    if (width <= 0 || height <= 0) {
                        JOptionPane.showMessageDialog(frame, "Width and height must be positive numbers.", "Invalid Dimensions", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Save As");
                    String originalFormatName = selectedAsset.substring(selectedAsset.lastIndexOf('.') + 1);
                    fileChooser.setSelectedFile(new File("resized_" + selectedAsset));

                    if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        File fileToSave = fileChooser.getSelectedFile();
                        File assetFile = new File(currentDirectory, selectedAsset);
                        BufferedImage originalImage = ImageIO.read(assetFile);

                        if (originalImage != null) {
                            int imageType = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
                            BufferedImage resizedImage = new BufferedImage(width, height, imageType);
                            Graphics2D g2d = resizedImage.createGraphics();
                            g2d.drawImage(originalImage, 0, 0, width, height, null);
                            g2d.dispose();

                            if (!ImageIO.write(resizedImage, originalFormatName, fileToSave)) {
                                JOptionPane.showMessageDialog(frame, "Could not save image. Format not supported.", "Write Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Please enter valid numbers for width and height.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Error saving image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            // --- Initial Load ---
            loadAssetsFromDirectory(listModel, new File("C:/Users/prospet0/IdeaProjects/ProjectAssetResize/src/main/java/org/example/dye_slimed_brick"));

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void setAppIcon(JFrame frame) {
        List<Image> icons = new ArrayList<>();
        String[] iconPaths = { "/icons/icon_16.png", "/icons/icon_32.png", "/icons/icon_64.png" };

        for (String path : iconPaths) {
            URL iconURL = Main.class.getResource(path);
            if (iconURL != null) {
                icons.add(new ImageIcon(iconURL).getImage());
            } else {
                System.err.println("Warning: Could not find icon resource: " + path);
            }
        }

        if (!icons.isEmpty()) {
            frame.setIconImages(icons);
        }
    }

    private static void loadAssetsFromDirectory(DefaultListModel<String> listModel, File directory) {
        currentDirectory = directory;
        listModel.clear();
        if (directory != null && directory.isDirectory()) {
            String[] files = directory.list((dir, name) -> name.toLowerCase().matches(".*\\.(png|jpg|jpeg|bmp|gif)$"));
            if (files != null) {
                for (String file : files) {
                    listModel.addElement(file);
                }
            }
        }
    }
}
