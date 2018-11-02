package ilb;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import struct.MetaImage;
import struct.Property;
import struct.Property.PropertyChangeEvent;
import struct.Property.PropertyChangeListener;

/**
 * Opens the application and defines the main window.
 * 
 * @author bonifantmc
 * 
 */
@SuppressWarnings("serial")
public class ILB extends JFrame implements PropertyChangeListener {
	/**
	 * A panel containing main content, either a single ThumbnailList(
	 * this.thumbnailList) or a GroupList(this.grouping)
	 */
	private final JScrollPane content;
	/** The JMenuBar for this application */
	private final Menus menus;
	/** Displays this application's ImageHandler's visible list */
	private final ThumbnailList<MetaImage> thumbnailList;
	/** Displays this application's ImageHandler's Annotation Groups */
	private final GroupList groupingAnnotations;
	/** Displays this application's ImageHandler Image Groups */
	private final Component groupingImages;
	/**
	 * This applications ImageHandler, provides a way of tracking all images and
	 * the current list, directory, and pattern
	 */
	private final ImageHandler handler;
	/**
	 * Name of resource file containing basic information about the application
	 * for start up
	 */
	private final String config = "config.properties";

	/**
	 * A timer, every 5 minutes autosave the lst file to the file ILB.TMP
	 * directory
	 */
	private final Timer autosavingToTemp;

	/** 5 minutes (aka 300000 ms). */
	private static long AUTOSAVE_INTERVAL = 300000;
	/** How to format the time stamp used in auto saves */
	protected static final DateFormat TIME_FORMATTER = new SimpleDateFormat("YYYY-MM-dd-HH.mm");
	/** The temp directory for autosaves */
	public static final String TMP = "TMP";

	/**
	 * Prepare the main display
	 */
	public ILB() {
		super();
		this.content = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		JPanel realcontent = new JPanel();
		realcontent.setLayout(new BorderLayout());
		realcontent.add(this.content, BorderLayout.CENTER);
		setContentPane(realcontent);
		getContent().getVerticalScrollBar().setUnitIncrement(40);
		this.handler = new ImageHandler(getContent());
		this.add(getHandler().getProgressBar(), BorderLayout.SOUTH);

		getHandler().addPropertyChangeListener(this, Property.loading, Property.directory, Property.listFile,
				Property.pattern, Property.mode, Property.displayArea);
		this.groupingAnnotations = new GroupList(getHandler(), getHandler().getAnnotationGroups());
		this.groupingImages = new GroupList(getHandler(), getHandler().getImageGroups());
		this.thumbnailList = new ThumbnailList<>(getHandler().getMasterList(),
				new ThumbnailTransferHandler(getHandler()), getHandler());
		this.menus = new Menus(getHandler());

		// load components of window
		setJMenuBar(getMenus());
		// set closing behavior
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				getHandler().checkedSaving();
				Configurations.setProperties(ILB.this, getHandler(), getConfig());
			}
		});
		Configurations.getDisplayProperties(this, getConfig());
		Configurations.getHandlerProperties(getHandler(), getConfig());

		// build the autosave routine
		this.autosavingToTemp = new Timer();
		this.autosavingToTemp.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				synchronized (ILB.this.getHandler()) {
					File dir = new File(ILB.TMP);
					if (!dir.exists() || !dir.isDirectory())
						dir.mkdir();
					String fileName = ILB.TMP + File.separator;
					if (ILB.this.getHandler().getListFile() != null)
						fileName += ILB.this.getHandler().getListFile().getName() + "-";
					Date d = new Date(System.currentTimeMillis());
					fileName += "-" + TIME_FORMATTER.format(d) + ".lst";
					try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)))) {
						for (MetaImage i : getHandler().getMasterList()) {
							bw.write(i.toString());
							bw.write("\n");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}, AUTOSAVE_INTERVAL, AUTOSAVE_INTERVAL);
	}

	/**
	 * Sets the application title according to the list, repository and glob
	 * pattern values in the format:
	 * <p>
	 * <b>list</b>@<b>repository</b>/(\)<b>glob</b>
	 * <p>
	 * Where list is the name of the .lst file or an empty string. Repository is
	 * the directory of the repository (and if available its 2 immediate parent
	 * directories) and glob is the applied pattern mask or the empty string. If
	 * none are set the title is Welcome to FaceMatch Image Browser Tool. Click
	 * \"File\" to select something to view..
	 */
	private void setTitle() {
		StringBuilder b = new StringBuilder();
		if (getHandler().getDirectory() != null) {
			File f = getHandler().getDirectory();

			// only display the current directory and two immediate parents to
			// prevent a huge title in event of the directory being buried deep
			// in a file structure
			String dir = "";
			int i = 3;
			while (f != null && f.exists() && i > 0) {
				dir = f.getName() + File.separator + dir;
				f = f.getParentFile();
				i--;
			}

			// display the list file
			String lst = "";
			if (getHandler().getListFile() != null)
				lst = getHandler().getListFile().getName();

			// display the pattern/glob/mask
			String mask = "";
			if (getHandler().getPattern() != null && !getHandler().getPattern().equals("*"))
				mask = getHandler().getPattern();
			b.append(lst).append("@").append(dir).append(mask);
		} else if (getHandler().getListFile() != null) {
			b.append("@" + getHandler().getListFile());
		} else {
			b.append("Welcome to FaceMatch Image Browser Tool. Click \"File\" to select something to view.");
		}
		setTitle(b.toString());
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		switch (arg0.getProperty()) {
		case loading:
			if ((boolean) arg0.getNewValue()) {
				getThumbnailList().setDragEnabled(false);
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			} else {
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				getThumbnailList().setDragEnabled(true);
			}
			break;
		case directory:
		case listFile:
		case pattern:
			setTitle();
			break;
		case mode:
			switch ((Mode) arg0.getNewValue()) {
			case GROUPING_ANNOTATIONS:
				if (getContent().getViewport().getComponentCount() == 0
						|| getContent().getViewport().getComponent(0) != getAnnotationGrouping()) {
					getContent().getViewport().removeAll();
					getContent().getViewport().add(getAnnotationGrouping());
				}
				break;
			case GROUPING_IMAGES:
				if (getContent().getViewport().getComponentCount() == 0
						|| getContent().getViewport().getComponent(0) != getImageGrouping()) {
					getContent().getViewport().removeAll();
					getContent().getViewport().add(getImageGrouping());
				}
				break;
			case LIST:
			case GRID:
			default:
				if (getContent().getViewport().getComponentCount() == 0
						|| getContent().getViewport().getComponent(0) != getThumbnailList()) {
					getContent().getViewport().removeAll();
					getContent().getViewport().add(getThumbnailList());
				}
			}
			break;
		case ascending:
		case displayArea:
		case imageList:
		case ordering:
		case rotation:
		case thumbnailSize:
		case tally:
		default:
			break;
		}
	}

	/**
	 * 
	 * @return a Component for displaying images sorted into groups
	 */
	private Component getImageGrouping() {
		return this.groupingImages;
	}

	/** @return the main content panel for this ILB */
	public JScrollPane getContent() {
		return this.content;
	}

	/** @return The ImageHandler for this ILB. */
	public ImageHandler getHandler() {
		return this.handler;
	}

	/**
	 * @return The ThumbnailList that displays this ILB's Handler's visible list
	 */
	private ThumbnailList<MetaImage> getThumbnailList() {
		return this.thumbnailList;
	}

	/**
	 * @return A Component for displaying Image/Annotation Pairs sorted into
	 *         groups.
	 */
	private GroupList getAnnotationGrouping() {
		return this.groupingAnnotations;
	}

	/** @return The menu bar of this ILB */
	Menus getMenus() {
		return this.menus;
	}

	/** @return the name of the config file used by this ILB */
	String getConfig() {
		return this.config;
	}

}