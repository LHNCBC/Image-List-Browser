package ilb;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.border.EtchedBorder;

import struct.MetaImage;
import struct.Property;
import struct.Property.PropertyChangeEvent;
import struct.Property.PropertyChangeListener;

/**
 * Holds the file, view and window menus. Also holds the ComboBox and Toggle for
 * sorting Common.ls. Defines the HelpMenu's frame and launching action.
 * 
 * @author bonifantmc
 * 
 */
@SuppressWarnings("serial")
public class Menus extends JMenuBar implements PropertyChangeListener {
	/** opens, closes, saves, and pattern matches lists and directories */
	private final FileMenu fileMenu;

	/**
	 * Controls how users view thumbnails allowing them to change thumbnail
	 * size, and if they're displayed in a grid, list or in grouping mode.
	 */
	private final ViewMenu viewMenu;

	/**
	 * The WindowMenu lets users configure how ILB opens new windows (replacing
	 * the old, or opening new ones).
	 */
	private final WindowMenu windowMenu;

	/**
	 * A paired Combo box and toggle button that allows ILB's master list to be
	 * sorted, the combo box lets users select the sorting method, and the
	 * toggle switches between ascending and descending order
	 */
	private final SortingComboBoxAndToggle comboAndToggle;

	/** menu for grouping annotations */
	private final GroupingMenu groupAnnotations;
	/** menu for grouping images */
	private final GroupingMenu groupImages;

	/**
	 * The ToolsMenu lets users set parameters for FaceFinder
	 */
	private final ToolsMenu tools;

	/**
	 * The Frame that opens when the user clicks on help.
	 */
	private final JFrame helpFrame;

	/** menu that provides stats about the List */
	private final StatusMenu stats;

	/** the image handler serving this application */
	private final ImageHandler handler;

	/**
	 * Construct the menu bar in the order file, view, window, sort-combo-box,
	 * sort-toggle-button, help
	 * 
	 * @param h
	 *            the ImageHandler that supplies property change events to
	 *            update the GUI
	 */
	Menus(ImageHandler h) {
		this.handler = h;
		h.addPropertyChangeListener(this, Property.loading, Property.tally, Property.mode, Property.scrolled);
		this.viewMenu = new ViewMenu(h);
		this.fileMenu = new FileMenu(h);
		this.windowMenu = new WindowMenu(h);
		this.comboAndToggle = new SortingComboBoxAndToggle(h);
		this.helpFrame = new JFrame("Help");
		this.groupAnnotations = new GroupingMenu(h, h.getAnnotationGroups());
		this.groupImages = new GroupingMenu(h, h.getImageGroups());
		this.tools = new ToolsMenu(h);
		this.stats = new StatusMenu(h);
		getHelpFrame().setSize(600, 400);
		getHelpFrame().add(new HelpMenu());

		setBorder(new EtchedBorder(EtchedBorder.RAISED, Color.WHITE, null));
		setLayout(new FlowLayout(FlowLayout.LEFT));

		// main menu
		add(getFileMenu());
		add(getViewMenu());
		add(getWindowMenu());
		add(getComboAndToggle().getComboBox());
		add(getComboAndToggle().getToggle());
		add(this.groupImages);
		add(this.groupAnnotations);
		add(getTools());
		add(Box.createHorizontalGlue());

		JMenuItem helpItem = new JMenuItem("Help");
		helpItem.setAction(new HelpMenuItem());

		add(helpItem);
		add(getStats());
	}

	/**
	 * Used for building and displaying the JFrame that displays the help menu.
	 * 
	 * @author bonifantmc
	 * 
	 */
	class HelpMenuItem extends AbstractAction {
		/**
		 * set text and tooltip of the menu item
		 */
		HelpMenuItem() {
			putValue(NAME, "Help");
			putValue(SHORT_DESCRIPTION, "Open and display the help menu.");
		}

		/**
		 * Display the help menu
		 */
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (!isVisible())
				getHelpFrame().setLocationByPlatform(true);
			getHelpFrame().setVisible(true);
		}
	}

	/**
	 * @return the file menu
	 */
	private FileMenu getFileMenu() {
		return this.fileMenu;
	}

	/**
	 * @return the helpFrame
	 */
	JFrame getHelpFrame() {
		return this.helpFrame;
	}

	/**
	 * 
	 * @return the view menu that controls how images should be displayed in the
	 *         ThumbPanel from the user's perspective
	 */
	ViewMenu getViewMenu() {
		return this.viewMenu;
	}

	/**
	 * 
	 * @return the window menu that controls how windows should be opened and
	 *         closed in the application from the user's perspective
	 */
	private WindowMenu getWindowMenu() {
		return this.windowMenu;
	}

	/**
	 * 
	 * @return the combobox and toggle button that control image sorting in the
	 *         application from the user's perspective
	 */
	private SortingComboBoxAndToggle getComboAndToggle() {
		return this.comboAndToggle;
	}

	// /**
	// * @return the GroupingMenu that gives users access to calling grouping
	// * methods, either via filename parsing of FaceMatch API calls
	// */
	// public GroupingMenu getGroup() {
	// return this.group;
	// }

	/**
	 * @return the ToolsMenu that gives users access to setting FaceMatch class
	 *         parameters
	 */
	public ToolsMenu getTools() {
		return this.tools;
	}

	/** @return the StatusMenu */
	public StatusMenu getStats() {
		return this.stats;
	}

	/** @return the Image Handler reference this Menu holds */
	public ImageHandler getHandler() {
		return this.handler;
	}

	/**
	 * Disable and enable menus as program loads/is active
	 * <p>
	 * rewrite the current tally
	 */
	@SuppressWarnings("boxing")
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		switch (arg0.getProperty()) {
		case loading:
			setEnabled(!((boolean) arg0.getNewValue()));
			for (Component c : this.getComponents())
				if (c != this.fileMenu)
					c.setEnabled(!((boolean) arg0.getNewValue()));
				else
					((FileMenu) c).propertyChange(arg0);
			//$FALL-THROUGH$
		case tally:
		case mode:
		case scrolled:
			int max = getHandler().getMasterList().getSize();
			int cur = 0;
			for (MetaImage i : getHandler().getMasterList())
				if (i.getDate() != 0)
					cur++;
			getStats().updateTally(cur, max);
			break;
		default:
			break;
		}
	}

}
