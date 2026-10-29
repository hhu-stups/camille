/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.occurrences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens;

/**
 * Highlights all occurrences of the identifier at the cursor, like "Mark
 * Occurrences" in the Java editor. The occurrences are also shown in the
 * overview ruler and can be visited with "Next/Previous Annotation"
 * (Ctrl+. / Ctrl+,).
 * <p>
 * All methods are called in the UI thread.
 * </p>
 */
public class MarkOccurrencesSupport {

	public static final String ANNOTATION_TYPE = "org.eventb.texteditor.ui.occurrences";

	private final EventBTextEditor editor;
	private final ISourceViewer viewer;
	private final IPreferenceStore preferences;

	private Annotation[] annotations = new Annotation[0];
	private IAnnotationModel annotationModel;

	/* tokens of the last analyzed text */
	private IDocument cachedDocument;
	private long cachedStamp = IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
	private EventBTokens cachedTokens;
	private String cachedText;

	/* avoids recomputation when the cursor moves inside of the same word */
	private int markedStart = -1;
	private int markedEnd = -1;
	private long markedStamp;

	private final ISelectionChangedListener selectionListener = new ISelectionChangedListener() {
		@Override
		public void selectionChanged(final SelectionChangedEvent event) {
			update(event.getSelection());
		}
	};

	private final IPropertyChangeListener preferenceListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent event) {
			if (TextEditorPlugin.PREF_MARK_OCCURRENCES.equals(event
					.getProperty())) {
				if (isEnabled()) {
					update(viewer.getSelectionProvider().getSelection());
				} else {
					removeAnnotations();
				}
			}
		}
	};

	public MarkOccurrencesSupport(final EventBTextEditor editor,
			final ISourceViewer viewer, final IPreferenceStore preferences) {
		this.editor = editor;
		this.viewer = viewer;
		this.preferences = preferences;
	}

	public void install() {
		if (viewer.getSelectionProvider() instanceof IPostSelectionProvider) {
			((IPostSelectionProvider) viewer.getSelectionProvider())
					.addPostSelectionChangedListener(selectionListener);
		}
		preferences.addPropertyChangeListener(preferenceListener);
	}

	public void uninstall() {
		if (viewer.getSelectionProvider() instanceof IPostSelectionProvider) {
			((IPostSelectionProvider) viewer.getSelectionProvider())
					.removePostSelectionChangedListener(selectionListener);
		}
		preferences.removePropertyChangeListener(preferenceListener);
		removeAnnotations();
		cachedTokens = null;
		cachedText = null;
		cachedDocument = null;
	}

	private boolean isEnabled() {
		return preferences.getBoolean(TextEditorPlugin.PREF_MARK_OCCURRENCES);
	}

	private void update(final ISelection selection) {
		if (!isEnabled() || !(selection instanceof ITextSelection)
				|| !editor.isEditorAlive()) {
			return;
		}

		final IDocument document = viewer.getDocument();
		if (document == null) {
			return;
		}

		final ITextSelection textSelection = (ITextSelection) selection;
		final int offset = textSelection.getOffset();
		final long stamp = getStamp(document);

		// same word as before, nothing changed
		if (stamp != IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP
				&& stamp == markedStamp && offset >= markedStart
				&& offset + textSelection.getLength() <= markedEnd) {
			return;
		}

		final EventBTokens tokens = getTokens(document, stamp);
		final List<Position> positions = OccurrencesFinder.find(cachedText,
				tokens,
				offset);

		markedStart = -1;
		markedEnd = -1;
		for (final Position position : positions) {
			if (position.includes(offset)
					|| position.getOffset() + position.getLength() == offset) {
				markedStart = position.getOffset();
				markedEnd = position.getOffset() + position.getLength();
			}
		}
		markedStamp = stamp;

		setAnnotations(positions);
	}

	private EventBTokens getTokens(final IDocument document, final long stamp) {
		if (cachedTokens == null || document != cachedDocument
				|| stamp == IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP
				|| stamp != cachedStamp) {
			cachedText = document.get();
			cachedTokens = new EventBTokens(cachedText);
			cachedDocument = document;
			cachedStamp = stamp;
		}
		return cachedTokens;
	}

	private void setAnnotations(final List<Position> positions) {
		final IAnnotationModel model = getAnnotationModel();
		if (model == null) {
			return;
		}

		final Map<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();
		for (final Position position : positions) {
			// no text: the hover shows the information about the identifier
			newAnnotations.put(new Annotation(ANNOTATION_TYPE, false, null),
					position);
		}

		if (model instanceof IAnnotationModelExtension) {
			((IAnnotationModelExtension) model).replaceAnnotations(
					annotations, newAnnotations);
		} else {
			for (final Annotation annotation : annotations) {
				model.removeAnnotation(annotation);
			}
			for (final Map.Entry<Annotation, Position> entry : newAnnotations
					.entrySet()) {
				model.addAnnotation(entry.getKey(), entry.getValue());
			}
		}
		annotations = newAnnotations.keySet().toArray(
				new Annotation[newAnnotations.size()]);
		annotationModel = model;
	}

	private void removeAnnotations() {
		if (annotationModel != null && annotations.length > 0) {
			if (annotationModel instanceof IAnnotationModelExtension) {
				((IAnnotationModelExtension) annotationModel)
						.replaceAnnotations(annotations, null);
			} else {
				for (final Annotation annotation : annotations) {
					annotationModel.removeAnnotation(annotation);
				}
			}
		}
		annotations = new Annotation[0];
		markedStart = -1;
		markedEnd = -1;
	}

	private IAnnotationModel getAnnotationModel() {
		if (editor.getDocumentProvider() == null) {
			return null;
		}
		return editor.getDocumentProvider().getAnnotationModel(
				editor.getEditorInput());
	}

	private static long getStamp(final IDocument document) {
		if (document instanceof IDocumentExtension4) {
			return ((IDocumentExtension4) document).getModificationStamp();
		}
		return IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
	}
}
