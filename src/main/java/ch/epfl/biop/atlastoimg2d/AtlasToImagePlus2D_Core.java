package ch.epfl.biop.atlastoimg2d;

import ch.epfl.biop.fiji.objectgui.ScijavaPanelizable;
import org.scijava.Context;
import org.scijava.object.ObjectService;

import ch.epfl.biop.atlas.BiopAtlas;
import ij.ImagePlus;

import javax.swing.JPanel;

abstract public class AtlasToImagePlus2D_Core implements AtlasToImg2D<ImagePlus>, ScijavaPanelizable {

	BiopAtlas ba;
	Object atlasLocation;
	
	boolean interactive=true;
	
	ObjectService os;
	
	@Override
	public void setObjectService(ObjectService os) {
		this.os = os;
	}	
	
	public void setInteractive(boolean flag) {
		interactive = flag;
	}
	
	@Override
	public void setAtlas(BiopAtlas ba) {
		this.ba=ba;
	}

	@Override
	public boolean isInitialized() {
		return (ba!=null);
	}

	@Override
	public void setAtlasLocation(Object location) {
		atlasLocation=location;
	}
	
	boolean registrationSet = false;
	
	@Override
	public void resetRegistration() {
		registrationSet = false;
	}

	@Override
	public boolean isRegistrationSet() {
		// TODO Auto-generated method stub
		return registrationSet;
	}

	public JPanel getPanel(Context ctx) {
		return new JPanel();
	}

	/*@Override
	abstract public void register(); 

	@Override
	public void resetRegistration() {
		
	}

	@Override
	public boolean isRegistrationSet() {
		return false;
	}

	@Override
	public ConvertibleRois transformRoisAtlasToImg(ConvertibleRois in) {
		return null;
	}

	@Override
	public ImagePlus transformImgToAtlas() {
		// TODO Auto-generated method stub
		return null;
	}*/

}
