package ilb;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import struct.ArrayListModel;
import struct.MetaImage;

/**
 * Demo - BasicDnD (Drag and Drop and Data Transfer)<http://docs.oracle.com
 * /javase/tutorial/uiswing/dnd/basicdemo.html>
 * 
 * A TransferHandler to handle dragging and dropping thumbnails in the GRID and
 * LIST mode
 * 
 * @author bonifantmc
 * 
 * 
 */

@SuppressWarnings("serial")
public class ThumbnailTransferHandler extends TransferHandler {
	/** the type of data that can be dragged and dropped, MetaImage */
	private final DataFlavor localObjectFlavor;
	/** provide access to the images and display area of said images */
	private final ImageHandler handler;

	/** objects being dragged and dropped */
	private Object[] transferedObjects = null;
	/** the selected indices */
	private int[] indices = null;
	/** location where selected items were dropped */
	private int addIndex = -1;
	/** the number of selected items */
	private int addCount = 0;

	/**
	 * 
	 * @param imageHandler
	 *            the ImageHandler, used for accessing the images being dragged
	 *            and dropped, and for refreshing their display area
	 */
	ThumbnailTransferHandler(ImageHandler imageHandler) {
		this.handler = imageHandler;
		this.localObjectFlavor = new ActivationDataFlavor(Object[].class, DataFlavor.javaJVMLocalObjectMimeType,
				"Array of items");
	}

	@Override
	protected Transferable createTransferable(JComponent c) {

		JList<?> list = (JList<?>) c;
		setIndices(list.getSelectedIndices());
		setTransferedObjects(list.getSelectedValuesList().toArray());
		return new DataHandler(getTransferedObjects(), getLocalObjectFlavor().getMimeType());
	}

	@Override
	public boolean canImport(TransferSupport info) {
		if (!info.isDrop() || !info.isDataFlavorSupported(getLocalObjectFlavor())) {
			return false;
		}
		return true;
	}

	@Override
	public int getSourceActions(JComponent c) {
		return MOVE; // TransferHandler.COPY_OR_MOVE;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean importData(TransferSupport info) {
		if (!canImport(info)) {
			return false;
		}
		JList<MetaImage> target;
		try {
			target = (JList<MetaImage>) info.getComponent();
		} catch (ClassCastException c) {
			return false;
		}

		JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();

		ArrayListModel<MetaImage> listModel = (ArrayListModel<MetaImage>) target.getModel();
		int index = dl.getIndex();
		int max = listModel.getSize();
		if (index < 0 || index > max) {
			index = max;
		}
		setAddIndex(index);
		try {
			Object[] v = (Object[]) info.getTransferable().getTransferData(getLocalObjectFlavor());
			MetaImage[] values = Arrays.copyOf(v, v.length, MetaImage[].class);
			setAddCount(values.length);
			for (int i = 0; i < values.length; i++) {
				int idx = index++;
				listModel.add(idx, values[i]);
				target.addSelectionInterval(idx, idx);

			}
			return true;
		} catch (UnsupportedFlavorException ufe) {
			ufe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	protected void exportDone(JComponent c, Transferable data, int action) {
		cleanup(c, action == MOVE);
	}

	/**
	 * after the images have been added remove their old position from the list
	 * 
	 * @param c
	 *            the JList the images came from
	 * @param remove
	 *            if true remove images from their prior location in the list
	 */
	private void cleanup(JComponent c, boolean remove) {
		if (remove && getIndices() != null) {
			JList<?> source = (JList<?>) c;
			ArrayListModel<?> model = (ArrayListModel<?>) source.getModel();
			// If we are moving items around in the same list, we
			// need to adjust the indices accordingly, since those
			// after the insertion point have moved.
			if (getAddCount() > 0) {
				for (int i = 0; i < getIndices().length; i++) {
					if (getIndices()[i] >= getAddIndex()) {
						getIndices()[i] += getAddCount();
					}
				}
			}
			for (int i = getIndices().length - 1; i >= 0; i--) {
				model.remove(getIndices()[i]);
			}
		}
		//

		this.handler.reindex(true);
		this.handler.setAscending(true);
		this.handler.setOrdering(MetaImage.SortOrder.ALT_SORT);

		setIndices(null);
		setAddCount(0);
		setAddIndex(-1);
	}

	/**
	 * @return the type of objects that can be dragged and dropped, mainly
	 *         MetaImages
	 */
	private DataFlavor getLocalObjectFlavor() {
		return this.localObjectFlavor;
	}

	/** @return the objects being dragged and dropped */
	private Object[] getTransferedObjects() {
		return this.transferedObjects;
	}

	/**
	 * 
	 * @param transferedObjects
	 *            the new objects being dragged and dropped
	 */
	private void setTransferedObjects(Object[] transferedObjects) {
		this.transferedObjects = transferedObjects;
	}

	/**
	 * 
	 * @return the indices of the objects being dragged and dropped, prior to
	 *         being dropped
	 */
	private int[] getIndices() {
		return this.indices;
	}

	/**
	 * 
	 * @param indices
	 *            the indices of the new objects being dragged and dropped,
	 *            prior to being dragged
	 */
	private void setIndices(int[] indices) {
		this.indices = indices;
	}

	/**
	 * 
	 * @return the location the objects being dragged and dropped are dropped at
	 */
	private int getAddIndex() {
		return this.addIndex;
	}

	/**
	 * 
	 * @param addIndex
	 *            the new drop location of the images being dragged and dropped
	 */
	private void setAddIndex(int addIndex) {
		this.addIndex = addIndex;
	}

	/**
	 * 
	 * @return the number of items being dragged and dropped
	 */
	private int getAddCount() {
		return this.addCount;
	}

	/**
	 * 
	 * @param addCount
	 *            the new number of items being dragged and dropped
	 */
	private void setAddCount(int addCount) {
		this.addCount = addCount;
	}

}