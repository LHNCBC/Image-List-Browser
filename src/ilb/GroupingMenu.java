package ilb;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import fm.FaceMatchJavaInterface;
import struct.AnnotationGroups;
import struct.Group;
import struct.ImageGroups;
import struct.Property.PropertyChangeEvent;
import struct.Property.PropertyChangeListener;

/**
 * This menu handles the various ways images can be grouped and is used adjust
 * its ImageHandler's masterSet.
 * 
 * @author bonifantmc
 * 
 */
@SuppressWarnings("serial")
public class GroupingMenu extends JMenu implements PropertyChangeListener {

	/**
	 * Builds the Image Grouping menu for the ILB
	 * 
	 * @param h
	 *            the ImageHandler this menu will adjust the masterSet of
	 * @param g
	 *            group this menu is working with
	 */
	public GroupingMenu(ImageHandler h, Group g) {
		if (g instanceof ImageGroups)
			setText("Image Grouping");
		else
			setText("Annotation Grouping");

		h.addPropertyChangeListener(this);

		if (fm.FaceMatchJavaInterface.loaded)
			this.add(new NearDupSortMenu(g, h));
		this.add(new PrefixSortMenu(g, h));
		this.add(new GlobSortMenu(g, h));
		this.add(new RegexSortMenu(g, h));

		for (JMenuItem jmi : g.getMenuItems())
			this.add(jmi);

		setToolTipText("Group your images in various ways");
	}

	/**
	 * 
	 * @param arg0
	 *            the change ImageHandler's state that requires this to adjust
	 *            its display in some way
	 */
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * Sort Group based on regular expressions.
	 * 
	 * @author bonifantmc
	 *
	 */
	private class RegexSortMenu extends JMenuItem {
		/**
		 * @param g
		 *            the group to apply regex sorting to
		 * @param h
		 *            the image handler to get images from
		 * 
		 */
		public RegexSortMenu(Group g, ImageHandler h) {
			super("Sort Images by a Regex Pattern");
			this.setToolTipText("Organize all images, matching their file names to the given regex.");
			this.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					String regex = Group.getValidRegex(h.getImageDisplay());
					if (regex == null)
						return;
					g.regexSort(regex);
					setMode(g, h);
				}

			});
		}
	}

	/**
	 * Sort the Group based on globs
	 * 
	 * @author bonifantmc
	 *
	 */
	private class GlobSortMenu extends JMenuItem {
		/**
		 * @param g
		 *            the group to apply glob sorting to
		 * @param h
		 *            the image handler to get images from
		 */
		public GlobSortMenu(Group g, ImageHandler h) {
			super("Sort Images by a Glob Pattern");
			this.setToolTipText("Organize all images, matching their file names to the given glob.");
			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {

					String glob = Group.getValidGlob(h.getImageDisplay());
					if (glob == null)
						return;
					g.globSort(glob);
					setMode(g, h);

				}
			});

		}
	}

	/** sort by prefix */
	private class PrefixSortMenu extends JMenuItem {
		/**
		 * @param g
		 *            the group to apply prefix sorting to
		 * @param h
		 *            the image handler to get images from
		 */
		public PrefixSortMenu(Group g, ImageHandler h) {
			super("Group Images by Prefix Length");
			this.setToolTipText(
					"Organize each image by putting all images with the same prefix in a group, based on a given prefix length.");
			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					int length = Group.getValidPrefix(h.getImageDisplay());
					g.prefixSort(length);
					setMode(g, h);

				}
			});

		}

	}

	/**
	 * 
	 * @author bonifantmc
	 *
	 */
	private class NearDupSortMenu extends JMenuItem {
		/** missing list error */
		final String errorLis = "Please designate a list file and try again.";
		/** missing directory error */
		final String errorDir = "Please designate a directory and try again.";
		/** missing directory and list error */
		final String errorDirLis = "Please designate a directory and list file, then try again";

		/**
		 * @param g
		 *            the group to apply neardup sorting to
		 * @param h
		 *            the image handler to get images from
		 */

		public NearDupSortMenu(Group g, ImageHandler h) {
			super("Near Duplicate Image Detection");
			this.setToolTipText("Organize each image by grouping it with like images via FaceMatch Library.");
			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					h.setLoading(true);
					setMode(g, h);
					SwingWorker<String, String> worker = new SwingWorker<String, String>() {
						@Override
						protected String doInBackground() {
							File r = h.getDirectory();
							File l = h.getListFile();
							if (r == null && l == null)
								return NearDupSortMenu.this.errorDirLis;
							if (l == null)
								return NearDupSortMenu.this.errorLis;
							if (r == null)
								return NearDupSortMenu.this.errorDir;
							return FaceMatchJavaInterface.nearDup(h.getDirectory() + File.separator,
									h.getListFile().getAbsolutePath());
						}
					};
					worker.execute();
					String dups = null;
					try {
						dups = worker.get();
					} catch (InterruptedException | ExecutionException except) {
						except.printStackTrace();
						JOptionPane.showMessageDialog(h.getImageDisplay(),
								"Error running NearDupImageDetector.\n"
										+ "Please ensure that the FaceMatchJavaInterface library is on this program's path\n"
										+ "(somewhere on the user/system path or in the same directory as this program).\n"
										+ "If the problem persists, contact FaceMatch@NIH.gov",
								"Problem loading/running NearDupImageDetector library", JOptionPane.ERROR_MESSAGE);
					}

					if (dups == null || dups.equals(NearDupSortMenu.this.errorDirLis)
							|| dups.equals(NearDupSortMenu.this.errorLis)
							|| dups.equals(NearDupSortMenu.this.errorDir)) {
						JOptionPane.showMessageDialog(h.getImageDisplay(), dups, "Could not call FaceMatch.",
								JOptionPane.ERROR_MESSAGE);
					} else {
						g.nearDupSort(dups);
					}

					h.setLoading(false);
				}

			});
		}

	}

	// * Call FaceMatch's ImageMatching code
	// *
	// * @author bonifantmc
	// *
	// */
	// private class FaceMatch extends AbstractAction {
	// /**
	// * Set the name to appear in the View menu and the tooltip to go with
	// * it.
	// */
	// public FaceMatch() {
	// putValue(NAME, "Group by FaceMatch");
	// putValue(SHORT_DESCRIPTION,
	// "Organize each image matching by a FaceMatch Library method.");
	// }
	//
	// @Override
	// public void actionPerformed(ActionEvent arg0) {
	// FaceMatchImageMatchForm.getFaceMatchParams(getHandler(), true);
	// }
	// }

	//
	// /**
	// * Action command to call fm.FaceMatchImageMatchForm.ingestDialog in order
	// * to run an ingest on the images for this the current listfile associated
	// * with the image handler
	// *
	// * @author bonifantmc
	// *
	// */
	// private class Ingest extends AbstractAction {
	// /** set label and tool tip */
	// Ingest() {
	// putValue(NAME, "Ingest");
	// putValue(SHORT_DESCRIPTION, "Ingest images for FaceMatch.");
	// }
	//
	// @Override
	// public void actionPerformed(ActionEvent e) {
	// fm.FaceMatchImageMatchForm.ingestDialog(getHandler());
	// }
	// }

	/***
	 * Set the Mode of the ImageListBrowser to ensure the proper grouping view
	 * is open when the user selects a grouping function
	 * 
	 * @param g
	 *            the group the function was looking at
	 * @param h
	 *            the imagehandler that can notify ILB at large of the change in
	 *            Mode
	 */
	private void setMode(Group g, ImageHandler h) {
		if (g instanceof ImageGroups && h.getMode() != Mode.GROUPING_IMAGES)
			h.setMode(Mode.GROUPING_IMAGES);
		else if (g instanceof AnnotationGroups && h.getMode() != Mode.GROUPING_ANNOTATIONS)
			h.setMode(Mode.GROUPING_ANNOTATIONS);

	}
}
