package jadex.bdiv3;

import jadex.bdiv3.IBDIClassGenerator;

public class MavenBDIModelLoader extends jadex.bdiv3.BDIModelLoader
{
	private MavenBDIClassReader mavenReader;


	public MavenBDIModelLoader()
	{
		super();
		this.mavenReader = new MavenBDIClassReader(this);
		this.reader = mavenReader;
	}
	
	
	public void setGenerator(IBDIClassGenerator gen) {
		this.mavenReader.setGenerator(gen);
	}
	
}
