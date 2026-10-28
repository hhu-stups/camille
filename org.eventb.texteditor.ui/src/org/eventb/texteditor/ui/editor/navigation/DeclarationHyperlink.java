/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.navigation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texteditor.ui.editor.hover.HoverIndex.Declaration;

/**
 * Opens the machine/context that declares a name and selects the
 * declaration.
 */
public class DeclarationHyperlink implements IHyperlink {

	private final EventBTextEditor editor;
	private final IRegion region;
	private final Declaration declaration;

	public DeclarationHyperlink(final EventBTextEditor editor,
			final IRegion region, final Declaration declaration) {
		this.editor = editor;
		this.region = region;
		this.declaration = declaration;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return region;
	}

	@Override
	public String getTypeLabel() {
		return null;
	}

	@Override
	public String getHyperlinkText() {
		return "Open Declaration";
	}

	@Override
	public void open() {
		EventBTextEditor target = editor;

		final IFile file = declaration.file;
		final IFile currentFile = editor.getEditorInput() instanceof IFileEditorInput ? ((IFileEditorInput) editor
				.getEditorInput()).getFile() : null;
		if (file != null && !file.equals(currentFile)) {
			try {
				final IEditorPart part = IDE.openEditor(editor.getSite()
						.getPage(), file, TextEditorPlugin.TEXTEDITOR_ID);
				if (!(part instanceof EventBTextEditor)) {
					return;
				}
				target = (EventBTextEditor) part;
			} catch (final PartInitException e) {
				TextEditorPlugin
						.getPlugin()
						.getLog()
						.log(new Status(IStatus.ERROR,
								TextEditorPlugin.PLUGIN_ID, "Cannot open "
										+ file.getName(), e));
				return;
			}
		}

		final IDocument document = target.getDocumentProvider().getDocument(
				target.getEditorInput());
		if (document == null) {
			return;
		}
		final IRegion found = DeclarationLocator.find(document.get(),
				declaration.kind, declaration.name, declaration.event);
		if (found != null) {
			target.selectAndReveal(found.getOffset(), found.getLength());
		}
	}
}
