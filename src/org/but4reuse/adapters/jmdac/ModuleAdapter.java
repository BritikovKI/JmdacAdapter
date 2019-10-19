package org.but4reuse.adapters.jmdac;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.but4reuse.adapters.IAdapter;
import org.but4reuse.adapters.IElement;
import org.but4reuse.utils.files.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Module structure adapter
 * 
 * @author jabier.martinez
 */
public class ModuleAdapter implements IAdapter {

	private URI rootURI;
	public boolean ignoreFolders;

	@Override
	public boolean isAdaptable(URI uri, IProgressMonitor monitor) {
		// Any folder is adaptable
		File file = FileUtils.getFile(uri);
		if (file != null && file.exists() && file.isDirectory()) {
			return true;
		}
		return false;
	}

	@Override
	public List<IElement> adapt(URI uri, IProgressMonitor monitor) {
		List<IElement> elements = new ArrayList<IElement>();
		File file = FileUtils.getFile(uri);
		rootURI = file.toURI();
		// start the containment tree traversal, with null as initial container
		adapt(file, elements, null);
	
		// construct dependencies
		for (IElement elem: elements) {
			ModuleElement felem = (ModuleElement) elem;
			for (String module: felem.dependency_modules) {
				IElement another_elem = get_element_by_name(elements, module);
				if (another_elem == null) {
					System.out.println("MODULE ELEMENT NOT FOUND:: " + module);
				} else {
					felem.addDependency("dependency", another_elem);
				}
			}
		}
		// in elements we have the result
		return elements;
	}

	private IElement get_element_by_name(List<IElement> elements, String name) {
		// TODO Auto-generated method stub
		System.out.println("get_element_by_name: "+name);
		for (IElement elem: elements) {
			ModuleElement felem = (ModuleElement) elem;
//			System.out.println(felem.getName());
			if (name.equals(felem.getName())) {
				return felem;
			};
		}
		return null;
	}

	/**
	 * adapt recursively
	 * 
	 * @param file
	 * @param elements
	 * @param container
	 */
	private void adapt(File file, List<IElement> elements, IElement container) {
		ModuleElement newElement = null;
		// Distinguish between file and folder
		if (!file.isDirectory() && FileUtils.getExtension(file).equalsIgnoreCase("jmdac")) {
			newElement = new ModuleElement();
			
			// Set the relevant information
			newElement.setUri(file.toURI());
			newElement.setRelativeURI(rootURI.relativize(file.toURI()));
	
			// Add to the list
			try {
				newElement.collectDependencies();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			elements.add(newElement);
		} else {
			// Go for the files in case of folder
			File[] files = file.listFiles();
			for (File subFile : files) {
				adapt(subFile, elements, newElement);
			}
		}
	}

	@Override
	public void construct(URI uri, List<IElement> elements, IProgressMonitor monitor) {
		for (IElement element : elements) {
			// check user cancel for each element
			if (!monitor.isCanceled()) {
				// provide user info
				monitor.subTask(element.getText());
				if (element instanceof ModuleElement) {
					ModuleElement fileElement = (ModuleElement) element;
					try {
						// Create parent folders structure
						URI newDirectoryURI = uri.resolve(fileElement.getRelativeURI());
						File destinationFile = FileUtils.getFile(newDirectoryURI);
						if (destinationFile != null && !destinationFile.getParentFile().exists()) {
							destinationFile.getParentFile().mkdirs();
						}
						if (destinationFile != null && !destinationFile.exists()) {
							// Copy the content. In the case of a folder, its
							// content is not copied
							File file = FileUtils.getFile(fileElement.getUri());
							Files.copy(file.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			monitor.worked(1);
		}
	}

}
