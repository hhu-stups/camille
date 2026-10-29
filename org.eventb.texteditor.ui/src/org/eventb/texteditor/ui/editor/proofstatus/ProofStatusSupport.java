/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.proofstatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texteditor.ui.editor.proofstatus.ProofStatusIndex.ProofObligation;
import org.eventb.texteditor.ui.editor.proofstatus.ProofStatusIndex.Summary;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.EventBlock;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.Kind;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.Token;

/**
 * Proof status of labels: colors the background of each label (and event
 * name) by the status of its proof obligations and provides the proof
 * obligations for the hover and the hyperlinks.
 * <ul>
 * <li>green: all proof obligations discharged</li>
 * <li>blue: all discharged or reviewed</li>
 * <li>yellow: some discharged or reviewed</li>
 * <li>red: none discharged or reviewed</li>
 * </ul>
 * The status is read from the Rodin database (saved state) and updated when
 * the proof status changes.
 */
public class ProofStatusSupport implements IResourceChangeListener {

	public static final String TYPE_PREFIX = "org.eventb.texteditor.ui.proof.";
	private static final String TYPE_DISCHARGED = TYPE_PREFIX + "discharged";
	private static final String TYPE_REVIEWED = TYPE_PREFIX + "reviewed";
	private static final String TYPE_PARTIAL = TYPE_PREFIX + "partial";
	private static final String TYPE_PENDING = TYPE_PREFIX + "pending";

	private static final int DELAY = 500;

	/** A label (or event name) in the text */
	public static final class LabelAt {
		/** the event of a guard/action/witness label, or the event itself */
		public final String event;
		/** the label, <code>null</code> for an event name */
		public final String label;
		/** position of the label including "@" */
		public final int offset;
		public final int length;

		LabelAt(final String event, final String label, final int offset,
				final int length) {
			this.event = event;
			this.label = label;
			this.offset = offset;
			this.length = length;
		}

		/** "@inv1" or "event inc" */
		public String describe() {
			return label != null ? "@" + label : "event " + event;
		}
	}

	private final EventBTextEditor editor;
	private final IPreferenceStore preferences;
	private volatile ProofStatusIndex index = ProofStatusIndex.EMPTY;
	private Annotation[] annotations = new Annotation[0];
	private IAnnotationModel annotationModel;
	private IDocument document;

	private final Job indexJob = new Job("Reading proof status") {
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			final IFile file = getFile();
			if (file != null && file.exists()) {
				try {
					index = ProofStatusIndex.build(file);
				} catch (final CoreException e) {
					index = ProofStatusIndex.EMPTY;
				} catch (final RuntimeException e) {
					index = ProofStatusIndex.EMPTY;
				}
				scheduleUpdate(0);
			}
			return Status.OK_STATUS;
		}
	};

	private final Runnable updateRunnable = new Runnable() {
		@Override
		public void run() {
			updateAnnotations();
		}
	};

	private final IDocumentListener documentListener = new IDocumentListener() {
		@Override
		public void documentAboutToBeChanged(final DocumentEvent event) {
			// IGNORE
		}

		@Override
		public void documentChanged(final DocumentEvent event) {
			scheduleUpdate(DELAY);
		}
	};

	private final IPropertyChangeListener preferenceListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent event) {
			if (TextEditorPlugin.PREF_PROOF_STATUS.equals(event.getProperty())) {
				scheduleUpdate(0);
			}
		}
	};

	public ProofStatusSupport(final EventBTextEditor editor,
			final IPreferenceStore preferences) {
		this.editor = editor;
		this.preferences = preferences;
		indexJob.setSystem(true);
		indexJob.setPriority(Job.DECORATE);
	}

	public void install() {
		document = editor.getDocumentProvider().getDocument(
				editor.getEditorInput());
		if (document != null) {
			document.addDocumentListener(documentListener);
		}
		preferences.addPropertyChangeListener(preferenceListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
				IResourceChangeEvent.POST_CHANGE);
		indexJob.schedule();
	}

	public void uninstall() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		preferences.removePropertyChangeListener(preferenceListener);
		if (document != null) {
			document.removeDocumentListener(documentListener);
		}
		indexJob.cancel();
		removeAnnotations();
	}

	private IFile getFile() {
		return editor.getEditorInput() instanceof IFileEditorInput ? ((IFileEditorInput) editor
				.getEditorInput()).getFile() : null;
	}

	/**
	 * Rebuild the index when the proof obligations or the proof status of
	 * the component change.
	 */
	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		final IFile file = getFile();
		if (file == null || event.getDelta() == null) {
			return;
		}
		final String baseName = file.getFullPath().removeFileExtension()
				.lastSegment();
		final boolean[] relevant = new boolean[1];
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(final IResourceDelta delta) {
					final IResource resource = delta.getResource();
					if (resource.getType() != IResource.FILE) {
						return !relevant[0];
					}
					final String extension = resource.getFileExtension();
					if (resource.getProject().equals(file.getProject())
							&& baseName.equals(resource.getFullPath()
									.removeFileExtension().lastSegment())
							&& ("bps".equals(extension) || "bpo"
									.equals(extension))) {
						relevant[0] = true;
					}
					return false;
				}
			});
		} catch (final CoreException e) {
			relevant[0] = true;
		}
		if (relevant[0]) {
			indexJob.schedule(300);
		}
	}

	/**
	 * @return the label at the offset, or <code>null</code>
	 */
	public static LabelAt findLabel(final String text, final int offset) {
		final EventBTokens tokens = new EventBTokens(text);
		final int index = tokens.indexAt(offset);
		if (index < 0) {
			return null;
		}
		return labelOf(tokens, index);
	}

	private static LabelAt labelOf(final EventBTokens tokens, final int index) {
		final Token token = tokens.get(index);
		final EventBlock event = tokens.eventOf(index);
		if (token.kind == Kind.LABEL) {
			return new LabelAt(event != null ? event.label : null, token.text,
					token.offset - 1, token.text.length() + 1);
		}
		if (event != null && event.labelIndex == index) {
			return new LabelAt(event.label, null, token.offset,
					token.text.length());
		}
		return null;
	}

	/**
	 * @return the proof obligations of the label (empty if none or unknown)
	 */
	public List<ProofObligation> getObligations(final LabelAt label) {
		if (label == null) {
			return Collections.emptyList();
		}
		final ProofStatusIndex currentIndex = index;
		return label.label != null ? currentIndex.forLabel(label.event,
				label.label) : currentIndex.forEvent(label.event);
	}

	private void scheduleUpdate(final int delay) {
		final Display display = Display.getDefault();
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (editor.isEditorAlive()) {
					display.timerExec(delay, updateRunnable);
				}
			}
		});
	}

	private void updateAnnotations() {
		if (!editor.isEditorAlive() || document == null) {
			return;
		}
		if (!preferences.getBoolean(TextEditorPlugin.PREF_PROOF_STATUS)) {
			removeAnnotations();
			return;
		}

		final Map<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();
		final EventBTokens tokens = new EventBTokens(document.get());
		for (int i = 0; i < tokens.getTokens().size(); i++) {
			final LabelAt label = labelOf(tokens, i);
			if (label == null) {
				continue;
			}
			final List<ProofObligation> obligations = getObligations(label);
			final Summary summary = ProofStatusIndex.summarize(obligations);
			if (summary == null) {
				continue;
			}
			newAnnotations.put(new Annotation(typeOf(summary), false,
					label.describe() + ": "
							+ ProofStatusIndex.describe(obligations)),
					new Position(label.offset, label.length));
		}

		final IAnnotationModel model = editor.getDocumentProvider()
				.getAnnotationModel(editor.getEditorInput());
		if (model instanceof IAnnotationModelExtension) {
			((IAnnotationModelExtension) model).replaceAnnotations(annotations,
					newAnnotations);
			annotations = newAnnotations.keySet().toArray(
					new Annotation[newAnnotations.size()]);
			annotationModel = model;
		}
	}

	private void removeAnnotations() {
		if (annotationModel instanceof IAnnotationModelExtension
				&& annotations.length > 0) {
			((IAnnotationModelExtension) annotationModel).replaceAnnotations(
					annotations, null);
		}
		annotations = new Annotation[0];
	}

	private static String typeOf(final Summary summary) {
		switch (summary) {
		case DISCHARGED:
			return TYPE_DISCHARGED;
		case REVIEWED:
			return TYPE_REVIEWED;
		case PARTIAL:
			return TYPE_PARTIAL;
		default:
			return TYPE_PENDING;
		}
	}
}
