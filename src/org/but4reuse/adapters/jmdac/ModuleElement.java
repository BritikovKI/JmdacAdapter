package org.but4reuse.adapters.jmdac;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.but4reuse.adapters.IElement;
import org.but4reuse.adapters.impl.AbstractElement;
import org.but4reuse.adapters.markers.IMarkerElement;
import org.but4reuse.utils.files.FileUtils;
import org.but4reuse.utils.workbench.WorkbenchUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * Module Element
 * 
 * @author jabier.martinez
 */
public class ModuleElement extends AbstractElement implements IMarkerElement {

	private URI uri;
	private URI relativeURI;
	public List<String> dependency_modules = new ArrayList<String>();

	@Override
	public double similarity(IElement anotherElement) {
		// When they have the same relative URI
		// TODO URIs can reference to the same file... check this
		if (anotherElement instanceof ModuleElement) {
			ModuleElement anotherFileElement = ((ModuleElement) anotherElement);

			// Same URI?
			if (this.getFileName().equals(anotherFileElement.getFileName())) {
				return 1;
			}
		}
		return 0;
	}

	@Override
	public String getText() {
		return getRelativeURI().toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getFileName() == null) ? 0 : getFileName().hashCode());
		return result;
	}

	public URI getRelativeURI() {
		return relativeURI;
	}

	public String getFileName() {
		File f = new File(uri);
		return f.getName();
	}

	public void setRelativeURI(URI relativeURI) {
		this.relativeURI = relativeURI;
	}

	/**
	 * I don't use it I don't know why is it here :/
	 */
	@Override
	public IMarker getMarker() {
		IMarker marker = null;
		IResource ifile = WorkbenchUtils.getIResourceFromURI(getUri());
		if (ifile != null && ifile.exists()) {
			try {
				marker = ifile.createMarker(IMarker.TEXT);
				marker.setAttribute(IMarker.LOCATION, ifile.getName());
				// marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return marker;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	/**
	 * Read the modules.xml file inside the .jmdac archive
	 * and get the dependency names and versions (but versions
	 * are just ignored yet)
	 * 
	 * @author jabier.martinez
	 */
	public void collectDependencies() throws IOException {
		System.out.println("Collecting dependencies of " + this.getFileName());
		ZipFile zipFile = new ZipFile(FileUtils.getFile(uri));

		Enumeration<? extends ZipEntry> entries = zipFile.entries();

		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();

			Path entryPath = Paths.get(getName(), "module.xml");

//			System.out.println("EntryName: " + entry.getName() + ";;" + entryPath);

			// filter out other files
			if (!entry.getName().equals(entryPath.toString())) {
				continue;
			}

//			System.out.println("Found module.xml: " + entry.getName());

			InputStream stream = zipFile.getInputStream(entry);

			// create a new DocumentBuilderFactory
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

			try {
				// use the factory to create a documentbuilder
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(stream);
				Node dependencies = doc.getElementsByTagName("Dependencies").item(0);
				if (dependencies == null) {
					// this module has no dependencies
					continue;
				}
				NodeList dependencyNodes = dependencies.getChildNodes();

				for (int i = 0; i < dependencyNodes.getLength(); i++) {

					Node node = dependencyNodes.item(i);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element elem = (Element) node;

						// Get the name of the dependency
						String name = elem.getAttribute("name");
						String version = elem.getAttribute("version");
						dependency_modules.add(name);
						System.out.println(this.getFileName() + " depends on " + name + "_" + version);
					}
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}

		}
		zipFile.close();
	}

	public String getName() {
		return getFileName().split("_", 2)[0];
	}

	@Override
	public ArrayList<String> getWords() {
		ArrayList<String> words = new ArrayList<String>();
		words.add(getName());  // add the name of the module
		
		String moduleDescription = "";  // TODO implement it
		StringTokenizer tk = new StringTokenizer(moduleDescription, " ");
		
		while (tk.hasMoreTokens()) {
			String s = tk.nextToken();
			words.add(s);
		}
		return words;
	}
}