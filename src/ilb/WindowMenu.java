package ilb;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;

import image.editing.EditImage;

/**
 * This JMenu controls children windows of the application. It controls how many
 * can be open at once and gives the ability to close all EditImage windows
 * 
 * @author bonifantmc
 * 
 */
@SuppressWarnings("serial")
public class WindowMenu extends JMenu {

	/**
	 * element of window menu for indicating only on {@link EditImage} should
	 * display at a time
	 */
	private final Action singleWindow = new SingleWindow();

	/**
	 * element of window menu for indicating many {@link EditImage}s may be
	 * opened at a time
	 */
	private final Action manyWindows = new OneWindowPerImage();

	/**
	 * Action to close all windows except the main JFrame window FMList
	 */
	private final Action closeAllWindows = new CloseAllWindows();
	/** tracks the number of windows to keep open for displaying images */
	private final ImageHandler h;

	/**
	 * Builds the Window Menu
	 * 
	 * @param h
	 *            deals with images and the windows displaying them
	 */
	WindowMenu(ImageHandler h) {
		super("Windows");
		this.h = h;
		add(getSingleWindow());
		add(getManyWindows());
		addSeparator();
		add(getCloseAllWindows());
		setToolTipText("Control the windows this application creates.");

	}

	/**
	 * @return the Action that tells the application to keep only one EditImage
	 *         open
	 */
	Action getSingleWindow() {
		return this.singleWindow;
	}

	/**
	 * @return the Action that tells the application to allow any number of
	 *         EditImages to be open.
	 */
	Action getManyWindows() {
		return this.manyWindows;
	}

	/** @return the Action that closes all EditImages currently open */
	private Action getCloseAllWindows() {
		return this.closeAllWindows;
	}

	/**
	 * Action for setting the the number of children window to 1.
	 * 
	 * @author bonifantmc
	 * 
	 */
	private class SingleWindow extends AbstractAction {
		/** Set display name and tool-tip */
		SingleWindow() {
			putValue(NAME, "Single Window");
			putValue(SHORT_DESCRIPTION,
					"Only one window is ever opened for displaying full size images. If more than one window is opened, extras will be closed.");

		}

		/**
		 * disable current action since re-performing it will have no effect,
		 * change EditImage state
		 */
		@Override
		public void actionPerformed(ActionEvent arg0) {
			WindowMenu.this.h.oneEditImage = true;
			WindowMenu.this.h.closeAllEditImages();
			getSingleWindow().setEnabled(false);
			getManyWindows().setEnabled(true);

		}
	}

	/**
	 * Action for setting the the number of children window to 1 per item in the
	 * fml.ls.
	 * 
	 * @author bonifantmc
	 * 
	 */
	private class OneWindowPerImage extends AbstractAction {
		/** Set display name and tool-tip */
		OneWindowPerImage() {
			putValue(NAME, "Many Windows");
			putValue(SHORT_DESCRIPTION,
					"Open at a max one window per image in the list, if more than one window per image is opened, extras will be closed.");
			this.setEnabled(false);
		}

		/**
		 * Disable current action since re-performing it will have no effect,
		 * change EditImage state
		 */
		@Override
		public void actionPerformed(ActionEvent arg0) {
			WindowMenu.this.h.oneEditImage = false;
			getSingleWindow().setEnabled(true);
			getManyWindows().setEnabled(false);

		}
	}

	/**
	 * Action closing all childrent windows.
	 * 
	 * @author bonifantmc
	 * 
	 */
	private class CloseAllWindows extends AbstractAction {
		/** Set display name and tool-tip */
		CloseAllWindows() {
			putValue(NAME, "Close all windows");
			putValue(SHORT_DESCRIPTION, "Close all windows displaying images.");
		}

		/**
		 * close all windows currently opened (not including the main window);
		 */
		@Override
		public void actionPerformed(ActionEvent arg0) {
			WindowMenu.this.h.closeAllEditImages();
		}
	}
}
