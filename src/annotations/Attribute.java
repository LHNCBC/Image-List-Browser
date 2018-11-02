package annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every Annotation can have Attributes. The Attribute is a special type of
 * Annotation that modifies an existing Annotation by giving additional
 * information about it.
 * <p>
 * For instance annotations with skin in them may have an Attribute to describe
 * the general tone of the skin.
 * 
 * 
 * @author bonifantmc
 * @author glingappa@mail.nih.gov for gender and age support
 */
public class Attribute {
	/** rules defining Gender */
	public final static AttributeSet Gender = AttributeSet.load("Attributes/Gender.txt");
	/** rules defining Wounds */
	public final static AttributeSet Wounds = AttributeSet.load("Attributes/Wounds.txt");
	/** rules defining Skin Tone */
	public final static AttributeSet SkinTone = AttributeSet.load("Attributes/SkinTone.txt");
	/** rules defining Occlusions */
	public final static AttributeSet Occlusions = AttributeSet.load("Attributes/Occlusions.txt");
	/** rules defining Age */
	public final static AttributeSet Age = AttributeSet.load("Attributes/Age.txt");
	/** rules defining Kind */
	public final static AttributeSet Kind = AttributeSet.load("Attributes/Kind.txt");

	/** String for any Attribute that has not been set */
	public final static String UNMARKED = "unmarked";

	/** a list of Attributes used */
	private List<String> state = new ArrayList<>();
	/** the rules defining this Attribute */
	final private AttributeSet rules;

	/**
	 * 
	 * @param rules
	 *            the rules defining this attribute
	 * @param defaultState
	 *            the default state of an attribute (usually "Unmarked")
	 */
	public Attribute(AttributeSet rules, String defaultState) {
		this.rules = rules;
		this.state.add(defaultState);
	}

	/**
	 * @param rules
	 *            the rules defining the Attribute
	 */
	public Attribute(AttributeSet rules) {
		this(rules, UNMARKED);
	}

	/**
	 * @return the regex for this Attribute's rule set.
	 */
	public String regex() {
		return this.rules.regex();
	}

	@Override
	public String toString() {
		StringJoiner sj = new StringJoiner(",", this.rules.id + "[", "]");
		for (String i : this.state)
			sj.add(i);
		return sj.toString();
	}

	/**
	 * @param values
	 *            the values to set the state to
	 * @return true if the state could be set (ie was valid) otherwise return
	 *         false
	 */
	public boolean setState(String... values) {
		if (values.length == 0)
			values = new String[] { Attribute.UNMARKED };
		if (this.rules.isValidSelection(values)) {
			this.state.clear();
			for (String s : values)
				this.state.add(s);
			return true;
		}
		return false;
	}

	/**
	 * @param s
	 *            the string to parse
	 * @return the attribute created
	 */
	public boolean parseSet(String s) {
		Pattern p = Pattern.compile(this.regex());
		Matcher m = p.matcher(s);
		if (m.matches())
			return this.setState(m.group(1).split(","));
		return false;

	}

	/** @return true if the only state included is UNMARKED */
	public boolean isUnmarked() {
		return this.state.size() == 1 && this.state.get(0).equals(UNMARKED);
	}

	/**
	 * @return an unmodifiable list of Attribute state/values
	 */
	public String[] values() {
		return this.state.toArray(new String[] {});
	}

	/** @return the rules of this Attribute */
	public AttributeSet getRules() {
		return this.rules;
	}

	/**
	 * @return a score for sorting this Attribute, the score is based on the
	 *         index the attribute's state is at in the rules' list of values,
	 *         higher scores are from closer to the top of the list, multiple
	 *         items also yields larger scores.
	 */
	public int score() {
		int score = 0;
		for (String s : this.state)
			score += (this.rules.values().size() - this.rules.values().indexOf(s));
		return score;
	}

	/**
	 * @return a List of all the states the Attribute is in currently.
	 */
	public List<String> getState() {
		return this.state;
	}
}