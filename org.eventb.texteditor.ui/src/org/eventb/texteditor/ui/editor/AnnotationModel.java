/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;
import org.eventb.core.IExtendsContext;
import org.eventb.core.IRefinesEvent;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.ISeesContext;
import org.eventb.emf.core.EventBExpression;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.EventBPredicate;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Parameter;
import org.eventb.emf.core.machine.Variable;
import org.eventb.emf.persistence.factory.RodinResource;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.model.texttools.TextRange;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.RodinDBException;
import org.rodinp.core.RodinMarkerUtil;

public class AnnotationModel extends ResourceMarkerAnnotationModel {
	private static final String LABEL_ATTRIBUTE_ID = "org.eventb.core.label";
	private final RodinResource rodinResource;

	public AnnotationModel(final IResource fileResource,
			final Resource emfResource) {
		super(fileResource);

		if (emfResource instanceof RodinResource) {
			rodinResource = (RodinResource) emfResource;
		} else {
			rodinResource = null;
		}
	}

	@Override
	protected Position createPositionFromMarker(final IMarker marker) {
		try {
			if (RodinMarkerUtil.RODIN_PROBLEM_MARKER.equals(marker.getType())) {
				return createPosition(marker);
			}
		} catch (final CoreException e) {
			TextEditorPlugin.getPlugin().getLog().log(
					new Status(IStatus.ERROR, TextEditorPlugin.PLUGIN_ID,
							"Error analyzing marker", e));
		}

		return super.createPositionFromMarker(marker);
	}

	private Position createPosition(final IMarker marker) {
		final IRegion region = getBestRegion(marker);

		if (region != null) {
			return new Position(region.getOffset(), region.getLength());
		} else {
			return null;
		}
	}

	private IRegion getBestRegion(final IMarker marker) {
		final int charStart = RodinMarkerUtil.getCharStart(marker);
		final int charEnd = RodinMarkerUtil.getCharEnd(marker);

		final IInternalElement rodinElement = RodinMarkerUtil
				.getInternalElement(marker);

		// special case for references (sees, refines, ...)
		if (isReferenceElement(rodinElement)) {
			return handleReferenceElement(rodinElement);
		}

		final EventBObject eventBObject = rodinResource.getMap().get(
				rodinElement);

		if (charStart >= 0 && charEnd >= 0) {
			/*
			 * If start and end char is set, it must be a problem inside a
			 * formula.
			 */
			return getRegionInFormula(eventBObject, charStart, charEnd);

		} else if (onlyLabelIsRelevant(marker, eventBObject)) {
			final TextRange range = TextPositionUtil.getInternalPosition(
					eventBObject, ((EventBNamed) eventBObject).getName());

			if (range != null) {
				return new Region(range.getOffset(), range.getLength());
			}
		} else {
			/*
			 * Asuming the whole element has a problem as a fallback
			 */
			final TextRange range = TextPositionUtil.getTextRange(eventBObject);

			if (range != null) {
				return new Region(range.getOffset(), range.getLength());
			}
		}

		return new Region(0, 0);
	}

	private boolean isReferenceElement(final IInternalElement rodinElement) {
		return rodinElement instanceof IRefinesEvent
				|| rodinElement instanceof ISeesContext
				|| rodinElement instanceof IRefinesMachine
				|| rodinElement instanceof IExtendsContext;
	}

	private IRegion handleReferenceElement(final IInternalElement rodinElement) {
		final EventBObject parent = rodinResource.getMap().get(
				rodinElement.getParent());

		if (parent != null) {
			String name = null;

			try {
				if (rodinElement instanceof IRefinesEvent) {
					name = ((IRefinesEvent) rodinElement)
							.getAbstractEventLabel();
				} else if (rodinElement instanceof ISeesContext) {
					name = ((ISeesContext) rodinElement).getSeenContextName();
				} else if (rodinElement instanceof IRefinesMachine) {
					name = ((IRefinesMachine) rodinElement)
							.getAbstractMachineName();
				} else if (rodinElement instanceof IExtendsContext) {
					name = ((IExtendsContext) rodinElement)
							.getAbstractContextName();
				}

				final TextRange range = TextPositionUtil.getInternalPosition(
						parent, name);
				if (range != null) {
					return new Region(range.getOffset(), range.getLength());
				}
			} catch (final RodinDBException e) {
				// IGNORE
			}
		}

		return new Region(0, 0);
	}

	private boolean onlyLabelIsRelevant(final IMarker marker,
			final EventBObject eventBObject) {
		/*
		 * The attribute means there's a problem with the label of this element.
		 * Of course this needs to be an EventBNamed element too.
		 */
		final String attributeId = marker.getAttribute(
				RodinMarkerUtil.ATTRIBUTE_ID, null);
		if (LABEL_ATTRIBUTE_ID.equals(attributeId)
				&& eventBObject instanceof EventBNamed) {
			return true;
		}

		/*
		 * If the surrounding element is an event we don't want to highlight the
		 * whole event. This could be very distracting to the user. So we just
		 * choose the event's name.
		 */
		if (eventBObject instanceof Event) {
			return true;
		}

		/*
		 * When the problem is in a variable or parameter declaration we want
		 * only the identifier to be highlighted but not optional comments.
		 */
		if (eventBObject instanceof Variable
				|| eventBObject instanceof Parameter) {
			return true;
		}

		return false;
	}

	private IRegion getRegionInFormula(final EventBObject eventBObject,
			final int charStart, final int charEnd) {
		TextRange range = null;

		if (eventBObject instanceof EventBPredicate) {
			range = TextPositionUtil.getInternalPosition(eventBObject,
					((EventBPredicate) eventBObject).getPredicate());
		} else if (eventBObject instanceof Action) {
			range = TextPositionUtil.getInternalPosition(eventBObject,
					((Action) eventBObject).getAction());
		} else if (eventBObject instanceof EventBExpression) {
			range = TextPositionUtil.getInternalPosition(eventBObject,
					((EventBExpression) eventBObject).getExpression());
		}

		if (range != null) {
			return new Region(range.getOffset() + charStart, charEnd
					- charStart);
		} else {
			// fall back and mark the complete formula
			range = TextPositionUtil.getTextRange(eventBObject);
			if (range != null) {
				return new Region(range.getOffset(), range.getLength());
			}
		}

		return null;
	}
}
