/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.formulas;

import org.eclipse.emf.common.util.EList;
import org.eventb.emf.core.AbstractExtension;
import org.eventb.emf.formulas.BFormula;

public class ExtensionHelper {

	/**
	 * Searches for a {@link BFormula} extension in the list and returns the
	 * first one found as the expected type <code>T</code>. Misuse will cause a
	 * {@link ClassCastException} to be thrown. If no matching extension element
	 * is found <code>null</code> is returned.
	 * 
	 * @param <T>
	 * @param extensions
	 * @return Formula casted to type <code>T</code> or <code>null</code> if no
	 *         matching one is found.
	 * @throws ClassCastException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends BFormula> T getFormula(
			final EList<AbstractExtension> extensions)
			throws ClassCastException {
		for (final AbstractExtension extension : extensions) {
			if (extension instanceof BFormula) {
				return (T) extension;
			}
		}

		return null;
	}
}
