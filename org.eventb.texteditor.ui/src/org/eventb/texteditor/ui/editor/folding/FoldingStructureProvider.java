/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.folding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.custom.StyledText;

/**
 * Keeps the folding annotations of an editor in sync with its text.
 * The regions are computed by {@link FoldingRegionFinder} shortly after
 * the user stops typing. Regions are identified by a key (e.g. the event
 * name), so collapsed regions stay collapsed while the text is edited.
 * <p>
 * All methods must be called in the UI thread.
 * </p>
 */
public class FoldingStructureProvider implements IProjectionListener {

	private static final int DELAY = 300;

	private final ProjectionViewer viewer;
	private final Map<ProjectionAnnotation, String> keys = new HashMap<ProjectionAnnotation, String>();
	private IDocument document;

	/*
	 * Keys of the regions that were collapsed before the whole text was
	 * replaced (e.g. by "Format"), to collapse them again afterwards.
	 */
	private final Set<String> collapsedBeforeReplace = new HashSet<String>();

	private final Runnable updateRunnable = new Runnable() {
		@Override
		public void run() {
			update();
		}
	};

	private final IDocumentListener documentListener = new IDocumentListener() {
		@Override
		public void documentAboutToBeChanged(final DocumentEvent event) {
			final IDocument doc = event.getDocument();
			if (event.getOffset() == 0 && event.getLength() > 0
					&& event.getLength() == doc.getLength()) {
				rememberCollapsedRegions();
			}
		}

		@Override
		public void documentChanged(final DocumentEvent event) {
			scheduleUpdate();
		}
	};

	private final ITextInputListener inputListener = new ITextInputListener() {
		@Override
		public void inputDocumentAboutToBeChanged(final IDocument oldInput,
				final IDocument newInput) {
			setDocument(null);
		}

		@Override
		public void inputDocumentChanged(final IDocument oldInput,
				final IDocument newInput) {
			setDocument(newInput);
			scheduleUpdate();
		}
	};

	public FoldingStructureProvider(final ProjectionViewer viewer) {
		this.viewer = viewer;
	}

	public void install() {
		viewer.addProjectionListener(this);
		viewer.addTextInputListener(inputListener);
		setDocument(viewer.getDocument());
	}

	public void uninstall() {
		viewer.removeProjectionListener(this);
		viewer.removeTextInputListener(inputListener);
		setDocument(null);
		keys.clear();
	}

	private void setDocument(final IDocument newDocument) {
		if (document != null) {
			document.removeDocumentListener(documentListener);
		}
		document = newDocument;
		if (document != null) {
			document.addDocumentListener(documentListener);
		}
	}

	private void rememberCollapsedRegions() {
		for (final Map.Entry<ProjectionAnnotation, String> entry : keys
				.entrySet()) {
			if (entry.getKey().isCollapsed()) {
				collapsedBeforeReplace.add(entry.getValue());
			}
		}
	}

	@Override
	public void projectionEnabled() {
		keys.clear();
		collapsedBeforeReplace.clear();
		update();
	}

	@Override
	public void projectionDisabled() {
		keys.clear();
	}

	private void scheduleUpdate() {
		final StyledText widget = viewer.getTextWidget();
		if (widget != null && !widget.isDisposed()) {
			// restarts the timer if it is already running
			widget.getDisplay().timerExec(DELAY, updateRunnable);
		}
	}

	/**
	 * Recomputes the folding regions immediately.
	 */
	public void update() {
		final StyledText widget = viewer.getTextWidget();
		final ProjectionAnnotationModel model = viewer
				.getProjectionAnnotationModel();
		final IDocument doc = viewer.getDocument();
		if (widget == null || widget.isDisposed() || model == null
				|| doc == null) {
			return;
		}

		final List<FoldingRegionFinder.Region> regions = FoldingRegionFinder
				.findRegions(doc.get());

		// the annotations created by us that are still in the model
		final Map<String, ProjectionAnnotation> existing = new HashMap<String, ProjectionAnnotation>();
		final Iterator<Annotation> iterator = model.getAnnotationIterator();
		while (iterator.hasNext()) {
			final Annotation annotation = iterator.next();
			final String key = keys.get(annotation);
			if (key != null) {
				existing.put(key, (ProjectionAnnotation) annotation);
			}
		}
		keys.keySet().retainAll(existing.values());

		final List<Annotation> deletions = new ArrayList<Annotation>();
		final List<Annotation> modifications = new ArrayList<Annotation>();
		final Map<Annotation, Position> additions = new HashMap<Annotation, Position>();

		for (final FoldingRegionFinder.Region region : regions) {
			final ProjectionAnnotation annotation = existing.remove(region.key);
			final Position position = annotation != null ? model
					.getPosition(annotation) : null;

			final boolean collapseAgain = collapsedBeforeReplace
					.contains(region.key);

			if (position != null && !position.isDeleted()) {
				boolean modified = false;
				if (position.getOffset() != region.offset
						|| position.getLength() != region.length) {
					position.setOffset(region.offset);
					position.setLength(region.length);
					modified = true;
				}
				if (collapseAgain && !annotation.isCollapsed()) {
					annotation.markCollapsed();
					modified = true;
				}
				if (modified) {
					modifications.add(annotation);
				}
				continue;
			}

			// new region, or the old one was replaced as a whole
			final boolean collapsed = collapseAgain || annotation != null
					&& annotation.isCollapsed();
			if (annotation != null) {
				deletions.add(annotation);
				keys.remove(annotation);
			}
			final ProjectionAnnotation newAnnotation = new ProjectionAnnotation(
					collapsed);
			additions.put(newAnnotation, new Position(region.offset,
					region.length));
			keys.put(newAnnotation, region.key);
		}

		collapsedBeforeReplace.clear();

		// regions that do not exist anymore
		for (final ProjectionAnnotation annotation : existing.values()) {
			deletions.add(annotation);
			keys.remove(annotation);
		}

		if (!deletions.isEmpty() || !additions.isEmpty()
				|| !modifications.isEmpty()) {
			model.modifyAnnotations(
					deletions.toArray(new Annotation[deletions.size()]),
					additions,
					modifications.toArray(new Annotation[modifications.size()]));
		}
	}

	/**
	 * Collapses or expands all regions of the given kind.
	 */
	public void setCollapsed(final FoldingRegionFinder.Kind kind,
			final boolean collapse) {
		update();
		final ProjectionAnnotationModel model = viewer
				.getProjectionAnnotationModel();
		if (model == null) {
			return;
		}

		final List<Annotation> changed = new ArrayList<Annotation>();
		for (final Map.Entry<ProjectionAnnotation, String> entry : keys
				.entrySet()) {
			if (!entry.getValue().startsWith(prefix(kind))) {
				continue;
			}
			final ProjectionAnnotation annotation = entry.getKey();
			if (annotation.isCollapsed() != collapse) {
				if (collapse) {
					annotation.markCollapsed();
				} else {
					annotation.markExpanded();
				}
				changed.add(annotation);
			}
		}

		if (!changed.isEmpty()) {
			model.modifyAnnotations(null, null,
					changed.toArray(new Annotation[changed.size()]));
		}
	}

	static String prefix(final FoldingRegionFinder.Kind kind) {
		switch (kind) {
		case EVENT:
			return "event:";
		case COMMENT:
			return "comment:";
		default:
			return "clause:";
		}
	}
}
