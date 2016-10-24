package fi.aalto.drumbeat.object_browser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Optional;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import fi.aalto.drumbeat.drumbeat_tree.DrumbeatTree.vo.DrumbeatAnnotationsData;
/*
* 
Jyrki Oraskari, Aalto University, 2016 

This research has partly been carried out at Aalto University in DRUMBEAT 
“Web-Enabled Construction Lifecycle” (2014-2017) —funded by Tekes, 
Aalto University, and the participating companies.

The MIT License (MIT)
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/


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
