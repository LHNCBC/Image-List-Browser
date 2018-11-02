package fm;

import java.io.File;

/**
 * Load FaceMatch API via Java Native Interface and get results back. It
 * requires a native library FaceMatchJavaInterFace to implement the native
 * methods to be somewhere in the user's path variable.
 */
public class FaceMatchJavaInterface {
	/** tells if the library for FM-ILB communication is loaded */
	public static final Boolean loaded;

	static {
		boolean ld = false;
		try {
			File f = new File("Files");
			if (!f.exists())
				f.mkdir();
			System.loadLibrary("FaceMatchJavaInterface");
			ld = true;
		} catch (UnsatisfiedLinkError e) {
			ld = false; // methods requiring the libraries will be disabled
			System.err.printf("WARNING: failed to load FaceMatchJavaInterface library: %s\n", e.getMessage());
		}
		loaded = ld;
	}

	/**
	 * Call FaceMatch's nearDup from native library and get string
	 * representation of NearDupImgDetector's getDups() map.
	 * 
	 * @param repoPath
	 *            repository path to search for images in
	 * @param lstInpFN
	 *            input list file to build the map against
	 * @return list of images with their near duplicates
	 */
	public native static String nearDup(String repoPath, String lstInpFN);

	// C:\Users\kimem\Documents\FaceMatch\Lists\CalTech
	/**
	 * Call FaceMatch's faceFind from native library
	 * 
	 * @param flags
	 *            numeric processing flags to set from FaceFinder.java
	 * 
	 * @param arr
	 *            list of variables to pass to faceFind
	 * @param images
	 *            list of all images to call FaceFinder on
	 * 
	 * @param j
	 *            the thread worker running this method
	 * @return beans upon success
	 */
	public native static String faceFind(int flags, Object[] arr, Object[] images, Object j);

	/**
	 * Call FaceMatch's ImageMatcher on the given repository and list file.
	 * 
	 * @param j
	 *            the CategoryTable so that imageMatch can call its methods as
	 *            it spits out results for each image
	 * @param ndxFile
	 *            the file to read indices from
	 * @param imMode
	 *            the type of matching to use
	 * @param path
	 *            location of images
	 * @param lst
	 *            list of images
	 * @param ffFlags
	 *            face finder flags for images without faces
	 * @param threshold
	 *            the score an image needs to be considered a match (if
	 * 
	 * 
	 * @return list of image with those that most closely match it
	 */
	public native static String imageMatch(String ndxFile, String imMode, String path, String lst, int ffFlags,
			float threshold, Object j);

	/**
	 * 
	 * @param arr1
	 *            the input parameters for the ingest
	 * @param arr2
	 *            the ndxs to make
	 * @param j
	 *            the worker thread working this method
	 * @return "beans" on success else an error message
	 */
	public native static String ingest(Object[] arr1, Object[] arr2, Object j);

}