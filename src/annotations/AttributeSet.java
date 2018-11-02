package annotations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.StringJoiner;

/**
 * @author bonifantmc
 *
 *         An attribute, is a list of possible features. In annotations they
 *         appear as a character id, followed by a list of selections comma
 *         separated.
 *         <p>
 *         For instance: w[blood,scar,bruise]. could indicate a wound with
 *         blood, scars, and bruising.
 *         <p>
 *         Or: g[male] could indicate a male gendered object.
 *
 *
 *
 */
public class AttributeSet {

	/** an array of possible attributes (ex: {"blood", "scars", "bruising") */
	final private List<String> tags;
	/**
	 * an array of valid states that attributes can be selected in (ex:
	 * {{"blood"}, {"blood", "scars"}, {"scars", "bruising"}})
	 */
	final private String[][] invalidSelections;
	/**
	 * a character id to identify what the attribute is marking (ex: w for wound
	 * or g for gender)
	 */
	char id;

	/**
	 * a string labeling what this AttributeSet is identifying (eg: gender,
	 * wounds, eye color, etc)
	 */
	String characteristic = "Label";

	/**
	 * the number of states that can be selected, anything less than 1 indicates
	 * there is no limit
	 */
	int validLength = -1;

	/**
	 * @param t
	 *            the tags
	 * @param inv
	 *            the invalid selection states
	 * @param i
	 *            the id
	 * @param n
	 *            the name for what this AttributeSet characterizes
	 * @param j
	 *            the length of a valid selection state
	 */
	public AttributeSet(String[] t, String[][] inv, char i, String n, int j) {
		this.id = i;
		this.tags = Collections.unmodifiableList(Arrays.asList(t));
		this.invalidSelections = inv;
		this.characteristic = n;
		this.validLength = j;
	}

	/**
	 * @param s
	 *            string to check if it's a valid attribute
	 * @return true if the attribute is contained in this set.
	 */
	public boolean contains(String s) {
		for (String is : this.tags)
			if (is.equals(s))
				return true;
		return false;
	}

	/**
	 * Checks if a given selection is valid.
	 * 
	 * @param selection
	 *            the state to check
	 * @return true if the state is among the valid selections
	 */
	public boolean isValidSelection(String... selection) {
		// easiest check first, must meet length requirement
		if (this.validLength > 0 && selection.length > this.validLength)
			return false;
		// next easiest check, must actually be a list of states for this
		// attribute
		for (String select : selection)
			if (!contains(select))
				return false;

		// hardest check, must be a valid state for this attribute
		for (String[] vs : this.invalidSelections)
			if (contains(selection, vs))
				return false;
		return true;
	}

	/**
	 * Checks if vs is a subset of selection
	 * 
	 * @param selection
	 *            the selection
	 * @param vs
	 *            a subset to look for
	 * @return true iff vs is a subset of selection
	 */
	private boolean contains(String[] selection, String[] vs) {
		if (vs.length > selection.length)
			return false;
		HashSet<String> s = new HashSet<String>();
		for (String select : selection)
			s.add(select);
		for (String select : vs)
			if (!s.contains(select))
				return false;

		return true;
	}

	/**
	 * @return a regex that can parse the String
	 */
	public String regex() {
		StringBuilder bldr = new StringBuilder();
		bldr.append(this.id);
		bldr.append("\\[(");

		StringJoiner options = new StringJoiner("|");
		for (String selection : this.tags)
			options.add(selection);
		bldr.append("((");
		bldr.append(options.toString());
		bldr.append("),)*");
		bldr.append("(");
		bldr.append(options.toString());
		bldr.append("))\\]");
		return bldr.toString();
	}

	/**
	 * Parses a File into an AttributeSet,
	 * 
	 * A file detailing an AttributeSet can have any number of commented lines
	 * at the start (lines starting with '#').
	 * <p>
	 * After comments the first line must be a single character, the id. (eg: w)
	 * <p>
	 * Then the second line must be a comma separated list of possible tags (eg:
	 * blood,guts,bruises)
	 * <p>
	 * Each of the remaining lines is a colon separated list detailing a valid
	 * state (eg: blood:scar, is the state that accepts scar and blood).
	 * 
	 * @param path
	 *            location of a text file encapsulating the attribute
	 * @return the Attribute
	 */
	public static AttributeSet load(String path) {
		File f = new File(path);
		if (!f.exists() || !f.isFile())
			throw new IllegalArgumentException(f.getAbsolutePath() + " is not a file to read");

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String id = br.readLine();
			while (id.charAt(0) == '#')// skip comments at the start
				id = br.readLine();
			String name = br.readLine();
			String length = br.readLine();
			String tags = br.readLine();
			ArrayList<String> arr = new ArrayList<String>();
			String invalidStates;
			while ((invalidStates = br.readLine()) != null)
				arr.add(invalidStates);

			return parse(name, id, tags, arr.toArray(new String[] {}), length);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * @param n
	 *            the name of the Attribute set
	 * @param i
	 *            the character id
	 * @param t
	 *            the tags, each state is separated by a comma
	 * @param inv
	 *            the invalid states that cannot be allowed
	 * @param length
	 *            the length a selection can be
	 * @return the Attribute these create.
	 */
	public static AttributeSet parse(String n, String i, String t, String[] inv, String length) {
		i = i.trim();
		if (i.length() != 1)
			throw new IllegalArgumentException(i + " is not a character");
		char id = i.charAt(0);

		String[] tags = t.split(",");
		String[][] validStates = new String[inv.length][];
		for (int j = 0; j < inv.length; j++)
			validStates[j] = inv[j].split(":");
		return new AttributeSet(tags, validStates, id, n, Integer.parseInt(length));
	}

	@Override
	public String toString() {
		return this.characteristic;
	}

	/** @return a List of the tags this AttributeSet contains */
	public List<String> values() {
		return this.tags;
	}

	/** @return the valid length of AttributeSet Selection */
	public int getValidLength() {
		return this.validLength;
	}
}
