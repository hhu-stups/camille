/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eventb.texteditor.ui.TextEditorPlugin;

public class TextEditorPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {
	public TextEditorPreferencePage() {
		super(GRID);
		setDescription("This section provides preferences for the Event-B TextEditor.");
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(TextEditorPlugin.PREF_AUTO_TRANSLATE,
				"&Replace ASCII notation by mathematical symbols while typing "
						+ "(e.g. ':' → '∈')", getFieldEditorParent()));
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TextEditorPlugin.getPlugin().getPreferenceStore());
	}
}
