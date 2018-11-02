package fm;

import java.awt.Component;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import annotations.Annotation;
import ilb.FileMenu;
import ilb.ImageHandler;
import image.editing.EditImage;
import struct.ImageAnnotationPair;
import struct.MetaImage;
import struct.NLMSThumbnails;

/**
 * Background Thread for calling FaceMatch methods
 * 
 * @author bonifantmc
 *
 */
public class FaceMatchWorker extends SwingWorker<Object, Object> implements Comparable<FaceMatchWorker> {
	/** All FaceMatchWorkers still working are stored here */
	public static ThreadPoolExecutor workers = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new WorkerBlockingQueue());
	/** Toggle on if grouping images */
	public static final int GROUP = 1;
	/** Toggle on if ingesting images */
	public static final int INGEST = 1 << 1;
	/** Toggle on if just getting distances/rank for one image */
	public static final int SEARCH = 1 << 2;
	/** Toggle on if thread is in background */
	public static final int BACKGROUND = 1 << 3;
	/** Toggle on if thread is locating faces */
	public static final int FACEFIND = 1 << 4;

	/**
	 * boolean to tell if FaceMatch is ingesting images, must block all
	 * ImageMatching until ingests are finished
	 */
	public static boolean ingesting = false;
	/** Progress bar to display when worker needs to pause main UI thread */
	private JProgressBar prog = new JProgressBar();
	/**
	 * store components of foreground here if progress bar needs to be displayed
	 */
	private Component[] p;
	/** ImageHandler to access images and display */
	private ImageHandler h;
	/** Input commands for method being called */
	Object[] in;

	/** list of flags for what to or not to do */
	int flags = 0;

	/** number of updates to UI made by calls of process method */
	int updates = 0;

	/** Processing flags to pass to FaceMatch classes */
	int procFlags = 0 >> 32;
	/** Object array of strings listing image names/paths to use */
	Object[] images;

	/**
	 * 
	 * @param h
	 *            ImageHandler to supply needed data
	 * @param in
	 *            an object array of inputs to pass to FaceMatch
	 * @param flags
	 *            flags defining what methods are being called and if the
	 *            progress bar should be displayed or not
	 * @param procFlags
	 *            processing flags for FaceMatch
	 * @param images
	 *            list of images for FaceMatch to run on
	 */
	public FaceMatchWorker(ImageHandler h, Object[] in, int flags, int procFlags, Object... images) {

		// if (procFlags == 0)
		// new IllegalArgumentException().printStackTrace();
		this.h = h;
		this.flags = flags;
		this.updates = 0;
		this.in = in;
		this.procFlags = procFlags;
		this.images = images;

	}

	@Override
	protected Object doInBackground() {
		// wait for previous ingests to complete
		while (isIngesting()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// doesn't matter so long as command continues to sleep and loop
				// until ingest completes
			}
		}

		// set to ingest as soon as possible if worker is ingesting
		ingesting = isIngest();
		if (!isBackground())
			publish("background");

		// execute the given command and return its result
		if (isIngest()) {
			return fm.FaceMatchJavaInterface.ingest(Arrays.copyOfRange(this.in, 0, this.in.length - 1),
					(Object[]) this.in[this.in.length - 1], this);

		} else if (isGroup() || isSearch()) {
			try {
				String arr0 = (String) this.in[0], arr1 = (String) this.in[1], arr2 = (String) this.in[2],
						arr3 = (String) this.in[3];
				int ff = (int) this.in[4];
				double d = (double) this.in[5];
				float t = (float) d;
				Object o = this;
				String ret = fm.FaceMatchJavaInterface.imageMatch(arr0, arr1, arr2, arr3, ff, t, o);
				return ret;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (isFaceFind()) {
			FaceMatchJavaInterface.faceFind(this.procFlags, this.in, this.images, this);
		} else {
			System.out.println("NOTHING");
		}
		return null;
	}

	@Override
	protected final void process(List<Object> arr) {
		// iterate through published results
		for (Object o : arr) {
			// they all should be strings, obscured as objects
			if (o instanceof String) {
				String str = (String) o;

				// handle default commands for clearing tables and setting up
				// progress bar
				if (str.equals("clear")) {
					this.h.getAnnotationGroups().clear();
				} else if (str.equals("background")) {
					processBackgroundLoading();
				} else if ((this.flags & (GROUP | SEARCH)) != 0) {
					this.h.getAnnotationGroups().parseImageMatcherResult(str);
				} else if (isFaceFind()) {
					processFaceFind(str);
				} else {
					System.out.println("PROCESSING OTHER: " + str);
				}
			} else {
				System.err.println("ERROR could not process object: " + o);
			}

			// update progress bar
			if (!isBackground()) {
				this.updates++;
				this.prog.setValue(this.updates);
			}
		}
	}

	/**
	 * Prep the progress bar at the start of background loading.
	 */
	private void processBackgroundLoading() {
		this.h.setLoading(true);
		this.prog.setMaximum(
				this.h.getMasterList().size() * (isIngest() ? ((Object[]) this.in[this.in.length - 1]).length : 1) + 1);
		this.prog.setStringPainted(true);
		this.prog.setFont(new Font(this.prog.getFont().getName(), Font.PLAIN, 100));
		this.p = this.h.getImageDisplay().getViewport().getComponents();
		this.h.getImageDisplay().getViewport().removeAll();
		this.h.getImageDisplay().getViewport().add(this.prog);
		// handle job specific responses
	}

	/**
	 * Process the result of a call to FaceFinder by extracting the image name
	 * from the resulting string and then parsing the Annotations found for that
	 * image (if any) and add them to its MetaImage's Annotation List
	 * 
	 * @param str
	 *            the String returned from the FaceFinder call.
	 */
	private void processFaceFind(String str) {
		this.h.getProgressBar().message.setText("Faces Found: " + str);
		MetaImage i = this.h.getMasterList().getByName(str.split("\t")[0]);

		if (i != null) {
			Annotation.addAllNonDuplicates(i.getAnnotations(), Annotation.parseAnnotationList(str));
			for (EditImage ei : this.h.getEditImages()) {
				ei.repaint();
				ei.snap();
			}
			h.getImageDisplay().repaint();
		}
	}

	@Override
	protected final void done() {
		super.done();
		// wait till all processes done
		if (!isBackground()) {
			this.h.setLoading(false);
			this.h.getImageDisplay().getViewport().removeAll();
			for (Component c : this.p)
				this.h.getImageDisplay().getViewport().add(c);
		}

		if ((this.flags & GROUP) != 0) {
			this.h.getAnnotationGroups().buildFromRankings();
			this.h.setTick(0);
		}

		if ((this.flags & SEARCH) != 0) {
			this.h.setOrdering(MetaImage.SortOrder.IMAGE_RANKING);
			this.h.setAscending(true);
			this.h.sort();
			this.h.reindexMaster();
			this.h.setTick(0);
		}
		if ((this.flags & INGEST) != 0) {
			synchronized (this.h) {
				ingesting = false;
			}
		}
		if (isFaceFind())
			this.h.getProgressBar().message.setText("Done searching for faces.");
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				FaceMatchWorker.this.h.getProgressBar().message.setText("");
				FaceMatchWorker.this.h.getProgressBar().prog.setVisible(false);
			}
		});
	}

	/** @return true if this worker is running in the background */
	boolean isBackground() {
		return (this.flags & BACKGROUND) != 0;
	}

	/** @return true if this worker is INGESTing images to indices */
	boolean isIngest() {
		return (this.flags & INGEST) != 0;
	}

	/** @return true if this work is SEACHing images */
	boolean isSearch() {
		return (this.flags & SEARCH) != 0;
	}

	/** @return true if this work is GROUPing images */
	boolean isGroup() {
		return (this.flags & GROUP) != 0;
	}

	/** @return true if this work is FACEFINDing images */
	boolean isFaceFind() {
		return (this.flags & FACEFIND) != 0;
	}

	/**
	 * INGEST the given list of images into the index files
	 * 
	 * @param h
	 *            image handler to determine image repository and list file
	 * @param objects
	 *            list of features to ingest images for
	 */
	public static void ingest(ImageHandler h, Object[] objects) {
		if (h.getListFile() == null) {
			// force user to save
			FileMenu.getNewSaveSpot(h);
			// user canceled
			if (h.getListFile() == null) {
				ingesting = false;
				return;
			}
			FileMenu.save(h, false);
		}

		// evaluate command
		Object[] ev = new Object[] { "-p", h.getDirectory().getAbsolutePath() + File.separator, "-lst",
				h.getListFile().getAbsolutePath(), "-ndx", "Files/" + h.getListFile().getName(), "-t", "-1", objects };
		FaceMatchWorker.workers.execute(new FaceMatchWorker(h, ev, INGEST, 0 >> 32));
	}

	/**
	 * Parse nearDup()'s response, and builds the ImageHandler's master set
	 * according to it.
	 * 
	 * @param result
	 *            response of nearDup()
	 * @param h
	 *            ImageHandler that will store the near duplicate sets
	 */
	public static void parseDuplicatesIntoGroups(String result, ImageHandler h) {
		// each line in the result is a single group
		String[] groups = result.split("\n");
		// need to remove old grouping.
		h.getAnnotationGroups().clear();

		// build each group
		for (String group : groups) {
			// write output to file
			String[] groupMembers = group.split("\t");

			// gather near duplicates
			NLMSThumbnails row = new NLMSThumbnails();
			for (String member : groupMembers) {
				MetaImage im = h.getMasterList().getByName(member);
				row.add(new ImageAnnotationPair(im, null));
			}
			h.getImageGroups().map.put(row.getName(), row);
		}
		h.sort();
	}

	/**
	 * Call FaceMatch's FaceFinder to search any number of MetaImage's images
	 * for faces and if found, add those annotations to the MetaImage.
	 * 
	 * @param h
	 *            the handler that holds images
	 * @param i
	 *            the images to find faces for
	 */
	public static void getFaces(final ImageHandler h, final MetaImage... i) {
		if (fm.FaceMatchJavaInterface.loaded) {
			h.getProgressBar().message.setText("Searching for Faces");

			// get list of images to process
			Object[] images = new Object[i.length];
			for (int idx = 0; idx < i.length; idx++)
				images[idx] = i[idx].getName();

			// construct string inputs
			List<Object> objs = new ArrayList<>();
			objs.addAll(Arrays.asList(new Object[] { "-p", h.getDirectory().getAbsolutePath() + File.separator }));
			// objs.addAll(Arrays.asList(new Object[] { "-s:in
			// NET_PL_9_15_50_90.18.ann", "-GPU" }));
			objs.addAll(Arrays.asList(new Object[] { "-skin" })); 
			// TODO: config/params more

			h.getProgressBar().message.setText("Searching for Faces");
			workers.execute(
					new FaceMatchWorker(h, objs.toArray(), FACEFIND | BACKGROUND, FaceFinder.getProcFlags(), images));

		} else // if not loaded
		{
			h.getProgressBar().message.setText("FaceMatch Library not loaded");
		}
	}

	/**
	 * @author bonifantmc A Blocking queue to organize when threads run. Ingests
	 *         should complete before searching/grouping with FaceMatch, but
	 *         NearDup doesn't need an ingest so it should call sooner, as
	 *         should FaceId'ing
	 */
	@SuppressWarnings("serial")
	public static class WorkerBlockingQueue extends PriorityBlockingQueue<Runnable> {
		/**
		 * Add new threads and update old ones.
		 */
		@Override
		public boolean offer(Runnable r) {
			while (contains(r))
				remove(r);
			return super.offer(r);
		}
	}

	/**
	 * Call FaceMatch and check image similarity against the given
	 * image/annotation pairing for the entire list. Order all images in
	 * comparison to it.
	 * 
	 * @param i
	 *            the image to order all images against in terms of similarity
	 * @param annotation
	 *            the annotation paired with this image
	 * @param h
	 *            reference to handler to get display info
	 */
	public static void searchByImage(MetaImage i, Annotation annotation, ImageHandler h) {
		ImageHandler.masterImage = new ImageAnnotationPair(i, annotation);

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File("Files/query.lst")))) {
			bw.write(i.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

		FaceMatchImageMatchForm.getFaceMatchParams(h, false);
	}

	// ingests always take precedence
	@Override
	public int compareTo(FaceMatchWorker arg0) {
		if (this.isIngest() && arg0.isIngest())
			return 0;
		else if (this.isIngest())
			return -1;
		else if (arg0.isIngest())
			return 1;
		else
			return 0;
	}

	/**
	 * 
	 * @return returns the boolean ingesting to indicate true if ingesting and
	 *         false if not
	 */
	public synchronized static boolean isIngesting() {
		return ingesting;

	}

	/**
	 * organize images in the Image Handler based on a selected annotation from
	 * the given image
	 * 
	 * @param i
	 *            the image to pick an annotation from
	 * @param h2
	 *            the handler to organize images for
	 */
	public static void searchByAnnotation(MetaImage i, ImageHandler h2) {
		if (i.getAnnotations().size() == 0)
			h2.getProgressBar().message.setText("Image has no annotations to sort by.");
		else if (i.getAnnotations().size() == 1) {
		}
		// TODO search by the only annotation
		else {
		} // TODO select an annotation and then sort by it
	}

	/**
	 * Organize the images based on the selected image
	 * 
	 * @param i
	 *            the image to search by
	 * @param h2
	 *            the handler to organize the images for
	 */
	public static void searchByImage(MetaImage i, ImageHandler h2) {
		// TODO search by the image itself alone

	}

}
