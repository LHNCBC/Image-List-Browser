package ilb;

import java.awt.Color;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import annotations.Annotation;
import annotations.AttributeSet;
import image.editing.EditImage;
import struct.AnnotationGroups;
import struct.ArrayListModel;
import struct.Group;
import struct.ILBImageReader;
import struct.ImageAnnotationPair;
import struct.ImageGroups;
import struct.ImageMap;
import struct.MetaImage;
import struct.NLMSThumbnails;
import struct.Property;
import struct.Property.PropertyChangeEvent;
import struct.Property.PropertyChangeListener;

/**
 * ImageHandler provides a way to store meta data about many images in both list
 * and set format, both as ListModels.
 * <p>
 * It also sends property changed events to anything registered to it, as values
 * change, so various other compononets of the application know when to refresh
 * and update their display
 * 
 * @author bonifantmc
 * 
 */
public class ImageHandler implements PropertyChangeListener {
	/** Maximum size a thumbnail may be */
	public final static int MAX_THUMBNAIL_SIZE = 400;
	/** Minimum size a thumbnail may be */
	public final static int MIN_THUMBNAIL_SIZE = 80;

	/** The main list, containing all images that could be displayed */
	private final ArrayListModel<MetaImage> masterList = new ArrayListModel<>();

	/** All the images sorted into sets */
	private final AnnotationGroups annotationGroupings = new AnnotationGroups(this);
	/** All the images sorted into sets */
	private final ImageGroups imageGroupings = new ImageGroups(this);

	/** the panel the images are being displayed in, whether as a set or list */
	private final JScrollPane imageDisplay;
	/** The list of listeners listening for a property change */
	private final HashMap<Property, Vector<PropertyChangeListener>> attachedListeners = new HashMap<>();
	/** The Map that caches images for thumbnails display */
	private final ImageMap images;
	/**
	 * A bar to display below the image Display that shows the progress of
	 * background threads in progress.
	 */
	private final MessageBar messages = new MessageBar();

	/** The current manner in which images are displayed */
	private Mode mode;
	/** the root directory to search for images from */
	private File directory;
	/** the file listing images to look for from the root directory */
	private File listFile;
	/** additional optional lists to compare to the master list */
	private List<File> optionalListFiles = new ArrayList<>();
	/** A glob pattern to match images against and limit the display */
	private String pattern;
	/** determines how to filter images while sorting */
	private AttributeSet filter;
	/** the order in which images should display */
	private MetaImage.SortOrder ordering;
	/** Whether images are sorted ing ascending or descedning order */
	private boolean ascending;
	/** true if an image was changed since the appliation loaded */
	private boolean masterListChanged;
	/** if the application is loading */
	private boolean loading;
	/** if its the first time the application has loaded since start-up */
	private boolean firstLoad = true;
	/** the size thumbnails should be displayed at */
	private int thumbnailSize;
	/** the position of the display area's scrollbar 'tick' at start up */
	private int tick;

	/**
	 * the worker that loads image information from directories and list files
	 */

	/** master image annotation pair if sorting by image similarity */
	public static ImageAnnotationPair masterImage;

	/**
	 * 
	 * @param c
	 *            the display area for this handler.
	 */
	public ImageHandler(JScrollPane c) {
		this.images = new ImageMap(this);
		this.imageDisplay = c;
		c.addComponentListener(new ComponentListener() {

			@Override
			public void componentHidden(ComponentEvent arg0) {
				firePropertyChange(Property.displayArea, getImageDisplay());
			}

			@Override
			public void componentMoved(ComponentEvent arg0) {
				firePropertyChange(Property.displayArea, getImageDisplay());
			}

			@Override
			public void componentResized(ComponentEvent arg0) {
				firePropertyChange(Property.displayArea, getImageDisplay());
			}

			@Override
			public void componentShown(ComponentEvent arg0) {
				firePropertyChange(Property.displayArea, getImageDisplay());
			}
		});

		c.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(AdjustmentEvent arg0) {
				ImageHandler.this.firePropertyChange(Property.scrolled, null);

			}
		});
	}

	/**
	 * Find the given image and rotate it permanently in the system's memory
	 * 
	 * @param i
	 *            the image to rotate
	 */
	public void rotateAngle(MetaImage i) {
		BufferedImage newImage = ILBImageReader.rotate(getDirectory().getAbsolutePath(), i.getName());
		int temp = i.getWidth();
		i.setWidth(i.getHeight());
		i.setHeight(temp);
		for (Annotation a : i.getAnnotations()) {
			a.rotateDimensions90(i.getWidth());
		}
		getImages().put(i, newImage);
		newImage.flush();
		setMasterListChanged(true);
		i.setRotationsDegree(i.getRotationsDegree() + 90);
		getImageDisplay().repaint();
		firePropertyChange(Property.rotation, i);
	}

	/**
	 * Reindexes the master list based on the current visible list, images not
	 * in the current visible list are appended after those in the visible list,
	 * but otherwise retain their order. This helps with dragging and dropping
	 * images when a pattern mask is applied.
	 * 
	 * @param alt
	 *            if true reindex the alternative indexing as well as the
	 *            current
	 */
	public void reindex(boolean alt) {
		int ind = 0;
		// reindex images
		if (alt)
			for (MetaImage i : getMasterList()) {
				i.setIndexCur(ind);
				i.setIndexAlt(ind++);
			}
		else
			for (MetaImage i : getMasterList()) {
				i.setIndexCur(ind++);
				i.setIndexAlt(ind);
			}
	}

	/** reindex only the master list */
	public void reindexMaster() {
		int i = 0;
		for (MetaImage img : getMasterList()) {
			img.setIndexCur(i++);
		}
	}

	/**
	 * Launches a dialog for the user to enter a new name for the displayed
	 * image and executes this name change if possible. If an alternate file
	 * extension is offered and can be used by the Java readers the file will be
	 * converted and renamed.
	 * 
	 * @param i
	 *            the image to rename
	 */
	public void rename(MetaImage i) {
		String prompt = String.format("<html>Enter a new name. If you change the extension" + "<br><p>"
				+ " file conversion will be attempted.</html");
		String newName = "";
		String oldExt = i.getName().substring(i.getName().lastIndexOf("."));
		String newExt = null;

		// prompt until a valid name is given or close on cancel.
		while (newName.equals("")) {
			newName = JOptionPane.showInputDialog(getImageDisplay(), prompt, i.getName());
			// user canceled
			if (newName == null)
				return;

			// blank names aren't allowed
			if (newName.equals(""))
				JOptionPane.showMessageDialog(getImageDisplay(), "Name cannot be blank.");

			// used names aren't allowed
			else if (!newName.equals(i.getName()) && new File(getDirectory() + File.separator + newName).exists()) {
				JOptionPane.showMessageDialog(getImageDisplay(), "This name is taken already try a different one.");
				newName = "";

				// unsupported file types aren't allowed
			} else {
				boolean unsupportedType = true;
				try {
					newExt = newName.substring(newName.lastIndexOf(".") + 1);
					for (String ext : MetaImage.EXTENSIONS) {
						if (newExt.equalsIgnoreCase(ext)) {

							unsupportedType = false;
							break;
						}
					}
				} catch (IndexOutOfBoundsException e) {
					JOptionPane.showMessageDialog(getImageDisplay(),
							"You must supply an extension. (E.g. .jpg, .png, etc.)");
				}

				if (unsupportedType) {
					JOptionPane.showMessageDialog(getImageDisplay(), "Sorry this file type is not supported.");
					newName = "";
				}
			}
		}

		File oldFile = new File(getDirectory(), i.getName());
		File newFile = new File(getDirectory(), newName);
		// if extensions are the same rename the file else convert the file type
		// to that of the new image.

		if (oldExt.equals(newExt)) {
			if (!oldFile.renameTo(newFile)) {
				JOptionPane.showMessageDialog(getImageDisplay(), "Could not rename the image.");
				return;
			}
		} else {
			try {
				// prepare new image for copying
				BufferedImage bufferedImage = ImageIO.read(oldFile);
				BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(),
						BufferedImage.TYPE_INT_RGB);
				newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.WHITE, null);

				// attempt to copy in new format the old image
				if (!ImageIO.write(newBufferedImage, newExt, newFile)) {
					JOptionPane.showMessageDialog(getImageDisplay(), "Sorry we were unable to convert your image.");
				} else {
					if (!oldFile.delete()) {
						String message = String.format("<html>" + "To convert your image we copied the image "
								+ "<br><p>" + "into the new format, and we unable to " + "<br><p>"
								+ "remove the old format copy.</html>");
						JOptionPane.showMessageDialog(getImageDisplay(), message);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(getImageDisplay(), "Sorry we were unable to convert your image.");
				return;
			}
		}
		propagateNameChange(i.getName(), newName);
	}

	/**
	 * Propagates file name changes throughout the system after a name change
	 * has been accepted.
	 * 
	 * @param oldName
	 *            the original name of the file
	 * @param newName
	 *            the new name of the file
	 */
	private void propagateNameChange(String oldName, String newName) {
		for (MetaImage image : getMasterList()) {
			if (image.getName().equals(oldName))
				image.setName(newName);
		}

		for (EditImage eI : this.getEditImages())
			eI.setTitle(eI.getMetaImage().getName());

		// update display labels
		getImageDisplay().repaint();
		// save updated list if there is a list
		if (getListFile() != null) {
			FileMenu.save(this, true);
		}
	}

	/**
	 * Delete file from the file Common.list
	 * 
	 * @param i
	 *            the image to delete
	 */
	public void deleteFromList(MetaImage i) {
		// if deleting while in LIST/GRID mode

		getAnnotationGroups().removeMetaImage(i);
		getImageGroups().removeMetaImage(i);
		getMasterList().remove(i);
		setMasterListChanged(true);
		reindex(false);
	}

	/**
	 * Checks for alterations in the List file and asks user to save if there
	 * are any changes. Used when closing the file.
	 */
	public void checkedSaving() {
		if (isMasterListChanged()) {
			int j = JOptionPane.showConfirmDialog(getImageDisplay(), "Do you want to save your list file?",
					"Warning List File was changed,", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (j == JOptionPane.OK_OPTION) {
				// save file to list if list exists
				if (getListFile() == null)
					FileMenu.getNewSaveSpot(this);
				FileMenu.save(this, false);
			}
		}
	}

	/**
	 * Action performed when the directory or list file change, will result in
	 * master list being reloaded and visible list and master set being reset
	 */
	public void load() {
		getMasterList().clear();
		getImages().clear();
		getAnnotationGroups().clear();
		getImageGroups().clear();

		if (ListReader.r != null)
			ListReader.r.cancel(true);
		ListReader.r = new ListReader(this);
		ListReader.r.execute();
	}

	/**
	 * sort the images of this ImageHandler in the master list according to the
	 * current ordering, ascending, and pattern values
	 */
	public void sort() {
		Comparator<MetaImage> c = new MetaImage.MetaImageComparator(this.ordering, this.ascending, this.pattern,
				this.getFilter());
		Collections.sort(getMasterList(), c);
		for (NLMSThumbnails set : getAnnotationGroups().getLists()) {
			set.setMasterPair();
			set.changeSortOrder(set.rankThenName);
		}
		this.imageDisplay.invalidate();
		this.imageDisplay.repaint();
	}

	/**
	 * 
	 * @param width
	 *            the current width
	 * @param length
	 *            the current length
	 * @return scaler value required to reduce inputs to fit within required
	 *         Thumbnail Size.
	 */
	double scaleThumbSize(int width, int length) {
		return ILBImageReader.scale(width, length, getThumbnailSize(), getThumbnailSize());
	}

	//
	// PropertChangeListener Handling
	//
	/**
	 * 
	 * @param l
	 *            add this to notification list for properties change
	 * @param properties
	 *            the property to start listening to
	 */
	public void addPropertyChangeListener(PropertyChangeListener l, Property... properties) {
		for (Property pp : properties)
			getAttachedListeners(pp).add(l);
	}

	/**
	 * 
	 * @param l
	 *            remove this from the notification list
	 * @param props
	 *            the properties to stop listening to
	 */
	public void removePropertyChangeListener(PropertyChangeListener l, Property... props) {
		for (Property p : props)
			getAttachedListeners(p).remove(l);
	}

	/**
	 * send out a property change event to the notification list
	 * 
	 * @param name
	 *            the field that was changed
	 * @param newVal
	 *            the value after changing
	 */
	@SuppressWarnings("unchecked")
	public void firePropertyChange(Property name, Object newVal) {

		PropertyChangeEvent event = new PropertyChangeEvent(name, newVal);
		Vector<PropertyChangeListener> targets;
		synchronized (this) {
			targets = (Vector<PropertyChangeListener>) getAttachedListeners(name).clone();
		}
		Enumeration<PropertyChangeListener> e = targets.elements();
		while (e.hasMoreElements())
			e.nextElement().propertyChange(event);
	}

	//
	// STATIC IMAGE OPERATIONS
	//

	//
	// Getters And Setters
	//

	/** @return the file location for the list being looked at */
	public File getListFile() {

		return this.listFile;
	}

	/**
	 * @param l
	 *            the file now being used to read a list of images from
	 */
	public void setListFile(File l) {
		this.listFile = l;
		firePropertyChange(Property.listFile, this.listFile);
	}

	/** @return the directory from which images are found */
	public File getDirectory() {
		return this.directory;
	}

	/**
	 * @param dir
	 *            the new directory from which images are found
	 */
	public void setDirectory(File dir) {
		this.directory = dir;
		firePropertyChange(Property.directory, this.directory);
	}

	/** @return the pattern used for filtering images */
	String getPattern() {
		return this.pattern;
	}

	/**
	 * @param p
	 *            the new pattern for filtering images
	 */
	void setPattern(String p) {
		this.pattern = p;
		sort();
		firePropertyChange(Property.pattern, this.pattern);

	}

	/**
	 * @return the parent directory of the one images are found in, or null if
	 *         no parent
	 */
	File getDirectoryParent() {
		return this.directory == null ? null : this.directory.getParentFile();
	}

	/**
	 * @return the directory in which the list file is found, or null if no
	 *         parent
	 */
	File getListFileDirecotry() {
		return this.listFile == null ? null : this.listFile.getParentFile();
	}

	/** @return the master list of this handler */
	public ArrayListModel<MetaImage> getMasterList() {
		return this.masterList;
	}

	/**
	 * clears the the master list and adds the input to it afterwards
	 * 
	 * @param list
	 *            the list of new images for the masterlist
	 */
	void setMasterList(List<MetaImage> list) {
		System.out.println("\tSetting Master List");

		long t = System.currentTimeMillis();
		this.masterList.clear();
		System.out.println("\t\tClear Time: " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();

		this.masterList.addAll(list);
		System.out.println("\t\tAdding Time: " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();
		this.annotationGroupings.build();
		System.out.println("\t\tAnnotation Grouping Time: " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();
		this.imageGroupings.build();
		System.out.println("\t\tImage Grouping Time: " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();
		sort();
		System.out.println("\t\tSorting Time: " + (System.currentTimeMillis() - t));
		System.out.println("\tDone Setting Master List");
		this.firePropertyChange(Property.mode, this.mode);
	}

	/**
	 * Set the master list without rebuilding the Image and Annotation Groups
	 * (to use when loading and making quick changes to the list, when done the
	 * real setMasterList should be used to rebuild the Groups)
	 * 
	 * @param list
	 *            the list to set
	 */
	public void setMasterListPartial(ArrayListModel<MetaImage> list) {
		this.masterList.clear();
		this.masterList.addAll(list);
		sort();
	}

	/** @return the map of how all annotations are grouped together */
	public AnnotationGroups getAnnotationGroups() {
		return this.annotationGroupings;
	}

	/** @return the map of how all images are grouped together */
	public ImageGroups getImageGroups() {
		return this.imageGroupings;
	}

	/** @return true if images are sorted in ascending order */
	boolean isAscending() {
		return this.ascending;
	}

	/**
	 * @param ascent
	 *            true if images are to be sorted in ascending order
	 */
	public void setAscending(boolean ascent) {
		this.ascending = ascent;
		firePropertyChange(Property.ascending, new Boolean(this.ascending));
	}

	/** @return the SortOrder of this Image handler */
	MetaImage.SortOrder getOrdering() {
		return this.ordering;
	}

	/**
	 * @param order
	 *            the new ordering of this ImageHandler
	 */
	public void setOrdering(MetaImage.SortOrder order) {
		this.ordering = order;
		firePropertyChange(Property.ordering, this.ordering);

	}

	/**
	 * @param load
	 *            true if the application is loading images via the worker
	 */
	public void setLoading(boolean load) {
		this.loading = load;
		firePropertyChange(Property.loading, this.loading);
	}

	/** @return true if this is loading images via the worker */
	public boolean isLoading() {
		return this.loading;
	}

	/**
	 * set's first load false after first call, used to set the scrollbar's tick
	 * location for the image display area
	 */
	public void setFirstLoad() {
		if (this.firstLoad && this.masterList.size() != 0) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					getImageDisplay().getVerticalScrollBar().setValue(ImageHandler.this.tick);
					ImageHandler.this.firstLoad = false;
				}
			});

		}
	}

	/**
	 * set the starting value of the image display area's scrollbar tick.
	 * 
	 * @param tick
	 *            the location of the start tick value
	 */
	public void setTick(int tick) {
		// don't override defualt because program wants to load more than it
		// should
		this.tick = tick;
		getImageDisplay().getVerticalScrollBar().setValue(tick);
	}

	/** @return the map caching all image's thumbnail information */
	ImageMap getImages() {
		return this.images;
	}

	/**
	 * @param newSize
	 *            the new thumbnail size
	 */
	void setThumbnailSize(int newSize) {
		int size = newSize;
		if (size < MIN_THUMBNAIL_SIZE)
			size = MIN_THUMBNAIL_SIZE;
		if (size > MAX_THUMBNAIL_SIZE)
			size = MAX_THUMBNAIL_SIZE;
		this.thumbnailSize = size;
		firePropertyChange(Property.thumbnailSize, this.thumbnailSize);
	}

	/**
	 * @return the current size for thumbnails
	 */
	public int getThumbnailSize() {
		return this.thumbnailSize;
	}

	/**
	 * @return the current mode the application is in, GROUPING, GRID, or LIST
	 */
	public Mode getMode() {
		return this.mode;
	}

	/**
	 * @param m
	 *            the new mode to set the application in
	 */
	public void setMode(Mode m) {
		this.mode = m;
		firePropertyChange(Property.mode, this.mode);
	}

	/** @return the area where images are displayed */
	public JScrollPane getImageDisplay() {
		return this.imageDisplay;
	}

	/**
	 * @return true if the master list has changed in such a way that it would
	 *         need to be saved
	 */
	private boolean isMasterListChanged() {
		return this.masterListChanged;
	}

	/**
	 * @param masterListChanged
	 *            , true if the list will need to be saved, false if it was just
	 *            saved
	 */
	public void setMasterListChanged(boolean masterListChanged) {
		this.masterListChanged = masterListChanged;
		this.firePropertyChange(Property.displayArea, this.imageDisplay);
	}

	/**
	 * @param p
	 *            the property whose listeners are to be retrieved
	 * @return a list of all objects listening to this handler
	 */
	private Vector<PropertyChangeListener> getAttachedListeners(Property p) {
		if (this.attachedListeners.get(p) == null)
			this.attachedListeners.put(p, new Vector<PropertyChangeListener>());
		return this.attachedListeners.get(p);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		firePropertyChange(evt.getProperty(), evt.getNewValue());

	}

	/**
	 * 
	 * @return the maximum number of images to load at a time ( 3 buffer rows
	 *         are added for when rows are partially off screen and in the event
	 *         user scrolls back the way they came)
	 */
	public int getMaxVisibleImages() {
		int w = getThumbnailSize();
		int h = w + ThumbnailList.CAPTION_BUFFER;

		int row = (getImageDisplay().getHeight() / h) + 3;
		int col = getImageDisplay().getWidth() / w;
		return col * row;
	}

	/** @return the tick mark where the display is scrolled to */
	public int gettick() {
		return this.tick;
	}

	// /////////////////////////////////////////
	// Fuctions to Interact with CategoryTable//
	// /////////////////////////////////////////

	/**
	 * Present a dialogue to select between different Annotations in a MetaImage
	 * to determine which Annotation will have its tag/category altered
	 * 
	 * @param i
	 *            the MetaImage source which must have at least one Annotation
	 * @return the Annotation attached to this image selected by the user
	 */
	/*
	 * public Annotation chooseImageAnnotationToChange(MetaImage i) { int size =
	 * i.getAnnotations().size();
	 * 
	 * // must contain annotations if (size == 0) throw new
	 * IllegalArgumentException( "image must have at least one annotation"); //
	 * no choice, obvious answer if (size == 1) return
	 * i.getAnnotations().get(0);
	 * 
	 * // display choices and let user select one
	 * 
	 * // list of images ArrayList<ImageIcon> imgs = new ArrayList<>(); for
	 * (Annotation a : i.getAnnotations()) imgs.add(new
	 * ImageIcon(ImageHandler.read( getDirectory().getAbsolutePath(),
	 * i.getName()).getSubimage( (int) a.x, (int) a.y, (int) a.width, (int)
	 * a.height)));
	 * 
	 * // prompt user for index of Annotation to select int j = JOptionPane
	 * .showOptionDialog( getImageDisplay(),
	 * "There are multiple annotations, select which annotation to tag: ",
	 * "Multiple Annotations", JOptionPane.OK_CANCEL_OPTION,
	 * JOptionPane.QUESTION_MESSAGE, null, imgs.toArray(), null);
	 * 
	 * // user canceled if (j < 0 || j > i.getAnnotations().size() - 1) return
	 * null;
	 * 
	 * // the selected Annotation return i.getAnnotations().get(j);
	 * 
	 * }
	 */

	/**
	 * Prompt user for the name/key of a Category in the CategoryTable, with the
	 * assumption that they are moving a MetaImage with Annotation n into this
	 * category and categorizing n into the result's category.
	 * 
	 * @param n
	 *            the Annotation being moved
	 * @return the string/key for a category in the CategoryTable given by the
	 *         user
	 */
	public String chooseACategory(Annotation n) {
		// prompt user
		String newName = JOptionPane.showInputDialog(getImageDisplay(),
				"Enter a new tag for this Annotaiton ('Untagged' and blank"
						+ "space if\n you wish to untag this Annotation)",
				n != null ? n.getCategory().toString() : "Enter Tag Here...");
		// user canceled
		if (newName == null)
			return newName;
		else
		//
		if (newName.equals(AnnotationGroups.UNANNOTATED)) {
			JOptionPane.showConfirmDialog(getImageDisplay(), "This image has an annotation, it can't be Unannotated",
					"Error Annotated Images aren't Unannotated.", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.ERROR_MESSAGE);
			return null;
		}
		if (newName.trim().equals(""))
			newName = AnnotationGroups.UNTAGGED;
		return newName;

	}

	/**
	 * Find the annotation being edited then called changeImageTag(MetaImage,
	 * Found Annotation). This will result in selecting one of the Image's
	 * annotations and changing its Annotation tag/Category.
	 * 
	 * @param i
	 *            the MetaImage whose's tag is to be changed
	 * @param g
	 *            the group the tag is being changed for
	 */
	/*
	 * public void changeImageTag(MetaImage i, Group g) { if
	 * (i.getAnnotations().size() == 0) {
	 * JOptionPane.showConfirmDialog(getImageDisplay(),
	 * "This image has no Annotation and therefore cannot" +
	 * " be tagged.\n Please annotate it before" + " trying to tag again.",
	 * "Error can't tag unannotated images", JOptionPane.OK_CANCEL_OPTION,
	 * JOptionPane.ERROR_MESSAGE); return; } Annotation a =
	 * chooseImageAnnotationToChange(i);
	 * 
	 * if (a != null) changeImageTag(i, a, g); }
	 */

	/**
	 * Find the new list to put MetaImage i in, changing the tag of the given
	 * Annotation.
	 * 
	 * @param i
	 *            the MetaImage whose's tag is to be changed
	 * @param n
	 *            the Annotation that is to be altered
	 * @param g
	 *            the group the tag is being changed for
	 */
	void changeImageTag(MetaImage i, Annotation n, Group g) {
		String newName = chooseACategory(n);
		if (newName == null)
			return;
		changeImageTag(i, n, newName, g);
	}

	/**
	 * Move the given image from the old category to the new category, the
	 * Image's annotation marking it for the new category must be set already
	 * 
	 * @param i
	 *            the image to move
	 * @param n
	 *            Annotation being changed
	 * @param newName
	 *            name of the new category
	 * @param g
	 *            the group the tag is being changed for
	 */
	public void changeImageTag(MetaImage i, Annotation n, String newName, Group g) {
		String oldName = n.getCategory().toString();
		n.setCategory(newName);
		getAnnotationGroups().get(oldName).remove(i);
		List<String> cats = Annotation.getAllCategories(i.getAnnotations());
		for (String s : cats)
			getAnnotationGroups().get(s).add(new ImageAnnotationPair(i, n));
		sort();
		firePropertyChange(g.getProperty(), null);
	}

	/**
	 * Rename a list and replace its old name for its new name in all
	 * metaImage's annotations.
	 * 
	 * @param model
	 *            the list that is to be renamed
	 * @param g
	 *            the group the tag is being changed for
	 */
	public void changeListTag(NLMSThumbnails model, Group g) {
		String newName = JOptionPane.showInputDialog(getImageDisplay(), "Enter a new tag for this list");
		String oldName = model.getName();
		if (newName == null)
			return;// user canceled

		getAnnotationGroups().changeKey(oldName, newName);

		sort();
		firePropertyChange(g.getProperty(), null);
	}

	/** @return a list of all the index files for FaceMatch that should exist */
	public String[] getNdxFiles() {
		String[] ret = { "HAAR", "SIFT", "SURF", "ORB", "LBPH", "RSILC" };
		for (int i = 0; i < ret.length; i++)
			ret[i] = "Files/" + (getListFile() == null ? "TEMP" : getListFile().getName()) + "." + ret[i] + ".ndx.out";
		return ret;
	}

	/**
	 * @return The Message/Progress bar to be displayed on the bottom of the
	 *         screen
	 */
	public MessageBar getProgressBar() {
		return this.messages;
	}

	/**
	 * @param b
	 *            true if image grouping changed
	 */
	public void setImageGroupsChanged(boolean b) {
		// TODO Auto-generated method stub

	}

	/**
	 * @param b
	 *            true if annotation grouping changed
	 */
	public void setAnnotationGroupsChanged(boolean b) {
		// TODO Auto-generated method stub

	}

	/** a list of the open edit images */
	private ArrayList<image.editing.EditImage> editImages = new ArrayList<image.editing.EditImage>();
	/** boolean tracking if 1 or many EditImages may be open at once */
	public boolean oneEditImage;

	/**
	 * Open a given MetaImage for display.
	 * 
	 * @param image
	 *            the image to display
	 */
	public void openImage(MetaImage image) {

		if (this.oneEditImage) {
			if (this.getEditImages().size() > 1) {
				closeAllEditImages();
				this.getEditImages().add(new EditImage(image, this));
				return;
			}
			this.getEditImages().get(0).changeImage(image);
			return;

		}

		for (EditImage ei : this.getEditImages())
			if (ei.getMetaImage().equals(image)) {
				ei.setVisible(true);
				return;
			}
		this.getEditImages().add(new image.editing.EditImage(image, this));
	}

	/**
	 * Closes all EditImages
	 */
	public void closeAllEditImages() {
		for (EditImage ei : getEditImages()) {
			ei.setVisible(false);
			ei.dispose();
		}

	}

	/** @return get all EditImages open */
	public ArrayList<image.editing.EditImage> getEditImages() {
		return this.editImages;
	}

	/**
	 * 
	 * @return the current filter applied to images in sorting
	 */
	public AttributeSet getFilter() {
		return filter;
	}

	/**
	 * 
	 * @param filter
	 *            the new Attribute set to filter images by while sorting
	 */
	public void setFilter(AttributeSet filter) {
		this.filter = filter;
		sort();
	}

	/**
	 * @return the list of additional optional List Files of annotations to
	 *         compare the list being edited with
	 */
	public List<File> getOptionalListFiles() {
		if (optionalListFiles == null)
			optionalListFiles = new ArrayList<>();
		return optionalListFiles;
	}

	/**
	 * @param f
	 *            the file to remove
	 */
	public void removeFileFromOptionalListfiles(File f) {
		int idx = this.getOptionalListFiles().indexOf(f);
		File removed = this.getOptionalListFiles().remove(idx);
		if (f == removed) {
			for (MetaImage i : this.getMasterList()) {
				i.getAlternativeAnnotations().remove(idx);

			}
		}
		this.setMode(Mode.LIST);

	}
}