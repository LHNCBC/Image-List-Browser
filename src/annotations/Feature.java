package annotations;

import java.awt.Color;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * The types of annotations that a .lst file can contain: Face, Profile, Skin,
 * Eyes, Nose, and Mouth. Each type has an associated character symbol for
 * printing in the annotations and a color for use in painting the annotation.
 * <p>
 * Features id an Annotation in its most general sense, and explain what an
 * annotations' bounding rectangle marks.
 * 
 * @author bonifantmc
 * 
 */
public enum Feature {
	// String sym, Color c, boolean human, boolean animal, boolean subfeature,
	// boolean square
	// Human Features
	/** Faces are blue and drawn as ovals */
	Face("f", Color.BLUE, FeatureFlags.HUMAN | FeatureFlags.ELLIPSE),
	/** Profiles are read and drawn as rectangles */
	Profile("p", Color.RED, FeatureFlags.HUMAN | FeatureFlags.RECTANGLE),
	/** Skin is magenta and drawn as rectangles */
	Skin("s", Color.MAGENTA, FeatureFlags.HUMAN | FeatureFlags.RECTANGLE),
	//
	// human facial features
	//
	/** Eyes are yellow and drawn as ovals */
	Eyes("i", Color.YELLOW, FeatureFlags.FACE_FEATURES | FeatureFlags.ELLIPSE | FeatureFlags.SUB_FEATURE),
	/** Noses are cyan and drawn as rectangles */
	Nose("n", Color.CYAN, FeatureFlags.FACE_FEATURES | FeatureFlags.RECTANGLE | FeatureFlags.SUB_FEATURE),
	/** Mouths are green and drawn as rectangles */
	Mouth("m", Color.GREEN, FeatureFlags.FACE_FEATURES | FeatureFlags.RECTANGLE | FeatureFlags.SUB_FEATURE),
	/** Ears are Pink and drawn as Rectangles */
	Ear("e", Color.PINK, FeatureFlags.FACE_FEATURES | FeatureFlags.ELLIPSE | FeatureFlags.SUB_FEATURE),
	//
	// Animal Features
	//
	/** an entire Animal, bounded by an orange box */
	Animal("a", Color.ORANGE, FeatureFlags.ANIMAL | FeatureFlags.RECTANGLE),
	//
	// Animal sub- Features
	//
	/** Body/torso of an animal, a dark gray box */
	Body("b", Color.DARK_GRAY, FeatureFlags.ANIMAL_FEATURES | FeatureFlags.SUB_FEATURE | FeatureFlags.RECTANGLE),
	/**
	 * Head of an animal (contains same facial features as humans) a gray box
	 */
	Head("h", Color.GRAY, FeatureFlags.ANIMAL_FEATURES | FeatureFlags.SUB_FEATURE | FeatureFlags.RECTANGLE | FeatureFlags.FACE_CONTAINING),
	/** Tail of an animal, a light grey box */
	Tail("t", Color.LIGHT_GRAY, FeatureFlags.ANIMAL_FEATURES | FeatureFlags.SUB_FEATURE | FeatureFlags.RECTANGLE),
	/** Leg of an animal, a white box */
	Leg("l", Color.WHITE, FeatureFlags.ANIMAL_FEATURES | FeatureFlags.SUB_FEATURE | FeatureFlags.RECTANGLE);

	/**
	 * the color of a feature that's been selected, for display purposes (it's
	 * currently a dark purple, 80,0,80 on the RGB scale
	 */
	public static final Color COLOR_SELECTED = new Color(0x80, 0x00, 0x80);
	/**
	 * This is the character string that is printed when an annotation is
	 * written to a .lst file
	 */
	private String s;

	/** a flag defining traits of the feature */
	int flag = 0;

	/** the color to draw the feature with */
	private Color c;

	/**
	 * @param sym
	 *            the character symbol that each Feature has.
	 * @param color
	 *            the color to draw the Feature as.
	 * @param f
	 *            the flag defining the Feature's feature is
	 *            (rectangle/animal/face... statements).
	 */
	Feature(String sym, Color color, int f) {
		this.s = sym;
		this.c = color;
		this.flag = f;
	}

	/**
	 * @return the character string used in printing annotations
	 */
	public String getIdString() {
		return this.s;
	}

	/**
	 * @return The color that this feature should use for drawing.
	 */
	public Color getColor() {
		return this.c;
	}

	/**
	 * Return an Feature based on the first letter of the submitted string. If
	 * the string matches either the s of an Feature or an Feature's toString()
	 * this method returns that Feature.
	 * <p>
	 * e.g.:"f" and "Face" will return AnnoationType.Face, but "face" will not.
	 * <p>
	 * Anything that does not match the strings will return null by default
	 * 
	 * @param string
	 *            the string to parse to an Feature
	 * @return the Feature specified by the string.
	 */
	public static Feature parseFeature(String string) {
		if (string == null)
			return Face;
		switch (string) {
		case "p":
		case "P":
		case "Profile":
			return Profile;
		case "n":
		case "N":
		case "Nose":
			return Nose;
		case "m":
		case "M":
		case "Mouth":
			return Mouth;
		case "i":
		case "I":
		case "Eyes":
			return Eyes;
		case "s":
		case "S":
		case "Skin":
			return Skin;
		case "e":
		case "E":
		case "Ear":
			return Ear;
		case "b":
		case "B":
		case "Body":
			return Body;
		case "h":
		case "H":
		case "Head":
			return Head;
		case "l":
		case "L":
		case "Leg":
			return Leg;
		case "t":
		case "T":
		case "Tail":
			return Tail;
		case "a":
		case "A":
		case "Animal":
			return Animal;
		case "f":
		case "F":
		case "Face":
			return Face;
		default:
			return null;
		}
	}

	/** @return list of all characters that might identify a Feature */
	public static List<String> charValues() {
		Feature[] values = values();
		String[] s = new String[values.length * 2];
		for (int i = 0; i < values.length * 2; i += 2) {
			s[i] = values[i / 2].s;
			s[i + 1] = values[i / 2].s.toUpperCase();

		}
		return Arrays.asList(s);
	}

	/** @return list of animal features */
	public static Feature[] animalValues() {
		Feature[] v = values();
		List<Feature> l = Arrays.asList(v);
		Iterator<Feature> i = l.iterator();
		Feature f;
		while (i.hasNext()) {
			f = i.next();
			if (!f.isAnimal())
				i.remove();
		}
		return (Feature[]) l.toArray();
	};

	/** @return list of human features */
	public static Feature[] humanValues() {
		Feature[] v = values();
		List<Feature> l = Arrays.asList(v);
		Iterator<Feature> i = l.iterator();
		Feature f;
		while (i.hasNext()) {
			f = i.next();
			if (!f.isHuman())
				i.remove();
		}
		return (Feature[]) l.toArray();
	}

	/** @return true if the feature is a sub_feature */
	public boolean isSubfeature() {
		return (this.flag & FeatureFlags.SUB_FEATURE) == FeatureFlags.SUB_FEATURE;
	}

	/** @return true if the feature is rectangular ellipse */
	public boolean isRectangle() {
		return (this.flag & FeatureFlags.RECTANGLE) == FeatureFlags.RECTANGLE;
	}

	/** @return true if the feature is human */
	public boolean isHuman() {

		return (this.flag & FeatureFlags.HUMAN) == FeatureFlags.HUMAN;

	}

	/** @return true if the feature is animalistic */
	public boolean isAnimal() {
		return (this.flag & FeatureFlags.ANIMAL) == FeatureFlags.ANIMAL;
	}

	/** @return true if the feature contains facial features */
	public boolean isFacialFeatureContaining() {
		return (this.flag & FeatureFlags.FACE_CONTAINING) == FeatureFlags.FACE_CONTAINING;
	}

	/** @return true if the feature is a facial feature */
	public boolean isFacialFeature() {
		return (this.flag & FeatureFlags.FACE_FEATURES) == FeatureFlags.FACE_FEATURES;
	}

	/** @return true if the feature is one found in animalistic features */
	public boolean isAnimalFeature() {
		return (this.flag & FeatureFlags.ANIMAL_FEATURES) == FeatureFlags.ANIMAL_FEATURES;
	}
}
