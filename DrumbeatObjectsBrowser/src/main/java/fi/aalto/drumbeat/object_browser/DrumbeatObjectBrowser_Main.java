package fi.aalto.drumbeat.object_browser;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.servlet.annotation.WebServlet;

import com.google.common.eventbus.Subscribe;
import com.jarektoro.responsivelayout.ResponsiveColumn;
import com.jarektoro.responsivelayout.ResponsiveLayout;
import com.jarektoro.responsivelayout.ResponsiveRow;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileResource;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Image;
import com.vaadin.ui.RichTextArea;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import fi.aalto.drumbeat.object_browser.data_handlers.AnnotationsDataHandler;
import fi.aalto.drumbeat.object_browser.data_handlers.EventBusCommunication;
import fi.aalto.drumbeat.object_browser.data_handlers.NameSpaceHandler;
import fi.aalto.drumbeat.object_browser.events.MainNodeUpdateEvent;
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
import fi.aalto.drumbeat.object_browser.events.NameSpaceDataChangedEvent;

/**
 * @author joraskur
 *
 */
@Push(PushMode.AUTOMATIC)
@Theme("Drumbeat")
public class DrumbeatObjectBrowser_Main extends UI {
	private final EventBusCommunication communication = EventBusCommunication.getInstance();
	private static final long serialVersionUID = 1L;
	private String url = "http://architectural.drb.cs.hut.fi/drumbeat/collections";
	private final Table ns_table = new Table("");
	private Optional<BrowserFrame> browser = Optional.empty();
	private Optional<RichTextArea> richTextArea = Optional.empty();

	private final AnnotationsDataHandler annotation_DataModel = new AnnotationsDataHandler();
	private DrumbeatTree objectbrowser_tree;
	private final NameSpaceHandler name_spaces_handler = new NameSpaceHandler();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.vaadin.ui.UI#init(com.vaadin.server.VaadinRequest)
	 */
	@Override
	protected void init(VaadinRequest request) {
		Page.getCurrent().getStyles().add(".bg-dark-trblue {  opacity: 0.7;background-color:  #004586;}");
		String app_url = handleURLparameter(request);

		ns_table.addContainerProperty("Name", String.class, null);
		ns_table.addContainerProperty("URI", String.class, null);
		ns_table.setColumnHeader("Name", "Namespace");
		ns_table.setColumnHeader("URI", "URI");
		ns_table.setPageLength(ns_table.size());

		ns_table.setImmediate(true);
		ns_table.setWidth("100%");

		final VerticalLayout main_layout = new VerticalLayout();
		main_layout.setStyleName("drumbeat");
		String basepath = VaadinService.getCurrent().getBaseDirectory().getAbsolutePath();
		FileResource resource = new FileResource(new File(basepath + "/WEB-INF/images/drumbeat_banner_small3.jpg"));
		Image drumbeat_logo = new Image("", resource);
		drumbeat_logo.setHeight("50px");
		main_layout.addComponent(drumbeat_logo);

		ResponsiveLayout content_area = new ResponsiveLayout();
		content_area.setSizeFull(true);

		ResponsiveRow rootResponsiveRow = content_area.addRow();
		rootResponsiveRow.setSizeFull();

		ResponsiveColumn treeCol = rootResponsiveRow.addColumn().withDisplayRules(12, 12, 7, 7);
		objectbrowser_tree = new DrumbeatTree(app_url, url, name_spaces_handler);
		treeCol.withComponent(objectbrowser_tree.getLayout());

		ResponsiveColumn detailsCol = rootResponsiveRow.addColumn().withDisplayRules(12, 12, 5, 5);
		ContentArea ca = new ContentArea();
		detailsCol.withComponent(ca);
		ResponsiveColumn info_co = ca.addColumn().withDisplayRules(12, 12, 12, 12).withVisibilityRules(true, true, true,
				true);

		ResponsiveColumn editor_co = createInfoBrowserPanel(ca, info_co);

		annotation_DataModel.read_data();
		createAnnotationsPanel(editor_co);

		main_layout.addComponent(content_area);
		main_layout.addComponents(ns_table);
		main_layout.setMargin(true);
		main_layout.setSpacing(true);
		setContent(main_layout);
		communication.register(this);
		objectbrowser_tree.initialize(); // Timing
	}

	/**
	 * @param request
	 * @return
	 */
	private String handleURLparameter(VaadinRequest request) {
		String app_url = "";
		String req_url = request.getParameter("url");
		if (req_url != null) {
			try {
				// validity check for the url
				URL u = new URL(req_url);
				if (u.getPath() != null) {
					url = req_url;
				}
			} catch (Exception e) {
				// The given URL is not valid. Do nothing
			}
		}

		try {
			String[] uri_parts = (Page.getCurrent().getLocation().getScheme() + ":"
					+ Page.getCurrent().getLocation().getSchemeSpecificPart()).split("?");
			app_url = uri_parts[0];

		} catch (Exception e) {

		}
		return app_url;
	}

	/**
	 * @param ca
	 * @param info_co
	 * @return
	 */
	private ResponsiveColumn createInfoBrowserPanel(ContentArea ca, ResponsiveColumn info_co) {
		final VerticalLayout browser_layout = new VerticalLayout();
		this.browser = Optional.of(new BrowserFrame("More Information (Powered bt Google)",
				new ExternalResource("http://www.buildingsmart-tech.org/specifications/ifc-overview")));
		if (browser.isPresent()) {
			browser.get().setWidth("600px");
			browser.get().setHeight("400px");
			browser_layout.addComponent(browser.get());
		}
		info_co.setComponent(browser_layout);

		ResponsiveColumn editor_co = ca.addColumn().withDisplayRules(12, 12, 12, 12).withVisibilityRules(true, true,
				true, true);
		return editor_co;
	}

	/**
	 * @param editor_co
	 */
	private void createAnnotationsPanel(ResponsiveColumn editor_co) {
		final VerticalLayout editor_layout = new VerticalLayout();
		this.richTextArea = Optional.of(new RichTextArea());
		if(richTextArea.isPresent())
		{
		richTextArea.get().setWidth("600px");
		richTextArea.get().setHeight("410px");
		richTextArea.get().setCaption("Comments editor for the entity");
		richTextArea.get().setValue("");
		richTextArea.get().addValueChangeListener(new Property.ValueChangeListener() {
			private static final long serialVersionUID = 7673034039823701356L;

			@Override
			public void valueChange(Property.ValueChangeEvent valueChangeEvent) {
				try {
					if (objectbrowser_tree.getMain_node() != null) {
						if(objectbrowser_tree.getMain_node().isPresent())
						if (objectbrowser_tree.getMain_node().get().hasURL()) {
							annotation_DataModel.getData_store().getUri_content()
									.put(objectbrowser_tree.getMain_node().get().getURI(), richTextArea.get().getValue());
							annotation_DataModel.save_data();
						}
					}
				} catch (Exception e) {
					// If exception, do nothing
				}

			}
		});
		editor_layout.addComponent(richTextArea.get());
		}
		editor_co.setComponent(editor_layout);
	}

	/**
	 * @author joraskur
	 *
	 */
	private static class ContentArea extends ResponsiveRow {
		private static final long serialVersionUID = 1L;

		public ContentArea() {
			setMargin(true);
			setVerticalSpacing(true);
			setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
		}
	}

	/**
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	@Subscribe
	public void doNSTable(NameSpaceDataChangedEvent event) {
		if (ns_table.getUI() == null)
			return;
		// ns_table.getUI().getSession().getLockInstance().lock();
		try {
			ns_table.removeAllItems();
			TreeMap<String, String> keys_uris = new TreeMap<String, String>();
			for (Map.Entry<String, String> entry : name_spaces_handler.getName_spaces().entrySet()) {
				keys_uris.put(entry.getValue(), entry.getKey());
			}

			for (Map.Entry<String, String> entry : keys_uris.entrySet()) {
				Object newItemId = ns_table.addItem();
				Item row1 = ns_table.getItem(newItemId);
				row1.getItemProperty("Name").setValue(entry.getKey());
				row1.getItemProperty("URI").setValue(entry.getValue());
			}
			ns_table.setPageLength(ns_table.size());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// ns_table.getUI().getSession().getLockInstance().unlock();
		}
	}

	private boolean main_url_set = false;
	private boolean main_edit_set = false;

	/**
	 * @param event
	 */
	@Subscribe
	public void updateInfoPanels(MainNodeUpdateEvent event) {
		if (!main_edit_set) {
			if (event.getMain_node().hasURL() && annotation_DataModel.getData_store() != null) {
				String data = annotation_DataModel.getData_store().getUri_content().get(event.getMain_node().getURI());
				if (data != null) {
					if(richTextArea.isPresent())
					  richTextArea.get().setValue(data);
					main_edit_set = true;
				}
			}
		}

		if (!main_url_set && browser != null) {
			String type = event.getMain_node().getType();
			if (type != null && type.length() > 0) {
				if (type.startsWith("Ifc")) {
					// TODO Check the IFC version
					if(browser.isPresent())
					   browser.get().setSource(new ExternalResource(
							"http://www.google.com/search?ie=UTF-8&oe=UTF-8&sourceid=navclient&gfns=1&q="
									+ type.toLowerCase()));
					main_url_set = true;
				}
			}
		}
	}

	@SuppressWarnings("serial")
	@WebServlet(urlPatterns = "/*", name = "DrumbeatObjectBrowserServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = DrumbeatObjectBrowser_Main.class, productionMode = false)
	public static class DrumbeatObjectBrowserServlet extends VaadinServlet {
	}
}
