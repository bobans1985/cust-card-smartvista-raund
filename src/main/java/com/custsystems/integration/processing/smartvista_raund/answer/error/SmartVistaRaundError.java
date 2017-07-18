package com.custsystems.integration.processing.smartvista_raund.answer.error;

import com.custsystems.custtools.XmlTools;
import com.custsystems.integration.card.ProcessingControlException;
import com.custsystems.integration.card.ProcessingRequestException;
import com.custsystems.integration.card.custom.answer.CardResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by GrishukovVM on 06.07.2017.
 */
public class SmartVistaRaundError extends CardResult {

    public SmartVistaRaundError(Object data) throws ProcessingControlException, ProcessingRequestException {
        super(data);
    }

    @Override
    public void init(Object data) throws ProcessingRequestException, ProcessingControlException {
        String faultcode;
        String faultstring;

        if (!(data instanceof Document)) {
            throw new ProcessingRequestException("SmartVista Raund Api-Gate response object not instance of XML document!");
        } else {
            Document document = (Document) data;
            Element e1 = document.getDocumentElement();
            Element cardLockResponse = XmlTools.getBranchElement(e1, "Body/Fault");
            faultcode = XmlTools.getStringEx(cardLockResponse, "faultcode");
            faultstring = XmlTools.getStringEx(cardLockResponse, "faultstring");
            throw new ProcessingRequestException("faultcode=" + faultcode + "; faultstring=" + faultstring);
        }
    }
}