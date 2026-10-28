/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRewriteTarget;
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
import org.eventb.texttools.ComponentNameUtil;
import org.eventb.texttools.ComponentNameUtil.Kind;
import org.eventb.texttools.ComponentNameUtil.NameInfo;
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
					5);

			editor.setIsSaving(true);
			try {
				final Resource resource = getResourceForElement((IEditorInput) element);
				final int[] cursor = rememberCursor(document);

				// trim lines to remove trailing whitespaces
				trimLines(document, subMonitor.newChild(1));

				// the component name must match the file name
				adaptComponentName(element, document);

				// parse and translate formulas
				final long stampBefore = getModificationStamp(document);
				reconcileContent(resource, document, subMonitor.newChild(1));

				if (stampBefore != getModificationStamp(document)) {
					/*
					 * The formula translation changed the text (padding with
					 * blanks). Trim again and update the model so that the
					 * stored text matches the editor content exactly. Otherwise
					 * the editor is dirty again right after saving.
					 */
					trimLines(document, null);
					reconcileContent(resource, document, subMonitor.newChild(1));
				} else {
					subMonitor.worked(1);
				}

				PersistenceHelper.saveText(resource, overwrite,
						subMonitor.newChild(1));

				/*
				 * We bypass FileDocumentProvider's own saving. So we have to
				 * update the modification stamp ourselves. Otherwise the file
				 * synchronizer believes the file was changed by someone else
				 * and reloads/re-parses/re-merges the whole component right
				 * after each save (UI freeze, cursor jumps).
				 */
				updateModificationStamp(element);

				restoreCursor(document, cursor);
			} finally {
				editor.setIsSaving(false);
			}
		} else {
			throw new CoreException(new Status(IStatus.ERROR,
					TextEditorPlugin.PLUGIN_ID, "Unsupported target element: "
							+ element));
		}
	}

	/**
	 * If the text was copied from another machine/context, its name differs
	 * from the file name. Rodin always uses the file name, so we adapt the
	 * text before saving. Previously the mismatch made the merge into the
	 * Rodin database fail and the editor content was replaced by the old
	 * content.
	 */
	private void adaptComponentName(final Object element,
			final IDocument document) {
		if (!(element instanceof IFileEditorInput)) {
			return;
		}

		final IFile file = ((IFileEditorInput) element).getFile();
		final Kind expectedKind = ComponentNameUtil.kindForFileExtension(file
				.getFileExtension());
		final String expectedName = file.getFullPath().removeFileExtension()
				.lastSegment();

		final NameInfo nameInfo = ComponentNameUtil.findComponentName(document
				.get());
		if (nameInfo == null || nameInfo.kind != expectedKind
				|| nameInfo.name.equals(expectedName)) {
			return;
		}

		try {
			document.replace(nameInfo.offset, nameInfo.getLength(),
					expectedName);
		} catch (final BadLocationException e) {
			// IGNORE, the model merge renames the component anyway
		}
	}

	private void updateModificationStamp(final Object element) {
		final ElementInfo info = getElementInfo(element);
		if (info instanceof FileInfo && element instanceof IFileEditorInput) {
			((FileInfo) info).fModificationStamp = computeModificationStamp(((IFileEditorInput) element)
					.getFile());
		}
	}

	private static long getModificationStamp(final IDocument document) {
		if (document instanceof IDocumentExtension4) {
			return ((IDocumentExtension4) document).getModificationStamp();
		}
		return document.get().hashCode();
	}

	/**
	 * @return line and column of the cursor
	 */
	private int[] rememberCursor(final IDocument document) {
		final int offset = editor.getCursorOffset();
		try {
			final int line = document.getLineOfOffset(offset);
			return new int[] { line, offset - document.getLineOffset(line) };
		} catch (final BadLocationException e) {
			return new int[] { 0, 0 };
		}
	}

	/**
	 * Puts the cursor back to the same line and column (or the end of the
	 * line if it became shorter by trimming).
	 */
	private void restoreCursor(final IDocument document, final int[] cursor) {
		try {
			final int line = Math.min(cursor[0],
					document.getNumberOfLines() - 1);
			final int lineOffset = document.getLineOffset(line);
			final String delimiter = document.getLineDelimiter(line);
			final int lineLength = document.getLineLength(line)
					- (delimiter != null ? delimiter.length() : 0);
			final int offset = lineOffset + Math.min(cursor[1], lineLength);

			editor.setCurserOffset(offset);
			editor.selectAndReveal(offset, 0);
		} catch (final BadLocationException e) {
			// IGNORE, cursor stays where it is
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

	private void trimLines(final IDocument document, final SubMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, 1);

		/* make all trimmed lines one single undo step */
		final IRewriteTarget rewriteTarget = editor.getAdapter(IRewriteTarget.class);
		if (rewriteTarget != null) {
			rewriteTarget.beginCompoundChange();
		}

		try {
			TrimLinesAction.trimLines(document, 0,
					document.getNumberOfLines() - 1, subMonitor.newChild(1));
		} catch (final BadLocationException e) {
			// IGNORE
		} finally {
			if (rewriteTarget != null) {
				rewriteTarget.endCompoundChange();
			}
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
