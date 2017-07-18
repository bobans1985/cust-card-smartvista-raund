package com.custsystems.integration.processing.smartvista_raund;

import com.custsystems.custerp.binding.ModelReaderBinding;
import com.custsystems.custerp.kernel.custerpProperties.CusterpProperties;
import com.custsystems.custerp.kernel.metaData.CusterpMetadataLocator;
import com.custsystems.custerp.kernel.metaData.cache.MetadataLocatorCacheManager;
import com.custsystems.custerp.kernel.metaData.cache.MetadataLocatorType;
import com.custsystems.custtools.StringTools;
import com.custsystems.custtools.XmlTools;
import com.custsystems.custtools.binding.IReaderBinding;
import com.custsystems.custtools.binding.MapBinding;
import com.custsystems.custtools.binding.ModelReaderBindingCollection;
import com.custsystems.integration.card.*;
import com.custsystems.integration.card.custom.answer.*;
import com.custsystems.integration.card.custom.request.*;
import com.custsystems.integration.common.ITestConnection;
import com.custsystems.integration.common.answer.testConnection.ErrorTestConnection;
import com.custsystems.integration.common.answer.testConnection.ITestConnectionAnswer;
import com.custsystems.integration.common.answer.testConnection.SuccessTestConnection;
import com.custsystems.integration.processing.smartvista_raund.answer.balance.SmartVistaRaundBalance;
import com.custsystems.integration.processing.smartvista_raund.answer.error.SmartVistaRaundError;
import com.custsystems.integration.processing.smartvista_raund.answer.lock.SmartVistaRaundLock;
import com.custsystems.integration.processing.smartvista_raund.answer.statement.SmartVistaRaundStm;
import com.custsystems.integration.processing.smartvista_raund.answer.status.SmartVistaRaundStatus;
import com.custsystems.integration.transform.FileBindingTransformer;
import com.custsystems.utils.ProxyTools;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;


/**
 * Created by GrishukovVM on 08.09.2016.
 */
public class ProcessingConnectorSmartVistaRaund extends ProcessingConnector
        implements ITestConnection, IProcessingConnectorPing, IProcessingConnectorBalance, IProcessingConnectorStatement, IProcessingConnectorLock, IProcessingConnectorStatus {

    public ProcessingConnectorSmartVistaRaund(String className, CusterpProperties properties) {
        super(className, properties);
    }

    public ProcessingConnectorSmartVistaRaund(CusterpProperties properties) {
        super(ProcessingConnectorSmartVistaRaund.class.getName(), properties);
    }

    protected static CusterpMetadataLocator getDefaultLocator() {
        return MetadataLocatorCacheManager.getLocator(MetadataLocatorType.XML, "data/ibankfl/processing/smartvista_raund");
    }

    protected static CusterpMetadataLocator getTemplateLocator(String service) {
        return MetadataLocatorCacheManager.getLocator(MetadataLocatorType.XML, getDefaultLocator(), "?service=" + service + ";extension=.xml");
    }

    /*Проверка корректности номера карты*/
    protected static void checkPAN(String PAN) throws ProcessingRequestException {
        if (PAN == null || !StringTools.equalLength(PAN, 16) || StringTools.isZero(PAN)) {
            throw new ProcessingRequestException("Invalid format of PAN!");
        }
    }


    protected HttpURLConnection getConnection(String uri) throws Exception {
        URL url = new URL(this.getProperties().getStringRaise("host") + (uri != null ? uri : ""));
        CusterpProperties proxyProperties = this.getProperties().getChildProperties("proxy");
        getLogger().trace((String) "Connect to: {0}", (Object) url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(ProxyTools.getProxy(proxyProperties != null ? proxyProperties : new CusterpProperties()));
        connection.setRequestMethod(this.getProperties().getString("request_method", "POST"));
        connection.setRequestProperty("Content-type", "text/xml; charset=utf-8");
        connection.setConnectTimeout(this.getProperties().getInteger("connecttimeout", Integer.valueOf(300000)).intValue());
        connection.setReadTimeout(this.getProperties().getInteger("readtimeout", Integer.valueOf(300000)).intValue());
        connection.setDoInput(true);
        connection.setDoOutput(true);

        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConnection1 = (HttpsURLConnection) connection;
            //httpsConnection1.setSSLSocketFactory(this.createSSLSocketFactory());
            httpsConnection1.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
        }

        return connection;
    }

    /*Собственно сам запрос*/
    protected Document sendRequest(String service, IReaderBinding binding, String uri) throws ProcessingRequestException, ProcessingControlException {
        getLogger().debug("sendRequest processRequest(" + service + ") start");
        Document e3;
        try {
            CusterpMetadataLocator e = getDefaultLocator();
            CusterpMetadataLocator templateLocator = getTemplateLocator(service);
            if (!templateLocator.exist()) {
                throw new ProcessingControlException("Can\'t find SmartVista Api-gate template: " + service);
            }
            ModelReaderBinding reader = new ModelReaderBinding(binding, this.getProperties(), e);
            HttpURLConnection connection = this.getConnection(uri);
            Document document;
            try {
                OutputStream e1 = connection.getOutputStream();
                try {
                    FileBindingTransformer out = new FileBindingTransformer(templateLocator, e);
                    out.setBinding(reader);
                    out.transform(e1);
                    e1.flush();
                    getLogger().debug((String) "Request:\n{0}", (Object) out);
                } finally {
                    e1.close();
                }
                InputStream out1;
                if (connection.getResponseCode() != 200) {
                    out1 = connection.getErrorStream();
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(out1));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            sb.append(line).append('\n');
                        }
                        if (!(XmlTools.createDocument(sb.toString()) instanceof Document)) {
                            throw new ProcessingControlException("Http response code:  " + connection.getResponseCode() + "\n" + sb.toString());
                        } else {   /*тогда обрабатываем ошибку от api-gate*/
                            getLogger().error("Http response code:  " + connection.getResponseCode() + "\n" + sb.toString());
                            new SmartVistaRaundError(XmlTools.createDocument(sb.toString()));
                        }
                    } finally {
                        out1.close();
                    }
                }
                out1 = connection.getInputStream();
                try {
                    document = XmlTools.getDocument(out1);
                    if (document == null) {
                        throw new ProcessingControlException("Error parsing response XML data");
                    }
                } finally {
                    out1.close();
                }
            } finally {
                connection.disconnect();
            }
            if (getLogger().isDebugEnabled()) {
                try {
                    Transformer e2 = TransformerFactory.newInstance().newTransformer();
                    e2.setOutputProperty("indent", "yes");
                    e2.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                    ByteArrayOutputStream out2 = new ByteArrayOutputStream();
                    e2.transform(new DOMSource(document), new StreamResult(out2));
                    getLogger().debug((String) "Transformed response:\n{0}", (Object) out2);
                } catch (Exception var61) {
                    getLogger().error((String) "Error in XML transformer", (Throwable) var61);
                }
            }
            e3 = document;
        } catch (ProcessingControlException var66) {
            //getLogger().error((Throwable) var66);
            throw var66;
        } catch (ProcessingRequestException var67) {
            //getLogger().error((Throwable) var67);
            throw var67;
        } catch (Exception var68) {
            //getLogger().error((Throwable) var68);
            throw new ProcessingRequestException(var68.getMessage(), var68);
        } finally {
            getLogger().debug("processRequest(" + service + ") end");
        }
        return e3;
    }

    /* Тест соединения IProcessingConnectorPing*/
    //Тупо есть или нет соединенения
    @Override
    public boolean ping() {
        try {
            HttpURLConnection e = this.getConnection(null);
            e.connect();
            e.disconnect();
            return true;
        } catch (Throwable var2) {
            return false;
        }
    }

    /* Тест соединения при нажатии на кнопку из настроек ITestConnection*/
    @Override
    public ITestConnectionAnswer testConnection() {
        try {
            HttpURLConnection e = this.getConnection(null);
            e.connect();
            e.disconnect();
            return (ITestConnectionAnswer) (SuccessTestConnection.instance);
        } catch (Throwable var3) {
            return new ErrorTestConnection(var3.getMessage());
        }


    }

    /*Получение остатка по карте IProcessingConnectorBalance*/
    @Override
    public ICardBalance getBalance(ICardBalanceRequest request) throws ProcessingControlException, ProcessingRequestException {
        String PAN = request.getCard().getNumber();
        checkPAN(PAN);
        String uri = this.getProperties().getString("smartvista_raund/uri_balance", "apigate/command/getCardBalance");
        ModelReaderBindingCollection binding = new ModelReaderBindingCollection(new IReaderBinding[]{(new MapBinding()).set("PAN", PAN)});
        SmartVistaRaundBalance response = new SmartVistaRaundBalance(this.sendRequest("balance", binding, uri));
        response.setActive(true);
        return response;
    }

    /*Выписка по карте*/
    @Override
    public ICardStatement getStatement(ICardStatementRequest request) throws ProcessingControlException, ProcessingRequestException {
        String PAN = request.getCard().getNumber();
        checkPAN(PAN);
        String uri = this.getProperties().getString("smartvista_raund/uri_stm", "apigate/command/getTransactions");

        Timestamp dtSt = request.getBeginDate();
        Timestamp dtEn = request.getEndDate();
        //dtSt = ((dtEn.getTime() - dtSt.getTime()) / (1000 * 60 * 60 * 24)) > 7 ? new Timestamp(dtEn.getTime()-Long.valueOf(7*24 * 60 * 60 * 1000)) : dtSt;

        IReaderBinding binding = new ModelReaderBindingCollection(new IReaderBinding[]{new MapBinding().set("PAN", PAN).set("from_date", dtSt).set("to_date", dtEn)});
        return new SmartVistaRaundStm(this.sendRequest("statement", binding, uri), this.getProperties());
    }

    /*Блокировка карты*/
    @Override
    public ICardResult lock(ICardLockRequest request) throws ProcessingControlException, ProcessingRequestException {
        String PAN = request.getCard().getNumber();
        checkPAN(PAN);
        String status = this.getProperties().getString("smartvista_raund/block_status", "05");
        String uri = this.getProperties().getString("smartvista_raund/uri_block", "apigate/command/blockCard");
        IReaderBinding binding = new ModelReaderBindingCollection(new IReaderBinding[]{new MapBinding().set("PAN", PAN).set("STATUS", status)});
        return new SmartVistaRaundLock(this.sendRequest("lock", binding, uri));
    }

    /*Разблокировка карты - заглушка*/
    @Override
    public ICardResult unlock(ICardUnlockRequest request) throws ProcessingControlException, ProcessingRequestException {
        return new SimpleCardResult(request) {
            public void init(Object data) {
                this.ok = false;
                this.message = "Unlocking service is not supported!";
            }
        };
    }

    /*Запрос статуса карты*/
    @Override
    public ICardStatus getStatus(ICardStatusRequest request) throws ProcessingControlException, ProcessingRequestException {
        String PAN = request.getCard().getNumber();
        checkPAN(PAN);
        String uri = this.getProperties().getString("smartvista_raund/uri_status", "apigate/command/cardStatusInquiry");
        IReaderBinding binding = new ModelReaderBindingCollection(new IReaderBinding[]{new MapBinding().set("PAN", PAN)});
        return new SmartVistaRaundStatus(this.sendRequest("status", binding, uri));
    }
}
