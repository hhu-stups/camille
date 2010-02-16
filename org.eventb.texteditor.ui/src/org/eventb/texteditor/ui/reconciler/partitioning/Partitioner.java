/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.reconciler.partitioning;

import org.eclipse.jface.text.rules.FastPartitioner;

public class Partitioner extends FastPartitioner {

	public Partitioner(final PartitionScanner partitionScanner,
			final String[] contentTypes) {
		super(partitionScanner, contentTypes);
	}

	public void clearCache() {
		clearPositionCache();
	}
}
