/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class TextDecoration {

	public enum ESyntaxElement {
		Comment("Comment"), Label("Label"), Keyword("Keyword"), MathKeyword(
				"Mathematical Keyword"), GlobalVariable("Global Variable"), Parameter(
				"Event Parameter"), BoundedVariable("Bound Variable"), Constant(
				"Constant"), Set("Carrier Set");

		private final String label;
		private final String colorKey;
		private final String styleKey;
		private final Token token = new Token(null);

		private ESyntaxElement(final String label) {
			this.label = label;

			colorKey = toString() + "." + "colorPreference";
			styleKey = toString() + "." + "stylePreference";
		}

		public String getLabel() {
			return label;
		}

		public String getColorKey() {
			return colorKey;
		}

		public String getStyleKey() {
			return styleKey;
		}

		public synchronized Token getToken() {
			if (token.getData() == null) {
				token.setData(getAttribute(this));
			}

			return token;
		}

		public synchronized void resetToken() {
			token.setData(getAttribute(this));
		}
	};

	private final static IPreferenceStore store = TextEditorPlugin.getPlugin()
			.getPreferenceStore();

	private static TextAttribute getAttribute(final ESyntaxElement element) {
		final RGB color = PreferenceConverter.getColor(store, element
				.getColorKey());
		final int style = store.getInt(element.getStyleKey());

		final TextAttribute attribute = new TextAttribute(new Color(Display
				.getDefault(), color), null, style);
		return attribute;
	}
}
