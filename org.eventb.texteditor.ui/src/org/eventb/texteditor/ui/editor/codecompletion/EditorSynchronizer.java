/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor.codecompletion;

import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eventb.texteditor.ui.editor.EventBTextEditor;

public class EditorSynchronizer implements ILinkedModeListener {

	private final EventBTextEditor editor;

	public EditorSynchronizer(final EventBTextEditor editor) {
		this.editor = editor;
		editor.setInLinkedMode(true);
	}

	public void left(final LinkedModeModel model, final int flags) {
		editor.setInLinkedMode(false);
	}

	public void resume(final LinkedModeModel model, final int flags) {
		// IGNORE
	}

	public void suspend(final LinkedModeModel model) {
		// IGNORE
	}
}
