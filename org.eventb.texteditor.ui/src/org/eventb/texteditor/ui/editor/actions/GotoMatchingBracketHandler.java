/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.texteditor.ui.editor.EventBTextEditor;

/**
 * Moves the cursor to the matching bracket.
 */
public class GotoMatchingBracketHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) {
		final IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor instanceof EventBTextEditor
				&& !((EventBTextEditor) editor).gotoMatchingBracket()) {
			HandlerUtil.getActiveShell(event).getDisplay().beep();
		}
		return null;
	}
}
