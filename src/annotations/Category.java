package annotations;

import java.util.List;
import java.util.regex.Pattern;

import struct.AnnotationGroups;

/**
 * Category Attributes mark an Annotation as being something more than just what
 * the Feature id of Annotation indicates. It gives a specific label, like
 * "Larry" or "Subject1". This is little more than an over-glorified string
 * wrapper in honesty.
 * 
 * @author bonifantmc
 *
 */
public class Category {
	/** A Pattern using this class's regex() method to matche Strings */
	static public final Pattern pattern = Pattern.compile(regex());
	/** A constant Category for untagged images */
	static public final Category UNTAGGED = new Category(AnnotationGroups.UNTAGGED, 'd');
	/** A constant Category unknown breeds */
	static public final Category BREED_UNKNOWN = new Category("unknown", 'b');
	/**
	 * Whatever this annotation is being marked as ( "Larry", "Pontiac Grand Am"
	 * , "Crumpled Oak Leaf"
	 */
	private String category;

	/**
	 * the choice of default character id/feature tag, default is 'd' for 'id'
	 * for identification
	 */
	final char id;

	/**
	 * Makes a free text attribute
	 * 
	 * @param s
	 *            a string marking what this category is of
	 * @param c
	 *            character mark for the attribute's id
	 */
	public Category(String s, Character c) {
		setIDString(s);

		if (c == null)
			c = 'd';
		this.id = c;
	}

	/**
	 * @return a regular expression for matching Age Attributes
	 */
	static public String regex() {
		return "[A-Za-z]\\[.*\\]";
	}

	/**
	 * Basic parser for getting an Age from a string
	 * 
	 * @param s
	 *            the string to parse
	 * @return Age that matches the input * @throws IllegalArgumentException if
	 *         the String doesn't match anything
	 */
	public static Category getAttribute(String s) {
		if (s.matches(regex()))
			return new Category(s.substring(2, s.length() - 1), s.charAt(0));
		throw new IllegalArgumentException();
	}

	/**
	 * 
	 * @return the formatted String required for writting this Attribute to a
	 *         lst file
	 */
	public String getID() {
		return this.id + "[" + toString() + "]";

	}

	/**
	 * @param category
	 *            value to set category to
	 */
	public void setIDString(String category) {

		if (category != null && category.equals(""))
			throw new IllegalArgumentException();
		this.category = category;
	}

	/**
	 * tells if category is exact same object, or at least same string
	 * 
	 * @return this == o || (o instanceof Category && this.category
	 *         .equals(((Category) o).category));
	 */
	@Override
	public boolean equals(Object o) {
		if (this.category == null)
			return false;
		if (!(o instanceof Category))
			throw new ClassCastException();

		return this.category.toString().equals(((Category) o).category.toString()) && this.id == ((Category) o).id;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * A string representation of the category (ie: this.category, in comparison
	 * to the Id String which is "id[(toString()]"
	 */
	@Override
	public String toString() {
		return this.category;
	}

	/**
	 * Test if a list of Categories contains a given object. Has to be rewritten
	 * from the default list.contains to treat Strings as Categories, because
	 * the default comparison does cati == oj || oj.equals(cati), which will
	 * fail since categories aren't Strings. This switches the object's place so
	 * its cati.equals(oj) in the comparison line.
	 * 
	 * @param list
	 *            the list to iterate over
	 * @param o
	 *            the object to see if the list contains
	 * @return true if the list contains a category representing the object
	 *         given
	 */
	static public boolean contains(List<? extends Category> list, Object o) {
		for (Category cat : list)
			if (cat == o || cat.equals(o))
				return true;
		return false;

	}
}