package com.alphaolomi;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;

public class DownloadManager extends JFrame implements Observer {

    private static final long serialVersionUID = 1L;
    private final JTextField addTextField = new JTextField(30);
    private final DownloadsTableModel tableModel = new DownloadsTableModel();
    private final JTable table;
    private final JButton pauseButton = new JButton("Pause");
    private final JButton resumeButton = new JButton("Resume");
    private final JButton cancelButton;
    private final JButton clearButton;
    private Download selectedDownload;
    private boolean clearing;

    public DownloadManager() {
        setTitle("Download Manager");
        setSize(640, 480);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(e -> System.exit(0));
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Set up add panel.
        JPanel addPanel = new JPanel();


        addPanel.add(addTextField);
        JButton addButton = new JButton("Add Download");
        addButton.addActionListener(e -> actionAdd());
        addPanel.add(addButton);

        // Set up Downloads table.
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(e -> tableSelectionChanged());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        ProgressRenderer renderer = new ProgressRenderer(0, 100);
        renderer.setStringPainted(true); // show progress text
        table.setDefaultRenderer(JProgressBar.class, renderer);

        table.setRowHeight((int) renderer.getPreferredSize().getHeight());

        JPanel downloadsPanel = new JPanel();
        downloadsPanel.setBorder(BorderFactory.createTitledBorder("Downloads"));
        downloadsPanel.setLayout(new BorderLayout());
        downloadsPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();

        pauseButton.addActionListener(e -> actionPause());
        pauseButton.setEnabled(false);
        buttonsPanel.add(pauseButton);

        resumeButton.addActionListener(e -> actionResume());
        resumeButton.setEnabled(false);
        buttonsPanel.add(resumeButton);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> actionCancel());
        cancelButton.setEnabled(false);
        buttonsPanel.add(cancelButton);
        clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> actionClear());
        clearButton.setEnabled(false);
        buttonsPanel.add(clearButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(addPanel, BorderLayout.NORTH);
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void actionAdd() {
        URL verifiedUrl = verifyUrl(addTextField.getText());

        if (verifiedUrl != null) {
            tableModel.addDownload(new Download(verifiedUrl));
            addTextField.setText(""); // reset add text field
        } else {
            JOptionPane.showMessageDialog(this, "Invalid Download URL", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private URL verifyUrl(String url) {
        if (!url.toLowerCase().startsWith("http://"))
            return null;

        URL verifiedUrl;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }

        if (verifiedUrl.getFile().length() < 2)
            return null;

        return verifiedUrl;
    }

    private void tableSelectionChanged() {
        if (selectedDownload != null)
            selectedDownload.deleteObserver(DownloadManager.this);

        if (!clearing && table.getSelectedRow() > -1) {
            selectedDownload = tableModel.getDownload(table.getSelectedRow());
            selectedDownload.addObserver(DownloadManager.this);
            updateButtons();
        }
    }

    private void actionPause() {
        selectedDownload.pause();
        updateButtons();
    }

    private void actionResume() {
        selectedDownload.resume();
        updateButtons();
    }

    private void actionCancel() {
        selectedDownload.cancel();
        updateButtons();
    }

    private void actionClear() {
        clearing = true;
        tableModel.clearDownload(table.getSelectedRow());
        clearing = false;
        selectedDownload = null;
        updateButtons();
    }

    private void updateButtons() {
        if (selectedDownload != null) {
            int status = selectedDownload.getStatus();
            switch (status) {
                case Download.DOWNLOADING:
                    pauseButton.setEnabled(true);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    break;
                case Download.PAUSED:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);

                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    break;
                case Download.ERROR:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
                    break;
                default: // COMPLETE or CANCELLED
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
            }
        } else {
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            cancelButton.setEnabled(false);
            clearButton.setEnabled(false);
        }
    }

    public void update(Observable o, Object arg) {
        // Update buttons if the selected download has changed.
        if (selectedDownload != null && selectedDownload.equals(o)) updateButtons();
    }

    // Run the Download Manager.
    public static void main(String[] args) {
        DownloadManager manager = new DownloadManager();
        manager.setVisible(true);
    }
}
 