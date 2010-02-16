/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.core.machine.Machine;

public class CollectingHelper {
	public static final String SUFFIX_MACHINE = "bum";
	public static final String SUFFIX_CONTEXT = "buc";

	protected static Resource resolveComponentsResource(
			final EventBNamedCommentedComponentElement component, final Resource referencingResource) {
		Resource resource = null;

		if (component.eIsProxy()) {
			// resolve context in resource's ResourceSet
			final EventBNamedCommentedComponentElement resolvedComponent = (EventBNamedCommentedComponentElement) EcoreUtil
					.resolve(component, referencingResource);

			/*
			 * Workaround when automatic resolving didn't work.
			 */
			if (resolvedComponent.eIsProxy()) {
				final URI uri = referencingResource.getURI()
						.trimFileExtension().trimSegments(1).appendSegment(
								((EventBNamed) component).getName())
						.appendFileExtension(getFileExtension(component));
				/*
				 * Only try to resolve if file already exists. We don't want new
				 * files to be created on the fly.
				 */
				if (URIConverter.INSTANCE.exists(uri, null)) {
					resource = referencingResource.getResourceSet()
							.getResource(uri, true);
				} else {
					resource = resolvedComponent.eResource();
				}
			} else {
				resource = resolvedComponent.eResource();
			}
		} else {
			resource = component.eResource();
		}

		return resource;
	}

	private static String getFileExtension(final EventBNamedCommentedComponentElement component) {
		if (component instanceof Machine) {
			return SUFFIX_MACHINE;
		} else {
			return SUFFIX_CONTEXT;
		}
	}
}
