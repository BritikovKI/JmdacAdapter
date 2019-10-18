package org.but4reuse.adapters.jmdac;

import java.util.ArrayList;
import java.util.List;

import org.but4reuse.adapters.IElement;
import org.but4reuse.utils.strings.StringUtils;

/**
 * Module Element
 * 
 * @author Diana MALABARD
 * @author Jason CHUMMUN
 */
public class ModuleElement extends FileElement {

	private String moduleSymbName;
	private String moduleVersion;
	private String bundleInfoLine;
	private String fragmentHost;
	private String name;
	// each moduleElement in require_Bundles, the list of require_Bundle will be
	// empty.
	// because we do not know the dependencies
	// The same thing for absoluthPath
	private ArrayList<String> require_Bundles;

	@Override
	public double similarity(IElement anotherElement) {
		// When they have the same relative URI
		// TODO URIs can reference to the same file... check this
		if (anotherElement instanceof ModuleElement) {
			ModuleElement anotherModuleElement = ((ModuleElement) anotherElement);

			// Same symbolic name
			if (this.getSymbName().equals(anotherModuleElement.getSymbName())) {
				// TODO no versioning supported
				return 1;
			}
		}
		return 0;
	}

	public ArrayList<String> getRequire_Bundles() {
		return require_Bundles;
	}

	private String absolutePath;
	private boolean isJar;

	public ModuleElement() {
		require_Bundles = new ArrayList<String>();
	}

	public String getSymbName() {
		return moduleSymbName;
	}

	public void setSymbName(String moduleSymbName) {
		this.moduleSymbName = moduleSymbName;
	}

	public void addRequire_bundle(String require_bundle) {
		this.require_Bundles.add(require_bundle);
	}

	public void removeRequire_bundle(String require_bundle) {
		this.require_Bundles.remove(require_bundle);
	}

	@Override
	public String getText() {
		return name + "  " + moduleSymbName + " " + moduleVersion;
	}

	public boolean isJar() {
		return isJar;
	}

	public void setJar(boolean isJar) {
		this.isJar = isJar;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public void setAbsolutePath(String absolutePath) {
		this.absolutePath = absolutePath;
	}

	public String getVersion() {
		return moduleVersion;
	}

	public void setVersion(String moduleVersion) {
		this.moduleVersion = moduleVersion;
	}

	public String getBundleInfoLine() {
		return bundleInfoLine;
	}

	public void setBundleInfoLine(String bundleInfoLine) {
		this.bundleInfoLine = bundleInfoLine;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFragmentHost() {
		return fragmentHost;
	}

	public void setFragmentHost(String fragmentHost) {
		this.fragmentHost = fragmentHost;
	}

	public boolean isFragment() {
		return fragmentHost != null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getSymbName() == null) ? 0 : getSymbName().hashCode());
		return result;
	}

	@Override
	public List<String> getWords() {
		List<String> words = new ArrayList<String>();
		if (name != null) {
			for (String s : StringUtils.tokenizeString(name)) {
				words.add(s);
			}
		}
		return words;
	}
}