package ilb;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import annotations.Attribute;
import struct.MetaImage;
import struct.Property;
import struct.Property.PropertyChangeEvent;
import struct.Property.PropertyChangeListener;
import struct.URLMetaImage;

/**
 * The various file choosers used for opening repositories and lists in
 * different fashions. Also handles clearing and saving lists, as well as the
 * actual loading of images.
 * 
 * @author bonifantmc
 * 
 */
@SuppressWarnings("serial")
public class FileMenu extends JMenu implements PropertyChangeListener {

	/** Opens and displays all images in a repository */
	private final Action reposOpener = new BrowseRepositories(false);

	/** Opens and displays all images in a .lst file */
	private final Action listOpener = new BrowseLists();

	/** Open a list of images from urls */
	private final Action urlOpener = new URLOpen();

	/** Saves the Handler's current master list */
	final Action saver = new SaveAs();

	/** Clears Handlers master list, directory, list file, and pattern */
	private final Action clearer = new Clear();

	/** Filters a repository by glob pattern matching */
	private final Action globber = new GlobMask();

	/** Clear Handler's pattern */
	private final Action globClearer = new ClearGlob();

	/** Clear Handler's list */
	private final Action listClearer = new ClearList();

	/** filter images */
	private final Action filter = new Filter();
	/** Adds the ability to generate lists ready for CSU evaluation */
	// private final Action csuEvaluator;

	/** an Array of all Actions this Menu contains */
	private final Action[] actions = { this.reposOpener, this.listOpener, this.saver, this.clearer, this.globber,
			this.globClearer, this.listClearer,
			this.filter /* , this.csuEvaluator */ };

	/**
	 * 
	 */
	public static final FileFilter directoryFilter = new FileFilter() {
		@Override
		public boolean accept(File arg0) {
			return arg0.isDirectory() || arg0.getParentFile().exists();
		}

		@Override
		public String getDescription() {
			return "Directories";
		}
	};

	/**
	 * valid lists are tab separated format files, preferably with .txt, .tsv,
	 * or .lst extensions.
	 */
	public static final String[] VALID_TEXT_EXTENSIONS = { ".txt", ".tsv", ".lst" };
	/**
	* 
	*/
	public static final FileFilter textListFilter = new FileFilter() {

		@Override
		public boolean accept(File arg0) {
			if (arg0.isDirectory())
				return true;
			for (String ext : FileMenu.VALID_TEXT_EXTENSIONS)
				if (arg0.getName().toLowerCase().endsWith(ext))
					return true;
			return false;
		}

		@Override
		public String getDescription() {
			return "txt/lst/tsv files";
		}
	};

	/**
	 * Handler supplying information about the Images being loaded, and sending
	 * out property changes so the menus know how to adjust and display
	 */
	private final ImageHandler handler;

	/**
	 * Build ILB's file menu
	 * 
	 * @param h
	 *            Handler that supplies image information and info about what
	 *            files are open for the menu
	 */
	FileMenu(ImageHandler h) {
		this.handler = h;
		getHandler().addPropertyChangeListener(this, Property.listFile, Property.directory, Property.pattern);
		// this.csuEvaluator = new GenerateCSUScripts(this.getHandler()); //TODO
		// REMOVE, This is for the way old 2000's Colorado State University
		// FERET test
		// this.actions[7] = this.csuEvaluator;
		setBorder(BorderFactory.createRaisedBevelBorder());
		setText("File");
		add(this.reposOpener);
		add(this.clearer);
		addSeparator();
		add(this.listOpener);
		add(this.listClearer);
		add(this.saver);
		addSeparator();
		add(this.urlOpener);
		addSeparator();
		add(this.globber);
		add(this.globClearer);
		add(this.filter);
		// addSeparator();
		// add(this.csuEvaluator);
		setToolTipText("Load, save, or clear your screen.");
	}

	/**
	 * This class handles opening a file browser for selecting a list file to
	 * open and use.
	 * 
	 * @author bonifantmc
	 * 
	 */
	private class BrowseLists extends AbstractAction {
		/**
		 * Set the name to appear in the File menu and the tooltip to go with
		 * it.
		 */
		BrowseLists() {
			putValue(NAME, "Open List");
			putValue(SHORT_DESCRIPTION,
					"Select a .lst or .txt file to read images and annotations from (list must detail location of image relative to the currently opened directory).");
		}

		/**
		 * Opens a file chooser that let's the user navigate to select lst/txt
		 * files. Will force the user to select a repository first if one is not
		 * selected yet for this menu's handler
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			getHandler().checkedSaving();
			getHandler().setMasterListChanged(false);

			// need a repository to use a lst file
			if (getHandler().getDirectory() == null || !getHandler().getDirectory().exists())
				new BrowseRepositories(true).actionPerformed(null);

			// user must have canceled browse
			if (!getHandler().getDirectory().exists())
				return;

			JFileChooser chooser = FileMenu.browser(FileMenu.VALID_TEXT_EXTENSIONS, JFileChooser.FILES_AND_DIRECTORIES,
					getHandler().getListFileDirecotry(), textListFilter, "Select a List to Open",
					"The file chosen does not exist or is not a lst file.");

			if (chooser.showOpenDialog(getHandler().getImageDisplay()) == JFileChooser.APPROVE_OPTION) {
				getHandler().setListFile(chooser.getSelectedFile());
				getHandler().load();
			}
		}
	}

	/**
	 * The Browse function for finding an image repository.
	 * 
	 * @author bonifantmc
	 * 
	 */
	private class BrowseRepositories extends AbstractAction {
		/**
		 * If true then this browser is being opened because a user tried to
		 * open the list browser or glob entry before having a valid repository
		 * selected.
		 */
		private boolean setting;

		/**
		 * 
		 * @param tryingToLoadListOrGlob
		 *            true if getting immediately before loading a list file
		 */
		BrowseRepositories(boolean tryingToLoadListOrGlob) {
			super("Open Directory");
			putValue(SHORT_DESCRIPTION, "Open a directory and load all images from it (subdirectories not included.");
			this.setting = tryingToLoadListOrGlob;
		}

		/**
		 * Displays a FileChooser for selecting a directory to set as the
		 * repository.
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			getHandler().checkedSaving();
			getHandler().setMasterListChanged(false);
			JFileChooser fcPath = new JFileChooser() {
				@Override
				public void approveSelection() {
					File f = getSelectedFile();
					if (!f.exists() || !f.isDirectory()) {
						JOptionPane.showMessageDialog(this, "The directory chosen does not exist.");
						return;
					}
					super.approveSelection();
				}
			};
			fcPath.setCurrentDirectory(getHandler().getDirectory());
			if (this.setting)
				fcPath.setDialogTitle("Select a directory to open, then select a list to open.");
			else
				fcPath.setDialogTitle("Select a directory to open and load images from.");

			fcPath.setFileFilter(directoryFilter);
			fcPath.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (fcPath.showOpenDialog(getHandler().getImageDisplay()) == JFileChooser.APPROVE_OPTION) {
				getHandler().setDirectory(fcPath.getSelectedFile());
				if (!this.setting) {
					getHandler().setListFile(null);
					getHandler().load();
				}
			}
		}
	}

	/**
	 * Save option in menu, for when users want to force a save
	 * 
	 * @author bonifantmc
	 * 
	 */
	class SaveAs extends AbstractAction {

		/**
		 * Default constructor, prepares tool tip, and title display for the
		 * JMenu
		 */
		SaveAs() {
			putValue(NAME, "Save as");
			putValue(SHORT_DESCRIPTION, "Save a .lst file of the images and annotations displayed below");
		}

		/**
		 * Creates a FileChooser to pick the save destination and then saves
		 * this ImageHandler's masterlist to that destination
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			getNewSaveSpot(getHandler());
			save(getHandler(), false);
		}

	}

	// TODO implenet a save that isn't save as

	// class Save extends AbstractAction{}

	/**
	 * @author bonifantmc Read in images from URLs
	 */
	class URLOpen extends AbstractAction {
		/**
		 * 
		 */
		URLOpen() {
			putValue(NAME, "Open URL List");
			putValue(SHORT_DESCRIPTION, "Open a list of images taken from urls on the internt");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			getHandler().setDirectory(null);
			JFileChooser chooser = browser(FileMenu.VALID_TEXT_EXTENSIONS, JFileChooser.FILES_AND_DIRECTORIES,
					getHandler().getListFileDirecotry(), textListFilter, "Open a list of URLs", "Not a list of URLs");
			if (chooser.showOpenDialog(getHandler().getImageDisplay()) == JFileChooser.APPROVE_OPTION) {
				getHandler().setListFile(chooser.getSelectedFile());
				getHandler().load();
			}
		}

	}

	/**
	 * Provides users with the option to enter a Glob to pattern match files
	 * against for display in the ThumbnailList as an additional filter beyond
	 * the generic lst file
	 * 
	 * @author bonifantmc
	 * 
	 */
	private class GlobMask extends AbstractAction {

		/**
		 * Creates basic user interface setting up a title to display in the
		 * JMenu, and its tool tip.
		 */
		GlobMask() {
			putValue(NAME, "Search Files by Name");
			putValue(SHORT_DESCRIPTION,
					"Seach images with basic pattern matching on file names ex: \"*010*.jpg\" will search for all jpgs with 010 in their file name.");
		}

		/**
		 * The actual action. Creates an input dialog to accept a String glob,
		 * if one is entered.
		 * 
		 * @param evt
		 *            the event causing this action to be performed
		 */
		@Override
		public void actionPerformed(ActionEvent evt) {
			// must have a repository
			if (getHandler().getDirectory() == null)
				new BrowseRepositories(true).actionPerformed(null);
			// get desired new name
			String glob = JOptionPane.showInputDialog(getHandler().getImageDisplay(),
					"<html>Enter a Pattern Mask <br> Example: *001* matches all images<br> with \"001\" somwhere in the name. <br>Matched Images will appear first in the list.");
			if (glob == null || glob.trim().equals("")
					|| (getHandler().getPattern() != null && FileMenu.this.getHandler().getPattern().equals(glob)))
				return;

			getHandler().setPattern(glob);
		}
	}

	/**
	 * This handles clearing the entire list and repository so there are no
	 * thumbnails displayed
	 */
	private class Clear extends AbstractAction {
		/** Set the action title for display and its tooltip */
		Clear() {
			putValue(NAME, "Close");
			putValue(SHORT_DESCRIPTION, "Closes the Directory and list, removes search term.");
		}

		@Override
		public void actionPerformed(ActionEvent e) {

			// TODO CHECK OTHER THREADS AND CANCEL THEM.
			getHandler().checkedSaving();
			if (ListReader.r != null && !ListReader.r.isDone()) {
				ListReader.r.cancel(this.enabled);
				while (!ListReader.r
						.isDone()) {/*
									 * block TODO better way to block thread?
									 */
				}
			}
			getHandler().setMasterListChanged(false);
			getHandler().getMasterList().clear();
			getHandler().getAnnotationGroups().clear();
			getHandler().getImageGroups().clear();
			getHandler().closeAllEditImages();
			
			
			getHandler().setListFile(null);
			getHandler().setDirectory(null);
			getHandler().setPattern(null);

			// TODO WHY DID THIS START BUGGING
			getHandler().firePropertyChange(Property.annotationGroups, null);
			getHandler().firePropertyChange(Property.imageGroups, null);
		}
	}

	/**
	 * Clears the current glob, and disables the Clear Glob choice on the JMenu.
	 * 
	 * @author bonifantmc
	 */
	private class ClearGlob extends AbstractAction {
		/**
		 * Sets the name to display in the file menu for this action and its
		 * tooltip
		 */
		ClearGlob() {
			putValue(NAME, "Clear search");
			putValue(SHORT_DESCRIPTION, "Remove pattern matching factor from displayed thumbnails.");

		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			getHandler().setPattern(null);
		}
	}

	/**
	 * Clears the current .lst file and disables the clear list option on the
	 * JMenu. Reloads entire repository (with glob filter if applied).
	 * 
	 * @author bonifantmc
	 * 
	 */
	private class ClearList extends AbstractAction {
		/** sets the name to display in the file menu and its tooltip */
		ClearList() {
			putValue(NAME, "Close List");
			putValue(SHORT_DESCRIPTION, "Closes the list leaving the directory open.");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			getHandler().checkedSaving();
			if (ListReader.r != null && !ListReader.r.isDone()) {
				ListReader.r.cancel(this.enabled);
				while (!ListReader.r
						.isDone()) {/*
									 * block TODO better way to block thread?
									 */
				}
			}
			getHandler().setMasterListChanged(false);
			getHandler().setListFile(null);
			getHandler().load();
		}

	}

	/**
	 * Menu Item that lets the user filter their images by selected attributes.
	 * 
	 * @author bonifantmc
	 *
	 */
	private class Filter extends AbstractAction {
		/** sets display name, and tooltip of action */
		Filter() {
			putValue(NAME, "Filter List");
			putValue(SHORT_DESCRIPTION, "Filters the List by selected attributes.");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (getHandler().getFilter() == null)
				getHandler().setFilter(Attribute.Gender);
			else
				getHandler().setFilter(null);
		}

	}

	/**
	 * Open a dialog asking for a new save spot, sets as the listFile for the
	 * input ImageHandler
	 * 
	 * @param handler
	 *            the handler whose listFile field is to be changed
	 */
	public static void getNewSaveSpot(ImageHandler handler) {
		JFileChooser save = new JFileChooser();
		save.setApproveButtonText("Save");
		if (handler.getListFile() != null)
			save.setCurrentDirectory(handler.getListFileDirecotry());

		if (save.showOpenDialog(handler.getImageDisplay()) == JFileChooser.APPROVE_OPTION)
			handler.setListFile(save.getSelectedFile());
	}

	/**
	 * Saves the given handler's master list at its listfile
	 * 
	 * @param h
	 *            the handler whose master list is to be saved
	 * 
	 * @param original
	 *            If true the saved file is in accordance with the original
	 *            index of the masterlist, if false with current index
	 * @return true if successful, false if not
	 */
	public static boolean save(final ImageHandler h, boolean original) {
		File saveToTemp = h.getListFile();
		if (!saveToTemp.getName().endsWith(".lst") && !saveToTemp.getName().endsWith(".tsv")
				&& !saveToTemp.getName().endsWith(".txt"))
			saveToTemp = new File(saveToTemp.getParent(), saveToTemp.getName() + ".lst");
		final File saveTo = saveToTemp;
		File temp = new File("tmp.lst");
		if (!temp.exists())
			try {
				if (!temp.createNewFile()) {
					h.getProgressBar().message.setText("CANNOT WRITE TO THIS DIRECTORY");
					return false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		if (saveTo.getParentFile().canWrite() || (saveTo.exists() && saveTo.canWrite()) && temp.canWrite()) {
			try (FileOutputStream fos = new FileOutputStream(temp);
					OutputStreamWriter out = new OutputStreamWriter(fos)) {
				// get original ordering from load
				if (original)
					Collections.sort(h.getMasterList(),
							new MetaImage.MetaImageComparator(MetaImage.SortOrder.UNSORTED, true, null, null));

				if (h.getMasterList().get(0) instanceof URLMetaImage)
					out.write("#" + ((URLMetaImage) h.getMasterList().get(0)).getBaseURL()+"\n");

				// write file
				for (MetaImage i : h.getMasterList())
					out.write(i + "\n");
				out.flush();
				out.close();

				// restore ordering from before saving
				if (original)
					h.sort();

				Files.copy(temp.toPath(), saveTo.toPath(), StandardCopyOption.REPLACE_EXISTING);
				Files.delete(temp.toPath());

				// remove autosaves of the same list file, they're now outdated
				File[] AutoSaveFiles = new File(ILB.TMP).listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File arg0, String arg1) {
						return arg1.startsWith(saveTo.getName() + "--");
					}
				});
				if (AutoSaveFiles != null)
					for (File f : AutoSaveFiles)
						f.delete();

				// Update list in title and memory if file saved under a new
				// name
				if (!saveTo.equals(h.getListFile()))
					h.setListFile(saveTo);

				h.getImageGroups().save();
				h.setMasterListChanged(false);

				return true;
			} catch (IOException e1) {
				System.err.println(temp.getAbsolutePath());
				e1.printStackTrace();
			}
		} else {
			String error = " Could not write in " + saveTo.getParent() + ".";
			JOptionPane.showMessageDialog(h.getImageDisplay(), error, "Permission Error", JOptionPane.YES_OPTION);
		}
		return false;
	}

	/**
	 * 
	 * @param extensions
	 *            valid file extensions (empty string for directories)
	 * @param mode
	 *            the mode to open the filechooser in
	 * @param dir
	 *            the start directory of the filechooser
	 * @param ff
	 *            a file filter for the file chooser
	 * @param dialog
	 *            a prompt for the user
	 * @param error
	 *            an error message if the user doens't select the right kind of
	 *            file
	 * @return a file chooser to load a specified type of file
	 */
	public static JFileChooser browser(String[] extensions, int mode, File dir, FileFilter ff, String dialog,
			String error) {
		// prepare filechooser for lst files JFileChooser fcPath = new
		JFileChooser result = new JFileChooser() {

			@Override
			public void approveSelection() {
				File f = getSelectedFile();
				String name = f.getName();
				for (String ending : extensions)
					if ((name.endsWith(ending) && f.exists()) || (f.isDirectory() && ending.equals(""))) {
						super.approveSelection();
						return;
					}
				JOptionPane.showMessageDialog(this, error);
				return;
			}
		};
		result.setCurrentDirectory(dir);
		result.setDialogTitle(dialog);
		result.setFileFilter(ff);
		result.setFileSelectionMode(mode);
		return result;

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		switch (evt.getProperty()) {
		case listFile:
			this.listClearer.setEnabled(evt.getNewValue() != null);
			break;
		case directory:
			this.clearer.setEnabled(evt.getNewValue() != null);
			break;
		case pattern:
			this.globClearer.setEnabled(evt.getNewValue() != null);
			break;
		case ascending:
		case displayArea:
		case loading:
			for (Action a : this.actions)
				if (a != this.reposOpener && a != this.listOpener && a != this.clearer && a != this.listClearer) {
					a.setEnabled(!this.handler.isLoading());
				}
			break;
		// TODO WHY DID THIS START BUGGING case masterSet:
		case mode:
		case ordering:
		case rotation:
		case thumbnailSize:
		case tally:
		default:
			break;
		}
	}

	/** @return this menu's Image Handler */
	ImageHandler getHandler() {
		return this.handler;
	}

}