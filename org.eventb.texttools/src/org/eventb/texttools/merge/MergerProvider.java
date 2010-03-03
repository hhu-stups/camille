/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.merge;

import java.util.Map;

import org.eclipse.emf.compare.diff.merge.IMerger;
import org.eclipse.emf.compare.diff.merge.IMergerProvider;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeRightTarget;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChangeLeftTarget;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChangeRightTarget;
import org.eclipse.emf.compare.diff.metamodel.ReferenceOrderChange;
import org.eclipse.emf.compare.util.EMFCompareMap;

public class MergerProvider implements IMergerProvider {
	private Map<Class<? extends DiffElement>, Class<? extends IMerger>> mergerTypes;

	public Map<Class<? extends DiffElement>, Class<? extends IMerger>> getMergers() {
		if (mergerTypes == null) {
			mergerTypes = new EMFCompareMap<Class<? extends DiffElement>, Class<? extends IMerger>>();

			mergerTypes.put(ReferenceChangeRightTarget.class,
					ReferenceChangeRightTargetMerger.class);
			mergerTypes.put(ReferenceChangeLeftTarget.class,
					ReferenceChangeLeftTargetMerger.class);
			mergerTypes.put(ModelElementChangeRightTarget.class,
					ModelElementChangeRightTargetMerger.class);
			mergerTypes.put(ReferenceOrderChange.class,
					ReferenceOrderChangeMerger.class);
		}
		return mergerTypes;
	}
}
