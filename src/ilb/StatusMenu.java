package ilb;

import java.awt.Component;

import javax.swing.JLabel;

/***
 * The Status Menu is a collection JLabels providing details about the state of
 * the list (the number of images that exist, the number that have been
 * reviewed, and the current set that is visible)
 * 
 * @author bonifantmc
 *
 */
@SuppressWarnings("serial")
public class StatusMenu extends JLabel {
	/** provide access to other areas of the application */
	private final ImageHandler h;

	/** current number of images viewed */
	private int cur;
	/** total number of images */
	private int max;

	/**
	 * Instantiates the StatusMenu
	 * 
	 * @param h
	 *            ImageHandler needed to get access to other parts of the
	 *            application
	 */
	public StatusMenu(ImageHandler h) {
		this.h = h;

	}

	/**
	 * Update the number of images viewed and max number of images
	 * 
	 * @param cur
	 *            number viewed
	 * @param max
	 *            total number of images
	 */
	public void updateTally(int cur, int max) {
		this.cur = cur;
		this.max = max;
		this.setText();
	}

	// TODO MAKE MORE FUNCTIONAL IN TERMS OF TRACKING VISIBLE IMAGES

	/** sets the text of the label */
	public void setText() {
		ThumbnailList<?> l = null;
		for (Component c : this.h.getImageDisplay().getViewport().getComponents())
			if (c instanceof ThumbnailList) {
				l = (ThumbnailList<?>) c;
				break;
			}
		int i = 0, j = 0;
		if (l != null) {
			i = l.getFirstVisibleIndex() + 1;
			j = l.getLastVisibleIndex() + 1;
		}
		String t = "Images Viewed: " + this.cur + "/" + this.max + ".";
		if (this.h.getMode() == Mode.GRID || this.h.getMode() == Mode.LIST)
			t += " Images Visible: " + i + " - " + j;
		this.setText(t);

	}

}
