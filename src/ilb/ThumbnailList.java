package ilb;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.DropMode;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionListener;

import ilb.ListPopUpMenu.ThumbnailListMenuItem;
import struct.AnnotationGroups;
import struct.ArrayListModel;
import struct.MetaImage;
import struct.NLMSThumbnails;
import struct.Property;
import struct.Property.PropertyChangeEvent;
import struct.Property.PropertyChangeListener;
import struct.Thumbnail;

/**
 * A specialized JList for working with the ThumbnailRenderer. Displays only
 * MetaImages
 * 
 * @author bonifantmc
 * @param <T>
 *            An object that implement the Thumbnail interface.
 * 
 */
@SuppressWarnings("serial")
public class ThumbnailList<T extends Thumbnail> extends JList<T> implements PropertyChangeListener {

	/** the source of which images to display */
	private final ImageHandler handler;
	/** the list of images to display */
	private final ListModel<T> model;
	/** size of buffer given for image caption */
	public static int CAPTION_BUFFER = 60;
	/** A list of menu items to use for this list */
	private ArrayList<ThumbnailListMenuItem> menuItems = new ArrayList<>();

	/**
	 * 
	 * @param i
	 *            the list model this list will work from
	 * @param t
	 *            how image drag and drop should be handled for the given list
	 * @param h
	 *            image source
	 */
	ThumbnailList(ListModel<T> i, TransferHandler t, ImageHandler h) {
		this.handler = h;
		this.model = i;
		getHandler().addPropertyChangeListener(this, Property.mode, Property.thumbnailSize, Property.displayArea,
				Property.loading, Property.imageList);

		setCellRenderer(new ThumbnailRenderer(getHandler()));
		setLayoutOrientation(JList.HORIZONTAL_WRAP);
		setVisibleRowCount(-1);
		getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		if (!(i instanceof NLMSThumbnails
				&& ((NLMSThumbnails) i).getName().equals(AnnotationGroups.UNANNOTATED))) {
			setDropMode(DropMode.INSERT);
			setDragEnabled(true);
			setModel(i);
			setTransferHandler(t);
		}

		// add basic menu items
		addPopUpMenuItem(new ListPopUpMenu.Open(null, getHandler()));
		addPopUpMenuItem(new ListPopUpMenu.Rename(null, getHandler()));

		addPopUpMenuItem(new ListPopUpMenu.DeleteFromList(null, getHandler()));
		addPopUpMenuItem(new ListPopUpMenu.Rotate(null, getHandler()));

		// open image on left click, display pop-up on right click
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

				int ind = locationToIndex(e.getPoint());
				if (ind != -1) {
					MetaImage img = getModel().getElementAt(ind).getImage();
					if (e.getClickCount() > 1 && SwingUtilities.isLeftMouseButton(e) && img.isFound())
						getHandler().openImage(getSelectedValue().getImage());
					if (getHandler().isLoading())
						return;

					if (SwingUtilities.isRightMouseButton(e)) {
						Point p = new Point(e.getPoint());
						p = SwingUtilities.convertPoint(ThumbnailList.this, e.getPoint(), getParent());
						{

							for (ThumbnailListMenuItem item : ThumbnailList.this.menuItems) {
								item.i = img;
								item.checkAndEnable();
							}
							new ListPopUpMenu(ThumbnailList.this.menuItems).show(getParent(), p.x, p.y);

						}
					}
				}
			}
		});

	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		switch (arg0.getProperty()) {
		case mode:
			switch ((Mode) arg0.getNewValue()) {
			case LIST:
				ThumbnailList.CAPTION_BUFFER=75;
				setLayoutOrientation(JList.VERTICAL);
				setFixedCellWidth(getWidth());
				this.setDragEnabled(false);
				break;
			case GROUPING_IMAGES:
			case GROUPING_ANNOTATIONS:
			case GRID:
			default:
				ThumbnailList.CAPTION_BUFFER=60;
				this.setDragEnabled(true);
				setLayoutOrientation(JList.HORIZONTAL_WRAP);
				break;
			}
			//$FALL-THROUGH$
		case imageList:
		case annotationGroups:
		case imageGroups:
		case thumbnailSize:
		case displayArea:
		case loading:
			this.setDisplay();
			break;
		default:
			break;
		}
	}

	/** set the display size for lists */
	private void setDisplay() {
		int i = getModel().getSize();
		double width = getHandler().getImageDisplay().getViewport().getWidth();
		double siz = getHandler().getThumbnailSize();
		if (width != 0) {
			double cols = 1;
			if (getHandler().getMode() != Mode.LIST)
				cols = Math.floor(width / siz);
			int rows = (int) Math.ceil(i / cols);

			Dimension d = new Dimension((int) width, (int) (rows * (siz + CAPTION_BUFFER)));
			setPreferredSize(d);
			setMaximumSize(d);
			setMinimumSize(d);
			setSize(d);
			setFixedCellWidth((int) (siz));
		}
		// give enough height for image and then file name caption
		setFixedCellHeight(getHandler().getThumbnailSize() + CAPTION_BUFFER);
		getHandler().getImageDisplay().revalidate();
		if (!getHandler().isLoading())
			getHandler().setFirstLoad();
		repaint();
	}

	/**
	 * @return the handler that provides the image and actions to perform on the
	 *         image
	 */
	ImageHandler getHandler() {
		return this.handler;
	}

	/**
	 * @return the groups identifying this ThumbnailList's images if its
	 *         represents an ArrayListModelSET and not just an ArrayListModel,
	 *         otherwise return null
	 */
	public String getGroups() {
		if (this.getModel() instanceof NLMSThumbnails)
			return ((NLMSThumbnails) this.getModel()).getName();
		return null;
	}

	@Override
	public ListModel<T> getModel() {
		return this.model;
	}

	/**
	 * Attempt to remove all references to this ThumbnailList
	 */
	public void destroy() {
		setDropTarget(null);
		setTransferHandler(null);
		getHandler().removePropertyChangeListener((PropertyChangeListener) getCellRenderer(), Property.values());
		setCellRenderer(null);

		ListSelectionListener[] li = this.getListSelectionListeners();
		int s = li.length - 1;
		for (; s > -1; s--)
			this.removeListSelectionListener(li[s]);
		getHandler().removePropertyChangeListener(this, Property.values());

		MouseListener[] mli = this.getMouseListeners();
		s = mli.length - 1;
		for (; s > -1; s--)
			this.removeMouseListener(mli[s]);
		this.setModel(new ArrayListModel<T>());
	}

	/**
	 * @param item
	 *            the menu item to add to the list
	 */
	public void addPopUpMenuItem(ThumbnailListMenuItem item) {
		this.menuItems.add(item);
	}
}
