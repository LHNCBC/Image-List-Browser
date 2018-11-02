package ilb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import annotations.Annotation;
import ilb.ViewMenu.ExtraListMenu.AddList;
import struct.ArrayListModel;
import struct.ImageGroups;
import struct.ImageMap;
import struct.MetaImage;
import struct.Pair;
import struct.URLMetaImage;

/**
 * Runs a background thread to read all images in a given directory and
 * listfile. While this runs its ImageHandler is set to loading and the
 * Handler's image display is replaced with a progress bar
 * <p>
 * This thread only loads meta data about images, to actually load information
 * needed to display the image see the ImageHandler's ImageMap.
 * 
 * @author bonifantmc
 * @see MetaImage
 * @see ImageHandler
 * @see ImageMap
 */
public class ListReader extends SwingWorker<ArrayList<MetaImage>, Void> {
	/**
	 * Only one ListReader should ever be running at one time, and its whichever
	 * reader is stored here in r.
	 */
	public static ListReader r;

	/** number of records to process before updating screen */
	private static final int UPDATE_RATE = 250;
	/** The ImageHandler requesting images to be loaded */
	private final ImageHandler handler;
	/** The list file images are read from */
	private final File readList;
	/** The root directory to look for images from */
	private final File readDir;
	/** A pattern to mask images with and filter */
	private final String mask;
	/**
	 * The new master list for the ImageHandler launching this thread as it is
	 * being built
	 */
	private final ArrayListModel<MetaImage> result = new ArrayListModel<>();

	/**
	 * Prepares the worker before it is launched.
	 * 
	 * @param h
	 *            the ImageHandler making a request for new images.
	 */
	ListReader(ImageHandler h) {
		if (r != null) {
			r.cancel(true);
			while (!r.isDone()) {/*
									 * TODO See if there's any better way to
									 * wait, look at CountDownLatch.
									 */
			}
		}
		r = this;
		this.handler = h;
		this.readList = getHandler().getListFile();
		this.readDir = getHandler().getDirectory();
		if (getHandler().getPattern() == null)
			this.mask = "*";
		else
			this.mask = getHandler().getPattern();
		getHandler().setLoading(true);
		getHandler().getProgressBar().prog.setStringPainted(true);
	}

	/**
	 * Loads all images from a directory (but not its sub-directories or all
	 * images indicated from a list file (provided they are contained within a
	 * given directory & its sub-directories are indicated in the list)
	 * 
	 * @see javax.swing.SwingWorker#doInBackground()
	 */
	@Override
	protected ArrayListModel<MetaImage> doInBackground() {

		if (getReadList() == null)
			try {
				loadFromRepository();
			} catch (NullPointerException e) {
				getHandler().getProgressBar().message.setText("ILB could not find the requested repository");
			}
		else if (getReadDir() == null) {
			loadFromURLs();
		} else {
			loadFromListFile();
		}
		return getResult();
	}

	/***
	 * Load a list file of images found online with URLs.
	 */
	private void loadFromURLs() {
		int index = 0;
		int siz = 0;
		String line;

		// check for autosaves of the list file
		List<File> oldSaves = Arrays.asList(new File(ILB.TMP).listFiles(new FileFilter() {
			@Override
			public boolean accept(File arg0) {
				return arg0.getName().startsWith(getReadList().getName());
			}
		}));
		File list = checkForAutosave(oldSaves);

		// if no autosaves selected, use default list
		if (list == null)
			list = getReadList();

		String baseURL = "";

		// quick count of how many images to load
		try (BufferedReader br = new BufferedReader(new FileReader(list))) {
			while ((line = br.readLine()) != null)
				if (line.startsWith("#")) {
					baseURL = line.substring(1).trim();
				} else
					siz++;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// use count to display progress
		getHandler().getProgressBar().prog.setMaximum(siz);
		getHandler().getProgressBar().prog.setValue(0);

		// read the images
		try (BufferedReader br = new BufferedReader(new FileReader(list))) {
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				if (index % UPDATE_RATE == 0)
					publish();
				String name = line.split("[\t]")[0];
				if (MetaImage.IMAGE_FILTER.accept(null, name)) {
					MetaImage toAdd = new URLMetaImage(baseURL, name, index++, line, 0);
					getResult().add(toAdd);

				}
				if (isCancelled())
					return;
			}

			// getHandler().getMasterSet().build();
			index = readTimeStamps(index);
			ImageGroups.load(getResult(), list.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// TODO Auto-generated method stub

	}

	/**
	 * Loads all images from repository/directory when no list file is provided.
	 * Only looks at the given directory, not sub-directories thereof.
	 * 
	 */
	private void loadFromRepository() {
		int index = 0;
		// try with a stream of all files matching the glob in the
		// repository

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(getReadDir().toPath(), this.getMask())) {
			getHandler().getProgressBar().prog.setMaximum(getReadDir().listFiles().length);
			getHandler().getProgressBar().prog.setValue(0);
			Iterator<Path> itr = stream.iterator();
			while (itr.hasNext()) {
				if (index % UPDATE_RATE == 0)
					publish();
				File f = itr.next().toFile();
				if (MetaImage.IMAGE_FILTER.accept(f, f.getName()))
					getResult().add(new MetaImage(f, index++));

				if (isCancelled())
					return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads all images from a given list file, then check for configuration
	 * information about the files, checking for a map to build the
	 * ImageHandler's master set and setting the previous order of images along
	 * with previous access dates to each image
	 */
	private void loadFromListFile() {
		int index = 0;
		int siz = 0;
		String line;

		// check for autosaves of the list file
		List<File> oldSaves = Arrays.asList(new File(ILB.TMP).listFiles(new FileFilter() {
			@Override
			public boolean accept(File arg0) {
				return arg0.getName().startsWith(getReadList().getName());
			}
		}));
		File list = checkForAutosave(oldSaves);

		// if no autosaves selected, use default list
		if (list == null)
			list = getReadList();

		// quick count of how many images to load
		try (BufferedReader br = new BufferedReader(new FileReader(list))) {
			while ((line = br.readLine()) != null)
				siz++;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// use count to display progress
		getHandler().getProgressBar().prog.setMaximum(siz);
		getHandler().getProgressBar().prog.setValue(0);

		// read the images
		try (BufferedReader br = new BufferedReader(new FileReader(list))) {
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.startsWith("#"))
					continue;
				if (index % UPDATE_RATE == 0)
					publish();
				String name = line.split("[\t]")[0];
				File f = new File(getReadDir(), name);
				if (MetaImage.IMAGE_FILTER.accept(f, f.getName())) {
					MetaImage toAdd = new MetaImage(f, index++, line, 0);
					getResult().add(toAdd);
				}
				if (isCancelled())
					return;
			}
			// getHandler().getMasterSet().build();
			index = readTimeStamps(index);
			ImageGroups.load(getResult(), list.getName());

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @param oldSaves
	 *            a list of autosaved files
	 * @return the most lastest autosave, if one is desired
	 */
	private File checkForAutosave(List<File> oldSaves) {
		if (oldSaves.size() != 0) {
			int response = JOptionPane.showConfirmDialog(getHandler().getImageDisplay(),
					"Autosaves for this file were found, do you want to load that instead?");
			if (response == JOptionPane.OK_OPTION) {
				// sort the files from most recent to least recent
				Collections.sort(oldSaves, new Comparator<File>() {

					@Override
					public int compare(File arg1, File arg2) {

						// get the time stamp from each file
						String t1 = arg1.getName().split("(--)|(\\.lst)|(\\.txt)")[1],
								t2 = arg2.getName().split("(--)|(\\.lst)|(\\.txt)")[1];
						// parse the stamps into their time (ms since the epoch
						// 1970)
						long time1, time2;
						try {
							time1 = ILB.TIME_FORMATTER.parse(t1).getTime();
							time2 = ILB.TIME_FORMATTER.parse(t2).getTime();
						} catch (ParseException e) {
							// e.printStackTrace();
							return -1;
						}

						// the greater time is presumably closer to the present
						// (unless someone trolled the autosaves and added some
						// more recent or in the future)
						return Long.compare(time1, time2);
					}
				});
				return oldSaves.get(0);
			}
		}
		return null;
	}

	/**
	 * 
	 * @param index
	 *            the steps completed before reading timestamps file, for
	 *            tracking progress
	 * @return steps completed after reading map file
	 */
	@SuppressWarnings("boxing")
	public int readTimeStamps(int index) {
		// read timestamps to reset previous list order and access dates
		File timeFile = new File("timestamps" + File.separator + getReadList().getName());
		HashMap<String, Pair<Long, Integer>> dateMap = new HashMap<>();
		if (timeFile.exists())
			try (BufferedReader tr = new BufferedReader(new FileReader(timeFile))) {
				String time;
				Long value;
				String key;
				int cnt = 0;
				while ((time = tr.readLine()) != null) {
					value = Long.valueOf(time.split("[\t]")[0]);
					key = time.split("[\t]", 2)[1];
					dateMap.put(key, new Pair<>(value, cnt++));
				}
				for (MetaImage i : getResult()) {
					if (dateMap.containsKey(i.toString())) {
						i.setDate(dateMap.get(i.toString()).x);
						i.setIndexCur(dateMap.get(i.toString()).y);
						i.setIndexAlt(i.getIndexCur());
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		return index;
	}

	@SuppressWarnings("boxing")
	@Override
	protected final void process(List<Void> a) {
		if (isCancelled()) {
			return;
		}
		getHandler().setMasterListPartial(getResult());

		getHandler().getProgressBar().prog.setValue(getHandler().getMasterList().size());
		getHandler().getProgressBar().message.setText("Loaded " + getHandler().getProgressBar().prog.getValue() + " of "
				+ getHandler().getProgressBar().prog.getMaximum() + " images.");
		getHandler().getProgressBar().prog.setVisible(true);
	}

	@Override
	protected void done() {
		long t = System.currentTimeMillis();
		final JProgressBar prog = getHandler().getProgressBar().prog;
		final JLabel message = getHandler().getProgressBar().message;
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				prog.setVisible(false);
				message.setText("");

			}
		});

		if (this.isCancelled()) {
			getHandler().setMasterList(new ArrayListModel<MetaImage>());
			prog.setValue(0);
			message.setText("Cancelled loading.");
			getHandler().setLoading(false);
			super.done();

			return;
		}
		prog.setValue(prog.getMaximum());
		message.setText("Loaded " + prog.getValue() + " of " + prog.getMaximum() + " images.");

		// set new master list
		getHandler().setMasterList(getResult());
		getHandler().reindex(true);
		for (MetaImage i : getHandler().getMasterList())
			i.launchFileMetaDataLoad();
		getHandler().setLoading(false);
		super.done();

		sanitizeAutosaves();
	}

	/***
	 * Delete unneeded autosaves
	 */
	private void sanitizeAutosaves() {
		// Sanitize the old autosaves for this list since the user loaded a new
		// list ignoring them.
		if (getReadList() != null) {
			File[] list = new File(ILB.TMP).listFiles(new FileFilter() {
				@Override
				public boolean accept(File arg0) {
					return arg0.getName().startsWith(getReadList().getName());
				}
			});

			if (list != null)
				for (File f : list)
					f.delete();
		}

	}

	/** @return the ImageHandler requesting new Images */
	private ImageHandler getHandler() {
		return this.handler;
	}

	/** @return the pattern mask for filtering images */
	private String getMask() {
		return this.mask;
	}

	/**
	 * @return the .lst file that contains a list of which images to display
	 *         along with any annotations about them
	 */
	private File getReadList() {
		return this.readList;
	}

	/** @return the directory from which the reader looks for images */
	private File getReadDir() {
		return this.readDir;
	}

	/** @return the images that have been loaded so far */
	private ArrayListModel<MetaImage> getResult() {
		return this.result;
	}

	/**
	 * @param h
	 *            the handler giving access to the images
	 * @param newList
	 *            the file containing the additional annotations
	 * @param newlistidx
	 *            the index of the new list in the extra lists List.
	 * @param addList
	 *            the menu that should be disabled while the worker is running.
	 */
	public static void loadExtraList(ImageHandler h, File newList, int newlistidx, AddList addList) {
		SwingWorker<Void, Integer> newWorker = new SwingWorker<Void, Integer>() {
			@Override
			protected Void doInBackground() throws Exception {
				try (BufferedReader br = new BufferedReader(new FileReader(newList))) {
					for (MetaImage i : h.getMasterList())
						i.getAlternativeAnnotations().add(new ArrayList<>());
					int cnt = 0;
					int percent = h.getMasterList().getSize() / 100;
					if (percent <= 0)
						percent = 1;
					String line;

					while ((line = br.readLine()) != null) {
						String name = line.split("[\t]")[0];
						MetaImage image = h.getMasterList().getByName(name);
						if (image != null)
							image.getAlternativeAnnotations().get(newlistidx)
									.addAll(Annotation.parseAnnotationList(line));
	
						if (cnt % percent == 0) {
							publish(cnt);
						} 
						cnt++;
					}

				}
				return null;
			}

			@SuppressWarnings("boxing")
			@Override
			protected final void process(List<Integer> a) {
				for (Integer i : a) {
					h.getProgressBar().prog.setValue(i);
					h.getProgressBar().message.setText("Loaded " + h.getProgressBar().prog.getValue() + " of "
							+ h.getProgressBar().prog.getMaximum() + " images.");
					h.getProgressBar().prog.setVisible(true);
				}
			}

			@Override
			protected void done() {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						h.getProgressBar().prog.setVisible(false);
						h.getProgressBar().message.setText("");
						addList.setEnabled(true);
					}
				});
			}
		};
		addList.setEnabled(false);
		newWorker.execute();

	}
}
