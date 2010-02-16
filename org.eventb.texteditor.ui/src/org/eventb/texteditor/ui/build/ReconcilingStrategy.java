/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eventb.texteditor.ui.editor.EventBTextEditor;

public class ReconcilingStrategy implements IReconcilingStrategy,
		IReconcilingStrategyExtension {

	private IDocument document;
	private IProgressMonitor monitor;
	private final EventBTextEditor editor;

	public ReconcilingStrategy(final EventBTextEditor editor) {
		this.editor = editor;
	}

	public void initialReconcile() {
		monitor.beginTask("Reconciling '" + editor.getEditorInput().getName()
				+ "'", 3);

		if (document != null && !editor.isSaving() && !editor.isInLinkedMode()) {
			new Builder().run(editor, document, monitor);
		}

		monitor.done();
	}

	public void reconcile(final IRegion partition) {
		initialReconcile();
	}

	public void reconcile(final DirtyRegion dirtyRegion, final IRegion subRegion) {
		initialReconcile();
	}

	public void setDocument(final IDocument document) {
		this.document = document;
	}

	public void setProgressMonitor(final IProgressMonitor monitor) {
		if (monitor != null) {
			this.monitor = monitor;
		} else {
			this.monitor = new NullProgressMonitor();
		}
	}
}
