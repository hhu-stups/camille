/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.preferences;

import static org.eclipse.swt.SWT.*;
import static org.eventb.texteditor.ui.TextDecoration.ESyntaxElement.*;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.TextDecoration.ESyntaxElement;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	private IPreferenceStore store;

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
	 * initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		store = TextEditorPlugin.getPlugin().getPreferenceStore();

		setDefaultValues(Comment, COLOR_GRAY, ITALIC);
		setDefaultValues(Label, COLOR_DARK_GRAY, NORMAL);
		setDefaultValues(Keyword, COLOR_DARK_RED, BOLD);
		setDefaultValues(MathKeyword, COLOR_DARK_RED, NORMAL);
		setDefaultValues(GlobalVariable, COLOR_BLUE, NORMAL);
		setDefaultValues(BoundedVariable, COLOR_DARK_GREEN, ITALIC);
		setDefaultValues(Parameter, COLOR_BLUE, ITALIC);
		setDefaultValues(Constant, COLOR_BLUE, BOLD);
		setDefaultValues(Set, COLOR_DARK_GREEN, BOLD);
	}

	private void setDefaultValues(final ESyntaxElement element,
			final int color, final int style) {
		PreferenceConverter.setDefault(store, element.getColorKey(),
				getRGB(color));
		store.setDefault(element.getStyleKey(), style);
	}

	private static RGB getRGB(final int colorCode) {
		return Display.getDefault().getSystemColor(colorCode).getRGB();
	}
}
