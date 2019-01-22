package ch.epfl.biop.atlas.commands;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ch.epfl.biop.atlas.allen.adultmousebrain.AllenBrainAdultMouseAtlasV2;
import ch.epfl.biop.atlas.allen.adultmousebrain.AllenBrainAdultMouseAtlasV3;
import ch.epfl.biop.atlas.BiopAtlas;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Open Atlas...")
public class BrowseAtlasCommand implements Command {

	@Parameter(type = ItemIO.OUTPUT)
	BiopAtlas ba;
	
	@Parameter(choices={"Adult Mouse Allen Brain v2","Adult Mouse Allen Brain v3"})
	String atlasId;
	
	@Parameter
	CommandService cs;
	
	@Override
	public void run() {
		BiopAtlas biopatlas;
		Future<CommandModule> f=null;
		switch(atlasId) {
		case "Adult Mouse Allen Brain v2":
			f = cs.run(AllenBrainAdultMouseAtlasV2.class, true);
			break;
		case "Adult Mouse Allen Brain v3":
			f = cs.run(AllenBrainAdultMouseAtlasV3.class, true);
			break;	
		}
		if (f!=null) {
			try {
				ba = (BiopAtlas) (f.get().getOutput("ba"));
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	}

}
