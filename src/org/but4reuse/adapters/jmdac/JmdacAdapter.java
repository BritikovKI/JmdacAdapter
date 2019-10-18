package org.but4reuse.adapters.jmdac;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.but4reuse.adapters.IAdapter;
import org.but4reuse.adapters.IElement;
import org.but4reuse.adapters.jmdac.FileElement;
import org.but4reuse.adapters.jmdac.ModuleElement;
import org.but4reuse.adapters.jmdac.module_infos_extractor.utils.DependenciesBuilder;
import org.but4reuse.adapters.jmdac.module_infos_extractor.utils.ModuleInfosExtractor;
import org.but4reuse.utils.files.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Eclipse adapter
 * 
 * @author Fjorilda Gjermizi
 * @author Krista Drushku
 * @author Diana MALABARD
 * @author Jason CHUMMUN
 * @param <ModuleElement>
 * 
 */
public class JmdacAdapter implements IAdapter {

	private URI rootURI;

	/**
	 * This method check if the artefact is adaptable with the EclipseAdapter
	 */

	@Override
	public boolean isAdaptable(URI uri, IProgressMonitor monitor) {
		File file = FileUtils.getFile(uri);
		if (file.isDirectory()) {
			File modulesFolder = new File(file.getAbsolutePath() + "/modules");
			return modulesFolder.exists() && modulesFolder.isDirectory();
		}
		return false;
	}

	Map<String, String> bundlesInfoLines;

	/**
	 * Provides the atomic elements (modules) this distribution is made of
	 * 
	 * @param uri
	 *            URI of the distribution
	 * @param monitor
	 */
	@Override
	public List<IElement> adapt(URI uri, IProgressMonitor monitor) {

		List<IElement> elements = new ArrayList<IElement>();
		File file = FileUtils.getFile(uri);
		rootURI = file.toURI();

		// A hashmap of bundle symbolic names and the complete line in the
		// bundles.info file
		bundlesInfoLines = ModuleInfosExtractor.createBundlesInfoMap(uri);

		// start the containment tree traversal, with null as initial container
		adapt(file, elements, null);

		// module dependencies
		for (IElement elem : elements) {
			if (elem instanceof ModuleElement) {
				ModuleElement pe = (ModuleElement) elem;
				DependenciesBuilder.build(pe, elements);
			}
		}

		// in elements we have the result
		return elements;
	}

	/**
	 * adapt recursively
	 * 
	 * @param file
	 * @param elements
	 * @param container
	 */
	private void adapt(File file, List<IElement> elements, IElement container) {
		FileElement newElement = null;
		if (ModuleInfosExtractor.isAModule(file)) {
			try {
				// Unzipped module
				if (file.isDirectory()) {
					newElement = ModuleInfosExtractor.getModuleInfosFromManifest(file.getAbsolutePath()
							+ "/META-INF/MANIFEST.MF");
				} else {
					// Jar module
					newElement = ModuleInfosExtractor.getModuleInfosFromJar(file.getAbsolutePath());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			newElement = new FileElement();
		}

		// Set the relevant information
		newElement.setUri(file.toURI());
		newElement.setRelativeURI(rootURI.relativize(file.toURI()));

		// Add dependency to the parent folder
		if (container != null) {
			newElement.addDependency("container", container);
		}

		// Add the bundles info
		if (newElement instanceof ModuleElement) {
			ModuleElement module = (ModuleElement) newElement;
			String line = bundlesInfoLines.get(module.getSymbName());
			// in the case of source code modules, line will be null but no
			// problem
			module.setBundleInfoLine(line);

			// if (module.getName() == null || module.getName().contains("%")) {
			// System.out.println("EclipseAdapter.adapt() No name found: " +
			// " isFragment:" + module.isFragment()
			// + "  " + module.getSymbName() + " at " + file.getAbsolutePath());
			// }

		}

		// Add to the list
		addElement(elements, newElement);

		// Go for the files in case of folder
		if (file.isDirectory()) {
			// Exclude the features folder
			if (!newElement.getRelativeURI().toString().equals("features/")) {
				File[] files = file.listFiles();
				for (File subFile : files) {
					adapt(subFile, elements, newElement);
				}
			}
		}
	}

	/**
	 * This method was created just to be overriden by the benchmark adapter
	 * 
	 * @param elements
	 * @param newElement
	 */
	protected void addElement(List<IElement> elements, FileElement newElement) {
		elements.add(newElement);
	}

	@Override
	public void construct(URI uri, List<IElement> elements, IProgressMonitor monitor) {
		boolean constructBundlesInfo = false;
		String bundlesInfoContent = "#version=1\n";
		for (IElement element : elements) {
			// check user cancel for each element
			if (!monitor.isCanceled()) {
				// provide user info
				monitor.subTask(element.getText());
				if (element instanceof FileElement) {
					FileElement fileElement = (FileElement) element;
					if (fileElement.getRelativeURI().toString().equals(ModuleInfosExtractor.BUNDLESINFO_RELATIVEPATH)) {
						constructBundlesInfo = true;
					}
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
				// prepare the bundles.info configuration file
				// just in case we need to construct it
				if (element instanceof ModuleElement) {
					ModuleElement moduleElement = (ModuleElement) element;
					String line = moduleElement.getBundleInfoLine();
					if (line != null) {
						String[] lineFields = line.split(",");
						bundlesInfoContent += moduleElement.getSymbName() + ",";
						bundlesInfoContent += moduleElement.getVersion() + ",";
						bundlesInfoContent += moduleElement.getRelativeURI() + ",";
						bundlesInfoContent += lineFields[3] + ",";
						bundlesInfoContent += lineFields[4] + "\n";
					}
				}
			}
			monitor.worked(1);
		}
		// Replace bundles.info content
		if (constructBundlesInfo) {
			try {
				File tmpFile = File.createTempFile("tempBundles", "info");
				FileUtils.appendToFile(tmpFile, bundlesInfoContent);
				File file = FileUtils.getFile(uri);
				File bundlesInfo = new File(file.getAbsolutePath() + "/"
						+ ModuleInfosExtractor.BUNDLESINFO_RELATIVEPATH);
				FileUtils.replace(bundlesInfo, tmpFile);
				tmpFile.deleteOnExit();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
