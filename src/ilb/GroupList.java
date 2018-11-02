package ilb;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import struct.Group;
import struct.ImageGroups;
import struct.NLMSThumbnails;
import struct.Property;
import struct.Property.PropertyChangeEvent;
import struct.Property.PropertyChangeListener;
import struct.Thumbnail;

/**
 * The Panel to display in the ImageHandlers MasterSet when the user is doing
 * Visual Grouping. Displays a JPanel with a vertical box layout filled with
 * JPanels with horizontal box layouts. These children JPanels then contain
 * ThumbnailLists of image groupings
 * 
 * @author bonifantmc
 * 
 */

@SuppressWarnings("serial")
public class GroupList extends JPanel implements ListSelectionListener, DropTargetListener, PropertyChangeListener {
	/** the handler that supplies the images for this to display */
	private final ImageHandler handler;
	/** handles most of drag and drop for this panel */
	private final GroupTransferHandler transferer;
	/**
	 * the spot where a drop will occur if not inside one of the existing lists,
	 * in which case a new one is made and added to the panel
	 */
	private Point dropSpot;
	/** true if something is being dragged in the panel */
	private boolean dragging;

	/** The Groups this Group List displays */
	Group groupListed;

	/**
	 * Build up the basic structure of the panel
	 * 
	 * @param h
	 *            the ImageHandler that supplies this with images
	 * @param g
	 *            the groups this group list displays.
	 */
	public GroupList(ImageHandler h, Group g) {
		this.handler = h;
		this.groupListed = g;
		this.transferer = new GroupTransferHandler(getHandler(), this);

		getHandler().addPropertyChangeListener(this, Property.mode, Property.loading, Property.thumbnailSize,
				g.getProperty(), Property.displayArea, Property.annotationGroups, Property.imageGroups);

		setAlignmentX(LEFT_ALIGNMENT);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setDropTarget(new DropTarget(this, this));
		getDropTarget().setActive(true);
		setOpaque(true);
		setAutoscrolls(true);
		setTransferHandler(getTransferer());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		for (Component c : getComponents()) {
			if (c != arg0.getSource())
				if (c instanceof ThumbnailList)
					((ThumbnailList<Thumbnail>) c).clearSelection();

		}
	}

	/**
	 * rebuild the panel
	 */
	@SuppressWarnings("unchecked")
	public void reset() {
		synchronized (this) {
			for (Component c : this.getComponents())
				if (c instanceof ThumbnailList)
					((ThumbnailList<Thumbnail>) c).destroy();

			removeAll();
			ArrayList<NLMSThumbnails> arr = new ArrayList<>(this.groupListed.getLists());

			if ((getHandler().getMode() == Mode.GROUPING_IMAGES || getHandler().getMode() == Mode.GROUPING_ANNOTATIONS)
					&& !getHandler().isLoading() && arr.size() > 0) {

				Collections.sort(arr, arr.get(0).buildComparator());

				for (NLMSThumbnails g : arr) {
					if (g.size() > 0) {
						add(Box.createVerticalStrut(10));
						add(new ThumbnailList<>(g, getTransferer(), getHandler()));
					}
				}
				add(Box.createVerticalStrut(5));
				getHandler().getImageDisplay().revalidate();
				getHandler().getImageDisplay().repaint();
				getHandler().firePropertyChange(Property.displayArea, null);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Component add(Component c) {
		Component ret = super.add(c);
		if (c instanceof ThumbnailList)
			setDisp((ThumbnailList<Thumbnail>) c);

		return ret;
	}

	/** set display for all ThumbnailLists in the panel */
	@SuppressWarnings("unchecked")
	private void setDisp() {
		for (Component c : getComponents())
			if (c instanceof ThumbnailList)
				setDisp((ThumbnailList<Thumbnail>) c);
	}

	/**
	 * @param l
	 *            the list to set the display for
	 */
	private void setDisp(ThumbnailList<Thumbnail> l) {
		l.setAlignmentX(LEFT_ALIGNMENT);
		l.addListSelectionListener(this);
		((ThumbnailRenderer) l.getCellRenderer()).setOpaque(false);
		l.setSize(getWidth(), l.getHeight());
		l.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		l.setFixedCellWidth(getHandler().getThumbnailSize());
		Color c = groupListed instanceof ImageGroups ? Color.BLUE : Color.RED;
		TitledBorder title = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(c, 2), l.getGroups());
		l.setBorder(title);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Component add(Component c, int i) {
		Component ret = super.add(c, i);
		if (c instanceof ThumbnailList)
			setDisp((ThumbnailList<Thumbnail>) c);
		return ret;

	}

	@SuppressWarnings("boxing")
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		switch (arg0.getProperty()) {
		case mode:
			switch ((Mode) arg0.getNewValue()) {
			case GRID:
			case LIST:
			case GROUPING_IMAGES:
			case GROUPING_ANNOTATIONS:
			default:

				reset();
			}
			break;

		case loading:
			if (!(boolean) arg0.getNewValue())
				reset();
			break;
		case thumbnailSize:
			setDisp();
			break;
		case imageList:
		case imageGroups:
		case annotationGroups:
			reset();
			break;
		case ascending:
		case directory:
		case displayArea:
		case listFile:
		case ordering:
		case pattern:
		case rotation:
		case tally:
		default:
			break;

		}
	}

	@Override
	public void dragEnter(DropTargetDragEvent arg0) {
		setDragging(true);
		if (getComponentAt(arg0.getLocation()) instanceof Box.Filler)
			this.dropSpot = arg0.getLocation();
		else
			this.dropSpot = null;
	}

	@Override
	public void dragExit(DropTargetEvent arg0) {
		// do nothing
	}

	@Override
	public void dragOver(DropTargetDragEvent arg0) {
		arg0.acceptDrag(DnDConstants.ACTION_MOVE);
		repaint();
	}

	/**
	 * If a drop is in the GroupList then its creating a new ArrayListModelSet
	 * in the CategoryTable, ask for a name for the group, move the selected
	 * images to that group
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void drop(DropTargetDropEvent info) {

		info.acceptDrop(DnDConstants.ACTION_MOVE);
		int dropIndex = indexOf(this.getComponentAt(info.getLocation()));

		// test if drop location is valid
		if (!(dropIndex > -1 && dropIndex < getComponentCount())) {
			// invalid
			info.dropComplete(false);
			return;
		}

		// prompt user for a name for this new group
		String newName = getHandler().chooseACategory(null);
		// make sure the user didn't cancel or give an invalid response
		if (newName == null)
			return;

		// get source list
		ThumbnailList<Thumbnail> source;
		try {
			source = ((ThumbnailList<Thumbnail>) ((Object[]) info.getTransferable()
					.getTransferData(((GroupTransferHandler) getTransferHandler()).getLocalObjectFlavor()))[0]);
		} catch (UnsupportedFlavorException | IOException e) {
			// fail silently, ie if it can't work do nothing.
			e.printStackTrace();
			return;
		}

		for (Thumbnail m : source.getSelectedValuesList())
			this.groupListed.move(m, newName);

		setDragging(false);

		try {
			info.dropComplete(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		getHandler().firePropertyChange(this.groupListed.getProperty(), null);
		getHandler().getImageDisplay().invalidate();

	}

	/**
	 * 
	 * @param c
	 *            the component to look for
	 * @return the index of said component or -1 if not found
	 */
	int indexOf(Component c) {
		int i = -1;
		boolean found = false;
		for (Component a : this.getComponents()) {
			i++;
			if (a == c) {
				found = true;
				break;
			}
		}
		return found ? i : -1;
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent arg0) {
		if (arg0.getDropAction() == DnDConstants.ACTION_MOVE)
			arg0.acceptDrag(arg0.getDropAction());

	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Point p = getDropSpot();
		if (p != null && isDragging())
			g.drawLine(0, (int) p.getY(), getWidth(), (int) p.getY());
	}

	/** @return the ImageHandler for this panel */
	public ImageHandler getHandler() {
		return this.handler;
	}

	/** @return the TransferHandler for this panel */
	public GroupTransferHandler getTransferer() {
		return this.transferer;
	}

	/**
	 * 
	 * @return true if somethings being dragged on the screen
	 */
	public boolean isDragging() {
		return this.dragging;
	}

	/**
	 * 
	 * @param b
	 *            whether the mouse is dragging something now or not
	 */
	public void setDragging(boolean b) {
		this.dragging = b;
	}

	/**
	 * 
	 * @return the point where a drop event can occur between lists to add a new
	 *         list instead of adding to an old one
	 */
	Point getDropSpot() {
		return this.dropSpot;
	}
}
