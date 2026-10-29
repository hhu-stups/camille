/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
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

		addField(new BooleanFieldEditor(
				TextEditorPlugin.PREF_MATCHING_BRACKETS,
				"Highlight &matching brackets", getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				TextEditorPlugin.PREF_ENCLOSING_BRACKETS,
				"Highlight &enclosing brackets", getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				TextEditorPlugin.PREF_BRACKET_AT_CARET,
				"Highlight the bracket at the &cursor too",
				getFieldEditorParent()));
		addField(new ColorFieldEditor(
				TextEditorPlugin.PREF_MATCHING_BRACKETS_COLOR,
				"Matching brackets &color:", getFieldEditorParent()));

		addField(new BooleanFieldEditor(
				TextEditorPlugin.PREF_MARK_OCCURRENCES,
				"Mark &occurrences of the name at the cursor",
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(TextEditorPlugin.PREF_PROOF_STATUS,
				"Color &labels by the status of their proof obligations",
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(TextEditorPlugin.PREF_FOLDING,
				"Enable &folding (for newly opened editors)",
				getFieldEditorParent()));
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TextEditorPlugin.getPlugin().getPreferenceStore());
	}
}
