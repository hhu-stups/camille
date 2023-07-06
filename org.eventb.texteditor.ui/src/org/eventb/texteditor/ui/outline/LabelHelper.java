/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.outline;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eventb.emf.core.EventBNamed;

public class LabelHelper {

	public static final Styler ATTRIBUTE_STYLER = StyledString.QUALIFIER_STYLER;
	public static final String ATTRIBUTE_DELIMITER = " : ";

	public static StyledString getPlainStyledText(final String contents) {
		return new StyledString(contents);
	}

	public static StyledString getStyledName(final String name) {
		return getPlainStyledText(name);
	}

	public static <T> String joinEList(final EList<T> parameters) {
		return String.join(", ", getAsStringList(parameters));
	}

	public static void appendAttrDelim(final StyledString result) {
		result.append(ATTRIBUTE_DELIMITER, ATTRIBUTE_STYLER);
	}

	private static <T> List<String> getAsStringList(final EList<T> parameters) {
		final List<String> result = new ArrayList<String>();

		for (final T element : parameters) {
			if (element instanceof EventBNamed) {
				result.add(((EventBNamed) element).getName());
			} else {
				result.add(element.toString());
			}
		}

		return result;
	}
}
