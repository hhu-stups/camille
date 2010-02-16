/*******************************************************************************
 * Copyright (c) 2009 Systerel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Systerel - initial API and implementation
 *******************************************************************************/
package org.eventb.texteditor.ui.editor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.texteditor.ui.editor.EventBTextEditor;

/**
 * Handles insertion command (id="org.eventb.ui.edit.insert").
 * 
 * @author "Nicolas Beauger"
 *
 */
public class InsertHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);

		if (!(activeEditor instanceof EventBTextEditor)) {
			return "Unexpected editor";
		}

		final String insertText = event
				.getParameter("org.eventb.ui.edit.insert.text");
		if (insertText == null) {
			return "Unable to retrieve the text to insert";
		}

		((EventBTextEditor) activeEditor).insert(insertText, true);
		return null;
	}
}
