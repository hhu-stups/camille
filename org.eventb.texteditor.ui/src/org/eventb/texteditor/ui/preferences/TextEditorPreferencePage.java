/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class TextEditorPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {
	public TextEditorPreferencePage() {
		super();
		setDescription("This section provides preferences for the Event-B TextEditor.");
	}

	@Override
	protected void createFieldEditors() {
		// nothing for now
	}

	public void init(final IWorkbench workbench) {
		// nothing for now
	}
}
