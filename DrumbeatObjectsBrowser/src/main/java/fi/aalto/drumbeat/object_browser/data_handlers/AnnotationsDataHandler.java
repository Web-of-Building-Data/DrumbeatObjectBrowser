package fi.aalto.drumbeat.object_browser.data_handlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Optional;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import fi.aalto.drumbeat.object_browser.vo.DrumbeatAnnotationsData;


/**
 * @author joraskur
 *
 */
public class AnnotationsDataHandler {
	private Optional<DrumbeatAnnotationsData> data_store=Optional.empty();
	
	/**
	 * @return
	 */
	public DrumbeatAnnotationsData getData_store() {
		if(data_store.isPresent())
		  return data_store.get();
		else
		{
		  data_store=Optional.of(new DrumbeatAnnotationsData());
		  return data_store.get();
		}
	}

	/**
	 * 
	 */
	public void save_data()
	{
		if(data_store.isPresent())
		  internal_save_data(data_store.get());	
	}
	
	/**
	 * @param datastore
	 */
	private void internal_save_data(DrumbeatAnnotationsData datastore) {
		try {
			XStream xstream = new XStream();
			String xml = xstream.toXML(datastore);
			try {
				String path = "/var/drumbeat_admin";
				if (isWindows())
					path = "c:" + path;
				File f = new File(path);
				java.nio.file.Path path_base = Paths.get(path);
				if (!f.exists()) {

					Files.createDirectory(path_base);
				} else if (!f.isDirectory()) {
					System.err.println("Is not a path: " + path);
					return;
				}
				Files.write(Paths.get(path_base.toString()+"/object_browser.xml"), xml.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public void read_data()
	{
		DrumbeatAnnotationsData da=internal_read_data();
		if(da==null)
			data_store=Optional.of(new DrumbeatAnnotationsData());
		else
		    data_store=Optional.of(da);
	}
	
	/**
	 * @return
	 */
	private DrumbeatAnnotationsData internal_read_data() {
		try {
			XStream xstream = new XStream(){
			    @Override
			    protected MapperWrapper wrapMapper(MapperWrapper next) {
			        return new MapperWrapper(next) {
			            @Override
			            public boolean shouldSerializeMember(Class definedIn, String fieldName) {
			                if (definedIn == Object.class) {
			                    return false;
			                }
			                return super.shouldSerializeMember(definedIn, fieldName);
			            }
			        };
			    }
			};
			try {
				String path = "/var/drumbeat_admin";
				if (isWindows())
					path = "c:" + path;
				File f = new File(path);
				java.nio.file.Path path_base = Paths.get(path);
				if (!f.exists()) {

					Files.createDirectory(path_base);
				} else if (!f.isDirectory()) {
					System.err.println("Is not a path: " + path);
					return null;
				}
				byte[] data=Files.readAllBytes(Paths.get(path_base.toString()+"/object_browser.xml"));
				String xml=new String(data);
				DrumbeatAnnotationsData datastore = (DrumbeatAnnotationsData) xstream.fromXML(xml);				
				return datastore;
			} 
			
			catch(NoSuchFileException e)
			{
				// OK
			}
			catch (IOException e) {
				
				e.printStackTrace();
			}

		} catch (Exception e) {
			// Data format changed
		}
		return null;
	}
	
	/**
	 * @return
	 */
	private boolean isWindows() {
		String OS = System.getProperty("os.name").toLowerCase();
		return (OS.indexOf("win") >= 0);
	}


}
