package deviation.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import org.fife.rsta.ui.CollapsibleSectionPanel;
//import org.fife.rsta.ui.DocumentMap;
import org.fife.rsta.ui.GoToDialog;
import org.fife.rsta.ui.SizeGripIcon;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.rsta.ui.search.FindToolBar;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import deviation.FileInfo;


/**
 * An application that demonstrates use of the RSTAUI project.  Please don't
 * take this as good application design; it's just a simple example.<p>
 *
 * Unlike the library itself, this class is public domain.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class TextEditor extends JDialog implements SearchListener {

	private static final long serialVersionUID = 1L;
	private CollapsibleSectionPanel csp;
	private RSyntaxTextArea textArea;
	private FindDialog findDialog;
	private ReplaceDialog replaceDialog;
	private FindToolBar findToolBar;
	//private ReplaceToolBar replaceToolBar;
	private StatusBar statusBar;
	private String finalData;
	private FileInfo fileinfo;
	private boolean changed;


	public TextEditor(FileInfo file) {
		fileinfo = file;		
		changed = false;
		finalData = new String(file.data());

		initSearchDialogs();

		JPanel contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);
		csp = new CollapsibleSectionPanel();
		contentPane.add(csp);

		setJMenuBar(createMenuBar());
		
		textArea = new RSyntaxTextArea(25, 80);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setCodeFoldingEnabled(true);
		textArea.setMarkOccurrences(true);
		textArea.append(finalData);
		Font f = textArea.getFont();
		System.out.format("Font: %d", f.getSize());
		f = new Font(f.getFamily(), f.getStyle(), f.getSize()+5);
		textArea.setFont(f);
		RTextScrollPane sp = new RTextScrollPane(textArea);
		csp.add(sp);
		csp.addBottomComponent(findToolBar);
		csp.showBottomComponent(findToolBar);

		ErrorStrip errorStrip = new ErrorStrip(textArea);
		contentPane.add(errorStrip, BorderLayout.LINE_END);
//org.fife.rsta.ui.DocumentMap docMap = new org.fife.rsta.ui.DocumentMap(textArea);
//contentPane.add(docMap, BorderLayout.LINE_END);

		statusBar = new StatusBar();
		contentPane.add(statusBar, BorderLayout.SOUTH);

		setTitle(file.name());
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() 
		{
			  public void windowClosing(WindowEvent e)
			  {
				if (! textArea.getText().equals(finalData)) {
					int result = JOptionPane.showConfirmDialog(null,
							"There are unsaved changes\nWould you like to save before exiting?",
							"alert", JOptionPane.YES_NO_CANCEL_OPTION);
					if (result == JOptionPane.CANCEL_OPTION) {
						return;
					}
					if (result == JOptionPane.YES_OPTION) {
						finalData = textArea.getText();
						changed = true;
					}
				}
				fileinfo.setData(finalData.getBytes());
				dispose();
			  }
			});
	}

	public FileInfo getFileInfo() { return fileinfo; }
	public boolean changed() { return changed; }

	private JMenuBar createMenuBar() {

		JMenuBar mb = new JMenuBar();
		JMenu menu = new JMenu("File");
		menu.add(new JMenuItem(new FileSaveAction()));
		menu.add(new JMenuItem(new FileRevertAction()));
		menu.add(new JMenuItem(new FileCloseAction()));
		mb.add(menu);

		menu = new JMenu("Search");
		menu.add(new JMenuItem(new ShowFindDialogAction()));
		menu.add(new JMenuItem(new ShowReplaceDialogAction()));
		menu.add(new JMenuItem(new GoToLineAction()));
		//menu.addSeparator();

		//int ctrl = getToolkit().getMenuShortcutKeyMask();
		//int shift = InputEvent.SHIFT_MASK;
		//KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl|shift);
		//Action a = csp.addBottomComponent(ks, findToolBar);
		//a.putValue(Action.NAME, "Show Find Search Bar");
		//menu.add(new JMenuItem(a));
		//ks = KeyStroke.getKeyStroke(KeyEvent.VK_H, ctrl|shift);
		//a = csp.addBottomComponent(ks, replaceToolBar);
		//a.putValue(Action.NAME, "Show Replace Search Bar");
		//menu.add(new JMenuItem(a));

		mb.add(menu);

		//menu = new JMenu("LookAndFeel");
		//ButtonGroup bg = new ButtonGroup();
		//LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
		//for (int i=0; i<infos.length; i++) {
		//	addItem(new LookAndFeelAction(infos[i]), bg, menu);
		//}
		//mb.add(menu);

		return mb;

	}


	public String getSelectedText() {
		return textArea.getSelectedText();
	}


	/**
	 * Creates our Find and Replace dialogs.
	 */
	public void initSearchDialogs() {

		findDialog = new FindDialog(this, this);
		replaceDialog = new ReplaceDialog(this, this);

		// This ties the properties of the two dialogs together (match case,
		// regex, etc.).
		SearchContext context = findDialog.getSearchContext();
		replaceDialog.setSearchContext(context);

		// Create tool bars and tie their search contexts together also.
		findToolBar = new FindToolBar(this);
		findToolBar.setSearchContext(context);
		//replaceToolBar = new ReplaceToolBar(this);
		//replaceToolBar.setSearchContext(context);

	}


	/**
	 * Listens for events from our search dialogs and actually does the dirty
	 * work.
	 */
	public void searchEvent(SearchEvent e) {

		SearchEvent.Type type = e.getType();
		SearchContext context = e.getSearchContext();
		SearchResult result = null;

		switch (type) {
			default: // Prevent FindBugs warning later
			case MARK_ALL:
				result = SearchEngine.markAll(textArea, context);
				break;
			case FIND:
				result = SearchEngine.find(textArea, context);
				if (!result.wasFound()) {
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				}
				break;
			case REPLACE:
				result = SearchEngine.replace(textArea, context);
				if (!result.wasFound()) {
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				}
				break;
			case REPLACE_ALL:
				result = SearchEngine.replaceAll(textArea, context);
				JOptionPane.showMessageDialog(null, result.getCount() +
						" occurrences replaced.");
				break;
		}

		String text = null;
		if (result.wasFound()) {
			text = "Text found; occurrences marked: " + result.getMarkedCount();
		}
		else if (type==SearchEvent.Type.MARK_ALL) {
			if (result.getMarkedCount()>0) {
				text = "Occurrences marked: " + result.getMarkedCount();
			}
			else {
				text = "";
			}
		}
		else {
			text = "Text not found";
		}
		statusBar.setLabel(text);

	}


	//public static void main(String[] args) {
	//	SwingUtilities.invokeLater(new Runnable() {
	//		public void run() {
	//			try {
	//				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//					UIManager.setLookAndFeel("org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel");
	//			} catch (Exception e) {
	//				e.printStackTrace();
	//			}
	//			new TextEditor(null).setVisible(true);
	//		}
	//	});
	//}


	private class GoToLineAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		public GoToLineAction() {
			super("Go To Line...");
			int c = getToolkit().getMenuShortcutKeyMask();
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, c));
		}

		public void actionPerformed(ActionEvent e) {
			if (findDialog.isVisible()) {
				findDialog.setVisible(false);
			}
			if (replaceDialog.isVisible()) {
				replaceDialog.setVisible(false);
			}
			GoToDialog dialog = new GoToDialog(TextEditor.this);
			dialog.setMaxLineNumberAllowed(textArea.getLineCount());
			dialog.setVisible(true);
			int line = dialog.getLineNumber();
			if (line>0) {
				try {
					textArea.setCaretPosition(textArea.getLineStartOffset(line-1));
				} catch (BadLocationException ble) { // Never happens
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					ble.printStackTrace();
				}
			}
		}

	}


    private class FileSaveAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		public FileSaveAction() {
    		super("Save");
			int c = getToolkit().getMenuShortcutKeyMask();
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, c));    		
    	}
		public void actionPerformed(ActionEvent e) {
			if (! finalData.equals(textArea.getText())) {
				finalData = textArea.getText();
				changed = true;
			}
		}
    }

    private class FileRevertAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		public FileRevertAction() {
    		super("Revert");
    	}
		public void actionPerformed(ActionEvent e) {
			textArea.setText(finalData);
		}
    }
    
    private class FileCloseAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		public FileCloseAction() {
    		super("Exit");
    	}
		public void actionPerformed(ActionEvent e) {
            TextEditor.this.dispatchEvent(new WindowEvent(
                    TextEditor.this, WindowEvent.WINDOW_CLOSING));

		}
    }

	private class ShowFindDialogAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public ShowFindDialogAction() {
			super("Find...");
			int c = getToolkit().getMenuShortcutKeyMask();
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, c));
		}

		public void actionPerformed(ActionEvent e) {
			if (replaceDialog.isVisible()) {
				replaceDialog.setVisible(false);
			}
			findDialog.setVisible(true);
		}

	}


	private class ShowReplaceDialogAction extends AbstractAction {
		private static final long serialVersionUID = 3787250518357123525L;

		public ShowReplaceDialogAction() {
			super("Replace...");
			int c = getToolkit().getMenuShortcutKeyMask();
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, c));
		}

		public void actionPerformed(ActionEvent e) {
			if (findDialog.isVisible()) {
				findDialog.setVisible(false);
			}
			replaceDialog.setVisible(true);
		}

	}


	private static class StatusBar extends JPanel {
		private static final long serialVersionUID = 1L;
		private JLabel label;

		public StatusBar() {
			label = new JLabel("Ready");
			setLayout(new BorderLayout());
			add(label, BorderLayout.LINE_START);
			add(new JLabel(new SizeGripIcon()), BorderLayout.LINE_END);
		}

		public void setLabel(String label) {
			this.label.setText(label);
		}

	}


}