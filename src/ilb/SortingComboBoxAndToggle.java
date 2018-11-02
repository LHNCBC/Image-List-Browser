package ilb;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

import struct.MetaImage;
import struct.Property;
import struct.Property.PropertyChangeEvent;
import struct.Property.PropertyChangeListener;

/**
 * Controls the ComboBox and ArrowButton used for sorting the ImageHandler's
 * master lists. The Arrow is a toggle (up/north for ascending order, and
 * down/south for descending order). The combo box lists the different sort
 * methods the user can use as defined by the MetaImage.SortOrder enumerated
 * type.
 * 
 * @author bonifantmc
 * 
 */
class SortingComboBoxAndToggle implements PropertyChangeListener {
	/**
	 * Lets the user select sorting methods
	 */
	private final JComboBox<MetaImage.SortOrder> comboBox = new JComboBox<>(MetaImage.SortOrder.values());
	/**
	 * Lets the user select sorting order
	 */
	private final BasicArrowButton toggle = new BasicArrowButton(SwingConstants.NORTH);

	/**
	 * the ImageHandler provides updates via property change events for updating
	 * the GUI
	 */
	private final ImageHandler handler;

	/**
	 * Prepares the ComboBox and ArrowButton
	 * 
	 * @param h
	 *            the ImageHandler that provides updates to this class
	 */
	SortingComboBoxAndToggle(ImageHandler h) {
		this.handler = h;
		getHandler().addPropertyChangeListener(this, Property.ascending, Property.ordering);
		getComboBox().setToolTipText("Sort Order.");
		getComboBox().addActionListener(new ComboAction());
		// sorting toggle
		getToggle().setToolTipText("Ascending/Descending");
		getToggle().addActionListener(new ArrowAction());
	}

	/**
	 * Define action for the toggle Arrow Button
	 */
	@SuppressWarnings("serial")
	class ArrowAction extends AbstractAction {
		/**
		 * Switch between North and South directions whenever clicked, and
		 * always flip the ImageHandler's ascending boolean. Sort images in
		 * handler accordingly.
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			getHandler().setAscending(!getHandler().isAscending());
			if (SortingComboBoxAndToggle.this.getToggle().getDirection() == SwingConstants.NORTH)
				SortingComboBoxAndToggle.this.getToggle().setDirection(SwingConstants.SOUTH);
			else
				SortingComboBoxAndToggle.this.getToggle().setDirection(SwingConstants.NORTH);
			if (getHandler().getMasterList().size() > 0) {
				getHandler().sort();
				getHandler().reindexMaster();
				getHandler().setTick(0);
			}
		}
	}

	/**
	 * Define action for the drop down menu.
	 * 
	 * @author bonifantmc
	 * 
	 */
	@SuppressWarnings("serial")
	class ComboAction extends AbstractAction {

		/**
		 * Change to the new sort order based on user input, sort master list,
		 * move user back to the top of the image display and repaint the the
		 * display.
		 */
		@Override
		public void actionPerformed(ActionEvent arg0) {

			MetaImage.SortOrder val = (MetaImage.SortOrder) getComboBox().getSelectedItem();

			getHandler().setOrdering(val);
			if (getHandler().getMasterList().size() > 0) {
				getHandler().sort();
				getHandler().reindexMaster();
				getHandler().setTick(0);
			}
		}
	}

	@SuppressWarnings("boxing")
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		switch (arg0.getProperty()) {
		case ascending:
			if ((boolean) arg0.getNewValue())
				getToggle().setDirection(SwingConstants.NORTH);
			else
				getToggle().setDirection(SwingConstants.SOUTH);
			break;
		case ordering:
			getHandler().reindex(true);
			getComboBox().setSelectedItem(arg0.getNewValue());
			break;
		default:
			break;
		}
	}

	/**
	 * @return the drop down menu of different sort orders
	 */
	JComboBox<MetaImage.SortOrder> getComboBox() {
		return this.comboBox;
	}

	/** @return the ascending/descending toggle button */
	BasicArrowButton getToggle() {
		return this.toggle;
	}

	/**
	 * @return the ImageHandler whose sort order and ascending order this class
	 *         allows users to edit
	 */
	ImageHandler getHandler() {
		return this.handler;
	}
}
