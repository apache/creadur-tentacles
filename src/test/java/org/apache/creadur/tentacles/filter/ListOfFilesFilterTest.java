package org.apache.creadur.tentacles.filter;

import static org.junit.Assert.*;

import org.junit.Test;

public class ListOfFilesFilterTest {
	@Test
	public void testCreationAndToString() {
		ListOfFilesFilter filter = new ListOfFilesFilter("a12", "b23", "c34");
		String toString = filter.toString();
		assertTrue(toString.contains("a12"));
		assertTrue(toString.contains("b23"));
		assertTrue(toString.contains("c34"));
		
		assertEquals(3, filter.getListOfFiles().size());
	}
}
