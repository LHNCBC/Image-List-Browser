package ilb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import annotations.Annotation;
import fm.FaceFinder;
import fm.FaceMatchJavaInterface;
import image.editing.FaceMask;
import struct.MetaImage;

/**
 * Configurations provides static methods for obtaining and saving user
 * preferences as well as modification time and image groupings at closing of
 * the ILB (Image List Browser)
 * 
 * @author bonifantmc
 * 
 */
public class Configurations {
	/** default starting width of an ILB */
	public static final int dw = 700;

	/** default starting height of an ILB */
	public static final int dh = 700;

	/** default starting x coord of an ILB */
	public static final int dx = 0;

	/** default starting y coord of an ILB */
	public static final int dy = 0;

	/** default position of scroll bar for ImageHandler */
	public static final int dtick = 0;

	/** default size of thumbnails for ImageHandler */
	public static final int dsize = 150;

	/** default list file absolute path for ImageHandler */
	public static final String dl = null;

	/** default directory absolute path for ImageHandler */
	public static final String dr = null;

	/** default application mode for ImageHandler */
	public static final Mode dm = Mode.GRID;

	/** default application sorting order for ImageHandler */
	public static final MetaImage.SortOrder dO = MetaImage.SortOrder.UNSORTED;

	/** default application sorting direction for ImageHandler */
	public static final boolean da = true;

	/**
	 * {@link annotations.Annotation#min_diff} if a user defined min_diff isn't
	 * found use the default
	 */
	public static final float annotation_min_diff = 0.8f;

	/**
	 * Loads the initial location and size of the ImageListBrowser and sets
	 * them.
	 * 
	 * @param iLB
	 *            the browser whose fields will be set
	 * @param fileName
	 *            name of the configuration file
	 */
	public static void getDisplayProperties(ILB iLB, String fileName) {
		Properties prop = new Properties();
		File f = new File(fileName);
		// defaults
		int w = dw, h = dh, x = dx, y = dy;

		if (f.exists()) {
			try (FileInputStream input = new FileInputStream(f)) {
				prop.load(input);
				try {
					w = Integer.parseInt(prop.getProperty("windowwidth", "700"));
				} catch (NumberFormatException e) {
					w = dw;
				}

				try {
					h = Integer.parseInt(prop.getProperty("windowheight", "700"));
				} catch (NumberFormatException e) {
					h = dh;
				}

				try {
					x = Double.valueOf((prop.getProperty("positionX", "0"))).intValue();
				} catch (NumberFormatException e) {
					x = dx;
				}

				try {
					y = Double.valueOf((prop.getProperty("positionY", "0"))).intValue();

				} catch (NumberFormatException e) {
					y = dy;
				}
				float scale = Float.valueOf(prop.getProperty("FaceMaskScale", Float.toString(FaceMask.defaultScale)));
				float translucency = Float.valueOf(
						prop.getProperty("FaceMaskTranslucency", Float.toString(FaceMask.defaultTranslucency)));
				FaceMask.scale.set(scale);
				FaceMask.translucency.set(translucency);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// check for negative values, someone could hardcode it into the file,
		// and if a window is closed while minimized (at least in windows) the
		// position is then given as a negative
		if (w < 0)
			w = dw;
		if (h < 0)
			h = dh;
		if (x < 0)
			x = dx;
		if (y < 0)
			y = dy;
		iLB.setSize(w, h);
		iLB.setLocation(x, y);
		if (FaceMatchJavaInterface.loaded)
			FaceFinder.ProcFlag.getProperties(prop);

	}

	/**
	 * Loads the initial configurations for the ImageHandler, ie: image
	 * repository/directory, list file, etc.
	 * 
	 * @param h
	 *            the handler whose values will be set
	 * @param fileName
	 *            the name of the file that contains the config info
	 */
	public static void getHandlerProperties(ImageHandler h, String fileName) {
		Properties prop = new Properties();
		File f = new File(fileName);

		String l = dl, r = dr;
		int size = dsize, tick = dtick;
		Mode m = dm;
		boolean a = da;
		MetaImage.SortOrder o = dO;
		float min_diff = annotation_min_diff;
		if (f.exists())
			try (FileInputStream input = new FileInputStream(f)) {
				prop.load(input);

				// load repository and list names
				r = prop.getProperty("repository", null);
				l = prop.getProperty("list", null);
				if (r.equals("NULL"))
					r = null;
				if (l.equals("NULL"))
					l = null;

				// get the ImageHandler's thumbnailSize
				try {
					size = Integer.parseInt(prop.getProperty("size"));
				} catch (NumberFormatException e) {
					size = dsize;
				}

				// get where along the list the user was last at
				try {
					tick = Integer.parseInt(prop.getProperty("scrolltick", "0"));
				} catch (NumberFormatException e) {
					tick = dtick;
				}

				// find what mode the application should be in, grid, list, or
				// grouping
				m = Mode.parseMode(prop.getProperty("displaySetting", Mode.GRID.toString()));

				// find the order to sort images by
				o = MetaImage.SortOrder.parseSortOrder(prop.getProperty("sortOrder", "Unsorted"));
				a = Boolean.parseBoolean(prop.getProperty("ascending", "true"));
				min_diff = Float.parseFloat(prop.getProperty("annotation#min_diff", "0.8"));

			} catch (Exception e) {
				e.printStackTrace();
			}
		h.setDirectory(r == null ? null : new File(r));
		h.setListFile(l == null ? null : new File(l));
		h.setThumbnailSize(size);
		h.setTick(tick);
		h.setMode(m);
		h.setOrdering(o);
		h.setAscending(a);
		Annotation.min_diff = min_diff;
	}

	/**
	 * Saves the order of the images at closing and save the times each was last
	 * modified.
	 * 
	 * @param h
	 *            the ImageHandler whose images will have their ordering and
	 *            modification times saved.
	 */
	private static void setTimeStamps(ImageHandler h) {
		File time = new File("timestamps");
		if (!time.exists())
			time.mkdir();
		if (h.getListFile() != null) {
			String list = h.getListFile().getName();
			try (BufferedWriter btime = new BufferedWriter(new FileWriter(new File(time, list)))) {
				for (MetaImage i : h.getMasterList())
					btime.append(String.valueOf(i.getDate()) + "\t" + i + "\n");
				btime.flush();
				btime.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Store user configurations to a configuration file for use on next start
	 * up of ILB
	 * 
	 * @param iLB
	 *            the ILB to store information for
	 * @param h
	 *            the Image Handler used by this ILB
	 * @param fileName
	 *            the name of the config file to save to
	 */
	static void setProperties(ILB iLB, ImageHandler h, String fileName) {
		Properties prop = new Properties();
		setTimeStamps(h);
		/* Now grouping via tag per annotation */
		// setGroupings(h);
		if (h.getDirectory() == null)
			prop.setProperty("repository", "NULL");
		else
			prop.setProperty("repository", h.getDirectory().getAbsolutePath());
		if (h.getListFile() == null)
			prop.setProperty("list", "NULL");
		else
			prop.setProperty("list", h.getListFile().getAbsolutePath());

		prop.setProperty("size", String.valueOf(iLB.getMenus().getViewMenu().getThumbSizeSlider().getValue()));
		prop.setProperty("windowwidth", String.valueOf(iLB.getWidth()));
		prop.setProperty("windowheight", String.valueOf(iLB.getHeight()));
		prop.setProperty("scrolltick", String.valueOf(iLB.getContent().getVerticalScrollBar().getValue()));
		prop.setProperty("positionX", String.valueOf(iLB.getLocationOnScreen().getX()));
		prop.setProperty("positionY", String.valueOf(iLB.getLocationOnScreen().getY()));
		prop.setProperty("displaySetting", String.valueOf(h.getMode()));
		prop.setProperty("sortOrder", h.getOrdering().toString());
		prop.setProperty("ascending", String.valueOf(h.isAscending()));
		prop.setProperty("annotation#min_diff", String.valueOf(Annotation.min_diff));
		prop.setProperty("FaceMaskScale", Float.toString(FaceMask.scale.getFloat()));
		prop.setProperty("FaceMaskTranslucency", Float.toString(FaceMask.translucency.getFloat()));
		if (FaceMatchJavaInterface.loaded)
			FaceFinder.ProcFlag.setProperties(prop);
		try (FileOutputStream os = new FileOutputStream(fileName)) {
			prop.store(os, null);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}