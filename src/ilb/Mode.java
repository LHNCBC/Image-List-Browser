package ilb;

/**
 * Thumbails can be handled in 3 different Modes: GRID, LIST, and GROUPING.
 * <P>
 * GRID is where an ImageHandler displays Images in a single ThumbnailList that
 * has multiple rows and columns based off its visible list. Only the thumbnail
 * and image name are displayed.
 * <p>
 * LIST is where an ImageHandler displays Images in a single Thumbnail List that
 * has multiple rows, but only one column based off its visible list. Most of an
 * Image's metadata is directly displayed on screen next to the Image.
 * <p>
 * Grouping is where an ImageHandler displays Images with a GrouList based of
 * its master set.
 * 
 * @author bonifantmc
 * 
 */
public enum Mode {
	/**
	 * display the image with its name under it, one type of Annotating Mode,
	 * images fill row, images are in a grid JList
	 */
	GRID,
	/**
	 * display the image with its meta data to its right, one type of Annotating
	 * Mode, images are one per row, images are in a single column JList
	 */
	LIST,
	/**
	 * display images in same manner as GRID, images are in a GroupList (list of
	 * JPanels where each panel holds a horizontal JList for a set of
	 * images/annotation pairs in the AnnotationGroups)
	 */
	GROUPING_IMAGES,
	/**
	 * display images in same manner as GRID, images are in a GroupList (list of
	 * JPanels where each panel holds a horizontal JList for a set of images in
	 * the ImageGroups)
	 */
	GROUPING_ANNOTATIONS;

	/**
	 * 
	 * @param s
	 *            string to parse into a Mode
	 * @return the Mode String s indicates
	 */
	public static Mode parseMode(String s) {

		switch (s.toUpperCase()) {
		case "GROUPING_IMAGES":
			return GROUPING_IMAGES;
		case "GROUPING_ANNOTATIONS":
			return GROUPING_ANNOTATIONS;
		case "LIST":
			return LIST;
		case "GRID":
		default:
			return GRID;
		}
	}

}
