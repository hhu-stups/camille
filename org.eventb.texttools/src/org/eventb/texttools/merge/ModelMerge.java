/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.merge;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.diff.merge.service.MergeService;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeLeftTarget;
import org.eclipse.emf.compare.diff.service.DiffService;
import org.eclipse.emf.compare.match.MatchOptions;
import org.eclipse.emf.compare.match.engine.IMatchEngine;
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.match.service.MatchService;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eventb.emf.core.Annotation;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.core.Extension;
import org.eventb.emf.formulas.BFormula;
import org.eventb.emf.persistence.factory.RodinResource;
import org.eventb.texttools.PersistenceHelper;
import org.eventb.texttools.TextPositionUtil;

/**
 * <p>
 * This class helps merging an core model instance which has been created by the
 * parser back into the original model instance which was created from the
 * RodinDB.
 * </p>
 * <p>
 * It takes the original version (old version or left version) and a new version
 * (right version) and merges the right into the left. This means it takes all
 * changes from the right and applies them to the left. During this process it
 * preserves those elements that are not part of the core model and have been
 * added as {@link Extension}s or {@link Annotation}s or {@link EAnnotation}s.
 * </p>
 */
public class ModelMerge {
	private final EventBNamedCommentedComponentElement oldVersion;
	private final EventBNamedCommentedComponentElement newVersion;
	private final IResource resource;
	private final Map<String, Object> matchOptions;

	public ModelMerge(final EventBNamedCommentedComponentElement oldVersion,
			final EventBNamedCommentedComponentElement newVersion) {
		this.oldVersion = oldVersion;
		this.newVersion = newVersion;

		resource = PersistenceHelper.getIResource(oldVersion.eResource());

		/*
		 * Configure the matching process: We want to match elements in the
		 * model by their similarity, so we ignore any IDs.
		 */
		matchOptions = new HashMap<String, Object>();
		matchOptions.put(MatchOptions.OPTION_IGNORE_ID, true);
		matchOptions.put(MatchOptions.OPTION_IGNORE_XMI_ID, true);
		matchOptions
				.put(EventBMatchEngine.OPTION_DONT_COMPARE_COMPONENTS, true);
	}

	/**
	 * Compares the two model versions that have been given to the constructor
	 * and merges all changes in the new version into the old version of the
	 * model. For this process certain changes are ignored:
	 * <ul>
	 * <li>RodinInternalAnnotations</li>
	 * <li>{@link Extension}s which are not {@link BFormula}s (they are changed
	 * by the text tools parser)</li>
	 * </ul>
	 * 
	 * @throws InterruptedException
	 */
	public void applyChanges(final IProgressMonitor monitor)
			throws InterruptedException {
		final SubMonitor subMonitor = SubMonitor.convert(monitor,
				"Analyzing model changes", 4);

		final IMatchEngine matchEngine = MatchService
				.getBestMatchEngine(resource.getFileExtension());

		// Workaround to make sure the models have an associated resource
		// See setResourceFile() for Bug info

		// First find the file extension
		String path = oldVersion.getURI().path();
		String extension = path.substring(path.lastIndexOf('.'));
		String projectName = oldVersion.getURI().segment(1);
		File tmpFileNew = null;

		if (newVersion.eResource() == null) {
			tmpFileNew = setResourceFile(newVersion, extension);
			// setRodinResource(newVersion, extension, projectName);
		}

		matchOptions.put(MatchOptions.OPTION_PROGRESS_MONITOR, subMonitor
				.newChild(1));
		final MatchModel matchModel = matchEngine.contentMatch(oldVersion,
				newVersion, matchOptions);

		final DiffModel diff = getDiffModel(matchModel, subMonitor.newChild(2));
		final EList<DiffElement> ownedElements = diff.getOwnedElements();

		if (ownedElements.size() > 0) {
			// MergeService
			// .merge(new ArrayList<DiffElement>(ownedElements), false);

			MergeService.merge(ownedElements, false);

			matchOptions.put(MatchOptions.OPTION_PROGRESS_MONITOR, subMonitor
					.newChild(1));
			subMonitor.worked(1);
		}

		// cleanup tmp files
		EcoreUtil.remove(newVersion);
		if (tmpFileNew != null) {
			tmpFileNew.delete();
		}
	}

	/**
	 * This method exists to work around a bug in EMF Compare 1.0.1. The compare
	 * framework only works if a model has an associated resource. see
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=258703
	 */
	private File setResourceFile(EventBNamedCommentedComponentElement element,
			String extension) {
		try {
			File tmpFile = File.createTempFile("camille-", extension);
			tmpFile.deleteOnExit();
			URI uri = URI.createFileURI(tmpFile.getAbsolutePath());
			Resource resource = new XMLResourceImpl(uri);
			resource.getContents().add(element);
			return tmpFile;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Alternative to setResourceFile: set a RodinResource
	 */
	private static File setRodinResource(
			EventBNamedCommentedComponentElement element, String extension,
			String projectPath) {
		try {
			File tmpFile = File.createTempFile("camille-", extension);
			tmpFile.deleteOnExit();
			URI uri = URI.createFileURI(tmpFile.getAbsolutePath());
			// XMLResourceImpl resource = new XMLResourceImpl(uri);
			RodinResource resource = new RodinResource();
			resource.setURI(URI.createPlatformResourceURI(projectPath + "/"
					+ tmpFile.getName(), true));
			resource.eSetDeliver(true);
			resource.getContents().add(element);
			return tmpFile;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Do the diff for the {@link MatchModel} and removed ignored elements
	 * afterwards.
	 * 
	 * @param matchModel
	 * @param monitor
	 * @return
	 */
	private DiffModel getDiffModel(final MatchModel matchModel,
			final IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, 2);
		final DiffModel diff = DiffService.doDiff(matchModel);
		subMonitor.worked(1);

		final EList<DiffElement> ownedElements = diff.getOwnedElements();
		final EList<DiffElement> cleanedDiffs = cleanDiffElements(ownedElements);
		subMonitor.worked(1);

		ownedElements.clear();
		ownedElements.addAll(cleanedDiffs);

		return diff;
	}

	/**
	 * Recursively remove all ignored diff elements.
	 * 
	 * @see #isIgnoredDiff(DiffElement)
	 * @param elements
	 * @return
	 */
	private EList<DiffElement> cleanDiffElements(
			final EList<DiffElement> elements) {
		final EList<DiffElement> result = new BasicEList<DiffElement>();

		if (elements != null) {
			for (final DiffElement diffElement : elements) {
				if (!isIgnoredDiff(diffElement)) {
					// add this diff element to result
					result.add(diffElement);

					// continue clean recursively
					final EList<DiffElement> subDiffElements = diffElement
							.getSubDiffElements();
					final EList<DiffElement> subResult = cleanDiffElements(subDiffElements);
					subDiffElements.clear();
					subDiffElements.addAll(subResult);
				}
			}
		}

		return result;
	}

	/**
	 * Returns whether the given {@link DiffElement} should be ignored when
	 * applying the diff.
	 * 
	 * @param diffElement
	 * @return
	 */
	private boolean isIgnoredDiff(final DiffElement diffElement) {
		if (diffElement instanceof ModelElementChangeLeftTarget) {
			/*
			 * EMF elements of the original model (left) show up as
			 * RemoveModelElement diffs when they are missing in the new version
			 * (right). If we don't want to remove them for the merged version
			 * we just ignore the diff element.
			 */
			final ModelElementChangeLeftTarget removedDiff = (ModelElementChangeLeftTarget) diffElement;
			final EObject leftElement = removedDiff.getLeftElement();

			/*
			 * The original model may contain arbitrary annotations which we
			 * didn't create or handle. Ignore EAnnotations containing
			 * Textranges
			 */
			if (leftElement instanceof EAnnotation
					&& !TextPositionUtil.ANNOTATION_TEXTRANGE
							.equals(((EAnnotation) leftElement).getSource())) {
				return true;
			}
			/*
			 * With eventb.emf v3.1.0 EAnnotations where replaced by
			 * Annotations. Ignore Annotations containing Textranges
			 */
			if (leftElement instanceof Annotation
					&& !TextPositionUtil.ANNOTATION_TEXTRANGE
							.equals(((Annotation) leftElement).getSource())) {
				return true;

			}

			/*
			 * Ignore all Extensions but our own, i.e. BFormula
			 */
			if (leftElement instanceof Extension
					&& !(leftElement instanceof BFormula)) {
				return true;
			}
		}
		return false;
	}
}
