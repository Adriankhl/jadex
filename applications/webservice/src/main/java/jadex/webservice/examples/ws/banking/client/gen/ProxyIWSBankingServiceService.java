
package jadex.webservice.examples.ws.banking.client.gen;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "ProxyIWSBankingServiceService", targetNamespace = "http://jadex.webservice.examples.ws.banking/", wsdlLocation = "http://localhost:8080/banking/?WSDL")
public class ProxyIWSBankingServiceService
    extends Service
{

    private final static URL PROXYIWSBANKINGSERVICESERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(jadex.webservice.examples.ws.banking.client.gen.ProxyIWSBankingServiceService.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = jadex.webservice.examples.ws.banking.client.gen.ProxyIWSBankingServiceService.class.getResource(".");
            url = new URL(baseUrl, "http://localhost:8080/banking/?WSDL");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'http://localhost:8080/banking/?WSDL', retrying as a local file");
            logger.warning(e.getMessage());
        }
        PROXYIWSBANKINGSERVICESERVICE_WSDL_LOCATION = url;
    }

    public ProxyIWSBankingServiceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ProxyIWSBankingServiceService() {
        super(PROXYIWSBANKINGSERVICESERVICE_WSDL_LOCATION, new QName("http://jadex.webservice.examples.ws.banking/", "ProxyIWSBankingServiceService"));
    }

    /**
     * 
     * @return
     *     returns ProxyIWSBankingService
     */
    @WebEndpoint(name = "ProxyIWSBankingServicePort")
    public ProxyIWSBankingService getProxyIWSBankingServicePort() {
        return super.getPort(new QName("http://jadex.webservice.examples.ws.banking/", "ProxyIWSBankingServicePort"), ProxyIWSBankingService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ProxyIWSBankingService
     */
    @WebEndpoint(name = "ProxyIWSBankingServicePort")
    public ProxyIWSBankingService getProxyIWSBankingServicePort(WebServiceFeature... features) {
        return super.getPort(new QName("http://jadex.webservice.examples.ws.banking/", "ProxyIWSBankingServicePort"), ProxyIWSBankingService.class, features);
    }

}
