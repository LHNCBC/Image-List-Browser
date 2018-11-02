package annotations;

/**
 * Flags indicating metadata about specific features.
 * 
 * @author bonifantmc
 *
 */
public class FeatureFlags {
	//
	// Flags
	//
	/** human features are skin, profile, and face */
	static int HUMAN = 1;
	/** animal features are animal */
	static int ANIMAL = HUMAN << 1;
	/** face containing features are face, profile, head, and skin */
	static int FACE_CONTAINING = ANIMAL << 1;
	/** face features are eye, nose, ear, and mouth */
	static int FACE_FEATURES = FACE_CONTAINING << 1;
	/** animal features are head, leg, tail, and body */
	static int ANIMAL_FEATURES = FACE_FEATURES << 1;
	/** Features that are displayed as Rectangles */
	static int RECTANGLE = ANIMAL_FEATURES << 1;
	/** Features that are displayed as Ellipses */
	static int ELLIPSE = RECTANGLE << 1;
	/** true if a feature found within other features */
	static int SUB_FEATURE = ELLIPSE << 1;
}
