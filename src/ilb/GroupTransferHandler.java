package ilb;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

import struct.ImageAnnotationPair;
import struct.NLMSThumbnails;
import struct.Property;
import struct.Thumbnail;

/**
 * This handler takes care of the drag and drop of images for the GroupList
 * JPanel
 * 
 * @author bonifantmc
 * 
 */
@SuppressWarnings("serial")
class GroupTransferHandler extends TransferHandler {
	/** flavor of objects that can be dragged and dropped, MetaImages */
	private final DataFlavor localObjectFlavor;
	/** Handler the provides images */
	private final ImageHandler handler;

	/** The GroupList using this TransferHandler */
	private final GroupList gl;

	/**
	 * @param h
	 *            the handler this needs to obtain image information
	 * @param gl
	 *            the GroupList using this TransgerHander
	 */
	GroupTransferHandler(ImageHandler h, GroupList gl) {
		this.handler = h;
		this.gl = gl;
		this.localObjectFlavor = new ActivationDataFlavor(Object[].class, DataFlavor.javaJVMLocalObjectMimeType,
				"Array of items");
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		return new DataHandler(new Object[] { c }, getLocalObjectFlavor().getMimeType());
	}

	@Override
	public boolean canImport(TransferSupport info) {
		if (!info.isDrop() || !info.isDataFlavorSupported(getLocalObjectFlavor()))
			return false;
		return true;
	}

	@Override
	public int getSourceActions(JComponent c) {
		return MOVE; // TransferHandler.COPY_OR_MOVE;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean importData(TransferSupport info) {
		if (!canImport(info))
			return false;
		ThumbnailList<ImageAnnotationPair> source;
		try {
			source = ((ThumbnailList<ImageAnnotationPair>) ((Object[]) info.getTransferable()
					.getTransferData(getLocalObjectFlavor()))[0]);
		} catch (IOException | UnsupportedFlavorException e) {
			e.printStackTrace();
			return false;
		}
		ThumbnailList<ImageAnnotationPair> target = (ThumbnailList<ImageAnnotationPair>) info.getComponent();
		String newName = ((NLMSThumbnails) target.getModel()).getName();

		// dropping into the same place is pointless
		if (source == target)
			return true;
		for (Thumbnail m : source.getSelectedValuesList())
			this.gl.groupListed.move(m, newName);

		getHandler().setMasterListChanged(true);
		return true;
	}

	@Override
	protected void exportDone(JComponent c, Transferable data, int action) {
		super.exportDone(c, data, action);
		getGl().getHandler().firePropertyChange(Property.displayArea, null);
	}

	/** @return allowed DataFlavors */
	DataFlavor getLocalObjectFlavor() {
		return this.localObjectFlavor;
	}

	/** @return the handler providing this with image information */
	private ImageHandler getHandler() {
		return this.handler;
	}

	/**
	 * @return the GroupList using this TransferHandler
	 */
	public GroupList getGl() {
		return this.gl;
	}

}