package gr.uom.java.xmi;

import java.io.Serializable;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierList;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

public class UMLModifier implements Serializable, LocationInfoProvider {
	private String keyword;
	private LocationInfo locationInfo;
	
	public UMLModifier(PsiFile cu, String filePath, PsiModifierList modifierList, String keyword) {
		this.keyword = keyword;
		this.locationInfo = new LocationInfo(cu, filePath, modifierList, CodeElementType.MODIFIER);
	}

	public String getKeyword() {
		return keyword;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	public String toString() {
		return keyword;
	}
}
