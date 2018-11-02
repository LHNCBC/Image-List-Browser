package annotations;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import struct.AnnotationGroups;
import struct.ArrayListModel;
import struct.MetaImage;

/**
 * An <i>Annotation</i> is a specialized Rectangle2D.Double, that has additional
 * tags marking it for specialized display on screen and when printed as a
 * toString.
 * <p>
 * The first specialization of an Annotation is it's Feature, every annotation
 * has a Feature to indicate that the Annotation marks some specific region (ie:
 * a face, an eye, etc). In addition to a Feature all Annotations have optional
 * Attributes, and sub Annotations (Annotations defined relative to their parent
 * Annotation rather than an image).
 * <p>
 * Example: of a toString: p[10,10;25,25] defines a rectangle starting at the
 * coordinate (x=10,y=10) extending down and to the right sweeping out a
 * w=25*h=25 pixel^2 area of a Profile Feature, indicating a human face tilted
 * nominally at least 45 degrees to the left or right (ie the profile of a human
 * face).
 * <p>
 * The class Annotation includes methods for parsing, printing, displaying, and
 * adjusting Annotations.
 * <p>
 * Annotations have the following conditions required in reference to their
 * size:
 * <p>
 * Rectangles must be at least {@value #MIN_AREA} pixels^2 and
 * <p>
 * have a minimum aspect ratio of {@value #MIN_ASPECT_RATIO} regardless of the
 * rectangle's orientation.
 * <p>
 * Depending on the Feature an Annotation may be treated as an ellipse which is
 * nominally no different from a rectangle that circumscribes the ellipse, given
 * {@link Ellipse2D} and {@link Rectangle2D} are both extensions of
 * {@link java.awt.geom.RectangularShape}
 * 
 * <p>
 * Annotations with no subAnnotation or Qttributes are considered <i>generic</i>
 * and are of the format
 * <p>
 * <b> &lt;char&gt;[&lt;tint&gt;,&lt;int&gt;;&lt;int&gt;,&lt;int&gt;]</b>
 * <p>
 * Annotations with subFeatures or attributes are considered <i>complex</i> and
 * are of the format
 * <p>
 * <b>&lt;char&gt;{[&lt;int&gt;,&lt;int&gt;;&lt;int&gt;,&lt;int&gt;] ...}</b>
 * <p>
 * where "..." indicates a tab separated list of subAnnotations and Attributes.
 * Coordinates of subAnnotation are relative to their parent annotations's
 * location, not the images they belong to.
 * 
 * @author bonifantmc
 * 
 * @see Feature
 * @see Attribute
 * @see MetaImage
 */
@SuppressWarnings("serial")
public class Annotation extends Rectangle2D.Double {
	// ////////////////////////////////////////////////
	// Regular Expressions for matching annotations //
	// ////////////////////////////////////////////////

	/** a regular expression that matches a rectangle [x,y;w,h] */
	static private final String reRectangle = "\\[(-?\\d+),(-?\\d+);(-?\\d+),(-?\\d+)\\]";

	/** Attributes are string tags for an Annotation. */
	static private final String reAttribute = "\\[([.*]+)\\]";

	/** the contents of square braces are either rectangles or attributes */
	static private final String reContent = "(" + reRectangle + ")|(" + reAttribute + ")";

	/** A simple Annotation is only a single Feature id and a rectangle **/
	static private final String reSimpleAnnotation = "(([A-Za-z])(" + reContent + "))";
	/**
	 * Complex Annotations are a Feature id, rectangle and tab separated list of
	 * sub Annotations and Attributes
	 */
	static private final String reComplexAnnotation = "(([A-Za-z])\\{" + reRectangle + "(.*)\\})";

	/**
	 * A regular expression that matches both generic and complex Annotations
	 */
	static private final String ANNOTATION = reSimpleAnnotation + "|" + reComplexAnnotation;

	/** The Pattern that checks if a string might be an Annotation */
	static private final Pattern annotationPattern = Pattern.compile(ANNOTATION);

	/***/
	static private final String FLOAT_REGEX = "([+-]?\\d*\\.?\\d*)";
	/** regular expression for the roll/yaw/pitch */
	private static final String ROTATION_REGEX = "r\\[" + FLOAT_REGEX + "," + FLOAT_REGEX + "," + FLOAT_REGEX + "\\]";
	/** pattern for the roll/yaw/pitch */
	private static final Pattern ROTATION_PATTERN = Pattern.compile(ROTATION_REGEX);

	/** */
	private static final String PITCH_MIN = null;
	/**	 */
	private static final String PITCH_MAX = null;
	/**	 */
	private static final String ROLL_MIN = null;
	/**	 */
	private static final String ROLL_MAX = null;
	/**	 */
	private static final String YAW_MIN = null;
	/**	 */
	private static final String YAW_MAX = null;

	// ////////////////////////////////////////////////
	// Requirements of Annotation size and dimension //
	// ////////////////////////////////////////////////

	/** Annotations must cover at least this area of pixels */
	static private final int MIN_AREA = 64;

	/** Annotations must be at least this number of pixels across */
	static private final int MIN_WIDTH = 20;

	/** Annotations must be in rectangles with at least this aspect ratio */
	static private final double MIN_ASPECT_RATIO = 0.2;

	// ////////////////////////////////////////////////
	// Instance Variables that make up an Annotation //
	// ////////////////////////////////////////////////

	/** What type of Feature this Annotation is (Face, Profile, Nose, etc) */
	private Feature id;

	/**
	 * Annotations within this Annotation, e.g. Annotations of subFeatures like
	 * the mouth and nose within a face
	 */
	private ArrayListModel<Annotation> subannotes = new ArrayListModel<>();

	/** the skin tone marked for this Annotation */
	private Attribute skin = new Attribute(Attribute.SkinTone);

	/** the gender marked for this Annotation */
	private Attribute gender = new Attribute(Attribute.Gender);

	/** the age range marked for this Annotation */
	private Attribute age = new Attribute(Attribute.Age);

	/** the wounds the annotation marks if any */
	private Attribute wound = new Attribute(Attribute.Wounds);

	/** the occlusions the annotation marks, if any */
	private Attribute occlusions = new Attribute(Attribute.Occlusions);

	/** the species/Kind the annotation marks, if any */
	private Attribute kind = new Attribute(Attribute.Kind);
	/**
	 * flag marking if the annotation is selected when selected, the color is
	 * overridden as a bright GREEN
	 */
	private boolean isSelected = false;

	/**
	 * a category this annotation can be classified as (as in this isn't just a
	 * face it's Bob's face, or this isn't a car its a 2997 Pontiac Grand AM,
	 * etc). All annotations are by default in the UNTAGGED category, untagged
	 * won't print in the lst file, the tag will simply be ommitted
	 * <p>
	 * TODO allow multiple categories? (ie if it might be one thing or another,
	 * like trying to id a twin, or if you want to mark it green since it has
	 * green, and yellow if it also happens to have yellow)
	 */
	private Category category = new Category(Category.UNTAGGED.toString(), 'd');
	/**
	 * An additional category allowed for annotations of animals, intended to
	 * mark the breed of the animal as opposed to specifically identifying an
	 * individual animal
	 */
	private Category breed = new Category(Category.BREED_UNKNOWN.toString(), 'b');

	/** rotation of the annotation around a perceived longitudinal axis */
	private float roll;
	/** rotation of the annotation around a perceived lateral axis */
	private float pitch;
	/** rotation of the annotation around a perceived vertical axis */
	private float yaw;

	/**
	 * The parent Annotation if this Annotation if this Annotation marks a
	 * subFeature
	 */
	private Annotation parent;

	/** objects checking for changes to this annotation */
	private Vector<AnnotationChangeListener> listeners = new Vector<>();
	/**
	 * True if the annotation is being dragged/resized and its rectangle should
	 * be displayed (only significant if Annotation is displayed as an ellipse)
	 */
	public boolean isDragEdgeVisible = false;

	// ///////////////
	// Constructors //
	// ///////////////

	/**
	 * Create an Annotation marking a specific rectangular spot on an image.
	 * 
	 * @param idType
	 *            the Feature this Annotation will mark
	 * @param x
	 *            initial x coordinate (left-most x coordinate)
	 * @param y
	 *            initial y coordinate (top-most y coordinate)
	 * @param width
	 *            width of this Annotation
	 * @param height
	 *            height of this Annotation
	 * @param parent
	 *            if this annotation is a sub annotation, this is its parent
	 * @param skin
	 *            attribute of skin identified
	 * @param gender
	 *            attribute of gender identified
	 * @param age
	 *            attribute of age identified
	 * @param wound
	 *            attribute marking if feature is wounded
	 * @param occlusions
	 *            attribute marking occlusions
	 * @param kind
	 *            attribute marking animal species/kind
	 *
	 * @param breed
	 *            attribute marking the breed of an animal
	 * @param category
	 *            classifies the annotation as being a specific object (car,
	 *            truck, Bob, human face, etc)
	 * 
	 */
	public Annotation(Feature idType, double x, double y, double width, double height, Annotation parent, String skin,
			String gender, String age, String wound, String occlusions, String kind, String breed, String category) {
		if (idType == null)
			throw new IllegalArgumentException("Annotations must mark a particular Feature");
		setId(idType);
		setRect(new Rectangle2D.Double(x, y, width, height));
		setParent(parent);
		if (parent != null) {
			this.x += parent.x;
			this.y += parent.y;
			parent.subannotes.add(this);
		}
		setSkin(skin == null ? "Unmarked" : skin);
		setGender(gender == null ? "Unmarked" : gender);
		setAge(age == null ? "Unmarked" : age);
		setWound(wound == null ? "Unmarked" : wound);
		setOcclusions(occlusions == null ? "Unmarked" : occlusions);
		setKind(kind == null ? "Unmarked" : kind);

		setBreed(breed == null || breed.trim() == "" ? AnnotationGroups.UNTAGGED : breed);
		setCategory(category == null || category.trim() == "" ? AnnotationGroups.UNTAGGED : category);

	}

	/**
	 * -
	 * 
	 * @param id
	 *            id
	 * @param x
	 *            x location in image
	 * @param y
	 *            y location in image
	 * @param width
	 *            width of annotation
	 * @param height
	 *            height of annotation
	 * @param parent
	 *            the parent of the annotation (if its a sub annotation)
	 * @param skin
	 *            the skin attribute
	 * @param gender
	 *            the gender attribute
	 * @param age
	 *            the age attribute
	 * @param wound
	 *            the wound attribute
	 * @param occlusions
	 *            the occlusion attribute
	 * @param kind
	 *            the kind attribute
	 * @param breed
	 *            the breed attribute
	 * @param category
	 *            the category attribute
	 */
	public Annotation(Feature id, double x, double y, double width, double height, Annotation parent, Attribute skin,
			Attribute gender, Attribute age, Attribute wound, Attribute occlusions, Attribute kind, String breed,
			String category) {
		setId(id);
		setRect(new Rectangle2D.Double(x, y, width, height));
		setParent(parent);
		setSkin(skin.values());
		setGender(gender.values());
		setAge(age.values());
		setWound(wound.values());
		setOcclusions(occlusions.values());
		setKind(kind.values());
		setBreed(breed);
		setCategory(category);
	}
	// ///////////////////
	// Parsing methods //
	// ///////////////////

	/**
	 * Read a line checking for generic and complex annotations
	 * <p>
	 * Any number of Annotations can be in a line. This just splits the
	 * Annotations and calls parseAnnotation on each part.
	 * <p>
	 * Parts are delineated based on tabs, in the case of complex annotations,
	 * all tab separated sections will be included from the initial curly brace
	 * until that brace is closed.
	 * <p>
	 * This parser skips sections in the tab delineation if the section doesn't
	 * contain a square brace since all generic annotations and attributes use
	 * them and anything without one is considered extraneous (for instance if
	 * the line starts with a file name, that will be skipped).
	 * 
	 * @param line
	 *            the string to parse
	 * @return an ArrayList of all Annotations found in the string
	 */
	static public ArrayList<Annotation> parseAnnotationList(String line) {
		// break up the presumably tab separated line into a its composite
		// segments
		String[] segments = line.trim().split("[\t]");

		// the list of Annotations the input line contained separated from each
		// other
		ArrayList<String> toParse = new ArrayList<>();

		// the current annotation being built up from the segmented list
		String readLine = "";

		// whenever reentrant reaches zero an Annotation has been complete and
		// readLine needs to be added to toParse and then reset
		int reentrant = 0;

		// loop through all the segments and build up strings of individual
		// generic and complex annotations
		for (String segment : segments) {
			// all Annotations and Attributes require at least "[" & "]". If the
			// segment contains neither skip it
			if (!(segment.contains("[") || segment.contains("]")))
				continue;

			// build current Annotation
			readLine += "\t" + segment;

			// curly braces indicates list of sub-Annotations and Attributes
			// until they all close don't add to toParse
			if (segment.contains("{"))
				reentrant += charFrequency('{', segment);

			if (segment.contains("}"))
				reentrant -= charFrequency('}', segment);

			// All braces have been closed, readline is ready to be added to
			// toParse
			if (reentrant == 0) {
				toParse.add(readLine.trim());
				readLine = "";
			}
		}

		// parse the Annotations and build an ArrayList
		ArrayList<Annotation> res = new ArrayList<>();
		for (String note : toParse) {
			Annotation toAdd = parseAnnotation(note, null);
			if (toAdd != null)
				res.add(toAdd);
		}

		// remove any annotations that don't fit the min reqs of an annotation
		Annotation.removedTooSmallAnnotations(res);

		return res;
	}

	/**
	 * Count the number of occurrences of a character c in a string
	 * 
	 * @param c
	 *            the character to seek
	 * @param s
	 *            the string to look in
	 * @return the number of occurences of c in s
	 */
	static int charFrequency(char c, String s) {
		int i = 0;
		for (int j = 0; j < s.length(); j++)
			if (s.charAt(j) == c)
				i++;

		return i;
	}

	/**
	 * Parses an Annotation from a string using {@value #ANNOTATION} to get
	 * Feature type and the initial coordinates of the Annotation and the list
	 * of tab separated subFeatures/Annotations and Attributes (if any).
	 * <p>
	 * By breaking up the list of subFeatures/Annotations and Attributes at tab
	 * delineations, recursive calls to this method can be made to find
	 * subFeatures on segments of the list, while Attributes can be found by
	 * attempting matchings with the various Patterns supplied by Attribute and
	 * its nested classes.
	 * 
	 * @param lineSegment
	 *            the line to parse, generall a segment of a line read from a
	 *            .lst file, generally given from the parseAnnotationList
	 *            method.
	 * @param parent
	 *            optional parameter to set the annotation's parent at
	 *            instantiation
	 * @return A single Annotation with all its sub-Annotations and Attributes
	 *         (if it has any).
	 * 
	 * @throws IllegalArgumentException
	 *             thrown when the input does not match the annotationPattern,
	 *             Attributes are repeated, or Attributes or
	 *             subFeature/Annotations are malformed.
	 */
	static public Annotation parseAnnotation(String lineSegment, Annotation parent) {
		Matcher matcher = annotationPattern.matcher(lineSegment);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(
					"Input does not match basic Annotation formatting: " + lineSegment + ".");
		}

		// a basic annotation, with no muss or fuss
		int x, y, w, h;
		Feature t;
		int off = 1;
		// generic annotation
		if (matcher.group(off) != null) {
			String grp = matcher.group(++off);
			t = Feature.parseFeature(grp);
			int GrpCnt = matcher.groupCount();
			// loop to find next useful group
			while (++off < GrpCnt && (matcher.group(off) == null || matcher.group(off).startsWith("["))) {/* looping */
			}
			if (off >= GrpCnt)
				throw new IllegalArgumentException("invalid annotation group " + matcher.group(0));

			grp = matcher.group(off);
			x = Integer.parseInt(grp);
			y = Integer.parseInt(matcher.group(++off));

			w = Integer.parseInt(matcher.group(++off));
			h = Integer.parseInt(matcher.group(++off));

			return new Annotation(t, x, y, w, h, parent, Attribute.UNMARKED, Attribute.UNMARKED, Attribute.UNMARKED,
					Attribute.UNMARKED, Attribute.UNMARKED, Attribute.UNMARKED, Category.BREED_UNKNOWN.toString(),
					Category.UNTAGGED.toString());
		}

		// a complicated mussy fussy feature
		int GrpCnt = matcher.groupCount();
		while (off < GrpCnt && matcher.group(off) == null) {
			++off;
		}

		if (off >= GrpCnt)
			throw new IllegalArgumentException("invalid annotation group");

		t = Feature.parseFeature(matcher.group(++off));
		x = Integer.parseInt(matcher.group(++off));
		y = Integer.parseInt(matcher.group(++off));
		w = Integer.parseInt(matcher.group(++off));
		h = Integer.parseInt(matcher.group(++off));

		// the result of this parsing, before checking for fancy attributes and
		// sub features
		Annotation res = new Annotation(t, x, y, w, h, parent, Attribute.UNMARKED, Attribute.UNMARKED,
				Attribute.UNMARKED, Attribute.UNMARKED, Attribute.UNMARKED, Attribute.UNMARKED,
				Category.BREED_UNKNOWN.toString(), Category.UNTAGGED.toString());

		// list of subFeatures and Attributes previously tab separated
		String grp = matcher.group(++off); // old=13
		String[] featuresAndAttributes = grp.split("\t");

		// false once an age is found
		boolean ageNotFound = true;
		// false once a gender is found
		boolean genderNotFound = true;
		// false once a skin is found
		boolean skinNotFound = true;
		// false once a wound is found
		boolean woundNotFound = true;
		// false once a glasses is found
		boolean occlusionsNotFound = true;
		// false once animal type is found
		boolean kindNotFound = true;
		// false once a category is found
		boolean categoryNotFound = true;
		// false once breed is found
		boolean breedNotFound = true;
		// false once rotation is found
		boolean rotationNotFound = true;

		// loop through the list, and assign the first age, gender, and skin
		// found, while adding subFeatures, if a second age, gender, or skin is
		// found throw an error because you can have only one of each
		for (int i = 0; i < featuresAndAttributes.length; i++) {
			String part = featuresAndAttributes[i].trim();

			// parse for an age
			if (ageNotFound && res.getAge().parseSet(part))
				ageNotFound = false;

			// parse for a gender
			else if (genderNotFound && res.getGender().parseSet(part))
				genderNotFound = false;

			// parse for a skin type
			else if (skinNotFound && res.getSkin().parseSet(part))
				skinNotFound = false;

			// parse for a wound
			else if (woundNotFound && res.getWound().parseSet(part))
				woundNotFound = false;

			// parse for occlusions
			else if (occlusionsNotFound && res.getOcclusions().parseSet(part))
				occlusionsNotFound = false;

			// parse for kind
			else if (kindNotFound && res.getOcclusions().parseSet(part))
				kindNotFound = false;

			else if (rotationNotFound && res.parseRotation(part))
				rotationNotFound = false;
			else

			// parse for a subfeature
			if (annotationPattern.matcher(part).matches())

			{
				parseAnnotation(part, res);

			} else
			// parse for a subfeature with fancy sub children for the really
			// recursive stuff
			if (part.contains("{")) {
				String fancyAnnotationPart = "";
				int reentrant = 0;
				for (; i < featuresAndAttributes.length; i++) {
					if (featuresAndAttributes[i].contains("{"))
						reentrant += charFrequency('{', featuresAndAttributes[i]);
					if (featuresAndAttributes[i].contains("}"))
						reentrant -= charFrequency('}', featuresAndAttributes[i]);
					fancyAnnotationPart += "\t" + featuresAndAttributes[i];
					if (reentrant == 0)
						break;
				}
				while (reentrant < 0) {
					fancyAnnotationPart.subSequence(0, fancyAnnotationPart.lastIndexOf('}'));
					i--;

				}
				Annotation.parseAnnotation(fancyAnnotationPart.trim(), res);

			} else // parse for a category
			if (Category.pattern.matcher(part).matches()) {
				Category tmp = Category.getAttribute(part);
				if (tmp.id == 'd')
					if (categoryNotFound) {
						res.setCategory(tmp.toString());
						categoryNotFound = false;
					} else
						throw new IllegalArgumentException("Individual Annotations can only have one category.");

				// parse for a breed
				else if (tmp.id == 'b')
					if (breedNotFound) {
						res.setBreed(tmp.toString());
						breedNotFound = false;
					} else
						throw new IllegalArgumentException("Individual Annotations can only have one category.");
			} else if (!part.equals("")) {
				throw new IllegalArgumentException(part + " is not a valid feature or attribute");
			}

		}
		return res;

	}

	//
	// general methods
	//

	/**
	 * Generate a String in the form f{[x,y;w,h] i[x',y';w',h']...}, if
	 * sub-annotations are included; otherwise, generate f[x,y;w,h].
	 */
	@Override
	public String toString() {
		// girish: added gender and age support
		StringBuilder ret = new StringBuilder();

		// generic annotation
		if (getSubannotes().size() == 0 && getSkin().isUnmarked() && getGender().isUnmarked() && getAge().isUnmarked()
				&& getWound().isUnmarked() && getOcclusions().isUnmarked() && getKind().isUnmarked()
				&& getCategory().toString().equals(Category.UNTAGGED.toString())
				&& getBreed().toString().equals(Category.BREED_UNKNOWN.toString()) && getRoll() == 0 && getYaw() == 0
				&& getPitch() == 0) {
			ret.append(getId().getIdString());
			ret.append("[");
			ret.append(getRelX());
			ret.append(",");
			ret.append(getRelY());
			ret.append(";");
			ret.append((int) getWidth());
			ret.append(",");
			ret.append((int) getHeight());
			ret.append("]");
			return ret.toString();
		}

		// complex annotation
		ret.append(getId().getIdString());
		ret.append("{[");
		ret.append(getRelX());
		ret.append(",");
		ret.append(getRelY());
		ret.append(";");
		ret.append((int) getWidth());
		ret.append(",");
		ret.append((int) getHeight());
		ret.append("]");

		if (!getSkin().isUnmarked())
			ret.append("\t").append(getSkin());

		// Girish: added gender and age support
		if (!getGender().isUnmarked())
			ret.append("\t").append(getGender());

		if (!getAge().isUnmarked())
			ret.append("\t").append(getAge());

		if (!getWound().isUnmarked())
			ret.append("\t").append(getWound());

		if (!getOcclusions().isUnmarked())
			ret.append("\t").append(getOcclusions());

		if (!getKind().isUnmarked())
			ret.append("\t").append(getKind());

		if (getCategory().getID() != null && !getCategory().getID().equals("")
				&& !getCategory().toString().equals(Category.UNTAGGED.toString()))
			ret.append("\t").append(getCategory().getID());

		if (!getBreed().toString().equals(Category.BREED_UNKNOWN.toString()))
			ret.append("\t").append(getBreed().getID());
		if (!(getRoll() == 0 && getYaw() == 00 && getPitch() == 0))
			ret.append("\t").append("r[").append(getRoll()).append(",").append(getPitch()).append(",").append(getYaw())
					.append("]");

		for (int i = 0; i < this.getSubannotes().size(); i++)
			ret.append("\t").append(getSubannotes().get(i));
		ret.append("}");
		return ret.toString();
	}

	/** @return the start x of this relative to its parent annotation */
	int getRelX() {
		if (this.parent != null)
			return (int) (this.x - this.parent.x);
		return (int) this.x;
	}

	/** @return the start y of this relative to its parent annotation */
	int getRelY() {
		if (this.parent != null)
			return (int) (this.y - this.parent.y);
		return (int) this.y;
	}

	/**
	 * Confirms if point p is within this Annotation, check takes into account
	 * whether or not the annotation is an ellipse or a rectangle
	 * 
	 * @param p
	 *            the point to check
	 * @return true if the point is inside or on the rectangle, else false;
	 */
	public boolean contains(Point2D.Double p) {
		if (getId().isRectangle())
			return new Rectangle2D.Double(this.x, this.y, this.width, this.height).contains(p);
		return new Ellipse2D.Double(this.x, this.y, this.width, this.height).contains(p);
	}

	/**
	 * If a sub-annotation contains the given point, removes that sub-annotation
	 * from this and return the sub-annotation. If no sub-annotation contains
	 * the point, but this does, returns this, if the point isn't contained in
	 * this, returns null
	 * 
	 * @param p
	 *            the point to test
	 * @return null if this doesn't contain the point, else the first
	 *         sub-annotation that contains the point, else this
	 * 
	 */
	public Annotation remove(Point2D.Double p) {
		Annotation ret = null;
		if (!contains(p))
			return ret;
		for (Annotation a : getSubannotes()) {
			ret = a.remove(p);
			if (ret != null) {
				getSubannotes().remove(ret);
				return ret;
			}
		}
		return this;
	}

	/**
	 * Paints an ArrayList of Annotations on a Graphics image.
	 * <p>
	 * Specifies eyes and faces to display as ovals and all other features are
	 * set as rectangles.
	 * <p>
	 * Colors are set based on the {@link Feature}'s getColor method for each
	 * Annotation, though {@link Attribute}s may override the default colorings
	 * 
	 * @param g
	 *            The Graphics to paint the Annotations on
	 * @param annotations
	 *            The ArrayList of Annotations to paint
	 * @param scale
	 *            The factor to scale the Annotations to if different from the
	 *            image's default size.
	 */
	public static void paintAnnotations(Graphics2D g, List<Annotation> annotations, double scale) {
		int xi, yi, hi, wi;
		for (Annotation note : annotations) {
			if (note.isDragEdgeVisible)
				g.setStroke(new BasicStroke(5));
			else
				g.setStroke(new BasicStroke(1));

			g.setColor(note.getId().getColor());
			if (note.isSelected) {
				g.setColor(Feature.COLOR_SELECTED);
				// paintFaceRotationMask(g, note, scale);
			}
			xi = (int) (note.x * scale);
			yi = (int) (note.y * scale);
			hi = (int) (note.height * scale);
			wi = (int) (note.width * scale);
			if (note.getId().isRectangle())
				g.drawRect(xi, yi, wi, hi);
			else
				g.drawOval(xi, yi, wi, hi);

			paintAnnotations(g, note.getSubannotes(), scale);
		}
	}



	/**
	 * Same as paintAnnotations, but ignore all x/y offset for the single given
	 * Annotation. Used when the Annotation is being painted on a subImage of
	 * the image it's associated with that's been cropped to the given
	 * Annotation.
	 * 
	 * @param g
	 *            The Graphics to paint the Annotations on
	 * @param a
	 *            The ArrayList of Annotations to paint
	 * @param scale
	 *            The factor to scale the Annotations to if different from the
	 *            image's default size.
	 */
	public static void paintTrimmedAnnotation(Graphics2D g, Annotation a, double scale) {
		int xi, yi, hi, wi;
		g.setStroke(new BasicStroke(3));
		g.setColor(a.getId().getColor());

		xi = 0;
		yi = 0;
		hi = (int) (a.height * scale);
		wi = (int) (a.width * scale);
		if (a.getId().isRectangle())
			g.drawRect(xi, yi, wi, hi);
		else
			g.drawOval(xi, yi, wi, hi);
		paintTrimmedAnnotations(g, a.getSubannotes(), scale, xi, yi);
	}

	/**
	 * Paint annotations using their relative coordinates since these are
	 * sub-annotations to a Trimmed image's annotation
	 * 
	 * @param g
	 *            graphics to paint on
	 * @param annotations
	 *            annotations to paint
	 * @param scale
	 *            scale to adjust annotations to
	 * @param oX
	 *            origin X coordinate all sub annotations are drawn relative to
	 * @param oY
	 *            origin Y coordinate all sub annotations are drawn relative to
	 */
	private static void paintTrimmedAnnotations(Graphics2D g, ArrayList<Annotation> annotations, double scale, int oX,
			int oY) {
		int xi, yi, hi, wi;

		for (Annotation note : annotations) {
			g.setStroke(new BasicStroke(3));
			// if (!note.getId().isSubfeature() && note.getSkin() !=
			// Skin.Neither)
			// g.setColor(note.getSkin().getColor());
			// else
			// NOTE: NO LONGER MARK COLOR BY Attribute
			g.setColor(note.getId().getColor());

			xi = (int) (oX + note.getRelX() * scale);
			yi = (int) (oY + note.getRelY() * scale);
			hi = (int) (note.height * scale);
			wi = (int) (note.width * scale);
			if (note.getId().isRectangle())
				g.drawRect(xi, yi, wi, hi);
			else
				g.drawOval(xi, yi, wi, hi);

			paintTrimmedAnnotations(g, note.getSubannotes(), scale, xi, yi);
		}
	}

	/** @return x offset of the root parent annotation */
	public int getRootOffSetX() {
		if (this.parent == null)
			return (int) this.getX();
		return this.parent.getRootOffSetX();
	}

	/** @return y offset of the root parent annotation */
	public int getRootOffSetY() {
		if (this.parent == null)
			return (int) this.getY();
		return this.parent.getRootOffSetY();
	}

	/**
	 * @return The smallest possible area that an Annotation can cover
	 */
	public static int getMinArea() {
		return MIN_AREA;
	}

	/**
	 * @return The smallest possible aspect ratio that an Annotation can be
	 *         drawn to.
	 */
	public static double getMinAspectRatio() {
		return MIN_ASPECT_RATIO;
	}

	/**
	 * @param w
	 *            the width of the parent holding this Annotation
	 */
	public void rotateDimensions90(double w) {
		// when we rotate the length and width switch
		double temp = getWidth();
		this.width = getHeight();
		this.height = temp;

		/*
		 * from the rotation matrix x' = -y & y'= x, but this is a rotation
		 * around the origin, which moves the point into quadrant II. Add the
		 * width of the parent to shift back to quadrant I. This is now the
		 * upper right corner because the rectangle was rotated, subtract the
		 * width of the rectangle to get the upper left corner
		 */
		temp = getX();
		this.x = -getY() + w - getWidth();
		this.y = temp;

		// Propagate this to children annotations
		for (Annotation a : getSubannotes())
			a.rotateDimensions90(w);
		this.fireListener();
	}

	/** performs a deep copy of this Annotation */
	@Override
	public Annotation clone() {
		// girish: added gender and age support
		Annotation a = new Annotation(getId(), getX(), getY(), getWidth(), getHeight(), null, getSkin(), getGender(),
				getAge(), getWound(), getOcclusions(), getKind(), getBreed().toString(), getCategory().toString());
		final ArrayList<Annotation> list = getSubannotes();
		if (list != null)
			for (Annotation t : list) {
				Annotation clone = t.clone();
				clone.setParent(a);
				a.getSubannotes().add(clone);
			}
		// girish: added gender and age support
		return a;
	}

	/**
	 * Performs a deep copy on an ArrayList of Annotations
	 * 
	 * @param list
	 *            the list to copy
	 * @return the deep copy
	 */
	public static ArrayList<Annotation> cloneList(ArrayList<Annotation> list) {
		if (list == null)
			return null;
		ArrayList<Annotation> ret = new ArrayList<>();
		for (Annotation a : list)
			ret.add(a.clone());
		return ret;
	}

	/**
	 * 
	 * @param annotations
	 *            to count total of
	 * @return the total number of annotations in the list (including its sub
	 *         features which ArrayList.getSize() would not account for)
	 */
	static int totalAnnotations(ArrayList<Annotation> annotations) {
		if (annotations == null)
			return 0;
		int cnt = 0;
		for (Annotation a : annotations) {
			cnt++;
			cnt += totalAnnotations(a.getSubannotes());
		}
		return cnt;
	}

	/**
	 * @return the current Feature of this Annotation
	 */
	public Feature getId() {
		return this.id;
	}

	/**
	 * @param id
	 *            the new Feature of this Annotation
	 */
	public void setId(Feature id) {
		this.id = id;
		this.fireListener();
	}

	/**
	 * @return the parent Annotation of this Annotation, if one exists,
	 *         otherwise null.
	 */
	public Annotation getParent() {
		return this.parent;
	}

	/**
	 * @param parent
	 *            the new parent Annotation of this Annotation, or null if
	 *            removing the parent
	 */
	public void setParent(Annotation parent) {
		this.parent = parent;
		this.fireListener();
	}

	/**
	 * @return the current list of all sub-Annotations of this Annotation.
	 */
	public ArrayList<Annotation> getSubannotes() {
		return this.subannotes;
	}

	/**
	 * @param subannotes
	 *            the new list of all sub-Annotations of this Annotation.
	 */
	public void setSubannotes(ArrayListModel<Annotation> subannotes) {
		this.subannotes.clear();
		this.subannotes.addAll(subannotes);
		this.fireListener();

	}

	/**
	 * @return the area this Annotation covers in pixels^2.
	 */
	public double getArea() {
		return getWidth() * getHeight();
	}

	/**
	 * @return the lowest aspect ratio this Annotation could have regardless of
	 *         orientation.
	 */
	public double getAspectRatio() {
		double widthOverHeight = getWidth() / getHeight();
		double heightOverWidth = getHeight() / getWidth();
		return widthOverHeight < heightOverWidth ? widthOverHeight : heightOverWidth;

	}

	/**
	 * 
	 * @param p
	 *            if this contains p, then the Annotation will not be null
	 * @return the toString of this Annotation if this contains p, but
	 *         sub-Annotations do not. If a sub-Annotation does contain p, then
	 *         that sub-Annotation's toString, otherwise null;
	 */
	public String getToolTip(Point2D.Double p) {
		if (contains(p)) {
			for (Annotation n : getSubannotes()) {
				String ret = n.getToolTip(p);
				if (ret != null)
					return ret;
			}
			return toString();
		}
		return null;
	}

	/**
	 * Removes annotations from a list if sub-annotations lack width or height,
	 * or if regular annotations lack height, are less than MIN_WIDTH, MIN_AREA,
	 * or MIN_ASPECT_RATIO
	 * 
	 * @param notes
	 *            the list of annotations to check
	 * 
	 * @return removed Annotations
	 */
	public static ArrayList<Annotation> removedTooSmallAnnotations(ArrayList<Annotation> notes) {
		ArrayList<Annotation> ret = new ArrayList<>();
		for (int i = notes.size() - 1; i >= 0; i--) {
			Annotation note = notes.get(i);
			ArrayList<Annotation> subNotes = note.getSubannotes();
			for (int j = subNotes.size() - 1; j >= 0; j--) {
				Annotation subNote = subNotes.get(j);
				if (subNote.height == 0 || subNote.width == 0) {
					ret.add(subNotes.remove(j));
				}
			}
			if (note.height == 0 || note.width < Annotation.MIN_WIDTH || note.getArea() < Annotation.getMinArea()
					|| note.getAspectRatio() < Annotation.getMinAspectRatio()) {
				ret.add(notes.remove(i));
			}
		}
		return ret;
	}

	/**
	 * Removes the smallest annotation at the given point from given list of
	 * annotations.
	 * 
	 * @param p
	 *            the point to check for removing annotations from
	 * @param notes
	 *            list of Annotations to check for point
	 * @return the removed Annotation or null if no annotations exist at this
	 *         point
	 */
	public static Annotation removeAnnotationAtPoint(Point2D.Double p, ArrayList<Annotation> notes) {
		List<Annotation> removalCandidates = getAnnotationsAtPoint(p, notes);
		Annotation toRemove = getSmallestAnnotation(removalCandidates);
		removeRecurse(toRemove, notes);
		return toRemove;
	}

	/**
	 * find the smallest Annotation in a list of Annotations
	 * 
	 * @param lst
	 *            the list of Annotations to look for the smallest Annotation in
	 * @return the smallest Annotation in the given list (null if the list is
	 *         empty)
	 */
	private static Annotation getSmallestAnnotation(List<Annotation> lst) {
		switch (lst.size()) {
		case 0:
			return null;
		case 1:
			return lst.get(0);
		default:
			Annotation smallest = lst.get(0);
			double min = smallest.getArea();
			for (int i = 1; i < lst.size(); i++)
				if (min > lst.get(i).getArea()) {
					smallest = lst.get(i);
					min = smallest.getArea();
				}
			return smallest;
		}
	}

	/**
	 * Remove annotation a completely from arraylist arr
	 * 
	 * @param a
	 *            annotation to remove
	 * @param arr
	 *            list to remove annotation from
	 */
	public static void removeRecurse(Annotation a, List<Annotation> arr) {
		arr.remove(a);
		for (Annotation check : arr)
			removeRecurse(a, check.subannotes);

	}

	/**
	 * @return this Annotations current classification based on its category
	 *         Attribute
	 */
	public Category getCategory() {
		return this.category;
	}

	/**
	 * @param update
	 *            the new category to put this annotation in.
	 */
	public void setCategory(String update) {
		this.category.setIDString(update);
		this.fireListener();

	}

	/**
	 * 
	 * @param list
	 *            a list of annotations to inspect
	 * @return all categories marked in the list
	 */
	public static ArrayList<String> getAllCategories(List<Annotation> list) {
		ArrayList<String> ret = new ArrayList<>();
		if (list.size() == 0)
			return ret;

		for (Annotation a : list)
			if (!ret.contains(a.getCategory().toString()))
				ret.add(a.getCategory().toString());
		return ret;
	}

	/**
	 * Looks at the list, and takes all annotations of a category indicated by
	 * the string input
	 * 
	 * @param annotations
	 *            list to decategorize from input
	 * @param id2
	 *            name of category annotations no longer belong to
	 */
	public static void removeAllTraceOfCategroy(ArrayList<Annotation> annotations, String id2) {
		for (Annotation a : annotations) {
			if (a.getCategory().toString().equals(id2)) {
				a.setCategory(Category.UNTAGGED.toString());
				a.fireListener();
			}
			Annotation.removeAllTraceOfCategroy(a.getSubannotes(), id2);
		}
	}

	/**
	 * Given a list of Annotations convert all Category tags of oldT to newT.
	 * 
	 * @param oldT
	 *            the old category tag name
	 * @param newT
	 *            the new Category tag name
	 * @param list
	 *            the list of annotations to inspect
	 */
	public static void renameTag(String oldT, String newT, ArrayList<Annotation> list) {
		for (Annotation a : list)
			if (a.getCategory().toString().equals(oldT)) {
				a.setCategory(newT);
				a.fireListener();

			}
	}

	/**
	 * Extra Space Buffer for when extending & clipping so sub annotations don't
	 * end up being the same size as parent annotations in the occasional odd
	 * input where such a case might occur otherwise.
	 */
	private static final int EXS = 6;

	/**
	 * the ratio to which the area of intersect of two annotations can be and
	 * still be considered the same annotation, the default is 0.8 but the user
	 * can redefine it
	 */
	public static double min_diff = 0.8;

	/**
	 * Extend the annotation to fit features extending beyond the main feature
	 * (ie the nose is listed as outside the range of the face); then crop the
	 * annotation to fit the image (and its sub features if needed).
	 * 
	 * @param a
	 *            the annotation to clip
	 * @param frame
	 *            maximal boundary of image used to clip annotations down
	 */
	public static void extendAndClip(Annotation a, Rectangle2D.Double frame) {
		// bound of image
		for (Annotation n : a.subannotes)
			extendParent(n);
		clip(a, frame);
		a.fireListener();
	}

	/**
	 * Crops an Annotation to fit within the given frame, will remove
	 * Annotations that don't fit/ fall outside the bounds of the cropped image
	 * 
	 * @param a
	 *            the annotation to crop
	 * @param frame
	 *            the frame to fit the annotation to.
	 */
	private static void clip(Annotation a, Double frame) {
		// adjust start point of rectangle horizontally
		double shift;
		if (a.getMinX() < frame.getMinX()) {
			shift = frame.x - a.x;
			a.x = frame.x;
			// crop the width down to size
			a.width += shift;
		}
		// adjust start point of rectangle vertically
		if (a.getMinY() < frame.getMinY()) {
			shift = frame.y - a.y;
			a.y = 0;
			// crop the height down to size
			a.height += shift;
		}
		// adjust width to fit bound of frame
		if (a.getMaxX() > frame.getMaxX()) {
			a.width += frame.width - a.getEnd().x;
		}

		// adjust height to fit bound of frame
		if (a.getMaxY() > frame.getMaxY()) {
			a.height += frame.height - a.getEnd().y;
		}

		Iterator<Annotation> itr = a.subannotes.iterator();
		while (itr.hasNext()) {
			Annotation note = itr.next();
			if (!a.contains(note))
				itr.remove();
			clip(note, a);
		}
	}

	/**
	 * Makes sure the parent contains the child by increasing the size of the
	 * parent by extending out by EXS pixels pixels beyond the child if the
	 * child is outside the parent
	 * 
	 * @param child
	 *            is held by the parent
	 */
	static void extendParent(Annotation child) {
		if (child.parent == null)
			return;

		for (Annotation note : child.subannotes)
			extendParent(note);

		// roll back x cord to fit child annotation
		if (child.parent.getMinX() > child.getMinX()) {
			child.parent.x -= (child.parent.getMinX() - child.getMinX() + EXS);
		}

		// roll back y cord to fit child annotation
		if (child.parent.getMinY() > child.getMinY()) {
			child.parent.y -= (child.parent.getMinY() - child.getMinY() + EXS);
		}

		// extend parent if child still extends past boundary horizontally
		if (child.parent.getMaxX() < child.getMaxX())
			child.parent.width += (child.getMaxX() - child.parent.getMaxX()) + EXS;

		// extend parent if child still extends past boundary vertically
		if (child.parent.getMaxY() < child.getMaxY())
			child.parent.width += (child.getMaxY() - child.parent.getMaxY()) + EXS;

	}

	/**
	 * Confirm if a point is within a given buffer distance from the boarder of
	 * this annotation.
	 * <p>
	 * This is done by defining two Rectangular shapes that serve as an inner
	 * and outer boarder for the area that is within the given buffer distance
	 * of this Annotation.
	 * <p>
	 * If a given point is within the given buffer distance from this
	 * annotation, it will then be inside the outer Rectangular shape, but not
	 * inside the inner Rectangular shape, which means check that only the outer
	 * shape contains the point confirms its within the buffer distance of this
	 * Annotation.
	 * 
	 * @param pt
	 *            the point to check
	 * @param buffer
	 *            distance from the boarder the pt can be and still return true
	 * @return true if the pt is within the buffer's distance from the boarder
	 */
	public boolean withinBufferRange(Point2D.Double pt, int buffer) {
		// assume its not within the buffer range
		boolean ret = false;
		// the boarder's of the buffer range
		RectangularShape inner, outer;
		Point2D.Double s = getStart();
		int crop = 2 * buffer;

		// define the inner and outer boarders of the buffer range
		if (this.id.isRectangle()) {
			inner = new Rectangle2D.Double(s.x + buffer, s.y + buffer, this.width - crop, this.height - crop);
			outer = new Rectangle2D.Double(s.x - buffer, s.y - buffer, this.width + crop, this.height + crop);
		} else {
			inner = new Ellipse2D.Double(s.x + buffer, s.y + buffer, this.width - crop, this.height - crop);
			outer = new Ellipse2D.Double(s.x - buffer, s.y - buffer, this.width + crop, this.height + crop);
		}
		ret = !inner.contains(pt) && outer.contains(pt);
		return ret;
	}

	/**
	 * @return the start point of this Annotation relative to its image origin
	 */
	public Point2D.Double getStart() {
		return new Point2D.Double(this.x, this.y);
	}

	/** @return the end point of this Annotation relative to its image origin */
	public Point2D.Double getEnd() {
		Point2D.Double ret = getStart();
		ret.x += getWidth();
		ret.y += getHeight();
		return ret;
	}

	/**
	 * 
	 * @param p
	 *            the new Start Point of this Annotation (relative to its image
	 *            origin)
	 */
	void setStart(Point2D.Double p) {
		this.x = p.x;
		this.y = p.y;
		this.fireListener();
	}

	/**
	 * @param p
	 *            the new End Point of this Annotation (relative to its image
	 *            origin)
	 */
	void setEnd(Point2D.Double p) {
		Point2D.Double s = getStart();
		this.width = p.x - s.x;
		this.height = p.y - s.y;
		this.fireListener();
	}

	/**
	 * sets the Annotation's boundaries based on the two given points, with the
	 * confine that the annotation cannot leave its parent if a parent exists
	 * 
	 * @param pt1
	 *            one point to define the rectangle
	 * @param pt2
	 *            another point to define the rectangle
	 * @param frame
	 *            the boundary this Annotation cannot exceed
	 */
	public void setRect(Point2D.Double pt1, Point2D.Double pt2, Rectangle2D.Double frame) {
		Point2D.Double s = new Point2D.Double(Math.min(pt1.x, pt2.x), Math.min(pt1.y, pt2.y));
		Point2D.Double e = new Point2D.Double(Math.max(pt1.x, pt2.x), Math.max(pt1.y, pt2.y));
		// must stay inside parent

		if (s.x < frame.x)
			s.x = frame.x;

		if (s.y < frame.y)
			s.y = frame.y;

		if (e.x > frame.getMaxX())
			e.x = frame.getMaxX();

		if (e.y > frame.getMaxY())
			e.y = frame.getMaxY();

		setStart(s);
		setEnd(e);
		this.fireListener();
	}

	/**
	 * Iterates through the list of annotations looking to see if the given
	 * point is within the buffer's error margin of an annotation
	 * 
	 * @param p
	 *            the point to look for annotations around
	 * @param buffer
	 *            the error margin to search for annotations within
	 * @param list
	 *            the list of annotations to search inside
	 * @return the first annotation near the buffer found (or null if none
	 *         exist)
	 */
	static public Annotation nearAnnotation(Point2D.Double p, int buffer, ArrayList<Annotation> list) {
		Annotation ret = null;
		for (Annotation a : list) {
			for (Annotation subnote : a.getSubannotes()) {
				if (ret == null && subnote.withinBufferRange(p, buffer))
					ret = subnote;
				else
					subnote.isDragEdgeVisible = false;
			}
			if (ret == null && a.withinBufferRange(p, buffer))
				ret = a;
			else
				a.isDragEdgeVisible = false;
		}
		if (ret != null) {
			ret.isDragEdgeVisible = true;
		}
		return ret;
	}

	/**
	 * Iterate through the list of annotations and return the first annotation
	 * found that contains the given point
	 * 
	 * @param pt
	 *            the point to search for Annotations at
	 * @param list
	 *            the list of Annotations to search for the point in.
	 * @return the first Annotation that contains the point (or null if none
	 *         exist)
	 */
	static public Annotation getAnnotationAtPoint(Point2D.Double pt, ArrayList<Annotation> list) {
		Annotation ret = null;
		for (Annotation a : list) {
			ret = getAnnotationAtPoint(pt, a.subannotes);
			if (ret != null)
				return ret;
			if (a.contains(pt))
				return a;
		}
		return ret;
	}

	/**
	 * Provide a list of all annotations that overlap a given point
	 * 
	 * @param pt
	 *            the point to look for annotations at
	 * @param list
	 *            a source list of possible annotations, only those in the list
	 *            can potentially be returned
	 * @return the list of annotation at the given point
	 */
	static public List<Annotation> getAnnotationsAtPoint(Point2D.Double pt, ArrayList<Annotation> list) {
		ArrayList<Annotation> arr = new ArrayList<>();
		for (Annotation a : list) {
			if (a.contains(pt))
				arr.add(a);
			arr.addAll(getAnnotationsAtPoint(pt, a.subannotes));
		}
		return arr;

	}

	/**
	 * Translate the annotation a distance indicated by the given point
	 * 
	 * @param dist
	 *            more a vector than an actual point this tells how far in the x
	 *            and y direction to move this Annotation
	 * @param frame
	 *            boundary that Annotation can't be translated outside of
	 */
	public void translate(Point2D.Double dist, Rectangle2D.Double frame) {
		// translate the distance
		this.x += dist.x;
		this.y += dist.y;

		// whatever extra space the translation overflowed when crossing the
		// frame
		double temp;
		if (this.x < frame.x) {
			temp = frame.x - this.x;
			this.x = frame.x;
			if (dist.x < 0)
				dist.x += temp;
			else
				dist.x -= temp;
		}
		if (this.y < frame.y) {
			temp = frame.y - this.y;
			this.y = frame.y;
			if (dist.y < 0)
				dist.y += temp;
			else
				dist.y -= temp;

		}
		if (this.getMaxX() > frame.getMaxX()) {
			temp = this.getMaxX() - frame.getMaxX();
			this.x = frame.getMaxX() - this.width;
			if (dist.x < 0)
				dist.x += temp;
			else
				dist.x -= temp;
		}
		if (this.getMaxY() > frame.getMaxY()) {
			temp = this.getMaxY() - frame.getMaxY();
			this.y = frame.getMaxY() - this.height;
			if (dist.y < 0)
				dist.y += temp;
			else
				dist.y -= temp;
		}
		for (Annotation note : getSubannotes())
			note.translate(dist, this);
		this.fireListener();
	}

	/** @return the lower left corner of the rectangle */
	public Point2D.Double lowerLeft() {
		return new Point2D.Double(getStart().x, getEnd().y);
	}

	/** @return the upper right corner of the rectangle */
	public Point2D.Double upperRight() {
		return new Point2D.Double(getEnd().x, getStart().y);
	}

	/**
	 * @return the four corners of the rectangle in the order: upper left, upper
	 *         right, lower left, lower right
	 */
	public Point2D.Double[] getCorners() {
		return new Point2D.Double[] { getStart(), upperRight(), getEnd(), lowerLeft() };
	}

	/*** @return the breed of the given annotation */
	public Category getBreed() {
		return this.breed;
	}

	/**
	 * @param update
	 *            the breed to set the annotation to
	 */
	public void setBreed(String update) {
		this.breed.setIDString(update);
		this.fireListener();
	}

	/**
	 * Calculate the distance from the nearest corner.
	 * 
	 * @param pt
	 *            the point
	 * @return the distance the point is from the nearest edge of the annotation
	 *         shape.
	 */
	public double nearestCornerDistanceSquared(Point2D.Double pt) {
		double x = this.x;
		double y = this.y;

		double X = pt.x;
		double Y = pt.y;
		// component's
		double mX = (x - X), x1 = mX * mX, mXX = mX + this.width, x2 = mXX * mXX, mY = (y - Y), y1 = mY * mY,
				mYY = mY + this.height, y2 = mYY * mYY;
		// upper left
		double res = x1 + y1;
		// lower left
		double resPos = x1 + y2;
		if (resPos < res)
			res = resPos;

		// upper right
		resPos = x2 + y1;
		if (resPos < res)
			res = resPos;
		// lower right
		resPos = x2 + y2;
		if (resPos < res)
			res = resPos;
		return res;
	}

	/**
	 * @author bonifantmc
	 *
	 */
	public interface AnnotationChangeListener {
		/** fire when an annotation changes */
		public void onAnnotationChange();
	}

	/**
	 * @param l
	 *            the listener to add
	 */
	public void addAnnotationListener(AnnotationChangeListener l) {
		if (!this.listeners.contains(l))
			this.listeners.addElement(l);
		return;
	}

	/**
	 * @param l
	 *            the listener to remove
	 */
	public void removeListener(AnnotationChangeListener l) {
		while (this.listeners.contains(l))
			this.listeners.remove(l);

	}

	/**
	 * activate listeners.
	 */
	public synchronized void fireListener() {
		for (AnnotationChangeListener l : this.listeners)
			l.onAnnotationChange();
	}

	/** @return the Skin of this Annotation */
	public Attribute getSkin() {
		return this.skin;
	}

	/**
	 * @param values
	 *            the new Skin for this Annotation
	 */
	public void setSkin(String... values) {
		this.skin.setState(values);
		this.fireListener();
	}

	/**
	 * @return the current Gender of this Annotation
	 */
	public Attribute getGender() {
		return this.gender;
	}

	/**
	 * @param values
	 *            the new Gender of this Annotation
	 */
	public void setGender(String... values) {
		this.gender.setState(values);
		this.fireListener();
	}

	/**
	 * @return the current Age of this Annotation
	 */
	public Attribute getAge() {
		return this.age;
	}

	/**
	 * @param values
	 *            the new Age of this Annotation
	 */
	public void setAge(String... values) {
		this.age.setState(values);
		this.fireListener();
	}

	/**
	 * @return the type of wound the annotation has
	 */
	public Attribute getWound() {
		return this.wound;
	}

	/***
	 * 
	 * @param values
	 *            the attribute to set
	 */
	public void setWound(String... values) {
		this.wound.setState(values);
		this.fireListener();
	}

	/**
	 * @return the type of glasses the annotation has
	 */
	public Attribute getOcclusions() {
		return this.occlusions;
	}

	/**
	 * @param values
	 *            the attribute to set
	 */
	public void setOcclusions(String... values) {
		this.occlusions.setState(values);
		this.fireListener();

	}

	/**
	 * @return the AnimalType of the annotation
	 */
	public Attribute getKind() {
		return this.kind;
	}

	/**
	 * @param values
	 *            the attribute to set
	 */
	public void setKind(String... values) {
		this.kind.setState(values);
		this.fireListener();
	}

	/** @return the pitch */
	public float getPitch() {
		return this.pitch;
	}

	/**
	 * @param p
	 *            the pitch rotation of the annotation
	 */
	public void setPitch(float p) {
		if (p == pitch)
			return;
		this.pitch = p;
		this.fireListener();

	}

	/** @return the roll */
	public float getRoll() {
		return this.roll;
	}

	/**
	 * @param r
	 *            the roll rotation of the annotation
	 */
	public void setRoll(float r) {
		if (r == roll)
			return;
		this.roll = r;
		this.fireListener();

	}

	/** @return the yaw */
	public float getYaw() {
		return this.yaw;
	}

	/**
	 * @param y
	 *            the yaw rotation of the annotation
	 */
	public void setYaw(float y) {
		if (y == yaw)
			return;
		this.yaw = y;
		this.fireListener();
	}

	/** @return if this annotation is selected */
	public boolean isSelected() {
		return this.isSelected;
	}

	/**
	 * @param b
	 *            true if the annotation is selected, false otherwise
	 **/
	public void setSelected(boolean b) {
		this.isSelected = b;
	}

	/**
	 * 
	 * @param as
	 *            the AttributeSet of rules to seek
	 * @return the attribute for the given attribute set, null if the set DNE
	 */
	public Attribute getAttribute(AttributeSet as) {
		switch (as.characteristic) {
		case "Gender":
			return this.gender;
		case "Age":
			return this.age;
		case "Skin Tone":
			return this.skin;
		case "Occlusions":
			return this.occlusions;
		case "Wounds":
			return this.wound;
		default:
			return null;
		}

	}

	/**
	 * Attempts to parse a string into the roll/yaw/pitch of an image
	 * 
	 * @param s
	 *            the string to parse
	 * @return true if it parsed into the rotation angles
	 */
	private boolean parseRotation(String s) {
		Matcher m = ROTATION_PATTERN.matcher(s);

		if (m.matches()) {
			setRoll(java.lang.Float.parseFloat(m.group(1)));
			setPitch(java.lang.Float.parseFloat(m.group(2)));
			setYaw(java.lang.Float.parseFloat(m.group(3)));
		}

		return false;
	}

	/***
	 * Add all annotations from list 2 to list 1, that aren't duplicates
	 * 
	 * @param list1
	 *            add annotations to this list
	 * @param list2
	 *            if the annotations in this list aren't duplicates of those in
	 *            the first, add them to the new list
	 */
	public static void addAllNonDuplicates(ArrayListModel<Annotation> list1, ArrayList<Annotation> list2) {
		for (Annotation a : list2) {
			boolean dup = false;
			// check for duplicates,
			for (Annotation b : list1) {
				dup |= areDuplicates(a, b);
				// if it's a duplicate, but has more info, replace it
				// TODO implement a merge instead of a replace.
				if (dup) {
					if (a.getSubannotes().size() > b.getSubannotes().size()) {
						list1.remove(b);
						list1.add(a);
					}
					break;
				}

			}
			// not a duplicate add to list
			if (!dup)
				list1.add(a);
		}

	}

	/**
	 * Checks if the two annotations are the same, by id & boundaries
	 * 
	 * @param a
	 *            the first annotation to compare
	 * @param b
	 *            the second annotation to compare a with
	 * @return true if the annotations are the same Feature with the same bounds
	 */
	private static boolean areDuplicates(Annotation a, Annotation b) {
		return a.getId() == b.getId() && a.x == b.x && a.y == b.y && a.width == b.width && a.height == b.height;
	}

}