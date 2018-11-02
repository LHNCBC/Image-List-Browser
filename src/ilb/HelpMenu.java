package ilb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Paths;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * The Help class constructs a menu for displaying information on the navigation
 * of this application from the user's perspective. There are two components a
 * navigation tree for displaying topics and a JScrollpane for displaying
 * information.
 * 
 * @author bonifantmc
 * 
 */
@SuppressWarnings("serial")
class HelpMenu extends JSplitPane implements TreeSelectionListener {

	/**
	 * Tree that displays the different links users can sift through while
	 * trying to read the help menu
	 */
	final private JTree navigation;

	/**
	 * When the user clicks an element of the navigation tree an associated
	 * string is printed here
	 */
	final private JEditorPane display = new JEditorPane();

	/** Node for explaining how to view a repository */
	final private DefaultMutableTreeNode howToRep = new DefaultMutableTreeNode("How to View a Repository");

	/** Node for explaining how to load a list file */
	final private DefaultMutableTreeNode howToLst = new DefaultMutableTreeNode("How to View a .lst File");

	/** Node for explaining how to use the glob searching feature */
	final private DefaultMutableTreeNode howToSear = new DefaultMutableTreeNode("How to search for a specific image");

	/** Node for explaining how to sort the images/list */
	final private DefaultMutableTreeNode howToSort = new DefaultMutableTreeNode("How to sort the .lst file");

	/** Node that explains how to save */
	final private DefaultMutableTreeNode howToSave = new DefaultMutableTreeNode(
			"How to save or create a new .lst file");

	/** Node that welcomes user upon opening the help browser */
	final private DefaultMutableTreeNode top = new DefaultMutableTreeNode("Image Browser");

	/** Node for explaining how to rename images */
	final private DefaultMutableTreeNode howToRename = new DefaultMutableTreeNode("How to rename an image file");

	/** Node for explaining how to use regular expression */
	final private DefaultMutableTreeNode howToRegex = new DefaultMutableTreeNode(
			"How to use regular expressions in image grouping");

	/** Node for explaining how to use glob patterns */
	final private DefaultMutableTreeNode howToGlob = new DefaultMutableTreeNode(
			"How to use glob patterns in image grouping");

	/**
	 * Builds the help menu
	 */
	HelpMenu() {

		buildTree();
		this.navigation = new JTree(this.top);
		this.display.setEditable(false);

		add(new JScrollPane(this.display, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), JSplitPane.RIGHT);
		add(new JScrollPane(this.navigation), JSplitPane.LEFT);
		this.navigation.addTreeSelectionListener(this);
		this.navigation.setSelectionInterval(0, 0);

	}

	/**
	 * Builds the navigation JTree.
	 */
	final private void buildTree() {
		this.top.add(this.howToRep);
		this.top.add(this.howToLst);
		this.top.add(this.howToSear);
		this.top.add(this.howToSort);
		this.top.add(this.howToSave);
		this.top.add(this.howToRename);
		this.top.add(this.howToRegex);
		this.top.add(this.howToGlob);
	}

	/**
	 * When an item is selected its passage is displayed.
	 */
	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.navigation.getLastSelectedPathComponent();
		String page = "HelpFiles/";
		if (node == null) {
			return;
		} else if (node == this.top) {
			page = null;
		} else if (node == this.howToRep) {
			page += "open_repository";
		} else if (node == this.howToLst) {
			page += "open_list";
		} else if (node == this.howToSave) {
			page += "save_list";
		} else if (node == this.howToSort) {
			page += "sorting";
		} else if (node == this.howToSear) {
			page += "searching";
		} else if (node == this.howToRename) {
			page += "renaming";
		} else if (node == this.howToRegex) {
			page += "regular_expressions";
		} else if (node == this.howToGlob) {
			page += "glob_expressions";
		}
		if (page == null) {
			String text = "Image List Browser is a photo browser and drawing tool for annotating images. This release is release: "
					+ Versioning.CURRENT_VERSION + " (Version " + Versioning.VERSION + "). ";
			try {
				text += ("This version is " + (Versioning.isFutureRelease() >= 0 ? "up to date."
						: (" outdated consider updating to " + Versioning.getLatestRelease())));
			} catch (AccessDeniedException e) {
				text += ("ILB access " + Versioning.ILBRepository.getAbsolutePath() + " to check for updates.");
			} catch (FileNotFoundException e) {
				text += ("ILB find " + Versioning.ILBRepository.getAbsolutePath() + " to check for updates.");
			}

			this.display.setText(text);
		} else {
			page += ".html";
			if (new File(page).exists())
				try {
					this.display.setPage(Paths.get(page).toUri().toURL());
					return;
				} catch (IOException e) {
					// do nothing/ fall through to error message
				}

			else
				this.display.setText("404 Error Could Not Find Requested Help Page.");
		}
	}
}
