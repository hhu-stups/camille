/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.outline;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.editors.text.TextEditor;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.context.ContextPackage;
import org.eventb.emf.core.machine.MachinePackage;
import org.eventb.texttools.model.texttools.TextRange;

public class RevealSelectionListener implements ISelectionChangedListener {
	private final INavigationProvider machineNavProvider = new MachineNavigationProvider();
	private final INavigationProvider contextNavProvider = new ContextNavigationProvider();
	private final TextEditor editor;

	public RevealSelectionListener(final TextEditor editor) {
		this.editor = editor;
	}

	public void selectionChanged(final SelectionChangedEvent event) {
		final ISelection selection = event.getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}

		final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		if (structuredSelection.size() != 1) {
			return;
		}

		final Object firstElement = structuredSelection.getFirstElement();
		if (!(firstElement instanceof EventBObject)) {
			return;
		}

		highlightElement((EventBObject) firstElement);
	}

	private void highlightElement(final EventBObject element) {
		TextRange range = null;

		final String packageNsURI = element.eClass().getEPackage().getNsURI();

		if (packageNsURI.equals(MachinePackage.eNS_URI)) {
			range = machineNavProvider.getHighlightRange(element);
		} else if (packageNsURI.equals(ContextPackage.eNS_URI)) {
			range = contextNavProvider.getHighlightRange(element);
		}

		if (range != null) {
			editor.selectAndReveal(range.getOffset(), range.getLength());
		}
	}
}
