package ilb;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import struct.Group;
import struct.MetaImage;
import struct.NLMSThumbnails;

/**
 * Pop-up menu for when user right clicks on a a thumbnail. Lets the user open,
 * rename, or delete the image.
 * 
 * @author bonifantmc
 */
@SuppressWarnings("serial")
public class ListPopUpMenu extends JPopupMenu {
	/***
	 * All JMenuItems in the ThumbnailList's popup menu should extend this base
	 * class
	 * 
	 * @author bonifantmc
	 *
	 */
	public static class ThumbnailListMenuItem extends JMenuItem {
		/** The Meta Image being acted on */
		MetaImage i;
		/** the ImageHandler in use */
		ImageHandler h;
		/** The Group being acted on */
		Group g;

		/**
		 * 
		 * @param s
		 *            text display of the menu item
		 * @param i
		 *            the image to be used
		 * @param h
		 *            the handler to be used
		 * @param g
		 *            the group to be used
		 */
		ThumbnailListMenuItem(String s, MetaImage i, ImageHandler h, Group g) {
			super(s);
			this.i = i;
			this.h = h;
			this.g = g;

		}

		/**
		 * enable or disable the MenuItem based on its own person requirments
		 */
		public void checkAndEnable() {
			this.setEnabled(false);
		}
	}

	/** action for opening images */
	public static class Open extends ThumbnailListMenuItem {
		/**
		 * 
		 * @param image
		 *            the image to open
		 * @param handler
		 *            the handler that supplies the image
		 */
		Open(MetaImage image, ImageHandler handler) {
			super("Open", image, handler, null);
			setToolTipText("Open this image to edit its annotations.");

			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Open.this.h.openImage(Open.this.i);

				}
			});
		}

		@Override
		public void checkAndEnable() {
			this.setEnabled(true);
		}

	}

	/**
	 * Action for renaming images
	 * 
	 * @author bonifantmc
	 *
	 */
	public static class Rename extends ThumbnailListMenuItem {
		/**
		 * 
		 * @param image
		 *            the image to rename
		 * @param handler
		 *            the handler that supplies the image
		 */
		Rename(MetaImage image, ImageHandler handler) {
			super("Rename", image, handler, null);
			setToolTipText("Enter a new name for this image.");

			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Rename.this.h.rename(Rename.this.i);

				}
			});
		}

		@Override
		public void checkAndEnable() {
			this.setEnabled(true);
		}

	}

	/**
	 * Action for deleting an image from the list
	 * 
	 * @author bonifantmc
	 *
	 */
	public static class DeleteFromList extends ThumbnailListMenuItem {
		/**
		 * 
		 * @param image
		 *            the image to delete
		 * @param h
		 *            the handler that supplies the image
		 */
		DeleteFromList(MetaImage image, ImageHandler h) {
			super("Delete From List", image, h, null);
			setToolTipText("Remove this image from the list.");
			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					h.deleteFromList(DeleteFromList.this.i);

				}
			});
		}

		@Override
		public void checkAndEnable() {
			this.setEnabled(this.h.getListFile() != null);
		}

	}

	/**
	 * Action for rotating an image in
	 * 
	 * @author bonifantmc
	 *
	 */
	public static class Rotate extends ThumbnailListMenuItem {
		/**
		 * 
		 * @param image
		 *            the image to rotate
		 * @param h
		 *            the handler that supplies the image
		 */
		Rotate(MetaImage image, ImageHandler h) {
			super("Rotate", image, h, null);
			setToolTipText("Perform a quarter rotation on the image.");
			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					h.rotateAngle(Rotate.this.i);

				}
			});
		}

		@Override
		public void checkAndEnable() {
			this.setEnabled(true);
		}
	}

	/**
	 * Action for pattern matching entire list
	 * 
	 * @author bonifantmc
	 *
	 */
	public static class PatternMatch extends ThumbnailListMenuItem {
		/**
		 * 
		 * @param arr
		 *            the images to pattern match
		 * @param h
		 *            the handler that supplies the image
		 * @param g
		 *            the group to match with
		 */
		PatternMatch(NLMSThumbnails arr, Group g, ImageHandler h) {
			super("Pattern Match", null, h, g);
			setToolTipText("Organize the images into groups based on the given glob pattern.");
			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// if (g.patternMatch(arr, h.getImageDisplay()))
					// h.firePropertyChange(g.getProperty(), null);

				}
			});
		}

	}

	/**
	 * Action for changing a group's tag
	 * 
	 * @author bonifantmc
	 *
	 */
	public static class ChangeGroupTag extends ThumbnailListMenuItem {
		/**
		 * 
		 * @param image
		 *            the image to rotate
		 * @param h
		 *            the handler that supplies the image
		 * @param g
		 *            the group to change the tag for
		 */
		ChangeGroupTag(MetaImage image, ImageHandler h, Group g) {
			super("Change Group Tag", image, h, g);
			setToolTipText("Change this image's tag and move it to a new group.");
			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					h.changeListTag((NLMSThumbnails) getModel(), g);
				}
			});
		}

	}

	/**
	 * Action for searching by image/sorting entire list by a specified image
	 * 
	 * @author bonifantmc
	 *
	 */
	public static class SearchByImage extends ThumbnailListMenuItem {
		/**
		 * 
		 * @param image
		 *            the image to search by
		 * @param h
		 *            the handler that supplies the image
		 */
		SearchByImage(MetaImage image, ImageHandler h) {
			super("Search By Image", image, h, null);
			setToolTipText("Organize all images based on similarity to this image.");
			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					fm.FaceMatchWorker.searchByImage(SearchByImage.this.i, h);
				}
			});
		}

		@Override
		public void checkAndEnable() {
			this.setEnabled(fm.FaceMatchJavaInterface.loaded);
		}

	}

	/**
	 * Action for searching by image/sorting entire list by a specified image
	 * 
	 * @author bonifantmc
	 *
	 */
	public static class SearchByImageAnnotation extends ThumbnailListMenuItem {
		/**
		 * 
		 * @param image
		 *            the image to search by
		 * @param h
		 *            the handler that supplies the image
		 */
		SearchByImageAnnotation(MetaImage image, ImageHandler h) {
			super("Search By Annotation", image, h, null);
			setToolTipText(
					"Select an annotation and organize all images by similarity to the selected annotation in this image.");
			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					fm.FaceMatchWorker.searchByImage(SearchByImageAnnotation.this.i, h);
				}
			});
		}

		@Override
		public void checkAndEnable() {
			this.setEnabled(fm.FaceMatchJavaInterface.loaded);
		}

	}

	/**
	 * Build the drop-down menu.
	 * 
	 * @param menuItems
	 *            the items to be used in the menu
	 */
	ListPopUpMenu(ArrayList<ThumbnailListMenuItem> menuItems) {
		for (JMenuItem item : menuItems)
			if (item != null)
				add(item);
	}

}