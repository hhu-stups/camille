/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.hover;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.editor.EventBTextEditor;

/**
 * Text hover of the Camille editor (issue #4): shows problem messages and
 * information about the identifier under the mouse, e.g. the machine or
 * context it is declared in, its type and its comment.
 * <p>
 * It replaces the standard text hover of
 * {@link org.eclipse.ui.editors.text.TextSourceViewerConfiguration}, which
 * Camille used before for problem messages, and works the same way: the
 * annotations at the mouse position are filtered with the same preferences
 * and shown in the same information control (HTML text with "Press 'F2' for
 * focus"). Only non-deprecated API is used.
 * </p>
 * <p>
 * Hover information is computed in a background thread by the JFace hover
 * manager. While such a computation is running, the manager does not start
 * another one. So the computation must never block: it only reads the
 * immutable {@link HoverIndex}, which is (re)built in a separate job whenever
 * the component or the components it depends on are saved.
 * </p>
 */
public class EventBTextHover implements ITextHover, ITextHoverExtension,
		ITextHoverExtension2, IResourceChangeListener {

	/**
	 * Decides which annotations are shown in the hover. The editor
	 * configuration passes its preference based filter here (the same one
	 * the standard text hover uses).
	 */
	public interface AnnotationFilter {
		boolean isShown(Annotation annotation);
	}

	private static final Pattern EVENT = Pattern
			.compile("(?<![\\w'])event\\s+([\\w']+)");
	private static final Pattern EVENTS = Pattern
			.compile("(?<![\\w'])events(?![\\w'])");

	private final EventBTextEditor editor;
	private final ISourceViewer sourceViewer;
	private final AnnotationFilter annotationFilter;

	private volatile HoverIndex index = HoverIndex.EMPTY;

	private final Job indexJob = new Job("Updating Camille hover information") {
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			final IFile file = getFile();
			if (file != null && file.exists()) {
				HoverIndex newIndex;
				try {
					newIndex = HoverIndex.build(file);
				} catch (final Exception e) {
					newIndex = HoverIndex.failed(e);
				} catch (final LinkageError e) {
					newIndex = HoverIndex.failed(e);
				}
				index = newIndex;
				if (newIndex.getProblem() != null) {
					TextEditorPlugin
							.getPlugin()
							.getLog()
							.log(new Status(IStatus.WARNING,
									TextEditorPlugin.PLUGIN_ID,
									"Could not read all hover information for "
											+ file.getName(), newIndex
											.getProblem()));
				}
			}
			return Status.OK_STATUS;
		}
	};

	public EventBTextHover(final EventBTextEditor editor,
			final ISourceViewer sourceViewer,
			final AnnotationFilter annotationFilter) {
		this.editor = editor;
		this.sourceViewer = sourceViewer;
		this.annotationFilter = annotationFilter;

		indexJob.setSystem(true);
		indexJob.setPriority(Job.SHORT);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
				IResourceChangeEvent.POST_CHANGE);
		indexJob.schedule();
	}

	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		indexJob.cancel();
	}

	private IFile getFile() {
		final IEditorInput input = editor.getEditorInput();
		return input instanceof IFileEditorInput ? ((IFileEditorInput) input)
				.getFile() : null;
	}

	/*
	 * Rebuild the index when a machine/context (or a static checker result)
	 * in the same project changes, e.g. after saving.
	 */
	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		final IFile file = getFile();
		if (file == null || event.getDelta() == null) {
			return;
		}

		final IResourceDelta projectDelta = event.getDelta().findMember(
				file.getProject().getFullPath());
		if (projectDelta == null) {
			return;
		}

		final boolean[] relevant = new boolean[1];
		try {
			projectDelta.accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(final IResourceDelta delta) {
					final IResource resource = delta.getResource();
					if (resource.getType() == IResource.FILE) {
						final String extension = resource.getFileExtension();
						if ("bum".equals(extension) || "buc".equals(extension)
								|| "bcm".equals(extension)
								|| "bcc".equals(extension)) {
							relevant[0] = true;
						}
					}
					return !relevant[0];
				}
			});
		} catch (final CoreException e) {
			relevant[0] = true;
		}

		if (relevant[0]) {
			indexJob.schedule(200);
		}
	}

	@Override
	public IRegion getHoverRegion(final ITextViewer textViewer, final int offset) {
		final IDocument document = textViewer.getDocument();
		if (document == null) {
			return null;
		}

		try {
			int start = offset;
			while (start > 0 && isIdentifierPart(document.getChar(start - 1))) {
				start--;
			}
			int end = offset;
			while (end < document.getLength()
					&& isIdentifierPart(document.getChar(end))) {
				end++;
			}
			if (end > start) {
				return new Region(start, end - start);
			}
			// no identifier: still allow problem hovers on operators etc.
			return offset < document.getLength() ? new Region(offset, 1)
					: null;
		} catch (final BadLocationException e) {
			return null;
		}
	}

	private static boolean isIdentifierPart(final char c) {
		return Character.isJavaIdentifierPart(c) || c == '\'';
	}

	/**
	 * Only called by clients that do not know {@link ITextHoverExtension2}.
	 */
	@Override
	@Deprecated
	public String getHoverInfo(final ITextViewer textViewer,
			final IRegion region) {
		final Object info = getHoverInfo2(textViewer, region);
		return info != null ? info.toString() : null;
	}

	@Override
	public Object getHoverInfo2(final ITextViewer textViewer,
			final IRegion region) {
		if (region == null) {
			return null;
		}

		final String problems = getProblems(region);

		String info = null;
		final IDocument document = textViewer.getDocument();
		if (document != null) {
			try {
				final String word = document.get(region.getOffset(),
						region.getLength());
				if (word.length() > 0 && isIdentifierPart(word.charAt(0))) {
					final HoverIndex currentIndex = index;
					final String text = currentIndex.lookup(word,
							findEvent(document.get(), region.getOffset()));
					if (text != null) {
						info = toHtml(text);
					} else if (currentIndex.getProblem() != null
							&& !Character.isDigit(word.charAt(0))) {
						info = "<i>No Camille hover information available: "
								+ escape(String.valueOf(currentIndex
										.getProblem().getMessage()))
								+ "</i>";
					}
				}
			} catch (final BadLocationException e) {
				// IGNORE, text changed in the meantime
			}
		}

		if (problems != null && info != null) {
			return problems + "<br><br>" + info;
		}
		return problems != null ? problems : info;
	}

	/**
	 * Messages of the annotations (errors, warnings, ...) at the region, like
	 * the standard text hover shows them.
	 */
	private String getProblems(final IRegion region) {
		final IAnnotationModel model = sourceViewer.getAnnotationModel();
		if (model == null) {
			return null;
		}

		final Set<String> messages = new LinkedHashSet<String>();
		final Iterator<Annotation> iterator = model.getAnnotationIterator();
		while (iterator.hasNext()) {
			final Annotation annotation = iterator.next();
			if (annotation.isMarkedDeleted() || annotation.getText() == null
					|| !annotationFilter.isShown(annotation)) {
				continue;
			}
			final Position position = model.getPosition(annotation);
			if (position != null
					&& position.overlapsWith(region.getOffset(),
							Math.max(1, region.getLength()))) {
				messages.add(escape(annotation.getText()));
			}
		}

		if (messages.isEmpty()) {
			return null;
		}
		final StringBuilder result = new StringBuilder();
		for (final String message : messages) {
			if (result.length() > 0) {
				result.append("<br>");
			}
			result.append(message);
		}
		return result.toString();
	}

	/**
	 * The information control renders HTML (like for the problem messages):
	 * first line bold, line breaks kept.
	 */
	static String toHtml(final String text) {
		final String[] lines = text.split("\n", -1);
		final StringBuilder html = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			if (i == 0) {
				html.append("<b>").append(escape(lines[i])).append("</b>");
			} else {
				html.append("<br>").append(escape(lines[i]));
			}
		}
		return html.toString();
	}

	private static String escape(final String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;")
				.replace(">", "&gt;");
	}

	/**
	 * @return the current information about the component (read from the
	 *         Rodin database)
	 */
	public HoverIndex getIndex() {
		return index;
	}

	/**
	 * @return label of the event the offset is in, or <code>null</code>
	 */
	public static String findEvent(final String text, final int offset) {
		final Matcher events = EVENTS.matcher(text);
		if (!events.find() || events.start() > offset) {
			return null;
		}

		String label = null;
		final Matcher matcher = EVENT.matcher(text);
		while (matcher.find() && matcher.start() < offset) {
			label = matcher.group(1);
		}
		return label;
	}

	@Override
	public IInformationControlCreator getHoverControlCreator() {
		// the same information control as the standard text hover
		return new IInformationControlCreator() {
			@Override
			public IInformationControl createInformationControl(
					final Shell parent) {
				return new DefaultInformationControl(parent,
						EditorsUI.getTooltipAffordanceString());
			}
		};
	}
}
