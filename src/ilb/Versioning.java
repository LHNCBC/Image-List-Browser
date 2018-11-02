package ilb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Versioning class details the current version of the ILB source code
 * overall, whenever ILB is going to be released to users and testing, it should
 * be updated to a new version number. All versions should be stored at
 * \\\\lhcdevfiler\\FMProject\\ImageListBrowser\\Current with the naming
 * convention: YYYY.MM.DD.ILB.exe. (eg: a release from January 2nd, 1970 would
 * be named 1970.01.01.ILB.exe).
 * <p>
 * This also provides methods to check the current version against those in the
 * ILB repository, to provide the current version of ILB and confirm if the
 * version in use is the most recent.
 * 
 * @author bonifantmc
 *
 */
public class Versioning {
	/** The Directory where all versions of ILB are to be stored. */
	final public static File ILBRepository = new File("\\\\lhcdevfiler\\FMProject\\ImageListBrowser\\Current");
	/**
	 * a Regex for parsing self-extracting zip files with ILB at the repository.
	 * (ILB is stored in self-extracting zip files made from 7zip titled by
	 * release date: "YYYY.MM.DD.ILB.exe"
	 */
	final public static String ILBRegex = "(\\d\\d\\d\\d)\\.(\\d\\d)\\.(\\d\\d)\\.ILB.*\\.exe";

	/** The current version of ILB by date */
	final public static String CURRENT_VERSION = "2017.08.10.ILB.exe";

	/** The current version of ILB numerically. */
	final public static String VERSION = "1.8.1";

	/**
	 * @return true if the current code is the most recent code, else false
	 *         (assumes if there's an ILB exe in the ILB repository with a more
	 *         recent date stamp, that it is in fact more recent and not someone
	 *         being funny and just naming a file to suggest its ILB and more
	 *         recent.) Returns NULL if the repository is empty or can't be
	 *         reached.
	 * @throws FileNotFoundException
	 *             if the ILB repository cannot be found
	 * @throws AccessDeniedException
	 *             if the ILB repository cannot be read
	 */
	static public Boolean isUpToDate() throws AccessDeniedException, FileNotFoundException {
		return isFutureRelease() == 0;
	}

	/**
	 * 
	 * @return the newest release according to the ILB repository at
	 *         \\\\lhcdevfiler\\FMProject\\ImageListBrowser\\Current, based on
	 *         the release dates marked. NULL if ILB couldn't reach/find any
	 *         releases.
	 * @throws AccessDeniedException
	 *             If the ILB repository can't be read from
	 * @throws FileNotFoundException
	 *             If the ILB repository can't be found
	 */
	static public String getLatestRelease() throws AccessDeniedException, FileNotFoundException {
		if (!ILBRepository.exists())
			throw new FileNotFoundException("Could not find: " + !ILBRepository.canRead());
		if (!ILBRepository.canRead())
			throw new AccessDeniedException("Could not read: " + ILBRepository.getAbsolutePath());
		// !ILBRepository.canRead())
		String[] releases = ILBRepository.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.matches(ILBRegex);
			}
		});
		if (releases == null)
			return null;
		ArrayList<String> list = new ArrayList<>();
		for (String s : releases)
			list.add(s);
		Collections.sort(list);

		// could not find any releases in release folder.
		if (list.size() == 0)
			return null;

		return list.get(list.size() - 1);
	}

	/**
	 * Checks where in versioning the Current Version is, based on those in the
	 * release folder on DevFiler.
	 * 
	 * @return 1 if future (ALPHA/BETA/Development) release, 0 if current
	 *         production release, -1 if outdated release. null if could not
	 *         determine (IE could not reach ILB release folder, or folder was
	 *         emptied etc.).
	 * @throws FileNotFoundException
	 *             if the ILB repository cannot be found
	 * @throws AccessDeniedException
	 *             if the ILB repository cannot be read
	 */
	static public Integer isFutureRelease() throws AccessDeniedException, FileNotFoundException {
		return compare(CURRENT_VERSION, getLatestRelease());

	}

	/**
	 * Compares two Strings assuming them to be ILB.exe archives. Ranks the two
	 * based on which archive is more recent
	 * 
	 * @param arg0
	 *            one ILB.zip archive
	 * @param arg1
	 *            another ILB.zip archive
	 * @return a value less than 0 if arg0 &lt; arg1; and a value greater than 0
	 *         if arg0 &gt; arg1, 0 if arg0 == arg1
	 * @throws IllegalArgumentException
	 *             if either arg0 or arg1 are not in the ILB.zip format
	 * 
	 */
	public static int compare(String arg0, String arg1) {
		if (arg1 == null || arg0 == null)
			return 0;
		Matcher m0 = Pattern.compile(ILBRegex).matcher(arg0);
		Matcher m1 = Pattern.compile(ILBRegex).matcher(arg1);
		if (!m0.matches() || !m1.matches())
			throw new IllegalArgumentException(arg0 + " and/or " + arg1 + " is not a valid ILB exe.");

		int y0 = Integer.parseInt(m0.group(1)), y1 = Integer.parseInt(m1.group(1)),
				mth0 = Integer.parseInt(m0.group(2)), mth1 = Integer.parseInt(m1.group(2)),
				d0 = Integer.parseInt(m0.group(3)), d1 = Integer.parseInt(m1.group(3));
		int y = Integer.compare(y0, y1);
		int mth = Integer.compare(mth0, mth1);
		int d = Integer.compare(d0, d1);
		if (y == 0) {
			if (mth == 0)
				return d;
			return mth;
		}
		return y;
	}

	/**
	 * Updates ILB with the most recent version.
	 */
	public void update() {
		try {
			if (!Versioning.isUpToDate()) {
				// TODO
				System.out.println("COMING TO A COMPUTER NEAR YOU, SELF-UPDATING ILB, BUT NOT YET.");
				return;
			} else
				System.out.println("ILB is up to date (or the update repository is unavailable)");
		} catch (AccessDeniedException | FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}