package deviation.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JLabel;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import de.ailis.usb4java.libusb.LibUsb;
import deviation.*;
import deviation.DevoFat.FatStatus;

import javax.swing.JTextArea;


public class DeviationUploadGUI {

    private JFrame frame;
    private JTextField txtTransmitter;
    private JTable table;

    private JTextArea msgTextArea;
    private JProgressBar progressBar;
    //private final Action action = new SwingAction();

    private TxInfo txInfo;
    private MonitorUSB monitor;
    private DevoFat.FatStatus fatStatus;
    private List<DfuMemory> devMemory;

    private DfuSendTab DfuSendPanel;
    private InstallTab InstallPanel;
    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        DeviationUploadGUI window = new DeviationUploadGUI();
                        window.frame.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {}
    }

    /**
     * Create the application.
     */
    public DeviationUploadGUI() {
        /*
	    byte[] b = new byte[] { 
	            0, 0, 0, 0, 0, 0, 0, 0, 'D', 'E', 'V', 'O', '-', '0', '8', 0,
	            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	            };
	    txInfo = new TxInfo(b);
         */
        txInfo = new TxInfo();
        monitor = new MonitorUSB(this);
        //redirectSystemStreams();
        initialize();
        RefreshDevices(null);
        LibUsb.init(null);
        Timer timer1 = new Timer();
        
        timer1.schedule(monitor, 0, 5000);
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
    	DeviationVersion ver = new DeviationVersion();
        frame = new JFrame();
        frame.setBounds(100, 100, 590, 480);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle(ver.name() + " - " + ver.version());
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{0, 0};
        gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{1.0, 1.0, 1.0, 0.0, Double.MIN_VALUE};
        frame.getContentPane().setLayout(gridBagLayout);

        JPanel panel = new JPanel();
        GridBagConstraints gbc_panel = new GridBagConstraints();
        gbc_panel.insets = new Insets(0, 0, 5, 0);
        gbc_panel.fill = GridBagConstraints.BOTH;
        gbc_panel.gridx = 0;
        gbc_panel.gridy = 0;
        frame.getContentPane().add(panel, gbc_panel);
        GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.columnWidths = new int[]{99, 100, 0};
        gbl_panel.rowHeights = new int[]{15, 43, 0};
        gbl_panel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_panel.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        panel.setLayout(gbl_panel);

        JLabel lblTransmitter = new JLabel("Transmitter");
        GridBagConstraints gbc_lblTransmitter = new GridBagConstraints();
        gbc_lblTransmitter.insets = new Insets(0, 0, 5, 5);
        gbc_lblTransmitter.anchor = GridBagConstraints.WEST;
        gbc_lblTransmitter.gridx = 0;
        gbc_lblTransmitter.gridy = 0;
        panel.add(lblTransmitter, gbc_lblTransmitter);

        txtTransmitter = new JTextField();
        txtTransmitter.setEditable(false);
        GridBagConstraints gbc_txtTransmitter = new GridBagConstraints();
        gbc_txtTransmitter.insets = new Insets(0, 0, 5, 0);
        gbc_txtTransmitter.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtTransmitter.gridx = 1;
        gbc_txtTransmitter.gridy = 0;
        panel.add(txtTransmitter, gbc_txtTransmitter);
        txtTransmitter.setColumns(10);

        TableModel dataModel = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;
            private String[] columnNames = {"Name", "Start Address", "Size", "# Sectors", "Sector Size"};
            public String getColumnName(int col) {
                return columnNames[col];
            }
            public int getColumnCount() { return columnNames.length; }
            public int getRowCount() { return devMemory.size();}
            public boolean isCellEditable(int row, int col) { return false; }
            public Object getValueAt(int row, int col) {
                DfuMemory mem = devMemory.get(row);
                Sector sector = mem.find((int)mem.findStartingAddress());
                switch(col) {
                case 0: return mem.name();
                case 1: return String.format("0x%08x", sector.start());
                case 2: return String.valueOf(sector.size() * sector.count() / 1024) + " kb";
                case 3: return sector.count();
                case 4: return String.valueOf(sector.size() / 1024) + " kb";
                default: return "";
                }
            }
        };
        table = new JTable(dataModel);
        table.setRowSelectionAllowed(false);

        JScrollPane tblScroll = new JScrollPane(table);
        GridBagConstraints gbc_table = new GridBagConstraints();
        gbc_table.gridwidth = 2;
        gbc_table.insets = new Insets(0, 0, 0, 5);
        gbc_table.fill = GridBagConstraints.BOTH;
        gbc_table.gridx = 0;
        gbc_table.gridy = 1;
        panel.add(tblScroll, gbc_table);

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        GridBagConstraints gbc_tabbedPane = new GridBagConstraints();
        gbc_tabbedPane.insets = new Insets(0, 0, 5, 0);
        gbc_tabbedPane.fill = GridBagConstraints.BOTH;
        gbc_tabbedPane.gridx = 0;
        gbc_tabbedPane.gridy = 1;
        frame.getContentPane().add(tabbedPane, gbc_tabbedPane);
        
        JScrollPane txtScroll = new JScrollPane(msgTextArea);
        GridBagConstraints gbc_msgTextArea = new GridBagConstraints();
        gbc_msgTextArea.insets = new Insets(0, 0, 5, 0);
        gbc_msgTextArea.fill = GridBagConstraints.BOTH;
        gbc_msgTextArea.gridx = 0;
        gbc_msgTextArea.gridy = 2;
        frame.getContentPane().add(txtScroll, gbc_msgTextArea);

        progressBar = new JProgressBar();
        progressBar.setMinimum( 0 );
        progressBar.setMaximum( 100 );
        GridBagConstraints gbc_progressBar = new GridBagConstraints();
        gbc_progressBar.fill = GridBagConstraints.HORIZONTAL;
        gbc_progressBar.gridx = 0;
        gbc_progressBar.gridy = 3;
        frame.getContentPane().add(progressBar, gbc_progressBar);

        //Create tabs
        InstallPanel = new InstallTab(this);
        tabbedPane.addTab("Install/Upgrade", null, InstallPanel, null);
        tabbedPane.setEnabledAt(0, true);
        
        DfuSendPanel = new DfuSendTab(this, txInfo, progressBar);
        tabbedPane.addTab("DFU", null, DfuSendPanel, null);
        tabbedPane.setEnabledAt(1, true);

        JPanel BinSendPanel = new JPanel();
        tabbedPane.addTab("Bin-Send", null, BinSendPanel, null);
        tabbedPane.setEnabledAt(2, false);
        
        //JPanel BinFetchPanel = new JPanel();
        //tabbedPane.addTab("Bin-Fetch", null, BinFetchPanel, null);
        //tabbedPane.setEnabledAt(2, false);

        msgTextArea = new JTextArea();
        msgTextArea.setRows(8);
        msgTextArea.setEditable(false);

    }
    public void RefreshDevices(List<DfuDevice> devs) {
        //Update USB Device list entries
        if (devs == null) {
            txInfo = new TxInfo();
            devMemory = new ArrayList<DfuMemory>();
            fatStatus = FatStatus.NO_FAT;
        } else {
            txInfo = TxUtils.getTxInfo(devs.get(0));
            fatStatus = TxUtils.getFatStatus(devs.get(0), txInfo.type());
            devMemory = new ArrayList<DfuMemory>();
            for (DfuInterface iface : devs.get(0).Interfaces()) {
                devMemory.add(iface.Memory());
            }
            monitor.ReleaseDevices();
        }
        txtTransmitter.setText(TxInfo.typeToString(txInfo.type()));
        AbstractTableModel tableModel = (AbstractTableModel) table.getModel();
        tableModel.fireTableDataChanged();
        DfuSendPanel.refresh();
        InstallPanel.refresh();
    }
    public void RefreshDevices() {
        List<DfuDevice> devs = monitor.GetDevices();
        RefreshDevices(devs);
    }
    /*
	private class SwingAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        public SwingAction() {
			putValue(NAME, "...");
			putValue(SHORT_DESCRIPTION, "Open File Chooser Dialog");
		}
		public void actionPerformed(ActionEvent e) {
		}
	}
     */


    private void updateTextArea(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                msgTextArea.append(text);
            }
        });
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                updateTextArea(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateTextArea(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }
    
    public MonitorUSB getMonitor() { return monitor; }
    public JProgressBar getProgressBar() { return progressBar; }
    public TxInfo getTxInfo() {return txInfo;}
    public FatStatus getFatType() { return fatStatus; }
}