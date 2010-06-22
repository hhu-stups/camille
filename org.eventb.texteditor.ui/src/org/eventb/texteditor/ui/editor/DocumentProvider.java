/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.ui.util.EditUIUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.build.Builder;
import org.eventb.texteditor.ui.build.dom.DomManager.ParseResult;
import org.eventb.texteditor.ui.reconciler.partitioning.PartitionScanner;
import org.eventb.texteditor.ui.reconciler.partitioning.Partitioner;
import org.eventb.texttools.PersistenceHelper;

public class DocumentProvider extends FileDocumentProvider {

	private final EventBTextEditor editor;

	public DocumentProvider(final EventBTextEditor editor) {
		this.editor = editor;
	}

	@Override
	protected IAnnotationModel createAnnotationModel(final Object element)
			throws CoreException {
		if (element instanceof IFileEditorInput) {
			final IFileEditorInput input = (IFileEditorInput) element;
			return new AnnotationModel(input.getFile(),
					getResourceForElement(input));
		}

		return super.createAnnotationModel(element);
	}

	@Override
	protected void setupDocument(final Object element, final IDocument document) {
		// remove old partitioner if set
		final IDocumentPartitioner oldPartitioner = document
				.getDocumentPartitioner();
		if (oldPartitioner != null) {
			oldPartitioner.disconnect();
		}

		final Partitioner partitioner = new Partitioner(new PartitionScanner(),
				PartitionScanner.CONTENT_TYPES);
		document.setDocumentPartitioner(partitioner);
		partitioner.connect(document, true);
	}

	@Override
	protected ElementInfo createElementInfo(final Object element)
			throws CoreException {
		final ElementInfo info = super.createElementInfo(element);

		if (info != null && element instanceof IEditorInput) {
			final IDocument document = info.fDocument;

			document.addDocumentListener(new IDocumentListener() {
				public void documentChanged(final DocumentEvent event) {
					final boolean newState = isDirty(info,
							(IEditorInput) element);

					if (info.fCanBeSaved != newState) {
						info.fCanBeSaved = newState;
						editor.updateIsDirty();
					}
				}

				public void documentAboutToBeChanged(final DocumentEvent event) {
					// IGNORE
				}
			});
		}
		return info;
	}

	private boolean isDirty(final ElementInfo info, final IEditorInput element) {
		final String currentInput = info.fDocument.get();
		final String savedInput = PersistenceHelper
				.getTextAnnotation(getResourceForElement(element));

		return currentInput != null && !currentInput.equals(savedInput);
	}

	@Override
	protected boolean setDocumentContent(final IDocument document,
			final IEditorInput editorInput, final String encoding)
			throws CoreException {
		/*
		 * We intercept loading from file and replace it with loading from
		 * RodinDB plus conversion to text (if necessary).
		 */
		final Resource resource = getResourceForElement(editorInput);

		// load content as stream
		InputStream stream;

		/* prefer \n as the line delimiter */
		String lineDelimiter = document.getLegalLineDelimiters()[0];
		for (int i = 1; i < document.getLegalLineDelimiters().length; i++) {
			if (document.getLegalLineDelimiters()[i].equals("\n")) {
				lineDelimiter = "\n";
				break;
			}
		}

		try {
			stream = createContentStream(resource, encoding, lineDelimiter);
		} catch (final IOException e) {
			// IGNORE
			return false;
		}

		try {
			setDocumentContent(document, stream, encoding);

			/*
			 * After setting content in new document initially parse and merge
			 * result into original model to create text positions in the
			 * original model.
			 */
			reconcileContent(resource, document, null);
			return true;
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (final IOException ex) {
				// IGNORE, no solution available here
			}
		}
	}

	@Override
	protected ISchedulingRule getSaveRule(final Object element) {
		/*
		 * We need to set the scheduling rule to the workspace root. Save
		 * operations of the editor run in a runnable and its scope is
		 * determined with the help of this method. The EMF persistence layer
		 * wrappes a save into another runnable which is handled by the RodinDB.
		 * The DB uses the workspace root as scope. If we set a smaller scope
		 * this collides with the larger scope.
		 */
		if (element instanceof IFileEditorInput) {
			return ResourcesPlugin.getWorkspace().getRoot();
		}

		return null;
	}

	@Override
	protected void doSaveDocument(final IProgressMonitor monitor,
			final Object element, final IDocument document,
			final boolean overwrite) throws CoreException {

		if (element instanceof IEditorInput) {
			final SubMonitor subMonitor = SubMonitor.convert(monitor, "Saving",
					4);

			editor.setIsSaving(true);
			final Resource resource = getResourceForElement((IEditorInput) element);

			// trim lines to remove trailing whitespaces
			final int newCursorOffset = trimLines(document, subMonitor
					.newChild(1));

			// parse and translate formulas
			reconcileContent(resource, document, subMonitor.newChild(1));
			subMonitor.worked(1);

			PersistenceHelper.saveText(resource, overwrite, subMonitor
					.newChild(1));

			editor.setIsSaving(false);
			editor.setCurserOffset(newCursorOffset);
			editor.selectAndReveal(newCursorOffset, 0);
		} else {
			throw new CoreException(new Status(IStatus.ERROR,
					TextEditorPlugin.PLUGIN_ID, "Unsupported target element: "
							+ element));
		}
	}

	private void reconcileContent(final Resource resource,
			final IDocument document, final IProgressMonitor m) {
		final SubMonitor monitor = SubMonitor.convert(m, 2);
		EventBNamedCommentedComponentElement astRoot = null;

		final Builder builder = new Builder();
		builder.run(editor, document, monitor.newChild(1));

		if (builder.wasSuccessful()) {
			final ParseResult lastParseResult = TextEditorPlugin
					.getDomManager()
					.getLastParseResult(editor.getEditorInput());

			if (lastParseResult != null) {
				astRoot = (EventBNamedCommentedComponentElement) lastParseResult.astRoot;
			}
		}

		if (astRoot != null) {
			PersistenceHelper.mergeRootElement(resource, astRoot, monitor
					.newChild(1));
		} else {
			// we need to store the current text because it is not parsable
			try {
				PersistenceHelper.addTextAnnotation(resource, document.get(),
						System.currentTimeMillis());
			} catch (final CoreException e) {
				TextEditorPlugin
						.getPlugin()
						.getLog()
						.log(
								new Status(
										IStatus.ERROR,
										TextEditorPlugin.PLUGIN_ID,
										"Cannot store text representation into Resource",
										e));
			}

			monitor.worked(1);
		}
	}

	private int trimLines(final IDocument document, final SubMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
		int cursorOffset = editor.getCursorOffset();

		try {
			final int removedChars = TrimLinesAction.trimLines(document, 0,
					document.getNumberOfLines() - 1, subMonitor.newChild(1));
			cursorOffset -= removedChars;

			return cursorOffset < document.getLength() ? cursorOffset
					: document.getLength() - 1;
		} catch (final BadLocationException e) {
			// IGNORE
			return cursorOffset;
		}
	}

	private Resource getResourceForElement(final IEditorInput editorInput) {
		final URI inputUri = EditUIUtil.getURI(editorInput);
		final Resource resource = editor.getEditingDomain().getResourceSet()
				.getResource(inputUri, true);
		return resource;
	}

	private InputStream createContentStream(final Resource resource,
			final String encoding, final String lineBreak)
			throws CoreException, IOException {
		return new ByteArrayInputStream(PersistenceHelper.loadText(resource,
				lineBreak).getBytes(encoding));
	}
}
