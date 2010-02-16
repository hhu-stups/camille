/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.outline;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eventb.texteditor.ui.editor.EventBTextEditor;

public class OutlinePage extends ContentOutlinePage {

	private final EventBTextEditor editor;

	public OutlinePage(final EventBTextEditor editor) {
		this.editor = editor;
	}

	@Override
	public void createControl(final Composite parent) {
		super.createControl(parent);

		final TreeViewer viewer = getTreeViewer();
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
				new OutlineLabelProvider()));
		viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);

		viewer.setInput(editor.getEditorInput());
	}
}
