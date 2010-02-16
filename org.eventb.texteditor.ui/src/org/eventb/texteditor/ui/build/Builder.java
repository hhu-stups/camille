/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IDocument;
import org.eventb.texteditor.ui.build.ast.AstBuilder;
import org.eventb.texteditor.ui.build.dom.DomBuilder;
import org.eventb.texteditor.ui.editor.EventBTextEditor;

public class Builder {
	private final List<IBuildPhase> buildPhases = new ArrayList<IBuildPhase>();
	private boolean successful;

	public Builder() {
		buildPhases.add(new AstBuilder());
		buildPhases.add(new DomBuilder());
	}

	public void run(final EventBTextEditor editor, final IDocument document,
			final IProgressMonitor monitor) {
		final Resource resource = editor.getResource();

		final SubMonitor subMonitor = SubMonitor.convert(monitor, "Building '"
				+ resource + "'...", buildPhases.size());
		successful = true;

		for (final IBuildPhase phase : buildPhases) {
			if (subMonitor.isCanceled()) {
				return;
			}

			phase.run(editor, resource, document, subMonitor.newChild(1));

			if (phase.canFail() && !phase.wasSuccessful()) {
				successful = false;
				return;
			}
		}
	}

	public boolean wasSuccessful() {
		return successful;
	}
}
